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
    /** The name of the file matching the search.  This does NOT
     *  include the double null terminator.
     */
    private String name;
    private String metadata;

    /** Creates a fresh new response.
     *
     * @requires index and size can fit in 4 unsigned bytes, i.e.,
     *  0 <= index, size < 2^32
     */
    public Response(long index, long size, String name, String metadata) {
        Assert.that((index & 0xFFFFFFFF00000000l)==0,
                "Response constructor: index too big!");
        Assert.that((size &  0xFFFFFFFF00000000l)==0,
                "Response constructor: size too big!");

        this.index=index;
        this.size=size;
        this.name=name;
        this.metadata=metadata;
    }

    public long getIndex() {
        return index;
    }

    public long getSize() {
        return size;
    }

    public String getName() {
        return name;
    }

    public String getMetadata() {
        return metadata;
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
}
