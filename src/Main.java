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
        // Arguments check, exit if bad launch args.
        if (argsCheck(args) == false) { return; }
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

    public static void printLaunchOptions() {
        System.out.println("Launch:  `java Main <options>`");
        System.out.println("Options: -ip (ip address), -p (port number)");
    }

    private static boolean argsCheck(String[] inArgs) {
        try {
            for (int i = 0; i < inArgs.length; i++) {
                if (!inArgs[i].equals("-ip") && !inArgs[i].equals("-p")) {
                    throw new Exception("Invalid argument: " + inArgs[i]);
                }
                if (inArgs[i].equals("-ip")) {
                    targetIP = inArgs[++i];
                }
                if (inArgs[i].equals("-p")) {
                    targetPort = Integer.valueOf(inArgs[++i]);
                    if (targetPort < 0 || targetPort > 65535) {
                        throw new Exception("Bad port number: " + inArgs[i]);
                    }
                }
            }
        } catch (NumberFormatException e) {
            System.err.print("NumberFormartException parsing port option. ");
            System.err.println(e.getMessage());
            printLaunchOptions();
            return false;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            printLaunchOptions();
            return false;
        }
        return true;
    }
}
