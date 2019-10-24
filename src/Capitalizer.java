import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Capitalizer {
    private int id;
    private ObservableSocket socket;
    private Exec onExit;
    private Exec onBroadcast;

    Capitalizer(int id, Socket socket) throws IOException {
        this.id = id;
        this.socket = new ObservableSocket(socket);

        // Bind message handler to observable socket
        this.socket.addOnMessageHandler(this::handleMessage);
    }

    public void setOnExit(Exec onExit) {
        this.onExit = onExit;
    }

    public void setOnBroadcast(Exec onBroadcast) {
        this.onBroadcast = onBroadcast;
    }

    public void start() {
        // Send welcome message to client
        sendMessage("You are client number: " + id + ".");
        sendMessage("Send kill to terminate the current connection.");
    }

    public Runnable getThreadRunnable() {
        return this.socket;
    }

    public int getId() {
        return id;
    }

    public void stop() {
        socket.sendMessage(new Message("KILL", null));
        socket.stop();
    }

    public void sendMessage(String message) {
        List<String> args = new ArrayList<>();
        args.add(message);
        socket.sendMessage(new Message("SEND", args));
    }

    public void sendMessage(Message message) {
        socket.sendMessage(message);
    }

    private String handleMessage(Message message) {
        switch (message.getCommand()) {
            case "CAPITALIZE":
                sendMessage(message.getArgs().get(0).toUpperCase());
                break;
            case "KILL":
                onExit.exec();
                stop();
                break;
            case "HEARTBEAT":
                System.out.println("Received heartbeat from " + id);
                break;
            case "BROADCAST_HEARTBEAT":
                System.out.println("Received broadcast heartbeat from " + id);
                onBroadcast.exec();
                break;
        }
        return "";
    }
}
