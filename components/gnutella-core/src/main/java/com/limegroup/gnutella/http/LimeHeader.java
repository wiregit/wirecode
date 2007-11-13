package com.limegroup.gnutella.http;

import org.apache.http.message.BasicHeader;

public class LimeHeader extends BasicHeader {

    private final HTTPHeaderName name;
    private final HTTPHeaderValue value;

    public LimeHeader(HTTPHeaderName name, HTTPHeaderValue value) {
        super(name.httpStringValue(), value.httpStringValue());
        this.name = name;
        this.value = value;
    }
    
    public HTTPHeaderName getHttpHeaderName() {
        return name;
    }
    
    public HTTPHeaderValue getHttpHeaderValue() {
        return value;
    }
}
