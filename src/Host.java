import java.util.Scanner;
import java.net.*;
import java.util.HashMap;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Host {
    private String macAddress;
    private InetAddress ipAddress;
    private int port;
    private DatagramSocket socket;
    private HashMap<String, Port> neighbors;
    private ExecutorService executorService;

    public Host(String macAddress, File config) throws UnknownHostException, FileNotFoundException {
        this.macAddress = macAddress;
        Parser parser = new Parser(config);
        Port portInfo = parser.parseMac(macAddress);
        this.ipAddress = (portInfo.getIpAddress());
        this.port = (portInfo.getUdpPort());
        this.neighbors = parser.getNeighbors(macAddress);
    }
    public void start() throws IOException {
        socket = new DatagramSocket(port);
        executorService = Executors.newCachedThreadPool();
        System.out.println("Host " + macAddress + " started at " + ipAddress.getHostAddress() + ":" + port);
        System.out.println("MAC: " + macAddress);
        System.out.println("Connected to: " + neighbors.keySet());

        executorService.submit(() -> {
            while (!socket.isClosed()) {
                try {
                    byte[] buffer = new byte[1024];
                    DatagramPacket recvPacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(recvPacket);
                    String frame = new String(recvPacket.getData(), 0, recvPacket.getLength());
                    Packet p = new Packet(frame);

                    if (p.getDestinationMacAddress().equals(macAddress)) {
                        System.out.println("\n[" + macAddress + "] Received:");
                        System.out.println("  Src: " + p.getSourceMacAddress());
                        System.out.println("  Dst: " + p.getDestinationMacAddress());
                        System.out.println("  SrcIP: " + p.getSourceIPAddress());
                        System.out.println("  DstIP: " + p.getDestinationIPAddress());
                        System.out.println("  Data: " + p.getData());
                    } else {
                        wrongMAC(p);

                    }
                } catch (IOException e) {
                    if (!socket.isClosed()) {
                        System.err.println("Error receiving: " + e.getMessage());
                    }
                }
            }
        });
    }

    public void send(String data, String destinationMac) {
        executorService.submit(() -> {
            try {
                String virtualSrcIP;
                String virtualDstIP;
                String gatewayMac = null;
                try {
                    Parser parser = new Parser(new File("src/config.txt"));
                    virtualSrcIP = parser.getVirtualIP(this.macAddress);
                    virtualDstIP = parser.getVirtualIP(destinationMac);
                    if (!sameSubnet(virtualSrcIP, virtualDstIP)) {
                        gatewayMac = parser.getGateway(this.macAddress);
                        if (gatewayMac != null) {
                            System.out.println("  [Routing to " + destinationMac + " via gateway " + gatewayMac + "]");
                        }
                    }
                } catch (Exception e) {
                    virtualSrcIP = "0.0.0.0";
                    virtualDstIP = "0.0.0.0";
                }

                String frameDstMac = (gatewayMac != null) ? gatewayMac : destinationMac;
                String frame = "0:" + this.macAddress + ":" + frameDstMac + ":" + virtualSrcIP + ":" + virtualDstIP + ":" + data;
                Packet packet = new Packet(frame);
                System.out.println("[" + macAddress + "] Sending:");
                System.out.println("  Src: " + packet.getSourceMacAddress());
                System.out.println("  Dst: " + packet.getDestinationMacAddress());
                System.out.println("  SrcIP: " + packet.getSourceIPAddress());
                System.out.println("  DstIP: " + packet.getDestinationIPAddress());
                System.out.println("  Data: " + packet.getData());

                for (Port neighborPort : neighbors.values()) {
                    byte[] sendData = frame.getBytes();
                    InetAddress destAddress = neighborPort.getIpAddress();
                    int destPort = neighborPort.getUdpPort();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, destAddress, destPort);
                    socket.send(sendPacket);
                    System.out.println("  -> " + destAddress.getHostAddress() + ":" + destPort);
                    break;
                }
            } catch (IOException e) {
                System.err.println("Error sending: " + e.getMessage());
            }
        });
    }

    public void close() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    private void wrongMAC(Packet p) {
        System.err.println("[" + macAddress + "] Packet dropped — destination " + p.getDestinationMacAddress() + " does not match MAC. Must be a flooded frame ");
    }

    private static boolean sameSubnet(String srcIP, String dstIP) {
        String srcNet = srcIP.contains(".") ? srcIP.substring(0, srcIP.indexOf('.')) : srcIP;
        String dstNet = dstIP.contains(".") ? dstIP.substring(0, dstIP.indexOf('.')) : dstIP;
        return srcNet.equals(dstNet);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java Host <MAC_ADDRESS>");
            return;
        }

        String macAddress = args[0];
        File config = new File("Project 2/src/config.txt");
        Host host = new Host(macAddress, config);
        host.start();

        Scanner keyboard = new Scanner(System.in);
        while (true) {
            System.out.println("\n1. Send\n2. Exit");
            System.out.print("Choice: ");
            String choice = keyboard.nextLine();

            if (choice.equals("1")) {
                System.out.print("Destination (A/B/C/D): ");
                String dest = keyboard.nextLine().toUpperCase();
                System.out.print("Message: ");
                String msg = keyboard.nextLine();
                host.send(msg, dest);
            } else if (choice.equals("2")) {
                host.close();
                break;
            }
        }
        keyboard.close();
    }
}
