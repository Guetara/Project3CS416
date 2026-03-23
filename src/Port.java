import java.net.InetAddress;

public class Port {
    private final int udpPort;
    private final InetAddress ipAddress;

    public Port () {
        udpPort = 0;
        ipAddress = null;
    }

    public Port (int udpPort, InetAddress ipAddress) {
        this.udpPort = udpPort;
        this.ipAddress = ipAddress;
    }

    public int getUdpPort() {
        return udpPort;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public String getFullPort() { return Integer.toString(udpPort) + ":" + ipAddress.getHostAddress();}
}
