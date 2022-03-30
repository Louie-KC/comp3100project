public class ServerResource {
    private String type;
    private int id;
    private int coreCount;

    public ServerResource(String resourceString) {
        String[] data = resourceString.split(" ");
        type = data[0];
        id = Integer.valueOf(data[1]);
        coreCount = Integer.valueOf(data[4]);
    }

    public String getType() {
        return type;
    }

    public int getCoreCount() {
        return coreCount;
    }

    public String getName() {
        return type + " " + id;
    }
}