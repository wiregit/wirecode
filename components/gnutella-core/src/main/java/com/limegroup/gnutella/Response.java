package com.limegroup.gnutella;

import java.util.StringTokenizer;
import com.limegroup.gnutella.xml.*;
import org.xml.sax.SAXException;
import java.io.*;
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

	/**
	 * Constant for an empty, unmodifiable <tt>Set</tt>.  This is necessary
	 * because Collections.EMPTY_SET is not serializable in the collections
	 * 1.1 implementation.
	 */
	private static final Set EMPTY_SET = 
		Collections.unmodifiableSet(new HashSet());

	/**
	 * Cached immutable empty array of bytes to avoid unnecessary allocations.
	 */
	private final static byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /** Creates a fresh new response.
     *
     * @requires index and size can fit in 4 unsigned bytes, i.e.,
     *  0 <= index, size < 2^32
     */
    public Response(long index, long size, String name) {
		this(index, size, name, "", null, null);
    }


    /**
     * Creates a new response with parsed metadata.  Typically this is used
     * to respond to query requests.
     * @param doc the metadata to include
     */
    public Response(long index, long size, String name, LimeXMLDocument doc) {
        this(index, size, name, extractMetadata(doc), null, doc);
    }


    /**
     * Creates a new response with raw unparsed metadata.  Typically this
     * is used when reading replies from the network.
     * @param metadata a string of metadata, typically XML
     */
    public Response(long index, long size, String name, String metadata) {
		this(index, size, name, metadata, null, null);
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
			 "", fd.getUrns(), null);
	}

      /** CHORD ADDITION
	  Allows the construction of responses from remote hosts with URNS.
      */
      public Response(long index, long size, String name, Set urns)
      {
	 this(index,size,name,null,urns,null);
      }

    /**
	 * Overloaded constructor that allows the creation of Responses with
     * meta-data and a <tt>Set</tt> of <tt>URN</tt> instances.  This 
	 * is the primary constructor that establishes all of the class's 
	 * invariants, does any necessary parameter validation, etc.
	 *
	 * @param index the index of the file referenced in the response
	 * @param size the size of the file (in bytes)
	 * @param name the name of the file
	 * @param metadata the string of metadata associated with the file
	 * @param urns the <tt>Set</tt> of <tt>URN</tt> instances associated
	 *  with the file
	 * @param doc the <tt>LimeXMLDocument</tt> instance associated with
	 *  the file
     */
    private Response(long index, long size, String name,
					 String metadata, Set urns, LimeXMLDocument doc) {
        Assert.that((index & 0xFFFFFFFF00000000l)==0,
                "Response constructor: index too big!");
        Assert.that((size &  0xFFFFFFFF00000000l)==0,
                "Response constructor: size too big!");
        this.index=index;
        this.size=size;
		if(name == null) {
			this.name = "";
		} else {
			this.name = name;
		}
        this.nameBytes = this.name.getBytes();

		if(urns == null) {
			// this is necessary because Collections.EMPTY_SET is not
			// serializable in collections 1.1
			this.urns = EMPTY_SET;
		}
		else {
			this.urns = Collections.unmodifiableSet(urns);
		}
		this.extBytes = createExtBytes(this.urns);

		if(((metadata == null) || (metadata.equals(""))) && (doc != null)) {
			// this is guaranteed to be non-null, although it could be the
			// empty string
			this.metadata = extractMetadata(doc);
		} else if(metadata == null) {
			this.metadata = "";
		} else {
			this.metadata = metadata.trim();
		}
		this.metaBytes = this.metadata.getBytes();
		
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
            baos.write(c);
        }
        String name = new String(baos.toByteArray());

        // Extract extra info, if any
        baos.reset();
        while((c=is.read())!=0) {
            baos.write(c);
        }
        String betweenNulls = new String(baos.toByteArray());
        if(betweenNulls==null || betweenNulls.equals("")) {
			if(is.available() < 16) {
				throw new IOException("not enough room for the GUID");
			}
            return new Response(index,size,name);
        } else {
			// now handle between-the-nulls
			// \u001c is the HUGE v0.93 GEM delimiter
			StringTokenizer stok = new StringTokenizer(betweenNulls,"\u001c"); 
			Set urns = null;
			String metaString = null;
			while(stok.hasMoreTokens()) {
				String ext = stok.nextToken();
				if(URN.isUrn(ext)) {
					// it's a HUGE v0.93 URN name for the same files
					try {
						URN urn = URN.createSHA1Urn(ext);
						if (urns == null) urns = new HashSet();
						urns.add(urn);
					} catch(IOException e) {
						// there was an error creating the URN, so go to the
						// next one
						continue;
					}
				} else {
					metaString = createXmlString(name, ext);
				}
			}			
			return new Response(index, size, name, metaString, urns, null);
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
	private static byte[] createExtBytes(Set urns) {
        try {
			if(urns == null) {
				return EMPTY_BYTE_ARRAY;
			}
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Iterator iter = urns.iterator();
			while (iter.hasNext()) {
				URN urn = (URN)iter.next();
				baos.write(urn.toString().getBytes());
				if (iter.hasNext()) {
					baos.write(0x1c);
				}
			}
            return baos.toByteArray();
        } catch (IOException ioe) {
			// simply do not store any bytes for extensions if there
			// was a problem
			return EMPTY_BYTE_ARRAY;
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
		metaBytes = metadata.getBytes();
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
		else if (metadata != null) {
			try {
				return new LimeXMLDocument(metadata);
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
	 * Accessor for the SHA1 URN for this <tt>Response</tt>.
	 *
	 * @return the SHA1 <tt>URN</tt> for this <tt>Response</tt>, or 
	 *  <tt>null</tt> if there is none
	 */
	public URN getSHA1Urn() {
		Iterator iter = urns.iterator(); 
		while(iter.hasNext()) {
			URN urn = (URN)iter.next();
			if(urn.isSHA1()) {
				return urn;
			}
		}
		return null;
	}
    
    byte[] getExtBytes() {
        return extBytes;
    }

	// overrides Object.equals to provide a broader and more precise
	// definition of equality
    public boolean equals(Object o) {
		if(o == this) return true;
        if (! (o instanceof Response))
            return false;
        Response r=(Response)o;
		return (getIndex() == r.getIndex() &&
                getSize() == r.getSize() &&
				getName().equals(r.getName()) &&
                Arrays.equals(getNameBytes(), r.getNameBytes()) &&
				getMetadata().equals(r.getMetadata()) &&
				Arrays.equals(getMetaBytes(), r.getMetaBytes()) &&
				((getDocument() == null) ? (r.getDocument() == null) :
				 getDocument().equals(r.getDocument())) &&
				getUrns().equals(r.getUrns()) &&
				Arrays.equals(getExtBytes(), r.getExtBytes()));
    }


    public int hashCode() {
        //Good enough for the moment
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

    //Unit test can be found in
    //core/com/limegroup/gnutella/tests/ResponseTest.java (soon to be
    //tests/com/limegroup/gnutella/tests/ResponseTest.java)
}

