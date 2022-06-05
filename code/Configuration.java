import java.io.*;
import java.net.*;
import java.util.Properties; // https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Properties.html

/**
 * Application Configuration information.
 * CS4105 Practical P2 - Control plane.
 *
 * This is an object that gets passed around, containing useful information.
 *
 * @author Saleem Bhatti, Oct 2020, Sep 2019, Oct 2018.
 * @author 170004680, Nov 2020.
 */
public class Configuration {

    public Properties properties_;
    public String propertiesFile_ = "filetreebrowser.properties";

    public LogFileWriter log_;
    public String logFile_ = "../logs/filetreebrowser.log";

    // These default values are overridden by the properties file.
    public String mAddr_ = "239.255.41.05"; // CS4105 group multicast address.
    public int mPort_ = 4105; // Default port to use.
    public int mTTL_ = 2; // Plenty for the lab.
    public boolean loopbackOff_ = false; // Ignore my own transmissions.
    public boolean reuseAddr_ = false; // Allow address use by other apps.
    public int soTimeout_ = 1; // ms.
    public int sleepTime_ = 5000; // ms.

    // Application config - default values.
    public String rootDir_ = "../root_dir"; // Root directory is parallel to code/ directory.
    public String id_; // System.getProperty("user.name") @ fqdn;
    public int maximumDiscoveryMessageSize_ = 500; // Bytes.
    public int maximumBeaconPeriod_ = 1000; // ms.

    public String[] remoteBrowseOptions_ = {"true", "false"};
    public boolean remoteBrowse_ = false; // Only local browsing.

    public String[] searchOptions_ = {"true", "false"}; // Could have been enum.
    public boolean search_ = false; // from searchOptions_

    public static final String[] searchMatchOptions_ = {"none", "path", "path-filename", "path-filename-substring"};
    public String searchMatch_ = "none"; // from searchMatchOptions_

    public String[] remoteDownloadOptions_ = {"true", "false"};
    public boolean download_ = false;
    public String[] remoteUploadOptions_ = {"true", "false"};
    public boolean upload_ = false;
    public String[] remoteDeleteOptions_ = {"true", "false"};
    public boolean delete_ = false;

    // These should not be loaded from a config file, of course.
    public InetAddress mGroup_;
    public String hostInfo_;


    /**
     * Constructor: Creates a configuration instance using either the default or a specified properties file.
     *
     * @param propertiesFile Specified properties file to read configuration from.
     */
    Configuration(String propertiesFile) {

        InetAddress inetAddress;
        String hostname;

        if (propertiesFile != null) {
            propertiesFile_ = propertiesFile;
        }

        try {

            inetAddress = InetAddress.getLocalHost();
            hostname = inetAddress.getCanonicalHostName();

            hostInfo_ = hostname + " " + inetAddress.getHostAddress();
            id_ = System.getProperty("user.name") + "@" + hostname;
            logFile_ = "../logs/" + id_ + "-log.log";

            properties_ = new Properties();
            InputStream p = getClass().getClassLoader().getResourceAsStream(propertiesFile_);
            if (p != null) {
                properties_.load(p);
                String s;

                if ((s = properties_.getProperty("logFile")) != null) {
                    System.out.println(propertiesFile_ + " logFile: " + logFile_ + " -> " + s);
                    logFile_ = s;
                }

                if ((s = properties_.getProperty("id")) != null) {
                    System.out.println(propertiesFile_ + " id: " + id_ + " -> " + s);
                    id_ = s + "@" + hostname;
                }

                if ((s = properties_.getProperty("rootDir")) != null) {
                    System.out.println(propertiesFile_ + " rootDir: " + rootDir_ + " -> " + s);
                    rootDir_ = s;
                }

                if ((s = properties_.getProperty("mAddr")) != null) {
                    System.out.println(propertiesFile_ + " mAddr: " + mAddr_ + " -> " + s);
                    mAddr_ = s;
                    // Should check for valid multicast address range
                }

                if ((s = properties_.getProperty("mPort")) != null) {
                    System.out.println(propertiesFile_ + " mPort: " + mPort_ + " -> " + s);
                    mPort_ = Integer.parseInt(s);
                    // should check for valid port number range
                }

                if ((s = properties_.getProperty("mTTL")) != null) {
                    System.out.println(propertiesFile_ + " mTTL: " + mTTL_ + " -> " + s);
                    mTTL_ = Integer.parseInt(s);
                    // should check for valid TTL number range
                }

                if ((s = properties_.getProperty("loopbackOff")) != null) {
                    System.out.println(propertiesFile_ + " loopbackOff: " + loopbackOff_ + " -> " + s);
                    loopbackOff_ = Boolean.parseBoolean(s);
                }

                if ((s = properties_.getProperty("reuseAddr")) != null) {
                    System.out.println(propertiesFile_ + " reuseAddr: " + reuseAddr_ + " -> " + s);
                    reuseAddr_ = Boolean.parseBoolean(s);
                }

                if ((s = properties_.getProperty("soTimeout")) != null) {
                    System.out.println(propertiesFile_ + " soTimeout: " + soTimeout_ + " -> " + s);
                    soTimeout_ = Integer.parseInt(s);
                    // should check for "sensible" timeout value
                }

                if ((s = properties_.getProperty("sleepTime")) != null) {
                    System.out.println(propertiesFile_ + " sleepTime: " + sleepTime_ + " -> " + s);
                    sleepTime_ = Integer.parseInt(s);
                    // should check for "sensible" sleep value
                }

                if ((s = properties_.getProperty("maximumDiscoveryMessageSize")) != null) {
                    System.out.println(propertiesFile_ + " maximumDiscoveryMessageSize: " + maximumDiscoveryMessageSize_ + " -> " + s);
                    maximumDiscoveryMessageSize_ = Integer.parseInt(s);
                    // should check for "sensible" message size value
                }

                if ((s = properties_.getProperty("maximumBeaconPeriod")) != null) {
                    System.out.println(propertiesFile_ + " maximumBeaconPeriod: " + maximumBeaconPeriod_ + " -> " + s);
                    maximumBeaconPeriod_ = Integer.parseInt(s);
                    // should check for "sensible" period value
                }

        /*
          Should check values of remoteBrowse, search, searchMatch values

            remoteBrowse :  false    true    true
            search       :  false    false   true
            searchMatch  :  none     none    !none
        */
                if ((s = properties_.getProperty("remoteBrowse")) != null) {
                    if (!checkOption(s, remoteBrowseOptions_)) {
                        System.out.println(propertiesFile_ + " bad value for 'remoteBrowse': '" + s + "' -> using 'false'");
                        s = "false";
                    }
                    System.out.println(propertiesFile_ + " remoteBrowse: " + remoteBrowse_ + " -> " + s);
                    remoteBrowse_ = Boolean.parseBoolean(s);
                }

                if ((s = properties_.getProperty("search")) != null) {
                    if (!checkOption(s, searchOptions_)) {
                        System.out.println(propertiesFile_ + " bad value for 'search': '" + s + "' -> using 'none'");
                        s = "none";
                    }
                    System.out.println(propertiesFile_ + " search: " + search_ + " -> " + s);
                    search_ = Boolean.parseBoolean(s);
                }

                if ((s = properties_.getProperty("searchMatch")) != null) {
                    if (!checkOption(s, searchMatchOptions_)) {
                        System.out.println(propertiesFile_ + " bad value for 'searchMatch': '" + s + "' -> using 'none'");
                        s = "none";
                    }
                    System.out.println(propertiesFile_ + " searchMatch: " + searchMatch_ + " -> " + s);
                    searchMatch_ = s;
                }

                if ((s = properties_.getProperty("download")) != null) {
                    if (!checkOption(s, remoteDownloadOptions_)) {
                        System.out.println(propertiesFile_ + " bad value for 'download': '" + s + "' -> using 'false'");
                        s = "false";
                    }
                    System.out.println(propertiesFile_ + " download: " + remoteBrowse_ + " -> " + s);
                    download_ = Boolean.parseBoolean(s);
                }

                if ((s = properties_.getProperty("upload")) != null) {
                    if (!checkOption(s, remoteUploadOptions_)) {
                        System.out.println(propertiesFile_ + " bad value for 'upload': '" + s + "' -> using 'false'");
                        s = "false";
                    }
                    System.out.println(propertiesFile_ + " upload: " + remoteBrowse_ + " -> " + s);
                    upload_ = Boolean.parseBoolean(s);
                }

                if ((s = properties_.getProperty("delete")) != null) {
                    if (!checkOption(s, remoteDeleteOptions_)) {
                        System.out.println(propertiesFile_ + " bad value for 'delete': '" + s + "' -> using 'false'");
                        s = "false";
                    }
                    System.out.println(propertiesFile_ + " delete: " + remoteBrowse_ + " -> " + s);
                    delete_ = Boolean.parseBoolean(s);
                }

                p.close();

            } // (p != null).

            log_ = new LogFileWriter(logFile_);
            log_.writeLog("-* logFile=" + logFile_, true);
            log_.writeLog("-* id=" + id_, true);
            log_.writeLog("-* rootDir=" + rootDir_, true);
            log_.writeLog("-* mAddr=" + mAddr_, true);
            log_.writeLog("-* mPort=" + mPort_, true);
            log_.writeLog("-* mTTL=" + mTTL_, true);
            log_.writeLog("-* loopbackOff=" + loopbackOff_, true);
            log_.writeLog("-* reuseAddr=" + reuseAddr_, true);
            log_.writeLog("-* soTimeout=" + soTimeout_, true);
            log_.writeLog("-* sleepTime=" + sleepTime_, true);
            log_.writeLog("-* maximumDiscoveryMessageSize=" + maximumDiscoveryMessageSize_, true);
            log_.writeLog("-* maximumBeaconPeriod=" + maximumBeaconPeriod_, true);
            log_.writeLog("-* remoteBrowse=" + remoteBrowse_, true);
            log_.writeLog("-* search=" + search_, true);
            log_.writeLog("-* searchMatch=" + searchMatch_, true);
            log_.writeLog("-* download=" + download_, true);
            log_.writeLog("-* upload=" + upload_, true);
            log_.writeLog("-* delete=" + delete_, true);

        } catch (NumberFormatException | IOException e) {
            System.err.println("Configuration.Configuration() Error: " + e.getMessage());
        }

    } // Configuration().


    /**
     * Check option exists in a given list of options.
     *
     * @param value String to check if exists as an option.
     * @param optionList_ List of options.
     *
     * @return True if options exists, false otherwise.
     */
    public Boolean checkOption(String value, String[] optionList_) {

        boolean found = false;

        for (String option : optionList_) {
            if (value.equals(option)) {
                found = true;
                break;
            }
        }

        return found;

    } // checkOption().


} // Configuration{}.
