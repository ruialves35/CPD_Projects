package server;

import server.cluster.MembershipService;
import server.network.Message;
import server.network.Sender;
import server.network.TCPListener;
import server.network.UDPListener;
import server.storage.StorageService;
import server.storage.TransferService;

import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Store {
    public static void main(String[] args) {
        if (args.length < 4 || args.length > 5) {
            System.out.println("Wrong number of arguments. Please invoke the program as:");
            System.out.println("java Store <IP_mcast_addr> <IP_mcast_port> <node_id> <Store_port> [isRootNode]");
            System.exit(1);
        }

        final String multicastIPAddr = args[0];
        final int multicastIPPort = Integer.parseInt(args[1]);
        final String nodeId = args[2];
        final int storePort = Integer.parseInt(args[3]);

        boolean isRootNode = false;
        if (args.length == 5) {
            isRootNode = Boolean.parseBoolean(args[4]);
        }

        final MembershipService membershipService = new MembershipService(multicastIPAddr, multicastIPPort, nodeId, isRootNode);
        final StorageService storageService = new StorageService(membershipService.getNodeMap());
        final TransferService transferService = new TransferService(membershipService.getNodeMap());
        final ExecutorService executorService = Executors.newCachedThreadPool();

        executorService.submit(new TCPListener(storageService, membershipService, transferService, executorService, nodeId, storePort));

        if (membershipService.join()) {
            executorService.submit(new UDPListener(storageService, membershipService, transferService, executorService));

        }
    }
}

/*
* This is where a Service node is invoked. This class will act as a node in the server. Next steps is to find how multicast works
* and try to send a message to other service nodes?
* */