package co.icesi.buscaminas.client;

import co.icesi.buscaminas.client.model.Cell;

/**
 * Dibuja el tablero en consola con indices de fila/columna y colores ANSI.
 */
public class BoardRenderer {

    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String GRAY = "\u001B[90m";
    public static final String BOLD = "\u001B[1m";

    private BoardRenderer() {
    }

    public static String render(Cell[][] board) {
        if (board == null || board.length == 0 || board[0].length == 0) {
            return "(tablero vacio)";
        }
        int rows = board.length;
        int cols = board[0].length;
        int rowLabelWidth = String.valueOf(rows - 1).length();
        StringBuilder sb = new StringBuilder();

        // Cabecera de columnas: cada celda ocupa 5 caracteres "[ x ]", el indice va centrado
        sb.append(" ".repeat(rowLabelWidth + 1));
        for (int j = 0; j < cols; j++) {
            sb.append(BOLD).append(String.format("%3d  ", j)).append(RESET);
        }
        sb.append('\n');

        for (int i = 0; i < rows; i++) {
            sb.append(BOLD).append(String.format("%" + rowLabelWidth + "d ", i)).append(RESET);
            for (int j = 0; j < cols; j++) {
                sb.append("[ ").append(symbol(board[i][j])).append(" ]");
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    static String symbol(Cell cell) {
        if (cell.isMarked()) {
            return YELLOW + "M" + RESET;
        }
        if (cell.isHide() && !cell.isShowAll()) {
            return GRAY + "." + RESET;
        }
        if (cell.isLandMine()) {
            return RED + "*" + RESET;
        }
        int v = cell.getValue();
        if (v == 0) {
            return " ";
        }
        return numberColor(v) + v + RESET;
    }

    private static String numberColor(int v) {
        switch (v) {
            case 1:
                return BLUE;
            case 2:
                return GREEN;
            case 3:
                return MAGENTA;
            default:
                return CYAN;
        }
    }
}
