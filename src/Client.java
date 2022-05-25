import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Client {
    
    private BufferedReader inStream;
    private DataOutputStream outStream;

    private String lastMsgRCVD;
    private boolean connected;
    private int lastKnownTime;

    private State curState;
    private List<Job> jobList;
    private List<Server> serverList;
    boolean serversFiltered;

    public Client(BufferedReader inputStream, DataOutputStream outputStream) {
        inStream = inputStream;
        outStream = outputStream;
        lastMsgRCVD = "";
        connected = true;
        lastKnownTime = 0;
        curState = new InitialState();
        jobList = new LinkedList<>();  // No rewriting entire list when removing from or growing list
        serverList = new ArrayList<>();  // ArrList as Servers do not change. May be filtered only once.
        serversFiltered = false;
    }

    /**
     * Run/Start the client.
     */
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
     * Job list getter.
     * @return list of jobs stored by client.
     */
    public List<Job> getJobs() {
        return jobList;
    }

    /**
     * Server list getter.
     * @return list of servers stored by client
     */
    public List<Server> getServers() {
        return serverList;
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

    public int getKnownTime() {
        return lastKnownTime;
    }

    /**
     * Update last known time with data from the last message if it's a JOBN or JCPL message.
     * Relies on the format of JOBN and JCPL to mention the time in their second white space
     * separated value.
     * <p>
     * Message formats for reference:
     * <ul>
     * <li> JOBN submitTime jobID estRuntime core memory disk </li>
     * <li> JCPL endTime jobID serverType serverID </li>
     * </ul>
     * Assumes that submitTime and endTime are the current time with the message.
     */
    public void updateKnownTime() {
        if (getLastMsg().contains("JOBN") || getLastMsg().contains("JCPL")) {
            lastKnownTime = Integer.valueOf(getLastMsg().split(" ")[1]);
        }
    }

    public List<Server> sendGets(String getsOption, Job job) {
        String gets = "GETS ";
        if (!getsOption.equals("All") && job != null) {
            gets = gets + getsOption + " " + job.getQueryString();
        } else {
            gets = gets + getsOption;
        }
        sendMessage(gets);
        readMessage(); // DATA nRecs recLen
        int nRecs = Integer.valueOf(getLastMsg().split(" ")[1]);
        sendMessage("OK");
        List<Server> result = new ArrayList<>();
        for (int i = 0; i < nRecs; i++) {
            readMessage();
            result.add(new Server(getLastMsg()));
        }
        if (result.size() != 0) { sendMessage("OK"); }
        readMessage();
        return result;
    }
}
