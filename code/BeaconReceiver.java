import java.util.HashMap;

/**
 * BeaconReceiver is a class used for reading in beacons for remote file browsers in a separate thread.
 *
 * @author 170004680, Nov 2020.
 */
public class BeaconReceiver implements Runnable {


    MulticastHandler multicastHandler; // Instance of MulticastHandler.
    Configuration configuration; // Configuration of MulticastHandler.
    HashMap<String, Message> beacons; // HashMap of currently available remote file browser via beacons.


    /**
     * Constructor: Creates an instance of BeaconReceiver as a separate thread.
     *
     * @param multicastHandler Instance of MulticastHandler, used for creating and reading messages.
     */
    BeaconReceiver(MulticastHandler multicastHandler) {

        this.multicastHandler = multicastHandler;
        this.configuration = multicastHandler.configuration;
        this.beacons = new HashMap<>(); // Structure containing active beacons.

        // Create thread for managing reading beacons from multicast group.
        Thread thread = new Thread(this);
        thread.start();

    } // BeaconReceiver().


    /**
     * Maintains active beacons by removing beacons that have expired.
     */
    @Override
    public void run() {

        do { // Do until application terminated.

            // Removes beacons from beacons structure if maximumBeaconTime_ has expired.
            removeExpiredBeacons();

            try { // Read beacons every 'configuration.sleepTime_'.
                Thread.sleep(configuration.sleepTime_);
            } catch (InterruptedException ignored) {
            }

        } while (true);

    } // run().


    /**
     * Given a message know to be a beacon, add the beacon appropriately to the beacons structure.
     *
     * @param beacon Beacon to be added to beacons.
     */
    public synchronized void addBeacon(Message beacon) {

        // Synchronised to prevent simultaneous adding and removing beacons issues due to threading.

        // Identify beacons by the identifier (username@hostname) and port number.
        String beaconIdentifier = beacon.getIdentifier() + ":" + beacon.getServerPort();

        if (beacons.get(beaconIdentifier) == null) { // Add beacon if new.
            beacons.put(beaconIdentifier, beacon);
        } else { // Updates the beacon if it exists already with new timestamp info.
            beacons.replace(beaconIdentifier, beacon);
        }

    } // addBeacon().


    /**
     * Remove expired beacons from list of beacons.
     */
    public synchronized void removeExpiredBeacons() {

        // Synchronised to prevent simultaneous adding and removing beacons issues due to threading.

        // Beacon has expired if it has been longer than maximumBeaconPeriod_ since received.
        beacons.entrySet().removeIf(entry ->
                (System.currentTimeMillis() - entry.getValue().getTimestamp().getTime()) > configuration.maximumBeaconPeriod_);

    } // removeExpiredBeacons().


    /**
     * Display beacons to stdout.
     */
    public void showBeacons() {

        System.out.println("Beacons:");

        int beaconNum = 0;

        for (Message beaconMessage : beacons.values()) {

            String beaconString = "";

            beaconString += "(" + beaconNum++ + ") ";
            beaconString += "Identifier: " + beaconMessage.getIdentifier();
            beaconString += ", ";
            beaconString += "Port: " + beaconMessage.getServerPort();
            beaconString += ", ";
            beaconString += "Services: " + beaconMessage.getServices();
            beaconString += ".";

            System.out.println(beaconString);

        } // for (all beacon messages).

    } // showBeacons().


} // BeaconReceiver{}.
