public class Job {
    private int jobID;
    private int estRuntime;
    private int CPURequirement;
    private int RAMRequirement;
    private int diskSpaceRequirement;

    private int estCompleteTime;

    public Job(String jobString) {
        // JOBN format: JOBN submitTime jobID estRuntime core memory disk
        String[] data = jobString.split(" ");
        jobID = Integer.valueOf(data[2]);
        estRuntime = Integer.valueOf(data[3]);
        CPURequirement = Integer.valueOf(data[4]);
        RAMRequirement = Integer.valueOf(data[5]);
        diskSpaceRequirement = Integer.valueOf(data[6]);

        estCompleteTime = -1;
    }

    public int getJobID() {
        return jobID;
    }

    public int getCPUReq() {
        return CPURequirement;
    }

    public int getMemReq() {
        return RAMRequirement;
    }

    public int getDiskReq() {
        return diskSpaceRequirement;
    }

    /**
     * Used for querying DS-SIM server about job capabilities.
     * @return String used after "GETS " to find server capability.
     */
    public String getQueryString() {
        String result = CPURequirement + " " + RAMRequirement + " " + diskSpaceRequirement;
        return result;
    }

    /**
     * Used for stage 2 algorthim.
     * <p>
     * Sets the estimated completion time based on the last known time (JOBN msg) and the
     * estimated run time value from the JOBN message.
     * @param knownTime last known time
     */
    public void setEstCompleteTime(int knownTime) {
        estCompleteTime = knownTime + estRuntime;
    }

    /**
     * Used for stage 2 algorithm.
     * <p>
     * Returns the estimated job completion time assuming it has been set.
     * @return The estimated completion time, -1 if not set.
     */
    public int getEstCompleteTime() {
        return estCompleteTime;
    }

    public int getEstRunTime() {
        return estRuntime;
    }

    /**
     * Compare a job with another. Puts emphasis on CPU requirements in the comparison as
     * CPU tends to be the main bottleneck of ds-sim servers.
     * Also compares memory and disk requirements.
     * @param other
     * @return -1 if (this) is less than other, 0 if equal, 1 if (this) greater than other
     */
    public int compareJob(Job other) {
        if (getCPUReq() < other.getCPUReq()) { return -1; }
        if (getCPUReq() > other.getCPUReq()) { return 1; }
        if (getMemReq() < other.getMemReq()) { return -1;}
        if (getMemReq() > other.getMemReq()) { return 1; }
        if (getDiskReq() < other.getDiskReq()) { return -1; }
        if (getDiskReq() > other.getDiskReq()) { return 1; }
        return 0;
    }

    public static Job getJobFromLSTJ(String line) {
        if (line.isEmpty()) { return null; }
        String data[] = line.split(" ");
        // jobID jobState submitTime startTime estRunTime core memory disk
        int id = Integer.valueOf(data[0]);
        int state = Integer.valueOf(data[1]);
        int submitTime = Integer.valueOf(data[2]);
        int startTime = Integer.valueOf(data[3]);
        int estRunTime = Integer.valueOf(data[4]);
        int core = Integer.valueOf(data[5]);
        int memory = Integer.valueOf(data[6]);
        int disk = Integer.valueOf(data[7]);
        // JOBN format: JOBN submitTime jobID estRuntime core memory disk
        Job temp = new Job("JOBN "+submitTime+" "+id+" "+estRunTime+" "+core+" "+memory+" "+disk);
        if (state == 2 && startTime != -1) {
            temp.setEstCompleteTime(startTime);
        }
        return temp;
    }
}
