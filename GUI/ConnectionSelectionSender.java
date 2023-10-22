package GUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import sender.SenderTCPListener;
import sender.SenderTCPSignalListener;
import java.awt.*;

/**
 * Class is defined for 2 buttons and a textfield for the sender's port number
 */
public class ConnectionSelectionSender extends JFrame {
    int port = 0;

    public void run() {
        setTitle("filetransfer2.0");
        setSize(600, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel buttonWindow = new JPanel();

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
                FileSelection window1 = new FileSelection();
                window1.run();
                String path = window1.getPath();
                System.out.println(path);
                int num = Integer.parseInt(port.getText());
                System.out.println(num);
                SenderTCPListener server = new SenderTCPListener(
                        num, path);
                System.out.println("Running TCP Server");
                server.start();
                while (server.getConnections().size() < 1) {
                    try {
                        Thread.sleep(100);
                    } catch (Exception e1) {
                    }

                }
                ProgressBarSenderTCP barWorker = new ProgressBarSenderTCP(server);
                barWorker.execute();
            }
        });

        // Defines what happens when UDP button is pressed
        udp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                FileSelection window1 = new FileSelection();
                window1.run();
                String path = window1.getPath();
                System.out.println(path);
                int num = Integer.parseInt(port.getText());
                System.out.println(num);
                SenderTCPSignalListener server = new SenderTCPSignalListener(
                        num, path);
                System.out.println("Running RBUDP Server");
                server.start();
                while (server.getConnections().size() < 1) {
                    try {
                        Thread.sleep(100);
                    } catch (Exception e1) {
                    }

                }
                ProgressBarSenderUDP barWorker = new ProgressBarSenderUDP(server);
                barWorker.execute();
            }
        });

        getContentPane().add(port, BorderLayout.NORTH);
        getContentPane().add(buttonWindow, BorderLayout.CENTER);
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
