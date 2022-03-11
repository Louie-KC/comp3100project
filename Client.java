import java.net.Socket;
import java.net.UnknownHostException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Client {
    static String targetIP;
    static int targetPort = 50000;  // default port per ds-sim user guide

    public static void main(String[] args) {
        Socket socket = null;
        DataInputStream inStream = null;
        DataOutputStream outStream = null;
        switch (args.length) {
            case 2:
                targetPort = Integer.valueOf(args[1]);
                // fall through
            case 1:
                targetIP = args[0];
                break;
            default:  // Close program if too few/many arguments.
                System.err.println("! Run executable with a target IP and/or target port !");
                System.out.println("command: java Client <IP> <Port (optional, default: 50000)>");
                return;
        }
        try {
            socket = new Socket(targetIP, targetPort);
            inStream = new DataInputStream(socket.getInputStream());
            outStream = new DataOutputStream(socket.getOutputStream());
            greetDS(inStream, outStream);
            outStream.write(("REDY").getBytes("UTF-8"));
            System.out.println(getInMsgString(inStream));
            outStream.write(("GETS").getBytes("UTF-8"));
            System.out.println(getInMsgString(inStream));
            outStream.write(("QUIT").getBytes("UTF-8"));
            System.out.println(getInMsgString(inStream));
        } catch (UnknownHostException e) {
            System.err.println("Socket: Unknown host " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Socket/inStream/OutStream: " + e.getMessage());
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    System.err.println("Socket closing: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Performs initial connection steps outlined in section 6.1 of ds-sim user guide.
     * @param inInputStream
     * @param inOutputStream
     */
    private static void greetDS(DataInputStream inputStream, DataOutputStream outputStream) {
        try {
            outputStream.write(("HELO").getBytes("UTF-8"));
            System.out.println(getInMsgString(inputStream));
            outputStream.write(("AUTHlouie").getBytes("UTF-8"));
            System.out.println(getInMsgString(inputStream));
            System.out.println("DS-Sim greet successful.");
        } catch (IOException e) {
            System.err.println("greetDS: " + e.getMessage());
        }
    }

    /**
     * Reads string from input stream. Necessary as DS-Sim ds-server does not add a null 
     * terminating character nor an EOT character expected by DataInputStream.
     * @param inputStream
     * @return Message sent by ds-server as String
     * @throws IOException
     */
    private static String getInMsgString(DataInputStream inputStream) throws IOException {
        byte[] readBuffer = new byte[128];
        inputStream.read(readBuffer);
        String output = new String(readBuffer);
        return output;
    }
}
