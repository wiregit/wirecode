package com.limegroup.gnutella;

import java.util.StringTokenizer;
import java.io.*;
import com.sun.java.util.collections.*;

/**
 * A single result from a query reply message.
 * Create these to respond to a search.   Immutable.
 */
public class Response {
    /** Both index and size must fit into 4 unsigned bytes; see
     *  constructor for details. */
    private long index;
    private long size;
    private byte[]  nameBytes;
    /** The name of the file matching the search.  This does NOT
     *  include the double null terminator.
     */
    private String name;

	/** The meta variable is a string of meta information that
	 *  will be added per response (as opposed to per QueryReply
	 */
	private String metadata;

    private byte[] metaBytes;

    /** Per HUGE v0.93 proposal, urns includes returned urn-values
     */
    private HashSet urns;
    // HUGE v.093 GeneralExtensionMechanism between-the-null extensions
    private byte[] extBytes;
    
    /** Creates a fresh new response.
     *
     * @requires index and size can fit in 4 unsigned bytes, i.e.,
     *  0 <= index, size < 2^32
     */
    public Response(long index, long size, String name) {
        Assert.that((index & 0xFFFFFFFF00000000l)==0,
                "Response constructor: index "+index+" too big!");
        Assert.that((size &  0xFFFFFFFF00000000l)==0,
                "Response constructor: size "+size+" too big!");

        this.index=index;
        this.size=size;
        this.name=name;
        this.nameBytes = name.getBytes();
		metadata = "";
        this.metaBytes = metadata.getBytes();
    }

    /**Overloaded constructor that allows the creation of Responses with
     * meta-data
     */
    public Response(long index, long size, String name,String metadata) {
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
    }

    /**
     * Overloaded constructor that picks up data from between the  nulls
     * That data is then made into a nice xml string that can 
     * be converted into a LimeXMLDocument
     */
    public Response(String betweenNulls, long index, long size, String name){
        this(index,size,name,""); // reuse standard constructor

        //now handle between-the-nulls
		// \u001c is the HUGE v0.93 GEM delimiter
        StringTokenizer stok = new StringTokenizer(betweenNulls,"\u001c"); 
        while(stok.hasMoreTokens()) {
            this.handleLegacyOrGemExtensionString(stok.nextToken());
        }
    }
    
    protected void handleLegacyOrGemExtensionString(String ext) {      
		if(URN.isURN(ext)) {
			// it's a HUGE v0.93 URN name for the same files
			try {
				URN urn = URNFactory.createURN(ext);
				if (urns == null) urns = new HashSet();
				this.addUrn(urn);
			} catch(IOException e) {
				// there was an error creating the URN, so return
				return;
			}
        } else {
            // it's legacy between-the-nulls gump
            //create an XML string out of the data between the nulls
            String length="";
            String bitrate="";
            String first="";
            String second="";
            StringTokenizer tok = new StringTokenizer(ext);
            try{
                first = tok.nextToken();
                second = tok.nextToken();
            }catch(Exception e){//If I catch an exception, all bets are off.
                first="";
                second="";
            }
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
            if(bearShare1 || bearShare2 || gnotella){//some metadata we understand
                this.metadata = "<audios xsi:noNamespaceSchemaLocation="+
                     "\"http://www.limewire.com/schemas/audio.xsd\">"+
                     "<audio title=\""+name+"\" bitrate=\""+bitrate+
                     "\" seconds=\""+length+"\">"+
                     "</audio></audios>";
                this.metaBytes=metadata.getBytes();
            }
            else{
                this.metadata= "";
                this.metaBytes = metadata.getBytes();
            }
            this.index=index;
            this.size=size;
            this.name=name;
            this.nameBytes = name.getBytes(); 
        }
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
        if (urns != null) {
            updateExtBytes();
            System.arraycopy(extBytes, 0, array, i, extBytes.length);
            i+=extBytes.length;
        }
        //add the second null terminator
        array[i++]=(byte)0;
        return i;
    }
    
    protected void updateExtBytes() {
        if (extBytes != null) {
            // already up-to-date
            return;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (urns != null ) {
                Iterator iter = urns.iterator();
                while (iter.hasNext()) {
                    URN urn = (URN)iter.next();
                    baos.write(urn.getURNString().getBytes());
                    if (iter.hasNext()) {
                        baos.write(0x1c);
                    }
                }
            }
            extBytes = baos.toByteArray();
        } catch (IOException ioe) {
            System.out.println("Response.updateExtBytes() IOException");
            extBytes = new byte[0];
        }
    }
    
    /**
     */
    public int getLength() {
        // must match same number of bytes writeToArray() will write
        updateExtBytes();
        return 8 +                   // index and size
               nameBytes.length +
               1 +                   // null
               extBytes.length +
               1;                    // final null
    }
    
    /**
     * utility function for instantiating individual responses from inputstream
     */
    public static Response readFromStream(InputStream is) throws IOException {
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
        String name=new String(baos.toByteArray());

        // Extract extra info, if any
        baos.reset();
        while((c=is.read())!=0) {
            baos.write(c);
        }
        String betweenNulls=new String(baos.toByteArray());
        if(betweenNulls==null || betweenNulls.equals("")) {
            return new Response(index,size,name);
        } else {
            return new Response(betweenNulls,index,size,name);
        }
    }
    

	/**
	 * Adds a URN to the set of URNs for this <tt>Response</tt> instance.
	 * Note that this method does no validity checking a requires that
	 * the caller supply a valid URN.
	 *
	 * @param urn the <tt>URN</tt> instance to add to the set of URNs for
	 *  this response
	 */
    public void addUrn(URN urn){
        if (urns == null) urns = new HashSet();
        urns.add(urn);
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
        return nameBytes;
    }

    public byte[] getMetaBytes() {
        return metaBytes;
    }

    public String getName() {
        return name;
    }

	public String getMetadata() {
		return metadata;
	}

    public HashSet getUrns() {
            return urns;
    }
  
    /**
     * returns true if metadata is not XML, but ToadNode's response

     public boolean hasToadNodeData(){
     if(metadata.indexOf("<")>-1 && metadata.indexOf(">") > -1)
     return false;
     return true;//no angular brackets. This is a TOADNODE response
     }
    */

    public boolean equals(Object o) {
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
}

