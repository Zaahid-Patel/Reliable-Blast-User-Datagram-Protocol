package sender;
import java.net.*;
import java.util.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;

/**
 * Thread that listens for TCP connections
 */
public class SenderTCPListener extends Thread {
    
    int port;
	boolean running = true;
    String fileName;

    ArrayList<SenderTCP> connections = new ArrayList<SenderTCP>();

    public SenderTCPListener(int p, String name) {
        port = p;
        fileName = name;
    }

    @Override
    public void run() {
        // Opens a socket that listens for sender connections on [port]
        try (ServerSocketChannel socket = ServerSocketChannel.open()) {
            socket.socket().bind(new InetSocketAddress(port));
            socket.configureBlocking(false);
            while (running) {
                SocketChannel s = socket.accept();
                if (s != null) {
                    System.out.println("Sender Connected");

                    SenderTCP tcpSender = new SenderTCP(s, fileName);
                    tcpSender.start();
                    connections.add(tcpSender);
                }
            }
            socket.close();
            System.out.println("Server socket properly closed:" + !socket.isOpen());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public ArrayList<SenderTCP> getConnections() {
        return this.connections;
    }

}
