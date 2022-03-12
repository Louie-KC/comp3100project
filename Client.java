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
            sendMessage("REDY", outStream);  // Step 5
            String job1 = getInMsgString(inStream);
            System.out.println(job1);
            sendMessage("GETS", outStream);  // Needs more data to be sent
            System.out.println(getInMsgString(inStream));
            System.out.println("Sending: QUIT");
            sendMessage("QUIT", outStream);
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
        sendMessage("HELO", outputStream);  // Step 1
        if (!checkInMessage("OK", inputStream)) { return; }  // Step 2
        sendMessage("AUTHlouie", outputStream);  // Step 3
        if (!checkInMessage("OK", inputStream)) { return; }  // Step 4
        System.out.println("DS-Sim greet successful.");
    }

    /**
     * Reads string from input stream. Necessary as DS-Sim ds-server does not add a null 
     * terminating character nor an EOT character expected by DataInputStream.
     * @param inputStream
     * @return Message sent by ds-server as String
     * @throws IOException
     */
    private static String getInMsgString(DataInputStream inputStream) {
        byte[] readBuffer = new byte[128];
        try {
            inputStream.read(readBuffer);
        } catch (Exception e) {
            System.err.println("getInMsgString IOException: could not read stream into buffer");
            return "";
        }
        return new String(readBuffer).trim();
    }

    /**
     * Check for server response when response is known, such as in the initial greet sequence
     * where server should respond "OK".
     * @param expectedString
     * @param inputStream
     * @return True if received message is as expected, false otherwise.
     */
    private static Boolean checkInMessage(String expectedString, DataInputStream inputStream) {
        String received = getInMsgString(inputStream);
        if (received.equals(expectedString)) { return true; }
        System.err.println("checkInMessage: got '"+received+"' expected '"+expectedString+"'.");
        System.out.println("received length: " + received.length());
        return false;
    }

    /**
     * Send message String via DataOutputStream paramater, handles conversion of String to bytes
     * without any terminating characters to match ds-server sim expectation.
     * @param msgToSend
     * @param outputStream
     */
    private static void sendMessage(String msgToSend, DataOutputStream outputStream) {
        try {
            outputStream.write((msgToSend).getBytes("UTF-8"));
        } catch (IOException e) {
            System.err.println("sendMessage: IOException - " + e.getMessage());
        }
    }
} 
