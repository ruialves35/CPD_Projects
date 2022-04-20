package client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 5500);
            DataOutputStream stream = new DataOutputStream(socket.getOutputStream());

            stream.writeUTF("Boas mano ta tudo");

            stream.flush();
            stream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
