import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * TCPBrowseServer is a class for handling incoming connection requests for remote file browsing.
 *
 * @author 170004680, Nov 2020.
 */
public class TCPBrowseServer implements Runnable {


    ServerSocket serverSocket; // Socket for this TCP server.
    Configuration configuration; // FileTreeBrowser program configuration.


    /**
     * Constructor: Creates an instance of TCPServer as a separate thread.
     *
     * @param configuration FileTreeBrowser program configuration.
     */
    TCPBrowseServer(Configuration configuration) {

        this.configuration = configuration;

        // Create server socket, etc.
        try {

            serverSocket = new ServerSocket(configuration.mPort_); // Make a server socket to listen on.
            serverSocket.setSoTimeout(configuration.soTimeout_);

        } catch (IOException e) {
            System.err.println("TCPServer.TCPServer() Error: " + e.getMessage());
        }

        // Create new thread for TCP server.
        Thread thread = new Thread(this);
        thread.start();

    } // TCPServer().


    /**
     * Constantly listen for incoming connections.
     * When a connection is made, create a new client connection (thread), and add to connectedClients.
     */
    @Override
    public void run() {

        do { // While loop to continue listening for connections until program terminated.

            try {

                // Create working socket for server if there is a new connection.
                Socket clientSocket = serverSocket.accept();

                // Create new thread for the working socket.
                TCPBrowseServerThread tcpBrowseServerThread = new TCPBrowseServerThread(clientSocket, configuration);

            } catch (IOException e) {
                // Nothing to do - non-blocking socket.
            }

        } while (true);

    } // run().


    /**
     * Close the server socket.
     */
    public void closeServerSocket() {

        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("TCPServer.closeServerSocket() Error: " + e.getMessage());
        }

    } // closeServerSocket().


} // TCPBrowseServer{}.
