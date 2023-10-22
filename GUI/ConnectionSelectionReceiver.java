package GUI;

import javax.swing.*;
import receiver.TCPReceiver;
import receiver.TCPSignaller;
import receiver.UDPReceiver;
import receiver.MainReceiver.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.awt.*;
import java.io.*;
import java.nio.*;
import java.net.*;

/**
 * Class is defined for 2 buttons and 2 textfields for the receiver's IP and
 * port number
 */
public class ConnectionSelectionReceiver extends JFrame {
    public static ArrayList<RBUDP_API> udp_api = new ArrayList<RBUDP_API>();
    public static ArrayList<TCP_API> tcp_api = new ArrayList<TCP_API>();
    int port = 0;

    public void run() {
        setTitle("filetransfer2.0");
        setSize(600, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel buttonWindow = new JPanel();

        JTextField ip = new JTextField("IP Address");
        JTextField port = new JTextField("Port");
        JButton tcp = new JButton("TCP");
        JButton udp = new JButton("UDP");
        add(port);
        buttonWindow.add(tcp);
        buttonWindow.add(udp);

        // Defines what happens when TCP button is pressed
        tcp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                String word = ip.getText();
                int num = Integer.parseInt(port.getText());
                System.out.println(num);

                TCPReceiver tcpReceiver = new TCPReceiver(word, (short) (num));
                tcpReceiver.start();
                tcp_api.add(new TCP_API(tcpReceiver));

                ProgressBarReceiverTCP barWorker = new ProgressBarReceiverTCP();
                barWorker.execute();

            }
        });

        // Defines what happens when UDP button is pressed
        udp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                String word = ip.getText();
                short num = Short.parseShort(port.getText());
                System.out.println(num);

                ByteBuffer receiverHostBytes = ByteBuffer.allocate(4);
                String receiverHost = "";

                try {
                    receiverHostBytes = ByteBuffer.wrap(InetAddress.getLocalHost().getAddress());
                    receiverHost = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException ex) {
                    System.out.println("Could not find host address");
                }

                // Create RBUDP threads
                TCPSignaller tcpSignaller = new TCPSignaller(receiverHostBytes, (short) 1234, word, num);
                UDPReceiver udpReceiver = new UDPReceiver(receiverHost, (short) 1234);
                // Connect both threads to each other
                tcpSignaller.setUDPReceiver(udpReceiver);
                udpReceiver.setTCPSignaller(tcpSignaller);
                // Starts threads
                tcpSignaller.start();
                udpReceiver.start();
                // Adds threads to api list
                udp_api.add(new RBUDP_API(tcpSignaller, udpReceiver));

                ProgressBarReceiverUDP barWorker = new ProgressBarReceiverUDP();
                barWorker.execute();

            }
        });

        getContentPane().add(ip, BorderLayout.NORTH);
        getContentPane().add(port, BorderLayout.CENTER);
        getContentPane().add(buttonWindow, BorderLayout.SOUTH);
        pack();
        setVisible(true);
    }

    /**
     * main method.
     * 
     * @param args
     */
    public static void main(String[] args) {
        ConnectionSelectionSender app = new ConnectionSelectionSender();
        app.run();
    }
}