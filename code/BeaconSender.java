/**
 * BeaconSender is a class used for sending beacons to remote file browsers in a separate thread.
 *
 * @author 170004680, Nov 2020.
 */
public class BeaconSender implements Runnable {


    MulticastHandler multicastHandler;  // Instance of MulticastHandler used for creating beacon messages to send.
    Configuration configuration; // Configuration of MulticastHandler.


    /**
     * Constructor: Creates an instance of BeaconSender as a separate thread.
     *
     * @param multicastHandler Instance of MulticastHandler, used for creating and reading messages.
     */
    BeaconSender(MulticastHandler multicastHandler) {

        this.multicastHandler = multicastHandler;
        this.configuration = multicastHandler.configuration;

        // Create thread for managing sending beacons to multicast group.
        Thread thread = new Thread(this);
        thread.start();

    } // BeaconSender().


    /**
     * Constantly attempt to send new beacons to the multicast group in a separate thread.
     */
    @Override
    public void run() {

        do { // Do until application terminated.

            String beaconSent = txBeacon(); // Send beacon according to the specification.

            try { // Send beacons every configuration.maxBeaconPeriod_.
                Thread.sleep(configuration.maximumBeaconPeriod_);
            } catch (InterruptedException ignored) {
            }

        } while (true);

    } // run().


    /**
     * Method for sending a beacon to the multicast group.
     *
     * @return String representing the beacon sent, or null if no beacon sent.
     */
    public String txBeacon() {

        // Encode beacon message so correct protocol format is used.
        Message beacon = Message.beaconMessage(configuration);

        // Send beacon to the multicast group.
        boolean sent = multicastHandler.txMessage(beacon);

        if (sent) {
            return beacon.toString();
        } else {
            return null;
        }

    } // txBeacon().


} // BeaconsSender{}.
