import java.net.Socket;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    static String targetIP = "127.0.0.1";  // Need to set default IP as localhost for demo
    static int targetPort = 50000;  // default port per ds-sim user guide

    static Socket socket;
    static Client client;

    public static void main(String[] args) {
        // Arguments check
        switch (args.length) {
            case 2:
                targetPort = Integer.valueOf(args[1]);
                // fall through
            case 1:
                targetIP = args[0];
                break;
            case 0:
                break;
            default:  // Close program if too few/many arguments.
                System.err.println("! Run executable with a target IP and/or target port !");
                System.out.println("command: java Client <IP (optional, default 127.0.0.1)> <Port (optional, default: 50000)>");
                return;
        }
        // Create socket and client, start client.
        try {
            socket = new Socket(targetIP, targetPort);
            client = new Client(new BufferedReader(new InputStreamReader(socket.getInputStream())),
                                new DataOutputStream(socket.getOutputStream()));
            client.run();
        } catch (UnknownHostException e) {
            System.err.println("Socket: Unknown host " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Socket/inStream/OutStream: " + e.getMessage());
        }
        
        // Closing socket
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
                System.err.println("Socket closing: " + e.getMessage());
            }
        }
    }
}
