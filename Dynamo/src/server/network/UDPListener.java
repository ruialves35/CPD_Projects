package server.network;

import common.Message;
import server.cluster.MembershipService;
import server.storage.StorageService;
import server.storage.TransferService;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.io.StringReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

public class UDPListener implements Runnable {
    private final StorageService storageService;
    private final MembershipService membershipService;
    private final TransferService transferService;
    private final ExecutorService executorService;

    public UDPListener(StorageService storageService, MembershipService membershipService, TransferService transferService,
                       ExecutorService executorService) {
        this.storageService = storageService;
        this.membershipService = membershipService;
        this.transferService = transferService;
        this.executorService = executorService;
    }

    public void run() {
        try {
            MulticastSocket socket = new MulticastSocket(this.membershipService.getMulticastIPPort());
            InetSocketAddress group = new InetSocketAddress(
                    this.membershipService.getMulticastIpAddr(),
                    this.membershipService.getMulticastIPPort());
            NetworkInterface netInf = NetworkInterface.getByIndex(0);
            socket.joinGroup(group, netInf);

            System.out.println("Listening UDP messages");
            while (true) {
                byte[] msg = new byte[Message.MAX_MSG_SIZE];
                DatagramPacket packet = new DatagramPacket(msg, msg.length);

                socket.receive(packet);

                if (!this.processEvent(packet))
                    break;
            }

            socket.leaveGroup(group, netInf);
            socket.close();
        } catch (IOException e) {
            System.out.println("Error opening UDP server");
            throw new RuntimeException(e);
        }
    }

    private boolean processEvent(DatagramPacket packet) {
        //System.out.println("Got Packet from :" + packet.getAddress());
        // TODO Parse message and generate event
        Message message = new Message(packet.getData());

        System.out.println("Received message from: \n" + message.getNodeId());
        System.out.println("-----------------");

        // ASSUMING THIS IS THE ONLY TYPE OF MESSAGE RECEIVED THROUGH UDP

        ByteBuffer bb = ByteBuffer.wrap(message.getBody());
        int membershipCounter = bb.getInt();
        System.out.println("GOT body: " + membershipCounter);

        this.membershipService.addLog(message.getNodeId(), membershipCounter);

        final int randomWait = new Random().nextInt(10);
        try {
            Thread.sleep(randomWait * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // TODO: send membership message

        return "end".equals(message);
    }
}
