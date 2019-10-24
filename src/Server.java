import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private int port;
    private int clientNum;
    private ExecutorService clientPool;
    private ServerViewModel model;
    private ServerView view;
    private boolean shouldRun;
    private ServerSocket listener;

    public Server(int port) {
        this.port = port;
        clientNum = 0;
        clientPool = Executors.newCachedThreadPool();
        model = new ServerViewModel();
        view = new ServerView(model);
        shouldRun = true;
    }

    public void start() {
        model.addText("The capitalization server is starting...");

        try {
            listener = new ServerSocket(port);
        } catch (IOException e) {
            model.addText("Failed to start the server: " + e.getMessage());
            return;
        }
        view.setOnCommandListener(this::handleCommand);
        view.start();

        System.out.println("Server started");

        while (shouldRun) {
            // Accept a client connection
            Socket newClient;
            try {
                newClient = listener.accept();
                this.clientNum++;
                System.out.println("New client connection with client #: " + clientNum + " at " + newClient.toString());
            } catch (SocketException e) {
                System.out.println("Server is shutting down...");
                continue;
            } catch (IOException e) {
                System.out.println("Failed to accept client connection: " + e.getMessage());
                e.printStackTrace();
                continue;
            }

            try {
                // Create the Capitalizer task to handle the connection
                Capitalizer capitalizer = new Capitalizer(clientNum, newClient);

                // Set on close handler
                capitalizer.setOnExit(() -> model.killClient(clientNum));

                // Set on broadcast handler
                capitalizer.setOnBroadcast(() -> model.getClients().forEach(client -> {
                    List<String> args = new ArrayList<>();
                    args.add("" + capitalizer.getId());
                    client.sendMessage(new Message("CLIENT_HEARTBEAT", args));
                }));

                // Save a reference to the client using its id as a lookup value
                model.addClient(capitalizer);

                // Run the Capitalizer using the thread pool
                clientPool.execute(capitalizer.getThreadRunnable());

                capitalizer.start();
            } catch (IOException e) {
                System.out.println("Socket fail when instantiating capitalizer thread");
                e.printStackTrace();
            }
        }

        view.close();
        System.out.println("Server stopped");
    }

    private String handleCommand(String commandString) {
        String[] args = commandString.split(" ");
        String command = args[0].toUpperCase();

        switch(command) {
            case "LIST":
                StringBuilder response = new StringBuilder();

                response.append("Connected client ids:\n");

                for (Capitalizer c : model.getClients()) {
                    response.append("- Client ");
                    response.append(c.getId());
                    response.append("\n");
                }

                response.append("Total connected clients: ");
                response.append(model.getClients().size());
                response.append("\n");

                return response.toString();
            case "HELP":
                return  "Available commands:\n" +
                        "- BROADCAST <Message>: Sends a message to all connected clients\n" +
                        "- LIST: List all connected clients\n" +
                        "- KILL <Client ID>: Kill the specified client\n" +
                        "- KILLALL: Kill all connected clients\n" +
                        "- SEND <Client ID> <Message>: Send the specified client a message\n" +
                        "- SENDMANY <Comma Separated IDs> <Message>: Send the specified clients a message\n" +
                        "- STOP: Close all open connections and stop the server";
            case "KILL":
                if (args.length != 2) {
                    return "Invalid command! Command should be KILL <client id>";
                }

                boolean killed = model.killClient(Integer.parseInt(args[1]));

                if (killed) {
                    return "Killed client with id " + args[1];
                }
                return "Failed to kill client with id " + args[1];
            case "STOP":
                shouldRun = false;
                model.killAllClients();

                System.exit(0);
            case "KILLALL":
                boolean killedAll = model.killAllClients();

                if (!killedAll) {
                    return "Failed to kill all clients!";
                }
                return "Killed all clients";
            case "SEND":
                if (args.length < 3) {
                    return "Invalid command! Command should be SEND <client id> <message>";
                }

                String finalMessage = convertArgsToString(args, 2);
                Capitalizer client = model.getClientById(Integer.parseInt(args[1]));

                if (client != null) {
                    client.sendMessage(finalMessage);
                    return "Sent client " + args[1] + " message: " + finalMessage;
                } else {
                    return "Failed to send message to client " + args[1] + "! They do not exist!";
                }
            case "SENDMANY":
                if (args.length < 3) {
                    return "Invalid command! Command should be SENDMANY <Comma Separated IDs> <Message>!";
                }

                String[] clientIds = args[1].split(",");
                String mClientMessage= convertArgsToString(args, 2);

                for (String id : clientIds) {
                    Capitalizer c = model.getClientById(Integer.parseInt(id));

                    if (c != null) {
                        c.sendMessage(mClientMessage);
                    }

                    return "Sent clients: " + args[1] + " " + mClientMessage;
                }
            case "BROADCAST":
                if (args.length < 2) {
                    return "Invalid command! Command should be BROADCAST <Message>";
                }

                String messageArg = convertArgsToString(args, 1);

                model.getClients().forEach(c -> c.sendMessage(messageArg));
                return "Sent '" + messageArg + "' to all clients";
            default:
                return "Unknown command!";
        }
    }

    private String convertArgsToString(String[] args, int startOffset) {
        StringBuilder message = new StringBuilder();
        for (int i = startOffset; i < args.length; i++) {
            message.append(args[i]);
            message.append(" ");
        }

        // Clean up message
        String finalMessage = message.toString();
        return finalMessage.substring(0, finalMessage.length() - 1);
    }
}
