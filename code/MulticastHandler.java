import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * MulticastHandler is a class used for handling control-plane communications of the remote file browser.
 *
 * @author 170004680, Nov 2020.
 */
public class MulticastHandler implements Runnable {


    private MulticastSocket multicastSocket; // Connection to Multicast group.
    public final Configuration configuration; // Current FileTreeBrowser configuration.

    public BeaconSender beaconSender; // BeaconSender thread object for periodically sending beacons.
    public BeaconReceiver beaconReceiver; // BeaconReceiver thread object for periodically reading beacons.

    public SearchReceiver searchReceiver; // SearchReceiver thread object for handing incoming search messages.
    public DownloadReceiver downloadReceiver; // FileMessageReceiver thread object handles download messages.
    public UploadReceiver uploadReceiver; // FileMessageReceiver thread object handles upload messages.
    public DeleteReceiver deleteReceiver; // FileMessageReceiver thread object handles delete messages.


    /**
     * Constructor: Creates instance of MulticastHandler.
     *
     * @param config Configuration object containing multicast options to set.
     */
    MulticastHandler(Configuration config) {

        this.configuration = config;

        try {

            // Get InetAddress of multicast group, and create socket for the group.
            this.configuration.mGroup_ = InetAddress.getByName(this.configuration.mAddr_);
            multicastSocket = new MulticastSocket(this.configuration.mPort_);

            configuration.log_.writeLog("Multicast Socket Created: "
                    + this.configuration.mAddr_ + ", " + this.configuration.mPort_); // Create log.

            // Set up multicast socket according to the configuration.
            multicastSocket.setTimeToLive(this.configuration.mTTL_);
            multicastSocket.setLoopbackMode(this.configuration.loopbackOff_);
            multicastSocket.setReuseAddress(this.configuration.reuseAddr_);
            multicastSocket.setSoTimeout(this.configuration.soTimeout_); // Non-blocking.

            join(); // Join the multicast group.
            configuration.log_.writeLog("Joined Multicast Group: " + configuration.mGroup_); // Create log.

            // Create search message receiver for handling search messages sent over the multicast group.
            this.searchReceiver = new SearchReceiver(this);
            // Create download message receiver for handling messages sent over the multicast group.
            this.downloadReceiver = new DownloadReceiver(this);
            // Create upload message receiver for handling messages sent over the multicast group.
            this.uploadReceiver = new UploadReceiver(this);
            // Create delete message receiver for handling messages sent over the multicast group.
            this.deleteReceiver = new DeleteReceiver(this);

            // Create beacon sender and receivers, which act individually as threads.
            this.beaconSender = new BeaconSender(this);
            this.beaconReceiver = new BeaconReceiver(this);

            // Create thread for this class which listens for multicast messages and parses them.
            Thread thread = new Thread(this);
            thread.start();

        } catch (Exception e) {
            System.err.println("MulticastHandler.MulticastHandler() Error: " + e.getMessage());
        }

    } // MulticastHandler().


    /**
     * Constantly listen for new UDP multicast messages, parse them and add them to the correct data structures.
     */
    @Override
    public void run() {

        do { // Do until application terminated.

            Message message = rxMessage(); // Read message according to the specification.

            if (message != null) { // A message was read from the multicast socket.

                // Handle message according to message type.
                if (message.getPayloadType().contains("beacon")) {

                    beaconReceiver.addBeacon(message);

                } else if (message.getPayloadType().contains("search")) {

                    searchReceiver.addSearchMessage(false, message);

                } else if (message.getPayloadType().contains("download")) {

                    downloadReceiver.addMessage(false, message);

                } else if (message.getPayloadType().contains("upload")) {

                    uploadReceiver.addMessage(false, message);

                } else if (message.getPayloadType().contains("delete")) {

                    deleteReceiver.addMessage(false, message);

                } // if (message of type).

            } // if (message != null).

        } while (true);

    } // run().


    /**
     * Method for updating message structures based on received messages from the multicast socket.
     *
     * @return Message object representing the message read, or null if no message read.
     */
    public Message rxMessage() {

        // Discovery beacons should not exceed a given maximum number of bytes, as specified by the configuration.
        byte[] buffer = new byte[1024];

        // Read from the multicast group into buffer.
        if (rx(buffer)) {

            String message = new String(buffer, StandardCharsets.US_ASCII).trim();

            Message messageObj = Message.parseMessage(message);

            if (messageObj != null) {
                configuration.log_.writeLog("Message Read: " + messageObj.toString()); // Create log.
            }

            return messageObj;

        } // if (rx(buffer)).

        return null; // Forced a read for message. If there aren't any, then indicate so by returning null.

    } // rxMessage().


    /**
     * Method for reading from the multicast group.
     *
     * @param buffer Byte buffer to read into.
     * @return Byte array containing the packet content read from the multicast socket.
     */
    public boolean rx(byte[] buffer) {

        DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
        boolean done = false;

        try {

            multicastSocket.receive(datagramPacket);
            done = true;

        } catch (IOException ignored) {
        }

        return done;

    } // rx().


    /**
     * Method for sending messages to the multicast group.
     *
     * @return Message object representing the message read, or null if no message read.
     */
    public boolean txMessage(Message message) {

        byte[] buffer;

        if (message != null) {

            buffer = message.toString().getBytes(StandardCharsets.US_ASCII); // Get bytes from the beacon string to send.

            boolean sent = tx(buffer); // Send beacon.

            if (sent) {
                configuration.log_.writeLog("Message Sent: " + message.toString()); // Create log.
            }

            return sent;

        } else {
            System.err.println("MulticastHandler.txMessage() Error: Could not send message.");
            return false; // Message not sent.
        }

    } // txMessage().


    /**
     * Method for sending to the multicast group.
     *
     * @param buffer Byte buffer to send from.
     *
     * @return True if send successful to multicast endpoint, false if not.
     */
    public boolean tx(byte[] buffer) {

        boolean done = false;
        DatagramPacket datagramPacket;

        try {

            datagramPacket = new DatagramPacket(buffer, buffer.length, configuration.mGroup_, configuration.mPort_);
            multicastSocket.send(datagramPacket);
            done = true;

        } catch (SocketTimeoutException e) {
            System.err.println("MulticastHandler.tx() Error: Could not send - " + e.getMessage());
        } catch (IOException e) {
            System.err.println("MulticastHandler.tx() error: " + e.getMessage());
        }

        return done;

    } // tx().


    /**
     * Method for joining the multicast group.
     */
    public void join() {

        try {
            multicastSocket.joinGroup(configuration.mGroup_);
        } catch (IOException e) {
            System.err.println("MulticastHandler.join() Error: " + e.getMessage());
        }

    } // join().


    /**
     * Method for leaving the multicast group.
     */
    public void leave() {

        try {
            multicastSocket.leaveGroup(configuration.mGroup_);
            multicastSocket.close();
        } catch (IOException e) {
            System.err.println("MulticastHandler.leave() Error: " + e.getMessage());
        }

    } // leave().


} // MulticastHandler{}.
