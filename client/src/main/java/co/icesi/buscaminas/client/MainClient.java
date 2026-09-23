package co.icesi.buscaminas.client;

import java.io.IOException;
import java.util.Scanner;

import co.icesi.buscaminas.client.dtos.Request;
import co.icesi.buscaminas.client.dtos.Response;

/**
 * Cliente interactivo por consola del Buscaminas distribuido.
 * Uso: java co.icesi.buscaminas.client.MainClient [host] [puerto]
 * Si no se pasan argumentos, se piden por consola (Enter = localhost / 12345).
 */
public class MainClient {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 12345;

    private final BuscaminasTCPClient client;
    private final Scanner scanner;

    public MainClient(BuscaminasTCPClient client, Scanner scanner) {
        this.client = client;
        this.scanner = scanner;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String host = args.length > 0 ? args[0] : null;
        Integer port = null;
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Puerto invalido '" + args[1] + "'");
            }
        }
        BuscaminasTCPClient client = connect(scanner, host, port);
        new MainClient(client, scanner).run();
    }

    /**
     * Pide IP y puerto (si no llegaron por argumentos) y verifica que el servidor
     * responda con un GET_BOARD antes de mostrar el menu. Si falla, vuelve a preguntar.
     */
    private static BuscaminasTCPClient connect(Scanner scanner, String host, Integer port) {
        while (true) {
            if (host == null) {
                String line = readLine(scanner, "IP del servidor (Enter = " + DEFAULT_HOST + "): ");
                host = line.isEmpty() ? DEFAULT_HOST : line;
            }
            while (port == null) {
                String line = readLine(scanner, "Puerto (Enter = " + DEFAULT_PORT + "): ");
                try {
                    port = line.isEmpty() ? DEFAULT_PORT : Integer.parseInt(line);
                } catch (NumberFormatException e) {
                    System.out.println("Ingrese un numero entero.");
                }
            }
            BuscaminasTCPClient client = new BuscaminasTCPClient(host, port);
            System.out.println("Conectando a " + host + ":" + port + "...");
            try {
                client.send(new Request("GET_BOARD"));
                System.out.println(BoardRenderer.GREEN + "Conectado al servidor " + host + ":" + port + BoardRenderer.RESET);
                return client;
            } catch (IOException e) {
                System.out.println(BoardRenderer.RED + "No se pudo conectar a " + host + ":" + port
                        + " -> " + e.getMessage() + BoardRenderer.RESET);
                System.out.println("Verifique que el servidor este encendido, la IP y el puerto, y que esten en la misma red.");
                host = null;
                port = null;
            }
        }
    }

    private static String readLine(Scanner scanner, String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            System.out.println();
            System.exit(0);
        }
        return scanner.nextLine().trim();
    }

    public void run() {
        client.setPlayer(readLine(scanner, "Tu nombre de jugador (Enter para usar tu IP): "));
        try {
            System.out.println("Tablero actual del servidor:");
            show(client.send(new Request("GET_BOARD")));
        } catch (IOException e) {
            System.out.println(BoardRenderer.RED + "No se pudo obtener el tablero: " + e.getMessage() + BoardRenderer.RESET);
        }
        while (true) {
            printMenu();
            int option = readInt("Seleccione una opcion: ");
            try {
                switch (option) {
                    case 1:
                        initGame();
                        break;
                    case 2:
                        selectCell();
                        break;
                    case 3:
                        markCell();
                        break;
                    case 4:
                        show(client.send(new Request("GET_BOARD")));
                        break;
                    case 5:
                        System.out.println("Te rendiste. Tablero completo:");
                        show(client.send(new Request("SOW_ALL")));
                        break;
                    case 6:
                        System.out.println("Hasta luego.");
                        return;
                    default:
                        System.out.println("Opcion no valida, elija un numero entre 1 y 6.");
                }
            } catch (IOException e) {
                System.out.println(BoardRenderer.RED + "No se pudo comunicar con el servidor "
                        + client.getHost() + ":" + client.getPort() + " -> " + e.getMessage() + BoardRenderer.RESET);
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("=============================================");
        System.out.println("     BUSCAMINAS DISTRIBUIDO - CLIENTE TCP");
        System.out.println("=============================================");
        System.out.println("[1] Iniciar nueva partida (Filas, Columnas, Minas)");
        System.out.println("[2] Destapar celda (Fila, Columna)");
        System.out.println("[3] Marcar / Desmarcar bandera (Fila, Columna)");
        System.out.println("[4] Consultar estado actual del tablero");
        System.out.println("[5] Rendirse y revelar tablero completo");
        System.out.println("[6] Salir");
    }

    private void initGame() throws IOException {
        int n = readInt("Filas: ");
        int m = readInt("Columnas: ");
        int mines = readInt("Minas: ");
        Response response = client.send(new Request("INIT_GAME").with("n", n).with("m", m).with("minas", mines));
        if (response.isOk()) {
            System.out.println("Nueva partida de " + n + "x" + m + " con " + mines + " minas.");
        }
        show(response);
    }

    private void selectCell() throws IOException {
        int i = readInt("Fila: ");
        int j = readInt("Columna: ");
        Response response = client.send(new Request("SELECT_CELL").with("i", i).with("j", j));
        if (!response.isOk() || !response.isGameEnd()) {
            show(response);
            return;
        }
        if (response.isWin()) {
            System.out.println(BoardRenderer.render(response.board()));
            System.out.println(BoardRenderer.GREEN + BoardRenderer.BOLD
                    + "*** FELICITACIONES! Destapaste todas las celdas sin minas. GANASTE! ***"
                    + BoardRenderer.RESET);
        } else {
            System.out.println(BoardRenderer.RED + BoardRenderer.BOLD
                    + "*** BOOM! Pisaste una mina en (" + i + ", " + j + "). PERDISTE ***" + BoardRenderer.RESET);
            System.out.println("Tablero revelado:");
            show(client.send(new Request("SOW_ALL")));
        }
    }

    private void markCell() throws IOException {
        int i = readInt("Fila: ");
        int j = readInt("Columna: ");
        show(client.send(new Request("MARK_CELL").with("i", i).with("j", j)));
    }

    private void show(Response response) {
        if (!response.isOk()) {
            System.out.println(BoardRenderer.RED + "Error del servidor: " + response.message() + BoardRenderer.RESET);
        } else if (response.message() != null) {
            System.out.println(response.message());
        }
        if (response.board() != null) {
            System.out.println(BoardRenderer.render(response.board()));
        }
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                System.out.println();
                System.exit(0);
            }
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Ingrese un numero entero.");
            }
        }
    }
}
