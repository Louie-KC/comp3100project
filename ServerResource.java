public class ServerResource {
    private String type;
    private int id;
    // private String state;
    // private int curStartTime;
    private int coreCount;
    // private int memory;
    // private int diskSpace;
    // private int wJobs;
    // private int rJobs;

    public ServerResource(String resourceString) {
        String[] data = resourceString.split(" ");
        type = data[0];
        id = Integer.valueOf(data[1]);
        // state = data[2];
        // curStartTime = Integer.valueOf(data[3]);
        coreCount = Integer.valueOf(data[4]);
        // memory = Integer.valueOf(data[5]);
        // diskSpace = Integer.valueOf(data[6]);
        // wJobs = Integer.valueOf(data[7]);
        // rJobs = Integer.valueOf(data[8]);

        /**
         * Format: "serverType serverID state curStartTime core memory disk #wJobs #rJobs"
         * Reference:
         * joon 0 inactive -1 4 16000 64000 0 0
         * joon 1 inactive -1 4 16000 64000 0 0
         * super-silk 0 inactive -1 16 64000 512000 0 0
         */
    }

    public String getServerType() {
        return type;
    }

    public int getCoreCount() {
        return coreCount;
    }

    public String getName() {
        return type + " " + id;
    }
}


