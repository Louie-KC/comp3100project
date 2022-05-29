public class InitialState implements State {

    @Override
    public void action(Client c) {
        // Greet ds-server
        c.sendMessage("HELO");
        c.readMessage();
        c.sendMessage("AUTH " + System.getProperty("user.name"));
        c.readMessage();
        c.changeState(new GetServersState()); // Update client state to getServersState
    }
}
