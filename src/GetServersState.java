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
        String tempMsg = c.getLastMsg();  // Store JOBN data temporarily
        
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
        // Since we have already retrieved the first job we do not want to query for it again.
        // Instead only this once do we re-write the clients last received message so that it
        // contains the job details we stored earlier. The scheduling state is responsible for
        // building/maintaining the clients job list, so we are give it a job to start with.
        c.setLastMsg(tempMsg);
        c.changeState(new SchedulingState());
    }
    
}
