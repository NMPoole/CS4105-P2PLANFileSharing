import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * UploadReceiver is a class for handling incoming upload messages given by MulticastHandler in a separate thread.
 *
 * @author 170004680, Nov 2020.
 */
public class UploadReceiver implements Runnable {


    MulticastHandler multicastHandler; // Instance of MulticastHandler used.
    Configuration configuration; // Configuration of MulticastHandler.

    HashMap<String, Message> outgoingUploadRequests; // HashMap of active upload requests that have been sent.
    HashMap<String, Message> incomingUploadMessages; // HashMap of incoming download messages pending processing.


    /**
     * Constructor: Creates an instance of UploadReceiver as a separate thread.
     *
     * @param multicastHandler Instance of MulticastHandler, used for creating and reading protocol messages.
     */
    UploadReceiver(MulticastHandler multicastHandler) {

        this.multicastHandler = multicastHandler;
        this.configuration = multicastHandler.configuration;

        this.outgoingUploadRequests = new HashMap<>(); // HashMap of active download requests that have been sent.
        this.incomingUploadMessages = new HashMap<>(); // HashMap of incoming messages pending processing.

        // Create thread for managing handling search messages from multicast group.
        Thread thread = new Thread(this);
        thread.setPriority(9);
        thread.start();

    } // UploadReceiver().


    /**
     * Process incoming upload messages.
     */
    @Override
    public void run() {

        do { // Do until application terminated.

            processIncomingUploadMessages();

        } while (true);

    } // run().


    /**
     * Process incoming upload messages.
     */
    public synchronized void processIncomingUploadMessages() {

        LinkedList<String> messagesToRemove = new LinkedList<>();

        // Loop over incoming messages and process them.
        Iterator<Map.Entry<String, Message>> messageIterator = incomingUploadMessages.entrySet().iterator();
        while (messageIterator.hasNext()) {

            // Get current message to process.
            Map.Entry<String, Message> entry = messageIterator.next();
            String messageIdentifier = entry.getKey();
            Message message = entry.getValue();

            // If upload request, then process the upload request.
            if (message.getPayloadType().equalsIgnoreCase("upload-request")) {

                try {

                    // Check the request is for us by verifying that the identifier is for this machine.
                    String ourIdentifier = System.getProperty("user.name") + "@" + InetAddress.getLocalHost().getCanonicalHostName();
                    if (message.getTargetPeerIdentifier().equals(ourIdentifier)) {

                        String addedSeparator = "";
                        if (message.getTargetFilePath().charAt(0) != '/') {
                            addedSeparator = "/";
                        }
                        String uploadFileRootPath = configuration.rootDir_ + addedSeparator + message.getTargetFilePath();

                        // Check file path given is acceptable.
                        boolean validFileLocation = checkFile(uploadFileRootPath, message.getTargetFilePath());

                        if (validFileLocation) {

                            // Set up a TCP Server (give port 0 so ephemeral port assigned).
                            TCPFileServer tcpServer = new TCPFileServer(configuration, uploadFileRootPath, false);
                            int uploadPort = tcpServer.serverSocket.getLocalPort();
                            // Send a upload-result message with the TCP server ephemeral port.
                            Message uploadResult = Message.uploadResultMessage(message.getIdentifier(), message.getSerialNumber(), uploadPort);
                            multicastHandler.txMessage(uploadResult);

                        } else {

                            // Send a upload-error message to the peer that made the request.
                            Message uploadError = Message.uploadErrorMessage(message.getIdentifier(), message.getSerialNumber());
                            multicastHandler.txMessage(uploadError);

                        } // If (valid upload location given).

                    } // If upload request not for this machine, then ignore this message.


                } catch (UnknownHostException e) {
                    System.err.println("UploadReceiver.processIncomingUploadMessages() Error - Could not get hostname: " + e.getMessage());
                }

            } else { // Otherwise, a upload-result or upload-error.

                String incomingMessageIdentifier = message.getResponseIDSerialNum();

                // Check response-id and serial number matches a message in the outgoing structure.
                if (outgoingUploadRequests.containsKey(incomingMessageIdentifier)) { // Match for one of our requests.

                    Message initialRequest = outgoingUploadRequests.get(incomingMessageIdentifier);
                    showUploadResponse(initialRequest, message);

                } // If not a result for one of our requests, then ignore.

            } // Anything else does not conform to the protocol so ignore.

            messagesToRemove.add(messageIdentifier);

        } // for (all incoming messages).

        // Remove incoming messages once processed.
        removeProcessedMessages(incomingUploadMessages, messagesToRemove);

    } // processIncomingUploadMessages().


    /**
     * Check that a provided file path to upload a file to is a valid file path.
     *
     * @param rootPath The file path to upload to relative to the root directory for this FIleTreeBrowser.
     * @param path The provided path from the upload message request.
     *
     * @return True if location is valid and able to upload to, false otherwise.
     */
    public boolean checkFile(String rootPath, String path) {

        File file = new File(rootPath);

        if (!path.contains("..")) { // Only permit direct path from the root.

            if (file.exists() && !file.isDirectory()) { // Existing file.

                if (file.canWrite()) {  // Make sure we can overwrite the file.
                    return true;
                }

            } else { // File does not exist, so it should be creatable.

                if (path.charAt(path.length() - 1) != '/') { // Need file not a directory.

                    // Check filename provided is valid. Format: string.string
                    String[] filePathParts = path.split("/");
                    String fileNamePart = filePathParts[filePathParts.length - 1];
                    String[] fileNameParts = fileNamePart.split("\\.");

                    if (fileNameParts.length == 2) {

                        if (fileNameParts[0].length() != 0 && fileNameParts[1].length() != 0) {

                            try {
                                return file.createNewFile();
                            } catch (IOException ignore) {
                            }

                        } // if (filename has valid name and extension).

                    } // if (filename has two parts, name and extension).

                } // if (file not a directory).

            } // if (file exists), else.

        } // if (user path contains "..").

        return false;

    } // checkFile().


    /**
     * Given a message know to be an upload message, add the message appropriately to the structures.
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
            if (incomingUploadMessages.get(messageIdentifier) == null) { // Add if new.
                incomingUploadMessages.put(messageIdentifier, message);
            } else { // Updates the message if it exists already.
                incomingUploadMessages.replace(messageIdentifier, message);
            }

        } else {

            // Outgoing download messages are messages that have been sent out and are awaiting answers.
            if (outgoingUploadRequests.get(messageIdentifier) == null) { // Add if new.
                outgoingUploadRequests.put(messageIdentifier, message);
            } else { // Updates the message if it exists already.
                outgoingUploadRequests.replace(messageIdentifier, message);
            }

        } // if (outgoing or not).

    } // addMessage().


    /**
     * Remove processed messages from list of messages to process.
     *
     * @param incomingMessages Structure of incoming messages to be processed.
     * @param messagesToRemove List of messages that have been processed and should be removed from incomingMessages.
     */
    public synchronized void removeProcessedMessages(HashMap<String, Message> incomingMessages, LinkedList<String> messagesToRemove) {

        incomingMessages.entrySet().removeIf(entry -> messagesToRemove.contains(entry.getKey()));

    } // removeProcessedMessages().


    /**
     * Method for outputting results to stdout for upload responses that match one of our upload requests.
     *
     * @param initialRequest The initial upload request.
     * @param uploadResponse The upload request response.
     */
    public void showUploadResponse(Message initialRequest, Message uploadResponse) {

        System.out.println("----------------------------------------------");

        System.out.println("Upload Request: Upload To: " + initialRequest.getTargetPeerIdentifier()
                + ", Location To Upload: " + initialRequest.getTargetFilePath());

        if (uploadResponse.getPayloadType().equalsIgnoreCase("upload-result")) {

            // Get file path of file to upload.
            String fileToUploadPath = FileTreeBrowser.getFile(System.in,false);

            // Create TCP Client and upload the file (connect to identifier and uploadPort).
            TCPClient tcpClient = new TCPClient(configuration, uploadResponse.getHostname(), uploadResponse.getFileTransferPort());
            tcpClient.processFile(fileToUploadPath, false);

            // Output results to the user.
            System.out.println("Upload Result: Successfully Uploaded To " + initialRequest.getTargetFilePath()
                    + " At " + initialRequest.getTargetPeerIdentifier());

            String rootDirName = FileTreeBrowser.getRootDirName();
            String fileUploadedRootPath = fileToUploadPath.split(rootDirName)[1];
            System.out.println("File Uploaded: " + fileUploadedRootPath + ".");

        } else { // Upload-error.

            System.out.println("Upload Result: Could not upload the file.");

        }

        System.out.println("----------------------------------------------");

    } // showUploadResponse().


} // UploadReceiver{}.
