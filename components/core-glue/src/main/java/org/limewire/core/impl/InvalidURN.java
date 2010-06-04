package org.limewire.core.impl;


public final class InvalidURN implements org.limewire.common.URN {
    
    public static InvalidURN instance = new InvalidURN();
    
    private InvalidURN() {
        //singleton
    }
    
    @Override
    public int compareTo(org.limewire.common.URN o) {
        return toString().compareTo(o.toString());
    }
    
    @Override
    public String toString() {
        return "Unknown URN";
    }
}