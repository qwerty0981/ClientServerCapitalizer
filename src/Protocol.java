import java.util.HashMap;
import java.util.Map;

public class Protocol {
    public static final Map<String, Integer> commands;
    static {
        commands = new HashMap<>();
        commands.put("SEND", 1);
        commands.put("CAPITALIZE", 1);
        commands.put("HEARTBEAT", 0);
        commands.put("BROADCAST_HEARTBEAT", 0);
        commands.put("CLIENT_HEARTBEAT", 1);
        commands.put("KILL", 0);
    }

    static boolean isCommand(String command) {
        return commands.keySet().stream().filter(v -> v.equals(command.toUpperCase())).count() == 1;
    }
}
