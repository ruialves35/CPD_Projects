package server;

import server.cluster.MembershipService;
import server.cluster.Node;
import server.network.TCPListener;
import server.network.UDPListener;
import server.storage.StorageService;
import server.storage.TombstoneManager;
import server.storage.TransferService;

import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Store implements Server{
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

        final ExecutorService executorService = Executors.newCachedThreadPool();

        final MembershipService membershipService = new MembershipService(multicastIPAddr, multicastIPPort, nodeId,
                storePort);
        final StorageService storageService = new StorageService(membershipService.getNodeMap(), nodeId, executorService);
        final TransferService transferService = new TransferService(membershipService.getNodeMap(), storageService, new Node(nodeId, storePort));
        final ExecutorService executorService = Executors.newCachedThreadPool();     // TODO: CHANGED TO newFixedThreadPool?

        ServerSocket serverSocket = null;
        MulticastSocket multicastSocket = null;
        try {
            // THERE ARE 2 SOLUTIONS: THIS ONE or LEAVING THE THREADS RUNNING BUT NOT PROCESSING THE EVENTS
            try {
                InetAddress addr = InetAddress.getByName(nodeId);
                serverSocket = new ServerSocket(storePort, 50, addr);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            executorService.submit(new TCPListener(storageService, membershipService, transferService, executorService, serverSocket));


            membershipService.join();
            transferService.join();

            try {
                multicastSocket = new MulticastSocket(multicastIPPort);
                executorService.submit(new UDPListener(storageService, membershipService, transferService, executorService, multicastSocket));
                executorService.submit(new TombstoneManager(storageService.getDbFolder()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch(RuntimeException re) {
            System.err.println(re.getMessage());
            // Clear ExecutorService threads?
        }

        // THIS IS FOR TESTING THE LEAVE
        Scanner myObj = new Scanner(System.in); // Create a Scanner object
        System.out.println("> Enter action");

        String action = myObj.nextLine(); // Read user input
        if (action.equals("leave")) {
            try {
                try {
                    if (serverSocket != null) serverSocket.close();
                    if (multicastSocket != null) {
                        InetSocketAddress group = new InetSocketAddress(multicastIPAddr, multicastIPPort);
                        NetworkInterface netInf = NetworkInterface.getByIndex(0);
                        multicastSocket.leaveGroup(group, netInf);
                        multicastSocket.close();
                    }
                    executorService.shutdownNow();
                    if (executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                        System.out.println("Executor terminated.");
                    } else {
                        System.out.println("Executor still running");
                    }
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
                transferService.leave();
                membershipService.leave();
            } catch (RuntimeException re) {
                System.err.println(re.getMessage());
                // Clear ExecutorService threads?
            }
        }
    }

    @Override
    public boolean join() throws RemoteException {
        return false;
    }

    @Override
    public boolean leave() throws RemoteException {
        return false;
    }

    @Override
    public boolean put(String filePath) throws RemoteException {
        return false;
    }

    @Override
    public boolean get(String fileKey) throws RemoteException {
        return false;
    }

    @Override
    public boolean delete(String fileKey) throws RemoteException {
        return false;
    }
}
