import java.util.List;

public class SchedulingState implements State {
    // Putting in LRR just for testing after refactor. Will be changed to
    // new scheduling algo later.

    // NOTE:
    // Client-server communication in scheduling algorithms MUST ONLY be of sending a
    // scheduling message, and receiving (and checking) the "OK" that should follow.
    // Retrieving updates from the server (including new jobs) is handled by the action
    // method that is called by the Client class. Where jobs are created is up to the
    // algorithm (client or on server instance).

    // Used for converting job list size to a useful index.
    private final int JOB_OFFSET = -1;

    @Override
    public void action(Client c) {
        if (c.getLastMsg().equals("NONE")) {
            c.changeState(new TerminatingState());
            return;
        }
        if (c.getLastMsg().contains("JCPL")) {
            stage2JCLP(c);
        }
        if (c.getLastMsg().contains("JOBN")) {
            // largestRoundRobin(c);
            stage2JOBN(c);
        }
        // Receiving next update from server
        c.sendMessage("REDY");
        c.readMessage();
    }

    /**
     * Goals:
     * 1. Start fitting to worst fit, 
     * 2. Each job scheduled (assumed running and known queued) are to be stored in each Server inst
     * 3. Update clients lastKnownTime/clock
     * 4. If resources are not immediately available on any server, add to queue of server with shortest
     *    estimated time to some job completion that deallocates the resources needed for the new job.
     * @param c
     */
    private void stage2JOBN(Client c) {
        c.updateKnownTime();  // Update client clock
        c.addJob();
        Server curServer;
        Job job = c.getJobs().get(c.getJobs().size() + JOB_OFFSET);
        String schdMessage = "SCHD " + job.getJobID() + " ";
        // int[] estTimeToStart = new int[c.getServers().size()];

        int minTimeIdx = 0;
        int minTime = Integer.MAX_VALUE;

        // Search server list backwards (As larger servers are received last) for ready now server
        for (int i = c.getServers().size()+JOB_OFFSET; i >= 0; i--) {
            curServer = c.getServers().get(i);
            if (curServer.canRunJob(job)) {
                c.sendMessage(schdMessage + curServer.getName());
                c.readMessage();
                curServer.jobScheduled(job);
                return;
            }
            // If the server cannot run job immediately, check when/if it can and record time if it
            // provides the smallest known queue waiting time.
            if (curServer.canEventuallyRunJob(job)) {
                // estTimeToStart[i] = curServer.estWhenReadyForJob(job, c.getKnownTime());
                int curTimeEst = curServer.estWhenReadyForJob(job, c.getKnownTime());
                if (minTime > curTimeEst) {
                    minTime = curTimeEst;
                    minTimeIdx = i;
                }
            }
            // } else {
            //     estTimeToStart[i] = Integer.MAX_VALUE;
            // }
        }
        // int minTimeIdx = 0;
        // int minTime = Integer.MAX_VALUE;
        // for (int i = 0; i < estTimeToStart.length; i++) {
        //     if (minTime > estTimeToStart[i]) {
        //         minTime = estTimeToStart[i];
        //         minTimeIdx = i;
        //     }
        // }
        // Get the server that provides the shortest estimated waiting time in queue
        curServer = c.getServers().get(minTimeIdx);
        c.sendMessage(schdMessage + curServer.getName());
        c.readMessage();
        curServer.jobScheduled(job);
        curServer.serverValidityCheck();
    }

    /**
     * Goals:
     * 1. Update Server in memories known available resources (deallocation)
     * 2. Update clients lastKnownTime/clock
     * 3. Check all other servers queued jobs to see if some queued job can be migrated to the
     *    server which just completed a job.
     * @param c
     */
    private void stage2JCLP(Client c) {
        c.updateKnownTime();  // Update client clock
        // JCPL format: JCPL endTime jobID serverType serverID
        String data[] = c.getLastMsg().split(" ");
        Server JCPLServer = null;  // Target for job migration as job has completed.

        // Deallocate resources off appropriate server
        for (Server s : c.getServers()) {
            // data[3] == serverType, data[4] == serverID
            if (s.getType().equals(data[3]) && s.getID() == Integer.valueOf(data[4])) {
                s.jobCompleted(Integer.valueOf(data[2]));  // data[2] == jobID
                // if (!s.getQueuedJobs().isEmpty()) {
                //     s.moveQueuedJobToRunning();
                //     serverOfInterest = s;
                // }
                JCPLServer = s;
                break;
            }
        }
        // if JCPLServer has job queue, end process (no point migrating a job to JCPLServer)
        if (JCPLServer == null) { return; }
        if (!JCPLServer.getQueuedJobs().isEmpty()) {
            JCPLServer.moveQueuedJobToRunning();
            return;
        }
        // Check if we can reschedule some job to the server (as no queued jobs)

        // int minQueueTime = Integer.MAX_VALUE;
        // Server minQueueServer = null;
        // Job eventualJob = null;
        // For every other server that exists (not JCPLServer)

        for (Server otherS : c.getServers()) {
            if (otherS == JCPLServer) { continue; }
            // For every queued job on other server
            for (Job queuedJob : otherS.getQueuedJobs()) {
                if (JCPLServer.canRunJob(queuedJob)) {  // If JCPLServer can start job immediately
                    // MIGJ format: MIGJ jobID srcServerType srcServerID tgtServerType tgtServerID 
                    System.out.println("MIGRATING JOB!!!");
                    c.sendMessage("MIGJ " + otherS.getName() + " " + JCPLServer.getName());
                    c.readMessage();  // Expect "OK" as ACK for MIGJ message
                    // Migrate job in server in memory
                    otherS.migrateJob(JCPLServer, queuedJob);
                    return;
                }

                // If it can eventually run the server
                // if (JCPLServer.canEventuallyRunJob(queuedJob)) {
                //     int queueTime = JCPLServer.estWhenReadyForJob(queuedJob, c.getKnownTime());
                //     if (queueTime < minQueueTime) {
                //         minQueueTime = queueTime;
                //         minQueueServer = otherS;
                //         eventualJob = queuedJob;
                //     }
                // }
            }
        }
        // JCPLServer.migrateJob(minQueueServer, eventualJob);
    }

    /**
     * Stage 1 - Largest Round Robin
     * <p>
     * Schedule jobs to a set of servers with the largest physical CPU resources that
     * occur first in the list of servers. Filters the server list if not already done.
     * <p>
     * Sequentially cycles through the set of first largest servers.
     * @param c
     */
    private void largestRoundRobin(Client c) {
        c.addJob();
        if (!c.serversFiltered) { getLargestCPUServers(c.getServers()); }
        Job job = c.getJobs().get(c.getJobs().size() + JOB_OFFSET);
        Server target = c.getServers().get((c.getJobs().size() + JOB_OFFSET) % c.getServers().size());
        c.sendMessage("SCHD " + job.getJobID() + " " + target.getName());
        c.readMessage();
        if (!c.getLastMsg().equals("OK")) {
            System.err.println("LRR: Did not get 'OK' after job schedule");
            c.setDisconnect();
            return;
        }
    }

    /**
     * Stage 1 - LRR component
     * <p>
     * Updates the list of servers to contain only the *first* server type with the largest CPU
     * core count. <p>
     * Suppose the ds-server had the following servers available:
     * <pre>
| Type |ID|Cores|
|------|--|-----|
|Medium| 0|   4 |
|Large | 0|   8 |
|Large | 1|   8 |
|Large2| 0|   8 |
     * </pre>
     * Large 0 and Large 1 will be all that remains, as it has the largest core count (8) and
     * occurs before Large2 0 (also with 8 cores).
     * @param servers
     */
    private void getLargestCPUServers(List<Server> servers) {
        if (servers == null || servers.isEmpty()) { return; }
        int tempMaxCPU = 0;
        String firstTopType = "";
        for (Server s : servers) {
            if (s.getCoreCount() > tempMaxCPU) {
                tempMaxCPU = s.getCoreCount();
                firstTopType = s.getType();
            }
        }
        String topType = firstTopType;
        servers.removeIf(sr -> !sr.getType().equals(topType));
    }
}
