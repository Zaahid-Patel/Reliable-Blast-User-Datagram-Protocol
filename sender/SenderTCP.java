package sender;
import java.net.*;
import java.util.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;

/**
 * Thread that implements signalling in RBUDP
 */
public class SenderTCP extends Thread {
    
    String recieverName;
    int recieverPort;

    SocketChannel socket;
    int dataSize = 16384;

	boolean running = true;

    String fileName;
    FileInputStream file;
    int fileSize;
    int bytesSent = 0;
 
    SenderTCP(SocketChannel s, String fname) {
        socket = s;

        try {
            fileName = fname;
            System.out.println(fileName);
            file = new FileInputStream(fileName);
            fileSize = (int)file.getChannel().size();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            socket.configureBlocking(true);
            while (!socket.finishConnect()) {}

            sendMetadata(getPacketsToSend(), fileName);

            while (running) {
                byte[] buffer = new byte[dataSize];

                int bufferSize = file.read(buffer);

                if (bufferSize == -1) {
                    break;
                }

                ByteBuffer message = ByteBuffer.allocate(bufferSize);
                message.position(0);
                message.put(buffer, 0, bufferSize);
                message.flip();

                bytesSent += socket.write(message);
            }
            System.out.println("Closing channel");
            file.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send filename and number of packets to receiver
     * @param numPackets Number of packets the file will be divided into
     * @param name Filename
     */
    public void sendMetadata(int numPackets, String name) {
        System.out.println("Sending filename and number of packets");
        ByteBuffer message = ByteBuffer.allocate(4 + name.length());
        message.putInt(numPackets);
        message.put(name.getBytes());
        message.flip();
        // System.out.println(Arrays.toString(message.array()));
        try {
            // System.out.println(Arrays.toString(message.array()));
            socket.write(message);
        } catch (IOException e) {
            // TODO: handle exception
        }
    }

    public int getFileSize() {
        return fileSize;
    }

    public int getBytesSent() {
        return bytesSent;
    }

    public int getPacketsToSend() {
        if ((int)Math.ceil(fileSize/dataSize) == 0) {
            return (int)Math.ceil(fileSize/dataSize) + 1;
        } else {
            return (int)Math.ceil(fileSize/dataSize);
        }
        
    }
}
