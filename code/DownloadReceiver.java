import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;


/**
 * DownloadReceiver is a class for handling incoming download messages given by MulticastHandler in a separate thread.
 *
 * @author 170004680, Nov 2020.
 */
public class DownloadReceiver implements Runnable {


    MulticastHandler multicastHandler; // Instance of MulticastHandler used.
    Configuration configuration; // Configuration of MulticastHandler.

    HashMap<String, Message> outgoingDownloadRequests; // HashMap of active download requests that have been sent.
    HashMap<String, Message> incomingDownloadMessages; // HashMap of incoming download messages pending processing.


    /**
     * Constructor: Creates an instance of DownloadReceiver as a separate thread.
     *
     * @param multicastHandler Instance of MulticastHandler, used for creating and reading protocol messages.
     */
    DownloadReceiver(MulticastHandler multicastHandler) {

        this.multicastHandler = multicastHandler;
        this.configuration = multicastHandler.configuration;

        this.outgoingDownloadRequests = new HashMap<>(); // HashMap of active download requests that have been sent.
        this.incomingDownloadMessages = new HashMap<>(); // HashMap of incoming messages pending processing.

        // Create thread for managing handling search messages from multicast group.
        Thread thread = new Thread(this);
        thread.setPriority(8);
        thread.start();

    } // DownloadReceiver().


    /**
     * Process incoming download messages.
     */
    @Override
    public void run() {

        do { // Do until application terminated.

            processIncomingDownloadMessages();

        } while (true);

    } // run().


    /**
     * Process incoming download messages.
     */
    public synchronized void processIncomingDownloadMessages() {

        LinkedList<String> messagesToRemove = new LinkedList<>();

        // Loop over incoming messages and process them.
        Iterator<Map.Entry<String, Message>> messageIterator = incomingDownloadMessages.entrySet().iterator();
        while (messageIterator.hasNext()) {

            // Get current message to process.
            Map.Entry<String, Message> entry = messageIterator.next();
            String messageIdentifier = entry.getKey();
            Message message = entry.getValue();

            // If download request, then process the download request.
            if (message.getPayloadType().equalsIgnoreCase("download-request")) {

                try {

                    // Check the request is for us by verifying that the downloadIdentifier is for this machine.
                    String ourIdentifier = System.getProperty("user.name") + "@" + InetAddress.getLocalHost().getCanonicalHostName();
                    if (message.getTargetPeerIdentifier().equals(ourIdentifier)) {

                        String addedSeparator = "";
                        if (message.getTargetFilePath().charAt(0) != '/') {
                            addedSeparator = "/";
                        }
                        String downloadFileRootPath = configuration.rootDir_ + addedSeparator + message.getTargetFilePath();

                        File fileRequestedDownload = new File(downloadFileRootPath);

                        // Check the requested file to download exists in the root directory and that it can be opened and sent.
                        if (fileRequestedDownload.exists() && fileRequestedDownload.isFile() && !message.getTargetFilePath().contains("..")) {

                            // Set up a TCP Server (give port 0 so ephemeral port assigned).
                            TCPFileServer tcpServer = new TCPFileServer(configuration, downloadFileRootPath, true);
                            int downloadPort = tcpServer.serverSocket.getLocalPort();
                            // Send a download-result message with the TCP server ephemeral port.
                            Message downloadResult = Message.downloadResultMessage(message.getIdentifier(), message.getSerialNumber(), downloadPort);
                            multicastHandler.txMessage(downloadResult);

                        } else { // If download request file path does not exist or is not a file in root, then send error.

                            // Send a download-error message to the peer that made the request.
                            Message downloadError = Message.downloadErrorMessage(message.getIdentifier(), message.getSerialNumber());
                            multicastHandler.txMessage(downloadError);

                        } // if (requested download file can be downloaded).

                    } // if (download request not for this machine).


                } catch (UnknownHostException e) {
                    System.err.println("DownloadReceiver.processIncomingDownloadMessages() Error - Could not get hostname: " + e.getMessage());
                }

            } else { // Otherwise, a download-result or download-error.

                String incomingMessageIdentifier = message.getResponseIDSerialNum();

                // Check response-id and serial number matches a message in the outgoing structure.
                if (outgoingDownloadRequests.containsKey(incomingMessageIdentifier)) { // Match for one of our requests.

                    Message initialRequest = outgoingDownloadRequests.get(incomingMessageIdentifier);
                    showDownloadResponse(initialRequest, message);

                } // If not a result for one of our requests, then ignore.

            } // Anything else does not conform to the protocol so ignore.

            messagesToRemove.add(messageIdentifier);

        } // for (all incoming messages).

        // Remove incoming messages once processed.
        removeProcessedMessages(incomingDownloadMessages, messagesToRemove);

    } // processIncomingDownloadMessages().


    /**
     * Given a message know to be a download message, add the message appropriately to the structures.
     *
     * @param outgoing Whether to add the message to the outgoing or incoming messages structure.
     * @param message Message to add to the message structures.
     */
    public synchronized void addMessage(boolean outgoing, Message message) {

        // Synchronised to prevent simultaneous adding and removing issues due to threading.

        // Create key for message structures.
        String messageIdentifier = message.getIdentifier() + ":" + message.getSerialNumber();

        // Determine which structure to add message to.
        if (!outgoing) {

            // Incoming download messages are messages from other peers to respond to.
            if (incomingDownloadMessages.get(messageIdentifier) == null) { // Add if new.
                incomingDownloadMessages.put(messageIdentifier, message);
            } else { // Updates the message if it exists already.
                incomingDownloadMessages.replace(messageIdentifier, message);
            }

        } else {

            // Outgoing download messages are messages that have been sent out and are awaiting answers.
            if (outgoingDownloadRequests.get(messageIdentifier) == null) { // Add if new.
                outgoingDownloadRequests.put(messageIdentifier, message);
            } else { // Updates the message if it exists already.
                outgoingDownloadRequests.replace(messageIdentifier, message);
            }

        } // if (outgoing or not).

    } // addMessage().


    /**
     * Remove processed messages from list of search messages to process.
     *
     * @param incomingMessages Structure of incoming messages to be processed.
     * @param messagesToRemove List of messages that have been processed and should be removed from incomingMessages.
     */
    public synchronized void removeProcessedMessages(HashMap<String, Message> incomingMessages, LinkedList<String> messagesToRemove) {

        // Synchronised to prevent simultaneous adding and removing issues due to threading.

        incomingMessages.entrySet().removeIf(entry -> messagesToRemove.contains(entry.getKey()));

    } // removeProcessedMessages().


    /**
     * Method for outputting results to stdout for download responses that match one of our download requests.
     *
     * @param initialRequest The initial download request.
     * @param downloadResponse The download request response.
     */
    public void showDownloadResponse(Message initialRequest, Message downloadResponse) {

        System.out.println("----------------------------------------------");

        System.out.println("Download Request: Download From: " + initialRequest.getTargetPeerIdentifier()
                                + ", File To Download: " + initialRequest.getTargetFilePath());

        if (downloadResponse.getPayloadType().equalsIgnoreCase("download-result")) {

            // Get file path to save file to.
            String filePathToSaveTo = FileTreeBrowser.getFile(System.in,true);

            // Create TCP Client and download the file (connect to identifier and downloadPort).
            TCPClient tcpClient = new TCPClient(configuration, downloadResponse.getHostname(), downloadResponse.getFileTransferPort());
            tcpClient.processFile(filePathToSaveTo, true);

            // Output results to the user.
            System.out.println("Download Result: Successfully downloaded " + initialRequest.getTargetFilePath()
                    + " From " + initialRequest.getTargetPeerIdentifier());

            String rootDirName = FileTreeBrowser.getRootDirName();
            String fileToSaveToRootPath = filePathToSaveTo.split(rootDirName)[1];
            System.out.println("File Saved To: " + fileToSaveToRootPath + ".");

        } else { // Download-error.

            System.out.println("Download Result: Could not download the file.");

        }

        System.out.println("----------------------------------------------");

    } // showDownloadResponse().


} // DownloadReceiver{}.
