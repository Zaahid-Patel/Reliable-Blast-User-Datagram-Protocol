package GUI;

import javax.swing.*;
import sender.SenderTCP;
import sender.SenderTCPListener;
import sender.SenderTCPSignaller;

/**
 * Class is defined for a progress bar for the receiver when UDP is selected
 */
class ProgressBarReceiverUDP extends SwingWorker<Void, Integer> {
    private JProgressBar progressBar;

    public ProgressBarReceiverUDP() {
        JFrame frame = new JFrame();
        progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setStringPainted(true);
        frame.setSize(200, 50);
        frame.add(progressBar);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    /**
     * Gets the current packets sent and the total packets to be sent. Also makes a
     * nice percentage.
     * 
     * @return Void
     * @throws Exception
     */
    @Override
    protected Void doInBackground() throws Exception {
        int packetsSent = 0, totalPackets = 0;
        while (true) {
            packetsSent = ConnectionSelectionReceiver.udp_api.get(0).getTotalPacketsReceived();
            System.out.println("" + packetsSent);
            totalPackets = ConnectionSelectionReceiver.udp_api.get(0).getExpectedPacketsCount();
            int progress = (int) (((double) packetsSent / totalPackets) * 100);
            publish(progress);
            if (packetsSent == totalPackets) {
                break;
            }
            Thread.sleep(10);

        }
        return null;
    }

    /**
     * Updates the progress bar.
     * 
     * @param progressList
     */
    @Override
    protected void process(java.util.List<Integer> progressList) {
        int progress = progressList.get(progressList.size() - 1);
        progressBar.setValue(progress);
    }

    /**
     * Displays pop up message when progress bar is full.
     * 
     * @return Void
     */
    @Override
    protected void done() {
        progressBar.setValue(progressBar.getMaximum());
        JOptionPane.showMessageDialog(null, "File transfer completed.");
    }
}
