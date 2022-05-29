import java.util.ArrayList;
import java.util.List;

public class SchedulingState implements State {
    // NOTE:
    // Client-server communication in scheduling algorithms MUST NOT contain the query
    // for the next update from the ds-sim server. This is handled at the end of the action
    // method. Action is continuously called by the Client until its state changes to the
    // TerminatingState.

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
            c.checkLastMsgForJob();
            // largestRoundRobin(c);
            stage2JOBN(c);
        }
        // Receiving next update from server
        c.sendMessage("REDY");
        c.readMessage();
    }

    /**
     * Steps:
     * 1. Schedule job to first fit in immediately available servers
     * 2. If 1 cannot be achieved, schedule to the eventually capable server with the lowest
     * estimated wait time and no waiting jobs.
     * 3. If all servers have waiting jobs, default to scheduling to the largest server.
     * @param c client reference
     */
    private void stage2JOBN(Client c) {
        Job job = c.getLastJob();
        String schdMessage = "SCHD " + job.getJobID() + " ";

        // Fit to smallest server that can run job immediately
        List<Server> avail = sendGets(c, "Avail", job);
        if (!avail.isEmpty()) {
            c.sendMessage(schdMessage + avail.get(0).getName());
            c.readMessage();
            return;
        }

        // Find smallest wait time and schedule to that eventually capable server
        List<Server> capable = sendGets(c, "Capable", job);
        // Default to largest server as no job requires more resources than the largest server type.
        Server minWaitServer = capable.get(capable.size()-1);
        int minTime = Integer.MAX_VALUE;
        // Find the server with the smallest wait time for the job to be scheduled.
        for (Server s : capable) {
            List<String> serverJobs = sendLSTJ(c, s);
            // LSTJ format: jobID jobState submitTIme startTime estRunTime core memory disk
            for (String line : serverJobs) {
                String[] data = line.split(" ");
                if (!data[1].equals("2")) {
                    break;
                }
                // running job resources
                int cores = Integer.valueOf(data[5]);
                int mem = Integer.valueOf(data[6]);
                int disk = Integer.valueOf(data[7]);
                // skip if the job is smaller than job to be scheduled
                if (cores < job.getCPUReq() || mem < job.getMemReq() || disk < job.getDiskReq()) {
                    continue;
                }
                // startTime + estRunTime = estimated completion time
                int estCompletion = Integer.valueOf(data[3]) + Integer.valueOf(data[4]);
                if (estCompletion < minTime) {
                    minTime = estCompletion;
                    minWaitServer = s;
                }
            }
        }
        c.sendMessage(schdMessage + minWaitServer.getName());
        c.readMessage();
    }

    /**
     * Steps:
     * 1. Find server instance that job completed on (JCPLServer).
     * 2. If JCPLServer has waiting jobs, return.
     * 3. Check all other servers for waiting jobs.
     * 4. Migrates as many waiting jobs to JCPLServer that can be started immediately until
     * JCPLS resources run out.
     * @param c client reference
     */
    private void stage2JCLP(Client c) {
        // JCPL format: JCPL endTime jobID serverType serverID
        String[] data = c.getLastMsg().split(" ");
        String JCPLServerName = data[3] +" "+data[4];
        Server JCPLServer = null;
        // Find the server that just completed a job
        for (Server s : c.getServers()) {
            if (s.getName().equals(JCPLServerName)) {
                JCPLServer = s;
                break;
            }
        }

        // Find JCPL currently available resources
        int core = JCPLServer.getCoreCount();
        int mem = JCPLServer.getMemory();
        int disk = JCPLServer.getDisk();
        List<String> JCPLServerJobs = sendLSTJ(c, JCPLServer);
        for (String line : JCPLServerJobs) {
            String[] lineData = line.split(" ");
            // if JCPL Server holds a queue of jobs, end the migration process
            if (lineData[1].equals("1")) { return; }
            core -= Integer.valueOf(lineData[5]);
            mem -= Integer.valueOf(lineData[6]);
            disk -= Integer.valueOf(lineData[7]);
        }
        // At this point we know that JCPLServer has no waiting jobs
        // Search for a job we can migrate to the JCPL server
        List<Server> all = sendGets(c, "All", null);
        for (Server s : all) {
            if (s.getWJobs() == 0) { continue; }  // Skip, there are no waiting jobs to migrate
            if (s == JCPLServer) { continue; }  // Skip checking JCPLServer, reduce traffic
            List<String> jobs = sendLSTJ(c, s);
            for (String line : jobs) {
                // If we use up all resources on JCPLServer, stop migration process
                if (core <= 0 || mem <= 0 || disk <= 0) { return; }
                // LSTJ format: jobID jobState submitTIme startTime estRunTime core memory disk
                String[] lineData = line.split(" ");
                if (lineData[1].equals("1")) {
                    // Waiting job resource requirements
                    int coreReq = Integer.valueOf(lineData[5]);
                    int memReq = Integer.valueOf(lineData[6]);
                    int diskReq = Integer.valueOf(lineData[7]);
                    // Check if we can migrate job to JCPLServer, if so decrease available reosurces
                    if (coreReq <= core && memReq <= mem && diskReq <= disk) {
                        c.sendMessage("MIGJ "+lineData[0]+" "+s.getName()+" " +JCPLServerName);
                        c.readMessage();
                        core -= coreReq;
                        mem -= memReq;
                        disk -= diskReq;
                    }
                }
            }
        }
    }


    /**
     * Stage 2 - Get the job list of a server <p>
     * Queries ds-sim for all jobs scheduled to a server. <p>
     * Format: jobID jobState submitTIme startTime estRunTime core memory disk
     * @param c client reference
     * @param server we wish to learn about
     * @return A list of jobs on server in format specified above
     */
    private List<String> sendLSTJ(Client c, Server server) {
        if (c == null || server == null) { return null; }
        List<String> jobsOnServer = new ArrayList<>();
        c.sendMessage("LSTJ " + server.getName());
        // DATA nRecs recLen
        c.readMessage();
        c.sendMessage("OK");
        int nRecs = Integer.valueOf(c.getLastMsg().split(" ")[1]);
        for (int i = 0; i < nRecs; i++) {
            c.readMessage();
            jobsOnServer.add(c.getLastMsg());
        }
        if (nRecs != 0) { c.sendMessage("OK"); }
        c.readMessage();
        return jobsOnServer;
    }

    /**
     * Stage 2 <p>
     * Sends GETS message to ds-sim with the specified GETS option (All, Capable, Avail) and adds
     * job details if necessary. Stores the response in a Server List and returns it.
     * @param c client reference
     * @param getsOption "All", "Capable", "Avail"
     * @param job The job to get resource information if necessary
     * @return The ds-sim servers response as a list of Servers. Null if invalid parameters.
     */
    public List<Server> sendGets(Client c, String getsOption, Job job) {
        String gets = "GETS ";
        if (getsOption.equals("Capable") || getsOption.equals("Avail") && job != null) {
            gets = gets + getsOption + " " + job.getQueryString();
        } else if (getsOption.equals("All")) {
            gets = gets + getsOption;
        }
        c.sendMessage(gets);
        c.readMessage(); // DATA nRecs recLen
        int nRecs = Integer.valueOf(c.getLastMsg().split(" ")[1]);
        c.sendMessage("OK");
        List<Server> result = new ArrayList<>();
        for (int i = 0; i < nRecs; i++) {
            c.readMessage();
            result.add(new Server(c.getLastMsg()));
        }
        if (result.size() != 0) { c.sendMessage("OK"); }
        c.readMessage();
        return result;
    }

    /**
     * Stage 1 - Largest Round Robin
     * <p>
     * Schedule jobs to a set of servers with the largest physical CPU resources that
     * occur first in the list of servers. Filters the server list if not already done.
     * <p>
     * Sequentially cycles through the set of first largest servers.
     * @param c client reference
     */
    private void largestRoundRobin(Client c) {
        if (!c.getServerFilterStatus()) {
            getLargestCPUServers(c.getServers());
            c.setFilteredStatus();
        }
        Job job = c.getLastJob();
        Server target = c.getServers().get((c.getJobCount()-1) % c.getServers().size());
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
