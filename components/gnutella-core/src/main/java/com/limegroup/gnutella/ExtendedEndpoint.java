
// Commented for the Learning branch

package com.limegroup.gnutella;

import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.util.Comparator;
import java.util.Iterator;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.Buffer;
import com.limegroup.gnutella.util.StringUtils;
import com.limegroup.gnutella.util.NetworkUtils;

/**
 * An ExtendedEndpoint holds information about a remote computer on the Internet running Gnutella software, like its IP address, port number, and how much it's online.
 * We can try to connect to this computer to connect to the Gnutella network.
 * The HostCatcher class keeps a list of ExtendedEndpoint objects, and picks the best looking ones to try to connect to next.
 * 
 * Code here converts between an ExtendedEndpoint object and a line of text in the gnutella.net file, like this:
 * 
 * 67.9.175.234:6346,86400      ,1132897988921,1134345039890;1133994459812;1133799034890,                       ,en           ,2                   \n
 * IP Address  :Port,dailyUptime,timeRecorded ,getConnectionSuccesses()                 ,getConnectionFailures(),_clientLocale,udpHostCacheFailures
 * 
 * dailyUptime is the number of seconds the computer is online on an average day.
 * timeRecorded is the last time we got a pong from the remote computer. (do) or when we added it to the cache
 * The connection successes are the 3 most recent times we've connected to the computer.
 * The times are at least 24 hours apart from one another.
 * The connection failures are the times we tried to connect, and couldn't.
 * _clientLocale is the computer's language preference.
 * 
 * Some ExtendedEndpoint objects describe the IP address and port number of UDP host caches instead of Gnutella computers.
 * In the lien of text, the udpHostCacheFailures number is only present if this is the IP address and port number of a UDP host cache.
 * It's the number of times we've tried to connect to this UDP host cache, and were unable to.
 * 
 * ExtendedEndpoint has a nested class named PriorityComparator.
 * You can call PriorityComparator.compare(e1, e2), giving it two ExtendedEndpoint objects.
 * It will tell you which one you'll be more likely to reach online.
 * 
 * ExtendedEndpoint extends Endpoint, an object that can hold an IP address and port number.
 * ExtendedEndpoint does not override the compareTo method because that could create confusion between compareTo and equals.
 */
public class ExtendedEndpoint extends Endpoint {

    /** 0, if the gnutella.net file doesn't specify a pong time, we'll use 0, a really long time ago. */
    static final long DEFAULT_TIME_RECORDED = 0;

    /**
     * The time when the HostCatcher added this IP address and port number to the cache.
     * This is also the time we last received a pong packet from this remote computer. (do)
     * The ExtendedEndpoint constructors set timeRecorded to now.
     * -1 if we don't know.
     */
    private long timeRecorded = -1;

    /**
     * 345 seconds, the value to use for dailyUptime for a computer if we don't know it.
     * getDailyUptime returns 345 if _dailyUptime was not set from -1.
     * 
     * By looking at version logs, LimeWire developers found the average session uptime for a host was about 8.1 minutes.
     * Check out http://www.limewire.com/developer/lifetimes/
     * Also, LimeWire developers estimated that users connect to the network about 0.71 times per day.
     * This leads to a total of 8.1 * 60 * 0.71 = 345 seconds of uptime each day.
     * 
     * We'll choose a node with an unknown uptime over one with a confirmed low uptime.
     * We use a default of 345 instead of 0 to give unknown computers the advantage.
     */
    static final int DEFAULT_DAILY_UPTIME = 345;

    /**
     * The average daily uptime in seconds, as reported by the daily uptime "DU" GGEP extension. (do)
     * -1 if we don't know, in which case we'll use 345 DEFAULT_DAILY_UPTIME for calculations.
     */
    private int dailyUptime = -1;

    /** 3, we'll keep this many times in the connectSuccesses and connectFailures Buffer objects. */
    static final int HISTORY_SIZE = 3;

    /** 1 day in milliseconds, we'll make sure the times we record in connectSuccesses and connectFailures are more than 24 hours apart. */
    static final long WINDOW_TIME = 24 * 60 * 60 * 1000;

    /**
     * A buffer that holds 3 times that we got a response back from this remote computer.
     * The most recent time is stored first, and all the times are at least 24 hours apart.
     * If we add a 4th time, the Buffer will push the oldest off the edge and discard it.
     */
    private Buffer connectSuccesses = new Buffer(HISTORY_SIZE); // We'll hold Long objects here that are times in milliseconds

    /**
     * A buffer that holds 3 times when we weren't able to contact this remote computer.
     * The most recent time is stored first, and all the times are at least 24 hours apart.
     * If we add a 4th time, the Buffer will push the oldest off the edge and discard it.
     */
    private Buffer connectFailures = new Buffer(HISTORY_SIZE); // We'll hold Long objects here that are times in milliseconds

    /**
     * The language preference of this remote computer, like "en" for English.
     * When we find out the remote computer's language preference, we'll call setClientLocale() to set it.
     */
    private String _clientLocale = ApplicationSettings.DEFAULT_LOCALE.getValue();

    /**
     * If this is the IP address and port number of a UDP host cache, udpHostCacheFailures counts how many times we've been unable to connect to it.
     * If this isn't the address of a UDP host cache, udpHostCacheFailures is -1.
     */
    private int udpHostCacheFailures = -1; // By default, this ExtendedEndpoing object won't hold the IP address and port number of a UDP host cache

    /** Get our language preferences from settings, like "en" for English. */
    private final static String ownLocale = ApplicationSettings.LANGUAGE.getValue();

    /**
     * Make a new ExtendedEndpoint with the given IP address and port number, and the average number of seconds each day this remote computer is online.
     * We read the uptime data from a ping reply.
     * Sets the creation time to now.
     * We assume that we haven't tried to connect to this remote computer yet.
     * 
     * @param host        An IP address in a string like "64.61.25.171"
     * @param port        A port number, like 6346
     * @param dailyUptime The number of seconds this remote computer is online in an average day
     */
    public ExtendedEndpoint(String host, int port, int dailyUptime) {

        // Save the given values in this new object and set timeRecorded to now
        super(host, port);               // Have the Endpoint constructor save the IP address and port number and make sure the address doesn't start 0 or 255
        this.dailyUptime  = dailyUptime; // Save the number of seconds this computer is online on an average day
        this.timeRecorded = now();       // Record now as when the HostCatcher added this IP address and port number
    }

    /**
     * Make a new ExtendedEndpoint with the given IP address and port number.
     * 
     * Creates a new ExtendedEndpoint without extended uptime information, we'll use the default.
     * Sets the creation time to now.
     * We assume we haven't tried to connect to this remote computer yet.
     * 
     * @param host An IP address in a string like "64.61.25.171"
     * @param port A port number, like 6346
     */
    public ExtendedEndpoint(String host, int port) {

        // Save the given values in this new object and set timeRecorded to now
        super(host, port);         // Have the Endpoint constructor save the IP address and port number and make sure the address doesn't start 0 or 255
        this.timeRecorded = now(); // Record now as when the HostCatcher added this IP address and port number
    }

    /**
     * Make a new ExtendedEndpoint with the given IP address and port number.
     * 
     * Creates a new ExtendedEndpoint without extended uptime information, we'll use the default.
     * Sets the creation time to now.
     * We assume we haven't tried to connect to this remote computer yet.
     * Does not validate the IP address.
     * 
     * @param host   An IP address in a string like "64.61.25.171"
     * @param port   A port number, like 6346
     * @param strict True to make sure the address doesn't start 0 or 255
     */
    public ExtendedEndpoint(String host, int port, boolean strict) {

        // Save the given values in this new object and set timeRecorded to now
        super(host, port, strict); // Have the Endpoint constructor save the IP address and port number and make sure the address doesn't start 0 or 255
        this.timeRecorded = now(); // Record now as when the HostCatcher added this IP address and port number
    }

    /**
     * Make a new ExtendedEndpoint with the given IP address and port number, the number of seconds this computer is online in a day, and its language preference.
     * 
     * @param host        An IP address in a string like "64.61.25.171"
     * @param port        A port number, like 6346
     * @param dailyUptime The number of seconds this remote computer is online in an average day
     * @param locale      The remote computer's language preference, like "en" for English
     */
    public ExtendedEndpoint(String host, int port, int dailyUptime, String locale) {

        // Save the given values in this new object and set timeRecorded to now
        super(host, port);               // Have the Endpoint constructor save the IP address and port number
        this.dailyUptime  = dailyUptime; // Save the given value
        this.timeRecorded = now();       // Record now as when the HostCatcher added this IP address and port number
        _clientLocale     = locale;      // Save the given value
    }

    /**
     * Make a new ExtendedEndpoint with the given IP address, port number, and language preference.
     * Leaves timeRecorded -1 instead of setting it to the time right now.
     * 
     * @param host   An IP address in a string like "64.61.25.171"
     * @param port   A port number, like 6346
     * @param locale The remote computer's language preference, like "en" for English
     */
    public ExtendedEndpoint(String host, int port, String locale) {

        // Save the given values in this new object
        this(host, port);       // Have the Endpoint constructor save the IP address and port number
        _clientLocale = locale; // Save the given value
    }

    /*
     * ////////////////////// Mutators and Accessors ///////////////////////
     */

    /** Only used by test code. */
    public long getTimeRecorded() {
        if (timeRecorded < 0)
            return DEFAULT_TIME_RECORDED; //don't know
        else
            return timeRecorded;
    }

    /**
     * The number of seconds this computer is online in an average day.
     * The remote computer told us this in the daily uptime "DU" GGEP extension in a pong packet.
     * 
     * @return The average daily uptime in seconds/day
     */
    public int getDailyUptime() {

        // Return the number of seconds the computer is online on an average day, or 345 seconds if we don't know
        if (dailyUptime < 0) return DEFAULT_DAILY_UPTIME; // Value not known, return 345 seconds
        else                 return dailyUptime;
    }

    /**
     * Record this computer's average daily uptime in the ExtendedEndpoing object that holds its IP address and port number.
     * 
     * @param uptime The average daily uptime in seconds/day
     */
    public void setDailyUptime(int uptime) {

        // Save the given value in this object
    	dailyUptime = uptime;
    }

    /**
     * Make a record in this object that we just connected to its IP address and port number.
     * HostCatcher.doneWithConnect() calls this.
     */
    public void recordConnectionSuccess() {
        
        // Add the time now to the list of times we've connected successfully to this IP address and port number
        recordConnectionAttempt(connectSuccesses, now()); // Won't create a new record until a day after the most recent one already in the Buffer
    }

    /**
     * Make a record in this object that we were just unable to connect to its IP address and port number.
     * HostCatcher.doneWithConnect() calls this.
     */
    public void recordConnectionFailure() {
        
        // Add the time now to the list of times we've been unable to connect to this IP address and port number
        recordConnectionAttempt(connectFailures, now()); // Won't create a new record until a day after the most recent one already in the Buffer
    }

    /**
     * Get the last 3 times we successfully connected to this IP address and port number.
     * 
     * @return an Iterator of up to 3 Long objects that are times in milliseconds since 1970, in most recent to older order
     */
    public Iterator getConnectionSuccesses() {

        // Just call iterator() on the Buffer object
        return connectSuccesses.iterator();
    }

    /**
     * Get the last 3 times we tried and failed to connect to this IP address and port number.
     * 
     * @return An Iterator of up to 3 Long objects that are times in milliseconds since 1970, in most recent to older order
     */
    public Iterator getConnectionFailures() {

        // Just call iterator() on the Buffer object
        return connectFailures.iterator();
    }

    /**
     * Get the remote computer's language choice.
     * addToLocaleMap() uses this.
     * 
     * @return A string like "en" for English
     */
    public String getClientLocale() {

        // Return the value we set
        return _clientLocale;
    }

    /**
     * Set the language choice this remote computer has.
     * We use this as soon as this remote computer tells us its language preference.
     * 
     * @param locale A String like "en" for english
     */
    public void setClientLocale(String locale) {

        // Save the value the remote computer told us
        _clientLocale = locale;
    }

    /**
     * Determine if this is the address of a UDP host cache.
     * 
     * @return True if this is the IP address and port number of a UDP host cache.
     *         False if it's the address of something else, like a Gnutella computer.
     */
    public boolean isUDPHostCache() {

        // If this isn't the address of a UDP host cache, we're not using udpHostCacheFailures
        return udpHostCacheFailures != -1;
    }

    /**
     * Record that we tried to contact the UDP host cache at this IP address and port number, and couldn't.
     * UDPHostCache.HostExpirier.unregistered() calls this.
     */
    public void recordUDPHostCacheFailure() {

        // Make sure we're using this ExtendedEndpoint object to hold the address of a UDP host cache
        Assert.that(isUDPHostCache());

        // Count one more failure trying to contact the UDP host cache at this IP address and port number
        udpHostCacheFailures++;
    }

    /**
     * Record one less failure trying to contact this address of a UDP host cache.
     * UDPHostCache.decrementFailures() lowers the failure count of all the UDP host caches in its list.
     * 
     * We use this when our Internet connection has died and we don't want to mark this UDP host cache unreachable. (do)
     */
    public void decrementUDPHostCacheFailure() {

        // Make sure we're using this ExtendedEndpoint object to hold the address of a UDP host cache
        Assert.that(isUDPHostCache());

        // Lower the count, but not below 0
        udpHostCacheFailures = Math.max(0, udpHostCacheFailures - 1);
    }

    /**
     * Record that we successfully contacted the UDP host cache at this address.
     * 
     * Sets udpHostCacheFailures to 0.
     * This marks that this ExtendedEndpoint as holding the IP address and port number of a UDP host cache.
     * It also gives it a clean record, numbers higher than 0 count the times we've been unable to contact it.
     */
    public void recordUDPHostCacheSuccess() {

        // Make sure we're using this ExtendedEndpoint object to hold the address of a UDP host cache
        Assert.that(isUDPHostCache());

        // Set the count to 0, marking this as a UDP host cache, and counting that we haven't had trouble contacting it
        udpHostCacheFailures = 0;
    }

    /**
     * Returns how many times we've had trouble contacting the UDP host cache at this address.
     * 
     * @return The number of times we've been unable to contact the UDP host cache at this IP address and port number.
     *         0 if this is a UDP host cache we've never tried to contact or we've always been able to contact.
     *         -1 if the IP address and port number in this ExtendedEndpoint aren't the address of a UDP host cache.
     */
    public int getUDPHostCacheFailures() {

        // Return the count we initialized to -1, set to 0, and incremented as we had trouble connecting
        return udpHostCacheFailures;
    }

    /**
     * Call setUDPHostCache(true) to mark this ExtendedEndpoint as holding the IP address and port number of a UDP host cache.
     * It will also start counting our unsuccessful attempts to contact it.
     * 
     * This method returns a reference to this object so you can use it in a call like this:
     * ExtendedEndpoint ep = new ExtendedEndpoint(host, port).setUDPHostCache(true);
     * 
     * @return A reference to this ExtendedEndpoint object
     */
    public ExtendedEndpoint setUDPHostCache(boolean cache) {

        // Mark or unmark this ExtendedEndpoint as holding the address of a UDP host cache
        if (cache == true) udpHostCacheFailures = 0;  // It is a UDP host cache, and we haven't had trouble contacting it yet
        else               udpHostCacheFailures = -1; // It's not a UDP host cache, it's something else, like a remote computer running Gnutella software

        // Return a pointer to this ExtendedEndpoint object so you can chain this method call right after the constructor
        return this;
    }

    /**
     * If the time is more than a day later than one already in the buffer, adds it.
     * This pushes the times already in the buffer down, and discards the last one.
     * 
     * @param buf The buffer we'll put the long number into
     * @param now A time to add to the buffer
     */
    private void recordConnectionAttempt(Buffer buf, long now) {

        // The buffer is empty
        if (buf.isEmpty()) {

            // Add it in the buffer's first position
            buf.addFirst(new Long(now)); // Make a new Long object with the long value, and store it in the Buffer

        // The buffer has a first value, and this time is more than 24 hours later than it
        } else if (now - ((Long)buf.first()).longValue() >= WINDOW_TIME) {

            // Add it in the buffer's first position
            buf.addFirst(new Long(now)); // This moves the other records forward, and discards the last one

        // The buffer has a first value, and this time is within 24 hours of it
        } else {

            // Replace the first buffer value with this one
            buf.removeFirst();
            buf.addFirst(new Long(now));
        }
    }

    /**
     * The time now, System.currentTimeMillis().
     * This is a separatre method just for testing.
     */
    protected long now() {

        // Ask the system for the number of milliseconds between January, 1970 and right now
        return System.currentTimeMillis();
    }

    /*
     * ///////////////////////// Reading and Writing ///////////////////////
     */

    /** A semicolon ";" the separator for list elements, like connection successes. (do) */
    private static final String LIST_SEPARATOR = ";";

    /** A comma "," the separator for fields in the gnutella.net file. */
    private static final String FIELD_SEPARATOR = ",";

    /**
     * "\n", the record separator in the gnutella.net file.
     * 
     * LimeWire has always used "\n" for the record separator in gnutella.net files,
     * even on systems like Windows that end a line with "\r\n".
     * This makes gnutella.net files portable across platforms.
     */
    public static final String EOL = "\n";

    /**
     * Writes the information in this ExtendedEndpoint object in a line of text.
     * We'll use this to save the ExtendedEndpoint objects in the host cache to gnutella.net file on the disk.
     * 
     * Takes a Java Writer object that we call write() on.
     * We don't flush the Writer.
     * 
     * @param out The object we'll call write(b) on to write text to the file
     */
    public void write(Writer out) throws IOException {

        /*
         * A line in the gnutella.net file ends with a "\n" character, and looks like this:
         * 
         * 67.9.175.234:6346,86400,1132897988921,1134345039890;1133994459812;1133799034890,,en,
         * 
         * Here are the different parts:
         * 
         * 67.9.175.234:6346,86400      ,1132897988921,1134345039890;1133994459812;1133799034890,                       ,en           ,
         * IP Address  :Port,dailyUptime,timeRecorded ,getConnectionSuccesses()                 ,getConnectionFailures(),_clientLocale,
         */

        // Start the line with the IP address and port number, like "64.61.25.171:6346,"
        out.write(getAddress());
        out.write(":");
        out.write(getPort() + ""); // Add an empty String to turn the int from getPort() into a String
        out.write(FIELD_SEPARATOR);

        // Continue the line with the number of seconds this computer is online in an average day, like "86400,"
        if (dailyUptime >= 0) out.write(dailyUptime + "");
        out.write(FIELD_SEPARATOR);

        // The time the HostCatcher added this IP address and port number to its cache, like "1132897988921,"
        if (timeRecorded >= 0) out.write(timeRecorded + "");
        out.write(FIELD_SEPARATOR);

        // The times we successfully connected to this address, separated by semicolons, like "1134345039890;1133994459812;1133799034890,"
        write(out, getConnectionSuccesses()); // Call the next method to add semicolons to the list
        out.write(FIELD_SEPARATOR);

        // The times we were unable to connect to this address, "," if there aren't any
        write(out, getConnectionFailures());
        out.write(FIELD_SEPARATOR);

        // The remote computer's language preference, like "en,"
        out.write(_clientLocale); // Text like "en"
        out.write(FIELD_SEPARATOR);

        // If we're using this ExtendedEndpoint object to hold the address of a UDP host cache
        if (isUDPHostCache()) {

            // After that, write the number of times we couldn't connect, like "2"
            out.write(udpHostCacheFailures + ""); // Add an empty String to turn the int into a String
        }

        // End the line with "\n"
        out.write(EOL);
    }

    /**
     * Moves the given Iterator over the objects, calling toString() on each and writing the text to the given Writer.
     * The write() method above uses this to compose text like "1134345039890;1133994459812;1133799034890".
     * 
     * @param out     The object we'll call write(b) on to write text to the file
     * @param objects An object we can call toString() on to get it to express itself as text
     */
    private void write(Writer out, Iterator objects) throws IOException {

        // Loop until the Iterator doesn't have any more objects
        while (objects.hasNext()) {

            // Get the next object from the Iterator, have it compose text about itself, and write the text to the given Writer
            out.write(objects.next().toString());

            // If this loop will run again, separate records with ";"
            if (objects.hasNext()) out.write(LIST_SEPARATOR);
        }
    }

    /**
     * Reads a line from the gnutella.net file and makes a new ExtendedEndpoint object with the information.
     * HostCatcher.read() calls this with each line from the gnutella.net file.
     * 
     * @param line A line of text from the gnutella.net file, like "67.9.175.234:6346,86400,1132897988921,,,en,2\n"
     * @return     A new ExtendedEndpoint object with that IP address, port number, uptime, time recorded, successes, failures, language, and UDP host cache failures
     */
    public static ExtendedEndpoint read(String line) throws ParseException {

        // Split the line around "," into an array of strings
        String[] linea = StringUtils.splitNoCoalesce(line, FIELD_SEPARATOR); // Use splitNoCoalesce to turn ",,," into blank strings in the array
        if (linea.length == 0) throw new ParseException("Empty line", 0);

        /*
         * Use the Endpoint constructor to parse text like "67.9.175.234:6346" into a host String and port number.
         * An earlier version of LimeWire may have left a corrupted gnutella.net file on the disk.
         * This code will work around it.
         */

        // Variables for the validated host String and port number
        boolean pureNumeric; // True if we got the Endpoint constructor to make sure all the parts of the IP address are numbers
        String  host;        // The IP address as text, like "67.9.175.234"
        int     port;        // The port number

        try {

            // Make a new Endpoint object from the text like "67.9.175.234:6346"
            Endpoint tmp = new Endpoint(linea[0], true); // True to have it make sure the numbers of the IP address are each 0-255
            host = tmp.getAddress(); // Read the IP address text and port number back from it
            port = tmp.getPort();
            pureNumeric = true; // We had the Endpoint make sure each part is a number

        // Making the new Endpoint caused an exception, reading some of the text as a number didn't work
        } catch (IllegalArgumentException e) {

            try {

                // This time, try to make the new Endpoint without any number checking at all
                Endpoint tmp = new Endpoint(linea[0], false, false);
                host = tmp.getAddress();
                port = tmp.getPort();
                pureNumeric = false; // We didn't have the Endpoing to number checking

            // Even doing it that way caused an exception
            } catch (IllegalArgumentException e2) {

                // Compose an exception and throw it
                ParseException e3 = new ParseException("Couldn't extract address and port from: " + linea[0], 0);
                if (CommonUtils.isJava14OrLater()) e3.initCause(e2);
                throw e3;
            }
        }

        // Make a new ExtendedEndpoint with the host and port we used the Endpoint constructor to parse and validate
        ExtendedEndpoint ret = new ExtendedEndpoint(host, port, false);

        // There's a part 2 after that, the average uptime in seconds/day, like "86400"
        if (linea.length >= 2) {

            // Save it in ret.dailyUptime
            try { ret.dailyUptime = Integer.parseInt(linea[1].trim()); } catch (NumberFormatException e) {}
        }

        // There's a part 3, the time of pong, like "1134345039890"
        ret.timeRecorded = DEFAULT_TIME_RECORDED; // If the gnutella.net file doesn't specify a time, use 0, not the time now
        if (linea.length >= 3) {

            // Save it in ret.timeRecorded
            try { ret.timeRecorded = Long.parseLong(linea[2].trim()); } catch (NumberFormatException e) {}
        }

        // There's a part 4, the times we've successfully connected, like "1134345039890;1133994459812;1133799034890"
        if (linea.length >= 4) {

            try {

                // Split the text on ";" and loop through the parts
                String times[] = StringUtils.split(linea[3], LIST_SEPARATOR);
                for (int i = times.length - 1; i >= 0; i--) {

                    // Add the times to the ret.connectSuccesses Buffer, making sure they are each 24 hours or more apart
                    ret.recordConnectionAttempt(ret.connectSuccesses, Long.parseLong(times[i].trim()));
                }

            // If reading the numerals as a number didn't work, ignore it and keep going
            } catch (NumberFormatException e) {}
        }

        // There's a part 5, the times we've failed to connect, like "1134345039822;1133799034878"
        if (linea.length >= 5) {

            try {

                // Split the text on ";" and loop through the parts
                String times[] = StringUtils.split(linea[4], LIST_SEPARATOR);
                for (int i = times.length - 1; i >= 0; i--) {

                    // Add the times to the ret.connectFailures Buffer, making sure they are each 24 hours or more apart
                    ret.recordConnectionAttempt(ret.connectFailures, Long.parseLong(times[i].trim()));
                }

            // If reading the numerals as a number didn't work, ignore it and keep going
            } catch (NumberFormatException e) {}
        }

        // There's a part 6, the language preference of the computer, like "en"
        if (linea.length >= 6) {

            // Save it in ret._clientLocale
            ret.setClientLocale(linea[5]);
        }

        // There's a part 7, the number of times we've failed to connect to this UDP host cache, like "2"
        if (linea.length >= 7) {

            try {

                // Save it in ret.udpHostCacheFailures
                int i = Integer.parseInt(linea[6]);
                if (i >= 0) ret.udpHostCacheFailures = i;

            // If reading the numerals as a number didn't work, ignore it and keep going
            } catch (NumberFormatException nfe) {}
        }

        // If the Endpoint constructor was able to validate that all the parts of the IP address are 0-255, make sure the first number isn't 0 or 255
        if (pureNumeric && !NetworkUtils.isValidAddress(host)) throw new ParseException("invalid dotted addr: " + ret, 0);

        // If this isn't the address of a UDP host cache and the Endpoint constructor had an exception reading the number, throw an exception
        if (!ret.isUDPHostCache() && !pureNumeric) throw new ParseException("illegal non-UHC endpoint: " + ret, 0);

        // Return the ExtendedEndpoint object we made and filled with values from the line of text
        return ret;
    }

    /*
     * ////////////////////////////// Other /////////////////////////////
     */

    /**
     * Get the PriorityComparator object.
     * Call compare() on it to see which of two ExtendedEndpoint objects has the best history of being available online.
     * 
     * @return The PriorityComparator object
     */
    public static Comparator priorityComparator() {

        // Return a reference to the static object the next line of code makes
        return PRIORITY_COMPARATOR;
    }

    /**
     * Make the one static PriorityComparator object.
     */
    private static final Comparator PRIORITY_COMPARATOR = new PriorityComparator();

    /**
     * A class you can use to see which of two ExtendedEndpoints is the best.
     * This class implements the Comparator interface, which requires it to have a compare() method.
     */
    static class PriorityComparator implements Comparator {

        /**
         * Given two ExtendedEndpoint objects, determine which we have a better chance of connecting to.
         * 
         * Here's how we decide:
         * First, look at the times we've connected and been unable to connect to the two computers.
         * If that's a tie, pick the one that matches our language preference.
         * If they're both the same, choose the one with the longest average daily uptime.
         * 
         * @param extEndpoint1 The first ExtendedEndpoint object to compare.
         * @param extEndpoint2 The second ExtendedEndpoint object to compare.
         * @return             A negative number if the second is the best.
         *                     0 if they are the same.
         *                     A positive number if the first is the best.
         */
        public int compare(Object extEndpoint1, Object extEndpoint2) {

            // Look at the given objects as their true type, ExtendedEndpoint
            ExtendedEndpoint a = (ExtendedEndpoint)extEndpoint1;
            ExtendedEndpoint b = (ExtendedEndpoint)extEndpoint2;

            // If we've connected to a more sucessfully than b, return 1
            int ret = a.connectScore() - b.connectScore();
            if (ret != 0) return ret;

            // If we match languages with a and not b, return 1
            ret = a.localeScore() - b.localeScore();
            if (ret != 0) return ret;

            // It's a tie for both of those, compare their daily average uptimes
            return a.getDailyUptime() - b.getDailyUptime();
        }
    }

    /**
     * Determine if our language choice, like "en", matches that of the remote computer at this IP address and port number.
     * 
     * @return -1 Bad, our languages choices are different.
     *         0 Neutral, we're not using locale preferencing in settings.
     *         1 Good, this remote computer's language choice matches ours.
     */
    private int localeScore() {

        // If settings have disabled locale preferencing, return 0
        if (!ConnectionSettings.USE_LOCALE_PREF.getValue()) return 0; // Locale preferencing is enabled by default

        // If our locale, like "en", matches the remote computer with this IP address and port number, return 1
        if (ownLocale.equals(_clientLocale)) return 1;

        // Our locales don't match, return -1
        return -1;
    }

    /**
     * Returns at number based on how successful we've been contacting this IP address and port number.
     * 
     * @return -1 Bad, we've tried to connect and failed.
     *         0 Unknown, we haven't tried to connect.
     *         1 Good, we've tried to connect and succeeded.
     */
    private int connectScore() {

        // We've never tried to connect to this IP address and port number
        if (connectSuccesses.isEmpty() && connectFailures.isEmpty()) {

            // Unknown availability
            return 0;

        // We've had only failures
        } else if (connectSuccesses.isEmpty()) {

            // Bad
            return -1;

        // We've had only successes
        } else if (connectFailures.isEmpty()) {

            // Good
            return 1;

        // We've had successes and failures
        } else {

            // Get the oldest times we succeeded and failed to connect to the remote computer
            long success = ((Long)connectSuccesses.last()).longValue();
            long failure = ((Long)connectFailures.last()).longValue();

            // Compare them
            if      (success > failure) return 1;  // Our first failure happened before our first success, good
            else if (success < failure) return -1; // Our first success happened before our first failure, bad
            else                        return 0;
        }
    }

    /**
     * ExtendedEndpoint objects are equal if their IP addresses and port numbers are the same.
     * The additional information they hold doesn't matter.
     * 
     * @param other Another ExtendedEndpoint object to compare this one to
     * @return      True if the IP addresses and port numbers match, false if they are different
     */
    public boolean equals(Object other) {

        // Just compare the IP address and port number
        return super.equals(other);

        /*
         * TODO: implement
         */
    }
}
