package server;

import model.Member;
import model.Message;
import util.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import coordinator.CoordinatorManager;

public class Server {

    private static final int PORT = 1234;
    private static Server instance;         
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final Map<String, Member> members = new ConcurrentHashMap<>();
    private String coordinatorId = null;

    private Server() {}

    public static Server getInstance() {
        if (instance == null) {
            instance = new Server();
        }
        return instance;
    }

    public void start() {
        Logger.getInstance().logSystem("Server started on port" + PORT);
        CoordinatorManager.getInstance(this).startPing();

        // Start coordinator ping
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler (socket, this);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            Logger.getInstance().logSystem("System error: " + e.getMessage());
        }
    }

    //Add new client
    public synchronized void addClient(String id, ClientHandler handler) {
        clients.put(id, handler);

        // Save member info
        String ip = handler.getClientIp();
        int port = handler.getClientPort();

        members.put(id, new Member(id, ip, port));
        if (coordinatorId == null) {
            //first client become a coordinator
            coordinatorId = id;
            handler.sendMessage("SYSTEM|SERVER|" + id + "|YOU ARE COORDINATOR");
        } else {
            // Tell new client who is coordinator
            handler.sendMessage("SYSTEM|SERVER|" + id + "|Current coordinator is: " + coordinatorId);
        }

        //Announce a new member
        broadcast("SYSTEM|SERVER|null|" + id + " added to the group", null);
    }


    ////Sugestion for add Client -->
    //  +Check fo duplocate Id , +use Member object , + use buildMessage(), Use Message.serialize()

    /// public synchronized void addClient(String id, ClientHandler handler) {
    /// 
    /// Reject duplicate IDs
    /// if (clients.containsKey(id)) {
    ///        handler.sendMessage(buildMessage(Message.Type.SYSTEM, "SERVER", id,
    ///                 "ERROR: ID '" + id + "' is already taken. Choose a different ID."));
    ///         Logger.getInstance().logSystem("Rejected duplicate ID: " + id);
    ///         handler.forceClose();   // close socket —> client later  reconnect with new ID
    ///         return;}
    /// 
    /// /// Register client + build Member object
    /// clients.put(id, handler);
    /// Member newMember = new Member(id, handler.getClientIp(), handler.getClientPort());
    /// members.put(id, newMember);
    /// 
    ///  /// Use Member.toString() 
    /// if (coordinatorId == null) {
            //first client become a coordinator
    ///       coordinatorId = id;
    ///        newMember.setCoordinator(true);
    ///        handler.sendMessage(buildMessage(Message.Type.SYSTEM, "SERVER", id, "YOU ARE COORDINATOR"));
    ///        Logger.getInstance().logSystem(id + " joined and became coordinator");
    ///    } else {
            // Tell new client who is coordinator 
    ///        Member coordinator = members.get(coordinatorId);
    ///        handler.sendMessage(buildMessage(Message.Type.SYSTEM, "SERVER", id,
    ///                "Current coordinator: " + coordinator.toString()));
    ///        Logger.getInstance().logSystem(id + " joined. Coordinator is: " + coordinatorId);}
 
        //Announce a new member
    ///    broadcast(buildMessage(Message.Type.SYSTEM, "SERVER", null,
    ///            id + " joined the group"), null);}
    /// 
    /// 
    /// 
    /// 
    ///  //  using Message.serialize()
    //helper function???
    ///private String buildMessage(Message.Type type, String from, String to, String text) {
    ///    Message msg = new Message(type, from, to, text);
    ///    Logger.getInstance().log(msg);   // FIX 5: every message goes to logger
    ///    return msg.serialize();}

    
    
    //delete client after log out
    public synchronized void removeClient(String id) {
        clients.remove(id);
        Logger.getInstance().logSystem(id + " logged out");

        broadcast("SYSTEM|SERVER|null|" + id + " logged out", null);

        //If coordinator is gone
        if (id.equals(coordinatorId)) {
            electNewCoordinator();
        }
    }

    //Choose new coordinator
    //fault tolerance
    private void electNewCoordinator() {
        if (clients.isEmpty()) {
            coordinatorId = null;
            Logger.getInstance().logSystem("Group is empty");
            return;
        }

        //First client in list became a coordinator
        String newCoordinatorId = clients.keySet().iterator().next();
        coordinatorId = newCoordinatorId;
        //members.get(newCoordinatorId).setCoordinator(true);


        clients.get(newCoordinatorId).sendMessage("SYSTEM|SERVER|NULL|" + newCoordinatorId + " IS NEW COORDINATOR");
        broadcast("SYSTEM|SERVER|null|New coordinator: " + newCoordinatorId, null);
        Logger.getInstance().logSystem("New coordinator: " + newCoordinatorId);
    }

    //Send a message everyone except sender
    public void broadcast(String rawMessage, String excludeId) {
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            if (!entry.getKey().equals(excludeId)) {
                entry.getValue().sendMessage(rawMessage);
            }
        }
    }

    //Send private message
    public void sendPrivate(String rawMessage, String toId) {
        ClientHandler handler = clients.get(toId);
        if (handler != null) {
            handler.sendMessage(rawMessage);
        } else {
            Logger.getInstance().logSystem("Client " + toId + "not found");
        }
    }

    //Return list of all clients
    public String getMembersList() {
        StringBuilder sb = new StringBuilder("Members:");
        for (Map.Entry<String, Member> entry : members.entrySet()) {
            Member m = entry.getValue();
            sb.append("| - ").append(m.getId())
                    .append(" ").append(m.getIp())
                    .append(":").append(m.getPort());
            if (m.getId().equals(coordinatorId)) {
                sb.append(" [COORDINATOR]");
            }
        }
        return sb.toString();
    }

    //----new getMembersList()  if we have only getMembersList()
    /*public String getMembersList() {
        StringBuilder sb = new StringBuilder("Members:");
        for (String id : clients.keySet()) {
            sb.append("| - ").append(id);
        }
        return sb.toString();
    }*/


    public String getCoordinatorId() {
        return coordinatorId;
    }

    public static void main(String[] args) {
        Server.getInstance().start();
    }
}
