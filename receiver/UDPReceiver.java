package receiver;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.net.*;
import java.util.*;
/**
 * Thread that receives a file over a UDP connection
 * Also contains algorithms used for the RBUDP protocol
 */
public class UDPReceiver extends Thread {
    TCPSignaller tcpSignaller;

    String hostname;
    short udpPort;
    boolean running = true;

    String fileName = "";
    String pathOut = "received_files";
    int expectedPacketCount = -1;
    boolean newFile = false;

    boolean drop = false;
    List<Short> dropPacketSeqNum = new ArrayList<Short>();    
    List<Short> listOfAllPacketsAskedToResend = new ArrayList<Short>();
    int dataSize = 16382;
    Map<Short, ByteBuffer> fileDictionary = new TreeMap<Short, ByteBuffer>();
    List<Short> seqNumbersReceived = new ArrayList<Short>();

    int packetsReceived;
    int interval = 5;
    int intervalCount = 0;
    boolean ready = false;
    public UDPReceiver(String host, short port) {
        hostname = host;
        udpPort = port;

        System.out.println("Hostname: " + host + "port: " + port);
    }

    @Override
    public void run() {
        try {
            DatagramChannel udpChannel = DatagramChannel.open();
            udpChannel.configureBlocking(false);
            udpChannel.bind(new InetSocketAddress(hostname, udpPort));
            while (running) {
                ByteBuffer buf = ByteBuffer.allocate(dataSize + 4);
                buf.rewind();
                SocketAddress address = udpChannel.receive(buf);
                if (buf.position() != 0) {
                    System.out.println(buf.position());
                }

                if (getNewFile()) {
                    if (!getReady()) {
                        tcpSignaller.succFilenameSet(true);
                        ready = true;
                    }
                    
                    if (checkAllDataReceived()) {
                        System.out.println("All data received");
                        createFile();
                        newFile = false;

                    } else if (address != null) {
                        buf.flip();
                        System.out.println("Getting info: " + getTotalPacketsReceived());

                        short seqNumShort = buf.getShort(0);
                        System.out.println("recieved seq num: " + seqNumShort);

                        short length = buf.getShort(2);

                        byte[] data = new byte[length];
                        buf.position(4);
                        buf.get(data, 0, length);
                        ByteBuffer dataBuffer = ByteBuffer.wrap(data);
                
                        if (fileDictionary.get(seqNumShort) == null) {
                            fileDictionary.put(seqNumShort, dataBuffer);
                            seqNumbersReceived.add(seqNumShort);
                            dropPackets(seqNumShort);
                        }
                    } else {
                        List<Short> neededPackets = new ArrayList<Short>();
                        for (short i = 0; i < expectedPacketCount; i++) {
                            if (!seqNumbersReceived.contains(i)) {
                                neededPackets.add(i);
                            }
                        }
                    }
                    
                }
                
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns whether or not the thread is ready to receive a new file
     * @return returns ready
     */
    synchronized boolean getReady() {
        return ready;
    }
    /**
     * Sets the connection to the RBUDP's corresponding TCP signaller thread
     * @param signaller TCP signaller thread
     */
    public void setTCPSignaller(TCPSignaller signaller) {
        tcpSignaller = signaller;
    }

    /**
     * Stops this thread
     */
    public void stopRunning() {
        running = false;
    }

    /**
     * Checks if all packets have been sent
     * If not, it makes sure the list of needed packets is still correct
     * @return Returns true if all packets have been sent
     */
    public boolean checkAllDataReceived() {
        if (fileDictionary.size() > expectedPacketCount) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Creates the file from the sequence of packets that was sent from sender
     */
    public void createFile() {
        String[] newFileName;
        if (fileName.contains("/")) {
            newFileName = fileName.split("/");
            pathOut = "received_files/";
        } else if (fileName.contains("\\")) {
            System.out.println("file: " +fileName);
            newFileName = fileName.split("\\\\");
            pathOut = "received_files\\";
        } else {
            newFileName = new String[1];
            newFileName[0] = fileName;
        }
        
        try (FileOutputStream fos = new FileOutputStream(pathOut + newFileName[newFileName.length - 1]); FileChannel channel = fos.getChannel()) {
            for (short key : fileDictionary.keySet()) {
                ByteBuffer currentData = fileDictionary.get(key);
                System.out.println("Piece " + key + ": " + fileDictionary.get(key));
                currentData.position(0);
                channel.write(currentData);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cleans all data in preperation for next file to be received
     */
    public void clearData() {
        seqNumbersReceived.clear();
        fileDictionary = new TreeMap<Short, ByteBuffer>();
        fileName = "";
        expectedPacketCount = -1;

        ready = false;

        newFile = false;
    }

    /**
     * Returns whether or not A file is currently being received
     * @return
     */
    synchronized boolean getNewFile() {
        return newFile;
    }

    /**
     * Set by the signaller
     * Sets the incoming file's name and packet count, and starts listening for packets
     * @param name
     * @param packetCount
     */
    public synchronized void receiveFile(String name, int packetCount) {
        clearData();
        System.out.println("Start listening for udp signals");
        if (!newFile) {
            fileName = name;
            expectedPacketCount = packetCount;
            newFile = true;
            
        } else {
            tcpSignaller.succFilenameSet(false);
        }
    }

    /**
     * Gets total file packets
     * @return Returns total packets that are expected to be sent to receiver
     */
    public synchronized int getExpectedPacketsCount() {
        return expectedPacketCount;
    }

    /**
     * Gets current amount of packets that are received
     * @return Returns current amout of packets that are received
     */
    public synchronized int getTotalPacketsReceived() {
        return fileDictionary.size();
    }

    /**
     * Sends list of only needed packets
     * @param sent packets that were sent by sender
     */
    public synchronized void getNeededPackets(List<Short> sent) {
        List<Short> sequenceNumbersNeedToResend = new ArrayList<Short>();

        if (newFile) {
            for (int i = 0; i < sent.size(); i++) {
                //If there is a sequence Number that was sent that is on the list of sequence Numbers that are still needed; 
                //Add that sequence number to the sequence numbers that need to be resent
                if (!seqNumbersReceived.contains(sent.get(i))) {
                    sequenceNumbersNeedToResend.add(sent.get(i));
                    listOfAllPacketsAskedToResend.add(sent.get(i));
                }    
            }
        } else {
            System.out.println("ended");
        }

        System.out.println("All asked to resend: " +listOfAllPacketsAskedToResend.size());
        tcpSignaller.sendSequenceNumbersNeeded(sequenceNumbersNeedToResend);
    }

    public synchronized void setDropPackets(boolean set, int amount) {
        if (set) {
            drop = true;
            dropPacketSeqNum.clear();
            Random random = new Random();
            
            for (int i = 0; i < amount; i++) {
                short dropRandom = (short) random.nextInt(amount);
                dropPacketSeqNum.add(dropRandom);
            }

            System.out.println("drop packets: " + Arrays.toString(dropPacketSeqNum.toArray()));
        } else {
            drop = false;
        }
    }
    synchronized void dropPackets(short packet) {
        if (drop) {
            if (dropPacketSeqNum.contains(packet)) {
                fileDictionary.remove((Object)packet);
                seqNumbersReceived.remove((Object)packet);

                dropPacketSeqNum.remove((Object)packet);
            }
        }
    }
}
