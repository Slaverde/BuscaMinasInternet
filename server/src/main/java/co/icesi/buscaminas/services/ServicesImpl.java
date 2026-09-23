package co.icesi.buscaminas.services;

import co.icesi.buscaminas.model.BoardGame;
import co.icesi.buscaminas.model.Cell;
public class ServicesImpl{
    private  BoardGame game;

    // Reemplaza el broadcast del Servidor original: como cada peticion usa su propia
    // conexion, en vez de empujar el aviso a todos se guarda la ultima jugada y se
    // devuelve en el "message" de las respuestas para que los demas jugadores la vean.
    private volatile String lastEvent = "Aun no hay jugadas en esta partida";

    public ServicesImpl(){
        game = new BoardGame();
        game.initGame(8, 8, 10);
    }

    public BoardGame getGame() {
        return game;
    }
    public int initGame(int n, int m, int mines) {
        return game.initGame(n, m, mines);
    }
    
    public boolean selectCell(int i, int j) {
        
        return game.selectCell(i, j);
    }

    public Cell[][] markCell(int i, int j) {
        game.markCell(i, j);
        return game.getBoard();
    }

    public void showAll(boolean show) {
        
        game.showAll(show);
    }

    public void registerEvent(String event) {
        lastEvent = event;
    }

    public String getLastEvent() {
        return lastEvent;
    }

    public Cell[][] printBoard() {
        game.printBoard();
        Cell [][] cells = game.getBoard();

        return cells;
    }
    
}
