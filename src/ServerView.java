import javax.swing.*;
import java.util.function.Function;

public class ServerView {
    private JFrame frame;
    private JTextField dataField;
    private JTextArea messageArea;
    private ServerViewModel model;

    public ServerView(ServerViewModel model) {
        this.model = model;

        // Initialize UI
        frame = new JFrame("Capitalize Server");
        dataField = new JTextField(40);
        messageArea = new JTextArea(8, 60);

        // Layout GUI
        messageArea.setEditable(false);
        frame.getContentPane().add(dataField, "North");
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");

        // Wire up data reloads from the model
        model.addObserver((observable, o) -> {
            ServerViewModel m = (ServerViewModel) observable;
            messageArea.setText(m.getText());
        });

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
    }

    public void setOnCommandListener(Function<String, String> action) {
        dataField.addActionListener(e -> {
            model.addText(action.apply(dataField.getText()));
            dataField.setText("");
            dataField.selectAll();
        });
    }

    public void start() {
        frame.setVisible(true);
    }

    public void close() {
        frame.dispose();
    }
}
