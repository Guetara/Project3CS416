import java.io.File;
import java.io.FileNotFoundException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Scanner;
import java.net.InetAddress;

public class Parser {
    File config;

    public Parser (File config){
        this.config = config;
    }

    public String parseHostOrRouter (String macAddress) throws FileNotFoundException, UnknownHostException {
        Scanner fileReader = new Scanner(config);
        boolean notMac = true;

        while(notMac) {
            String line = fileReader.nextLine();
            String[] sectionedLine = line.split(" ");
            if(sectionedLine[0].equals(macAddress)) {
                notMac = false;
                return line;
            }
        }

        return "";
    }


    public Port parseMac (String macAddress) throws FileNotFoundException, UnknownHostException {
        Scanner fileReader = new Scanner(config);
        boolean notMac = true;
        Port port = new Port();

        while(notMac) {
            String line = fileReader.nextLine();
            String[] sectionedLine = line.split(" ");
            if (sectionedLine[0].equals(macAddress)) {
                notMac = false;
                port = new Port(Integer.parseInt(sectionedLine[1]), InetAddress.getByName(sectionedLine[2]));
            }
        }

        return port;
    }

    public HashMap<String, Port> getNeighbors (String macAddress) throws FileNotFoundException, UnknownHostException {
        Scanner fileReader = new Scanner(config);
        boolean linksSection = false;
        HashMap<String, Port> neighbors = new HashMap<>();

        while(fileReader.hasNextLine()) {
            String line = fileReader.nextLine();
            if (linksSection) {
                String[] macAddresses = line.split(":");
                if (macAddresses[0].equals(macAddress)) {
                    Port neighborPort = parseMac(macAddresses[1]);
                    neighbors.put(macAddresses[1], neighborPort);
                }
                else if (macAddresses[1].equals(macAddress)){
                    Port neighborPort = parseMac(macAddresses[0]);
                    neighbors.put(macAddresses[0], neighborPort);
                }
            }
            if (line.equals("Links")) {
                linksSection = true;
            }
        }

        return neighbors;
    }

    public String getVirtualIP (String macAddress) throws FileNotFoundException {
        Scanner fileReader = new Scanner(config);

        while(fileReader.hasNextLine()) {
            String line = fileReader.nextLine();
            String[] sectionedLine = line.split(" ");
            if (sectionedLine[0].equals(macAddress)) {
                fileReader.close();
                return sectionedLine[3];
            }
        }
        fileReader.close();
        return null;
    }

    public String getGateway(String macAddress) throws FileNotFoundException {
        Scanner fileReader = new Scanner(config);

        while(fileReader.hasNextLine()) {
            String line = fileReader.nextLine();
            String[] sectionedLine = line.split(" ");
            if (sectionedLine[0].equals(macAddress) && sectionedLine.length > 4) {
                fileReader.close();
                String gateway = sectionedLine[4];
                if (gateway.contains(".")) {
                    return gateway.substring(gateway.lastIndexOf('.') + 1);
                }
                return gateway;
            }
        }
        fileReader.close();
        return null;
    }

    public HashMap<String, DistanceVector> getInitialRouterTable (String macAddress) throws FileNotFoundException, UnknownHostException {
        Scanner fileReader = new Scanner(config);
        boolean linksSection = false;
        HashMap<String, DistanceVector> initialRouterTable = new HashMap<>();

        while(fileReader.hasNextLine()) {
            String line = fileReader.nextLine();
            if (linksSection) {
                String[] macAddressesAndSubnet = line.split(":");
                if (macAddressesAndSubnet[0].equals(macAddress)) {
                    Port neighborPort = parseMac(macAddressesAndSubnet[1]);
                    DistanceVector distanceVector = new DistanceVector(1, neighborPort.getFullPort());
                    initialRouterTable.put(macAddressesAndSubnet[2], distanceVector);
                }
                else if (macAddressesAndSubnet[1].equals(macAddress)) {
                    Port neighborPort = parseMac(macAddressesAndSubnet[0]);
                    DistanceVector distanceVector = new DistanceVector(1, neighborPort.getFullPort());
                    initialRouterTable.put(macAddressesAndSubnet[2], distanceVector);
                }
            }
            if (line.equals("Links")) {
                linksSection = true;
            }
        }

        return initialRouterTable;
    }
}
