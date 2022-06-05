import java.io.*;
import java.util.*;
import java.text.*;

/**
 * LogFileWriter - Simple logging.
 *
 * @author Saleem Bhatti, Oct 2020, Sep 2019, Oct 2018.
 * @author 170004680, Nov 2020.
 */
public class LogFileWriter {


    public FileWriter fw_; // File writer used to write logs to the log file.
    public SimpleDateFormat sdf_; // Simple date format used for consistent log time entries.


    /**
     * Create new LogFileWriter, with log filename specified.
     *
     * @param fileName File name to write logs to.
     */
    public LogFileWriter(String fileName) {

        File lf = new File(fileName);
        sdf_ = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS");

        // Create file writer for the log file, creating the file if need be.
        try {
            if (!lf.exists()) {
                lf.createNewFile();
            }
            fw_ = new FileWriter(fileName, true);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    } // LogFileWriter().


    /**
     * Writes a log to the log file without displaying the log in stdout by default.
     *
     * @param logRequest The log (as string) to write to file.
     */
    public void writeLog(String logRequest) {
        writeLog(logRequest, false);
    } // writeLog().


    /**
     * Writes a log to the log file.
     *
     * @param logRequest The log (as string) to write to file.
     * @param stdout Whether oor not to also display the log to stdout (the console).
     */
    public void writeLog(String logRequest, Boolean stdout) {

        try {
            String now = sdf_.format(new Date());
            String logEntry = now + "| " + logRequest + "\n";
            fw_.write(logEntry, 0, logEntry.length());
            fw_.flush();
            if (stdout) {
                System.out.print(logEntry);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    } // writeLog().


} // LogFileWriter{}.
