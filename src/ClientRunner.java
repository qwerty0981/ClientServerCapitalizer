public class ClientRunner {
    public static void main(String[] args) {
        for (int i = 0; i < 320; i++) {
            CapitalizeClient client = new CapitalizeClient();
            client.connectToServer("localhost");
        }
    }
}
