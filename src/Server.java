import java.util.List;
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.LinkedList;

public class Server {
    private String type;
    private int id;
    private int coreCount;
    private int memory;
    private int disk;
    private int wJobs;
    private int rJobs;

    // private List<Job> runningJobs;  // Assumed
    // private List<Job> queuedJobs;  // Assumed

    // private Server originalStats;

    public Server(String resourceString) {
        // Server format: serverType serverID state curStartTime core memory disk #wJobs #rJobs
        String[] data = resourceString.split(" ");
        type = data[0];
        id = Integer.valueOf(data[1]);
        coreCount = Integer.valueOf(data[4]);
        memory = Integer.valueOf(data[5]);
        disk = Integer.valueOf(data[6]);
        wJobs = Integer.valueOf(data[7]);
        rJobs = Integer.valueOf(data[8]);

        // originalStats = new Server(this);
        // runningJobs = new ArrayList<>();
        // queuedJobs = new LinkedList<>();
    }

    // Only for creating originalStats instance.
    // private Server(Server inServer) {
    //     coreCount = inServer.coreCount;
    //     memory = inServer.memory;
    //     disk = inServer.disk;
    // }

    /**
     * Gets the servers type
     * @return server type
     */
    public String getType() {
        return type;
    }

    public int getID() {
        return id;
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

    public int getRJobs() {
        return rJobs;
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

    // public List<Job> getQueuedJobs() {
    //     return queuedJobs;
    // }

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
     * Find the maximum estimated completion time
     * @return
     */
    // public int maxRunningJobEstCompletionTime() {
    //     return maxJobEstTime(runningJobs);
    // }

    /**
     * Finds and returns the highest estimated job completion time in provided list.
     * @param list
     * @return highest estimated completion time in list
     */
    public int maxJobEstTime(List<Job> list) {
        int maxTime = -1;
        for (Job j : list) {
            if (j.getEstCompleteTime() > maxTime) {
                maxTime = j.getEstCompleteTime();
            }
        }
        return maxTime;
    }

    /**
     * Finds an estimate time when the server should be able to start processing a job. Looks at any
     * queued jobs, and any running jobs to estimate the ready time for a new job.
     * <p>
     * Note: When looking at running jobs, only considers all individual jobs and all combinations
     * of jobs that will release the resources needed for the new job.
     * @param jobToCheck
     * @param knownTime
     * @return The estimated time when the job can begin processing on the server.
     */
    // public int estWhenReadyForJob(Job jobToCheck, int knownTime) {
    //     int estQueueWaitTime = 0;

    //     // Assumption
    //     // The new job will join the back of a queue (if any) and will have to wait its turn.
    //     for (Job j : queuedJobs) { estQueueWaitTime += j.getEstRunTime(); } 

    //     // If there is a queue, then the server must be at capacity. Naive calculate time
    //     if (estQueueWaitTime != 0) {
    //         int timeToRunningJobsClear = maxRunningJobEstCompletionTime();
    //         return knownTime + timeToRunningJobsClear + estQueueWaitTime;
    //     }

    //     // All below if no queue
    //     List<Job> selection = new ArrayList<>();
    //     // Check all jobs and combination of jobs
    //     for (int i = 0; i < queuedJobs.size()-1; i++) {
    //         Job curJob1 = queuedJobs.get(i);
    //         // If job 1 is equal or larger in resources, 
    //         if (curJob1.compareJob(jobToCheck) >= 0 && !selection.contains(curJob1)) {
    //             if (curJob1.getEstCompleteTime() < maxJobEstTime(selection)) {
    //                 selection = new ArrayList<>();
    //                 selection.add(curJob1);
    //                 continue;
    //             }
    //         }
    //         // Check two jobs at a time
    //         Job[] comboJob = {curJob1, null};
    //         int comboMaxTime = -1;
    //         for (int j = i+1; j < queuedJobs.size()-1; j++) {
    //             comboJob[1] = queuedJobs.get(j);
    //             // Check resources of our running 2 job combination
    //             // JOBN format: JOBN submitTime jobID estRuntime core memory disk
    //             Job combined = new Job(String.format("j -1 -1 -1 %x %x %x", 
    //                                         comboJob[0].getCPUReq() + comboJob[1].getCPUReq(),
    //                                         comboJob[0].getMemReq() + comboJob[1].getMemReq(),
    //                                         comboJob[0].getDiskReq() + comboJob[1].getDiskReq()));
    //             // If our combination job is smaller than the job we wish to schedule, skip the rest
    //             if (combined.compareJob(jobToCheck) == -1) { continue; }

    //             // Checking time of our running job combo
    //             comboMaxTime = maxJobEstTime(new ArrayList<>(Arrays.asList(comboJob)));
    //             if (comboMaxTime < maxJobEstTime(selection)) {
    //                 selection = new ArrayList<>(Arrays.asList(comboJob));
    //             }
    //         }
    //     }
    //     return knownTime + maxJobEstTime(selection);
    // }

    /**
     * Checks if a server can at some point run a job.
     * @param jobToCheck
     * @return true if it can eventually run, false otherwise.
     */
    // public boolean canEventuallyRunJob(Job jobToCheck) {
    //     if (jobToCheck.getCPUReq() > originalStats.getCoreCount()) { return false; }
    //     if (jobToCheck.getMemReq() > originalStats.getMemory()) { return false; }
    //     if (jobToCheck.getDiskReq() > originalStats.getDisk()) { return false; }
    //     return true;
    // }

    /**
     * Updates interal list of jobs. Adds job to queuedJobs if server is known to not be able to
     * start job immediately. Reduces available resources if capable of starting job immediately.
     * @param job
     */
    // public void jobScheduled(Job job) {
    //     // Check if server can run immediately
    //     if (coreCount >= job.getCPUReq() && memory >= job.getMemReq() && disk >= job.getDiskReq()) {
    //         coreCount -= job.getCPUReq();
    //         memory -= job.getMemReq();
    //         disk -= job.getDiskReq();
    //         runningJobs.add(job);
    //     } else {  // Add to queue if incapable of starting immediately
    //         queuedJobs.add(job);
    //     }
    // }

    /**
     * Remove job from the Servers assumed runningJob list. Updates available resources accordingly.
     * @param jobID
     */
    // public void jobCompleted(int jobID) {
    //     for (int i = 0; i < runningJobs.size(); i++) {
    //         if (runningJobs.get(i).getJobID() == jobID) {
    //             Job completedJob = runningJobs.get(i);
    //             // Update resources
    //             coreCount += completedJob.getCPUReq();
    //             memory += completedJob.getMemReq();
    //             disk += completedJob.getDiskReq();

    //             runningJobs.remove(i);
    //         }
    //     }
    // }

    /**
     * To be called upon receiving JCPL about server. Moves a job from the assumed queued job list
     * to the running jobs list. 
     * <p>
     * Assumes that the job at the front of queue is moved to processing stage in ds-sim server.
     */
    // public void moveQueuedJobToRunning() {
    //     if (queuedJobs.isEmpty()) {
    //         System.err.println("moveQueuedJobToRunning: No job in queue to move");
    //         return;
    //     }
    //     jobScheduled(queuedJobs.remove(0));
    // }

    /**
     * Removes job from THIS server and schedules to targetServer. i.e. Job migration to target.
     * @param targetServer Server to migrate job to
     * @param jobToMigrate Job to be migrated
     */
    // public void migrateJob(Server targetServer, Job jobToMigrate) {
    //     if (targetServer == null || jobToMigrate == null || !queuedJobs.contains(jobToMigrate)) {
    //         System.err.println("migrateJobToOtherServer: Invalid migration!!!");
    //         return;
    //     }
    //     targetServer.jobScheduled(jobToMigrate);
    //     queuedJobs.remove(jobToMigrate);  // No need to update stats as queued jobs have no effect.
    // }

    /**
     * Checks resource stats and prints out error if resources of server fall below 0 or exceed
     * the servers original resource values.
     */
    // public void serverValidityCheck() {
    //     if (coreCount < 0 || coreCount > originalStats.getCoreCount()) {
    //         System.err.println("Server " + getName() + " failed coreCount validity check");
    //     }
    //     if (memory < 0 || memory > originalStats.getMemory()) {
    //         System.err.println("Server " + getName() + " failed memory validity check");
    //     }
    //     if (disk < 0 || disk > originalStats.getDisk()) {
    //         System.err.println("Server " + getName() + " failed disk validity check");
    //     }
    // }
}
