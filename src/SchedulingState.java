import java.util.ArrayList;
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
     * Steps:
     * 1. Schedule job to first fit in immediately available servers
     * 2. If 1 cannot be achieved, schedule to the eventually capable server with the lowest
     * estimated wait time.
     * 
     * @param c The client
     */
    private void stage2JOBN(Client c) {
        c.addJob();
        Job job = c.getJobs().get(c.getJobs().size() + JOB_OFFSET);
        String schdMessage = "SCHD " + job.getJobID() + " ";

        // Fit to smallest server that can run job immediately
        List<Server> avail = c.sendGets("Avail", job);
        if (!avail.isEmpty()) {
            c.sendMessage(schdMessage + avail.get(0).getName());
            c.readMessage();
            return;
        }

        // Find smallest wait time and schedule to that eventually capable server
        List<Server> capable = c.sendGets("Capable", job);
        Server minWaitServer = capable.get(capable.size()-1);  // default to largest server
        int minTime = Integer.MAX_VALUE;
        // Find the smallest wait time
        for (Server s : capable) {
            List<String> serverJobs = sendLSTJ(c, s);
            // LSTJ format: jobID jobState submitTIme startTime estRunTime core memory disk
            for (String line : serverJobs) {
                String data[] = line.split(" ");
                if (!data[1].equals("2")) {
                    break;
                }
                int cores = Integer.valueOf(data[5]);
                int mem = Integer.valueOf(data[6]);
                int disk = Integer.valueOf(data[7]);
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
     * 2. Check all other servers for waiting jobs.
     * 3. Migrate the first waiting job that can be started immediately on JCPLServer from 1.
     * @param c
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
        List<Server> all = c.sendGets("All", null);
        all.removeIf(s -> s.getWJobs() == 0);  // remove all servers w/ no waiting jobs
        for (Server s : all) {
            if (s == JCPLServer) { continue; }  // Skip checking JCPLServer, reduce traffic
            List<String> jobs = sendLSTJ(c, s);
            for (String line : jobs) {
                // LSTJ format: jobID jobState submitTIme startTime estRunTime core memory disk
                String[] lineData = line.split(" ");
                if (lineData[1].equals("1")) {
                    int coreReq = Integer.valueOf(lineData[5]);
                    int memReq = Integer.valueOf(lineData[6]);
                    int diskReq = Integer.valueOf(lineData[7]);
                    if (coreReq <= core && memReq <= mem && diskReq <= disk) {
                        c.sendMessage("MIGJ " + lineData[0] +" " + s.getName() + " " +JCPLServerName);
                        c.readMessage();
                        return;
                    }
                }
            }
        }
    }


    /**
     * Stage 2 - experimental <p>
     * Queries server for all jobs scheduled to a server. <p>
     * Format: jobID jobState submitTIme startTime estRunTime core memory disk
     * @param c client for communication
     * @param server we wish to learn about
     * @return list of jobs on server in format specified above
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
