package receiver;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;

/**
 * Thread that receives a file over a TCP connection with a sender application
 */

public class TCPReceiver extends Thread {
    String filePath = "received_files";

    boolean running = true;
    String senderHostname;
    short senderPort;

    int expectedBytes = 0;
    int totalBytesRead = 0;
    int dataSize = 16384;

    public TCPReceiver(String sName, short sPort) {
        senderHostname = sName;
        senderPort = sPort;
    }

    @Override
    public void run() {
        try {
            //Initial setup
            SocketChannel senderChannel = SocketChannel.open();
            senderChannel.configureBlocking(true);
            senderChannel.connect(new InetSocketAddress(senderHostname, senderPort));
            while (!senderChannel.finishConnect()) {
            }

             //Get total bytes expected from file
            ByteBuffer buf = ByteBuffer.allocate(dataSize);
            int bytesRead = senderChannel.read(buf);
             
            byte[] sequenceNumBuffer = new byte[4];
            byte[] fileNameBuffer = new byte[bytesRead - 4];
            
            buf.position(0);
            buf.get(sequenceNumBuffer, 0, 4);
            buf.position(4);
            buf.get(fileNameBuffer, 0, bytesRead - 4);

            ByteBuffer sequenceNumByteBuffer = ByteBuffer.wrap(sequenceNumBuffer);
            expectedBytes = sequenceNumByteBuffer.getInt();
            String fileName = new String(fileNameBuffer);

            String[] newFileName;
            if (fileName.contains("/")) {
                newFileName = fileName.split("/");
                filePath = "received_files/";
            } else if (fileName.contains("\\")) {
                newFileName = fileName.split("\\\\");
                filePath = "received_files\\";
            } else {
                newFileName = new String[1];
                newFileName[0] = fileName;
                filePath = "received_files/";
            }

            System.out.println("Filename: " + fileName);
            senderChannel.configureBlocking(false);
            FileChannel fileChannel = FileChannel.open(Paths.get(filePath + newFileName[newFileName.length - 1]), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            while (running) {
                ByteBuffer bufFile = ByteBuffer.allocate(dataSize);
                int bytesReadFile = senderChannel.read(bufFile);

                if (bytesReadFile != 0) {
                    addBytes(bytesReadFile);
                    bufFile.flip();
                    fileChannel.write(bufFile);
                    bufFile.clear();
                    if (bytesReadFile == -1) {
                        System.out.println("got to end");
                        break;
                    }
                }
            }
            fileChannel.close();
            senderChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add byte amount that was received to the total bytes received
     * @param read
     */
    void addBytes(int read) {
        totalBytesRead = totalBytesRead + read;
    }

    /**
     * Returns the Total bytes that the file consists of
     * @return
     */
    public synchronized int getExpectedBytes() {
        return expectedBytes;
    }

    /**
     * Returns Total bytes that were received
     * @return
     */
    public synchronized int getTotalBytesRead() {
        return totalBytesRead;
    }

    /**
     * Stops thread from running
     */
    public synchronized void stopRunning() {
        running = false;
    }

}
