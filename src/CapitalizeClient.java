
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * A simple Swing-based client for the capitalization server.
 * It has a main frame window with a text field for entering
 * strings and a textarea to see the results of capitalizing
 * them.
 */
public class CapitalizeClient {

    private JFrame frame = new JFrame("Capitalize Client");
    private JTextField dataField = new JTextField(40);
    private JTextArea messageArea = new JTextArea(8, 60);

    private ObservableSocket socket;

    private HeartBeatTask heartBeatTask;

    /**
     * Constructs the client by laying out the GUI and registering a
     * listener with the textfield so that pressing Enter in the
     * listener sends the textfield contents to the server.
     */
    public CapitalizeClient() {

        // Layout GUI
        messageArea.setEditable(false);
        frame.getContentPane().add(dataField, "North");
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");

        // Add Listeners
        dataField.addActionListener(new ActionListener() {
            /**
             * Responds to pressing the enter key in the textfield
             * by sending the contents of the text field to the
             * server and displaying the response from the server
             * in the text area.  If the response is "." we exit
             * the whole application, which closes all sockets,
             * streams and windows.
             */
            public void actionPerformed(ActionEvent e) {
                if (dataField.getText().toUpperCase().equals("KILL")) {
                    socket.sendMessage(new Message("KILL", new ArrayList<>()));
                } else {
                    List<String> messageText = new ArrayList<>();
                    messageText.add(dataField.getText());
                    socket.sendMessage(new Message("CAPITALIZE", messageText));
                }
                dataField.selectAll();
            }
        });

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                socket.sendMessage(new Message("KILL", null));
                socket.stop();
                heartBeatTask.stop();
            }
        });

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
    }

    /**
     * Implements the connection logic by prompting the end user for
     * the server's IP address, connecting, setting up streams, and
     * consuming the welcome messages from the server.  The Capitalizer
     * protocol says that the server sends three lines of text to the
     * client immediately after establishing a connection.
     */
    public void connectToServer() {

        // Get the server address from a dialog box.
        String serverAddress = JOptionPane.showInputDialog(
            frame,
            "Enter IP Address of the Server:",
            "Welcome to the Capitalization Program",
            JOptionPane.QUESTION_MESSAGE);

        connectToServer(serverAddress);
    }

    public void connectToServer(String serverAddress) {
        // Make connection
        try {
            Socket socket = new Socket(serverAddress, 9898);

            // Create the observable socket
            this.socket = new ObservableSocket(socket);

            // Bind the message handler
            this.socket.addOnMessageHandler(this::handleMessage);

            // Start the socket thread
            new Thread(this.socket).start();

            // Create a heartbeat task that will run every second
            heartBeatTask = new HeartBeatTask(this.socket);
            new Thread(heartBeatTask).start();

            // Display the UI
            frame.setVisible(true);
        } catch (IOException e) {
            System.out.println("Failed to start client!");
            e.printStackTrace();
        }
    }

    private String handleMessage(Message message) {
        switch (message.getCommand()) {
            case "SEND":
                // If the message is a 'SEND' type message simply display the arguments in the UI
                messageArea.append(message.getArgs().get(0) + "\n");
                break;
            case "KILL":
                // Kill the client if the proper response is sent
                // First clean up the socket thread
                this.socket.stop();

                // Then clean up the UI
                this.frame.setVisible(false);
                this.frame.dispose();

                // Exit the process
                frame.dispose();
                break;
            case "CLIENT_HEARTBEAT":
                // Print received heartbeat
                System.out.println("Received heartbeat from client with id: " + message.getArgs().get(0));
                break;
            default:
                messageArea.append("Unknown response command: " + message.getCommand() + "\n");
        }
        return "";
    }

    private class HeartBeatTask implements Runnable {
        ObservableSocket s;
        boolean shouldRun = true;

        HeartBeatTask(ObservableSocket s) {
            this.s = s;
        }

        @Override
        public void run() {
            int i = 0;
            while(shouldRun) {
                // Send heartbeat every second
                s.sendMessage(new Message("HEARTBEAT", null));

                // Send heartbeat to all clients every 5th loop
//                if (i == 4) {
//                    s.sendMessage(new Message("BROADCAST_HEARTBEAT", null));
//                }

                try {
                    Thread.sleep(1000);
                    i = (i + 1) % 5;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void stop() {
            shouldRun = false;
        }
    }
}