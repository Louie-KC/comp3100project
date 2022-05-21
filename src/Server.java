import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

public class Server {
    private String type;
    private int id;
    private int coreCount;
    private int memory;
    private int disk;

    private List<Job> runningJobs;  // Assumed
    private List<Job> queuedJobs;  // Assumed

    private Server originalStats;

    public Server(String resourceString) {
        // Server format: serverType serverID state curStartTime core memory disk
        String[] data = resourceString.split(" ");
        type = data[0];
        id = Integer.valueOf(data[1]);
        coreCount = Integer.valueOf(data[4]);
        memory = Integer.valueOf(data[5]);
        disk = Integer.valueOf(data[6]);
        originalStats = new Server(this);

        runningJobs = new ArrayList<>();
        queuedJobs = new LinkedList<>();
    }

    private Server(Server inServer) {
        coreCount = inServer.coreCount;
        memory = inServer.memory;
        disk = inServer.disk;
    }

    /**
     * Gets the servers type
     * @return server type
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the servers core count
     * @return server core count
     */
    public int getCoreCount() {
        return coreCount;
    }

    public int getMemory() {
        return memory;
    }

    public int getDisk() {
        return disk;
    }

    /**
     * Gets the name/info needed for specifying this exact server for things
     * such as scheduling jobs: "type id"
     * @return String used for tasks such as scheduling.
     */
    public String getName() {
        return type + " " + id;
    }

    /**
     * Checks if a server can immediately run/start a job.
     * @param jobToCheck
     * @return true if it can run immediately, false otherwise.
     */
    public boolean canRunJob(Job jobToCheck) {
        if (jobToCheck.getCPUReq() > getCoreCount()) { return false; }
        if (jobToCheck.getMemReq() > getMemory()) { return false; }
        if (jobToCheck.getDiskReq() > getDisk()) { return false; }
        return true;
    }

    /**
     * Checks if a server can at some point run a job.
     * @param jobToCheck
     * @return true if it can eventually run, false otherwise.
     */
    public boolean canEventuallyRunJob(Job jobToCheck) {
        if (jobToCheck.getCPUReq() > originalStats.getCoreCount()) { return false; }
        if (jobToCheck.getMemReq() > originalStats.getMemory()) { return false; }
        if (jobToCheck.getDiskReq() > originalStats.getDisk()) { return false; }
        return true;
    }
}
