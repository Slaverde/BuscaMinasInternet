package co.icesi.buscaminas.client.dtos;

import co.icesi.buscaminas.client.model.Cell;

/**
 * Misma trama que el Response del servidor, pero con "data" tipado para que Gson
 * deserialice el tablero directamente como Cell[][] en lugar de mapas genericos.
 */
public class Response {
    public String status;
    public Data data;

    public static class Data {
        public Cell[][] board;
        public Boolean win;
        public Boolean gameEnd;
        public String message;
    }

    public boolean isOk() {
        return "OK".equals(status);
    }

    public boolean isGameEnd() {
        return data != null && Boolean.TRUE.equals(data.gameEnd);
    }

    public boolean isWin() {
        return data != null && Boolean.TRUE.equals(data.win);
    }

    public Cell[][] board() {
        return data != null ? data.board : null;
    }

    public String message() {
        return data != null ? data.message : null;
    }
}
