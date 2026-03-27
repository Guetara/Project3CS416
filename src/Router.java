import java.io.File;
import java.io.FileNotFoundException;
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
//
        String macAddress = args[0];
        System.out.println("Router started with MAC: " + macAddress);

        File config = new File("src/config.txt");
        Parser parser = new Parser(config);

        String routerLine = parser.parseHostOrRouter(macAddress);
        String[] parts = routerLine.split(" ");

        int udpPort = Integer.parseInt(parts[1]);
        InetAddress realIP = InetAddress.getByName(parts[2]);

        HashMap<String, Port> neighbors = parser.getNeighbors(macAddress);
        HashMap<String, DistanceVector> routerTable = parser.getInitialRouterTable(macAddress);

        printRouterTable(routerTable);

        DatagramSocket socket = new DatagramSocket(udpPort);

        // Send initial distance vector table to neighbors
        sendDistanceVectors(routerTable, socket, neighbors, macAddress, parser);

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

            // Check if this is a distance vector update (flag = "1") or a data packet (flag = "0")
            String flag = packet.getFlag();
            if ("1".equals(flag)) {
                // Process distance vector update
                boolean tableChanged = processDistanceVector(packet, routerTable, neighbors);

                if (tableChanged) {
                    System.out.println("[ROUTER " + macAddress + "] Routing table updated!");
                    printRouterTable(routerTable);
                    // Send updated DV to all neighbors
                    sendDistanceVectors(routerTable, socket, neighbors, macAddress, parser);
                }
            } else {
                // Data packet — forward it
                String srcIP = packet.getSourceIPAddress();
                String destIP = packet.getDestinationIPAddress();
                String srcSubnet = srcIP.split("\\.")[0];
                String destSubnet = destIP.split("\\.")[0];

                if (srcSubnet.equals(destSubnet)) {
                    System.out.println("Dropping packet - source and destination in same subnet (" + srcSubnet + ")");
                } else {
                    DistanceVector dv = routerTable.get(destSubnet);

                    if (dv == null) {
                        System.out.println("No route to subnet: " + destSubnet);
                        continue;
                    }

                    String routeEntry = dv.getNextHop();

                    String newDestMac = null;
                    Port outgoingPort = null;

                    if (routeEntry.contains(":")) {
                        // Next hop is in IP:port format (directly connected neighbor)
                        for (String neighborMac : neighbors.keySet()) {
                            Port p = neighbors.get(neighborMac);
                            String full = p.getIpAddress().getHostAddress() + ":" + p.getUdpPort();

                            if (full.equals(routeEntry)) {
                                // If neighbor is a switch, send to the final destination's MAC
                                // If neighbor is a router, send to the router's MAC
                                // If neighbor is a host, send to the destination host's MAC
                                if (neighborMac.charAt(0) == 'S') {
                                    newDestMac = destIP.split("\\.")[1];
                                } else if (neighborMac.charAt(0) == 'R') {
                                    newDestMac = neighborMac;
                                } else {
                                    newDestMac = destIP.split("\\.")[1];
                                }
                                outgoingPort = p;
                                break;
                            }
                        }
                    } else {
                        // Next hop is in subnet.MAC format (learned via DV)
                        String nextHopMac = routeEntry.split("\\.")[1];
                        newDestMac = nextHopMac;
                        outgoingPort = neighbors.get(nextHopMac);
                    }

                    if (outgoingPort == null) {
                        System.out.println("Outgoing port not found.");
                        continue;
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
    }

    /**
     * Process an incoming distance vector update packet.
     * Returns true if the routing table was changed.
     */
    public static boolean processDistanceVector(Packet packet, HashMap<String, DistanceVector> routerTable, HashMap<String, Port> neighbors) {
        boolean changed = false;
        String senderMac = packet.getSourceMacAddress();
        String dvData = packet.getData();

        // DV data format: "subnet1 dist1,subnet2 dist2,..."
        String[] entries = dvData.split(",");

        for (String entry : entries) {
            entry = entry.trim();
            if (entry.isEmpty()) continue;

            String[] subnetAndDist = entry.split(" ");
            if (subnetAndDist.length < 2) continue;

            String subnet = subnetAndDist[0];
            int advertisedDistance;
            try {
                advertisedDistance = Integer.parseInt(subnetAndDist[1]);
            } catch (NumberFormatException e) {
                continue;
            }

            int newDistance = advertisedDistance + 1;

            // Build the next hop string: use the sender's port info
            Port senderPort = neighbors.get(senderMac);
            if (senderPort == null) continue;
            String nextHop = senderPort.getIpAddress().getHostAddress() + ":" + senderPort.getUdpPort();

            if (!routerTable.containsKey(subnet)) {
                // New subnet discovered
                routerTable.put(subnet, new DistanceVector(newDistance, nextHop));
                changed = true;
                System.out.println("  Learned new route: " + subnet + " via " + senderMac + " distance " + newDistance);
            } else {
                DistanceVector current = routerTable.get(subnet);
                if (newDistance < current.getDistance()) {
                    // Found a shorter path
                    current.setDistance(newDistance);
                    current.setNextHop(nextHop);
                    changed = true;
                    System.out.println("  Updated route: " + subnet + " via " + senderMac + " distance " + newDistance);
                }
            }
        }

        return changed;
    }

    public static void printRouterTable(HashMap<String, DistanceVector> routerTable) {

        System.out.println("\nRouting Table:");
        System.out.println("Subnet\tNext Hop\t\t\tDistance");

        for (String subnet : routerTable.keySet()) {
            System.out.println(subnet + "\t" + routerTable.get(subnet).getNextHop() + "\t\t" + routerTable.get(subnet).getDistance());
        }
    }

    public static void printPacket(Packet p) {

        System.out.println("  Flag   : " + p.getFlag());
        System.out.println("  Src MAC: " + p.getSourceMacAddress());
        System.out.println("  Dst MAC: " + p.getDestinationMacAddress());
        System.out.println("  Src IP : " + p.getSourceIPAddress());
        System.out.println("  Dst IP : " + p.getDestinationIPAddress());
        System.out.println("  Data   : " + p.getData());
    }

    public static void sendDistanceVectors(HashMap<String, DistanceVector> routerTable, DatagramSocket socket, HashMap<String, Port> neighbors, String srcMac, Parser parser) throws IOException {
        for (String destMac : neighbors.keySet()) {
            if (destMac.charAt(0) == 'R') {
                Port outgoingPort = neighbors.get(destMac);
                StringBuilder data = new StringBuilder();

                for (String subnet : routerTable.keySet()) {
                    data.append(subnet).append(" ").append(routerTable.get(subnet).getDistance()).append(",");
                }
                String subnet = parser.getLinkSubnet(srcMac, destMac);
                String packet = "1:" + srcMac + ":" + destMac + ":" + subnet + "." + srcMac + ":" + subnet + "." + destMac + ":" + data;
                byte[] outBytes = packet.getBytes();
                DatagramPacket outgoingPacket =
                        new DatagramPacket(
                                outBytes,
                                outBytes.length,
                                outgoingPort.getIpAddress(),
                                outgoingPort.getUdpPort()
                        );
                socket.send(outgoingPacket);
                System.out.println("[ROUTER " + srcMac + "] Sent DV to " + destMac);
            }
        }
    }

}
