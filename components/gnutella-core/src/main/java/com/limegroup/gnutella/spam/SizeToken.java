package com.limegroup.gnutella.spam;

/**
 * A token representing the file size
 */
public class SizeToken extends Token {
	private static final long serialVersionUID = 3906652994404955696L;

    /** 
     * File size is a fairly accurate identifier of a file, so we will
     * consider a certain file size spam after only a couple of bad ratings.
     */
    private static final float SIZE_WEIGHT = 0.6f;
    
	private final long size;
    
	public SizeToken(long size) {
		this.size = size;
	}
    
    @Override
    protected float getWeight() {
        return SIZE_WEIGHT;
    }
    
    @Override
    public final int hashCode() {
        return (int)size;
    }
    
    @Override
    public final boolean equals(Object o) {
        if(o == null)
            return false;
        if(!(o instanceof SizeToken))
            return false;
        return size == ((SizeToken)o).size;
    }
    
	@Override
    public String toString() {
		return "size " + size;
	}
}
