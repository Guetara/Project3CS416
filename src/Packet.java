public class Packet {
    private String flag;
    private String data;
    private String destinationMacAddress;
    private String sourceMacAddress;
    private String sourceIPAddress;
    private String destinationIPAddress;

    public Packet(String rawFrame) {
        String[] splitFrame = rawFrame.split(":");

        flag = splitFrame[0];
        sourceMacAddress = splitFrame[1];
        destinationMacAddress = splitFrame[2];
        sourceIPAddress = splitFrame[3];
        destinationIPAddress = splitFrame[4];
        data = splitFrame[5];
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

    public String getFlag() { return flag; }

    public void setDestinationMacAddress(String destinationMacAddress) { this.destinationMacAddress = destinationMacAddress; }

    public void setSourceMacAddress(String sourceMacAddress) { this.sourceMacAddress = sourceMacAddress;}

    public void setSourceIPAddress(String sourceIPAddress) { this.sourceIPAddress = sourceIPAddress; }

    public String createFrameString() {
        return flag + ":" + sourceMacAddress + ":" + destinationMacAddress + ":" + sourceIPAddress + ":" + destinationIPAddress + ":" + data;
    }
}