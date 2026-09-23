package co.icesi.buscaminas.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import co.icesi.buscaminas.client.dtos.Request;
import co.icesi.buscaminas.client.dtos.Response;

/**
 * Emisor de peticiones: abre una conexion TCP por peticion (short-lived connection),
 * envia el Request como una linea JSON y lee una linea JSON de respuesta.
 */
public class BuscaminasTCPClient {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;

    private final String host;
    private final int port;
    private final Gson gson = new GsonBuilder().create();
    private String player;

    public BuscaminasTCPClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setPlayer(String player) {
        this.player = player;
    }

    public Response send(Request request) throws IOException {
        // Clave extra "jugador" para que el servidor sepa quien hizo cada jugada
        if (player != null && !player.isBlank()) {
            request.data.put("jugador", player);
        }
        return sendRequest(host, port, request);
    }

    public Response sendRequest(String host, int port, Request request) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);
            try (BufferedReader reader = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 BufferedWriter writer = new BufferedWriter(
                         new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

                // Serializar Request a JSON y enviar con salto de linea (framing por '\n')
                String jsonOut = gson.toJson(request);
                writer.write(jsonOut);
                writer.newLine();
                writer.flush();

                // Leer la respuesta delimitada por fin de linea
                String jsonIn = reader.readLine();
                if (jsonIn == null) {
                    throw new IOException("Server closed the connection without responding");
                }
                try {
                    Response response = gson.fromJson(jsonIn, Response.class);
                    if (response == null || response.status == null) {
                        throw new IOException("Empty response from server");
                    }
                    return response;
                } catch (JsonSyntaxException e) {
                    throw new IOException("Invalid JSON from server: " + e.getMessage(), e);
                }
            }
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
