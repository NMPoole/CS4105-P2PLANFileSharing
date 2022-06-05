import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/**
 * TCPClient is a class that works with FileTreeBrowser to send remote browsing commands to a given server, so that the
 * file directory at the server can be browsed remotely.
 *
 * @author 170004680, Nov 2020.
 */
public class TCPClient {


    Socket socket; // Current socket connection to TCP server.
    String hostname; // Hostname associated with the socket.
    int port; // Port associated with the socket.
    DataInputStream inFromServer; // Handles socket input stream.
    DataOutputStream outToServer; // Handles socket output stream.

    Configuration configuration; // Current FileTreeBrowser configuration.


    /**
     * Constructor: Creates TCPClient instance for interacting with the TCP server at the remote file browser.
     *
     * @param configuration Current FileTreeBrowser configuration.
     * @param hostname Hostname to connect to.
     * @param port Port number at hostname to connect to.
     */
    TCPClient(Configuration configuration, String hostname, int port) {

        try {

            this.configuration = configuration;
            this.hostname = hostname;
            this.port = port;

            InetAddress address = InetAddress.getByName(hostname);

            this.socket = new Socket(address, port); // Connect to server.
            this.inFromServer = new DataInputStream(socket.getInputStream());
            this.outToServer = new DataOutputStream(socket.getOutputStream());

        } catch (UnknownHostException e) {
            System.err.println("TCPClient.TCPClient() Unknown Host Exception: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("TCPClient.TCPClient() IO Exception: " + e.getMessage());
        }

    } // TCPClient().


    /**
     * Given a command from the user, send the command to the server and wait for response to be given back to the
     * FileTreeBrowser.
     *
     * @param command String representing the command the user wishes to execute at the server remotely.
     *
     * @return String representing the response to the FileTreeBrowser command from the server.
     */
    public String sendCommand(String command) {

        String serverResponse = null;

        // Send command to the server.
        byte[] bytesToSend = command.getBytes();
        try {
            outToServer.write(bytesToSend);
            outToServer.flush();
            configuration.log_.writeLog("TCP Client Sent (" + hostname + ":" + port + "): '" + command + "'."); // Write log.
        } catch (IOException e) {
            //System.err.println("TCPClient.sendCommand() Error Sending To Server: " + e.getMessage());
            // Error detected from read, which gracefully handles the socket closure.
        }

        // Slow down to wait for server response.
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            System.err.println("TCPClient.sendCommand() Thread Sleeping Error: " + e.getMessage());
        }

        // Wait on command-response from the server.
        byte[] bytesReceived = new byte[1024]; // Should be sufficient.
        try {

            int numBytesRead = inFromServer.read(bytesReceived);

            if (numBytesRead != -1) {
                serverResponse = new String(bytesReceived, StandardCharsets.US_ASCII).trim();
            } else {
                closeClient();
                return null;
            }

        } catch (IOException e) {
            System.err.println("TCPClient.sendCommand() Error Reading From Server: " + e.getMessage());
        }

        // Return response from server to the user.
        return hostname + ":" + port + ":\n" + serverResponse;

    } // sendCommand().


    /**
     * Given a valid file path, read file contents from the TCP connection and save contents to the file path.
     *
     * @param fileToSaveTo File path of file to write file contents to. (File path may or may not exist).
     *
     * @return True if successful, false otherwise.
     */
    public void processFile(String fileToSaveTo, boolean download) {

        try {

            if (download) { // When downloading, read the requested file from the server.

                // Read file from TCP connection.
                OutputStream fileOut = new BufferedOutputStream(new FileOutputStream(fileToSaveTo));
                int bytesSent = copyToOut(socket.getInputStream(), fileOut);
                configuration.log_.writeLog("TCP Client Wrote " + bytesSent + " bytes to file."); // Write log.

            } else { // When uploading, read the file from the client.

                // Send file over TCP connection.
                InputStream fileIn = new BufferedInputStream(new FileInputStream(fileToSaveTo));
                int bytesSent = copyToOut(fileIn, socket.getOutputStream());
                configuration.log_.writeLog("TCP Client Sent (" + hostname + ":" + port + "): " + bytesSent + " bytes to server."); // Write log.

            }

        } catch (FileNotFoundException ignored) {
            // Should not occur as file existence is checked.
        } catch (IOException e) {
            System.err.println("TCPClient.processFileTransfer() IO Exception: " + e.getMessage());
        }

        closeClient();

    } // readFile().


    /**
     * Method to send all bytes from an input stream to an output stream.
     *
     * @param in Input stream to read bytes from.
     * @param out Output stream to send bytes to.
     *
     * @return numberOfBytes sent, used for debugging.
     *
     * @throws IOException Error interacting with the streams.
     */
    public static int copyToOut(InputStream in, OutputStream out) throws IOException {

        byte[] buf = new byte[2048];
        int bytesRead = 0;
        int totalBytes = 0;

        while ((bytesRead = in.read(buf)) != -1) { // While still bytes in file to send.
            totalBytes += bytesRead;
            out.write(buf, 0, bytesRead); // Write current chunk of bytes to output stream.
        }
        out.flush();

        return totalBytes;

    } // copyToOut().


    /**
     * Close the socket, which disconnects from the TCP Server.
     */
    public void closeClient() {

        try {
            inFromServer.close();
            outToServer.close();
            socket.close();
        } catch (IOException e) {
            System.err.println("TCPClient.closeClient() IO Exception: " + e.getMessage());
        }

    } // closeClient().


} // TCPClient{}.
