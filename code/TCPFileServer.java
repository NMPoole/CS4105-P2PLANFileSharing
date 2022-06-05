import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 *
 * @author 170004680, Nov 2020.
 */
public class TCPFileServer implements Runnable {

    ServerSocket serverSocket; // Socket for this TCP server.
    String hostname; // Hostname associated with the socket.
    int port; // Port associated with the socket.

    Configuration configuration; // FileTreeBrowser program configuration.

    String filePath; // Path to file to download/upload.
    boolean download; // Whether the server is sending a file (download) or receiving a file (upload).


    /**
     * Overloaded Constructor: Creates an instance of TCPServer as a separate thread, for the purpose of downloading
     * and uploading.
     *
     * @param configuration FileTreeBrowser program configuration.
     * @param filePath Exact file path string to the location to download/upload the file to (must exist).
     * @param download Which of downloading or uploading is taking place.
     */
    TCPFileServer(Configuration configuration, String filePath, boolean download) {

        this.configuration = configuration;
        this.filePath = filePath;
        this.download = download;

        // Create server socket, etc.
        try {

            serverSocket = new ServerSocket(0); // Assigns an ephemeral port that is available for use.
            this.hostname = serverSocket.getInetAddress().toString();
            this.port = serverSocket.getLocalPort();
            serverSocket.setSoTimeout(configuration.soTimeout_);

        } catch (IOException e) {
            System.err.println("TCPServer.TCPServer() IO Exception: " + e.getMessage());
        }

        // Handle downloading/uploading according to which is occurring.
        Thread thread = new Thread(this);
        thread.setPriority(9);
        thread.start();

    } // TCPServer().


    /**
     * Process file transfer according to whether downloading (sending) or uploading (receiving) a file.
     */
    @Override
    public void run() {

        processFileTransfer();

    } // run().


    /**
     * Send or receive a file over TCP according to whether a download or an upload is taking place.
     */
    public void processFileTransfer() {

        Socket clientSocket = null;
        do { // While loop to continue listening until client connects to the server.

            try {
                // Create working socket when the client connects.
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                // Nothing to do - non-blocking socket.
            }

        } while (clientSocket == null);


        try {

            if (download) { // When downloading, send the requested file to the client.

                // Send file over TCP connection.
                InputStream fileIn = new BufferedInputStream(new FileInputStream(filePath));
                int bytesSent = copyToOut(fileIn, clientSocket.getOutputStream());
                configuration.log_.writeLog("TCP Server Sent (" + hostname + ":" + port + "): " + bytesSent + " bytes to client."); // Write log.

            } else { // When uploading, read the file from the client.

                // Read file from TCP connection.
                OutputStream fileOut = new BufferedOutputStream(new FileOutputStream(filePath));
                int bytesSent = copyToOut(clientSocket.getInputStream(), fileOut);
                configuration.log_.writeLog("TCP Server Wrote " + bytesSent + " bytes to file."); // Write log.

            }

        } catch (FileNotFoundException ignored) {
            // Should not occur as file existence is checked.
        } catch (IOException e) {
            System.err.println("TCPFileServer.processFileTransfer() IO Exception: " + e.getMessage());
        }

        closeServerSocket();

    } // processFileTransfer().


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
     * Close the server socket.
     */
    public void closeServerSocket() {

        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("TCPServer.closeServerSocket() Error: " + e.getMessage());
        }

    } // closeServerSocket().


} // TCPFileTransferServer{}.
