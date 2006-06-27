package com.limegroup.gnutella.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;

import com.limegroup.gnutella.statistics.Statistic;

public class SimpleWriteHeaderState extends WriteHeadersIOState {
    
    private final String connectLine;
    private final Map<HTTPHeaderName, HTTPHeaderValue> headers;

    public SimpleWriteHeaderState(String connectLine, Map<HTTPHeaderName, HTTPHeaderValue> headers, Statistic stat) {
        super(stat);
        this.connectLine = connectLine;
        this.headers = headers;
    }

    protected ByteBuffer createOutgoingData() throws IOException {
        StringBuffer sb = new StringBuffer(connectLine.length() + headers.size() * 25);
        sb.append(connectLine).append("\r\n");
        for(Map.Entry<HTTPHeaderName, HTTPHeaderValue> entry : headers.entrySet())
            sb.append(HTTPUtils.createHeader(entry.getKey(), entry.getValue()));
        sb.append("\r\n");
        return ByteBuffer.wrap(sb.toString().getBytes()); // TODO: conversion?
    }

    protected void processWrittenHeaders() throws IOException {
        // does nothing.
    }

}
