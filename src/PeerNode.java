public class PeerNode {

    private String ip;
    private int port;

    PeerNode(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getFullAddress() {
        return ip + ":" + port;
    }

    public String getIp() {
        return ip;
    }
}
