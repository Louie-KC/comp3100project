public class Job {
    // private int submitTime;
    private int jobID;
    // private int estRunTime;
    private int CPURequirement;
    private int RAMRequirement;
    private int diskSpaceRequirement;


    public Job(String jobString) {
        String[] data = jobString.split(" ");
        // Ignore data[0] as it is "JOBN"
        // submitTime = Integer.valueOf(data[1]);
        jobID = Integer.valueOf(data[2]);
        // estRunTime = Integer.valueOf(data[3]);
        CPURequirement = Integer.valueOf(data[4]);
        RAMRequirement = Integer.valueOf(data[5]);
        diskSpaceRequirement = Integer.valueOf(data[6]);
    }

    public int getJobID() {
        return jobID;
    }

    /**
     * Used for querying DS-SIM server about job capabilities.
     * @return String used after "GETS " to find server capability.
     */
    public String getCapableString() {
        String result = CPURequirement + " " + RAMRequirement + " " + diskSpaceRequirement;
        return result;
    }
}
