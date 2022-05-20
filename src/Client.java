import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Client {
    
    BufferedReader inStream;
    DataOutputStream outStream;

    private String lastMsgRCVD;
    private boolean connected;
    private State curState;

    List<Job> jobList;
    List<Server> serverList;
    boolean serversFiltered;

    public Client(BufferedReader inputStream, DataOutputStream outputStream) {
        inStream = inputStream;
        outStream = outputStream;
        lastMsgRCVD = "";
        connected = true;
        curState = new InitialState();
        jobList = new LinkedList<>();
        serverList = new ArrayList<>();
        serversFiltered = false;
    }

    public void run() {
        while (connected) {
            curState.action(this);
        }
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
    public void readMessage() {
        if (inStream == null) {
            throw new IllegalArgumentException("Client inStream is null");
        }
        try {
            lastMsgRCVD = inStream.readLine();
        } catch (Exception e) {
            System.err.println("readMessage IOException: could not read stream into buffer");
        }
    }

    /**
     * Send message String via DataOutputStream paramater, handles conversion of String to bytes
     * without any terminating characters to match ds-server sim expectation.
     * <p>
     * Adds \n to end of message to indicate end of line.
     * @param msgToSend
     * @param outputStream
     */
    public void sendMessage(String msgToSend) {
        try {
            outStream.write((msgToSend + "\n").getBytes("UTF-8"));
        } catch (IOException e) {
            System.err.println("sendMessage: IOException - " + e.getMessage());
        }
    }

    /**
     * Creates and adds job to the jobList if the last message received contains job details.
     */
    public void addJob() {
        if (!lastMsgRCVD.contains("JOBN")) { return; }
        jobList.add(new Job(getLastMsg()));
    }

    /**
     * Creates and add server resource to server list if the last message received contains
     * server details AND the the clients state is appropriately the GetServerState.
     */
    public void addServer() {
        if (lastMsgRCVD.isEmpty() && curState instanceof GetServersState == false) {
            System.err.println("Client addServer: Attempting to add server in incorrect state");
            return;
        }
        serverList.add(new Server(getLastMsg()));
    }

    /**
     * Getter method for retrieving the last received message.
     * @return lastMessage
     */
    public String getLastMsg() {
        return lastMsgRCVD;
    }

    /**
     * USED ONLY DURING GetServersState!
     * <p>
     * Sets the clients last received message to the parameter message. Only here to
     * ensure that the transition from GetServerState to SchedulingState goes smoothly
     * without a doubling up of the first job.
     * @param message What to set lastReceivedMsg to
     */
    public void setLastMsg(String message) {
        if (curState instanceof GetServersState) {
            lastMsgRCVD = message;
        } else {
            System.err.println("setLastMsg: Attempt to rewrite lastMsg in incorrect state.");
        }
    }

    /**
     * USED ONLY DURING GetServerState!
     * <p>
     * Removes first job in joblist. Only here to ensure transition from GetServerState to
     * SchedulingState goes smoothly without doubling up of the first job.
     */
    public void removeFirstJob() {
        if (curState instanceof GetServersState) {
            jobList.remove(0);
        } else {
            System.err.println("removeFirstJob: Attempted to run in incorrect state");
        }
    }

    /**
     * Updates the Clients current state.
     * @param newState The state to transition to.
     */
    public void changeState(State newState) {
        curState = newState;
    }

    /**
     * Break the Client running loop for error or scheduling completed.
     */
    public void setDisconnect() {
        connected = false;
    }
}
