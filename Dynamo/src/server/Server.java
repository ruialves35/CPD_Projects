package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Server extends Remote {
    boolean join() throws RemoteException;
    boolean leave() throws RemoteException;

    String put(String filePath) throws RemoteException;
    byte[] get(String fileKey) throws RemoteException;
    boolean delete(String fileKey) throws RemoteException;
}
