
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
    // Runnable class means that this class can be run concurrently alongside other
    // runnable classes
    // because we implemented runnable, we need to have a run method

    // this class should be constantly looking for incoming connections from the
    // client requests\

    private ArrayList<connectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server() {
        connections = new ArrayList<>();
        done = false;
    }

    @Override
    public void run() {

        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                connectionHandler handler = new connectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }

        }

        // IOException is a class for exceptions that are thrown while accessing
        // information
        // basically everytime I try to access a file or information that doesnt exist
        // then this will stop an error from crashing my server
        catch (IOException e) {
            shutdown();
        }

    }

    public void broadcast(String message) {
        for (connectionHandler ch : connections) {
            if (ch != null) {
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown() {
        try {
            done = true;
            if (!server.isClosed()) {
                server.close();
            }
            for (connectionHandler ch : connections) {
                ch.shutdown();
            }
        } catch (IOException e) {

        }

    }

    class connectionHandler implements Runnable {

        private Socket client;
        // bufferedreader is able to read any type of information
        private BufferedReader in;
        // printwriter is able to write any type of information
        private PrintWriter out;
        private String username;

        public connectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Please enter a username: ");
                username = in.readLine();
                System.out.println(username + "connected");
                broadcast(username + "joined the chat");
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/nick ")) {
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2) {
                            broadcast(username + "renamed themselves to " + messageSplit[1]);
                            System.out.println(username + "renamed themselves to " + messageSplit[1]);
                            username = messageSplit[1];
                            out.println("Successfully changed username to " + username);
                        } else {
                            out.println("Invalid username was provided");
                        }
                    } else if (message.startsWith("/quit")) {
                        broadcast(username + " has left the chat");
                        shutdown();
                        
                    } else {
                        broadcast(username + ": " + message);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutdown() {
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();

                }
            } catch (IOException e) {

            }

        }
    }
    public static void main (String[] args) {
        Server server = new Server();
        server.run();
    }

}
