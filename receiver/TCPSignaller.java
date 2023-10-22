package receiver;
import java.net.*;
import java.util.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
/**
 * Thread used for signalling in the RBUDP protocol
 * Also used to send small important information eg. File Name; Receiver IP/Port
 */
public class TCPSignaller extends Thread {
    UDPReceiver udpReceiver;
    ByteBuffer receiverHostname;
    short receiverPort;

    String senderHostname;
    int senderPort;

    boolean running = true;

    int dataSize = 16382;

    List<byte[]> send = new ArrayList<byte[]>();
    public TCPSignaller(ByteBuffer rName, short rPort, String sName, short sPort) {
        receiverHostname = rName;
        receiverPort = rPort;

        senderHostname = sName;
        senderPort =sPort;
    }

    @Override
    public void run() {
        try {
            //Initial connection
            SocketChannel senderChannel = SocketChannel.open();
            senderChannel.configureBlocking(false);
            senderChannel.connect(new InetSocketAddress(senderHostname, senderPort));
            while (!senderChannel.finishConnect()) {}
            //Identifiers
            //0: first 4 bytes is ip; next 2 is port (short)
            //1: 4 bytes is number of sequence numbers; rest is FileName
            //2: ACK
            //3: 2 Bytes number of packets sent; rest list of sent sequence numbers
            //4: 2 Byted number of packets sent; List of received sequence numbers

            //identifier byte
            byte[] identifier = new byte[1];
            identifier[0] = 0;
            byte[] hostBytes = receiverHostname.array();
            
            ByteBuffer portBuf = ByteBuffer.allocate(2);
            portBuf.putShort(receiverPort);
            byte[] portBytes = portBuf.array();
            
            //Combine to send
            byte[] combined = new byte[7];
            System.arraycopy(identifier, 0, combined, 0, 1);
            System.arraycopy(hostBytes, 0, combined, 1, 4);
            System.arraycopy(portBytes, 0, combined, 5, 2);

            //send
            ByteBuffer writeBuffer = ByteBuffer.wrap(combined);
            while (writeBuffer.hasRemaining()) {
                senderChannel.write(writeBuffer);
            }

            while (running) {
                ByteBuffer buf = ByteBuffer.allocate(1024);
                int bytesRead = senderChannel.read(buf);

                if (send.size() > 0) {
                    //Send information
                    System.out.println("Sending information");
                    ByteBuffer sendBuffer = ByteBuffer.wrap(send.get(0));
                    while (sendBuffer.hasRemaining()) {
                        senderChannel.write(sendBuffer);
                    }
                    send.remove(0);
                } else {
                    //Receive information
                    if (bytesRead != 0) {
                        System.out.println("Receiving information");
                        buf.position(0);
                        byte incomingIdentifier = buf.get(0);

                        if (incomingIdentifier == 1) {
                            System.out.println("Code: 1");
                            byte[] sequenceNumBuffer = new byte[4];
                            byte[] fileNameBuffer = new byte[bytesRead - 5];
                            
                            buf.position(1);
                            buf.get(sequenceNumBuffer, 0, 4);
                            buf.position(5);
                            buf.get(fileNameBuffer, 0, bytesRead - 5);
    
                            ByteBuffer sequenceNumByteBuffer = ByteBuffer.wrap(sequenceNumBuffer);
                            int sequenceNum = sequenceNumByteBuffer.getInt();
                            String fileName = new String(fileNameBuffer);
                            udpReceiver.receiveFile(fileName, sequenceNum);
                        } else if (incomingIdentifier == 3) {
                            System.out.println("Code: 3");                        
                            short packetSizeShort = buf.getShort(1);

                            List<Short> seqNumList = new ArrayList<Short>();
  
                            for (int i = 3; i < 3 + packetSizeShort * 2; i = i + 2) {
                                seqNumList.add(buf.getShort(i));
                            }
                            udpReceiver.getNeededPackets(seqNumList);     
                        }
                    }
                }
            }
            senderChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stops this thread
     */
    public void stopRunning() {
        running = false;
    }

    /**
     * Sets the connection to the RBUDP's corresponding UDP receiver thread
     * @param receiver
     */
    public void setUDPReceiver(UDPReceiver receiver) {
        udpReceiver = receiver;
    }

    /**
     * Function called to signal the sender on the status of the receiver
     * @param set   True if the receiver is ready to receive UDP packets
     *              False if the receiver is already receiving a file
     */
    public synchronized void succFilenameSet(boolean set) {
        if (set) {
            byte[] ack = {2, 0};
            send.add(ack);
        } else {
            byte[] ack = {2, 1};
            send.add(ack);
        }
    }

    /**
     * Function called to send a list of packets that still needs to be sent.
     * PLEASE NOTE: It send the full list, regardless of if it was already sent or not
     * @param needed The list of sequence numbers that will be sent to the sender
     */
    public synchronized void sendSequenceNumbersNeeded(List<Short> needed) {
        byte[] identifier = new byte[1];
        identifier[0] = 4;

        ByteBuffer buf = ByteBuffer.allocate(needed.size() * Short.BYTES);

        for (short i:needed) {
            buf.putShort(i);
        }

        byte[] byteArray = buf.array();

        short size = (short)needed.size();
        ByteBuffer sizeBuf = ByteBuffer.allocate(2);
        sizeBuf.putShort(size);

        byte[] sizeByte = sizeBuf.array();

        byte[] combined = new byte[byteArray.length + 1 + 2];
        System.arraycopy(identifier, 0, combined, 0, 1);
        System.arraycopy(sizeByte, 0, combined, 1, 2);
        System.arraycopy(byteArray, 0, combined, 3, byteArray.length);

        send.add(combined);
    }
}
