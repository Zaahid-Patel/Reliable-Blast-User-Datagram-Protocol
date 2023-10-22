package sender;
import java.net.*;
import java.util.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;

/**
 * Thread that implements signalling of RBUDP protocol
 */
public class SenderTCPSignaller extends Thread {
    
    InetSocketAddress receiverAddress;

    SocketChannel socket;
    SenderUDP senderudp;

	boolean running = true;
    boolean haveReceiverAddress = false;

    String fileName;

    SenderTCPSignaller(SocketChannel s, String name) {
        socket = s;
        fileName = name;
    }

    @Override
    public void run() {
        try {
            socket.configureBlocking(false);
            while (!socket.finishConnect()) {}

            while (running) {
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                int bytesRead = socket.read(buffer);

                if (bytesRead != 0) {
                    // Operation code
                    int code = buffer.get(0); 
                    // System.out.println(Arrays.toString(buffer.array()));
                    if (code == 0) {
                        byte[] ip = new byte[4];
                        buffer.position(1);
                        buffer.get(ip, 0, 4);
                        short port = buffer.getShort(5);
                        // System.out.println("Ip: " + Arrays.toString(ip) + " port: " + port);
                        receiverAddress = new 
                                InetSocketAddress(InetAddress.getByAddress(ip),
                                                  port);
                        
                        senderudp = new SenderUDP(fileName, receiverAddress, this);
                        haveReceiverAddress = true;
                        sendMetadata(senderudp.getPacketsToSend(), fileName);
                    }
                    if (code == 2) {
                        //ACK
                        System.out.println("Code: 2");
                        byte isReady = buffer.get(1);
                        if (isReady == 0) {
                            senderudp.start();
                            short[] empty = new short[0];
                            senderudp.send(empty);
                        }
                    }
                    if (code == 4) {
                        System.out.println("Code: 4");
                        short numPacketsToResend = buffer.getShort(1);
                        System.out.println(numPacketsToResend);
                        short[] resendPackets = new short[numPacketsToResend];
                        // byte[] packetsToResend = new byte[numPacketsToResend];
                        // buffer.get(packetsToResend, 0, numPacketsToResend);
                        int count = 3;
                        // System.out.println(Arrays.toString(buffer.array()));
                        for (int i = 0; i < numPacketsToResend && count + 1 < buffer.capacity(); i++) {
                            resendPackets[i] = buffer.getShort(count);
                            count += 2;
                        }
                        System.out.println(Arrays.toString(resendPackets));
                        senderudp.send(resendPackets);
                    }
                }
            }

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
        System.out.println("sending filename and number of packets");
        ByteBuffer message = ByteBuffer.allocate(5 + name.length());
        byte identifier = 1;
        // System.out.println(numPackets);
        message.put(identifier);
        message.putInt(numPackets);
        //FIXME
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

    /**
     * Send a list of packet numbers sent
     * @param packetsSent List of sent packets
     */
    public synchronized void sendPacketsSent(short[] packetsSent) {
        System.out.println("SendPacketsSent: " + packetsSent.length);
        ByteBuffer message = ByteBuffer.allocate(3+2*packetsSent.length);
        byte identifier = 3;
        message.put(identifier);
        message.putShort((short)packetsSent.length);

        for (short value : packetsSent) {
            message.putShort(value);
        }
        
        // message.put(packetsSent);
        //FIXME
        try {
            message.position(0);
            System.out.println(Arrays.toString(message.array()));
            // System.out.println(Arrays.toString(message.array()));
            socket.write(message);
        } catch (IOException e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        
    }

    public int getPacketsSent() {
        return senderudp.getPacketsSentSuccessfully();
    }

    public int getNumPackets() {
        return senderudp.getPacketsToSend();
    }

}
