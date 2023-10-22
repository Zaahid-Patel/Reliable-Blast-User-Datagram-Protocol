package GUI;

import javax.swing.*;

/**
 * Class is defined for a file browser for the file needs to be selected on the
 * sender's side.
 */
public class FileSelection implements Runnable {
    String path;

    public void run() {
        JFrame frame = new JFrame();
        JFileChooser window2 = new JFileChooser();
        int result = window2.showOpenDialog(frame);

        if (result == JFileChooser.APPROVE_OPTION) {
            path = window2.getSelectedFile().getPath();
        } else if (result == JFileChooser.CANCEL_OPTION) {
            System.out.println("File selection cancelled.");
            System.exit(0);
        }
    }

    /**
     * Gets the path of the selected file.
     * 
     * @return String
     */
    public String getPath() {
        return path;
    }

    /**
     * main method.
     * 
     * @param args
     */
    public static void main(String[] args) {
    }
}
