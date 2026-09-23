package co.icesi.buscaminas.controllers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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

    private ServicesImpl services;

    private ServerSocket serverSocket;

    private boolean running;

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
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

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
                            throw new IllegalArgumentException("Request without action");
                        }
                        System.out.println("[" + thread + "] " + client + " -> " + rq.action + " " + rq.data);
                        handle(rq, response);
                    } catch (JsonSyntaxException e) {
                        error(response, "Malformed JSON request");
                    } catch (IllegalArgumentException e) {
                        error(response, e.getMessage());
                    }
                    json = gson.toJson(response);
                }

                writer.write(json);
                writer.newLine();
                writer.flush();
                System.out.println("[" + thread + "] " + client + " <- " + response.status
                        + (response.data.containsKey("message") ? " (" + response.data.get("message") + ")" : ""));
            } catch (Exception e) {
                System.out.println("[" + thread + "] Error with client " + client + ": " + e.getMessage());
            }
            System.out.println("[" + thread + "] Client disconnected: " + client);
        }

        private void handle(Request rq, Response response) {
            Map<String, String> data = rq.data != null ? rq.data : new HashMap<>();
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
                            response.data.put("message", "All safe cells revealed, you win!");
                        }
                    } catch (IllegalArgumentException e) {
                        throw e;
                    } catch (RuntimeException e) {
                        // Pisar una mina es un resultado valido del juego, no un error del protocolo
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
                    services.markCell(mi, mj);
                    response.status = "OK";
                    board = services.printBoard();
                    response.data.put("board", board);
                    break;
                case "SOW_ALL":
                    services.showAll(true);
                    board = services.printBoard();
                    response.status = "OK";
                    response.data.put("board", board);
                    break;
                case "GET_BOARD":
                    board = services.printBoard();
                    response.status = "OK";
                    response.data.put("board", board);
                    break;
                case "INIT_GAME":
                    int n = intParam(data, "n");
                    int m = intParam(data, "m");
                    int mines = intParam(data, "minas");
                    services.initGame(n, m, mines);
                    board = services.printBoard();
                    response.status = "OK";
                    response.data.put("board", board);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown action: " + rq.action);
            }
        }

        private int intParam(Map<String, String> data, String key) {
            String value = data.get(key);
            if (value == null) {
                throw new IllegalArgumentException("Missing parameter '" + key + "'");
            }
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Parameter '" + key + "' must be an integer");
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
