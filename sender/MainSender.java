package sender;
import java.net.*;
import java.util.*;

import GUI.ConnectionSelectionSender;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;

/**
 * Main terminal driver class
 */
public class MainSender {

    public static void main(String args[]) {
        // if (args[2].equals("udp")) {
        //     SenderTCPSignalListener server = new SenderTCPSignalListener(
        //             Integer.parseInt(args[0]), args[1]);
        //     System.out.println("Running RBUDP Server");
        //     server.start();
        // } else {
        //     SenderTCPListener server = new SenderTCPListener(
        //             Integer.parseInt(args[0]), args[1]);
        //     System.out.println("Running TCP Server");
        //     server.start();
        // }
        ConnectionSelectionSender window2 = new ConnectionSelectionSender();
            window2.run();
    }

}
