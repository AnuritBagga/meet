import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Client {
    private BufferedReader in;
    private PrintWriter out;
    private JFrame frame = new JFrame("Chat Client");
    private JTextField textField = new JTextField(40);
    private JTextArea messageArea = new JTextArea(8, 40);

    public Client(String serverAddress) {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.add(textField, BorderLayout.SOUTH);
        frame.add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.pack();

        textField.addActionListener(e -> {
            out.println(textField.getText());
            textField.setText("");
        });

        try {
            Socket socket = new Socket(serverAddress, 8888);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            while (true) {
                String line = in.readLine();
                if (line == null) break;

                if (line.startsWith("SUBMIT_NAME")) {
                    String name = JOptionPane.showInputDialog(
                        frame,
                        "Choose a username:",
                        "Username Selection",
                        JOptionPane.PLAIN_MESSAGE
                    );
                    if (name == null) return;
                    out.println(name);
                } else if (line.startsWith("NAME_ACCEPTED")) {
                    textField.setEditable(true);
                    frame.setTitle("Chat - " + line.substring(14));
                } else {
                    messageArea.append(line + "\n");
                }
            }
        } catch (IOException e) {
            messageArea.append("Connection error: " + e.getMessage());
        } finally {
            frame.setVisible(false);
            frame.dispose();
        }
    }

    public static void main(String[] args) {
        String serverAddress = JOptionPane.showInputDialog(
            null,
            "Enter IP Address of the Server:",
            "Welcome to Chat",
            JOptionPane.QUESTION_MESSAGE
        );
        if (serverAddress != null && !serverAddress.trim().isEmpty()) {
            Client client = new Client(serverAddress.trim());
            client.frame.setVisible(true);
        }
    }
}
