public class Server {
    private String type;
    private int id;
    private int coreCount;

    public Server(String resourceString) {
        String[] data = resourceString.split(" ");
        type = data[0];
        id = Integer.valueOf(data[1]);
        coreCount = Integer.valueOf(data[4]);
    }

    /**
     * Gets the servers type
     * @return server type
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the servers core count
     * @return server core count
     */
    public int getCoreCount() {
        return coreCount;
    }

    /**
     * Gets the name/info needed for specifying this exact server for things
     * such as scheduling jobs: "type id"
     * @return String used for tasks such as scheduling.
     */
    public String getName() {
        return type + " " + id;
    }
}
