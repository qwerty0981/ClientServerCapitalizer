import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.stream.Collectors;

public class ServerViewModel extends Observable {
    private Map<Integer, Capitalizer> connectedClients;
    private StringBuilder messageLog;

    public ServerViewModel() {
        connectedClients = new HashMap<>();
        messageLog = new StringBuilder();
    }

    public void addClient(Capitalizer client) {
        connectedClients.put(client.getId(), client);
        setChanged();
        notifyObservers();
    }

    public boolean killClient(int id) {
        Capitalizer client = connectedClients.get(id);
        if (client == null) {
            return false;
        }
        client.stop();
        connectedClients.remove(id);
        setChanged();
        notifyObservers();
        return true;
    }

    public boolean killAllClients() {
        Set<Integer> clients = connectedClients.keySet();
        for (Integer key : clients) {
            Capitalizer client = connectedClients.get(key);
            if (client == null) {
                return false;
            }
            client.stop();
        }
        connectedClients.clear();
        setChanged();
        notifyObservers();
        return true;
    }

    public List<Capitalizer> getClients() {
        return connectedClients.keySet().stream()
                .map(id -> connectedClients.get(id))
                .collect(Collectors.toList());
    }

    public Capitalizer getClientById(int id) {
        return connectedClients.get(id);
    }

    public void addText(String text) {
        messageLog.append(text + "\n");
        setChanged();
        notifyObservers();
    }

    public String getText() {
        return messageLog.toString();
    }
}
