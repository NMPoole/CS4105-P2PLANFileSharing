import java.io.*;
import java.util.*; // Date.
import java.text.*; // SimpleDateFormat.

/**
 * Browses a file tree with a simple text based output and navigation.
 *
 * @author <a href="https://saleem.host.cs.st-andrews.ac.uk/">Saleem Bhatti</a>
 * @version 1.2, 20 October 2020
 *
 * @author 170004680, Nov 2020.
 */
public final class FileTreeBrowser {


    // User commands.
    public static String quit_ = ":quit"; // Quit the application.
    public static String help_ = ":help"; // Display help message.
    public static String services_ = ":services"; // Show available services; remote browsing, search, upload, deletion.
    public static String up_ = ".."; // Go to parent directory (highest parent is the root directory).
    public static String list_ = "."; // List current directory contents.
    public static String showBeacons_ = ":showBeacons"; // Show available file browser beacons on the network.
    public static String remoteBrowse_ = ":remoteBrowse"; // Browse a remote file-space option.
    public static String localBrowse_ = ":localBrowse"; // Revert to local file-space browsing option.
    public static String search_ = ":search"; // Option for allowing user to search for files.
    public static String download_ = ":download"; // Option for allowing user to download remote files.
    public static String upload_ = ":upload"; // Option for allowing user to upload files to remote file-spaces.
    public static String delete_ = ":delete"; // Option for allowing user to delete files at remote file-spaces.

    static Configuration configuration_; // Current configuration of the FileTreeBrowser.

    static String rootPath_ = ""; // Path to the root directory.
    File thisDir_; // This (current) directory.
    String thisDirName_; // Name of this (current) directory.

    static TCPClient tcpClient; // TCPClient for this FileTreeBrowser instance, handles remote browsing.
    static FileTreeBrowser ftb; // Current instance of FileTreeBrowser, which is manipulated by main().
    static boolean remoteBrowsing; // Whether remote browsing is currently taking place or not.


    /**
     * Constructor: Create a new FileTreeBrowser.
     *
     * @param pathName The pathname (directory) at which to start.
     */
    public FileTreeBrowser(String pathName) {

        // If no pathname provided (empty), then get root directory from the configuration.
        if (pathName.equals("")) {
            pathName = configuration_.rootDir_;
        } else { // "." -- this directory, re-list only.

            if (pathName.equals(list_)) {
                pathName = thisDirName_;
            }

        }

        thisDir_ = new File(pathName);
        thisDirName_ = getPathName(thisDir_);

    } // FileTreeBrowser().


    /**
     * Main method for the FileTreeBrowser.
     *
     * @param args No arguments expected.
     */
    public static void main(String[] args) {

        // Load configuration and get path to root directory.
        configuration_ = new Configuration("filetreebrowser.properties");
        rootPath_ = getPathName(new File(configuration_.rootDir_));

        // Create MultiCast Handler, which sends and received beacons.
        MulticastHandler multicastHandler = new MulticastHandler(configuration_);

        // Create TCP server if remote browsing is enabled. TCPClient created when remote browsing enacted.
        TCPBrowseServer tcpBrowseServer = null;
        if (configuration_.remoteBrowse_) {
            tcpBrowseServer = new TCPBrowseServer(configuration_);
        }

        // Create initial file tree browser, using the root directory as default.
        ftb = new FileTreeBrowser(configuration_.rootDir_);
        String fileList = ftb.listFiles();
        System.out.println(fileList);

        // Variables required for handling user command input.
        InputStream keyboard = System.in;
        String userCmd;

        boolean quit = false; // When user specifies quit command, update to true, indicating to quit program.
        remoteBrowsing = false; // Whether the user is currently remote browsing or not.


        // Keep processing user commands until user specifies to quit.
        while (!quit) {

            // Print user options.
            String userOptions = userOptions(remoteBrowsing);
            System.out.println(userOptions);

            // Get selected option/command from the user.
            userCmd = getUserInput(keyboard);

            if (userCmd.equalsIgnoreCase(quit_)) { // Quit option selected: exit program.

                if (remoteBrowsing) { // When exiting program, close the TCP client if it exists.
                    tcpClient.sendCommand(quit_); // Indicate disconnect to server.
                    tcpClient.closeClient();
                    tcpClient = null;
                }

                quit = true;


            } else if (userCmd.equalsIgnoreCase(help_)) { // Help option selected: display help message.

                String helpInformation;
                if (remoteBrowsing) {
                    // Send help request via TCPClient to get server help information when remote browsing.
                    helpInformation = tcpClient.sendCommand(help_);
                    handleRemoteOutput(helpInformation);
                } else {
                    helpInformation = helpInformation();
                    System.out.println(helpInformation);
                }


            } else if (userCmd.equalsIgnoreCase(services_)) { // Service option selected: display services available.

                String services;
                if (remoteBrowsing) {
                    // Send services request via TCPClient.
                    services = tcpClient.sendCommand(services_);
                    handleRemoteOutput(services);
                } else {
                    services = displayServices();
                    System.out.println(services);
                }


            } else if (userCmd.equalsIgnoreCase(list_)) { // List ('.') option selected: print current directory contents.

                if (remoteBrowsing) {
                    // Send '.' request via TCPClient.
                    fileList = tcpClient.sendCommand(list_);
                    handleRemoteOutput(fileList);
                } else {
                    fileList = ftb.listFiles();
                    System.out.println(fileList);
                }


            } else if (userCmd.equalsIgnoreCase(up_)) { // Move up ('..') option selected: Move to parent directory directory.

                String moveUp;
                if (remoteBrowsing) {
                    // Send '..' request via TCPClient.
                    moveUp = tcpClient.sendCommand(up_);
                    handleRemoteOutput(moveUp);
                } else {
                    // Moving to parent creates a new FileTreeBrowser instance, and generates a message to indicate this.
                    AbstractMap.SimpleEntry<FileTreeBrowser, String> entry = ftb.getParent(ftb);
                    ftb = entry.getKey();
                    moveUp = entry.getValue();
                    System.out.println(moveUp);
                }


            } else if (userCmd.equalsIgnoreCase(showBeacons_)) { // ':showBeacons' option for showing remote file browser beacons.

                multicastHandler.beaconReceiver.showBeacons();


            } else if (userCmd.equalsIgnoreCase(remoteBrowse_) && configuration_.remoteBrowse_) { // ':remoteBrowse' option for remote browsing.

                // If no beacons then show unable to remote browse.
                if (multicastHandler.beaconReceiver.beacons.size() == 0) {

                    System.out.println("There are no beacons to connect to right now. Try again later.");

                } else {

                    // If already remote browsing, close the current TCP client before opening a new one.
                    if (tcpClient != null) {
                        tcpClient.closeClient();
                    }

                    // Get user choice of beacon (remote file-browser) to connect to.
                    Message chosenBeacon = getUserBeaconChoice(keyboard, multicastHandler); // Change.

                    if (chosenBeacon != null) { // Check user was able to make a selection.

                        // Open new TCPClient and connect to the beacon server.
                        tcpClient = new TCPClient(configuration_, chosenBeacon.getHostname(), chosenBeacon.getServerPort());
                        String rootInfo = tcpClient.sendCommand(list_); // Get root directory information.
                        handleRemoteOutput(rootInfo);

                        // Set remoteBrowsing boolean to true to handle future commands correctly.
                        remoteBrowsing = true;

                    } // if (chosenBeacon !- null).

                } // if (multicastHandler.beaconReceiver.beacons.size() == 0).


            } else if (userCmd.equalsIgnoreCase(localBrowse_) && configuration_.remoteBrowse_ && remoteBrowsing) { // ':localBrowse' option to return to local browsing.

                // Close TCPClient (close socket correctly by disconnecting from TCPServer).
                if (tcpClient != null) {
                    tcpClient.sendCommand(quit_); // Indicate disconnect to server.
                    tcpClient.closeClient();
                    tcpClient = null;
                }

                //Show local root information again.
                ftb = new FileTreeBrowser(configuration_.rootDir_);
                fileList = ftb.listFiles();
                System.out.println(fileList);

                remoteBrowsing = false; // Set remoteBrowsing to false to handle future commands correctly.


            } else if (userCmd.equalsIgnoreCase(search_) && configuration_.search_) {

                // Search operates the same regardless of whether remote or local browsing.

                // Get type of search from the user.
                String userSearchChoice = getUserSearchChoice(keyboard);

                // Get search string from the user.
                String userSearchString;
                do {
                    System.out.println("Enter a string to search for: ");
                    userSearchString = getUserInput(keyboard);
                } while (userSearchString.length() == 0);

                // Create search request message.
                Message searchRequest = Message.searchRequestMessage(userSearchChoice, userSearchString);
                // Send search request message over multicast group.
                multicastHandler.txMessage(searchRequest);
                // Add sent search request to pending request structure in SearchReceiver.
                multicastHandler.searchReceiver.addSearchMessage(true, searchRequest);


            } else if (userCmd.equalsIgnoreCase(download_) && configuration_.download_) {

                // Download operates the same regardless of whether remote or local browsing.

                // Get user choice of remote file browser to download from.
                Message userBeaconChoice = getUserBeaconChoice(keyboard, multicastHandler);

                if (userBeaconChoice != null) { // User was able to select a peer to download from.

                    // Get exact file to download path from user.
                    String userDownloadFilePath;
                    do {
                        System.out.println("Enter the exact path from the root ('/') to the file to download at the peer: ");
                        userDownloadFilePath = getUserInput(keyboard);
                    } while (userDownloadFilePath.length() == 0);

                    // Create and send download-request message over the multicast group.
                    String peerIdentifier = userBeaconChoice.getIdentifier();
                    Message downloadRequest = Message.downloadRequestMessage(peerIdentifier, userDownloadFilePath);
                    multicastHandler.txMessage(downloadRequest);

                    // Add download-request message to outgoing download requests, so responses can be handled.
                    multicastHandler.downloadReceiver.addMessage(true, downloadRequest);

                } // if (user selected a peer to download from).


            } else if (userCmd.equalsIgnoreCase(upload_) && configuration_.upload_) {

                // Upload operates the same regardless of whether remote or local browsing.

                // Get user choice of remote file browser to upload to.
                Message userBeaconChoice = getUserBeaconChoice(keyboard, multicastHandler);

                if (userBeaconChoice != null) {

                    // Get exact file to upload path from user.
                    String userUploadFilePath;
                    do {
                        System.out.println("Enter the exact path from the root ('/') to the location to upload to at the peer: ");
                        userUploadFilePath = getUserInput(keyboard);
                    } while (userUploadFilePath.length() == 0);

                    // Create and send upload-request message over the multicast group.
                    String peerIdentifier = userBeaconChoice.getIdentifier();
                    Message uploadRequest = Message.uploadRequestMessage(peerIdentifier, userUploadFilePath);
                    multicastHandler.txMessage(uploadRequest);

                    // Add upload-request message to outgoing upload requests, so responses can be handled.
                    multicastHandler.uploadReceiver.addMessage(true, uploadRequest);

                } // if (user selected a peer to upload to).


            } else if (userCmd.equalsIgnoreCase(delete_) && configuration_.delete_) {

                // Delete operates the same regardless of whether remote or local browsing.

                // Get user choice of remote file browser to upload to.
                Message userBeaconChoice = getUserBeaconChoice(keyboard, multicastHandler);

                if (userBeaconChoice != null) {

                    // Get exact file path to delete at the remote peer from user.
                    String userDeleteFilePath;
                    do {
                        System.out.println("Enter the exact path from the root ('/') to the location of the file to delete at the peer: ");
                        userDeleteFilePath = getUserInput(keyboard);
                    } while (userDeleteFilePath.length() == 0);

                    // Create and send delete-request message over the multicast group.
                    String peerIdentifier = userBeaconChoice.getIdentifier();
                    Message deleteRequest = Message.deleteRequestMessage(peerIdentifier, userDeleteFilePath);
                    multicastHandler.txMessage(deleteRequest);

                    // Add delete-request message to outgoing upload requests, so responses can be handled.
                    multicastHandler.deleteReceiver.addMessage(true, deleteRequest);

                } // if (userBeaconChoice != null).


            } else { // Do something with pathname.

                String pathnameString;
                if (remoteBrowsing) {
                    // Send pathname request via TCPClient.
                    pathnameString = tcpClient.sendCommand(userCmd);
                    handleRemoteOutput(pathnameString);
                } else {
                    // Evaluating pathname creates a new FileTreeBrowser instance, and generates a message to indicate this.
                    AbstractMap.SimpleEntry<FileTreeBrowser, String> entry = ftb.evaluatePathName(ftb, userCmd);
                    ftb = entry.getKey();
                    pathnameString = entry.getValue();
                    System.out.println(pathnameString);
                }


            } // else, do something with pathname.

        } // while(!quit).

        // Tidy up by closing connections, leaving multicast group, and exiting with successful status.
        multicastHandler.leave();
        if (tcpBrowseServer != null) {
            tcpBrowseServer.closeServerSocket();
        }
        System.exit(0);

    } // main().


    /**
     * Get the name of the root directory.
     *
     * @return String representing the root directory name.
     */
    public static String getRootDirName() {

        String rootDirPath = configuration_.rootDir_; // For this program: '../root_dir'.
        String[] rootDirPathParts = rootDirPath.split(File.separator); // Root dir should not end with '/' when given.
        String rootDirName = rootDirPathParts[rootDirPathParts.length - 1]; // Root dir name is last in the root path.

        return rootDirName;

    } // getRootDirName().


    /**
     * Handle the output from the remote browser.
     * If a null string is received from the TCP client, then the server disconnected, so revert to local browsing.
     *
     * @param output Response from the TCP server.
     */
    public static void handleRemoteOutput(String output) {

        if (output != null) { // If remote output is non-null, then display locally.
            System.out.println(output);
        } else { // If remote output is null, then connection to server is lost, so revert to local browsing.

            System.out.println("Connection To Remote File Browser Lost - Reverting To Local Browsing:");

            // Close TCPClient (close socket correctly by disconnecting from TCPServer).
            if (tcpClient != null) {
                tcpClient.sendCommand(quit_); // Indicate disconnect to server.
                tcpClient.closeClient();
                tcpClient = null;
            }

            //Show local root information again.
            ftb = new FileTreeBrowser(configuration_.rootDir_);
            String fileList = ftb.listFiles();
            System.out.println(fileList);

            remoteBrowsing = false;

        } // if (remote output not null), else.

    } // handleRemoteOutput().


    /**
     * Creates a string which indicates the available options for the user.
     *
     * @param remoteBrowsing If remote browsing, include option to allow user to return to local browsing.
     *
     * @return String representing the available user options.
     */
    public static String userOptions(boolean remoteBrowsing) {

        String optionSeparator = "' | '";
        String userOptions = "";

        userOptions += "\n[filename | '";
        userOptions += list_;
        userOptions += optionSeparator;
        userOptions +=  up_;
        userOptions += optionSeparator;
        userOptions += quit_;
        userOptions += optionSeparator;
        userOptions +=  services_;
        userOptions += optionSeparator;
        userOptions += help_;
        userOptions += optionSeparator;
        userOptions += showBeacons_;

        if (configuration_.search_) { // Can only search if enabled by te configuration.
            userOptions += optionSeparator;
            userOptions += search_;
        }
        if (configuration_.download_) { // Can only remote download if enabled by te configuration.
            userOptions += optionSeparator;
            userOptions += download_;
        }
        if (configuration_.upload_) { // Can only remote upload if enabled by te configuration.
            userOptions += optionSeparator;
            userOptions += upload_;
        }
        if (configuration_.delete_) { // Can only remote delete if enabled by te configuration.
            userOptions += optionSeparator;
            userOptions += delete_;
        }
        if (configuration_.remoteBrowse_) { // Can only remoteBrowser if enabled by te configuration.
            userOptions += optionSeparator;
            userOptions += remoteBrowse_;
        }
        if (remoteBrowsing) { // Can only switch back to local browsing if remote browsing.
            userOptions += optionSeparator;
            userOptions += localBrowse_;
        }

        userOptions += "'] ";

        return userOptions;

    } // userOptions().


    /**
     * Get input from the user.
     *
     * @param keyboard InputStream to read input from.
     *
     * @return User input as a string.
     */
    public static String getUserInput(InputStream keyboard) {

        String userCmd;

        // Loop until user provides option.
        while ((userCmd = ByteReader.readLine(keyboard)) == null) {
            try {
                Thread.sleep(configuration_.sleepTime_);
            } catch (InterruptedException ignored) {
            } // Thread.sleep() - do nothing.
        }

        return userCmd;

    } // getUserInput().


    /**
     * Get user choice of beacon to connect to.
     *
     * @param keyboard Input stream to get user input from.
     * @param multicastHandler Contains list of current beacons to show to user to select from.
     *
     * @return Message which is a given beacon.
     */
    public static Message getUserBeaconChoice(InputStream keyboard, MulticastHandler multicastHandler) {

        Message beaconChoice = null;

        // Show beacons.
        multicastHandler.beaconReceiver.showBeacons();

        System.out.println("Please select an identifier and port number. Format: '<identifier> <port>'");

        // Ask for valid beacon choice.
        boolean validBeaconChoice = false;
        do {

            String userInput = ByteReader.readLine(keyboard); // Read user input.

            if (userInput != null) { // Check user has provided an input.

                String[] userChoice = userInput.split(" "); // Split user choice according to format.

                if (userChoice.length >= 2) { // User should have provided an identifier and a port.

                    String identifier = userChoice[0]; // Get identifier.
                    String portStr = userChoice[1]; // Get port.

                    String beaconIdentifier = identifier + ":" + portStr; // Create key needed for beacons.

                    if (multicastHandler.beaconReceiver.beacons.size() == 0) {

                        // All beacons removed whilst getting user input.
                        System.out.println("There are no available beacons right now. Try again later.");
                        return null;

                    } else {

                        Message beaconSelected = multicastHandler.beaconReceiver.beacons.get(beaconIdentifier);

                        // Check if user selected a valid beacon from the list of available beacons.
                        if (beaconSelected != null) {
                            beaconChoice = beaconSelected;
                            validBeaconChoice = true;
                        } else {
                            System.out.println("Not a valid beacon. Try again. Format: '<hostname> <port>'");
                        }

                    } // if (there are available beacons), else.

                } else {
                    System.out.println("Incorrect input format given. Format: '<hostname> <port>'");
                }

            } // if (userInput == null).

        } while (!validBeaconChoice); // Keep looping until valid beacon selected, or until there are no beacons.

        return beaconChoice; // Return valid beacon choice.

    } // getUserBeaconChoice().


    /**
     *  Get choice of user search type when the ':search' option is used.
     *
     * @param keyboard Input stream to get user input from.
     *
     * @return String representing the search type choice.
     */
    public static String getUserSearchChoice(InputStream keyboard) {

        String userSearchChoice = null;

        String availableSearchMatches = configuration_.searchMatch_;

        System.out.println("Please select a search type: ");
        if (availableSearchMatches.contains("path")) {
            System.out.print("'path'");
        }
        if (availableSearchMatches.contains("filename")) {
            System.out.print(", 'filename'");
        }
        if (availableSearchMatches.contains("substring")) {
            System.out.print(", 'substring'");
        }
        System.out.println();

        // Loop until user provides option.
        boolean validSearchChoice = false;
        do {

            String userInput = ByteReader.readLine(keyboard); // Read user input.

            if (userInput != null) {

                if (userInput.equalsIgnoreCase("path")) {
                    userSearchChoice =  "path";
                    validSearchChoice = true;
                } else if (userInput.equalsIgnoreCase("filename")) {
                    userSearchChoice =  "filename";
                    validSearchChoice = true;
                } else if (userInput.equalsIgnoreCase("substring")) {
                    userSearchChoice =  "substring";
                    validSearchChoice = true;
                } else {
                    System.out.println("Please choose a valid search type.");
                }

            } // if (userInput != null).

        } while (!validSearchChoice);

        return userSearchChoice;

    } // getUserSearchChoice().


    /**
     * Method for getting a valid file path from the user.
     * When downloading, the file path retrieved is the location to save the file to, which must be valid and include a filename.
     * When uploading, the file path is the location of the file to send, which must exist and be readable.
     *
     * @param in The input stream used to retrieve the user input.
     * @param download Whether we are downloading a file or uploading a file, which effects what a valid file path is.
     *
     * @return String representing a valid file path for the purpose of downloading/uploading.
     */
    public static String getFile(InputStream in, boolean download) {

        if (download) {
            System.out.println("Enter direct file path from root to the location where the downloaded file will be saved.");
        } else {
            System.out.println("Enter direct file path from root to the location where the file to upload exists.");
        }

        String userPathToFile = "";

        boolean validFile = false;
        do {

            String userInput = ByteReader.readLine(in); // Read user input.

            if (userInput != null) { // When user input given.

                userPathToFile = configuration_.rootDir_ + "/" + userInput;
                File userFile = new File(userPathToFile);

                if (download) { // When downloading, validate the location chosen to save the file to.

                    if (!userInput.contains("..")) { // Only permit direct path from the root.

                        if (userFile.exists() && !userFile.isDirectory()) { // Existing file.

                            if (userFile.canWrite()) {  // Make sure we can overwrite the file.
                                validFile = true;
                            }

                        } else { // File does not exist, so it should be creatable.

                            if (userPathToFile.charAt(userPathToFile.length() - 1) != '/') { // Need file not a directory.

                                // Check filename provided is valid. Format: 'filename.extension'
                                String[] filePathParts = userPathToFile.split("/");
                                String fileNamePart = filePathParts[filePathParts.length - 1];
                                String[] fileNameParts = fileNamePart.split("\\.");

                                if (fileNameParts.length == 2) {

                                    if (fileNameParts[0].length() != 0 && fileNameParts[1].length() != 0) {

                                        try {
                                            validFile = userFile.createNewFile();
                                        } catch (IOException e) {
                                            System.out.println("Could not create new file for download location. Please try again.");
                                        }

                                    } else {
                                        System.out.println("Filename at end of path is invalid.");
                                    }

                                } else {
                                    System.out.println("Filename at end of path should include an extension.");
                                }

                            } else { // Directory specified when a file is needed.
                                System.out.println("Please enter a direct path to file and not a directory.");
                            }

                        }

                    } else {
                        System.out.println("Please enter a direct path to location to save file to ('..' not permitted).");
                    }


                } else { // For uploading, file must exist, not be a directory, and be in the root.

                    if (userFile.exists() && !userFile.isDirectory() && !userInput.contains("..")) {
                        validFile = true;
                    } else {
                        System.out.println("Please enter a direct path to an existing file to upload within the root directory.");
                    }

                } // if (download), else.

            } // if (user input not null).

        } while (!validFile);

        return userPathToFile;

    } // getFile().


    /**
     * Print/get help message.
     *
     * @return String representing help information for user options.
     */
    static String helpInformation() {

        return "\n--* Welcome to the simple FileTreeBrowser. *--\n" +
        "* The display consists of:\n" +
        "\t- The name of the current directory\n" +
        "\t- The list of files (the numbers for the files are of no\n" +
        "\t  significance, but may help you with debugging).\n" +
        "* Files that are directories have trailing '" + File.separator + "'.\n" +
        "* Use text entry to navigate the directory tree.\n" +
        "\t.\t\t\t\tTo refresh the view of the current directory.\n" +
        "\t..\t\t\t\tTo move up a directory level.\n" +
        "\tfilename\t\tTo list file details (if it is a file) or to\n" +
        "\t\t\t\t\tmove into that directory (if it is a directory name).\n" +
        "\t:services\t\tTo list the services offered.\n" +
        "\t:quit\t\t\tTo quit the program.\n" +
        "\t:help\t\t\tTo print this message.\n" +
        "\t:showBeacons\tShow currently available remote file-spaces.\n" +
        "\t:remoteBrowse\tBrowse a remote file-space.\n" +
        "\t:localBrowse\tReturn to local file-space browsing.\n" +
        "\t:search\t\t\tSearch for a path, filename or substring on the network.\n" +
        "\t:download\t\tDownload a file from a remote file-browser.\n" +
        "\t:upload\t\t\tUpload a file to a remote file-browser.\n" +
        "\t:delete\t\t\tDelete a file at a remote file-browser.\n";

    } // helpInformation().


    /**
     * Print/get config information.
     *
     * @return String representing services available.
     */
    static String displayServices() {

        String services = ":";
        services += "id=" + configuration_.id_ + ":";
        services += "timestamp=" + timestamp() + ":";
        services += "remoteBrowse=" + configuration_.remoteBrowse_ + ",";
        services += "search=" + configuration_.search_ + ",";
        services += "searchMatch=" + configuration_.searchMatch_ + ",";
        services += "download=" + configuration_.download_ + ",";
        services += "upload=" + configuration_.upload_ + ",";
        services += "delete=" + configuration_.delete_;
        services += ":";

        return services;

    } // displayServices().


    /**
     * List the names of all the files in this directory.
     *
     * @return String representing the files in the current directory.
     */
    public String listFiles() {

        File[] fileList = thisDir_.listFiles();

        StringBuilder fileListStr = new StringBuilder();

        fileListStr.append("\n+++  id: ").append(configuration_.id_);
        fileListStr.append("\n+++ dir: ").append(getPathName(thisDir_));
        fileListStr.append("\n+++\tfilename:");

        if (fileList != null) {

            for (int i = 0; i < fileList.length; ++i) {

                File f = fileList[i];
                String name = f.getName();
                if (f.isDirectory()) // add a trailing separator to dir names
                    name = name + File.separator;
                fileListStr.append("\n").append(i).append("\t").append(name);
            }

        }

        fileListStr.append("\n+++");

        return fileListStr.toString();

    } // listFiles().


    /**
     * Search for a name in the list of files in this directory.
     *
     * @param name The name of the file to search for.
     */
    public File searchList(String name) {

        File found = null;

        File[] fileList = thisDir_.listFiles();

        if (fileList != null) {

            for (File file : fileList) {

                if (name.equals(file.getName())) {
                    found = file;
                    break;
                }

            }

        }

        return found;
    } // searchList().


    /**
     * Get full pathname.
     *
     * @param f The File for which the pathname is required.
     */
    static public String getPathName(File f) {

        String pathName = null;

        try {
            pathName = f.getCanonicalPath();
        } catch (IOException e) {
            System.out.println("+++ FileTreeBrowser.pathname(): " + e.getMessage());
        }

        return pathName;
    } // getPathname().


    /**
     * Create timestamp string in the correct format: yyyyMMdd-HHmmss.SSS
     *
     * @return Formatted string.
     */
    public static String timestamp() {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS");
        return sdf.format(new Date());

    } // timestamp().


    /**
     * Get parent directory fo current directory.
     *
     * @param ftb FileTreeBrowser instance to move to parent directory of.
     *
     * @return Parent directory of this (current) directory as FileTreeBrowser object, an accompanying message.
     */
    public AbstractMap.SimpleEntry<FileTreeBrowser, String> getParent(FileTreeBrowser ftb) {

        String moveUp;

        // Move up to parent directory, but not above the point where we started!
        if (ftb.thisDirName_.equals(rootPath_)) {
            moveUp = "\nAt root : cannot move up.\n";
        } else {
            String parent = ftb.thisDir_.getParent();
            ftb = new FileTreeBrowser(parent);
            moveUp = "\n<<< " + parent + "\n";
        }

        return new AbstractMap.SimpleEntry<>(ftb, moveUp);

    } // getParent().


    /**
     * Evaluate a pathname.
     * If pathname leads to a file, show file information.
     * If pathname leads to a directory, move into the directory.
     * If pathname leads to nothing, display unknown filename message.
     *
     * @param ftb Current instance of FileTreeBrowser
     * @param userCmd User input which has been determined to have to be a pathname.
     *
     * @return New instance of FileTreeBrowser (if moved into directory, otherwise unchanged), and accompanying message.
     */
    public AbstractMap.SimpleEntry<FileTreeBrowser, String> evaluatePathName(FileTreeBrowser ftb, String userCmd) {

        String resultStr = "";

        File f = ftb.searchList(userCmd);

        if (f == null) {

            resultStr += "\nUnknown filename: " + userCmd;

        } else { // Act upon entered filename.

            String pathName = getPathName(f);

            if (f.isFile()) { // Print some file details.
                resultStr += "\nfile: " + pathName;
                resultStr += "\nsize: " + f.length();
            } else if (f.isDirectory()) { // Move into to the directory.
                resultStr += "\n>>> " + pathName;
                ftb = new FileTreeBrowser(pathName);
            }

        } // (f == null).

        return new AbstractMap.SimpleEntry<>(ftb, resultStr);

    } // evaluatePathname().


} // FileTreeBrowser{}.
