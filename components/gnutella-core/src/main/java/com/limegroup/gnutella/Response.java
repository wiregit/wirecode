package com.limegroup.gnutella;

import java.util.StringTokenizer;
import com.limegroup.gnutella.xml.*;

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


    /**
     * This class specifies and invariant, that if the metadata is set,
     * the value of metaBytes must also be set.
     */
    
	/** The meta variable is a string of meta information that
	 *  may be added per response (as opposed to per QueryReply)
	 */
	private String metadata;

    /** The bytes of the metadata instance variable.  There is some special
     *  processing needed here to deal with internationalization....
     */
    private byte[] metaBytes;

    /** The document representing the XML in this response.
     */
    private LimeXMLDocument document;

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
        this.nameBytes=name.getBytes();
		metadata="";
        this.metaBytes=metadata.getBytes();
    }


    /**
     * This constructor allows for the association of this Response with a
     * LimeXMLDocument (which presumably contains the responses metadata).
     */
    public Response(long index, long size, String name, LimeXMLDocument doc) {
        this(index,size,name);
        if(doc != null)
            this.document=doc;        
    }


    /**Overloaded constructor that allows the creation of Responses with
     * meta-data
     */
    public Response(long index, long size, String name,String metadata) {
        this(index,size,name);
        if(metadata != null && !metadata.equals("")) {
            this.metadata=metadata;
            this.metaBytes=metadata.getBytes();
        }
    }

    /**
     * Overloaded constructor that picks up data from between the  nulls
     * That data is then made into a nice xml string that can 
     * be converted into a LimeXMLDocument
     */
    public Response(String betweenNulls, long index, long size, String name){
        this(index,size,name);
        //create an XML string out of the data between the nulls
        //System.out.println("Between nulls is "+betweenNulls);
        String length="";
        String bitrate="";
        String first="";
        String second="";
        StringTokenizer tok=new StringTokenizer(betweenNulls);
        try{
            first=tok.nextToken();
            second=tok.nextToken();
        }catch(Exception e){//If I catch an exception, all bets are off.
            first="";
            second="";
            betweenNulls="";
        }
        boolean bearShare1=false;        
        boolean bearShare2=false;
        boolean gnotella=false;
        if(second.startsWith("kbps"))
            bearShare1=true;
        else if (first.endsWith("kbps"))
            bearShare2=true;
        if(bearShare1){
            bitrate=first;
        }
        else if (bearShare2){
            int j=first.indexOf("kbps");
            bitrate=first.substring(0,j);
        }
        if(bearShare1 || bearShare2){
            while(tok.hasMoreTokens())
                length=tok.nextToken();
            //OK we have the bitrate and the length
        }
        else if (betweenNulls.endsWith("kHz")){//Gnotella
            gnotella=true;
            length=first;
            //extract the bitrate from second
            int i=second.indexOf("kbps");
            if(i>-1)//see if we can find the bitrate                
                bitrate=second.substring(0,i);
            else//not gnotella, after all...some other format we do not know
                gnotella=false;
        }
        if(bearShare1 || bearShare2 || gnotella){//some metadata we understand
            this.metadata="<audios xsi:noNamespaceSchemaLocation="+
                 "\"http://www.limewire.com/schemas/audio.xsd\">"+
                 "<audio title=\""+name+"\" bitrate=\""+bitrate+
                 "\" seconds=\""+length+"\">"+
                 "</audio></audios>";
            this.metaBytes=metadata.getBytes();
        }
        else{
            this.metadata="";
            this.metaBytes=metadata.getBytes();
        }
    }

    /**
     * To add metaData to a response after it has been created.
     * Added to faciliatate setting audio metadata for responses
     * generated from ordinary searches. 
     */
    private void setMetadata(String meta){
        this.metadata=meta;
        this.metaBytes=meta.getBytes();
    }
    

    /**
     * To add a XMLDOC to a response after it has been created.
     * Added to faciliatate setting audio metadata for responses
     * generated from ordinary searches. 
     */
    public void setDocument(LimeXMLDocument doc) {
        this.document=doc;
    }

    public long getIndex() {
        return index;
    }

    public long getSize() {
        return size;
    }

    /**
     * lazily returns the LimeXMLDocument if possible. Returns null if all 
     * fails 
     */
    public LimeXMLDocument getDocument() {
        if (document != null) 
            return document;
        else if (metadata != null) {
            try {
                document = new LimeXMLDocument(metadata);
            }
            catch (Exception e) {}
            return document;
        }
        return null;//both document and metadata are null
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
        if(metaBytes != null)
            return metaBytes;
        else { //this condition means that metadata==null. See invariant above
            getMetadata(); //got the metadata and the metaBytes.
            return metaBytes;
        }
    }

    public String getName() {
        return name;
    }

    /**
     * checks if the string is known, otherwise tries to find out (lazily) 
     * from the document. 
     * <p>
     * retruns null if cannot calculate the string
     */
	public String getMetadata() {
        if (metadata != null && !metadata.equals(""))
            return metadata;
        else if (document != null) {
            try {
                metadata=document.getXMLString();
                if (metadata != null) {
                    if(!metadata.equals(""))
                        metaBytes=metadata.getBytes();
                    else //metadata == ""
                        metadata = null; //reset it
                }
            } 
            catch (Exception e) {
                e.printStackTrace();
                metadata = null;
                metaBytes = null;
            }
            return metadata;
        }
        return null;//metadata and document are both set to null
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
            LimeXMLDocument d=null;
            String xml = r4.getMetadata();
            try{
                d = new LimeXMLDocument(xml);
            }catch (Exception e){
                Assert.that(false,"XML not created well from between nulls");
            }
            String br = d.getValue("audios__audio__bitrate__");
            Assert.that(br.equals("a"));
            String len = d.getValue("audios__audio__seconds__");
            Assert.that(len.equals("b"));
        }
        //Tests for checking new LimeXMLDocument code added.
        LimeXMLSchemaRepository rep = LimeXMLSchemaRepository.instance();

        String xml1 = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio genre=\"Speech\" bitrate=\"192\"></audio></audios>";
        
        String xml2 = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio genre=\"Speech\" bitrate=\"150\"></audio></audios>";
        
        //create documents.
        LimeXMLDocument d1 = null;
        LimeXMLDocument d2 = null;
        try {
            d1 = new LimeXMLDocument(xml1);
            d2 = new LimeXMLDocument(xml2);
        } catch (Exception stop) {
            System.out.println("Warning: Test is incorrect");
            System.exit(1);
        }//not the Responses fault.
        Response ra = new Response(12,231,"def1.txt",d1);
        Response rb = new Response(13,232,"def2.txt",d2);
        Assert.that(ra.getDocument() == d1, "problem with doc constructor");
        Assert.that(rb.getDocument() == d2, "problem with doc constructor");
        
        Assert.that(ra.getMetadata().equals(xml1),
                    "mismatched strings"+ra.getMetadata()+", "+xml1);
        Assert.that(rb.getMetadata().equals(xml2),
                    "mismatched strings"+rb.getMetadata()+", "+xml2);
    } 
}

