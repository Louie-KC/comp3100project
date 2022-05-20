public class GetServersState implements State {

    @Override
    public void action(Client c) {
        // Before we can retrive the ds-sims servers, we must retrieve at
        // least one job otherwise ds-sim complains.
        c.sendMessage("REDY");
        c.readMessage();
        if (!c.getLastMsg().contains("JOBN")) {
            System.err.println("GetServersState: Did not get JOBN on initial REDY");
            c.changeState(new TerminatingState());
            return;
        }
        c.addJob();
        String tempMsg = c.getLastMsg();  // Store JOBN temporarily
        
        // Get server info
        c.sendMessage("GETS All");  // Did not need job details, only for one to be sent
        c.readMessage();  // DATA nRecs recLen
        int numServers = Integer.valueOf(c.getLastMsg().split(" ")[1]);  // get nRecs value
        c.sendMessage("OK");
        for (int i = 0; i < numServers; i++) {
            c.readMessage();  // Details of a server
            c.addServer();  // Add server instance to serverList.
        }
        c.sendMessage("OK");
        c.readMessage();
        if (!c.getLastMsg().equals(".")) {
            System.err.println("GetServersState: Did not get '.' after servers");
            c.changeState(new TerminatingState());
            return;
        }
        // Since we have already retrieved the first job and added it to the job list we
        // need to remove it and set the last receveived message to be of the job details.
        // This is because the Scheduling state is responsible for building up the job list.
        c.setLastMsg(tempMsg); // rewrite msg so job does not need to be queried for again.
        c.removeFirstJob();  // Remove job so when added in SchedulingState we don't have two records.
        c.changeState(new SchedulingState());
    }
    
}
