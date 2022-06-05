import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;

/**
 * TCPBrowseServerThread is a class for handling a given client connection accepted by TCPServer.
 *
 * @author 170004680, Nov 2020.
 */
public class TCPBrowseServerThread implements Runnable {


    Socket socket; // Socket to the client managed by this thread.
    String hostname; // Hostname associated with the socket.
    int port; // Port associated with the socket.
    DataInputStream inFromClient; // Handles socket input stream.
    DataOutputStream outToClient; // Handles socket output stream.

    Configuration configuration; // FileTreeBrowser program configuration.
    FileTreeBrowser fileTreeBrowser; // FileTreeBrowser instance associated with this client.


    /**
     * Constructor: Create TCPServerThread instance that manages a given client socket.
     *
     * @param socket The socket connected to the client.
     * @param configuration FileTreeBrowser program configuration.
     */
    TCPBrowseServerThread(Socket socket, Configuration configuration) {

        try {

            this.socket = socket;
            this.hostname = socket.getInetAddress().toString();
            this.port = socket.getPort();
            this.inFromClient = new DataInputStream(socket.getInputStream());
            this.outToClient = new DataOutputStream(socket.getOutputStream());

            this.configuration = configuration;
            fileTreeBrowser = new FileTreeBrowser(configuration.rootDir_); // Client starts at the root directory.

            Thread thread = new Thread(this);
            thread.start();

        } catch (IOException e) {
            System.err.println("TCPBrowseServerThread.TCPBrowseServerThread() IO Exception: " + e.getMessage());
        }

    } // TCPBrowseServerThread().


    /**
     * Handle client connection by listening for commands and responding with affects of running those commands on the
     * client's remote instance (this instance) of fileTreeBrowser.
     */
    @Override
    public void run() {

        // Constantly listen for messages from the client.
        boolean clientConnected = true;
        do {

            // Wait on message from the client.
            try {

                // Read command from client.
                byte[] bytesReceived = new byte[1024]; // Should be sufficient.
                int numBytesRead = inFromClient.read(bytesReceived); // Blocking.

                if (numBytesRead != -1) {

                    String clientRequest = new String(bytesReceived, StandardCharsets.US_ASCII).trim();

                    // Execute the command on the client's instance of FileTreeBrowser, depending on the command.
                    if (clientRequest.equalsIgnoreCase(FileTreeBrowser.quit_)) {

                        // Client has indicated to quit, close the connection, no response needed.
                        closeSocket();
                        clientConnected = false;

                    } else if (clientRequest.equalsIgnoreCase(FileTreeBrowser.help_)) {

                        String helpInformation = FileTreeBrowser.helpInformation();
                        outToClient.writeBytes(helpInformation);
                        configuration.log_.writeLog("TCP Server Sent ("
                                + hostname + ":" + port + "): '" + helpInformation + "'."); // Write log.

                    } else if (clientRequest.equalsIgnoreCase(FileTreeBrowser.services_)) {

                        String servicesInformation = FileTreeBrowser.displayServices();
                        outToClient.writeBytes(servicesInformation);
                        configuration.log_.writeLog("TCP Server Sent ("
                                + hostname + ":" + port + "): '" + servicesInformation + "'."); // Write log.

                    } else if (clientRequest.equalsIgnoreCase(FileTreeBrowser.list_)) {

                        String filesList = fileTreeBrowser.listFiles();
                        outToClient.writeBytes(filesList);
                        configuration.log_.writeLog("TCP Server Sent ("
                                + hostname + ":" + port + "): '" + filesList + "'."); // Write log.

                    } else if (clientRequest.equalsIgnoreCase(FileTreeBrowser.up_)) {

                        AbstractMap.SimpleEntry<FileTreeBrowser, String> entry = fileTreeBrowser.getParent(fileTreeBrowser);
                        fileTreeBrowser = entry.getKey();
                        String moveUpInfo = entry.getValue();
                        outToClient.writeBytes(moveUpInfo);
                        configuration.log_.writeLog("TCP Server Sent ("
                                + hostname + ":" + port + "): '" + moveUpInfo + "'."); // Write log.

                    } else { // Evaluate command as a provided pathname.

                        AbstractMap.SimpleEntry<FileTreeBrowser, String> entry
                                = fileTreeBrowser.evaluatePathName(fileTreeBrowser, clientRequest);
                        fileTreeBrowser = entry.getKey();
                        String pathnameInfo = entry.getValue();
                        outToClient.writeBytes(pathnameInfo);
                        configuration.log_.writeLog("TCP Server Sent ("
                                + hostname + ":" + port + "): '" + pathnameInfo + "'."); // Write log.

                    } // end of possible client requests.

                    outToClient.flush();

                } else { // Client disconnected since read() returned '-1'.

                    // Client has disconnected, so close this socket.
                    closeSocket();
                    clientConnected = false;

                }

            } catch (IOException e) {
                System.err.println("TCPBrowseServerThread.run() Error: " + e.getMessage());
                closeSocket();
            }

        } while (clientConnected);

        closeSocket();

    } // run().


    /**
     * Close the socket.
     */
    public void closeSocket() {

        try {
            inFromClient.close();
            outToClient.close();
            socket.close();
        } catch (IOException e) {
            System.err.println("TCPBrowseServerThread.closeSocket() Error: " + e.getMessage());
        }

    } // closeSocket().


} // TCPServerThread{}.
