import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * DeleteReceiver is a class used for handling delete messages received by MulticastHandler in a separate thread.
 *
 * @author 170004680, Nov 2020.
 */
public class DeleteReceiver implements Runnable {


    MulticastHandler multicastHandler; // Instance of MulticastHandler used.
    Configuration configuration; // Configuration of MulticastHandler.

    HashMap<String, Message> outgoingDeleteRequests; // HashMap of active download requests that have been sent.
    HashMap<String, Message> incomingDeleteMessages; // HashMap of incoming download messages pending processing.


    /**
     * Constructor: Creates an instance of DeleteReceiver as a separate thread.
     *
     * @param multicastHandler Instance of MulticastHandler, used for creating and reading protocol messages.
     */
    DeleteReceiver(MulticastHandler multicastHandler) {

        this.multicastHandler = multicastHandler;
        this.configuration = multicastHandler.configuration;

        this.outgoingDeleteRequests = new HashMap<>(); // HashMap of active download requests that have been sent.
        this.incomingDeleteMessages = new HashMap<>(); // HashMap of incoming download messages pending processing.

        // Create thread for managing handling search messages from multicast group.
        Thread thread = new Thread(this);
        thread.setPriority(8);
        thread.start();


    } // DeleteReceiver().


    /**
     * Process incoming delete messages.
     */
    @Override
    public void run() {

        do { // Do until application terminated.

            processIncomingDeleteMessages();

        } while (true);

    } // run().


    /**
     * Process incoming delete messages.
     */
    public synchronized void processIncomingDeleteMessages() {

        LinkedList<String> messagesToRemove = new LinkedList<>();

        // Loop over incoming messages and process them.
        Iterator<Map.Entry<String, Message>> messageIterator = incomingDeleteMessages.entrySet().iterator();
        while (messageIterator.hasNext()) {

            // Get current message to process.
            Map.Entry<String, Message> entry = messageIterator.next();
            String messageIdentifier = entry.getKey();
            Message message = entry.getValue();

            // If delete request, then process the delete request.
            if (message.getPayloadType().equalsIgnoreCase("delete-request")) {

                try {

                    // Check the request is for us by verifying that the identifier is for this machine.
                    String ourIdentifier = System.getProperty("user.name") + "@" + InetAddress.getLocalHost().getCanonicalHostName();
                    if (message.getTargetPeerIdentifier().equals(ourIdentifier)) {

                        String addedSeparator = "";
                        if (message.getTargetFilePath().charAt(0) != '/') {
                            addedSeparator = "/";
                        }
                        String deleteFileRootPath = configuration.rootDir_ + addedSeparator + message.getTargetFilePath();

                        File fileRequestedDelete = new File(deleteFileRootPath);

                        // Check the requested file to delete exists in the root dir and that it can be opened and sent.
                        if (fileRequestedDelete.exists() && fileRequestedDelete.isFile()
                                && !message.getTargetFilePath().contains("..") && fileRequestedDelete.delete()) {

                            // Send a delete-result message as the file was successfully deleted.
                            Message deleteResult = Message.deleteResultMessage(message.getIdentifier(), message.getSerialNumber());
                            multicastHandler.txMessage(deleteResult);

                        } else { // If delete request file path does not exist, or is not a file, or could not be deleted.

                            // Send a delete-error message to the peer that made the request.
                            Message deleteError = Message.deleteErrorMessage(message.getIdentifier(), message.getSerialNumber());
                            multicastHandler.txMessage(deleteError);

                        } // if (requested file can be deleted).

                    } // If delete request not for this machine, then ignore this message.


                } catch (UnknownHostException e) {
                    System.err.println("DeleteReceiver.processIncomingDeleteMessages() Error - Could not get hostname: " + e.getMessage());
                }

            } else { // Otherwise, a delete-result or delete-error.

                String incomingMessageIdentifier = message.getResponseIDSerialNum();

                // Check response-id and serial number matches a message in the outgoing structure.
                if (outgoingDeleteRequests.containsKey(incomingMessageIdentifier)) { // Match for one of our requests.

                    Message initialRequest = outgoingDeleteRequests.get(incomingMessageIdentifier);
                    showDeleteResponse(initialRequest, message);

                } // If not a result for one of our requests, then ignore.

            } // Anything else does not conform to the protocol so ignore.

            messagesToRemove.add(messageIdentifier);

        } // for (all incoming messages).

        // Remove incoming messages once processed.
        removeProcessedMessages(incomingDeleteMessages, messagesToRemove);

    } // processIncomingDeleteMessages().


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
            if (incomingDeleteMessages.get(messageIdentifier) == null) { // Add if new.
                incomingDeleteMessages.put(messageIdentifier, message);
            } else { // Updates the message if it exists already.
                incomingDeleteMessages.replace(messageIdentifier, message);
            }

        } else {

            // Outgoing download messages are messages that have been sent out and are awaiting answers.
            if (outgoingDeleteRequests.get(messageIdentifier) == null) { // Add if new.
                outgoingDeleteRequests.put(messageIdentifier, message);
            } else { // Updates the message if it exists already.
                outgoingDeleteRequests.replace(messageIdentifier, message);
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

        incomingMessages.entrySet().removeIf(entry -> messagesToRemove.contains(entry.getKey()));

    } // removeProcessedMessages().


    /**
     * Method for outputting results to stdout for delete responses that match one of our delete requests.
     *
     * @param initialRequest The initial delete request.
     * @param deleteResponse The delete request response.
     */
    public void showDeleteResponse(Message initialRequest, Message deleteResponse) {

        System.out.println("----------------------------------------------");

        System.out.println("Delete Request: Delete At: " + initialRequest.getTargetPeerIdentifier()
                + ", File To Delete: " + initialRequest.getTargetFilePath());

        if (deleteResponse.getPayloadType().equalsIgnoreCase("delete-result")) {

            // Output results to the user.
            System.out.println("Delete Result: Successfully deleted " + initialRequest.getTargetFilePath()
                    + " At " + initialRequest.getTargetPeerIdentifier());

        } else { // Delete-error.

            // Output results to the user.
            System.out.println("Delete Result: Failed To Delete " + initialRequest.getTargetFilePath()
                    + " At " + initialRequest.getTargetPeerIdentifier());

        }

        System.out.println("----------------------------------------------");

    } // showDeleteResponse().


} // DeleteReceiver{}.
