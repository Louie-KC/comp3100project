public class Job {
    private int jobID;
    private int CPURequirement;
    private int RAMRequirement;
    private int diskSpaceRequirement;

    public Job(String jobString) {
        String[] data = jobString.split(" ");
        jobID = Integer.valueOf(data[2]);
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
    public String getQueryString() {
        String result = CPURequirement + " " + RAMRequirement + " " + diskSpaceRequirement;
        return result;
    } 
}
