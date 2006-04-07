package com.limegroup.gnutella.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;

import com.limegroup.gnutella.statistics.Statistic;

public class SimpleWriteHeaderState extends WriteHeadersIOState {
    
    private final String connectLine;
    private final Map headers;

    public SimpleWriteHeaderState(String connectLine, Map headers, Statistic stat) {
        super(stat);
        this.connectLine = connectLine;
        this.headers = headers;
    }

    protected ByteBuffer createOutgoingData() throws IOException {
        StringBuffer sb = new StringBuffer(connectLine.length() + headers.size() * 25);
        sb.append(connectLine).append("\r\n");
        for(Iterator i = headers.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry next = (Map.Entry)i.next();
            Object header = next.getKey();
            Object value  = next.getValue();
            if(header instanceof String && value instanceof String)
                sb.append(HTTPUtils.createHeader((String)header, (String)value));
            else if(header instanceof HTTPHeaderName && value instanceof String)
                sb.append(HTTPUtils.createHeader((HTTPHeaderName)header, (String)value));
            else if(header instanceof HTTPHeaderName && value instanceof HTTPHeaderValue)
                sb.append(HTTPUtils.createHeader((HTTPHeaderName)header, (HTTPHeaderValue)value));
            else if(header instanceof String && value instanceof HTTPHeaderValue)
                sb.append(HTTPUtils.createHeader((String)header, (HTTPHeaderValue)value));
            else
                throw new IllegalArgumentException("bad header: " + header + ", value: " + value);
        }
        sb.append("\r\n");
        return ByteBuffer.wrap(sb.toString().getBytes()); // TODO: conversion?
    }

    protected void processWrittenHeaders() throws IOException {
        // does nothing.
    }

}
