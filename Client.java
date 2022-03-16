import java.net.Socket;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Client {
    static String targetIP;
    static int targetPort = 50000;  // default port per ds-sim user guide

    public static void main(String[] args) {
        Socket socket = null;
        BufferedReader buffReader = null;
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
            buffReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outStream = new DataOutputStream(socket.getOutputStream());
            greetDS(buffReader, outStream);

            // Very rough dispatching to highest available CPU core resource.
            sendMessage("REDY", outStream);  // Step 5
            String step6 = readMessage(buffReader);  // Step 6
            List<Job> jobList = new ArrayList<>();
            int numJobs = 0;
            while (!step6.equals("NONE")) {
                if (step6.substring(0, 4).equals("JOBN")) {  // Handle JOBN
                    jobList.add(new Job(step6));
                    sendMessage("GETSCapable " + jobList.get(numJobs).getCapableString(), outStream);
                    // Data preparation: DATA nRecs recLen
                    String[] dataPrep = readMessage(buffReader).split(" ");
                    sendMessage("OK", outStream);
                    ArrayList<String> resources = new ArrayList<>();
                    for (int i = 0; i < Integer.valueOf(dataPrep[1]); i++) {
                        resources.add(readMessage(buffReader));
                    }
                    sendMessage("OK", outStream);
                    if (!checkInMessage(".", buffReader)) {
                        System.err.println("Did not get '.' after resources");
                        break;
                    }
                    ServerResource topCore = getTopCPU(resources);
                    sendMessage("SCHD " + jobList.get(numJobs).getJobID() + " " + topCore.getName(), outStream);
                    if (!checkInMessage("OK", buffReader)) { break; }

                    numJobs++;
                }
                if (step6.substring(0, 4).equals("JCPL")) {  // Handle JCPL, currently do nothing
                    System.out.println("Job completed: " + step6);
                }
                // Request new job/server update and start again
                sendMessage("REDY", outStream);
                // step6 = readMessage(inStream);
                step6 = readMessage(buffReader);
            }

            // Gracefully close connection
            sendMessage("QUIT", outStream);
            readMessage(buffReader);
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
    private static void greetDS(BufferedReader inputStream, DataOutputStream outputStream) {
        sendMessage("HELO", outputStream);  // Step 1
        if (!checkInMessage("OK", inputStream)) { return; }  // Step 2
        sendMessage("AUTH " + System.getProperty("user.name"), outputStream);  // Step 3
        if (!checkInMessage("OK", inputStream)) { return; }  // Step 4
        System.out.println("DS-Sim greet successful.");
    }

    /**
     * Reads string from input stream. Necessary as DS-Sim ds-server does not add a null 
     * terminating character nor an EOT character expected by DataInputStream.
     * <p>
     * Expects "\n" at the end of each line. Launch ds-server with option -n.
     * @param inputStream
     * @return Message sent by ds-server as String
     * @throws IOException
     */
    private static String readMessage(BufferedReader inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("DataInputStream is null");
        }
        try {
            String received = inputStream.readLine();
            System.out.println("Received: " + received);
            return received;
        } catch (Exception e) {
            System.err.println("readMessage IOException: could not read stream into buffer");
            return "";
        }
    }

    /**
     * Check for server response when response is known, such as in the initial greet sequence
     * where server should respond "OK".
     * @param expectedString
     * @param inputStream
     * @return True if received message is as expected, false otherwise.
     */
    private static Boolean checkInMessage(String expectedString, BufferedReader inputStream) {
        String received = readMessage(inputStream);
        if (received.equals(expectedString)) { return true; }
        System.err.println("checkInMessage: expected '" + expectedString + "'");
        System.out.println("received length: " + received.length());
        return false;
    }

    /**
     * Send message String via DataOutputStream paramater, handles conversion of String to bytes
     * without any terminating characters to match ds-server sim expectation.
     * <p>
     * Adds \n to end of message to indicate end of line.
     * @param msgToSend
     * @param outputStream
     */
    private static void sendMessage(String msgToSend, DataOutputStream outputStream) {
        try {
            // msgToSend += "\n";
            System.out.println("\nSending: " + msgToSend);
            outputStream.write((msgToSend + "\n").getBytes("UTF-8"));
        } catch (IOException e) {
            System.err.println("sendMessage: IOException - " + e.getMessage());
        }
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
