import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class ObservableSocket implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean shouldRun = true;
    private List<Function<Message, String>> onMessageHandlers;
    private List<Message> sendQueue;
    private ReentrantLock sendLock;

    public ObservableSocket(Socket socket) throws IOException {
        this.socket = socket;
        sendLock = new ReentrantLock();

        in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        onMessageHandlers = new ArrayList<>();
        sendQueue = new ArrayList<>();
    }

    public void addOnMessageHandler(Function<Message, String> handler) {
        onMessageHandlers.add(handler);
    }

    private void notifyHandlers(Message message) {
        onMessageHandlers.forEach(handler -> handler.apply(message));
    }

    public void stop() {
        shouldRun = false;

        // Send all remaining messages
        sendLock.lock();
        for (Message message : sendQueue) {
            out.println(message.serialize());
        }
        sendQueue.clear();
        sendLock.unlock();
    }

    public void sendMessage(Message message) {
        sendLock.lock();
        sendQueue.add(message);
        sendLock.unlock();
    }

    @Override
    public void run() {
        System.out.println("Started running");
        while(shouldRun) {
            // If there are messages to receive, get them and notify listeners
            try {
                if (in.ready()) {
                    String command = in.readLine().toUpperCase();

                    if (!Protocol.isCommand(command)) {
                        // Invalid message, throw it away and log
                        System.out.println("Invalid command sent: " + command);
                    }

                    List<String> args = new ArrayList<>();
                    for (int i = 0; i < Protocol.commands.get(command); i++) {
                        args.add(in.readLine());
                    }

                    notifyHandlers(new Message(command, args));
                }
            } catch (IOException e) {
                System.out.println("Socket closed unexpectedly!");
                shouldRun = false;
                continue;
            }

            // If there are messages to send, send them
            if (sendQueue.size() > 0) {
                sendLock.lock();
                for (Message message : sendQueue) {
                    out.println(message.serialize());
                }
                sendQueue.clear();
                sendLock.unlock();
            }
        }
        System.out.println("Stopped running");
    }
}
