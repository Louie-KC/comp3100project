import java.net.Socket;
import java.net.UnknownHostException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

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

            // One/test round
            sendMessage("REDY", outStream);  // Step 5
            String job1 = getInMsgString(inStream);  // Step 6, receiving job details
            sendMessage("GETSCapable" + job1.substring(13), outStream);  // Step 7, getting capable systems
            getInMsgString(inStream);  // Step 8, getting DATA/preparation message
            sendMessage("OK", outStream);  // Step 9, Sending OK for DATA we got preparation msg for in s8.
            String[] resources = parseCapability(getInMsgString(inStream));  // Step 10, in this case receive resource information
            ArrayList<ServerResource> topCPU = getTopCPUList(resources);
            sendMessage("OK", outStream);  // Step 11, send OK and go back to step 10
            getInMsgString(inStream);  // Step 10 again, get "." i.e. no more info return to step 7.
            sendMessage("SCHD0  "+topCPU.get(0).getName(), outStream);  // Step 7: Schedule first job (idx 0) to first resource in received resource info.

            checkInMessage("OK", inStream);  // Step 8: Check for OK scheduling confirmation from server.
            // Unsure of what step I am in below here, believe I should be at step 9 however
            // Am not receiving any data from the server.
            sendMessage("OK", outStream);  // Step 9: OK to get DS-server info/data (hopefully about jobs on server)
            getInMsgString(inStream);  // Step 10: hopefully info about jobs on server

            // -------------------------------------------------------------------------- //


            // sendMessage("REDY", outStream);
            // String step6 = getInMsgString(inStream);
            // while (step6 != "NONE") {
            //     if (step6.substring(0, 4).equals("JOBN")) {

                    
            //     }
                
            //     // Need to ensure return to step 5 from step 8 
            //     sendMessage("REDY", outStream);
            //     step6 = getInMsgString(inStream);
            // }

            // Gracefully close connection
            sendMessage("QUIT", outStream);
            getInMsgString(inStream);
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
        String output = new String(readBuffer).trim();
        System.out.println("Received: " + output);
        return output;
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
            System.out.println("\nSending: " + msgToSend);
        } catch (IOException e) {
            System.err.println("sendMessage: IOException - " + e.getMessage());
        }
    }

    /**
     * Parses servers GETSCapable response into an array of resource/capability Strings. 
     * @param capableResponse
     * @return String array of server capabilities/resources.
     */
    private static String[] parseCapability(String capableResponse) {
        // String[] capable = capableResponse.split("\\R");
        // System.out.println("parseCapabiltiy: Printing split string.");
        // for (String s : capable) { System.out.println(s); }
        // return capable;
        return capableResponse.split("\\R");

        /**
         * Reference response to parse:
         * joon 0 inactive -1 4 16000 64000 0 0
         * joon 1 inactive -1 4 16000 64000 0 0
         * super-silk 0 inactive -1 16 64000 512000 0 0
         */
    }

    /**
     * Provides list of server resources with highest CPU core count.
     * @param inResources
     * @return ArrList of resources with highest CPU core count.
     */
    private static ArrayList<ServerResource> getTopCPUList(String[] inResources) {
        ArrayList<ServerResource> resources = new ArrayList<>();
        int topCoreCount = 0;
        for (String s : inResources) {
            ServerResource sr = new ServerResource(s);
            if (sr.getCoreCount() > topCoreCount) { topCoreCount = sr.getCoreCount(); }
            resources.add(sr);
        }
        ArrayList<ServerResource> topCPU = new ArrayList<>();
        for (ServerResource sr : resources) {
            if (sr.getCoreCount() == topCoreCount) { topCPU.add(sr); }
        }
        return topCPU;
    }

} 
