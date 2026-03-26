import java.io.File;
import java.io.FileNotFoundException;
import java.net.*;
import java.util.HashMap;

public class Switch {
    public static void main(String[] args) throws Exception, FileNotFoundException {
        String macAddress = args[0];
        System.out.println("Mac Address: " + macAddress);
        File config = new File("src/config.txt");
        Parser parser = new Parser(config);
        Port switchPort = parser.parseMac(macAddress);
        HashMap<String, Port> neighbors = parser.getNeighbors(macAddress);
        HashMap<String, Integer> switchTable = new HashMap<>();
        DatagramSocket socket = new DatagramSocket(switchPort.getUdpPort());
        DatagramPacket incoming = new DatagramPacket(new byte[1024], 1024);


        //Main loop to receive and send out packets
        while(true) {
            //Receive the packet and create our own Packet object
            socket.receive(incoming);
            String frame = new String(incoming.getData(), 0, incoming.getLength());
            Packet incomingPacket;
            try {
                incomingPacket = new Packet(frame);
            } catch (Exception e) {
                continue;
            }

            //If the destination address is in the switch table, find the neighbor with matching udp port and send to them
            if(switchTable.containsKey(incomingPacket.getDestinationMacAddress())) {
                if(!switchTable.containsKey(incomingPacket.getSourceMacAddress())) {
                    switchTable.put(incomingPacket.getSourceMacAddress(), incoming.getPort());
                }
                byte[] outgoingByte = incomingPacket.createFrameString().getBytes();
                for (String s : neighbors.keySet()) {
                    Port p = neighbors.get(s);
                    if (p.getUdpPort() == switchTable.get(incomingPacket.getDestinationMacAddress())) {
                        DatagramPacket outgoingPacket = new DatagramPacket(
                                outgoingByte,
                                outgoingByte.length,
                                p.getIpAddress(),
                                p.getUdpPort()
                        );
                        socket.send(outgoingPacket);
                        printSwitchTable(switchTable);
                        System.out.println("Packet sent to " + s);
                    }
                }
            }
            else {
                //Otherwise, update the switch table and flood to all neighbors excluding where the packet came from
                switchTable.put(incomingPacket.getSourceMacAddress(), incoming.getPort());
                printSwitchTable(switchTable);
                flood(neighbors, socket, incoming.getPort(), incomingPacket);
            }
        }
    }

    public static void printSwitchTable(HashMap<String, Integer> switchTable) {
        System.out.println("Switch Table:");
        System.out.println("MAC\tPort");
        for (String mac : switchTable.keySet()) {
            System.out.println(mac + "\t" + switchTable.get(mac));
        }
    }

    public static void flood(HashMap<String, Port> neighbors, DatagramSocket socket, int incomingPort, Packet packet) {
        // Flood the frame to all neighbors except the incoming port
        for (String mac : neighbors.keySet()) {
            Port port = neighbors.get(mac);
            if (port.getUdpPort() != incomingPort) {
                byte[] outgoingByte = packet.createFrameString().getBytes();
                try {
                    DatagramPacket outgoingPacket = new DatagramPacket(
                            outgoingByte,
                            outgoingByte.length,
                            port.getIpAddress(),
                            port.getUdpPort()
                    );
                    socket.send(outgoingPacket);
                } catch (Exception e) {
                    System.err.println("Error flooding frame to " + mac + ": " + e.getMessage());
                }
            }
        }
        System.out.println("Frame flooded to all ports except incoming port " + incomingPort);
    }

}