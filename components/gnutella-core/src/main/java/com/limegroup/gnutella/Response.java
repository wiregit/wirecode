package com.limegroup.gnutella;

import java.util.StringTokenizer;

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
        Assert.that((index & 0xFFFFFFFF00000000l)==0,
                    "Response constructor: index too big!");
        Assert.that((size &  0xFFFFFFFF00000000l)==0,
                    "Response constructor: size too big!");
        //create an XML string out of the data between the nulls
        //System.out.println("Between nulls is "+betweenNulls);
        String length="";
        String bitrate="";
        String first="";
        String second="";
        StringTokenizer tok = new StringTokenizer(betweenNulls);
        try{
            first = tok.nextToken();
            second = tok.nextToken();
        }catch(Exception e){//If I catch an exception, all bets are off.
            first="";
            second="";
            betweenNulls="";
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
        else if (betweenNulls.endsWith("kHz")){//Gnotella
            gnotella = true;
            length=first;
            //extract the bitrate from second
            int i=second.indexOf("kbps");
            bitrate = second.substring(0,i);
        }
        if(bearShare1 || bearShare2 || gnotella){//some metadata we understand
            this.metadata = "<audios xsi:noNamespaceSchemaLocation="+
                 "\"http://www.limewire.com/schemas/audio.xsd\">"+
                 "<audio title=\""+name+"\" bitrate=\""+bitrate+
                 "\" length=\""+length+"\">"+
                 "</audio></audios>";
            this.metaBytes=metadata.getBytes();
            this.index=index;
        }
        this.size=size;
        this.name=name;
        this.nameBytes = name.getBytes(); 
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

    /*
    //Unit Test Code 
    //(Added when bytes stuff was added here 3/2/2001 by Sumeet Thadani)
    public static void main(String args[]){
        Response r = new Response(3,4096,"A.mp3");
        int nameSize = r.getNameBytesSize();
        Assert.that(nameSize==5);
        byte[] nameBytes = r.getNameBytes();
        Assert.that (nameBytes[0] == 65);
    } 
    */
}

