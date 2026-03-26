import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;

public class Router {

    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            System.out.println("Usage: java Router <MAC_ADDRESS>");
            return;
        }

        String macAddress = args[0];
        System.out.println("Router started with MAC: " + macAddress);

        File config = new File("src/config.txt");
        Parser parser = new Parser(config);

        String routerLine = parser.parseHostOrRouter(macAddress);
        String[] parts = routerLine.split(" ");

        int udpPort = Integer.parseInt(parts[1]);
        InetAddress realIP = InetAddress.getByName(parts[2]);

        String leftVirtualIP = parts[3];
        String rightVirtualIP = parts[4];

        String leftSubnet = leftVirtualIP.split("\\.")[0];
        String rightSubnet = rightVirtualIP.split("\\.")[0];

        HashMap<String, Port> neighbors = parser.getNeighbors(macAddress);
        HashMap<String, DistanceVector> routerTable = parser.getInitialRouterTable(macAddress);

        printRouterTable(routerTable);

        DatagramSocket socket = new DatagramSocket(udpPort);
        DatagramPacket incoming = new DatagramPacket(new byte[1024], 1024);

        while (true) {

            socket.receive(incoming);
            String frame = new String(incoming.getData(), 0, incoming.getLength());

            Packet packet;
            try {
                packet = new Packet(frame);
            } catch (Exception e) {
                continue;
            }

            System.out.println("\n[ROUTER " + macAddress + "] RECEIVED:");
            printPacket(packet);

            String srcIP = packet.getSourceIPAddress();
            String destIP = packet.getDestinationIPAddress();
            String srcSubnet = srcIP.split("\\.")[0];
            String destSubnet = destIP.split("\\.")[0];

            if (srcSubnet.equals(destSubnet)) {
                System.out.println("Dropping packet - source and destination in same subnet (" + srcSubnet + ")");
            }
            else {
                DistanceVector dv = routerTable.get(destSubnet);
                String routeEntry = dv.getNextHop();

                if (routeEntry == null) {
                    System.out.println("No route to subnet: " + destSubnet);
                    continue;
                }

                String newDestMac = null;
                Port outgoingPort = null;

                if (routeEntry.contains(":")) {

                    for (String neighborMac : neighbors.keySet()) {
                        Port p = neighbors.get(neighborMac);

                        String full =
                                p.getIpAddress().getHostAddress() +
                                        ":" +
                                        p.getUdpPort();

                        if (full.equals(routeEntry)) {
                            newDestMac = destIP.split("\\.")[1];
                            outgoingPort = p;
                            break;
                        }
                    }
                } else {
                    String nextHopMac = routeEntry.split("\\.")[1];
                    newDestMac = nextHopMac;
                    outgoingPort = neighbors.get(nextHopMac);
                }

                if (outgoingPort == null) {
                    System.out.println("Outgoing port not found.");
                    continue;
                }

                if (destSubnet.equals(leftSubnet)) {
                    packet.setSourceIPAddress(leftVirtualIP);
                } else if (destSubnet.equals(rightSubnet)) {
                    packet.setSourceIPAddress(rightVirtualIP);
                } else {
                    if (leftSubnet.equals("net2")) {
                        packet.setSourceIPAddress(leftVirtualIP);
                    } else {
                        packet.setSourceIPAddress(rightVirtualIP);
                    }
                }

                packet.setSourceMacAddress(macAddress);
                packet.setDestinationMacAddress(newDestMac);

                System.out.println("[ROUTER " + macAddress + "] FORWARDING:");
                printPacket(packet);

                byte[] outBytes = packet.createFrameString().getBytes();

                DatagramPacket outgoingPacket =
                        new DatagramPacket(
                                outBytes,
                                outBytes.length,
                                outgoingPort.getIpAddress(),
                                outgoingPort.getUdpPort()
                        );

                socket.send(outgoingPacket);
            }
        }
    }

    public static void printRouterTable(HashMap<String, DistanceVector> routerTable) {

        System.out.println("\nRouting Table:");
        System.out.println("Subnet\tNext Hop\t\t\tDistance");

        for (String subnet : routerTable.keySet()) {
            System.out.println(subnet + "\t" + routerTable.get(subnet).getNextHop() + "\t\t" + routerTable.get(subnet).getDistance());
        }
    }

    public static void printPacket(Packet p) {

        System.out.println("  Src MAC: " + p.getSourceMacAddress());
        System.out.println("  Dst MAC: " + p.getDestinationMacAddress());
        System.out.println("  Src IP : " + p.getSourceIPAddress());
        System.out.println("  Dst IP : " + p.getDestinationIPAddress());
        System.out.println("  Data   : " + p.getData());
    }

}
