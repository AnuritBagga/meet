import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class ChatApplication extends JFrame {
    private String currentUserId;
    private String currentUserName;
    private Map<String, User> users;
    private Map<String, Chat> chats;
    private Chat activeChat;

    // File paths for persistent storage
    private static final String USERS_FILE = "users_data.txt";
    private static final String CHATS_FILE = "chats_data.txt";

    // UI Components
    private JPanel mainPanel;
    private JPanel sidePanel;
    private JPanel chatPanel;
    private JTextArea messageArea;
    private JTextField messageInput;
    private JLabel chatTitleLabel;
    private DefaultListModel<String> chatListModel;
    private JList<String> chatList;

    // Colors
    private final Color BG_BLACK = new Color(15, 15, 15);
    private final Color PANEL_BLACK = new Color(25, 25, 25);
    private final Color GLOW_BLUE = new Color(0, 150, 255);
    private final Color LIGHT_BLUE = new Color(100, 180, 255);
    private final Color DARK_BLUE = new Color(0, 100, 200);
    private final Color TEXT_WHITE = new Color(240, 240, 240);

    public ChatApplication() {
        users = new HashMap<>();
        chats = new HashMap<>();

        // Load existing data from files
        loadUsersFromFile();
        loadChatsFromFile();

        setTitle("Chat Application");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Save data when application closes
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveUsersToFile();
                saveChatsToFile();
            }
        });

        showLoginDialog();

        if (currentUserId != null) {
            initializeUI();
        } else {
            System.exit(0);
        }
    }

    // ========== FILE STORAGE METHODS ==========

    private void loadUsersFromFile() {
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    String id = parts[0];
                    String name = parts[1];
                    users.put(id, new User(id, name));
                }
            }
            System.out.println("Loaded " + users.size() + " users from file");
        } catch (IOException e) {
            System.err.println("Error loading users: " + e.getMessage());
        }
    }

    private void saveUsersToFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USERS_FILE))) {
            for (User user : users.values()) {
                bw.write(user.id + "|" + user.name);
                bw.newLine();
            }
            System.out.println("Saved " + users.size() + " users to file");
        } catch (IOException e) {
            System.err.println("Error saving users: " + e.getMessage());
        }
    }

    private void loadChatsFromFile() {
        File file = new File(CHATS_FILE);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("CHAT:")) {
                    String[] parts = line.substring(5).split("\\|");
                    if (parts.length >= 3) {
                        String chatId = parts[0];
                        String chatName = parts[1];
                        boolean isGroup = Boolean.parseBoolean(parts[2]);

                        Chat chat = new Chat(chatId, chatName, isGroup);

                        // Read members
                        line = br.readLine();
                        if (line != null && line.startsWith("MEMBERS:")) {
                            String[] members = line.substring(8).split(",");
                            chat.members.addAll(Arrays.asList(members));
                        }

                        // Read messages
                        line = br.readLine();
                        while (line != null && line.startsWith("MSG:")) {
                            String[] msgParts = line.substring(4).split("\\|", 3);
                            if (msgParts.length == 3) {
                                Message msg = new Message(msgParts[0], msgParts[2]);
                                msg.timestamp = msgParts[1];
                                chat.messages.add(msg);
                            }
                            line = br.readLine();
                        }

                        chats.put(chatId, chat);
                    }
                }
            }
            System.out.println("Loaded " + chats.size() + " chats from file");
        } catch (IOException e) {
            System.err.println("Error loading chats: " + e.getMessage());
        }
    }

    private void saveChatsToFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(CHATS_FILE))) {
            for (Chat chat : chats.values()) {
                bw.write("CHAT:" + chat.id + "|" + chat.name + "|" + chat.isGroup);
                bw.newLine();

                bw.write("MEMBERS:" + String.join(",", chat.members));
                bw.newLine();

                for (Message msg : chat.messages) {
                    bw.write("MSG:" + msg.senderId + "|" + msg.timestamp + "|" + msg.content);
                    bw.newLine();
                }
            }
            System.out.println("Saved " + chats.size() + " chats to file");
        } catch (IOException e) {
            System.err.println("Error saving chats: " + e.getMessage());
        }
    }

    // ========== UI METHODS ==========

    private void showLoginDialog() {
        JDialog loginDialog = new JDialog(this, "Login", true);
        loginDialog.setSize(400, 280);
        loginDialog.setLocationRelativeTo(null);
        loginDialog.getContentPane().setBackground(BG_BLACK);
        loginDialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        formPanel.setBackground(BG_BLACK);
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setForeground(TEXT_WHITE);
        JTextField nameField = new JTextField();
        styleTextField(nameField);

        JLabel idLabel = new JLabel("User ID:");
        idLabel.setForeground(TEXT_WHITE);
        JTextField idField = new JTextField();
        styleTextField(idField);

        JButton loginBtn = new JButton("Login");
        styleButton(loginBtn);

        JButton registerBtn = new JButton("Register");
        styleButton(registerBtn);

        JButton viewUsersBtn = new JButton("View All Users");
        styleButton(viewUsersBtn);
        viewUsersBtn.setBackground(DARK_BLUE);

        formPanel.add(nameLabel);
        formPanel.add(nameField);
        formPanel.add(idLabel);
        formPanel.add(idField);
        formPanel.add(loginBtn);
        formPanel.add(registerBtn);
        formPanel.add(new JLabel());
        formPanel.add(viewUsersBtn);

        loginDialog.add(formPanel, BorderLayout.CENTER);

        loginBtn.addActionListener(e -> {
            String id = idField.getText().trim();

            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(loginDialog, "Please enter User ID");
                return;
            }

            if (users.containsKey(id)) {
                currentUserId = id;
                currentUserName = users.get(id).name;
                loginDialog.dispose();
            } else {
                JOptionPane.showMessageDialog(loginDialog,
                        "User not found! Please register.\n\nTotal registered users: " + users.size());
            }
        });

        registerBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String id = idField.getText().trim();

            if (name.isEmpty() || id.isEmpty()) {
                JOptionPane.showMessageDialog(loginDialog, "Please fill all fields");
                return;
            }

            if (users.containsKey(id)) {
                JOptionPane.showMessageDialog(loginDialog, "User ID already exists!");
                return;
            }

            User newUser = new User(id, name);
            users.put(id, newUser);
            saveUsersToFile(); // Save immediately after registration
            currentUserId = id;
            currentUserName = name;

            JOptionPane.showMessageDialog(loginDialog,
                    "✓ Registration successful!\n\nYour User ID: " + id + "\nKeep this ID safe!\n\nTotal users: " + users.size());
            loginDialog.dispose();
        });

        viewUsersBtn.addActionListener(e -> {
            if (users.isEmpty()) {
                JOptionPane.showMessageDialog(loginDialog, "No users registered yet!");
                return;
            }

            StringBuilder userList = new StringBuilder("Registered Users:\n\n");
            for (User user : users.values()) {
                userList.append("• ").append(user.name).append(" (ID: ").append(user.id).append(")\n");
            }

            JTextArea textArea = new JTextArea(userList.toString());
            textArea.setEditable(false);
            textArea.setBackground(PANEL_BLACK);
            textArea.setForeground(TEXT_WHITE);
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(350, 300));

            JOptionPane.showMessageDialog(loginDialog, scrollPane,
                    "All Users (" + users.size() + ")", JOptionPane.INFORMATION_MESSAGE);
        });

        loginDialog.setVisible(true);
    }

    private void initializeUI() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_BLACK);

        // Side Panel
        sidePanel = new JPanel(new BorderLayout());
        sidePanel.setPreferredSize(new Dimension(350, 700));
        sidePanel.setBackground(PANEL_BLACK);
        sidePanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 2, GLOW_BLUE));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PANEL_BLACK);
        headerPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel userLabel = new JLabel(currentUserName + " (" + currentUserId + ")");
        userLabel.setForeground(GLOW_BLUE);
        userLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JButton newChatBtn = new JButton("New Chat");
        JButton newGroupBtn = new JButton("New Group");
        styleButton(newChatBtn);
        styleButton(newGroupBtn);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        buttonPanel.setBackground(PANEL_BLACK);
        buttonPanel.add(newChatBtn);
        buttonPanel.add(newGroupBtn);

        headerPanel.add(userLabel, BorderLayout.NORTH);
        headerPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Chat List
        chatListModel = new DefaultListModel<>();
        chatList = new JList<>(chatListModel);
        chatList.setBackground(PANEL_BLACK);
        chatList.setForeground(TEXT_WHITE);
        chatList.setSelectionBackground(DARK_BLUE);
        chatList.setSelectionForeground(TEXT_WHITE);
        chatList.setFont(new Font("Arial", Font.PLAIN, 14));
        chatList.setBorder(new EmptyBorder(5, 10, 5, 10));

        JScrollPane chatScrollPane = new JScrollPane(chatList);
        chatScrollPane.setBorder(null);
        chatScrollPane.getViewport().setBackground(PANEL_BLACK);

        sidePanel.add(headerPanel, BorderLayout.NORTH);
        sidePanel.add(chatScrollPane, BorderLayout.CENTER);

        // Chat Panel
        chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(BG_BLACK);

        // Chat Header
        JPanel chatHeaderPanel = new JPanel(new BorderLayout());
        chatHeaderPanel.setBackground(PANEL_BLACK);
        chatHeaderPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        chatHeaderPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, GLOW_BLUE));

        chatTitleLabel = new JLabel("Select a chat");
        chatTitleLabel.setForeground(GLOW_BLUE);
        chatTitleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        chatHeaderPanel.add(chatTitleLabel, BorderLayout.WEST);

        // Message Area
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setBackground(BG_BLACK);
        messageArea.setForeground(TEXT_WHITE);
        messageArea.setFont(new Font("Arial", Font.PLAIN, 14));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane messageScrollPane = new JScrollPane(messageArea);
        messageScrollPane.setBorder(null);
        messageScrollPane.getViewport().setBackground(BG_BLACK);

        // Message Input
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(PANEL_BLACK);
        inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        messageInput = new JTextField();
        styleTextField(messageInput);

        JButton sendBtn = new JButton("Send");
        styleButton(sendBtn);
        sendBtn.setPreferredSize(new Dimension(100, 40));

        inputPanel.add(messageInput, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);

        chatPanel.add(chatHeaderPanel, BorderLayout.NORTH);
        chatPanel.add(messageScrollPane, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        mainPanel.add(sidePanel, BorderLayout.WEST);
        mainPanel.add(chatPanel, BorderLayout.CENTER);

        add(mainPanel);

        // Load user's chats
        loadUserChats();

        // Event Listeners
        newChatBtn.addActionListener(e -> createNewChat());
        newGroupBtn.addActionListener(e -> createNewGroup());
        sendBtn.addActionListener(e -> sendMessage());
        messageInput.addActionListener(e -> sendMessage());

        chatList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = chatList.getSelectedValue();
                if (selected != null) {
                    loadChat(selected);
                }
            }
        });

        setVisible(true);
    }

    private void loadUserChats() {
        chatListModel.clear();
        for (Chat chat : chats.values()) {
            if (chat.members.contains(currentUserId)) {
                chatListModel.addElement(chat.name);
            }
        }
    }

    private void createNewChat() {
        String userId = JOptionPane.showInputDialog(this,
                "Enter User ID to chat with:\n\nTotal users: " + users.size() +
                        "\n(Click 'View All Users' on login screen to see list)");
        if (userId == null || userId.trim().isEmpty()) return;

        userId = userId.trim();

        if (userId.equals(currentUserId)) {
            JOptionPane.showMessageDialog(this, "You cannot chat with yourself!");
            return;
        }

        if (!users.containsKey(userId)) {
            JOptionPane.showMessageDialog(this,
                    "User not found!\n\nUser ID: " + userId + " is not registered.\n" +
                            "Total registered users: " + users.size());
            return;
        }

        String chatId = generateChatId(currentUserId, userId);

        if (!chats.containsKey(chatId)) {
            Chat chat = new Chat(chatId, users.get(userId).name, false);
            chat.members.add(currentUserId);
            chat.members.add(userId);
            chats.put(chatId, chat);
            chatListModel.addElement(users.get(userId).name);
            saveChatsToFile(); // Save immediately
        }

        loadChat(users.get(userId).name);
    }

    private void createNewGroup() {
        String groupName = JOptionPane.showInputDialog(this, "Enter Group Name:");
        if (groupName == null || groupName.trim().isEmpty()) return;

        String memberIds = JOptionPane.showInputDialog(this,
                "Enter User IDs to add (comma-separated):\n\nExample: user1,user2,user3");
        if (memberIds == null || memberIds.trim().isEmpty()) return;

        String[] ids = memberIds.split(",");
        List<String> validIds = new ArrayList<>();
        validIds.add(currentUserId);

        for (String id : ids) {
            id = id.trim();
            if (!id.isEmpty() && users.containsKey(id) && !id.equals(currentUserId)) {
                validIds.add(id);
            }
        }

        if (validIds.size() < 2) {
            JOptionPane.showMessageDialog(this, "Group must have at least 2 members!");
            return;
        }

        String chatId = "group_" + System.currentTimeMillis();
        Chat chat = new Chat(chatId, groupName, true);
        chat.members.addAll(validIds);
        chats.put(chatId, chat);
        chatListModel.addElement(groupName);
        saveChatsToFile(); // Save immediately

        loadChat(groupName);
    }

    private void loadChat(String chatName) {
        for (Chat chat : chats.values()) {
            if (chat.name.equals(chatName)) {
                activeChat = chat;
                chatTitleLabel.setText(chat.name + (chat.isGroup ? " (Group)" : ""));
                displayMessages();
                break;
            }
        }
    }

    private void displayMessages() {
        messageArea.setText("");
        if (activeChat == null) return;

        for (Message msg : activeChat.messages) {
            String sender = users.containsKey(msg.senderId) ?
                    users.get(msg.senderId).name : msg.senderId;

            messageArea.append(String.format("[%s] %s: %s\n\n",
                    msg.timestamp, sender, msg.content));
        }

        messageArea.setCaretPosition(messageArea.getDocument().getLength());
    }

    private void sendMessage() {
        if (activeChat == null) {
            JOptionPane.showMessageDialog(this, "Please select a chat first!");
            return;
        }

        String text = messageInput.getText().trim();
        if (text.isEmpty()) return;

        Message msg = new Message(currentUserId, text);
        activeChat.messages.add(msg);

        messageInput.setText("");
        displayMessages();
        saveChatsToFile(); // Save after sending message
    }

    private String generateChatId(String id1, String id2) {
        List<String> ids = Arrays.asList(id1, id2);
        Collections.sort(ids);
        return ids.get(0) + "_" + ids.get(1);
    }

    private void styleButton(JButton btn) {
        btn.setBackground(GLOW_BLUE);
        btn.setForeground(Color.BLACK);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 15, 8, 15));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(LIGHT_BLUE);
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(GLOW_BLUE);
            }
        });
    }

    private void styleTextField(JTextField field) {
        field.setBackground(PANEL_BLACK);
        field.setForeground(TEXT_WHITE);
        field.setCaretColor(GLOW_BLUE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GLOW_BLUE, 2),
                new EmptyBorder(5, 10, 5, 10)
        ));
        field.setFont(new Font("Arial", Font.PLAIN, 14));
    }

    // Data Classes
    class User {
        String id;
        String name;

        User(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    class Chat {
        String id;
        String name;
        boolean isGroup;
        List<String> members;
        List<Message> messages;

        Chat(String id, String name, boolean isGroup) {
            this.id = id;
            this.name = name;
            this.isGroup = isGroup;
            this.members = new ArrayList<>();
            this.messages = new ArrayList<>();
        }
    }

    class Message {
        String senderId;
        String content;
        String timestamp;

        Message(String senderId, String content) {
            this.senderId = senderId;
            this.content = content;
            this.timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("HH:mm"));
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> new ChatApplication());
    }
}