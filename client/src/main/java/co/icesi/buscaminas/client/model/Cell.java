package co.icesi.buscaminas.client.model;

/**
 * Copia del modelo Cell del servidor. Los nombres de los atributos deben coincidir
 * exactamente con los del servidor porque Gson los usa como llaves del JSON.
 */
public class Cell {

    private boolean isLandMine;
    private int value;
    private boolean hide;
    private boolean showAll;
    private boolean isMarked;

    public Cell() {
    }

    public boolean isLandMine() {
        return isLandMine;
    }

    public int getValue() {
        return value;
    }

    public boolean isHide() {
        return hide;
    }

    public boolean isShowAll() {
        return showAll;
    }

    public boolean isMarked() {
        return isMarked;
    }
}
