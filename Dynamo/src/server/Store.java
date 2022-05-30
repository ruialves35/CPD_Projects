package server;

import server.cluster.MembershipService;
import server.cluster.Node;
import server.network.TCPListener;
import server.network.UDPListener;
import server.storage.StorageService;
import server.storage.TransferService;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

        final MembershipService membershipService = new MembershipService(multicastIPAddr, multicastIPPort, nodeId,
                storePort);
        final StorageService storageService = new StorageService(membershipService.getNodeMap(), nodeId);
        final TransferService transferService = new TransferService(membershipService.getNodeMap(), storageService, new Node(nodeId, storePort));
        final ExecutorService executorService = Executors.newCachedThreadPool();

        try {
            executorService.submit(new TCPListener(storageService, membershipService, transferService, executorService,
                    nodeId, storePort));

            membershipService.join();
            transferService.join();
            executorService
                    .submit(new UDPListener(storageService, membershipService, transferService, executorService));
        } catch (RuntimeException re) {
            System.err.println(re.getMessage());
            // Clear ExecutorService threads?
        }

        // THIS IS FOR TESTING THE LEAVE
        Scanner myObj = new Scanner(System.in); // Create a Scanner object
        System.out.println("> Enter action");

        String action = myObj.nextLine(); // Read user input
        if (action.equals("leave")) {
            try {
                membershipService.leave();
            } catch (RuntimeException re) {
                System.err.println(re.getMessage());
                // Clear ExecutorService threads?
            }
        }
    }
}
