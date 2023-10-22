package receiver;

import java.util.*;

import GUI.ConnectionSelectionReceiver;

import java.io.*;
import java.nio.*;

import java.net.*;

/**
 * Main thread used for:
 * 
 *      Starting most threads with regards to the Receiver
 *      API where GUI can communicate witn client
 *      Commands for the Receiver application
 */
public class MainReceiver {
    public static List<RBUDP_API> udp_api = new ArrayList<RBUDP_API>();
    public static List<TCP_API> tcp_api = new ArrayList<TCP_API>();

    static boolean toggel_is_udp = true;

    static boolean drop = false;
    static int amount = -1;
    public static void main(String[] args) {
        ConnectionSelectionReceiver window = new ConnectionSelectionReceiver();
        window.run();
    }

    public synchronized void toggleIsUDP(boolean toggle) {
        toggel_is_udp = toggle;
    }

    /**
     * Properly closes the threads related to a spesific sender
     * @param signaller Identifier thread for the spesific sender
     */
    public void removeRBUDP_API(TCPSignaller signaller) {
        for (int i = 0; i < udp_api.size(); i++) {
            if (udp_api.get(i).tcpSignaller.equals(signaller)) {
                udp_api.get(i).tcpSignaller.stopRunning();
                udp_api.get(i).udpReceiver.stopRunning();

                udp_api.remove(i);
            }
        }
    }
    /**
     * API used by the GUI to access information related to the RBUDP file transfer
     */
    public static class RBUDP_API {
        TCPSignaller tcpSignaller;
        UDPReceiver udpReceiver;
        public RBUDP_API(TCPSignaller signaller, UDPReceiver receiver) {
            tcpSignaller = signaller;
            udpReceiver = receiver;
        }

        /**
         * Test function that GUI can use to test if it can access the API
         * @return Hello World
         */
        public String testAPI() {
            return "Hello World";
        }

        /**
         * Gets total file packets from UDPReceiver
         * @return Returns total packets that are expected to be sent to receiver
         */
        public int getExpectedPacketsCount() {
            return udpReceiver.getExpectedPacketsCount();
        }

        /**
         * Gets current amount of packets that are received from UDPReceiver
         * @return Returns current amout of packets that are received
         */
        public int getTotalPacketsReceived() {
            return udpReceiver.getExpectedPacketsCount();
        }
    }
    
    /**
     * API used by the GUI to access information related to the TCP file transfer
     */
    public static class TCP_API {
        static TCPReceiver tcpReceiver;

        public TCP_API(TCPReceiver receiver) {
            tcpReceiver = receiver;
        }
        /**
         * Gets the total bytes the file is expected to be. 
         * Used for progress bar
         * @return filesize byte amount
         */
        public int getExpectedBytes() {
            return tcpReceiver.getExpectedBytes();
        }
    
        /**
         * Gets the total bytes that have been received
         * Used for progress bar
         * @return byte amount
         */
        public int getTotalBytesRead() {
            return tcpReceiver.getTotalBytesRead();
        }
    }
}
