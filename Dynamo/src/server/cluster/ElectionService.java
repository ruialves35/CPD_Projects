package server.cluster;

import common.Message;
import common.MessageTypes;
import common.Sender;
import common.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ElectionService implements Runnable{
        final String folderPath;
        final String multicastIPAddr;
        final int multicastPort;

        final String nodeId;

        public ElectionService(String nodeId, String folderPath, String multicastIPAddr, int multicastPort) {
                this.folderPath = folderPath;
                this.multicastIPAddr = multicastIPAddr;
                this.multicastPort = multicastPort;
                this.nodeId = nodeId;
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

        @Override
        public void run() {
                try {
                        while (true) {
                                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                                String nodeIdLine = nodeId + Utils.newLine;
                                byteOut.write(nodeIdLine.getBytes(StandardCharsets.UTF_8));

                                byte[] electionBody = LogHandler.buildLogsBytes(this.folderPath);
                                byteOut.write(electionBody);

                                Message msg = new Message(MessageTypes.REQUEST.getCode(), MessageTypes.ELECTION_PING.getCode(), byteOut.toByteArray());

                                Sender.sendMulticast(msg.toBytes(), this.multicastIPAddr, this.multicastPort);

                                Thread.sleep(Utils.electionPingTime);
                                // TODO: Check if needs to detect any exception to stop
                        }
                } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                }

        }
}
