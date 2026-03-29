package server;

import command.Command;
import command.CommandFactory;
import util.Logger;

import java.io.*;
import java.net.Socket;

//Handles communication with a one single connected client
public class ClientHandler implements Runnable {

    private final CommandFactory commandFactory;
    private final Socket socket;
    private final Server server;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String clientId;

    public ClientHandler (Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.commandFactory = new CommandFactory(server);
    }

    @Override
    public void run() {
        try {
            //Initialize input/output stream
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            //first message from client is their ID
            clientId = reader.readLine();

            if (clientId == null || clientId.isBlank()) {
                socket.close();
                return;
            }
            //register client on a server
            server.addClient(clientId, this);

            //Listen for incoming message in a loop
            String rawMessage;
            while ((rawMessage = reader.readLine()) != null) {
                handleMessage(rawMessage);
            }
        } catch (IOException e) {
            Logger.getInstance().logSystem("Connection lost with: " + clientId);
        } finally {
            //Clean up when client disconnected
            disconnect();
        }
    }
    // Routes message to correct command
    private void handleMessage(String rawMessage) {
        Logger.getInstance().logSystem("Received: " + rawMessage);

        // Create and execute command
        Command command = commandFactory.createCommand(rawMessage, this, clientId);
        command.execute();
    }

    // Sends a raw message string to this client
    public void sendMessage(String rawMessage) {
        try {
            writer.write(rawMessage + "\n");
            writer.flush();
        } catch (IOException e) {
            Logger.getInstance().logSystem(
                    "Failed to send to: " + clientId);
        }
    }

//  when duplicate ID is detected -> Closes socket
    
    //public void forceClose() {
    //    disconnected = true;
    //    try {
    //        socket.close();
    //    } catch (IOException e) {
    //        Logger.getInstance().logSystem("Error force-closing socket for duplicate: " + clientId);
    //    }}


    public String getClientIp() {
            return socket.getInetAddress().getHostAddress();
    }

    public int getClientPort() {
        return socket.getPort();
    }

    // Cleanly disconnects the client
    private void disconnect() {
        try {
            if (clientId != null) {
                server.removeClient(clientId);
            }
            socket.close();
        } catch (IOException e) {
            Logger.getInstance().logSystem("Error closing socket");
        }
    }
}
