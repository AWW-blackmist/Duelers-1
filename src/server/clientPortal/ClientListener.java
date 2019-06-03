package server.clientPortal;

import server.Server;

import java.net.Socket;
import java.util.Formatter;
import java.util.Scanner;

public class ClientListener extends Thread {
    private Socket socket;

    public ClientListener(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        Scanner scanner = null;
        Formatter formatter = null;
        try {
            scanner = new Scanner(socket.getInputStream());
            formatter = new Formatter(socket.getOutputStream());
            formatter.format("#Listening#\n");
            formatter.flush();
            String name;
            while (true) {
                name = scanner.nextLine().split("#")[1];
                if (name.length() >= 3 && !ClientPortal.getInstance().hasThisClient(name)) {
                    ClientPortal.getInstance().addClient(name, formatter);
                    formatter.format("#Valid#\n");
                    formatter.flush();
                    break;
                } else {
                    formatter.format("#InValid#\n");
                    formatter.flush();
                }
            }
            while (true) {
                String message = scanner.nextLine();
                ClientPortal.getInstance().addMessage(name, message);
            }
        } catch (Exception e) {
            Server.getInstance().serverPrint("Error ClientListener!");
        }
    }
}