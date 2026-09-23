package co.icesi.buscaminas.model;

import java.util.Random;

public class BoardGame {

    private Cell[][] board;

    private int mines;

    // Idea tomada del Servidor original (juegoTerminado): tras ganar, perder o rendirse
    // no se aceptan mas jugadas hasta que alguien inicie una nueva partida.
    private boolean gameOver;

    public int getMines() {
        return mines;
    }

    public synchronized int initGame(int n, int m, int mines){
        if (n <= 0 || m <= 0) {
            throw new IllegalArgumentException("Las dimensiones del tablero deben ser positivas");
        }
        if (mines < 0 || mines >= n * m) {
            throw new IllegalArgumentException("Las minas deben estar entre 0 y " + (n * m - 1));
        }
        this.mines = mines;
        gameOver = false;
        board = new Cell[n][m];
        Random rd = new Random();
        int mi = 0;
        for (int i = 0; i <n; i++) {
            for (int j = 0; j < m; j++) {
                boolean isMine = false;
                board[i][j] = new Cell(isMine,0);
            }
        }
        for (int i = 0; i < mines; i++) {
            int k =rd.nextInt(n);
            int l = rd.nextInt(m);
            Cell cell = board[k][l];
            mi += cell.isLandMine()?0:1;
            cell.setLandMine(true);
        }
        for (int i = 0; i <n; i++) {
            for (int j = 0; j < m; j++) {
                boolean isMine = board[i][j].isLandMine();
                if(!isMine){
                    int minesAround = getMinesAround(i,j);
                    board[i][j].setValue(minesAround);
                }
            }
        }
        return mi;
    }

    public synchronized void showAll(boolean show){
        for (int i = 0; i <board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                board[i][j].setShowAll(show);
            }
        }
        if (show) {
            gameOver = true;
        }
    }

    public synchronized boolean isGameOver() {
        return gameOver;
    }

    private void validateMove(int i, int j) {
        if(i<0 || i>= board.length || j<0 || j >= board[0].length ){
            throw new IllegalArgumentException("La celda (" + i + ", " + j + ") esta fuera del tablero de "
                    + board.length + "x" + board[0].length);
        }
        if (gameOver) {
            throw new IllegalStateException("La partida ya termino. Inicia una nueva partida para seguir jugando");
        }
    }

    private int getMinesAround(int i, int j) {
        int mines = 0;
        mines += i > 0 && board[i-1][j].isLandMine()?1:0;
        mines += i < board.length-1 && board[i+1][j].isLandMine()?1:0;
        mines += j > 0 && board[i][j-1].isLandMine()?1:0;
        mines += j < board[0].length-1 && board[i][j+1].isLandMine()?1:0;
        mines += i > 0 && j > 0 && board[i-1][j-1].isLandMine()?1:0;
        mines += i > 0 && j < board[0].length-1 && board[i-1][j+1].isLandMine()?1:0;
        mines += i < board.length-1 && j > 0 && board[i+1][j-1].isLandMine()?1:0;
        mines += i < board.length-1 && j < board[0].length-1 && board[i+1][j+1].isLandMine()?1:0;
        return mines;
    }

    public synchronized void printBoard(){
        System.out.println();
        System.out.print("   ");
        for (int i = 0; i < board[0].length; i++) {
            System.out.print(" " + i);
        }
        System.out.println();
        for (int i = 0; i <board.length; i++) {
            System.out.print(i+" [");
            for (int j = 0; j < board[0].length; j++) {
                System.out.print(" "+board[i][j]);
            }
            System.out.println(" ]");
        }
    }
    public synchronized boolean selectCell(int i, int j){
        validateMove(i, j);
        Cell cell = board[i][j];
        if(cell.isMarked()){
            throw new IllegalArgumentException("La celda (" + i + ", " + j + ") tiene bandera; desmarcala antes de destaparla");
        }
        if(cell.isLandMine()){
            showAll(true);
            throw new RuntimeException("BOOM! Mina en (" + i + ", " + j + "). Fin del juego");
        }else {
            if (cell.isHide()) {
                showCells(i,j,true);
            }
            boolean win = validWin();
            gameOver = win;
            return win;
        }
    }

    private boolean validWin(){
        boolean win = true;
        for (int i = 0; i <board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                win &= !board[i][j].isHide() || board[i][j].isLandMine();
            }
        }
        return win;
    }

    private void showCells(int i, int j, boolean deep) {
        // Como en revelarCeldas del Servidor original: la cascada no destapa celdas con bandera
        if(i<0 || i>= board.length || j<0 || j >= board[0].length || !board[i][j].isHide()
                || board[i][j].isMarked()){
            return;
        }
        if(deep && board[i][j].isHide()){
            board[i][j].setHide(false);
            deep = board[i][j].getValue() == 0;
        }

        if (board[i][j].getValue() == 0) {
            showCells(i, j - 1, deep);
            showCells(i, j + 1, deep);
            showCells(i - 1, j, deep);
            showCells(i + 1, j, deep);
            showCells(i - 1, j - 1, deep);
            showCells(i - 1, j + 1, deep);
            showCells(i + 1, j - 1, deep);
            showCells(i + 1, j + 1, deep);
        }
    }

    public synchronized Cell[][] getBoard() {
        return board;
    }

    public synchronized void markCell(int i, int j) {
        validateMove(i, j);
        Cell cell = board[i][j];
        if(!cell.isHide()){
            throw new IllegalArgumentException("La celda (" + i + ", " + j + ") ya esta destapada, no se puede marcar");
        }
        cell.setMarked(!cell.isMarked());
    }
}
