import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class SearchReceiver implements Runnable {


    MulticastHandler multicastHandler; // Instance of MulticastHandler used.
    Configuration configuration; // Configuration of MulticastHandler.
    HashMap<String, Message> outgoingSearchRequests; // HashMap of active search requests that have been sent.
    HashMap<String, Message> incomingSearchMessages; // HashMap of incoming messages pending processing.


    /**
     * Constructor: Creates an instance of SearchReceiver as a separate thread.
     *
     * @param multicastHandler Instance of MulticastHandler, used for creating and reading messages.
     */
    SearchReceiver(MulticastHandler multicastHandler) {

        this.multicastHandler = multicastHandler;
        this.configuration = multicastHandler.configuration;
        this.outgoingSearchRequests = new HashMap<>(); // HashMap of active search requests that have been sent.
        this.incomingSearchMessages = new HashMap<>(); // HashMap of incoming messages pending processing.

        // Create thread for managing handling search messages from multicast group.
        Thread thread = new Thread(this);
        thread.setPriority(7);
        thread.start();

    } // SearchReceiver().


    /**
     * Process incoming search messages.
     */
    @Override
    public void run() {

        do { // Do until application terminated.

            processIncomingSearchMessages();

        } while (true);

    } // run().


    /**
     * Process incoming search messages.
     */
    public synchronized void processIncomingSearchMessages() {

        LinkedList<String> messagesToRemove = new LinkedList<>();

        // Loop over incoming messages and process them.
        Iterator<Map.Entry<String, Message>> messageIterator = incomingSearchMessages.entrySet().iterator();
        while (messageIterator.hasNext()) {

            // Get current message to process.
            Map.Entry<String, Message> entry = messageIterator.next();
            String messageIdentifier = entry.getKey();
            Message message = entry.getValue();

            // If search request, then process the search request.
            if (message.getPayloadType().equalsIgnoreCase("search-request")) {

                // Determine the type of search to do.
                String searchType = message.getSearchType();

                // Perform the search, returning a list of strings that are all results.
                LinkedList<String> searchResults = new LinkedList<>();
                if (searchType.equalsIgnoreCase("path")) {

                    searchResults = pathSearch(message.getSearchString());

                } else if (searchType.equalsIgnoreCase("filename")) {

                    searchResults = filenameSearch(message.getSearchString());

                } else if (searchType.equalsIgnoreCase("substring")) {

                    searchResults = substringSearch(message.getSearchString());

                }


                if (searchResults.isEmpty()) { // There are no search results.

                    // If no results, then send a search-error message.
                    Message searchError = Message.searchErrorMessage(message.getIdentifier(), message.getSerialNumber());
                    multicastHandler.txMessage(searchError);

                } else {

                    // For each result, create a search-result message and send over multicast.
                    for (String currResult : searchResults) {

                        // Create and send a searchResult message.
                        Message searchResult = Message.searchResultMessage(message.getIdentifier(), message.getSerialNumber(), currResult);
                        multicastHandler.txMessage(searchResult);

                    } // for (all search results).

                } // if (there are search results), else.

            } else { // Otherwise, a search-result or search-error.

                String incomingMessageIdentifier = message.getResponseIDSerialNum();

                // Check response-id and serial number matches a message in the outgoing structure.
                if (outgoingSearchRequests.containsKey(incomingMessageIdentifier)) { // Match for one of our requests.

                    // Extract result and display to the user.
                    Message initialRequest = outgoingSearchRequests.get(incomingMessageIdentifier);
                    showSearchResponse(initialRequest, message);

                } // If not a result for one of our requests, then ignore.

            } // Anything else does not conform to the protocol so ignore.

            messagesToRemove.add(messageIdentifier);

        } // for (all incoming messages).

        // Remove incoming messages once processed.
        removeSearchMessages(incomingSearchMessages, messagesToRemove);

    } // processIncomingSearchMessages().


    /**
     * Given a message know to be a search message, add the message appropriately to the structures.
     *
     * @param outgoing Whether to add the message to the outgoing or incoming messages structure.
     * @param searchMessage Message to add to the search message structures.
     */
    public synchronized void addSearchMessage(boolean outgoing, Message searchMessage) {

        // Synchronised to prevent simultaneous adding and removing issues due to threading.

        // Create key for message structures.
        String messageIdentifier = searchMessage.getIdentifier() + ":" + searchMessage.getSerialNumber();

        // Determine which structure to add message to.
        if (!outgoing) {

            // Incoming search messages are messages from other peers to respond to.
            if (incomingSearchMessages.get(messageIdentifier) == null) { // Add if new.
                incomingSearchMessages.put(messageIdentifier, searchMessage);
            } else { // Updates the message if it exists already.
                incomingSearchMessages.replace(messageIdentifier, searchMessage);
            }

        } else {

            // Outgoing search messages are messages that have been sent out and are awaiting answers.
            if (outgoingSearchRequests.get(messageIdentifier) == null) { // Add if new.
                outgoingSearchRequests.put(messageIdentifier, searchMessage);
            } else { // Updates the message if it exists already.
                outgoingSearchRequests.replace(messageIdentifier, searchMessage);
            }

        } // if (outgoing or not).

    } // addSearchMessage().


    /**
     * Remove processed messages from list of search messages to process.
     *
     * @param incomingMessages Structure of incoming messages to be processed.
     * @param messagesToRemove List of messages that have been processed and should be removed from incomingMessages.
     */
    public synchronized void removeSearchMessages(HashMap<String, Message> incomingMessages, LinkedList<String> messagesToRemove) {

        incomingMessages.entrySet().removeIf(entry -> messagesToRemove.contains(entry.getKey()));

    } // removeSearchMessages().


    /**
     * Method for outputting results to stdout for search responses that match one of our search requests.
     *
     * @param initialRequest The initial request, showing the search type and query string.
     * @param searchResponse The request response, showing the resulting file string (if any) and where.
     */
    public void showSearchResponse(Message initialRequest, Message searchResponse) {

        System.out.println("----------------------------------------------");

        System.out.println("Search Request: Search Type: '" + initialRequest.getSearchType()
                                        + "', Search String: '" + initialRequest.getSearchString() + "'.");

        if (searchResponse.getPayloadType().equalsIgnoreCase("search-result")) {

            System.out.println("Search Result: '" + searchResponse.getSearchFileString()
                    + "' At " + searchResponse.getIdentifier());

        } else { // search-error.

            System.out.println("Search Result: No Result At " + searchResponse.getIdentifier());

        }

        System.out.println("----------------------------------------------");

    } // showSearchResponse().


    /**
     * Performs a search of the path relative to the root directory and returns the result, if found.
     *
     * @param searchString Query string used in path search.
     *
     * @return LinkedList containing a single file string path to the result, otherwise, empty linked list.
     */
    public LinkedList<String> pathSearch(String searchString) {

        LinkedList<String> results = new LinkedList<>();

        // Search string should be relative to the root directory indicated by "/" at start. If not present, add it.
        String missingDirSeparator = "";
        if ('/' != searchString.charAt(0)) {
            missingDirSeparator = "/";
        }

        // Follow path to file relative to root_dir. If valid then add file to linked list, formatted with path from root.
        searchString = configuration.rootDir_ + missingDirSeparator + searchString;
        File fileSearchString = new File(searchString);

        if (fileSearchString.exists()) { // If exists, then return result appropriately, else return empty list.

            if (fileSearchString.isFile() || fileSearchString.isDirectory()) {

                // Get path to file.
                String pathFileSearchString = fileSearchString.getAbsolutePath();
                // Make path relative to the root directory.
                String rootDirName = FileTreeBrowser.getRootDirName(); // Get name of root directory.
                pathFileSearchString = pathFileSearchString.split(rootDirName)[1]; // Get everything after root dir name.
                // Add "/" to indicate result is a directory, when this is the case.
                if (fileSearchString.isDirectory()) pathFileSearchString = pathFileSearchString + "/"; // Append to show a dir.
                // Add to linked list of results.
                results.add(pathFileSearchString);

            } // if (ifFile or isDirectory).

        } // if (file exists).

        return results; // If results found, return linked list of file string to result file, else return empty list.

    } // pathSearch().


    /**
     * Performs a search of the filename on all files in the root directory hierarchy, adding results as found.
     *
     * @param searchString Query string used in filename search.
     *
     * @return LinkedList containing all file string paths for the result, otherwise, empty linked list.
     */
    public LinkedList<String> filenameSearch(String searchString) {

        LinkedList<String> results = new LinkedList<>();

        walk(configuration.rootDir_, results, searchString, false);

        return results; // If results found, return linked list of file string to result file, else return empty list.

    } // pathSearch().


    /**
     * Performs a search of the substring on all files in the root directory hierarchy, adding results as found.
     *
     * @param searchString Query string used in filename search.
     *
     * @return LinkedList containing all file string paths for the result, otherwise, empty linked list.
     */
    public LinkedList<String> substringSearch(String searchString) {

        LinkedList<String> results = new LinkedList<>();

        walk(configuration.rootDir_, results, searchString, true);

        return results; // If results found, return linked list of file string to result file, else return empty list.

    } // pathSearch().


    /**
     * Method used to recursively explore the full root directory hierarchy to perform search.
     *
     * @param path Initially the root directory path, otherwise, the current directory path in the recursive search.
     * @param results LinkedList of file paths that match the search.
     * @param searchString Query string when performing search.
     * @param isSubstring Whether a substring match or exact filename match is required.
     */
    public void walk(String path, LinkedList<String> results, String searchString, boolean isSubstring) {

        File root = new File(path);
        File[] fileList = root.listFiles();

        if (fileList == null) return;

        for (File currFile : fileList) {

            if (currFile.isDirectory()) {
                walk(currFile.getAbsolutePath(), results, searchString, isSubstring);
            }

            if (isSubstring) {
                if (currFile.getName().contains(searchString)) {
                    String rootDirName = FileTreeBrowser.getRootDirName();
                    String rootRelativePath = currFile.getAbsolutePath().split(rootDirName)[1];
                    results.add(rootRelativePath);
                }
            } else {
                if (currFile.getName().equals(searchString)) {
                    String rootDirName = FileTreeBrowser.getRootDirName();
                    String rootRelativePath = currFile.getAbsolutePath().split(rootDirName)[1];
                    results.add(rootRelativePath);
                }
            }


        } // for (all files in current directory).

    } // walk().


} // SearchReceiver{}.
