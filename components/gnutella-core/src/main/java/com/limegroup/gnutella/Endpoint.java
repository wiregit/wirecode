
// Commented for the Learning branch

package com.limegroup.gnutella;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.StringUtils;

/**
 * An Endpoint object holds an IP address and port number.
 * You can make one from text like "64.61.25.171:6346".
 * If you don't specify a port number, the Endpoint will contain the Gnutella default, 6346.
 * An Endpoint also contains the number of files the computer is sharing, and their total size in KB.
 * 
 * Endpoint implements LimeWire's IpPort interface because it contains an IP address and port number.
 */
public class Endpoint implements Cloneable, IpPort, java.io.Serializable {

    /** A unique number that identifies this version of this class for serialization. */
    static final long serialVersionUID = 4686711693494625070L;

    /** The IP address of the computer, like "64.61.25.171". */
    private String hostname = null;

    /** The port number of the computer, like 6346. */
    int port = 0;

    /** How many files the computer is sharing, -1 if unknown. */
    private long files = -1; // Initialize to -1, unknown

    /** Total size in KB of all the files the computer is sharing, -1 if unknown. */
    private long kbytes = -1; // Initialize to -1, unknown

    /**
     * Make a new Endpoint object.
     * Marked protected so subclasses can serialize Endpoint objects.
     */
    protected Endpoint() {}

    /**
     * Make a new Endpoint object that holds an IP address and port number from text like "64.61.25.171:6346".
     * If your string doesn't have a port number, we'll put in the Gnutella default of 6346.
     * 
     * @param hostAndPort The IP address and port number in a String like "64.61.25.171:6346"
     */
    public Endpoint(String hostAndPort) throws IllegalArgumentException {

        // Make a new Endpoint with the given IP address and port number, not checking each number
        this(hostAndPort, false); // But it will check the first one
    }

    /**
     * Make a new Endpoint object that holds an IP address and port number from text like "64.61.25.171:6346".
     * If your string doesn't have a port number, we'll put in the Gnutella default of 6346.
     * 
     * @param  hostAndPort              The IP address and port number in a String like "64.61.25.171:6346"
     * @param  requireNumeric           True to have us make sure the number of the IP address are each 0-255
     * @throws IllegalArgumentException If we can't read one of the numerals as a number
     */
    public Endpoint(String hostAndPort, boolean requireNumeric) {

        // Make a new Endpoint with the given IP address and port number, checking each number and the first one
        this(hostAndPort, requireNumeric, true);
    }

    /**
     * Make a new Endpoint object with the IP address and port number from text like "64.61.25.171:6346".
     * 
     * @param hostAndPort    Text like "64.61.25.171:6346"
     * @param requireNumeric True to have us make sure the numbers of the IP address are each 0-255
     * @param strict         True to have us make sure the first number isn't 0 or 255
     */
    public Endpoint(String hostAndPort, boolean requireNumeric, boolean strict) {

        /*
         * TODO:kfaaborg I can't find any calls here that might cause a DNS lookup.
         * I think this part of the Javadoc is out of date.
         * 
         * If requireNumeric is true, or strict is false, no DNS lookups are ever involved.
         * If requireNumeric is false or strict is true, a DNS lookup MAY be performed.
         * if the hostname is not numeric.
         * To never block, make sure strict is false.
         */

        // If the caller gives us text without a port number, we'll use 6346, the default Gnutella port
        final int DEFAULT = 6346;

        // Find how far into the given text the colon is
        int j = hostAndPort.indexOf(":");

        // Colon not found, like "text"
        if (j < 0) {

            // Save the text as an IP address and use 6346 for the port number
            this.hostname = hostAndPort;
            this.port     = DEFAULT;

        // The text starts with a colon, like ":text"
        } else if (j == 0) {

            // We need an IP address, throw an exception
            throw new IllegalArgumentException();

        // The text ends with the colon, like "text:"
        } else if (j == (hostAndPort.length() - 1)) {

            // Clip out the IP address before the colon, and use 6346 for the port number
            this.hostname = hostAndPort.substring(0, j);
            this.port     = DEFAULT;

        // The colon is somewhere in the middle of the text, like "some:text"
        } else {

            // Clip out the IP address before the colon and save it in hostname
            this.hostname = hostAndPort.substring(0, j);

            try {

                // Read the text beyond the colon as a number
                this.port = Integer.parseInt(hostAndPort.substring(j + 1));

            // Reading the text as a number caused an exception
            } catch (NumberFormatException e) { throw new IllegalArgumentException(); }

            // Make sure the port number is 1 through 65535
			if (!NetworkUtils.isValidPort(getPort())) throw new IllegalArgumentException("invalid port");
        }

        // If the caller asked us to check the numbers of the IP address
        if (requireNumeric) {

            /* TODO3: implement with fewer allocations */

            // Split the IP address text we parsed around period, and make sure we get 4 parts
            String[] numbers = StringUtils.split(hostname, '.');
            if (numbers.length != 4) throw new IllegalArgumentException();

            // Loop for each of the 4 parts
            for (int i = 0; i < numbers.length; i++) {

                try {

                    // Read the numerals as a number, and make sure it's 0 through 255
                    int x = Integer.parseInt(numbers[i]);
                    if (x < 0 || x > 255) throw new IllegalArgumentException();

                // Reading the text as a number caused an exception
                } catch (NumberFormatException fail) { throw new IllegalArgumentException(); }
            }
        }

        // If the caller passed strict, make sure the first number isn't 0 or 255
        if (strict && !NetworkUtils.isValidAddress(hostname)) throw new IllegalArgumentException("invalid address: " + hostname);
    }

    /**
     * Make a new Endpoint given an IP address like "64.61.25.171" and a port number.
     * 
     * @param hostname An IP address in text like "64.61.25.171"
     * @param port     A port number like 6346
     */
    public Endpoint(String hostname, int port) {

        // Make a new Endpoint with the given IP address and port number
        this(hostname, port, true); // True to make sure the address doesn't start 0 or 255
    }

    /**
     * Make a new Endpoint given an IP address like "64.61.25.171" and a port number.
     * 
     * @param hostname An IP address in text like "64.61.25.171"
     * @param port     A port number like 6346
     * @param strict   True to make sure the address doesn't start 0 or 255
     */
    public Endpoint(String hostname, int port, boolean strict) {

        // Make sure the port number is 1 through 65535 and the address doesn't start 0 or 255
        if (!NetworkUtils.isValidPort(port))                  throw new IllegalArgumentException("invalid port: "    + port);
        if (strict && !NetworkUtils.isValidAddress(hostname)) throw new IllegalArgumentException("invalid address: " + hostname);

        // Save the given IP address text and port number
        this.hostname = hostname;
        this.port     = port;
    }

    /**
     * Make a new Endpoint given an IP address in an array of 4 bytes, and a port number.
     * 
     * @param hostBytes An IP address in an array of 4 bytes, with the most significant byte first
     * @param port      The port number
     */
    public Endpoint(byte[] hostBytes, int port) {

        // Make sure the port number is 1 through 65535 and the address doesn't start 0 or 255
        if (!NetworkUtils.isValidPort(port))         throw new IllegalArgumentException("invalid port: " + port);
        if (!NetworkUtils.isValidAddress(hostBytes)) throw new IllegalArgumentException("invalid address");

        // Save the given IP address port number
        this.port     = port;
        this.hostname = NetworkUtils.ip2string(hostBytes); // Write the byte array as "64.61.25.171"
    }

    /**
     * Make a new Endpoint with an IP address like "64.61.25.171", a port number like 6346, the number of files this computer is sharing, and their total size.
     * 
     * @param hostname An IP address in text like "64.61.25.171"
     * @param port     A port number like 6346
     * @param files    The number of files the computer is sharing
     * @param kbytes   The total size of all those files in KB
     */
    public Endpoint(String hostname, int port, long files, long kbytes) {

        // Store the IP address and port number in this new Endpoint object
        this(hostname, port);

        // Save the given shared file count and total KB size
        this.files  = files;
        this.kbytes = kbytes;
    }

    /**
     * Make a new Endpoint with the given IP address bytes, a port number like 6346, the number of files this computer is sharing, and their total size.
     * 
     * @param hostBytes An IP address in an array of 4 bytes, with the most significant byte first
     * @param port      A port number like 6346
     * @param files     The number of files the computer is sharing
     * @param kbytes    The total size of all those files in KB
     */
    public Endpoint(byte[] hostBytes, int port, long files, long kbytes) {

        // Store the IP address and port number in this new Endpoint object
        this(hostBytes, port);

        // Save the given shared file count and total KB size
        this.files  = files;
        this.kbytes = kbytes;
    }

    /**
     * Make a new Endpoint object from a given one, copying all the data into the new one.
     * 
     * @param ep The Endpoint object to copy all the information from
     */
    public Endpoint(Endpoint ep) {

        // Copy all the parts from the given Endpoint object into this new one
        this.files    = ep.files;    // The number of files the computer is sharing
        this.hostname = ep.hostname; // The IP address of the computer, like "64.61.25.171"
        this.kbytes   = ep.kbytes;   // The total size in KB of all the files the computer is sharing
        this.port     = ep.port;     // The port number of the computer, like 6346
    }

    /**
     * Express this Endpoint object as text like "64.61.25.171:6346".
     * 
     * @return A String
     */
    public String toString() {

        // Compose text like "64.61.25.171:6346"
        return hostname + ":" + port;
    }

    /**
     * The IP address of this Endpoint object.
     * 
     * @return The IP address as a String like "64.61.25.171"
     */
    public String getAddress() {

        // Return the hostname String
        return hostname;
    }

    /**
     * Get the IP address in this Endpoint as an InetAddress object.
     * 
     * @return The IP address as an InetAddress object.
     *         null if we couldn't read hostname as an IP address.
     */
    public InetAddress getInetAddress() {

        try {

            // Read the hostname String like "64.61.25.171" as a Java InetAddress object
            return InetAddress.getByName(hostname);

        // If that caused an exception, return null instead of an InetAddress object
        } catch (UnknownHostException e) { return null; }
    }

    /**
     * Set a new IP address for this Endpoint object.
     * 
     * @param hostname Text like "64.61.25.171"
     */
    public void setHostname(String hostname) {

        // Save the String
        this.hostname = hostname;
    }

    /**
     * Get the port number in this Endpoint object.
     * 
     * @return The port number stored in this Endpoint object.
     *         If you created this Endpoint object without specifing a port number, it will be 6346, the Gnutella default.
     */
    public int getPort() {

        // Return the port number
        return port;
    }

    /**
     * The number of files the computer is sharing, or -1 if we don't know.
     * 
     * @return The number of files the computer is sharing.
     *         -1 if we don't know.
     */
    public long getFiles() {

        // Return how many files the computer is sharing, -1 if unknown
        return files;
    }

    /**
     * Set the number of files the computer is sharing.
     * 
     * @param files How many files the computer is sharing.
     */
    public void setFiles(long files) {

        // Save the given value
        this.files = files;
    }

    /**
     * The total size in KB of all the files the computer is sharing, or -1 if we don't know.
     * 
     * @return The total size of all the files the computer is sharing.
     *         -1 if we don't know.
     */
    public long getKbytes() {

        /*
         * TODO:kfaaborg I don't see where it checks the ratio, as described by this Javadoc.
         * 
         * Returns the size of all files the host has, in kilobytes,
         *  or -1 if I don't know, it also makes sure that the kbytes/files
         *  ratio is not ridiculous, in which case it normalizes the values
         */

        // Return the total size in KB of all the files the computer is sharing, -1 if unknown
        return kbytes;
    }

    /**
     * Not used.
     * 
     * If the number of files or the kbytes exceed certain limit, it
     * considers them as false data, and initializes the number of
     * files as well as kbytes to zero in that case
     */
    public void normalizeFilesAndSize() {
        //normalize files
        try {
            if(kbytes > 20000000) { // > 20GB
                files = kbytes = 0;
                return;
            } else if(files > 5000) {  //> 5000 files
                files = kbytes = 0;
                return;
            } else if (kbytes/files > 250000) { //> 250MB/file
                files = kbytes = 0;
                return;
            }
        } catch (ArithmeticException ae) {
            files = kbytes = 0;
            return;
        }
    }

    /**
     * Set the total KB size of all the files the computer is sharing.
     * 
     * @param kbytes How much data the computer is sharing.
     */
    public void setKbytes(long kbytes) {

        // Save the given value
        this.kbytes = kbytes;
    }

    /**
     * Endpoints are equal if their IP addresses and port numbers are the same.
     * The shared file number and size doesn't matter.
     * 
     * @param o Another Endpoint object to compare this one to
     * @return  True if it's the same as this one, false if it's different
     */
    public boolean equals(Object o) {

        // If the given object isn't even an Endpoing, it's not the same as this one
        if (!(o instanceof Endpoint)) return false;

        // If the given object is the same object as this one, they're the same
        if (o == this) return true;

        // Look at the given object as an Endpoint
        Endpoint e = (Endpoint)o;

        // Return true if the hostname String objects contain the same text, and the port numbers are the same
        return hostname.equals(e.hostname) && port == e.port;
    }

    /**
     * Return hostname.hashCode(), the hash code of the IP address String text.
     * This is good enough, since one IP address rarely has multiple different port numbers.
     * 
     * The HostCatcher class keeps a HashSet of ExtendedEndpoint objects.
     * A HashSet is like a List that makes sure all its elements are unique.
     * To determine if a new ExtendedEndpoint is already in the HashSet, the HashSet will call e.hashCode() on the new one.
     * That call will go here.
     * The hash code we return is an int, the result of calling hashCode() on the hostname String like "64.61.25.171".
     * 
     * @return A hash code value for this object
     */
    public int hashCode() {

        // Call hashCode() on the hostname String, and return that as this Endpoint object's hash code
        return hostname.hashCode();
    }

    /**
     * Create and return a copy of this Endpoint object.
     * 
     * @return A new Endpoint object with the same data as this one
     */
    protected Object clone() {

        // Make a new Endpoint object with the IP address text and port, file, and total size numbers
        return new Endpoint(new String(hostname), port, files, kbytes); // Use new String(hostname) to copy the string, not pass a reference
    }

    /**
     * Get the IP address in this Endpoint as an array of 4 bytes.
     * 
     * @return A byte array with the IP address
     */
    public byte[] getHostBytes() throws UnknownHostException {

        // Make an InetAddress object from the hostname text like "64.61.25.171", and have it produce the array of 4 bytes
        return InetAddress.getByName(hostname).getAddress();
    }

    /**
     * Used only by test code.
     * 
     * @requires this and other have dotted-quad addresses, or
     *  names that can be resolved.
     * @effects Returns true if this is on the same subnet as 'other',
     *  i.e., if this and other are in the same IP class and have the
     *  same network number.
     */
    public boolean isSameSubnet(Endpoint other) {
        byte[] a;
        byte[] b;
        int first;
        try {
            a=getHostBytes();
            first=ByteOrder.ubyte2int(a[0]);
            b=other.getHostBytes();
        } catch (UnknownHostException e) {
            return false;
        }
        //See http://www.3com.com/nsc/501302.html
        //class A
        if (first<=127)
            return a[0]==b[0];
        //class B
        else if (first <= 191)
            return a[0]==b[0] && a[1]==b[1];
        //class C
        else
            return a[0]==b[0] && a[1]==b[1] && a[2]==b[2];
    }

    /**
     * Determines if this is the IP address and port number of a UDP host cache.
     * 
     * @return False, only ExtendedEndpoint objects are used this way.
     */
    public boolean isUDPHostCache() {

        // Report that this Endpoint object doesn't hold the IP address and port number of a UDP host cache
        return false;
    }
}
