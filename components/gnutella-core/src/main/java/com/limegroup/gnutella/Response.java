
// Commented for the Learning branch

package com.limegroup.gnutella;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.metadata.AudioMetaData;
import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.messages.HUGEExtension;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.NameValue;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * A Response object represents the part of a Gnutella query hit packet that describes a file being returned in the search results.
 * A query hit packet can contain one or many of these sections.
 * They have information about a file the computer is sharing.
 * 
 * A result has the following binary structure:
 * 
 * IIII
 * SSSS
 * File Name.mp3\0
 * extension[0x1C]extension[0x1C]extension\0
 * 
 * The first 4 byte int is the file index number the sharing computer has assigned this file.
 * We can use this number to request the file in a HTTP GET.
 * The next 4 byte int is the file size.
 * The file name is null terminated ASCII text.
 * The section ends with the extended area, which is also null terminated.
 * 
 * The extensions in the extended area can be:
 * Hash URNs like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ" that have the file's SHA1 hash.
 * Text metadata like "128 Kbps 44 kHz 3:12".
 * XML metadata about the file.
 * A GGEP block.
 * 
 * The GGEP block can have the "ALT" and "CT" extensions.
 * "ALT" Alternate Locations lists the IP addresses and port numbers of other computers sharing this file.
 * "CT" Creation Time is the file creation time.
 */
public class Response {

    /** A debugging log we can write lines of text to as the program runs. */
    private static final Log LOG = LogFactory.getLog(Response.class);

    /** 0x1C, the byte that separates extensions in the extended area. */
    private static final byte EXT_SEPARATOR = 0x1c;

    /** 10, the maximum number of alternate file locations we'll list under "ALT" in the hit's GGEP block. */
    private static final int MAX_LOCATIONS = 10;

    /*
     * Both index and size must fit into 4 unsigned bytes; see
     * constructor for details.
     */

    /** The file index the sharing computer has assigned this shared file. */
    private final long index;

    /** The file's size in bytes. */
    private final long size;

	/** The file name as ASCII bytes. */
    private final byte[] nameBytes;

    /** The file name, like "My Song.mp3". */
    private final String name;

    /** XML metadata about the file. */
    private LimeXMLDocument document;

    /**
     * The file hash, like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ".
     * A HashSet of URN objects.
     */
    private final Set urns;

	/**
     * The data of the extended area, like "extension[0x1C]extension[0x1C]extension".
     * The first extension may be a file hash like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ".
     * The last extension may be a GGEP block.
     * There are no 0 bytes in the extended area.
	 */
    private final byte[] extBytes;

    /**
     * The cached RemoteFileDesc created from this Response.
     * toRemoteFileDesc() caches the RemoteFileDesc object it makes here.
     * This keeps it from remaking the object if you ask for the same address again.
     */
    private volatile RemoteFileDesc cachedRFD;

    /**
     * A GGEPContainer object that holds the values of the "ALT" and "CT" extensions.
     * A GGEPContainer object doesn't contain a GGEP block, but we can make one with it.
     */
    private final GGEPContainer ggepData;

	/** "kbps", we'll look for this text in the Gnotella user metadata. */
	private static final String KBPS = "kbps";

	/** "kHz", we'll look for this text in the Gnotella user metadata. */
	private static final String KHZ = "kHz";

    /**
     * Make a new Response object to represent information about a file in a query hit packet.
     * 
     * @param index The Gnutella file index the sharing computer assigned the shared file
     * @param size  The file size
     * @param name  The file name
     * @return      A new Response object
     */
    public Response(long index, long size, String name) {

        // Make a new Response object with the given data and blank defaults
		this(
            index,
            size,
            name,
            null,  // Set             urns       No HashSet of URN objects with the file hash
            null,  // LimeXMLDocument doc        No XML metadata about the file
            null,  // GGEPContainer   ggepData   No data for the "ALT" or "CT" GGEP extensions
            null); // byte[]          extensions No extended area data
    }

    /**
     * Make a new Response object to represent information about a file in a query hit packet.
     * 
     * @param index The Gnutella file index the sharing computer assigned the shared file
     * @param size  The file size
     * @param name  The file name
     * @param doc   XML metadata about the file
     * @return      A new Response object
     */
    public Response(long index, long size, String name, LimeXMLDocument doc) {

        // Make a new Response object with the given data and blank defaults
        this(
            index,
            size,
            name,
            null,  // Set           urns       No HashSet of URN objects with the file hash
            doc,
            null,  // GGEPContainer ggepData   No data for the "ALT" or "CT" GGEP extensions
            null); // byte[]        extensions We're making this Response, so we don't have an extended data area to parse
    }

	/**
     * Make a new Response object to represent information about a file in a query hit packet.
     * 
     * @param fd A FileDesc object that has information about a file we're sharing
     * @return   A Response object that represents the part of a Gnutella query hit packet
	 */
	public Response(FileDesc fd) {

        // Make a new Response object with the given data and blank defaults
		this(

            // Get the share index number, file size, file name, and hash URN from the FileDesc object
            fd.getIndex(),
            fd.getFileSize(),
            fd.getFileName(),
            fd.getUrns(),

            // No XML metadata
            null,

            // Make a new GGEPContainer object to hold the GGEP "ALT" and "CT" values
            new GGEPContainer(

                // Get the SHA1 hash from the FileDesc, and use the AltlocManager to look up alternate locations for the GGEP "ALT" extension value
                getAsEndpoints(RouterService.getAltlocManager().getDirect(fd.getSHA1Urn())), // Returns a HashSet of Endpoint objects

                // Get the file's creation time from the CreationTimeCache
			    CreationTimeCache.instance().getCreationTimeAsLong(fd.getSHA1Urn())),

            // We're making this Response, so we don't have an extended data area to parse
			null);
	}

    /**
     * Make a new Response object to represent a file hit within a Gnutella query hit packet.
     * 
     * This is the packet maker.
     * Use it to make a new query response for a packet you will send.
     * The packet parser also calls it.
     * 
     * @param index      The Gnutella file index number
     * @param size       The file size in bytes
     * @param name       The file name, like "My Song.mp3"
     * @param urns       A HashSet of URN objects with the file hash, like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ"
     * @param doc        XML metadata about the file
     * @param ggepData   A GGEPContainer object with "ALT" IP addresses and port numbers and "CT" the file creation time
     * @param extensions The data of the extended area, like "extension[0x1C]extension[0x1C]extension"
     * @return           A new Response object with all that information about the file hit
     */
    private Response(long index, long size, String name, Set urns, LimeXMLDocument doc, GGEPContainer ggepData, byte[] extensions) {

        // Make sure the index and size will fit into 4 byte Java int variables
        if ((index & 0xFFFFFFFF00000000L) != 0)   throw new IllegalArgumentException("invalid index: " + index);
        if (size > Integer.MAX_VALUE || size < 0) throw new IllegalArgumentException("invalid size: "  + size);

        // Save them in this new object
        this.index = index;
        this.size  = size;

        // Save the file name as a String
		if (name == null) this.name = ""; // Save a blank String instead of null
		else              this.name = name;

        // Save the file name as a byte array of ASCII characters
        byte[] temp = null;
        try {
            temp = this.name.getBytes("UTF-8"); // UTF-8 is ASCII encoding
        } catch (UnsupportedEncodingException namex) {
            /*
             * b/c this should never happen, we will show and error
             * if it ever does for some reason.
             */
            ErrorService.error(namex);
        }
        this.nameBytes = temp;

        // Save the HashSet of URN objects with the file hash, like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ"
		if (urns == null) this.urns = Collections.EMPTY_SET; // Save our cached empty Set instead of null
		else              this.urns = Collections.unmodifiableSet(urns);

        // Save the GGEPContainer object with "ALT" IP addresses and port numbers and "CT" the file creation time
        if (ggepData == null) this.ggepData = GGEPContainer.EMPTY; // Save our cached empty GGEPContainer instead of null
        else                  this.ggepData = ggepData;

        // Save the given extended area data, or compose some from the URNs and "ALT" and "CT" values we just saved
		if (extensions != null) this.extBytes = extensions;
		else                    this.extBytes = createExtBytes(this.urns, this.ggepData); // Generate an extended area if the caller didn't give us one

        // Save the XML metadata
		this.document = doc;
    }

    /**
     * Read a file hit section of a Gnutella query hit packet, and parse the data into a new Response object.
     * 
     * This is the packet parser.
     * It does some parsing, and then calls the packet maker to actually create the new Response object.
     * 
     * @param is The InputStream object we can call is.read() on to get the next byte of the response data
     * @return   A new Response object that represents the query hit result
     */
    public static Response createFromStream(InputStream is) throws IOException {

        // Read the file index and size from the start of the hit
        long index = ByteOrder.uint2long(ByteOrder.leb2int(is)); // Reads the 4 byte file index
        long size  = ByteOrder.uint2long(ByteOrder.leb2int(is)); // Reads the 4 byte file size
        
        /*
         * must use Integer.MAX_VALUE instead of mask because
         * this value is later converted to an int, so we want
         * to ensure that when it's converted it doesn't become
         * negative.
         */

        // Make sure the index and size will fit into 4 byte Java int variables
        if ((index & 0xFFFFFFFF00000000L) != 0)   throw new IOException("invalid index: " + index);
        if (size > Integer.MAX_VALUE || size < 0) throw new IOException("invalid size: "  + size);

        /*
         * The file name is terminated by a null terminator.
         * A second null indicates the end of this response.
         * Gnotella & others insert meta-information between
         * these null characters.  So we have to handle this.
         * See http://gnutelladev.wego.com/go/wego.discussion.message?groupId=139406&view=message&curMsgId=319258&discId=140845&index=-1&action=view
         */

        // Read the file name
        ByteArrayOutputStream baos = new ByteArrayOutputStream();                // Make a ByteArrayOutputStream that will grow to hold the data we write to it
        int c;                                                                   // Each byte we read will be stored in this int c
        while ((c = is.read()) != 0) {                                           // Loop until we read the null terminator
            if (c == -1) throw new IOException("EOF before null termination");   // We reached the end of the given InputStream
            baos.write(c);                                                       // Write this character of the file name to the ByteArrayOutputStream
        }
        String name = new String(baos.toByteArray(), "UTF-8");                   // Convert it into a String, UTF-8 is ASCII
        if (name.length() == 0) throw new IOException("empty name in response"); // Make sure we got some text

        // Read the binary data of the extended area, which doesn't include a 0 byte and ends with one
        baos.reset();                                                            // Mark the ByteArrayOutputStream empty to use it again
        while ((c = is.read()) != 0) {                                           // Copy bytes until we read the null, don't copy it over, and leave the loop
            if (c == -1) throw new IOException("EOF before null termination");   // We reached the end of the given InputStream
            baos.write(c);                                                       // Copy the byte from in to baos
        }
        byte[] rawMeta = baos.toByteArray();                                     // Get the data we read as a byte array

        // There was no data before the second 0 byte
        if (rawMeta == null || rawMeta.length == 0) {

            // Make sure the InputStream still has 16 bytes of GUID left
			if (is.available() < 16) throw new IOException("not enough room for the GUID");

            // Make a new Response object from the file index number, file size, and file name we read
            return new Response(index, size, name); // Leads to the packet maker

        // We read the data of the extended area, like "extension[0x1C]extension[0x1C]extension"
        } else {

            /*
             * now handle between-the-nulls
             * \u001c is the HUGE v0.93 GEM delimiter
             */

            // Have the HUGEExtension constructor parse the extended area data into individual text URNs and the GGEP block
            HUGEExtension huge = new HUGEExtension(rawMeta);

            // Get the hash URN extension, made from text like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ"
			Set urns = huge.getURNS(); // A HashSet of URN objects

            // Parse the Gnotella text like "128 Kbps 44 kHz 3:12" for the bit rate and song length
			LimeXMLDocument doc = null;
            Iterator iter = huge.getMiscBlocks().iterator();
            while (iter.hasNext() && doc == null) doc = createXmlDocument(name, (String)iter.next()); // Returns the name, bit rate, and length in XML

            // Parse the "ALT" and "CT" GGEP extension header values into a GGEPContainer object
			GGEPContainer ggep = GGEPUtil.getGGEP(huge.getGGEP()); // A GGEPContainer object does not contain a GGEP block

            // Make a new Response object from the file index number, size, name, hash, XML metadata, and "ALT" and "CT" values
			return new Response(index, size, name, urns, doc, ggep, rawMeta); // Leads to the packet maker
        }
    }

	/**
     * Reads the bit rate and length from Gnotella user metadata like "128 Kbps 44 kHz 3:12", and returns the information in a new XML document.
     * 
     * @param name File name text like "Artist Name - Song Name.mp3".
     * @param ext  Text for the user like "128 Kbps 44 kHz 3:12".
     * @return     A LimeXMLDocument with the file name, bit rate 128, and length 3:12.
     *             If we couldn't figure out the text format, returns null.
	 */
	private static LimeXMLDocument createXmlDocument(String name, String ext) {

        // Split the user text like "128 Kbps 44 kHz 3:12" around spaces
        StringTokenizer tok = new StringTokenizer(ext);
		if (tok.countTokens() < 2) return null; // Make sure there are 2 or more parts

        // Grab the first and second words, like "128" and "Kbps", and lowercase them
		String first  = tok.nextToken();
		String second = tok.nextToken();
		if (first  != null) first  = first.toLowerCase();
		if (second != null) second = second.toLowerCase();

        // We'll set the song length and bit rate here when we find them
		String length  = "";
		String bitrate = "";

        // We'll set these flags to true when we identify what pattern the given text is using
		boolean bearShare1 = false;        
		boolean bearShare2 = false;
		boolean gnotella   = false;

        // Look for "Kbps" to determine how the information is formatted
		if (second.startsWith(KBPS)) bearShare1 = true; // The words are like "128" and "Kbps", BearShare's first format
		else if (first.endsWith(KBPS)) bearShare2 = true; // The words are like "128Kbps" and "44kHz", BearShare's second format

        // BearShare's first formatting, like "128" and "Kbps"
        if (bearShare1) {

            // Save the first word, "128", the bit rate
			bitrate = first;

        // BearShare's second formatting, like "128Kbps" and "44kHz"
		} else if (bearShare2) {

            // Clip out the "128" from "128Kbps"
			int j = first.indexOf(KBPS);
			bitrate = first.substring(0, j);
		}

        // If we identified either of these kinds of formatting
		if (bearShare1 || bearShare2) {

            // Loop through the remaining words until length is set to the last one, like "3:12"
			while (tok.hasMoreTokens()) length = tok.nextToken();

        // The text is Gnotella formatted, it ends with "kHz"
		} else if (ext.endsWith(KHZ)) {

            // We've identified Gnotella formatting
			gnotella = true;

            // The song length is the first word
			length = first;

            // Clip the bitrate from the second word
			int i = second.indexOf(KBPS);
			if (i > -1) bitrate  = second.substring(0, i); // Found the bitrate
			else        gnotella = false;                  // Not Gnotella formatting after all, some other format we don't recognize
		}

        // Make sure the words we parsed are actually numbers
		try {

            // Call parseInt not to get the number, just to see if it causes an exception
		    Integer.parseInt(bitrate);
		    Integer.parseInt(length);

            /*
             * TODO:kfaaborg If length is like "3:12", won't parseInt not be able to deal with that?
             */

        // They aren't, the text extension could be garbage or spam, return null
		} catch (NumberFormatException nfe) { return null; }

        // If we identified a pattern we understood
		if (bearShare1 || bearShare2 || gnotella) {

            // Format the name, bit rate, and song length into a new LimeXMLDocument, and return it
		    List values = new ArrayList(3);
		    values.add(new NameValue("audios__audio__title__", name));
		    values.add(new NameValue("audios__audio__bitrate__", bitrate));
		    values.add(new NameValue("audios__audio__seconds__", length));
		    return new LimeXMLDocument(values, AudioMetaData.schemaURI);
		}

        // Unable to parse text
		return null;
	}

    /**
     * Compose the data of an extended area with the given hash URNs and GGEP "ALT" and "CT" values.
     * The GGEPContainer object just contains values for the "ALT" and "CT" extensions.
     * We'll make a new GGEP block, add those extensions, add their values, and serialize it into the data we compose and return.
     * 
     * @param urns A HashSet of URN objects with the file hash, like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ"
     * @param ggep A GGEPContainer object with the values for the "ALT" and "CT" extensions
     * @return     A byte array with the data of the extended area, like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ[0x1C]GGEP block with ALT and CT"
	 */
	private static byte[] createExtBytes(Set urns, GGEPContainer ggep) {

        try {

            // If we don't have a hash URN or GGEP values, the extended are can be no bytes
            if (isEmpty(urns) && ggep.isEmpty()) return DataUtils.EMPTY_BYTE_ARRAY;

            // Make a ByteArrayOutputStream that will grow to hold the data we write to it
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // We have hash URNs
            if (!isEmpty(urns)) {

                // Loop through them
    			Iterator iter = urns.iterator();
    			while (iter.hasNext()) {
    				URN urn = (URN)iter.next();
                    Assert.that(urn != null, "Null URN");

                    // Write ASCII byte characters like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ"
    				baos.write(urn.toString().getBytes());

    				// If there's another URN, add the separating 0x1C byte
    				if (iter.hasNext()) baos.write(EXT_SEPARATOR);
    			}

    			// If there's ggep data, write the separator
    		    if (!ggep.isEmpty()) baos.write(EXT_SEPARATOR);
            }

            /*
             * It is imperitive that GGEP is added LAST.
             * That is because GGEP can contain 0x1c (EXT_SEPARATOR)
             * within it, which would cause parsing problems
             * otherwise.
             */

            // If we were given "ALT" or "CT" values, make a GGEP block with those extensions and serialize it to baos
            if (!ggep.isEmpty()) GGEPUtil.addGGEP(baos, ggep);

            // Return the extended area we composed
            return baos.toByteArray();

        // There was a problem writing to our own ByteArrayOutputStream
        } catch (IOException impossible) {

            // Let the ErrorService look at it, and return an empty byte array
            ErrorService.error(impossible);
            return DataUtils.EMPTY_BYTE_ARRAY;
        }
    }

    /**
     * Determine if a Set has any elements, or is empty.
     * 
     * @param set An object that implements the Set interface
     * @return    True if set is null or empty
     */
    private static boolean isEmpty(Set set) {

        // If set is null or empty, it has no elements
        return set == null || set.isEmpty();
    }

    /**
     * Turn an AlternateLocationCollection into a HashSet of Endpoint objects.
     * Leaves behind the firewalled locations.
     * Marks the locations we give out with the time right now.
     * 
     * @param col An AlternateLocationCollection from AltLocManager.getDirect()
     * @return    A HashSet of Endpoint objects with all the IP addresses and port numbers
     */
    private static Set getAsEndpoints(AlternateLocationCollection col) {

        // If we weren't given any locations, return the cached empty Set
        if (col == null || !col.hasAlternateLocations()) return Collections.EMPTY_SET;

        // We'll mark the alternate locations we send with the time now
        long now = System.currentTimeMillis();

        // Make sure we're the only thread accessing the AlternateLocationCollection right now
        synchronized (col) {

            // Loop through the first 10 locations 
            Set endpoints = null;
            int i = 0;
            for (Iterator iter = col.iterator(); iter.hasNext() && i < MAX_LOCATIONS; ) {

                // Get this DirectAltLoc
                Object o = iter.next();
            	if (!(o instanceof DirectAltLoc)) continue;
                DirectAltLoc al = (DirectAltLoc)o;

                // We're allowed to send this alternate location in a download mesh response
                if (al.canBeSent(AlternateLocation.MESH_RESPONSE)) {

                    // Get the IP address and port number, and make sure it's not our IP address
                    IpPort host = al.getHost();
                    if (!NetworkUtils.isMe(host)) {

                        // Make an Endpoint out of it and add it to the endpoints HashSet
                        if (endpoints == null) endpoints = new HashSet();
                        if (!(host instanceof Endpoint)) host = new Endpoint(host.getAddress(), host.getPort());
                        endpoints.add(host);

                        // Count that we added another address
                        i++;

                        // Record that we told another computer about this alternate location right now in a download mesh response
                        al.send(now, AlternateLocation.MESH_RESPONSE);
                    }

                // We're not allowed to tell anyone about this alternate location, remove it
                } else if (!al.canBeSentAny()) { iter.remove(); }
            }

            // Return the HashSet of Endpoint objects we made, or the empty set if we didn't have any
            return endpoints == null ? Collections.EMPTY_SET : endpoints;
        }
    }

    /**
     * Serialize the data of this file hit to a given OutputStream.
     * A QueryReply constructor calls this when it's writing the packet.
     * It's already written the header and payload, and now it needs to write the parts about each file.
     * 
     * @param os An OutputStream object we can call os.write(b) on to send it data
     */
    public void writeToStream(OutputStream os) throws IOException {

        /*
         * A result has the following binary structure:
         * 
         * IIII
         * SSSS
         * File Name.mp3\0
         * extension[0x1C]extension[0x1C]extension\0
         * 
         * There are 2 int numbers at the start, the shared file index, and the file size.
         * The file name text is null terminated.
         * After that is the extended area, which is also null terminated.
         */

        // Write the shared index number and file size, which take 4 bytes each
        ByteOrder.int2leb((int)index, os);
        ByteOrder.int2leb((int)size, os);

        // Write the null terminated file name
        for (int i = 0; i < nameBytes.length; i++) os.write(nameBytes[i]);
        os.write(0);

        // Write the null terminated extended area
        for (int i = 0; i < extBytes.length; i++) os.write(extBytes[i]);
        os.write(0);
    }

    /**
     * Load this Response object with XML metadata.
     * We'll include it as a text extension in the extended area beyond the file name when we write the query hit packet.
     * 
     * @param doc XML about this shared file we're describing in a query hit packet
     */
    public void setDocument(LimeXMLDocument doc) {

        // Save the object
        document = doc;
	}

    /**
     * Find out how many bytes this Response object will take in the serialized query hit packet.
     * This is how much data writeToStream(OutputStream) will write.
     * 
     * @return The size in bytes
     */
    public int getLength() {

        // Total the size
		return
            8 +                    // Index and size numbers
		    nameBytes.length + 1 + // File name characters and null terminator
		    extBytes.length  + 1;  // Extended area bytes and null terminator
    }

	/**
     * The file index the compuer that is sharing this file has assigned it.
     * This method returns a long, but the index number is in 4 bytes in the packet.
     * 
     * @return The index number
	 */
    public long getIndex() {

        // Return the value we parsed or saved
        return index;
    }

	/**
     * The file's size in bytes.
     * This method returns a long, but the index number is in 4 bytes in the packet.
     * 
     * @return The size
     */
    public long getSize() {

        // Return the value we parsed or saved
        return size;
    }

	/**
     * The file name, like "My Song.mp3".
     * 
     * @return The file name
	 */
    public String getName() {

        // Return the value we parsed or saved
        return name;
    }

    /**
     * XML metadata about the file.
     * 
     * @return The XML as a LimeXMLDocument object
     */
    public LimeXMLDocument getDocument() {

        // Return the value we parsed or saved
        return document;
    }

	/**
     * The file hash, like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ".
     * 
     * @return A HashSet of URN objects
     */
    public Set getUrns() {

        // Return the value we parsed or saved
		return urns;
    }

    /**
     * The IP addresses and port numbers of other computers that are sharing this file.
     * This is the value of the "ALT" Alternate Locations GGEP extension.
     * 
     * @return A HashSet of Endpoint objects
     */
    public Set getLocations() {

        // Return the value we parsed or saved
        return ggepData.locations;
    }

    /**
     * The file creation time.
     * This is the value of the GGEP "CT" Creation Time header.
     * 
     * Stored here, it's a number of milliseconds since 1970.
     * In GGEP, "CT" is the number of seconds since 1970.
     * 
     * @return The file creation time, or -1 if unknown
     */
    public long getCreateTime() {

        // Return the value we parsed or saved
        return ggepData.createTime;
    }

    /**
     * The data of the extended area, like "extension[0x1C]extension[0x1C]extension".
     * 
     * The first extension may be a file hash like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ".
     * The last extension may be a GGEP block.
     * There are no 0 bytes in the extended area, and the extensions are separated by 0x1C.
     * 
     * @return A byte array with the data of the extended area
     */
    byte[] getExtBytes() {

        // Return the value we parsed or saved
        return extBytes;
    }

    /**
     * Returns this Response as a RemoteFileDesc object. (do)
     * 
     * @param data A HostData object with more information
     * @return     A RemoteFileDesc object with information from this Response object and the given HostData object
     */
    public RemoteFileDesc toRemoteFileDesc(HostData data) {

        // We've already done this for this IP address and port number
        if (cachedRFD != null && cachedRFD.getPort() == data.getPort() && cachedRFD.getHost().equals(data.getIP())) {

            // Return the RemoteFileDesc we cached
            return cachedRFD;

        // This is a different address
        } else {

            // Make a new RemoteFileDesc object from this Response object and the given HostData object
            RemoteFileDesc rfd = new RemoteFileDesc(
                data.getIP(),
                data.getPort(),
                getIndex(),
                getName(),
                (int)getSize(),
                data.getClientGUID(),
                data.getSpeed(),
                data.isChatEnabled(),
                data.getQuality(),
                data.isBrowseHostEnabled(),
                getDocument(),
                getUrns(),
                data.isReplyToMulticastQuery(),
                data.isFirewalled(), 
                data.getVendorCode(),
                System.currentTimeMillis(),
                data.getPushProxies(),
                getCreateTime(),
                data.getFWTVersionSupported());

            // Cache it and return it
            cachedRFD = rfd;
            return rfd;
        }
    }

	/**
     * Determine if a given Response is the same as this one.
     * Compares the index number, size, file name, XML, and URNs only.
     * 
     * @param o Another Response object to compare this one to
     * @return  True if they are the same, false if they are different
	 */
    public boolean equals(Object o) {

        // If o is this, same, if o isn't a Response, different
		if (o == this) return true;
        if (!(o instanceof Response)) return false;

        // Look at o as a Response object, and return true if all the information is the same as this one
        Response r = (Response)o;
		return
            getIndex() == r.getIndex() &&
            getSize() == r.getSize() &&
			getName().equals(r.getName()) &&
            ((getDocument() == null) ? (r.getDocument() == null) : getDocument().equals(r.getDocument())) &&
            getUrns().equals(r.getUrns());
    }

    /**
     * Turn the information in this Response object into a hash code.
     * 
     * @return The hash code
     */
    public int hashCode() {
        
        /*
         * Good enough for the moment
         * TODO:: IMPROVE THIS HASHCODE!!
         */

        // Compute a hash code from the name String and size and index numbers
        return getName().hashCode() + (int)getSize() + (int)getIndex();
    }

	/**
     * Express this Response object as text.
	 * Overrides Object.toString() to print out a more informative message.
     * 
     * @return A String with several lines that has the index, size, name, XML, and hash URNs
	 */
	public String toString() {

        // Compose and return the String
		return ("index:        " + index    + "\r\n" +
				"size:         " + size     + "\r\n" +
				"name:         " + name     + "\r\n" +
				"xml document: " + document + "\r\n" +
				"urns:         " + urns);
	}

    /**
     * A class that contains static methods that compose and parse "ALT" and "CT" in GGEP blocks.
     */
    private static class GGEPUtil {

        /** Mark the constructor private so no one can make a GGEPUtil object. */
        private GGEPUtil() {}

        /**
         * Make a GGEP block with "ALT" and "CT" extensions, and serialize it to the given OutputStream.
         * 
         * @param out  An OutputStream we can call out.write(b) on to give it serialized data we're making
         * @param ggep A GGEPContainer object that has values for "ALT" and "CT" extensions
         */
        static void addGGEP(OutputStream out, GGEPContainer ggep) throws IOException {

            // Make sure we actually got a GGEPContainer with some information
            if (ggep == null || (ggep.locations.size() == 0 && ggep.createTime <= 0)) throw new NullPointerException("null or empty locations");

            // Make a new GGEP block
            GGEP info = new GGEP();

            // We have "ALT" locations
            if (ggep.locations.size() > 0) {

                // We'll compose the "ALT" value in this new ByteArrayOutputStream
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                try {

                    // Loop through the alternate locations
                    for (Iterator i = ggep.locations.iterator(); i.hasNext();) {

                        try {

                            // Write the IP address and port number into 6 bytes of "ALT" value data
                            Endpoint ep = (Endpoint)i.next();
                            baos.write(ep.getHostBytes());
                            ByteOrder.short2leb((short)ep.getPort(), baos);

                        } catch (UnknownHostException uhe) { continue; }
                    }

                } catch (IOException impossible) { ErrorService.error(impossible); }

                // Add the "ALT" extension to our new GGEP block with the value we just composed
                info.put(GGEP.GGEP_HEADER_ALTS, baos.toByteArray());
            }

            // If the GGEPContainer has a file creation time, add it under the "CT" extension
            if (ggep.createTime > 0) info.put(GGEP.GGEP_HEADER_CREATE_TIME, ggep.createTime / 1000); // Convert from millseconds to seconds

            // Serialize the GGEP block into the given OutputStream
            info.write(out);
        }

        /**
         * Parse the "ALT" and "CT" extensions from a GGEP block, returning their values wrapped in a GGEPContainer object.
         * 
         * Looks in the GGEP block for the "ALT" and "CT" extensions.
         * "ALT" is Alternate Locations, more IP addresses and port numbers we can try to connect to.
         * "CT" is Creation Time, the time the file was made.
         * 
         * If the GGEP block has either of these values, it reads them and parses them into a GGEPContainer object.
         * A GGEPContainer object doesn't contain a GGEP block.
         * Rather, it contains information from the "ALT" and "CT" extensions.
         * 
         * @param ggep A GGEP object that represents a GGEP block
         * @return     A new GGEPContainer object with the values of the "ALT" and "CT" extensions
         */
        static GGEPContainer getGGEP(GGEP ggep) {

            // If the caller didn't give us a GGEP block, return our cached empty GGEPContainer object
            if (ggep == null) return GGEPContainer.EMPTY;

            // Variables for the "ALT" and "CT" GGEP extension header values we'll try to read
            Set locations = null;
            long createTime = -1;

            /*
             * if the block has a ALTS value, get it, parse it,
             * and move to the next.
             */

            // The given GGEP block has the extension "ALT" Alternate Locations, the addresses of Gnutella computers we can try to connect to
            if (ggep.hasKey(GGEP.GGEP_HEADER_ALTS)) {

                // Parse the "ALT" value, which is IP addresses and port numbers in 6 byte chunks, into a HashSet of Endpoint objects called locations
                try {
                    locations = parseLocations(ggep.getBytes(GGEP.GGEP_HEADER_ALTS));
                } catch (BadGGEPPropertyException bad) {}
            }

            // The given GGEP block has the extension "CT" Creation Time, the time the file was made
            if (ggep.hasKey(GGEP.GGEP_HEADER_CREATE_TIME)) {

                // Get the "CT" value, which is the number of seconds since 1970, and convert it into milliseconds
                try {
                    createTime = ggep.getLong(GGEP.GGEP_HEADER_CREATE_TIME) * 1000; // Convert from seconds to milliseconds 
                } catch (BadGGEPPropertyException bad) {}
            }

            // Return the alternate locations and creation time in a GGEPContainer object
            return
                (locations == null && createTime == -1) ? // If the given GGEP block didn't have "ALT" or "CT"
                GGEPContainer.EMPTY :                     // Return our cached empty GGEPContainer object
                new GGEPContainer(locations, createTime); // If the given block has "ALT" or "CT", wrap just those values into a new GGEPContainer and return it
        }

        /**
         * Turn a byte array of IP addresses and port numbers in 6 byte chunks into a HashSet of Endpoint objects.
         * 
         * @param locBytes A byte array with IP addresses and port numbers in 6 byte chunks
         * @return         A HashSet of Endpoint objects with the IP addresses and port numbers
         */
        private static Set parseLocations(byte[] locBytes) {

            // Make a reference for the HashSet we'll fill with Endpoint objects and return
            Set locations = null;

            // Get access to our list of government and institutional IP addresses we won't contact
            IPFilter ipFilter = IPFilter.instance();

            // Only do something if the given data is a multiple of 6 bytes long
            if (locBytes.length % 6 == 0) {

                // Loop for each 6 byte chunk
                for (int j = 0; j < locBytes.length; j += 6) {

                    // Read the port number
                    int port = ByteOrder.ushort2int(ByteOrder.leb2short(locBytes, j + 4));
                    if (!NetworkUtils.isValidPort(port)) continue; // If it's 0, move to the next 6 byte chunk

                    // Read the IP address
                    byte[] ip = new byte[4];
                    ip[0] = locBytes[j];
                    ip[1] = locBytes[j + 1];
                    ip[2] = locBytes[j + 2];
                    ip[3] = locBytes[j + 3];

                    // If the IP address looks bad, move on to the next 6 byte chunk
                    if (!NetworkUtils.isValidAddress(ip) || // Make sure the IP address doesn't start 0 or 255
                        !ipFilter.allow(ip) ||              // Make sure the IP address isn't on our list of addresses to avoid
                        NetworkUtils.isMe(ip, port))        // Make sure this isn't our own IP address
                        continue;

                    // Keep the IP address and port number together in an Endpoint and add it to the locations HashSet
                    if (locations == null) locations = new HashSet(); // If we haven't made the HashSet yet, make it now
                    locations.add(new Endpoint(ip, port));
                }
            }

            // Return the locations HashSet of Endpoint objects we made
            return locations;
        }
    }

    /**
     * A container for the information we're adding to and reading from GGEP blocks.
     * 
     * A GGEPContainer doesn't contain a GGEP block.
     * Rather, it contains the values of the "ALT" and "CT" extensions.
     * "ALT" is Alternate Locations, here it's more IP addresses and port numbers of computers that have the file.
     * "CT" is Creation Time, the time the file was made.
     */
    static final class GGEPContainer {

        /** A HashSet of Endpoint objects with the IP address and port numbers of the "ALT" Alternate Locations extension. */
        final Set locations;

        /**
         * The file creation time.
         * This is the value of the GGEP "CT" Creation Time header.
         * 
         * In this GGEPContainer object, it's a number of milliseconds since 1970.
         * In GGEP, "CT" is the number of seconds since 1970.
         * 
         * If this GGEPContainer object isn't holding a creation time, createTime will be -1.
         */
        final long createTime;

        /** A cached empty GGEPContainer object to reference instead of repeatedly making a new empty one. */
        private static final GGEPContainer EMPTY = new GGEPContainer();

        /**
         * Make a new empty GGEPContainer.
         */
        private GGEPContainer() {

            // Call the next constructor, giving it no HashSet of Endpoints and -1 instead of a file creation time
            this(null, -1);
        }

        /**
         * Make a new GGEPContainer object to hold a HashSet of "ALT" Endpoints and a "CT" file creation time.
         * 
         * @param locs   A HashSet of Endpoint objects with the IP addresses and port numbers of the "ALT" extension
         * @param create The number of milliseconds since 1970 that the file was made for the "CT" extension
         */
        GGEPContainer(Set locs, long create) {

            // Save the given values in this object
            locations = locs == null ? Collections.EMPTY_SET : locs; // If locs is null, reference the cached empty Set instead
            createTime = create;
        }

        /**
         * Determine if this GGEPContainer object has any "ALT" or "CT" information in it.
         * 
         * @return False if it has an IP address and port number or a creation time, true if it is totally empty
         */
        boolean isEmpty() {

            // Return true if there is no data for "ALT" or "CT"
            return locations.isEmpty() && createTime <= 0; // If we don't have a creation time, createTime will be -1
        }
    }
}
