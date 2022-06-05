import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Message class is an object that holds all of the information that can be structured in a protocol message.
 *
 * @author 170004680, Nov 2020.
 */
public class Message {


    private static final long initialSerialNumber = System.currentTimeMillis();

    // Header Attributes:

    private String username = ""; // Identifier: username@hostname.
    private String hostname = ""; // Identifier: username@hostname.
    private long serialNumber = -1; // Serial Number.
    private Date timestamp = new Date(); // Timestamp as Date: "yyyyMMdd-HHmmss.SSS" format.

    // Payload Attributes:

    private String payloadType = ""; // "beacon", "search-...", "download-...", "upload-...", "delete-...".

    private String responseIdentifier = ""; // Identifier for response which matches the identifier of the request.
    private long responseSerialNumber = -1; // Serial number for response which matches the request serial number.

    // Beacon Payload Attributes.
    private int serverPort = -1; // Port number the file browser that sent the message is listening on.
    private boolean remoteBrowseAvailable = false; // Whether the remote file browser supports remote browsing.
    private boolean searchAvailable = false; // Whether the remote file browser supports remote search.
    private String searchMatch = ""; // Search Matching: "none", "path", "path-filename", "path-filename-substring".
    private boolean downloadAvailable = false; // Whether the remote file browser supports remote download.
    private boolean uploadAvailable = false; // Whether the remote file browser supports remote upload.
    private boolean deleteAvailable = false; // Whether the remote file browser supports remote deletion.

    // Search Payload Attributes:
    private String searchType = ""; // Type of search being made, i.e. 'path, 'filename', and 'substring'.
    private String searchString = ""; // Query string used in the search.
    private String searchFileString = ""; // Path to file meeting the criteria of the search.

    // Download/Upload/Delete Payload Attributes:
    private String targetPeerIdentifier = ""; // Remote peer to download/upload/delete.
    private String targetFilePath = ""; // Exact path to file to download/upload/delete at the remote peer.
    private int fileTransferPort = -1; // Port number indicated by remote peer to initiate download/upload.


    // Message Creation Methods:

    /**
     * Create a message object required to format a beacon message to send over multicast.
     *
     * @param configuration Current FileTreeBrowser configuration.
     *
     * @return Message object with the beacon information set.
     */
    public static Message beaconMessage(Configuration configuration) {

        Message message = new Message();

        message.setHeader(message);
        message.setPayloadType("beacon");
        message.setServerPort(configuration.mPort_);
        message.setRemoteBrowseAvailable(configuration.remoteBrowse_);
        message.setSearchAvailable(configuration.search_);
        message.setSearchMatch(configuration.searchMatch_);
        message.setDownloadAvailable(configuration.download_);
        message.setUploadAvailable(configuration.upload_);
        message.setDeleteAvailable(configuration.delete_);

        return message;

    } // beaconMessage().


    /**
     * Create a message object required to format a search request message to send over multicast.
     *
     * @param searchType Type of search, i.e. 'path', 'filename', 'substring'.
     * @param searchString Query string for search.
     *
     * @return Message object with search-request message information set.
     */
    public static Message searchRequestMessage(String searchType, String searchString) {

        Message message = new Message();

        message.setHeader(message);
        message.setPayloadType("search-request");
        message.setSearchType(searchType);
        message.setSearchString(searchString);

        return message;

    } // searchRequestMessage().


    /**
     * Create a message object required to format a search result message to send over multicast.
     *
     * @param responseIdentifier Identifier of the search-request this search-result corresponds to.
     * @param serialNumber Serial number of the search-request this search-result corresponds to.
     * @param searchFileString String representing path to a file that satisfies the search request.
     *
     * @return Message object with search-result message information set.
     */
    public static Message searchResultMessage(String responseIdentifier, long serialNumber, String searchFileString) {

        Message message = new Message();

        message.setHeader(message);
        message.setPayloadType("search-result");
        message.setResponseIdentifier(responseIdentifier);
        message.setResponseSerialNumber(serialNumber);
        message.setSearchFileString(searchFileString);

        return message;

    } // searchResultMessage().


    /**
     * Create a message object required to format a search error message to send over multicast.
     *
     * @param responseIdentifier Identifier of the search-request this search-result corresponds to.
     * @param serialNumber Serial number of the search-request this search-result corresponds to.
     *
     * @return Message object with search-error message information set.
     */
    public static Message searchErrorMessage(String responseIdentifier, long serialNumber) {

        Message message = new Message();

        message.setHeader(message);
        message.setPayloadType("search-error");
        message.setResponseIdentifier(responseIdentifier);
        message.setResponseSerialNumber(serialNumber);

        return message;

    } // searchErrorMessage().


    /**
     * Create a message object to format a download request message to send over multicast.
     *
     * @param downloadIdentifier Identifier (usernam@hostname) for peer to download from.
     * @param downloadFilePath Exact file path to the file to download at the peer.
     *
     * @return Message object with download-request information set.
     */
    public static Message downloadRequestMessage(String downloadIdentifier, String downloadFilePath) {

        Message message = new Message();

        message.setHeader(message);
        message.setPayloadType("download-request");
        message.setTargetPeerIdentifier(downloadIdentifier);
        message.setTargetFilePath(downloadFilePath);

        return message;

    } // downloadRequestMessage().


    /**
     * Create a message object to format a download result message to send over multicast.
     *
     * @param responseIdentifier Identifier (usernam@hostname) for peer who requested the download.
     * @param serialNumber Serial number associated with the download request message.
     * @param downloadPort Ephemeral port for the requester to download the file from.
     *
     * @return Message object with download-result information set.
     */
    public static Message downloadResultMessage(String responseIdentifier, long serialNumber, int downloadPort) {

        Message message = new Message();

        message.setHeader(message);
        message.setPayloadType("download-result");
        message.setResponseIdentifier(responseIdentifier);
        message.setResponseSerialNumber(serialNumber);
        message.setFileTransferPort(downloadPort);

        return message;

    } // downloadResultMessage().


    /**
     * Create a message object to format a download error message to send over multicast.
     *
     * @param responseIdentifier Identifier (usernam@hostname) for peer who requested the download.
     * @param serialNumber Serial number associated with the download request message.
     *
     * @return Message object with download-error information set.
     */
    public static Message downloadErrorMessage(String responseIdentifier, long serialNumber) {

        Message message = new Message();

        message.setHeader(message);
        message.setPayloadType("download-error");
        message.setResponseIdentifier(responseIdentifier);
        message.setResponseSerialNumber(serialNumber);

        return message;

    } // downloadErrorMessage().


    /**
     * Create a message object to format a upload request message to send over multicast.
     *
     * @param uploadIdentifier Identifier (usernam@hostname) for peer to upload from.
     * @param uploadFilePath Exact file path to the file to upload at the peer.
     *
     * @return Message object with upload-request information set.
     */
    public static Message uploadRequestMessage(String uploadIdentifier, String uploadFilePath) {

        Message message = new Message();

        message.setHeader(message);
        message.setPayloadType("upload-request");
        message.setTargetPeerIdentifier(uploadIdentifier);
        message.setTargetFilePath(uploadFilePath);

        return message;

    } // uploadRequestMessage().


    /**
     * Create a message object to format a upload result message to send over multicast.
     *
     * @param responseIdentifier Identifier (usernam@hostname) for peer who requested the upload.
     * @param serialNumber Serial number associated with the upload request message.
     * @param uploadPort Ephemeral port for the requester to upload the file to.
     *
     * @return Message object with upload-result information set.
     */
    public static Message uploadResultMessage(String responseIdentifier, long serialNumber, int uploadPort) {

        Message message = new Message();

        message.setHeader(message);
        message.setPayloadType("upload-result");
        message.setResponseIdentifier(responseIdentifier);
        message.setResponseSerialNumber(serialNumber);
        message.setFileTransferPort(uploadPort);

        return message;

    } // uploadResultMessage().


    /**
     * Create a message object to format a upload error message to send over multicast.
     *
     * @param responseIdentifier Identifier (usernam@hostname) for peer who requested the upload.
     * @param serialNumber Serial number associated with the upload request message.
     *
     * @return Message object with upload-error information set.
     */
    public static Message uploadErrorMessage(String responseIdentifier, long serialNumber) {

        Message message = new Message();

        message.setHeader(message);
        message.setPayloadType("upload-error");
        message.setResponseIdentifier(responseIdentifier);
        message.setResponseSerialNumber(serialNumber);

        return message;

    } // uploadErrorMessage().


    /**
     * Create a message object to format a delete request message to send over multicast.
     *
     * @param deleteIdentifier Identifier (usernam@hostname) for peer to delete th specified file.
     * @param deleteFilePath Exact file path to the file to delete at the peer.
     *
     * @return Message object with delete-request information set.
     */
    public static Message deleteRequestMessage(String deleteIdentifier, String deleteFilePath) {

        Message message = new Message();

        message.setHeader(message);
        message.setPayloadType("delete-request");
        message.setTargetPeerIdentifier(deleteIdentifier);
        message.setTargetFilePath(deleteFilePath);

        return message;

    } // deleteRequestMessage().


    /**
     * Create a message object to format a delete result message to send over multicast.
     *
     * @param responseIdentifier Identifier (usernam@hostname) for peer who requested the deletion.
     * @param serialNumber Serial number associated with the delete request message.
     *
     * @return Message object with delete-result information set.
     */
    public static Message deleteResultMessage(String responseIdentifier, long serialNumber) {

        Message message = new Message();

        message.setHeader(message);
        message.setPayloadType("delete-result");
        message.setResponseIdentifier(responseIdentifier);
        message.setResponseSerialNumber(serialNumber);

        return message;

    } // deleteResultMessage().


    /**
     * Create a message object to format a delete error message to send over multicast.
     *
     * @param responseIdentifier Identifier (usernam@hostname) for peer who requested the deletion.
     * @param serialNumber Serial number associated with the delete request message.
     *
     * @return Message object with delete-error information set.
     */
    public static Message deleteErrorMessage(String responseIdentifier, long serialNumber) {

        Message message = new Message();

        message.setHeader(message);
        message.setPayloadType("delete-error");
        message.setResponseIdentifier(responseIdentifier);
        message.setResponseSerialNumber(serialNumber);

        return message;

    } // deleteErrorMessage().


    // Message Parsing Methods:

    /**
     * Given a message as a string, break the message down into its components for use.
     *
     * @param message The message to read/interpret as a string.
     *
     * @return The message type, i.e., "beacon", "search", "download", "upload", "delete".
     */
    public static Message parseMessage(String message) {

        Message messageObj = new Message();

        String[] messageParts = message.split(":");

        if (messageParts.length < 3) {
            return null;
        }

        // Read message header.
        messageObj.setIdentifier(messageParts[1]);
        messageObj.setSerialNumber(messageParts[2]);

        try {
            messageObj.setTimestamp(messageParts[3]);
        } catch (ParseException e) {
            System.err.println("Message.parseMessage() Error - Could not parse date: " + e.getMessage());
        }

        messageObj.setPayloadType(messageParts[4]);
        String payloadType = messageObj.getPayloadType();

        // Read messagePayload
        String[] payload = Arrays.copyOfRange(messageParts, 5, messageParts.length);

        if (payloadType.contains("beacon")) {

            parsePayloadBeacon(messageObj, payload);

        } else if (payloadType.contains("search")) {

            parsePayloadSearch(messageObj, payload);

        } else if (payloadType.contains("download")) {

            parsePayloadDownload(messageObj, payload);

        } else if (payloadType.contains("upload")) {

            parsePayloadUpload(messageObj, payload);

        } else if (payloadType.contains("delete")) {

            parsePayloadDelete(messageObj, payload);

        } else {
            return null;
            // Ignore none protocol messages.
        }

        return messageObj;

    } // readMessage().


    /**
     * Given a message determined to be of the beacon type, read the beacon payload contents into usable memory.
     *
     * @param currMessage The current message object to update and return with updated payload data.
     * @param payload Array of strings representing the payload data to be read.
     *
     */
    public static void parsePayloadBeacon(Message currMessage, String[] payload) {

        // Read payload server port data and update to message object.
        currMessage.setServerPort(payload[0]);

        // Read payload service options data into message object.
        String[] services = payload[1].split(",");

        for (String currService : services) {

            String[] servicePair = currService.split("=");
            String serviceName = servicePair[0];
            String serviceStatusString = servicePair[1];
            boolean serviceStatus = serviceStatusString.equals("true");

            switch (serviceName) {
                case "remoteBrowse":
                    currMessage.setRemoteBrowseAvailable(serviceStatus);
                    break;
                case "search":
                    currMessage.setSearchAvailable(serviceStatus);
                    break;
                case "download":
                    currMessage.setDownloadAvailable(serviceStatus);
                    break;
                case "upload":
                    currMessage.setUploadAvailable(serviceStatus);
                    break;
                case "delete":
                    currMessage.setDeleteAvailable(serviceStatus);
                    break;
                default: // searchMatch.
                    currMessage.setSearchMatch(serviceStatusString);
            } // switch (serviceName).

        } // for currService in services.

    } // readPayloadBeacon().


    /**
     * Given a message determined to be a search message, read the search message payload into usable memory.
     *
     * @param currMessage The current message object to update and return with updated payload data.
     * @param payload Array of strings representing the payload data to be read.
     */
    public static void parsePayloadSearch(Message currMessage, String[] payload) {

        String searchType = currMessage.getPayloadType(); // i.e. "search-request", "search-result", "search-error".

        if (searchType.equalsIgnoreCase("search-request")) {

            currMessage.setSearchType(payload[0]);
            currMessage.setSearchString(payload[1]);

        } else if (searchType.equalsIgnoreCase("search-result")) {

            currMessage.setResponseIdentifier(payload[0]);
            currMessage.setResponseSerialNumber(Long.parseLong(payload[1]));
            currMessage.setSearchFileString(payload[2]);

        } else if (searchType.equalsIgnoreCase("search-error")) {

            currMessage.setResponseIdentifier(payload[0]);
            currMessage.setResponseSerialNumber(Long.parseLong(payload[1]));

        }

    } // parsePayloadSearch().


    /**
     * Given a message determined to be a download message, read the download message payload into usable memory.
     *
     * @param currMessage The current message object to update and return with updated payload data.
     * @param payload Array of strings representing the payload data to be read.
     */
    public static void parsePayloadDownload(Message currMessage, String[] payload) {

        String downloadType = currMessage.getPayloadType(); // i.e. "download-request", "download-result", "download-error".

        if (downloadType.equalsIgnoreCase("download-request")) {

            currMessage.setTargetPeerIdentifier(payload[0]);
            currMessage.setTargetFilePath(payload[1]);

        } else if (downloadType.equalsIgnoreCase("download-result")) {

            currMessage.setResponseIdentifier(payload[0]);
            currMessage.setResponseSerialNumber(Long.parseLong(payload[1]));
            currMessage.setFileTransferPort(Integer.parseInt(payload[2]));

        } else if (downloadType.equalsIgnoreCase("download-error")) {

            currMessage.setResponseIdentifier(payload[0]);
            currMessage.setResponseSerialNumber(Long.parseLong(payload[1]));

        }

    } // parsePayloadDownload().


    /**
     * Given a message determined to be an upload message, read the upload message payload into usable memory.
     *
     * @param currMessage The current message object to update and return with updated payload data.
     * @param payload Array of strings representing the payload data to be read.
     */
    public static void parsePayloadUpload(Message currMessage, String[] payload) {

        String downloadType = currMessage.getPayloadType(); // i.e. "upload-request", "upload-result", "upload-error".

        if (downloadType.equalsIgnoreCase("upload-request")) {

            currMessage.setTargetPeerIdentifier(payload[0]);
            currMessage.setTargetFilePath(payload[1]);

        } else if (downloadType.equalsIgnoreCase("upload-result")) {

            currMessage.setResponseIdentifier(payload[0]);
            currMessage.setResponseSerialNumber(Long.parseLong(payload[1]));
            currMessage.setFileTransferPort(Integer.parseInt(payload[2]));

        } else if (downloadType.equalsIgnoreCase("upload-error")) {

            currMessage.setResponseIdentifier(payload[0]);
            currMessage.setResponseSerialNumber(Long.parseLong(payload[1]));

        }

    } // parsePayloadUpload().


    /**
     * Given a message determined to be a delete message, read the delete message payload into usable memory.
     *
     * @param currMessage The current message object to update and return with updated payload data.
     * @param payload Array of strings representing the payload data to be read.
     */
    public static void parsePayloadDelete(Message currMessage, String[] payload) {

        String downloadType = currMessage.getPayloadType(); // i.e. "delete-request", "delete-result", "delete-error".

        if (downloadType.equalsIgnoreCase("delete-request")) {

            currMessage.setTargetPeerIdentifier(payload[0]);
            currMessage.setTargetFilePath(payload[1]);

        } else if (downloadType.equalsIgnoreCase("delete-result")) {

            currMessage.setResponseIdentifier(payload[0]);
            currMessage.setResponseSerialNumber(Long.parseLong(payload[1]));

        } else if (downloadType.equalsIgnoreCase("delete-error")) {

            currMessage.setResponseIdentifier(payload[0]);
            currMessage.setResponseSerialNumber(Long.parseLong(payload[1]));

        }

    } // parsePayloadDelete().


    // Auxiliary Methods:

    /**
     * Set the header for message objects.
     *
     * @param message Provided message object but with header set.
     */
    private void setHeader(Message message) {

        try {
            String identifier = System.getProperty("user.name") + "@" + InetAddress.getLocalHost().getCanonicalHostName();
            message.setIdentifier(identifier);
        } catch (UnknownHostException e) {
            System.err.println("Message.setHeader() Error - Could not get hostname: " + e.getMessage());
        }

        long serialNumber = System.currentTimeMillis() - initialSerialNumber;
        message.setSerialNumber(serialNumber);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS");
        String date = sdf.format(new Date());
        try {
            message.setTimestamp(date);
        } catch (ParseException e) {
            System.err.println("Message.setHeader() Error - Could not parse date: " + e.getMessage());
        }

    } // setHeader().


    /**
     * @return Calling toString on a message object will return the protocol representation of that message as a string.
     */
    @Override
    public String toString() {

        // Method could have been made more concise, but purposefully left explicit for ease of protocol format verification.

        String messageString = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS");

        if (this.payloadType.equalsIgnoreCase("beacon")) {

            messageString = ":" + this.getIdentifier()
                            + ":" + this.getSerialNumber()
                            + ":" + sdf.format(this.getTimestamp())
                            + ":" + this.getPayloadType()
                            + ":" + this.getServerPort()
                            + ":" + "remoteBrowse=" + this.isRemoteBrowseAvailable()
                            + "," + "search=" + this.isSearchAvailable()
                            + "," + "searchMatch=" + this.getSearchMatch()
                            + "," + "download=" + this.isDownloadAvailable()
                            + "," + "upload=" + this.isUploadAvailable()
                            + "," + "delete=" + this.isDeleteAvailable()
                            + ":";


        } else if (this.payloadType.equalsIgnoreCase("search-request")) {

            messageString = ":" + this.getIdentifier()
                            + ":" + this.getSerialNumber()
                            + ":" + sdf.format(this.getTimestamp())
                            + ":" + this.getPayloadType()
                            + ":" + this.getSearchType()
                            + ":" + this.getSearchString()
                            + ":";

        } else if (this.payloadType.equalsIgnoreCase("search-result")) {

            messageString = ":" + this.getIdentifier()
                            + ":" + this.getSerialNumber()
                            + ":" + sdf.format(this.getTimestamp())
                            + ":" + this.getPayloadType()
                            + ":" + this.getResponseIdentifier()
                            + ":" + this.getResponseSerialNumber()
                            + ":" + this.getSearchFileString()
                            + ":";

        } else if (this.payloadType.equalsIgnoreCase("search-error")) {

            messageString = ":" + this.getIdentifier()
                            + ":" + this.getSerialNumber()
                            + ":" + sdf.format(this.getTimestamp())
                            + ":" + this.getPayloadType()
                            + ":" + this.getResponseIdentifier()
                            + ":" + this.getResponseSerialNumber()
                            + ":";

        } else if (this.payloadType.equalsIgnoreCase("upload-request")) {

            messageString = ":" + this.getIdentifier()
                            + ":" + this.getSerialNumber()
                            + ":" + sdf.format(this.getTimestamp())
                            + ":" + this.getPayloadType()
                            + ":" + this.getTargetPeerIdentifier()
                            + ":" + this.getTargetFilePath()
                            + ":";

        } else if (this.payloadType.equalsIgnoreCase("upload-result")) {

            messageString = ":" + this.getIdentifier()
                            + ":" + this.getSerialNumber()
                            + ":" + sdf.format(this.getTimestamp())
                            + ":" + this.getPayloadType()
                            + ":" + this.getResponseIdentifier()
                            + ":" + this.getResponseSerialNumber()
                            + ":" + this.getFileTransferPort()
                            + ":";

        } else if (this.payloadType.equalsIgnoreCase("upload-error")) {

            messageString = ":" + this.getIdentifier()
                            + ":" + this.getSerialNumber()
                            + ":" + sdf.format(this.getTimestamp())
                            + ":" + this.getPayloadType()
                            + ":" + this.getResponseIdentifier()
                            + ":" + this.getResponseSerialNumber()
                            + ":";

        } else if (this.payloadType.equalsIgnoreCase("delete-request")) {

            messageString = ":" + this.getIdentifier()
                    + ":" + this.getSerialNumber()
                    + ":" + sdf.format(this.getTimestamp())
                    + ":" + this.getPayloadType()
                    + ":" + this.getTargetPeerIdentifier()
                    + ":" + this.getTargetFilePath()
                    + ":";

        } else if (this.payloadType.equalsIgnoreCase("delete-result")) {

            messageString = ":" + this.getIdentifier()
                    + ":" + this.getSerialNumber()
                    + ":" + sdf.format(this.getTimestamp())
                    + ":" + this.getPayloadType()
                    + ":" + this.getResponseIdentifier()
                    + ":" + this.getResponseSerialNumber()
                    + ":";

        } else if (this.payloadType.equalsIgnoreCase("delete-error")) {

            messageString = ":" + this.getIdentifier()
                    + ":" + this.getSerialNumber()
                    + ":" + sdf.format(this.getTimestamp())
                    + ":" + this.getPayloadType()
                    + ":" + this.getResponseIdentifier()
                    + ":" + this.getResponseSerialNumber()
                    + ":";

        } else if (this.payloadType.equalsIgnoreCase("download-request")) {

            messageString = ":" + this.getIdentifier()
                    + ":" + this.getSerialNumber()
                    + ":" + sdf.format(this.getTimestamp())
                    + ":" + this.getPayloadType()
                    + ":" + this.getTargetPeerIdentifier()
                    + ":" + this.getTargetFilePath()
                    + ":";

        } else if (this.payloadType.equalsIgnoreCase("download-result")) {

            messageString = ":" + this.getIdentifier()
                    + ":" + this.getSerialNumber()
                    + ":" + sdf.format(this.getTimestamp())
                    + ":" + this.getPayloadType()
                    + ":" + this.getResponseIdentifier()
                    + ":" + this.getResponseSerialNumber()
                    + ":" + this.getFileTransferPort()
                    + ":";

        } else if (this.payloadType.equalsIgnoreCase("download-error")) {

            messageString = ":" + this.getIdentifier()
                    + ":" + this.getSerialNumber()
                    + ":" + sdf.format(this.getTimestamp())
                    + ":" + this.getPayloadType()
                    + ":" + this.getResponseIdentifier()
                    + ":" + this.getResponseSerialNumber()
                    + ":";

        }

        return messageString;

    }


    // Getters and Setters:


    // Message Header:

    /**
     * @return Message identifier: username@hostname.
     */
    public String getIdentifier() {
        return username + "@" + hostname;
    }

    /**
     * @param identifier String representing an identifier (username@hostname) to be stored as username and hostname.
     */
    public void setIdentifier(String identifier) {

        String[] identifierParts = identifier.split("@");

        this.setUsername(identifierParts[0]);
        this.setHostname(identifierParts[1]);

    }

    /**
     * @return Username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username String representing a username to store.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return Hostname.
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * @param hostname String representing a hostname to store.
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * @return Serial Number.
     */
    public long getSerialNumber() {
        return serialNumber;
    }

    /**
     * @param serialNumber String used to set the serial number.
     */
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = Long.parseLong(serialNumber);
    }

    /**
     * @param serialNumber Long used to set the serial number.
     */
    public void setSerialNumber(long serialNumber) {
        this.serialNumber = serialNumber;
    }

    /**
     * @return Timestamp in format: 'yyyyMMdd-HHmmss.SSS'.
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp String used to set the timestamp.
     *
     * @throws ParseException Could not parse timestamp string to SimpleDateFormat.
     */
    public void setTimestamp(String timestamp) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS");
        this.timestamp = sdf.parse(timestamp);
    }


    // Message Payload:

    /**
     * @return Payload type.
     */
    public String getPayloadType() {
        return payloadType;
    }

    /**
     * @param payloadType String used to set the payload type.
     */
    public void setPayloadType(String payloadType) {
        this.payloadType = payloadType;
    }


    // Beacon Payload:

    /**
     * @return Server Port.
     */
    public int getServerPort() {
        return serverPort;
    }

    /**
     * @param serverPort String used to set the server port number.
     */
    public void setServerPort(String serverPort) {
        this.serverPort = Integer.parseInt(serverPort);
    }

    /**
     * @param serverPort Int used to set the server port number.
     */
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * @return Comma-separated string of services available for this message.
     */
    public String getServices() {

        String services = "";

        services += "remoteBrowse=" + isRemoteBrowseAvailable() + ", ";
        services += "search=" + isSearchAvailable() + ", ";
        services += "searchMatch=" + getSearchMatch() + ", ";
        services += "download=" + isDownloadAvailable() + ", ";
        services += "upload=" + isUploadAvailable() + ", ";
        services += "delete=" + isDeleteAvailable();

        return services;

    }

    /**
     * @return remoteBrowseAvailable: true if remote file browsing at the remote file browser is available.
     */
    public boolean isRemoteBrowseAvailable() {
        return remoteBrowseAvailable;
    }

    /**
     * @param remoteBrowseAvailable Set remoteBrowseAvailable.
     */
    public void setRemoteBrowseAvailable(boolean remoteBrowseAvailable) {
        this.remoteBrowseAvailable = remoteBrowseAvailable;
    }

    /**
     * @return searchAvailable: true if remote file searching at the remote file browser is available.
     */
    public boolean isSearchAvailable() {
        return searchAvailable;
    }

    /**
     * @param searchAvailable Set searchAvailable.
     */
    public void setSearchAvailable(boolean searchAvailable) {
        this.searchAvailable = searchAvailable;
    }

    /**
     * @return searchMatch: "none", "path", "path-filename", "path-filename-substring".
     */
    public String getSearchMatch() {
        return searchMatch;
    }

    /**
     * @param searchMatch Set searchMatch: "none", "path", "path-filename", "path-filename-substring".
     */
    public void setSearchMatch(String searchMatch) {
        this.searchMatch = searchMatch;
    }

    /**
     * @return downloadAvailable: true if remote file download at the remote file browser is available.
     */
    public boolean isDownloadAvailable() {
        return downloadAvailable;
    }

    /**
     * @param downloadAvailable Set downloadAvailable.
     */
    public void setDownloadAvailable(boolean downloadAvailable) {
        this.downloadAvailable = downloadAvailable;
    }

    /**
     * @return uploadAvailable: true if remote file upload at the remote file browser is available.
     */
    public boolean isUploadAvailable() {
        return uploadAvailable;
    }

    /**
     * @param uploadAvailable Set uploadAvailable.
     */
    public void setUploadAvailable(boolean uploadAvailable) {
        this.uploadAvailable = uploadAvailable;
    }

    /**
     * @return deleteAvailable: true if remote file deletion at the remote file browser is available.
     */
    public boolean isDeleteAvailable() {
        return deleteAvailable;
    }

    /**
     * @param deleteAvailable Set deleteAvailable.
     */
    public void setDeleteAvailable(boolean deleteAvailable) {
        this.deleteAvailable = deleteAvailable;
    }


    // Search Payload:

    /**
     * @return Response ID which has format: <identifier> <serial_number>, for search-result and search-error messages.
     */
    public String getResponseIDSerialNum() {
        return responseIdentifier + ":" + responseSerialNumber;
    }

    /**
     * @param responseID Set the response ID: <identifier> <serial_number>, for search-result and search-error messages.
     */
    public void setResponseIDSerialNum(String responseID) {
        String[] responseIDParts = responseID.split(":");
        this.responseIdentifier = responseIDParts[0];
        this.responseSerialNumber = Long.parseLong(responseIDParts[1]);
    }

    /**
     * @return Search type for search request messages, i.e. 'path', 'filename', 'substring'.
     */
    public String getSearchType() {
        return searchType;
    }

    /**
     * @param searchType Set search type for the search request, i.e. 'path', 'filename', 'substring'.
     */
    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    /**
     * @return Search String for search request: the string that queries the search.
     */
    public String getSearchString() {
        return searchString;
    }

    /**
     * @param searchString Set search string for search request: the string that queries the search.
     */
    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    /**
     * @return Response Identifier: same as an identifier, but given in response to match up correct search messages.
     */
    public String getResponseIdentifier() {
        return responseIdentifier;
    }

    /**
     * @param responseIdentifier Set the response identifier, used for matching up search messages.
     */
    public void setResponseIdentifier(String responseIdentifier) {
        this.responseIdentifier = responseIdentifier;
    }

    /**
     * @return Response serial number, used for matching up search messages.
     */
    public long getResponseSerialNumber() {
        return responseSerialNumber;
    }

    /**
     * @param responseSerialNumber Set the response serial number, used for matching up search messages.
     */
    public void setResponseSerialNumber(long responseSerialNumber) {
        this.responseSerialNumber = responseSerialNumber;
    }

    /**
     * @return File String, representing a file path to a file meeting the criteria of a search request.
     */
    public String getSearchFileString() {
        return searchFileString;
    }

    /**
     * @param searchFileString Set file string, representing a file path to a file meeting the criteria of a search request.
     */
    public void setSearchFileString(String searchFileString) {
        this.searchFileString = searchFileString;
    }

    /**
     * @return Identifier (username@hostname) for the peer targeted to complete the download/upload/delete.
     */
    public String getTargetPeerIdentifier() {
        return targetPeerIdentifier;
    }

    /**
     * @param targetPeerIdentifier Set identifier (username@hostname) for the peer targeted to complete the download/upload/delete.
     */
    public void setTargetPeerIdentifier(String targetPeerIdentifier) {
        this.targetPeerIdentifier = targetPeerIdentifier;
    }

    /**
     * @return File path for the file involved in the download/upload/delete from the root ('/') directory.
     */
    public String getTargetFilePath() {
        return targetFilePath;
    }

    /**
     * @param targetFilePath Set file path for the file involved in the download/upload/delete from the root ('/') directory.
     */
    public void setTargetFilePath(String targetFilePath) {
        this.targetFilePath = targetFilePath;
    }

    /**
     * @return Port to complete the file transfer when downloading and uploading.
     */
    public int getFileTransferPort() {
        return fileTransferPort;
    }

    /**
     * @param fileTransferPort Set port to complete the file transfer when downloading and uploading.
     */
    public void setFileTransferPort(int fileTransferPort) {
        this.fileTransferPort = fileTransferPort;
    }


} // Message{}.
