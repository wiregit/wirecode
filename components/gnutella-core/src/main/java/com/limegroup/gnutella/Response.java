package com.limegroup.gnutella;

import java.util.StringTokenizer;
import java.io.*;
import com.sun.java.util.collections.*;

/**
 * A single result from a query reply message.
 * Create these to respond to a search. 
 */
public class Response {
    /** Both index and size must fit into 4 unsigned bytes; see
     *  constructor for details. */
    private final long index;
    private final long size;
    private final byte[]  nameBytes;
    /** The name of the file matching the search.  This does NOT
     *  include the double null terminator.
     */
    private final String name;

	/** The meta variable is a string of meta information that
	 *  will be added per response (as opposed to per QueryReply
	 */
	private String metadata;

    private byte[] metaBytes;

    /** Per HUGE v0.93 proposal, urns includes returned urn-values
     */
    private final Set urns;

    // HUGE v.093 GeneralExtensionMechanism between-the-null extensions
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

    /** Creates a fresh new response.
     *
     * @requires index and size can fit in 4 unsigned bytes, i.e.,
     *  0 <= index, size < 2^32
     */
    public Response(long index, long size, String name) {
		this(index, size, name, "", null);
    }

    /**Overloaded constructor that allows the creation of Responses with
     * meta-data
     */
    public Response(long index, long size, String name,
					String metadata) {
		this(index, size, name, metadata, null);
	}

	/**
	 * Constructs a new <tt>Response</tt> instance from the data in the
	 * specified <tt>FileDesc</tt>.  
	 *
	 * @param fd the <tt>FileDesc</tt> containing the data to construct 
	 *  this <tt>Response</tt>
	 */
	public Response(FileDesc fd) {
		this(fd.getIndex(), fd.getFile().length(), fd.getFile().getName(), 
			 "", fd.getUrns());
	}

    /**
	 * Overloaded constructor that allows the creation of Responses with
     * meta-data and a <tt>Set</tt> of <tt>URN</tt> instances.
     */
    private Response(long index, long size, String name,
					 String metadata, Set urns) {
        Assert.that((index & 0xFFFFFFFF00000000l)==0,
                "Response constructor: index too big!");
        Assert.that((size &  0xFFFFFFFF00000000l)==0,
                "Response constructor: size too big!");
        metadata = metadata.trim();      
        this.index=index;
        this.size=size;
        this.name=name;
        this.nameBytes = name.getBytes();
		this.metadata = metadata;
        this.metaBytes = metadata.getBytes();
		if(urns == null) {
			this.urns = Collections.EMPTY_SET;
		}
		else {
			this.urns = Collections.unmodifiableSet(urns);
		}
		extBytes = createExtBytes(this.urns);
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
		if(second.startsWith("kbps"))
			bearShare1 = true;
		else if (first.endsWith("kbps"))
			bearShare2 = true;
		if(bearShare1){
			bitrate = first;
		}
		else if (bearShare2){
			int j = first.indexOf("kbps");
			bitrate = first.substring(0,j);
		}
		if(bearShare1 || bearShare2){
			while(tok.hasMoreTokens())
				length=tok.nextToken();
			//OK we have the bitrate and the length
		}
		else if (ext.endsWith("kHz")){//Gnotella
			gnotella = true;
			length=first;
			//extract the bitrate from second
			int i=second.indexOf("kbps");
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
     * write as if inside a QueryReply
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
	 * Staticc zero-length array that can be used for any <tt>Response</tt>
	 * instance that doesn't have urns.
	 */
	private final static byte[] NULL_EXT_ARRAY = new byte[0];

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
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
			if(urns == null) {
				return NULL_EXT_ARRAY;
			}

			Iterator iter = urns.iterator();
			while (iter.hasNext()) {
				URN urn = (URN)iter.next();
				baos.write(urn.stringValue().getBytes());
				if (iter.hasNext()) {
					baos.write(0x1c);
				}
			}
            return baos.toByteArray();
        } catch (IOException ioe) {
			// simply do not store any bytes for extensions if there
			// was a problem
			return NULL_EXT_ARRAY;
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
						URN urn = URNFactory.createUrn(ext);
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
            //return new Response(betweenNulls,index,size,name);
			return new Response(index, size, name, metaString, urns);
        }
    }
    
    /**
     * To add metaData to a response after it has been created.
     * Added to faciliatate setting audio metadata for responses
     * generated from ordinary searches. 
     */
    public void setMetadata(String meta){
		this.metadata = meta;
		this.metaBytes = meta.getBytes();
    }

    public long getIndex() {
        return index;
    }

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

    public int getMetaBytesSize() {
        return metaBytes.length;
    }

    public byte[] getNameBytes() {
		byte[] copy = new byte[nameBytes.length];
		System.arraycopy(nameBytes, 0, copy, 0, nameBytes.length);
        return copy;
    }

    public byte[] getMetaBytes() {
		byte[] copy = new byte[metaBytes.length];
		System.arraycopy(metaBytes, 0, copy, 0, metaBytes.length);
        return copy;
    }

    public String getName() {
        return name;
    }

	public String getMetadata() {
		return metadata;
	}

	/**
	 * Returns an immutable <tt>Set</tt> of <tt>URN</tt> instances for 
	 * this <tt>Response</tt>.
	 *
	 * @return an immutable <tt>Set</tt> of <tt>URN</tt> instances for 
	 * this <tt>Response</tt>
	 */
    public Set getUrns() {
		return Collections.unmodifiableSet(urns);
    }

	// TODO: do we care that this does not compare all of the elements?
    public boolean equals(Object o) {
		if(o == this) return true;
        if (! (o instanceof Response))
            return false;
        Response r=(Response)o;
        return r.getIndex()==getIndex()
            && r.getSize()==getSize()
            && r.getName().equals(getName());
    }

    public int hashCode() {
        //Good enough for the moment
        return name.hashCode()+(int)size+(int)index;
    }

    //Unit Test Code 
    //(Added when bytes stuff was added here 3/2/2001 by Sumeet Thadani)
	/*
    public static void main(String args[]){
        Response r = new Response(3,4096,"A.mp3");
        int nameSize = r.getNameBytesSize();
        Assert.that(nameSize==5);
        byte[] nameBytes = r.getNameBytes();
        Assert.that (nameBytes[0] == 65);
        Assert.that((new String(r.getMetaBytes())).equals(""),"Spurios meta");
        Assert.that(r.getMetaBytesSize() == 0,"Meta size not right");
        //
        Response r2 = new Response("",999,4,"blah.txt");
        Assert.that((new String(r2.getMetaBytes())).equals(""),"bad meta");
        Assert.that(r2.getMetaBytesSize() == 0,"Meta size not right");
        String md = "Hello";
        Response r3 = new Response(md,999,4,"king.txt");
        Assert.that((new String(r3.getMetaBytes())).equals(""),"bad meta");
        Assert.that(r3.getMetaBytesSize() == 0,"Meta size not right");
        //The three formats we support
        String[] meta = {"a kbps 44.1 kHz b","akbps 44.1 kHz b", 
                                             "b akbps 44.1kHz" };
        for(int i=0;i<meta.length;i++){
            Response r4 = new Response(meta[i],999+i,4,"abc.txt");
            com.limegroup.gnutella.xml.LimeXMLDocument d=null;
            String xml = r4.getMetadata();
            try{
                d = new com.limegroup.gnutella.xml.LimeXMLDocument(xml);
            }catch (Exception e){
                Assert.that(false,"XML not created well from between nulls");
            }
            String br = d.getValue("audios__audio__bitrate__");
            Assert.that(br.equals("a"));
            String len = d.getValue("audios__audio__seconds__");
            Assert.that(len.equals("b"));
        }
    }
	*/
}

