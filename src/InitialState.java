public class InitialState implements State {

    @Override
    public void action(Client c) {
        // Greet ds-server
        c.sendMessage("HELO");
        c.readMessage();
        c.sendMessage("AUTH " + System.getProperty("user.name"));
        c.readMessage();
        // Update client state to getServersState
        c.changeState(new GetServersState());
    }
}
