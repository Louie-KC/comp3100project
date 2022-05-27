import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Client {
    
    private BufferedReader inStream;
    private DataOutputStream outStream;

    private String lastMsgRCVD;
    private boolean connected;
    private State curState;
    private Job lastJob;
    private int jobCount;
    private List<Server> serverList;
    private boolean serversFiltered;

    // Constructor
    public Client(BufferedReader inputStream, DataOutputStream outputStream) {
        inStream = inputStream;
        outStream = outputStream;
        lastMsgRCVD = "";
        connected = true;
        curState = new InitialState();
        jobCount = 0;
        serverList = new ArrayList<>();  // ArrList as Servers do not change. May be filtered once.
        serversFiltered = false;
    }

    // Methods
    /**
     * Run/Start the client.
     */
    public void run() {
        while (connected) {
            curState.action(this);
        }
    }

    // Getters and setters
    public String getLastMsg() {
        return lastMsgRCVD;
    }

    public Job getLastJob() {
        return lastJob;
    }

    public int getJobCount() {
        return jobCount;
    }

    public List<Server> getServers() {
        return serverList;
    }

    public boolean getServerFilterStatus() {
        return serversFiltered;
    }

    public void setDisconnect() {
        connected = false;
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

    // All other methods
    /**
     * Reads string from input stream. Necessary as DS-Sim ds-server does not add a null 
     * terminating character nor an EOT character expected by DataInputStream.
     * <p>
     * Expects "\n" at the end of each line. Launch ds-server with option -n.
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
     */
    public void sendMessage(String msgToSend) {
        try {
            outStream.write((msgToSend + "\n").getBytes("UTF-8"));
        } catch (IOException e) {
            System.err.println("sendMessage: IOException - " + e.getMessage());
        }
    }

    /**
     * Checks the last received message and updates the last received job if appropriate.
     */
    public void checkLastMsgForJob() {
        if (getLastMsg().substring(0, 4).equals("JOBN")) {
            lastJob = new Job(getLastMsg());
            jobCount++;
        } else {
            System.err.println("checkLastMsgForJob: Last received message not a JOBN message");
            System.out.println("lastMessage: " + getLastMsg());
        }
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
     * Updates the Clients current state.
     * @param newState The state to transition to.
     */
    public void changeState(State newState) {
        curState = newState;
    }
    
}
