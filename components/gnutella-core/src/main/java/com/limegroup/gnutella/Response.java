package com.limegroup.gnutella;

import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.messages.HUGEExtension;
import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.SchemaNotFoundException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import org.xml.sax.SAXException;
import com.sun.java.util.collections.*;


/**
 * A single result from a query reply message.  (In hindsight, "Result" would
 * have been a better name.)  Besides basic file information, responses can
 * include metadata, which is actually stored query reply's QHD.  Metadata comes
 * in two formats: raw strings or parsed LimeXMLDocument.<p>
 *
 * Response was originally intended to be immutable, but it currently includes
 * mutator methods for metadata; these will be removed in the future.  
 */
public class Response {
    
    /**
     * The magic byte to use as extension seperators.
     */
    private static final byte EXT_SEPERATOR = 0x1c;
    
    /**
     * The magic byte, as a string, to use as extension seperators.
     */
    private static final String EXT_STRING = "\u001c";
    
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
    /**
     * Metadata can be stored in one of two forms: raw strings (read from the
     * network) or LimeXMLDocument (prepared by FileManager).  Storing the
     * latter makes calculating aggregate strings in the QHD more efficient.
     * Response provides methods to access both formats, lazily converting
     * between the two and caching the results as needed.     
     *
     * INVARIANT: metadata!=null ==> metaBytes==metadata.getBytes()
     * INVARIANT: metadata!=null && document!=null ==> metadata and document
     *  are equivalent but in different formats 
     */
    
	/** Raw unparsed XML metadata. */
	private String metadata;

    /** The bytes of the metadata instance variable, used for
     * internationalization purposes.  (Remember that
     * metadata.length()!=metaBytes.length.)  This is guaranteed to not be
	 * null
	 */
    private byte[] metaBytes;

    /** The document representing the XML in this response. */
    private LimeXMLDocument document;

    /** 
	 * The <tt>Set</tt> of <tt>URN</tt> instances for this <tt>Response</tt>,
	 * as specified in HUGE v0.94.  This is guaranteed to be non-null, 
	 * although it is often empty.
     */
    private final Set urns;

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
	 * Static constant for the beginning of the xml audios schema so
	 * that we don't have to generate new strings each time.
	 */
 	private static final String AUDIOS_NAMESPACE =
		"<audios xsi:noNamespaceSchemaLocation="+
		"\"http://www.limewire.com/schemas/audio.xsd\">";

	/**
	 * Constant for the XML audio title key.
	 */
	private static final String AUDIO_TITLE =
		"<audio title=\"";
	
	/**
	 * Constant for the XML audio bitrate key.
	 */
	private static final String AUDIO_BITRATE = 
		"bitrate=\"";

	/**
	 * Constant for the XML audio seconds key.
	 */
	private static final String AUDIO_SECONDS =
		"seconds=\"";

	/**
	 * Constant for the string that ends the audios xml.
	 */
	private static final String AUDIOS_CLOSE =
		"</audio></audios>";				

	/**
	 * Constant for a quote followed by a space, used in XML.
	 */
	private static final String QUOTE_SPACE ="\" ";

	/**
	 * Constant for a quote followed by the XML close tag.
	 */
	private static final String CLOSE_TAG = "\">";

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
		this(index, size, name, "", null, null, null, null);
    }


    /**
     * Creates a new response with parsed metadata.  Typically this is used
     * to respond to query requests.
     * @param doc the metadata to include
     */
    public Response(long index, long size, String name, LimeXMLDocument doc) {
        this(index, size, name, extractMetadata(doc), null, doc, null, null);
    }


    /**
     * Creates a new response with raw unparsed metadata.  Typically this
     * is used when reading replies from the network.
     * @param metadata a string of metadata, typically XML
     */
    public Response(long index, long size, String name, String metadata) {
		this(index, size, name, metadata, null, null, null, null);
	}

	/**
	 * Constructs a new <tt>Response</tt> instance from the data in the
	 * specified <tt>FileDesc</tt>.  
	 *
	 * @param fd the <tt>FileDesc</tt> containing the data to construct 
	 *  this <tt>Response</tt> -- must not be <tt>null</tt>
	 */
	public Response(FileDesc fd) {
		this(fd.getIndex(), fd.getSize(), fd.getName(), 
			 "", fd.getUrns(), null, 
			 new GGEPContainer(
			    getAsEndpoints(fd.getAlternateLocationCollection()),
			    CreationTimeCache.instance().getCreationTimeAsLong(fd.getSHA1Urn())),
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
	 * @param metadata the string of metadata associated with the file
	 * @param urns the <tt>Set</tt> of <tt>URN</tt> instances associated
	 *  with the file
	 * @param doc the <tt>LimeXMLDocument</tt> instance associated with
	 *  the file
	 * @param endpoints a collection of other locations on this network
	 *        that will have this file
	 * @param extensions The raw unparsed extension bytes.
     */
    private Response(long index, long size, String name,
					 String metadata, Set urns, LimeXMLDocument doc, 
					 GGEPContainer ggepData, byte[] extensions) {
        if( (index & 0xFFFFFFFF00000000L)!=0 )
            throw new IllegalArgumentException("invalid index: " + index);
        // see note in createFromStream about Integer.MAX_VALUE
        if (size > Integer.MAX_VALUE || size < 0)
            throw new IllegalArgumentException("invalid size: " + size);
            
        this.index=index;
        this.size=size;
        
		if (name == null)
			this.name = "";
		else
			this.name = name;

        byte[] temp = null;
        
        try {
            temp = this.name.getBytes("UTF-8");
        }
        catch(UnsupportedEncodingException namex) {
            //b/c this should never happen, we will show and error
            //if it ever does for some reason.
            ErrorService.error(namex);
        }
        
        this.nameBytes = temp;

		if (urns == null)
			this.urns = DataUtils.EMPTY_SET;
		else
			this.urns = Collections.unmodifiableSet(urns);
		
        if(ggepData == null)
            this.ggepData = GGEPContainer.EMPTY;
        else
		    this.ggepData = ggepData;
		
		if (extensions != null)
		    this.extBytes = extensions;
		else 
		    this.extBytes = createExtBytes(this.urns, this.ggepData);

		if(((metadata == null) || (metadata.equals(""))) && (doc != null)) {
			// this is guaranteed to be non-null, although it could be the
			// empty string
			this.metadata = extractMetadata(doc);
		} else if(metadata == null) {
			this.metadata = "";
		} else {
			this.metadata = metadata.trim();
		}
        Assert.that(this.metadata!=null, "Null metadata");
        try { //It's possible to get metadata between the null from others
            this.metaBytes = this.metadata.getBytes("UTF-8");
        } catch(UnsupportedEncodingException ueex) {
            //b/c this should never happen, we will show and error
            //if it ever does for some reason.
            ErrorService.error(ueex);
        }
		
		// we don't generate this from the metadata string in the case where the
		// LimeXMLDocument is null and the metadata string is not because the
		// construction of LimeXMLDocuments is expensive -- just let it be null
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
        long index=ByteOrder.ubytes2long(ByteOrder.leb2int(is));
        long size=ByteOrder.ubytes2long(ByteOrder.leb2int(is));
        
        if( (index & 0xFFFFFFFF00000000L)!=0 )
            throw new IOException("invalid index: " + index);
        // must use Integer.MAX_VALUE instead of mask because
        // this value is later converted to an int, so we want
        // to ensure that when it's converted it doesn't become
        // negative.
        if (size > Integer.MAX_VALUE || size < 0)
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
        if(name.length() == 0) {
            throw new IOException("empty name in response");
        }

        // Extract extra info, if any
        baos.reset();
        while((c=is.read())!=0) {
            if(c == -1)
                throw new IOException("EOF before null termination");
            baos.write(c);
        }
        byte[] rawMeta = baos.toByteArray();
        String betweenNulls = new String(rawMeta);
        if(betweenNulls==null || betweenNulls.equals("")) {
			if(is.available() < 16) {
				throw new IOException("not enough room for the GUID");
			}
            return new Response(index,size,name);
        } else {
			// now handle between-the-nulls
			// \u001c is the HUGE v0.93 GEM delimiter
            HUGEExtension huge = new HUGEExtension(rawMeta);

			Set urns = huge.getURNS();

			String metaString = "";
            Iterator iter = huge.getMiscBlocks().iterator();
            while (iter.hasNext() && metaString.equals(""))
                metaString = createXmlString(name, (String)iter.next());

			GGEPContainer ggep = GGEPUtil.getGGEP(huge.getGGEPBlocks());

			return new Response(index, size, name, metaString, 
			                    urns, null, ggep, rawMeta);
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
	private static String createXmlString(String name, String ext) {
		StringTokenizer tok = new StringTokenizer(ext);
		if(tok.countTokens() < 2) {
			// if there aren't the expected number of tokens, simply
			// return the empty string
			return "";
		}
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
		if(bearShare1 || bearShare2 || gnotella) {//some metadata we understand
			StringBuffer sb = new StringBuffer();
			sb.append(AUDIOS_NAMESPACE);
			sb.append(AUDIO_TITLE);
			sb.append(name);
			sb.append(QUOTE_SPACE);
			sb.append(AUDIO_BITRATE);
			sb.append(bitrate);
			sb.append(QUOTE_SPACE);
			sb.append(AUDIO_SECONDS);
			sb.append(length);
			sb.append(CLOSE_TAG);
			sb.append(AUDIOS_CLOSE);
			return sb.toString();
		}
		return "";
	}

	/**
	 * Helper method that creates an array of bytes for the specified
	 * <tt>Set</tt> of <tt>URN</tt> instances.  The bytes are written
	 * as specified in HUGE v 0.94.
	 *
	 * @param urns the <tt>Set</tt> of <tt>URN</tt> instances to use in
	 *  constructing the byte array
	 */
	private static byte[] createExtBytes(Set urns, GGEPContainer ggep) {
        try {
            if( isEmpty(urns) && ggep.isEmpty() )
                return DataUtils.EMPTY_BYTE_ARRAY;
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();            
            if( !isEmpty(urns) ) {
                // Add the extension for URNs, if any.
    			Iterator iter = urns.iterator();
    			while (iter.hasNext()) {
    				URN urn = (URN)iter.next();
                    Assert.that(urn!=null, "Null URN");
    				baos.write(urn.toString().getBytes());
    				// If there's another URN, add the seperator.
    				if (iter.hasNext()) {
    					baos.write(EXT_SEPERATOR);
    				}
    			}
    			
    			// If there's ggep data, write the separator.
    		    if( !ggep.isEmpty() )
    		        baos.write(EXT_SEPERATOR);
            }
            
            // It is imperitive that GGEP is added LAST.
            // That is because GGEP can contain 0x1c (EXT_SEPERATOR)
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
    private static boolean isEmpty(Set set) {
        return set == null || set.isEmpty();
    }
    
    /**
     * Utility method for converting an AlternateLocationCollection to a
     * smaller set of endpoints.
     */
    private static Set getAsEndpoints(AlternateLocationCollection col) {
        if( col == null || col.getAltLocsSize() == 0)
            return DataUtils.EMPTY_SET;
        
        synchronized(col) {
            Set endpoints = null;
            int i = 0;
            for(Iterator iter = col.iterator();
              iter.hasNext() && i < MAX_LOCATIONS;) {
                AlternateLocation al = (AlternateLocation)iter.next();
                Endpoint host = al.getHost();
                if( !NetworkUtils.isMe(host.getAddress(), host.getPort()) ) {
                    if (endpoints == null)
                        endpoints = new HashSet();
                    endpoints.add( al.getHost() );
                    i++;
                }
            }
            return endpoints == null ? DataUtils.EMPTY_SET : endpoints;
        }
        
    }           

	/**
	 * Utility method for extracting the metadata string from the given
	 * <tt>LimeXMLDocument</tt> instance.
	 *
	 * @param doc the <tt>LimeXMLDocument</tt> instance that contains the
	 *  desired metadata
	 * @return the metadata string for the <tt>LimeXMLDocument</tt>, or the
	 *  empty string if the data could not be extracted for any reason
	 */
	private static String extractMetadata(LimeXMLDocument doc) {
		if(doc == null) {
			return "";
		}
		try {
			return doc.getXMLString();
		} catch (SchemaNotFoundException e) {
		}
		return "";
	}

    /**
     * Write the response data to the given byte array, as if inside a 
	 * <tt>QueryReply</tt>.
     */
    public int writeToArray(byte[] array, int i) {
        ByteOrder.int2leb((int)index,array,i);
        ByteOrder.int2leb((int)size,array,i+4);
        i+=8;            
        System.arraycopy(nameBytes, 0, array, i, nameBytes.length);
        i+=nameBytes.length;
        //Write first null terminator.
        array[i++]=(byte)0;
        // write HUGE v0.93 General Extension Mechanism extensions
        // (currently just URNs)
		System.arraycopy(extBytes, 0, array, i, extBytes.length);
		i+=extBytes.length;
        //add the second null terminator
        array[i++]=(byte)0;
        return i;
    }
    

    /**
     * Like writeToArray(), but writes to an OutputStream.
     */
    public void writeToStream(OutputStream os) throws IOException {
        ByteOrder.int2leb((int)index, os);
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
     * Sets this' metadata.  Added to faciliatate setting audio metadata for
     * responses generated from ordinary searches.  Typically this should only
     * be called if no metadata was passed to this' constructor.
     * @param meta the parsed XML metadata 
     */	
    public void setDocument(LimeXMLDocument doc) {
        document = doc;

		// this is guaranteed to be non-null, although it could be the empty
		// string
		metadata = extractMetadata(document);
        try {
            metaBytes = metadata.getBytes("UTF-8");
        }
        catch(UnsupportedEncodingException uee) {
            //b/c this should never happen, we will show and error
            //if it ever does for some reason.
            ErrorService.error(uee);
        }
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
     * Returns the size of the name in bytes (not the whole response)
     * In the constructor we create the nameBytes array once which is 
     * the representation of the name in bytes. 
     *<p> storing the nameBytes is an optimization beacuse we do not want 
     * to call getBytes() on the name string every time we need the byte
     * representation or the number of bytes in the name.
     */
    public int getNameBytesSize() {
        return nameBytes.length;
    }

	/**
	 * Accessor fot the length of the array of bytes that stores the meta-
	 * data.
	 * 
	 * @return the length of the array of bytes that stores the meta-data
	 */
    public int getMetaBytesSize() {
        return metaBytes.length;
    }

	/**
	 * Returns a copy of the array of bytes for the file name for this 
	 * <tt>Response</tt> instance.
	 *
	 * @return a copy of the array of bytes for the file name for this 
	 * <tt>Response</tt> instance
	 */
    public byte[] getNameBytes() {
		// make a defensive copy to preserve invariants
		byte[] copy = new byte[nameBytes.length];
		System.arraycopy(nameBytes, 0, copy, 0, nameBytes.length);
        return copy;
    }

	/**
	 * Returns a copy of the array of bytes for the meta-data for this
	 * <tt>Response</tt> instance.
	 *
	 * @return a copy of the array of bytes for the meta-data
	 */
    public byte[] getMetaBytes() {
		// make a defensive copy to preserve invariants
		byte[] copy = new byte[metaBytes.length];
		System.arraycopy(metaBytes, 0, copy, 0, metaBytes.length);
        return copy;
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
	 * Returns the string of meta-data for this <tt>Response</tt> instance.
	 *
	 * @return the string of meta-data for this <tt>Response</tt> instance,
	 *  which is guaranteed to be non-null, but which can be the empty string
	 */
	public String getMetadata() {
		return metadata;
	}

    /**
     * Returns this' metadata as a parsed XML document
     * @return the metadata, or null if none exists or the metadata
     *  couldn't be parsed
     */
    public LimeXMLDocument getDocument() {
		if (document != null) 
			return document;
		else if (metadata != null && !metadata.equals("")) {
			try {
			    document = new LimeXMLDocument(metadata);
			    return document;
			} catch (SAXException e) {
			} catch (SchemaNotFoundException e) {
			} catch (IOException e) { }
		}
		return null;//both document and metadata are null
    }

	/**
	 * Returns an immutable <tt>Set</tt> of <tt>URN</tt> instances for 
	 * this <tt>Response</tt>.
	 *
	 * @return an immutable <tt>Set</tt> of <tt>URN</tt> instances for 
	 * this <tt>Response</tt>, guaranteed to be non-null, although the
	 * set could be empty
	 */
    public Set getUrns() {
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
    public Set getLocations() {
        return ggepData.locations;
    }
    
    /**
     * Returns the create time.
     */
    public long getCreateTime() {
        return ggepData.createTime;
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
                 getCreateTime()
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
               Arrays.equals(getNameBytes(), r.getNameBytes()) &&
               getMetadata().equals(r.getMetadata()) &&
               Arrays.equals(getMetaBytes(), r.getMetaBytes()) &&
               ((getDocument() == null) ? (r.getDocument() == null) :
               getDocument().equals(r.getDocument())) &&
               getUrns().equals(r.getUrns());
    }


    public int hashCode() {
        //Good enough for the moment
        // TODO:: IMPROVE THIS HASHCODE!!
        return getName().hashCode()+(int)getSize()+(int)getIndex();
    }

	/**
	 * Overrides Object.toString to print out a more informative message.
	 */
	public String toString() {
		return ("index:        "+index+"\r\n"+
				"size:         "+size+"\r\n"+
				"name:         "+name+"\r\n"+
				"metadata:     "+metadata+"\r\n"+
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
            if( ggep == null || (ggep.locations.size() == 0 && ggep.createTime <= 0))
                throw new NullPointerException("null or empty locations");
            
            GGEP info = new GGEP();
            if(ggep.locations.size() > 0) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    for(Iterator i = ggep.locations.iterator(); i.hasNext();) {
                        try {
                            Endpoint ep = (Endpoint)i.next();
                            baos.write(ep.getHostBytes());
                            ByteOrder.short2leb((short)ep.getPort(), baos);
                        } catch(UnknownHostException uhe) {
                            continue;
                        }
                    }
                } catch(IOException impossible) {
                    ErrorService.error(impossible);
                }   
                info.put(GGEP.GGEP_HEADER_ALTS, baos.toByteArray());
            }
            
            if(ggep.createTime > 0)
                info.put(GGEP.GGEP_HEADER_CREATE_TIME, ggep.createTime / 1000);
            
            
            info.write(out);
        }
        
        /**
         * Returns a <tt>Set</tt> of other endpoints described
         * in one of the GGEP arrays.
         */
        static GGEPContainer getGGEP(Set ggeps) {
            if (ggeps == null)
                return GGEPContainer.EMPTY;
            Set locations = null;
            long createTime = -1;
            final byte[] ip = new byte[4];
            IPFilter ipFilter = IPFilter.instance();
            Iterator iter = ggeps.iterator();
            while (iter.hasNext()) {
                GGEP currGGEP = (GGEP) iter.next();
                // if the block has a ALTS value, get it, parse it,
                // and move to the next.
                if (currGGEP.hasKey(GGEP.GGEP_HEADER_ALTS)) {
                    byte[] locBytes;
                    try {
                        locBytes = currGGEP.getBytes(GGEP.GGEP_HEADER_ALTS);
                        // must be a multiple of 6
                        if (locBytes.length % 6 != 0)
                            continue;
                    } catch (BadGGEPPropertyException bad) {
                        //locBytes not set, key was added without value
                        continue;
                    }

                    for (int j = 0; j < locBytes.length; j += 6) {
                        int port = ByteOrder.ubytes2int(
                                    ByteOrder.leb2short(locBytes, j+4)
                                   );                                
                        if (!NetworkUtils.isValidPort(port))
                            continue;
                        ip[0] = locBytes[j];
                        ip[1] = locBytes[j + 1];
                        ip[2] = locBytes[j + 2];
                        ip[3] = locBytes[j + 3];
                        if (!NetworkUtils.isValidAddress(ip) ||
                            !ipFilter.allow(ip) ||
                            NetworkUtils.isMe(ip, port))
                            continue;
                        if (locations == null)
                            locations = new HashSet();
                        locations.add(new Endpoint(ip, port));
                    }
                }
                
                if(currGGEP.hasKey(GGEP.GGEP_HEADER_CREATE_TIME)) {
                    try {
                        createTime =
                            currGGEP.getLong(GGEP.GGEP_HEADER_CREATE_TIME) *
                            1000;
                    } catch(BadGGEPPropertyException bad) {
                        continue;
                    }
                }
            }
            
            return (locations == null && createTime == -1) ?
                GGEPContainer.EMPTY : new GGEPContainer(locations, createTime);
        }
    }
    
    /**
     * A container for information we're putting in/out of GGEP blocks.
     */
    static final class GGEPContainer {
        final Set locations;
        final long createTime;
        private static final GGEPContainer EMPTY = new GGEPContainer();
        
        private GGEPContainer() {
            this(null, -1);
        }
        
        GGEPContainer(Set locs, long create) {
            locations = locs == null ? DataUtils.EMPTY_SET : locs;
            createTime = create;
        }
        
        boolean isEmpty() {
            return locations.isEmpty() && createTime <= 0;
        }
    }
}

