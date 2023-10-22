package sender;
import java.net.*;
import java.util.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;

/**
 * Thread that listens for TCP connections
 */
public class SenderTCPSignalListener extends Thread {
    
    int port;
	boolean running = true;
    String fileName;

    ArrayList<SenderTCPSignaller> connections = new ArrayList<SenderTCPSignaller>();

    public SenderTCPSignalListener(int p, String name) {
        port = p;
        fileName = name;
    }

    @Override
    public void run() {
        // Opens a socket that listens for sender connections on [port]
        
        try (ServerSocketChannel socket = ServerSocketChannel.open()) {
            
            socket.socket().bind(new InetSocketAddress(port));
            System.out.println(InetAddress.getLocalHost());
            socket.configureBlocking(false);
            while (running) {
                SocketChannel s = socket.accept();
                if (s != null) {
                    System.out.println("Sender Connected");
                    SenderTCPSignaller tcpSignaller = new SenderTCPSignaller(s,
                                                                             fileName);
                    tcpSignaller.start();
                    connections.add(tcpSignaller);
                }
            }
            socket.close();
            System.out.println("Server socket properly closed:" + !socket.isOpen());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public ArrayList<SenderTCPSignaller> getConnections() {
        return this.connections;
    }

}
