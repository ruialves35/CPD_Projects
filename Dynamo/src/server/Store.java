package server;

import server.cluster.MembershipService;
import server.storage.StorageService;
import server.storage.TransferService;

public class Store {
    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Wrong number of arguments. Please invoke the program as:");
            System.out.println("java Store <IP_mcast_addr> <IP_mcast_port> <node_id> <Store_port>");
            System.exit(1);
        }

        final String multicastIPAddr = args[0];
        final int multicastIPPort = Integer.parseInt(args[1]);
        final String nodeId = args[2];
        final int storePort = Integer.parseInt(args[3]);

        final MembershipService membershipService = new MembershipService(multicastIPAddr, multicastIPPort, nodeId);
        final StorageService storageService = new StorageService(membershipService.getNodeMap());
        final TransferService transferService = new TransferService(membershipService.getNodeMap());
    }
}

/*
* This is where a Service node is invoked. This class will act as a node in the server. Next steps is to find how multicast works
* and try to send a message to other service nodes?
* */