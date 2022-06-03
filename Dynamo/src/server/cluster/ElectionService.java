package server.cluster;

import common.Message;
import common.MessageTypes;
import common.Sender;
import common.Utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class ElectionService implements Runnable{
        private final String folderPath;
        private final String multicastIPAddr;
        private final int multicastPort;

        private final String nodeId;

        private final TreeMap<String, Node> nodeMap;

        public ElectionService(String nodeId, String folderPath, String multicastIPAddr, int multicastPort, TreeMap<String, Node> nodeMap) {
                this.folderPath = folderPath;
                this.multicastIPAddr = multicastIPAddr;
                this.multicastPort = multicastPort;
                this.nodeId = nodeId;
                this.nodeMap = nodeMap;
        }

        public static void sendRequest(String nodeId, Node nextNode) {
                if (nextNode == null) return;

                System.out.printf("Sending Election Request to %s...\n", nextNode.getId());
                Path path = Paths.get(Utils.generateFolderPath(nodeId) + Utils.membershipLogFileName);

                try {
                        byte[] fileData = Files.readAllBytes(path);
                        String nodeIdLine = nodeId + Utils.newLine;

                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        out.write(nodeIdLine.getBytes(StandardCharsets.UTF_8));
                        out.write(fileData);

                        Message electionMessage = new Message(MessageTypes.REQUEST.getCode(), MessageTypes.ELECTION_REQUEST.getCode(), out.toByteArray());
                        Sender.sendTCPMessage(electionMessage.toBytes(), nextNode.getId(), nextNode.getPort());
                } catch (IOException e) {
                        throw new RuntimeException(e);
                }
        }

        public static void propagateRequest(Message message, Node nextNode) {
                if (nextNode == null) return;

                System.out.printf("Propagating Election Request to %s...\n", nextNode.getId());
                try {
                        Sender.sendTCPMessage(message.toBytes(), nextNode.getId(), nextNode.getPort());
                } catch (IOException e) {
                        throw new RuntimeException(e);
                }
        }

        /**
         * If the leader leaves the cluster, it will send a leave request to the next Node so that it starts an election process.
         * @param nodeId
         * @param nextNode
         */
        public static void sendLeave(String nodeId, Node nextNode, byte[] leaveBody) {
                if (nextNode == null) return;

                System.out.printf("Sending Election Leave Request to %s:%d...\n", nextNode.getId(), nextNode.getPort());

                try {
                        Message electionMessage = new Message(MessageTypes.REQUEST.getCode(), MessageTypes.ELECTION_LEAVE.getCode(),leaveBody);
                        Sender.sendTCPMessage(electionMessage.toBytes(), nextNode.getId(), nextNode.getPort());
                } catch (IOException e) {
                        throw new RuntimeException(e);
                }
        }

        @Override
        public void run() {
                try {
                        while (true) {
                                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                                String nodeIdLine = nodeId + Utils.newLine;
                                byteOut.write(nodeIdLine.getBytes(StandardCharsets.UTF_8));

                                byte[] electionBody = LogHandler.buildLogsBytes(this.folderPath, this.nodeMap);
                                byteOut.write(electionBody);

                                Message msg = new Message(MessageTypes.REQUEST.getCode(), MessageTypes.ELECTION_PING.getCode(), byteOut.toByteArray());

                                Sender.sendMulticast(msg.toBytes(), this.multicastIPAddr, this.multicastPort);

                                Thread.sleep(Utils.electionPingTime);
                                // TODO: Check if needs to detect any exception to stop
                        }
                } catch (InterruptedException e) {
                        System.out.println("Node stopped being a leader.");
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }
}
