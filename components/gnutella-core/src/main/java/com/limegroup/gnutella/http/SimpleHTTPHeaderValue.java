package com.limegroup.gnutella.http;

public class SimpleHTTPHeaderValue implements HTTPHeaderValue {
    
    private final String value;

    public SimpleHTTPHeaderValue(String value) {
        this.value = value;
    }

    public String httpStringValue() {
        return value;
    }
    
    public String toString() {
        return httpStringValue();
    }

}
