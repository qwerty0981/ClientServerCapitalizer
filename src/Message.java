import java.util.List;
import java.util.stream.Collectors;

public class Message {
    private String command;
    private List<String> args;

    public Message(String command, List<String> args) {
        this.command = command;
        this.args = args;
    }

    public String getCommand() {
        return this.command;
    }

    public List<String> getArgs() {
        return this.args;
    }

    public String serialize() {
        switch (command) {
            case "BROADCAST_HEARTBEAT":
            case "HEARTBEAT":
            case "KILL":
                return command;
            default:
                return command + "\n" + args.stream().collect(Collectors.joining("\n"));
        }

    }
}
