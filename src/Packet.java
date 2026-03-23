public class Packet {
    private String data;
    private String destinationMacAddress;
    private String sourceMacAddress;
    private String sourceIPAddress;
    private String destinationIPAddress;

    public Packet(String rawFrame) {
        String[] splitFrame = rawFrame.split(":",5);

        sourceMacAddress = splitFrame[0];
        destinationMacAddress = splitFrame[1];
        sourceIPAddress = splitFrame[2];
        destinationIPAddress = splitFrame[3];
        data = splitFrame[4];
    }

    public String getDestinationMacAddress() {
        return destinationMacAddress;
    }

    public String getSourceMacAddress() {
        return sourceMacAddress;
    }

    public String getData() {
        return data;
    }

    public String getSourceIPAddress() { return sourceIPAddress; }

    public String getDestinationIPAddress() { return destinationIPAddress; }

    public void setDestinationMacAddress(String destinationMacAddress) { this.destinationMacAddress = destinationMacAddress; }

    public void setSourceMacAddress(String sourceMacAddress) { this.sourceMacAddress = sourceMacAddress;}

    public void setSourceIPAddress(String sourceIPAddress) { this.sourceIPAddress = sourceIPAddress; }

    public String createFrameString() {
        return sourceMacAddress + ":" + destinationMacAddress + ":" + sourceIPAddress + ":" + destinationIPAddress + ":" + data;
    }
}