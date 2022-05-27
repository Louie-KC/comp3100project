public class Server {
    private String type;
    private int id;
    private int coreCount;
    private int memory;
    private int disk;
    private int wJobs;

    // Constructor
    /**
     * Creates a Server instances. <p>
     * resourceString format: serverType serverID state curStartTime core memory disk #wJobs #rJobs
     * @param resourceString
     */
    public Server(String resourceString) {
        String[] data = resourceString.split(" ");
        type = data[0];
        id = Integer.valueOf(data[1]);
        coreCount = Integer.valueOf(data[4]);
        memory = Integer.valueOf(data[5]);
        disk = Integer.valueOf(data[6]);
        wJobs = Integer.valueOf(data[7]);
    }

    // Getter methods
    public String getType() {
        return type;
    }

    public int getID() {
        return id;
    }

    public int getCoreCount() {
        return coreCount;
    }

    public int getMemory() {
        return memory;
    }

    public int getDisk() {
        return disk;
    }

    public int getWJobs() {
        return wJobs;
    }

    /**
     * Gets the name/info needed for specifying this exact server for things
     * such as scheduling jobs: "type id"
     * @return String used for tasks such as scheduling.
     */
    public String getName() {
        return type + " " + id;
    }
    
}
