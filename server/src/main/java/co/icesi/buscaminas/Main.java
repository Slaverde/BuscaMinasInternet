package co.icesi.buscaminas;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

import co.icesi.buscaminas.controllers.TCPController;
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
}