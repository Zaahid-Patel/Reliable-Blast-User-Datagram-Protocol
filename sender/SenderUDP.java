package sender;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.net.*;
import java.util.*;

/**
 * Thread that implements UDP portion of RBUDP Protocol
 */
public class SenderUDP extends Thread {

    int dataSize = 16382, packetsPerBurst = 100;
    short currentPacketNumber = 0;
	short[] packetsSent;
    int packetsSentSuccessfully;
    
    String fileName;
    FileInputStream file;
    int fileSize;
    ByteBuffer data;
    ByteBuffer[] dataArray = new ByteBuffer[packetsPerBurst];
    boolean fileEmpty = false;

    InetSocketAddress receiverAddress;
    DatagramChannel udpChannel;
    SenderTCPSignaller signaller;

    boolean running = true;
    boolean send = false;

    int packetsToSend = 0;

    /**
     * Constructer
     * @param name Name of file to send
     * @param rname Name/IP address of reciever
     * @param port Port of reciever
     */
    SenderUDP(String fname, InetSocketAddress address, SenderTCPSignaller s) {
        System.out.println("Started UDP");
        receiverAddress = address;
        signaller = s;

        try {
            fileName = fname;
            // System.out.println(fileName);
            file = new FileInputStream(fileName);
            fileSize = (int)file.getChannel().size();

            udpChannel = DatagramChannel.open();
            // udpChannel.bind(null);
            udpChannel.configureBlocking(false);
            // udpChannel.connect(receiverAddress);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // boolean resent = false;
    @Override
    public void run() {
        
        
        while (running) {
            int i = 0, j = 0;
            if (getSend()) {
                if (packetsToSend > 0) {
                    packetsSent = new short[packetsToSend];
                    
                    System.out.println(receiverAddress.getAddress() + " : " + receiverAddress.getPort());
                    while (i < packetsPerBurst) {
                        ByteBuffer dataCopy = getArray(i);
                        try {
                            if (dataCopy != null && packetsSent.length > 0) {
                                // System.out.println("data sent: " + Arrays.toString(dataArray[i].array()));
                                
                                dataCopy.position(0);
                                // try {
                                //     //Wait split second for receiver to process received packets
                                //     Thread.sleep(10);
                                // } catch (Exception e) {
                                //     // TODO: handle exception
                                // }
                                udpChannel.send(getArray(i), 
                                                receiverAddress);

                                // System.out.println(Arrays.toString(dataArray[0].array()));
                                // System.out.println(getArray(i) );
                                dataCopy.position(0);
                                
                                // System.out.println(Arrays.toString(dataCopy[i].array()));
                                // System.out.println(packetsSent.length);
                                // System.out.println(dataArray.length);   
                                setPacketsSent(dataCopy.getShort(0), j);
                                j++;
                                packetsToSend--;                             
                                
                            } 
                            
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        i++;
                    }
                }
			    
                // try {
                //     //Wait split second for receiver to process received packets
                //     Thread.sleep(10);
                // } catch (Exception e) {
                //     // TODO: handle exception
                // }
                
                // resent = true;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                signaller.sendPacketsSent(packetsSent);
                send = false;
            } else {
                // System.out.println(Arrays.toString(packetsSent));
            }
        }

    }

    public synchronized ByteBuffer getArray(int i) {
        // System.out.println("get");
        return dataArray[i];
    }

    public synchronized void setArray(ByteBuffer set, int i) {
        // System.out.println("set: " + set);
        dataArray[i] = set;
    }
    public synchronized boolean getSend() {
        return send;
    }

    public synchronized boolean isBurstDone() {
        return packetsToSend == 0;
    }

	public synchronized short getPacketsSent(int i) {
        // System.out.println("get packet");
		return packetsSent[i];
	}

    public synchronized void setPacketsSent(short set, int i) {
        // System.out.println("set packet");
        if (packetsSent.length > i) {
            packetsSent[i] = set;
        }
        
    }

    /**
     * Causes thread to continue sending packets, resending indicated packets
     * Refills send buffer from file
     * @param packetNumbers Array of packet numbers that must be resent
     */
    public synchronized void send(short[] packetsToResend) {
        if (packetsToResend.length > 0) {
            System.out.println("Sending udp packets: " + packetsToResend[0]);
        }
        
		int tempPacketsToSend = 0;
        // System.out.println(Arrays.toString(dataArray));
        
        // Sets all successfully sent packets to null in dataArray
        for (int i = 0; i < dataArray.length; i++) {
            if (getArray(i)!= null) {
                short packetNumber = getArray(i).getShort(0);
                boolean wasSent = true;
                
                for (short resendNumber: packetsToResend) {
                    if (packetNumber == resendNumber) {
                        wasSent = false;
                    }
                }

                if (wasSent) {
                    // System.out.println("No resend: " + packetNumber);
                    packetsSentSuccessfully++;
                    setArray(null, i);
                } else {
                    tempPacketsToSend++;
                }
            }
        }

        byte[] buffer = new byte[dataSize];
        int bufferSize = dataSize;
        int i = 0;
        // System.out.println("loop");
        while (tempPacketsToSend < 100) {
            
            try {
                bufferSize = file.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
			if (bufferSize == -1) {
                fileEmpty = true;
				break;
			}

            // Moves index to next empty dataArray location
            while (getArray(i) != null) {
                i++;
            }

            setArray(ByteBuffer.allocate(bufferSize + 4), i);
            // System.out.println(Arrays.toString(buffer));
            // System.out.println("Buffer ");

            getArray(i).position(4);
            getArray(i).put(buffer, 0, bufferSize);
            getArray(i).putShort(0, currentPacketNumber);
            getArray(i).putShort(2,(short) bufferSize);
            buffer = new byte[dataSize];
            // System.out.println(Arrays.toString(dataArray[i].array()));
            currentPacketNumber++;
            tempPacketsToSend++;
            if (bufferSize < dataSize) {
                fileEmpty = true;
				break;
            }
        }
		packetsToSend = tempPacketsToSend;
        System.out.println("Packets to send: " + packetsToSend);
        if (packetsToSend > 0) {
            send = true;
        } else {
            send = false;
        }
        
    }

    public void stopRunning() {
        running = false;
    }

    public boolean isFileEmpty() {
        return fileEmpty;
    }

    public int getPacketsSentSuccessfully() {
       return packetsSentSuccessfully; 
    }

    public int getPacketsToSend() {
        //Hotfix, not applicable
        if ((int)Math.ceil(fileSize/dataSize) == 0) {
            return (int)Math.ceil(fileSize/dataSize) + 1;
        } else {
            return (int)Math.ceil(fileSize/dataSize);
        }
        
    }

}
