public class TerminatingState implements State {

    @Override
    public void action(Client c) {
        c.setDisconnect();
        c.sendMessage("QUIT");
        c.readMessage();
    }
    
}
