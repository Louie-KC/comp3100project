import java.net.Socket;
import java.net.UnknownHostException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

            // Very rough dispatching to highest available CPU core resource.
            sendMessage("REDY", outStream);  // Step 5
            String step6 = readMessage(inStream);  // Step 6
            List<Job> jobList = new ArrayList<>();
            int numJobs = 0;
            while (!step6.equals("NONE")) {
                if (step6.substring(0, 4).equals("JOBN")) {  // Handle JOBN
                    jobList.add(new Job(step6));
                    sendMessage("GETSCapable " + jobList.get(numJobs).getCapableString(), outStream);
                    readMessage(inStream);  // Data preparation message, currently useless
                    sendMessage("OK", outStream);
                    ArrayList<String> resources2 = new ArrayList<>();
                    String resourceString = "";
                    String inResource = readMessage(inStream);
                    while (!inResource.equals(".") & inResource.length() > 2) {
                        if (resourceString.length() > 2) {
                            if (resourceString.charAt(resourceString.length()-1) != ' ') {
                                resourceString += " ";
                            }
                        }
                        resourceString += inResource;
                        sendMessage("OK", outStream);  // Request new set of resources, or "."
                        inResource = readMessage(inStream);
                    }
                    System.out.println("!! Parsing resources !!");
                    for (String res : parseCapability(resourceString)) {
                        System.out.println("Parsing one: " + res);
                        resources2.add(res);
                    }
                    ServerResource topCore = getTopCPU(resources2);
                    sendMessage("SCHD" + jobList.get(numJobs).getJobID() + " " + topCore.getName(), outStream);
                    if (!checkInMessage("OK", inStream)) { break; }

                    numJobs++;
                }
                if (step6.substring(0, 4).equals("JCPL")) {
                    System.out.println("Job completed: " + step6);
                }
                // Request new job/server update and start again
                sendMessage("REDY", outStream);
                step6 = readMessage(inStream);
            }

            // Gracefully close connection
            sendMessage("QUIT", outStream);
            readMessage(inStream);
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
    private static String readMessage(DataInputStream inputStream) {
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
        String received = readMessage(inputStream);
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
     * Returns the ServerResource with highest available CPU core count. If two or more have
     * the same highest CPU core count, return one which will lower the available cores for
     * next time the function is called.
     * @param inResources
     * @return ServerResource with highest available core count
     */
    private static ServerResource getTopCPU(ArrayList<String> inResources) {
        if (inResources == null | inResources.size() == 0) { return null; }
        ServerResource topSR = new ServerResource(inResources.get(0));
        for (int i = 1; i < inResources.size(); i++) {
            ServerResource temp = new ServerResource(inResources.get(i));
            if (temp.getCoreCount() > topSR.getCoreCount()) {
                topSR = temp;
            }
        }
        return topSR;
    }

} 
