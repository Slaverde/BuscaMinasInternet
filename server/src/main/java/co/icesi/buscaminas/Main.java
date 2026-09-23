package co.icesi.buscaminas;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Scanner;

import co.icesi.buscaminas.controllers.TCPController;
import co.icesi.buscaminas.model.BoardGame;
import co.icesi.buscaminas.services.ServicesImpl;

public class Main {

    public static void main(String[] args)
    {
        // Uso: java co.icesi.buscaminas.Main [puerto]   (por defecto 12345)
        int port = TCPController.DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port '" + args[0] + "', using " + port);
            }
        }

        ServicesImpl serv = new ServicesImpl();
        new Thread(() -> apply(serv.getGame())).start();
        // TCPController controller = new TCPController(serv);
        // controller.startService();

        TCPController iceController = new TCPController(serv, port);
        printLanAddresses(port);
        iceController.startService();
    }

    // Basado en obtenerIPLocal() del Servidor original: muestra a que IP deben
    // conectarse los companeros del salon. Se listan todas las interfaces activas
    // porque en Windows la primera suele ser un adaptador virtual (WSL, VirtualBox).
    static void printLanAddresses(int port) {
        System.out.println("Tus companeros pueden conectarse con alguna de estas IPs:");
        boolean found = false;
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (ni.isLoopback() || !ni.isUp()) continue;
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address) {
                        System.out.println("  " + addr.getHostAddress() + "  (" + ni.getDisplayName() + ")"
                                + "  ->  ./gradlew :client:run --args=\"" + addr.getHostAddress() + " " + port + "\"");
                        found = true;
                    }
                }
            }
        } catch (SocketException e) {
            // si falla, se usa localhost como respaldo
        }
        if (!found) {
            System.out.println("  localhost (no se encontraron interfaces de red activas)");
        }
    }
    public static void apply(BoardGame bg) {

        int n = bg.getBoard().length;
        int m = bg.getBoard()[0].length;
        System.out.println("LandMines on the table: "+ bg.getMines());
//        bg.showAll(true);
//        bg.printBoard();
//        bg.showAll(false);
        bg.printBoard();
        Scanner scanner = new Scanner(System.in);
        System.out.println("select a cell (i,j) between 0 and "+(n-1)+","+(m-1)+" to play, or (-1,-1) to exit");
        System.out.println("you have "+bg.getMines()+" mines to avoid");
        System.out.println("use the format: <operation> <i> <j>");
        System.out.println("operation 1: select cell, operation 2: mark/unmark cell");
        int operation = scanner.nextInt();
        int i = scanner.nextInt();
        int j = scanner.nextInt();
        do{
            try {
                if(operation==2){
                    bg.markCell(i,j);
                }else if(operation ==1){
                    boolean r = bg.selectCell(i, j);
                    if (r) {
                        System.out.println("you win, Congratulations");
                        break;
                    }
                }
                bg.printBoard();
            }catch (RuntimeException e){
                System.out.println(e.getMessage());
                break;
            }
            operation = scanner.nextInt();
            i = scanner.nextInt();
            j = scanner.nextInt();
        }while (i>=0 && j>=0);
        bg.showAll(true);
        bg.printBoard();
        System.out.println("exit");
        scanner.close();

    }
}