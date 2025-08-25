import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    static Map<String, ClientHandler> clients = new HashMap<>();
    static Map<String, List<String>> messages = new HashMap<>();
    static Map<String, String> status = new HashMap<>();
    static Map<String, List<String>> contacts = new HashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(1234);
        System.out.println("Server started on port 1234...");

        while (true) {
            Socket socket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(socket);
            new Thread(handler).start();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private String username;
        private BufferedReader in;
        private PrintWriter out;

        ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }

        @Override
        public void run() {
            try {
                out.println("Enter username: ");
                username = in.readLine();
                clients.put(username, this);
                status.put(username, "Online");
                contacts.putIfAbsent(username, new ArrayList<>());
                messages.putIfAbsent(username, new ArrayList<>());

                out.println("Hello " + username + ", you are connected!");
                menuLoop();
            } catch (IOException e) {
                System.out.println("Client disconnected: " + username);
            }
        }

        private void menuLoop() throws IOException {
            while (true) {
                out.println("\n=== MENU ===\n" +
                        "1. View Contacts\n" +
                        "2. Search Contact\n" +
                        "3. Add Contact\n" +
                        "4. Edit Contact\n" +
                        "5. Private Chat\n" +
                        "6. Group Chat\n" +
                        "7. View Message History\n" +
                        "8. Delete Chat History\n" +
                        "9. Change Status\n" +
                        "10. Exit\n" +
                        "Choose option: ");
                String choice = in.readLine();

                switch (choice) {
                    case "1": viewContacts(); break;
                    case "2": searchContact(); break;
                    case "3": addContact(); break;
                    case "4": editContact(); break;
                    case "5": privateChat(); break;
                    case "6": groupChat(); break;
                    case "7": viewHistory(); break;
                    case "8": deleteChatHistory(); break;
                    case "9": changeStatus(); break;
                    case "10": exitConfirmation(); return;
                    default: out.println("Invalid choice");
                }
            }
        }

        private void viewContacts() {
            List<String> list = contacts.get(username);
            if (list.isEmpty()) out.println("No contacts found.");
            else {
                out.println("Your Contacts:");
                for (String c : list) {
                    out.println("- " + c + " (" + status.getOrDefault(c, "Offline") + ")");
                }
            }
        }

        private void searchContact() throws IOException {
            out.println("Enter name to search: ");
            String search = in.readLine();
            boolean found = false;
            for (String name : contacts.get(username)) {
                if (name.toLowerCase().contains(search.toLowerCase())) {
                    out.println("Found: " + name);
                    found = true;
                }
            }
            if (!found) out.println("Contact not found.");
        }

        private void addContact() throws IOException {
            out.println("Enter contact name: ");
            String newContact = in.readLine();
            contacts.get(username).add(newContact);
            out.println("Contact added.");
        }

        private void editContact() throws IOException {
            viewContacts();
            out.println("Enter contact to edit: ");
            String oldName = in.readLine();
            if (contacts.get(username).contains(oldName)) {
                out.println("Enter new name: ");
                String newName = in.readLine();
                contacts.get(username).remove(oldName);
                contacts.get(username).add(newName);
                out.println("Contact updated.");
            } else out.println("Contact not found.");
        }

        private void privateChat() throws IOException {
            out.println("Enter username to chat with: ");
            String target = in.readLine();
            if (clients.containsKey(target)) {
                out.println("Connected to " + target + ". Type 'exit' to stop.");
                while (true) {
                    String msg = in.readLine();
                    if ("exit".equalsIgnoreCase(msg)) break;
                    clients.get(target).out.println(username + ": " + msg);
                    messages.get(target).add(username + ": " + msg);
                    messages.get(username).add("Me to " + target + ": " + msg);
                }
            } else {
                out.println("User offline or does not exist.");
            }
        }

        private void groupChat() throws IOException {
            out.println("Enter message for group: ");
            String msg = in.readLine();
            for (String user : clients.keySet()) {
                if (!user.equals(username)) {
                    clients.get(user).out.println("[Group] " + username + ": " + msg);
                    messages.get(user).add("[Group] " + username + ": " + msg);
                }
            }
        }

        private void viewHistory() {
            List<String> history = messages.get(username);
            if (history.isEmpty()) out.println("No message history.");
            else {
                out.println("Message History:");
                for (String m : history) out.println(m);
            }
        }

        private void deleteChatHistory() {
            messages.get(username).clear();
            out.println("Chat history deleted.");
        }

        private void changeStatus() throws IOException {
            out.println("Enter new status (Online/Offline/Busy): ");
            String newStatus = in.readLine();
            status.put(username, newStatus);
            out.println("Status updated to " + newStatus);
        }

        private void exitConfirmation() throws IOException {
            out.println("Are you sure you want to exit? (yes/no): ");
            String ans = in.readLine();
            if (ans.equalsIgnoreCase("yes")) {
                status.put(username, "Offline");
                clients.remove(username);
                socket.close();
            } else {
                out.println("Exit cancelled.");
            }
        }
    }
}
