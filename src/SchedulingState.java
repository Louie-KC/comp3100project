import java.util.List;

public class SchedulingState implements State {
    // Putting in LRR just for testing after refactor. Will be changed to
    // new scheduling algo later.

    // NOTE:
    // Client-server communication in scheduling algorithms MUST ONLY be of sending a
    // scheduling message, and receiving (and checking) the "OK" that should follow.
    // Retrieving updates from the server (including new jobs) is handled by the action
    // method that is called by the Client class.

    // Used for converting job list size to a useful index.
    private final int JOB_OFFSET = -1;

    @Override
    public void action(Client c) {
        if (c.getLastMsg().equals("NONE")) {
            c.changeState(new TerminatingState());
            return;
        }
        if (c.getLastMsg().contains("JCPL")) {
            c.sendMessage("REDY");
            c.readMessage();
            return;
        }
        if (c.getLastMsg().contains("JOBN")) {
            c.addJob();
            largestRoundRobin(c);
        }
        // Receiving next update from server
        c.sendMessage("REDY");
        c.readMessage();
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
