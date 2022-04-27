package server;

public class Store {
    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Wrong number of arguments. Please invoke the program as:");
            System.out.println("java Store <IP_mcast_addr> <IP_mcast_port> <node_id> <Store_port>");
            System.exit(1);
        }

        final String multicastIPAddr = args[0];
        final String multicastIPPort = args[1];
        final String nodeId = args[2];
        final String storePort = args[3];
    }
}
