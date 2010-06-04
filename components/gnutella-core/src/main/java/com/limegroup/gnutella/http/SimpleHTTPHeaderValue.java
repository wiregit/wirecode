package com.limegroup.gnutella.http;

import org.limewire.common.HTTPHeaderValue;

public class SimpleHTTPHeaderValue implements HTTPHeaderValue {
    
    private final String value;

    public SimpleHTTPHeaderValue(String value) {
        this.value = value;
    }

    public String httpStringValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return httpStringValue();
    }

}
