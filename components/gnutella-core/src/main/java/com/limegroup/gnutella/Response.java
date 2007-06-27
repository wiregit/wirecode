package com.limegroup.gnutella;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.limewire.collection.BitNumbers;
import org.limewire.collection.NameValue;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IPPortCombo;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkUtils;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.messages.HUGEExtension;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;


/**
 * A single result from a query reply message.  (In hindsight, "Result" would
 * have been a better name.)  Besides basic file information, responses can
 * include metadata.
 *
 * Response was originally intended to be immutable, but it currently includes
 * mutator methods for metadata; these will be removed in the future.  
 */
public class Response {
    
    //private static final Log LOG = LogFactory.getLog(Response.class);
    
    /**
     * The magic byte to use as extension separators.
     */
    private static final byte EXT_SEPARATOR = 0x1c;
    
    /**
     * The maximum number of alternate locations to include in responses
     * in the GGEP block
     */
    private static final int MAX_LOCATIONS = 10;
    
    /** Both index and size must fit into 4 unsigned bytes; see
     *  constructor for details. */
    private final long index;
    private final long size;

	/**
	 * The bytes for the name string, guaranteed to be non-null.
	 */
    private final byte[] nameBytes;

    /** The name of the file matching the search.  This does NOT
     *  include the double null terminator.
     */
    private final String name;

    /** The document representing the XML in this response. */
    private LimeXMLDocument document;

    /** 
	 * The <tt>Set</tt> of <tt>URN</tt> instances for this <tt>Response</tt>,
	 * as specified in HUGE v0.94.  This is guaranteed to be non-null, 
	 * although it is often empty.
     */
    private final Set<URN> urns;

	/**
	 * The bytes between the nulls for the <tt>Response</tt>, as specified
	 * in HUGE v0.94.  This is guaranteed to be non-null, although it can be
	 * an empty array.
	 */
    private final byte[] extBytes;
    
    /**
     * The cached RemoteFileDesc created from this Response.
     */
    private volatile RemoteFileDesc cachedRFD;
    
    /**
     * The container for extra GGEP data.
     */
    private final GGEPContainer ggepData;
    
    /**
     * If this is a response for a metafile, i.e. a file
     * that itself triggers another download.
     */
    private final boolean isMetaFile;

	/**
	 * Constant for the KBPS string to avoid constructing it too many
	 * times.
	 */
	private static final String KBPS = "kbps";

	/**
	 * Constant for kHz to string to avoid excessive string construction.
	 */
	private static final String KHZ = "kHz";

    /** Creates a fresh new response.
     *
     * @requires index and size can fit in 4 unsigned bytes, i.e.,
     *  0 <= index, size < 2^32
     */
    public Response(long index, long size, String name) {
		this(index, size, name, null, null, null, null);
    }


    /**
     * Creates a new response with parsed metadata.  Typically this is used
     * to respond to query requests.
     * @param doc the metadata to include
     */
    public Response(long index, long size, String name, LimeXMLDocument doc) {
        this(index, size, name, null, doc, null, null);
    }

	/**
	 * Constructs a new <tt>Response</tt> instance from the data in the
	 * specified <tt>FileDesc</tt>.  
	 *
	 * @param fd the <tt>FileDesc</tt> containing the data to construct 
	 *  this <tt>Response</tt> -- must not be <tt>null</tt>
	 */
	public Response(FileDesc fd) {
		this(fd.getIndex(), fd.getFileSize(), fd.getFileName(), 
			 fd.getUrns(), null, 
			 new GGEPContainer(
			    getAsIpPorts(RouterService.getAltlocManager().getDirect(fd.getSHA1Urn())),
			    CreationTimeCache.instance().getCreationTimeAsLong(fd.getSHA1Urn()),
                fd.getFileSize()),
			 null);
	}

    /**
	 * Overloaded constructor that allows the creation of Responses with
     * meta-data and a <tt>Set</tt> of <tt>URN</tt> instances.  This 
	 * is the primary constructor that establishes all of the class's 
	 * invariants, does any necessary parameter validation, etc.
	 *
	 * If extensions is non-null, it is used as the extBytes instead
	 * of creating them from the urns and locations.
	 *
	 * @param index the index of the file referenced in the response
	 * @param size the size of the file (in bytes)
	 * @param name the name of the file
	 * @param urns the <tt>Set</tt> of <tt>URN</tt> instances associated
	 *  with the file
	 * @param doc the <tt>LimeXMLDocument</tt> instance associated with
	 *  the file
	 * @param endpoints a collection of other locations on this network
	 *        that will have this file
	 * @param extensions The raw unparsed extension bytes.
     */
    private Response(long index, long size, String name,
					 Set<? extends URN> urns, LimeXMLDocument doc, 
					 GGEPContainer ggepData, byte[] extensions) {
        if( (index & 0xFFFFFFFF00000000L)!=0 )
            throw new IllegalArgumentException("invalid index: " + index);
        // see note in createFromStream about Integer.MAX_VALUE
        if (size < 0 || size > Constants.MAX_FILE_SIZE)
            throw new IllegalArgumentException("invalid size: " + size);
            
        this.index=index;
        this.size=size;
        
		if (name == null)
			this.name = "";
		else 
			this.name = name;
		
		isMetaFile = name.toLowerCase().endsWith(".torrent");

        byte[] temp = null;
        try {
            temp = this.name.getBytes("UTF-8");
        } catch(UnsupportedEncodingException namex) {
            //b/c this should never happen, we will show and error
            //if it ever does for some reason.
            ErrorService.error(namex);
        }
        this.nameBytes = temp;

		if (urns == null)
			this.urns = Collections.emptySet();
		else
			this.urns = Collections.unmodifiableSet(urns);
		
        if(ggepData == null) {
            if (size <= Integer.MAX_VALUE)
                this.ggepData = GGEPContainer.EMPTY;
            else
                this.ggepData = new GGEPContainer(null,-1l, size);
        } else
		    this.ggepData = ggepData;
		
		if (extensions != null)
		    this.extBytes = extensions;
		else 
		    this.extBytes = createExtBytes(this.urns, this.ggepData);

		this.document = doc;
    }
  
    /**
     * Factory method for instantiating individual responses from an
	 * <tt>InputStream</tt> instance.
	 * 
	 * @param is the <tt>InputStream</tt> to read from
	 * @throws <tt>IOException</tt> if there are any problems reading from
	 *  or writing to the stream
     */
    public static Response createFromStream(InputStream is) 
		throws IOException {
        // extract file index & size
        long index=ByteOrder.uint2long(ByteOrder.leb2int(is));
        long size=ByteOrder.uint2long(ByteOrder.leb2int(is));
        
        if( (index & 0xFFFFFFFF00000000L)!=0 )
            throw new IOException("invalid index: " + index);
        if (size < 0)
            throw new IOException("invalid size: " + size);        

        //The file name is terminated by a null terminator.  
        // A second null indicates the end of this response.
        // Gnotella & others insert meta-information between
        // these null characters.  So we have to handle this.
        // See http://gnutelladev.wego.com/go/
        //         wego.discussion.message?groupId=139406&
        //         view=message&curMsgId=319258&discId=140845&
        //         index=-1&action=view

        // Extract the filename
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int c;
        while((c=is.read())!=0) {
            if(c == -1)
                throw new IOException("EOF before null termination");
            baos.write(c);
        }
        String name = new String(baos.toByteArray(), "UTF-8");
        checkFilename(name); // throws IOException

        // Extract extra info, if any
        baos.reset();
        while((c=is.read())!=0) {
            if(c == -1)
                throw new IOException("EOF before null termination");
            baos.write(c);
        }
        byte[] rawMeta = baos.toByteArray();
        if(rawMeta.length == 0) {
			if(is.available() < 16) {
				throw new IOException("not enough room for the GUID");
			}
            return new Response(index,size,name);
        } else {
			// now handle between-the-nulls
			// \u001c is the HUGE v0.93 GEM delimiter
            HUGEExtension huge = new HUGEExtension(rawMeta);

			Set<URN> urns = huge.getURNS();

			LimeXMLDocument doc = null;
            for(String next : huge.getMiscBlocks()) {
                doc = createXmlDocument(name, next);
                if(doc != null)
                    break;
            }

			GGEPContainer ggep = GGEPUtil.getGGEP(huge.getGGEP());
            if (ggep.size64 > Constants.MAX_FILE_SIZE)
                throw new IOException(" file too large "+ggep.size64);
            if (ggep.size64 > Integer.MAX_VALUE)
                size = ggep.size64;

			return new Response(index, size, name, 
			                    urns, doc, ggep, rawMeta);
        }
    }
    
	/**
	 * Constructs an xml string from the given extension sting.
	 *
	 * @param name the name of the file to construct the string for
	 * @param ext an individual between-the-nulls string (note that there
	 *  can be multiple between-the-nulls extension strings with HUGE)
	 * @return the xml formatted string, or the empty string if the
	 *  xml could not be parsed
	 */
	private static LimeXMLDocument createXmlDocument(String name, String ext) {
		StringTokenizer tok = new StringTokenizer(ext);
		// if there aren't the expected number of tokens, simply
		// return the empty string
		if(tok.countTokens() < 2)
			return null;
		
		String first  = tok.nextToken();
		String second = tok.nextToken();
		if (first != null)
		    first = first.toLowerCase();
		if (second != null)
		    second = second.toLowerCase();
		String length="";
		String bitrate="";
		boolean bearShare1 = false;        
		boolean bearShare2 = false;
		boolean gnotella = false;
		if(second.startsWith(KBPS))
			bearShare1 = true;
		else if (first.endsWith(KBPS))
			bearShare2 = true;
		if(bearShare1){
			bitrate = first;
		}
		else if (bearShare2){
			int j = first.indexOf(KBPS);
			bitrate = first.substring(0,j);
		}
		if(bearShare1 || bearShare2){
			while(tok.hasMoreTokens())
				length=tok.nextToken();
			//OK we have the bitrate and the length
		}
		else if (ext.endsWith(KHZ)){//Gnotella
			gnotella = true;
			length=first;
			//extract the bitrate from second
			int i=second.indexOf(KBPS);
			if(i>-1)//see if we can find the bitrate                
				bitrate = second.substring(0,i);
			else//not gnotella, after all...some other format we do not know
				gnotella=false;
		}
		
		// make sure these are valid numbers.
		try {
		    Integer.parseInt(bitrate);
		    Integer.parseInt(length);
		} catch(NumberFormatException nfe) {
		    return null;
		}
		
		if(bearShare1 || bearShare2 || gnotella) {//some metadata we understand
		    List<NameValue<String>> values = new ArrayList<NameValue<String>>(3);
		    values.add(new NameValue<String>(LimeXMLNames.AUDIO_TITLE, name));
		    values.add(new NameValue<String>(LimeXMLNames.AUDIO_BITRATE, bitrate));
		    values.add(new NameValue<String>(LimeXMLNames.AUDIO_SECONDS, length));
		    return new LimeXMLDocument(values, LimeXMLNames.AUDIO_SCHEMA);
		}
		
		return null;
	}

	/**
	 * Helper method that creates an array of bytes for the specified
	 * <tt>Set</tt> of <tt>URN</tt> instances.  The bytes are written
	 * as specified in HUGE v 0.94.
	 *
	 * @param urns the <tt>Set</tt> of <tt>URN</tt> instances to use in
	 *  constructing the byte array
	 */
	private static byte[] createExtBytes(Set<? extends URN> urns, GGEPContainer ggep) {
        try {
            if( isEmpty(urns) && ggep.isEmpty())
                return DataUtils.EMPTY_BYTE_ARRAY;
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();            
            if( !isEmpty(urns) ) {
                // Add the extension for URNs, if any.
                for(Iterator<? extends URN> iter = urns.iterator(); iter.hasNext(); ) {
    				URN urn = iter.next();
                    Assert.that(urn!=null, "Null URN");
    				baos.write(urn.toString().getBytes());
    				// If there's another URN, add the separator.
    				if (iter.hasNext()) {
    					baos.write(EXT_SEPARATOR);
    				}
    			}
    			
    			// If there's ggep data, write the separator.
    		    if( !ggep.isEmpty() )
    		        baos.write(EXT_SEPARATOR);
            }
            
            // It is imperitive that GGEP is added LAST.
            // That is because GGEP can contain 0x1c (EXT_SEPARATOR)
            // within it, which would cause parsing problems
            // otherwise.
            if(!ggep.isEmpty())
                GGEPUtil.addGGEP(baos, ggep);
			
            return baos.toByteArray();
        } catch (IOException impossible) {
            ErrorService.error(impossible);
            return DataUtils.EMPTY_BYTE_ARRAY;
        }
    }
    
    /**
     * Utility method to know if a set is empty or null.
     */
    private static boolean isEmpty(Set<?> set) {
        return set == null || set.isEmpty();
    }
    
    /**
     * Utility method for converting the non-firewalled elements of an
     * AlternateLocationCollection to a smaller set of endpoints.
     */
    private static Set<? extends IpPort> getAsIpPorts(AlternateLocationCollection<DirectAltLoc> col) {
        if( col == null || !col.hasAlternateLocations() )
            return Collections.emptySet();
        
        long now = System.currentTimeMillis();
        synchronized(col) {
            Set<IpPort> endpoints = null;
            int i = 0;
            for(Iterator<DirectAltLoc> iter = col.iterator(); iter.hasNext() && i < MAX_LOCATIONS;) {
                DirectAltLoc al = iter.next();
                if (al.canBeSent(AlternateLocation.MESH_RESPONSE)) {
                    IpPort host = al.getHost();
                    if( !NetworkUtils.isMe(host) ) {
                        if (endpoints == null)
                            endpoints = new IpPortSet();
                        
                        endpoints.add(host);
                        i++;
                        al.send(now, AlternateLocation.MESH_RESPONSE);
                    }
                } else if (!al.canBeSentAny())
                    iter.remove();
            }
            if(endpoints == null)
                return Collections.emptySet();
            else
                return endpoints;
        }
    }    

    /**
     * Like writeToArray(), but writes to an OutputStream.
     */
    public void writeToStream(OutputStream os) throws IOException {
        ByteOrder.int2leb((int)index, os);
        if (size > Integer.MAX_VALUE) 
            ByteOrder.int2leb(0xFFFFFFFF, os);
        else
            ByteOrder.int2leb((int)size, os);
        for (int i = 0; i < nameBytes.length; i++)
            os.write(nameBytes[i]);
        //Write first null terminator.
        os.write(0);
        // write HUGE v0.93 General Extension Mechanism extensions
        // (currently just URNs)
        for (int i = 0; i < extBytes.length; i++)
            os.write(extBytes[i]);
        //add the second null terminator
        os.write(0);
    }

    /**
     * Sets this' metadata.
     * @param meta the parsed XML metadata 
     */	
    public void setDocument(LimeXMLDocument doc) {
        document = doc;
	}
	
    
    /**
     */
    public int getLength() {
        // must match same number of bytes writeToArray() will write
		return 8 +                   // index and size
		nameBytes.length +
		1 +                   // null
		extBytes.length +
		1;                    // final null
    }   
   

	/**
	 * Returns the index for the file stored in this <tt>Response</tt>
	 * instance.
	 *
	 * @return the index for the file stored in this <tt>Response</tt>
	 * instance
	 */
    public long getIndex() {
        return index;
    }

	/**
	 * Returns the size of the file for this <tt>Response</tt> instance
	 * (in bytes).
	 *
	 * @return the size of the file for this <tt>Response</tt> instance
	 * (in bytes)
	 */
    public long getSize() {
        return size;
    }

	/**
	 * Returns the name of the file for this response.  This is guaranteed
	 * to be non-null, but it could be the empty string.
	 *
	 * @return the name of the file for this response
	 */
    public String getName() {
        return name;
    }

    /**
     * Returns this' metadata.
     */
    public LimeXMLDocument getDocument() {
        return document;
    }

	/**
	 * Returns an immutable <tt>Set</tt> of <tt>URN</tt> instances for 
	 * this <tt>Response</tt>.
	 *
	 * @return an immutable <tt>Set</tt> of <tt>URN</tt> instances for 
	 * this <tt>Response</tt>, guaranteed to be non-null, although the
	 * set could be empty
	 */
    public Set<URN> getUrns() {
		return urns;
    }
    
    /**
     * Returns an immutabe <tt>Set</tt> of <tt>Endpoint</tt> that
     * contain the same file described in this <tt>Response</tt>.
     *
     * @return an immutabe <tt>Set</tt> of <tt>Endpoint</tt> that
     * contain the same file described in this <tt>Response</tt>,
     * guaranteed to be non-null, although the set could be empty
     */
    public Set<? extends IpPort> getLocations() {
        return ggepData.locations;
    }
    
    /**
     * Returns the create time.
     */
    public long getCreateTime() {
        return ggepData.createTime;
    }    
    
    public boolean isMetaFile() {
    	return isMetaFile;
    }
    
    byte[] getExtBytes() {
        return extBytes;
    }
    
    /**
     * Returns this Response as a RemoteFileDesc.
     */
    public RemoteFileDesc toRemoteFileDesc(HostData data){
        if(cachedRFD != null &&
           cachedRFD.getPort() == data.getPort() &&
           cachedRFD.getHost().equals(data.getIP()))
            return cachedRFD;
        else {
            RemoteFileDesc rfd = new RemoteFileDesc(
                 data.getIP(),
                 data.getPort(),
                 getIndex(),
                 getName(),
                 getSize(),
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
                 data.getPushProxies(),
                 getCreateTime(),
                 data.getFWTVersionSupported(),
                 data.isTLSCapable()
                );
            cachedRFD = rfd;
            return rfd;
        }
    }

	/**
	 * Overrides equals to check that these two responses are equal.
	 * Raw extension bytes are not checked, because they may be
	 * extensions that do not change equality, such as
	 * otherLocations.
	 */
    public boolean equals(Object o) {
		if(o == this) return true;
        if (! (o instanceof Response))
            return false;
        Response r=(Response)o;
		return getIndex() == r.getIndex() &&
               getSize() == r.getSize() &&
			   getName().equals(r.getName()) &&
               ((getDocument() == null) ? (r.getDocument() == null) :
               getDocument().equals(r.getDocument())) &&
               getUrns().equals(r.getUrns());
    }


    public int hashCode() {
        return  (int)((31 * 31 * getName().hashCode() + 31 * getSize()+getIndex()));
    }

	/**
	 * Overrides Object.toString to print out a more informative message.
	 */
	public String toString() {
		return ("index:        "+index+"\r\n"+
				"size:         "+size+"\r\n"+
				"name:         "+name+"\r\n"+
				"xml document: "+document+"\r\n"+
				"urns:         "+urns);
	}
	
    /**
     * Utility class for handling GGEP blocks in the per-file section
     * of QueryHits.
     */
    private static class GGEPUtil {
        
        /**
         * Private constructor so it never gets constructed.
         */
        private GGEPUtil() {}
        
        /**
         * Adds a GGEP block with the specified alternate locations to the 
         * output stream.
         */
        static void addGGEP(OutputStream out, GGEPContainer ggep)
          throws IOException {
            if( ggep == null || (ggep.locations.size() == 0 && 
                    ggep.createTime <= 0 && ggep.size64 <= Integer.MAX_VALUE))
                throw new NullPointerException("null or empty locations and small size");
            
            GGEP info = new GGEP(true);
            if(ggep.locations.size() > 0) {
                BitNumbers bn = new BitNumbers(ggep.locations.size());
                int i = 0;
                for(IpPort ipp : ggep.locations) {
                    if(ipp instanceof Connectable && ((Connectable)ipp).isTLSCapable())
                        bn.set(i);
                    i++;
                }
                    
                byte[] output = NetworkUtils.packIpPorts(ggep.locations);   
                info.put(GGEP.GGEP_HEADER_ALTS, output);
                if(!bn.isEmpty())
                    info.put(GGEP.GGEP_HEADER_ALTS_TLS, bn.toByteArray());
            }
            
            if(ggep.createTime > 0)
                info.put(GGEP.GGEP_HEADER_CREATE_TIME, ggep.createTime / 1000);
            
            if (ggep.size64 > Integer.MAX_VALUE && ggep.size64 <= Constants.MAX_FILE_SIZE)
                info.put(GGEP.GGEP_HEADER_LARGE_FILE, ggep.size64);
            
            info.write(out);
        }
        
        /**
         * Returns a <tt>Set</tt> of other endpoints described
         * in one of the GGEP arrays.
         */
        static GGEPContainer getGGEP(GGEP ggep) {
            if (ggep == null)
                return GGEPContainer.EMPTY;

            Set<? extends IpPort> locations = null;
            long createTime = -1;
            long size64 = 0;
            
            // if the block has a ALTS value, get it, parse it,
            // and move to the next.
            if (ggep.hasKey(GGEP.GGEP_HEADER_ALTS)) {
                byte[] tlsData = null;
                if(ggep.hasKey(GGEP.GGEP_HEADER_ALTS_TLS)) {
                    try {
                        tlsData = ggep.getBytes(GGEP.GGEP_HEADER_ALTS_TLS);
                    } catch(BadGGEPPropertyException ignored) {}
                }
                BitNumbers bn = tlsData == null ? null : new BitNumbers(tlsData);
                try {
                    locations = parseLocations(bn, ggep.getBytes(GGEP.GGEP_HEADER_ALTS));
                } catch (BadGGEPPropertyException bad) {}
            }
            
            if(ggep.hasKey(GGEP.GGEP_HEADER_CREATE_TIME)) {
                try {
                    createTime = ggep.getLong(GGEP.GGEP_HEADER_CREATE_TIME) * 1000;
                } catch(BadGGEPPropertyException bad) {}
            }
            
            if (ggep.hasKey(GGEP.GGEP_HEADER_LARGE_FILE)) {
                try {
                    size64 = ggep.getLong(GGEP.GGEP_HEADER_LARGE_FILE);
                } catch(BadGGEPPropertyException bad) {}
            }
            
            return (locations == null && createTime == -1 && size64 <= Integer.MAX_VALUE) ?
                GGEPContainer.EMPTY : new GGEPContainer(locations, createTime, size64);
        }
        
        /**
         * Returns a set of IpPorts corresponding to the IpPorts in data.
         * If BitNumbers is non-null, the addresses in the index corresponding
         * to any 'on' indexes in BitNumbers are considered tlsCapable.
         * Whenever an invalid address is encountered, all further hosts
         * are prevented from being TLS capable.
         */
        private static Set<? extends IpPort> parseLocations(BitNumbers tlsHosts, byte[] data) {
            Set<IpPort> locations = null;
            
            if (data.length % 6 != 0)
                return null;
            
            int size = data.length/6;
            byte [] current = new byte[6];
            for (int i=0;i<size;i++) {
                System.arraycopy(data,i*6,current,0,6);
                IpPort ipp;
                try {
                    ipp = IPPortCombo.getCombo(current);
                } catch(InvalidDataException ide) {
                    tlsHosts = null; // turn off TLS
                    continue;
                }
                
                // if we're me or banned, ignore.
                if(!RouterService.getIpFilter().allow(ipp) || NetworkUtils.isMe(ipp))
                    continue;
                
                if(locations == null)
                    locations = new IpPortSet();
                
                // if this addr was TLS-capable, mark it as such.
                if(tlsHosts != null && tlsHosts.isSet(i))
                    ipp = new ConnectableImpl(ipp, true);
                
                locations.add(ipp);
            }
            return locations;
        }
    }
    
    
    private final static void checkFilename(String name) throws IOException {
        if (name.length() == 0) {
            throw new IOException("empty name in response");
        }
        // sanity checks for filename 
        if (name.indexOf('/') != -1 || name.indexOf('\n') != -1 || name.indexOf('\r') != -1) {
            throw new IOException("Illegal filename " + name + "contains one of [/\\n\\r]");
        }
    }
    
    /**
     * A container for information we're putting in/out of GGEP blocks.
     */
    static final class GGEPContainer {
        final Set<? extends IpPort> locations;
        final long createTime;
        final long size64;
        private static final GGEPContainer EMPTY = new GGEPContainer();
        
        private GGEPContainer() {
            this(null, -1, 0);
        }
        
        GGEPContainer(Set<? extends IpPort> locs, long create, long size64) {
            if(locs == null)
                locations = Collections.emptySet();
            else
                locations = Collections.unmodifiableSet(locs);
            createTime = create;
            this.size64 = size64;
        }
        
        boolean isEmpty() {
            return locations.isEmpty() && createTime <= 0 && size64 <= Integer.MAX_VALUE;
        }
    }
}

