package co.icesi.buscaminas.controllers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import co.icesi.buscaminas.controllers.dtos.Request;
import co.icesi.buscaminas.controllers.dtos.Response;
import co.icesi.buscaminas.model.Cell;
import co.icesi.buscaminas.services.ServicesImpl;

public class TCPController {

    public static final int DEFAULT_PORT = 12345;

    private static final int POOL_SIZE = 5;

    // Tiempo maximo esperando la peticion: evita que un cliente que se conecta y no
    // envia nada deje ocupado para siempre uno de los hilos del pool.
    private static final int READ_TIMEOUT_MS = 5000;

    private ServicesImpl services;

    private ServerSocket serverSocket;

    private volatile boolean running;

    private ExecutorService executor;

    private Gson gson;

    public TCPController(ServicesImpl services) {
        this(services, DEFAULT_PORT);
    }

    public TCPController(ServicesImpl services, int port) {
        this.services = services;
        try {
            // 0.0.0.0 escucha en todas las interfaces (localhost, WiFi, Ethernet...),
            // asi el servidor no depende de la IP de una maquina en particular.
            serverSocket = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"));
            executor = Executors.newFixedThreadPool(POOL_SIZE);
            gson = new GsonBuilder().create();
        } catch (Exception e) {
            throw new IllegalStateException("Could not open TCP server on port " + port, e);
        }
        running = true;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isRunning() {
        return running;
    }

    public void startService() {
        System.out.println("TCP Service started on " + serverSocket.getInetAddress().getHostAddress()
                + ":" + serverSocket.getLocalPort() + " with a pool of " + POOL_SIZE + " threads");
        while (running) {
            try {
                executor.execute(new TCPClientHandler(serverSocket.accept(), services));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();
        try {
            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class TCPClientHandler implements Runnable {

        private Socket clientSocket;
        private ServicesImpl services;

        public TCPClientHandler(Socket clientSocket, ServicesImpl services) {
            this.clientSocket = clientSocket;
            this.services = services;
        }

        @Override
        public void run() {
            String thread = Thread.currentThread().getName();
            String client = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
            try (Socket socket = clientSocket;
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 BufferedWriter writer = new BufferedWriter(
                         new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

                socket.setSoTimeout(READ_TIMEOUT_MS);
                System.out.println("[" + thread + "] Client connected: " + client);
                String line = reader.readLine();
                Response response = new Response();
                response.data = new HashMap<>();

                String json;
                // Se serializa dentro del lock del tablero para que la respuesta sea
                // una foto consistente y no se mezcle con la jugada de otro cliente.
                synchronized (services.getGame()) {
                    try {
                        Request rq = gson.fromJson(line, Request.class);
                        if (rq == null || rq.action == null) {
                            throw new IllegalArgumentException("Peticion sin accion");
                        }
                        System.out.println("[" + thread + "] " + client + " -> " + rq.action + " " + rq.data);
                        handle(rq, response, clientSocket.getInetAddress().getHostAddress());
                    } catch (JsonSyntaxException e) {
                        error(response, "JSON mal formado");
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        error(response, e.getMessage());
                    }
                    json = gson.toJson(response);
                }

                writer.write(json);
                writer.newLine();
                writer.flush();
                System.out.println("[" + thread + "] " + client + " <- " + response.status
                        + (response.data.containsKey("message") ? " (" + response.data.get("message") + ")" : ""));
            } catch (SocketTimeoutException e) {
                System.out.println("[" + thread + "] " + client + " no envio peticion en " + READ_TIMEOUT_MS
                        + " ms, se libera el hilo");
            } catch (Exception e) {
                System.out.println("[" + thread + "] Error with client " + client + ": " + e.getMessage());
            }
            System.out.println("[" + thread + "] Client disconnected: " + client);
        }

        private void handle(Request rq, Response response, String clientIp) {
            Map<String, String> data = rq.data != null ? rq.data : new HashMap<>();
            // "jugador" es opcional y no forma parte del contrato de la guia; si no llega
            // se identifica al jugador por su IP, como hacia el Servidor original.
            String player = playerName(data, clientIp);
            Cell[][] board;
            switch (rq.action) {
                case "SELECT_CELL":
                    int i = intParam(data, "i");
                    int j = intParam(data, "j");
                    try {
                        boolean resp = services.selectCell(i, j);
                        response.status = "OK";
                        response.data.put("win", resp);
                        response.data.put("gameEnd", resp);
                        if (resp) {
                            services.registerEvent(player + " destapo (" + i + ", " + j + ") y gano la partida");
                            response.data.put("message", "Todas las celdas seguras quedaron destapadas. Ganaste!");
                        } else {
                            services.registerEvent(player + " destapo (" + i + ", " + j + ")");
                            response.data.put("message", "Ultima jugada: " + services.getLastEvent());
                        }
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        throw e;
                    } catch (RuntimeException e) {
                        // Pisar una mina es un resultado valido del juego, no un error del protocolo
                        services.registerEvent(player + " piso una mina en (" + i + ", " + j + ")");
                        response.status = "OK";
                        response.data.put("gameEnd", true);
                        response.data.put("win", false);
                        response.data.put("message", e.getMessage());
                    }
                    board = services.printBoard();
                    response.data.put("board", board);
                    break;
                case "MARK_CELL":
                    int mi = intParam(data, "i");
                    int mj = intParam(data, "j");
                    board = services.markCell(mi, mj);
                    services.registerEvent(player + (board[mi][mj].isMarked() ? " marco" : " desmarco")
                            + " (" + mi + ", " + mj + ")");
                    response.status = "OK";
                    response.data.put("message", "Ultima jugada: " + services.getLastEvent());
                    board = services.printBoard();
                    response.data.put("board", board);
                    break;
                case "SOW_ALL":
                    // Si la partida ya termino (p.ej. el cliente revela tras pisar una mina) no es una rendicion
                    if (!services.getGame().isGameOver()) {
                        services.registerEvent(player + " se rindio y revelo el tablero");
                    }
                    services.showAll(true);
                    board = services.printBoard();
                    response.status = "OK";
                    response.data.put("board", board);
                    break;
                case "GET_BOARD":
                    board = services.printBoard();
                    response.status = "OK";
                    response.data.put("message", "Ultima jugada: " + services.getLastEvent()
                            + (services.getGame().isGameOver() ? " (partida terminada)" : ""));
                    response.data.put("board", board);
                    break;
                case "INIT_GAME":
                    int n = intParam(data, "n");
                    int m = intParam(data, "m");
                    int mines = intParam(data, "minas");
                    services.initGame(n, m, mines);
                    services.registerEvent(player + " inicio una partida de " + n + "x" + m + " con " + mines + " minas");
                    board = services.printBoard();
                    response.status = "OK";
                    response.data.put("board", board);
                    break;
                default:
                    throw new IllegalArgumentException("Accion desconocida: " + rq.action);
            }
        }

        private String playerName(Map<String, String> data, String clientIp) {
            String name = data.get("jugador");
            if (name == null || name.isBlank()) {
                return "Jugador en " + clientIp;
            }
            name = name.trim();
            return name.length() > 20 ? name.substring(0, 20) : name;
        }

        private int intParam(Map<String, String> data, String key) {
            String value = data.get(key);
            if (value == null) {
                throw new IllegalArgumentException("Falta el parametro '" + key + "'");
            }
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("El parametro '" + key + "' debe ser un numero entero");
            }
        }

        private void error(Response response, String message) {
            response.status = "ERROR";
            response.data.put("message", message);
            // Se adjunta el tablero para que el cliente pueda seguir mostrando el estado
            response.data.put("board", services.printBoard());
        }
    }

}
