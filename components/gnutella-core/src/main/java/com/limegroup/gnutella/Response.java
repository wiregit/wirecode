package com.limegroup.gnutella;

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
	private String meta;
	

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
		meta = "";
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

    public byte[] getNameBytes() {
        return nameBytes;
    }

    public String getName() {
        return name;
    }

	public void setMeta(String m) {
		meta = m;
	}
	
	public String getMeta() {
		return meta;
	}


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
