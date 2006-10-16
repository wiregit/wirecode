package com.limegroup.gnutella.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import com.limegroup.gnutella.statistics.Statistic;

public class SimpleWriteHeaderState extends WriteHeadersIOState {
    
    private final String connectLine;
    private final Map<? extends HTTPHeaderName, ? extends HTTPHeaderValue> headers;

    public SimpleWriteHeaderState(String connectLine,
                                  Map<? extends HTTPHeaderName, ? extends HTTPHeaderValue> headers,
                                  Statistic stat) {
        super(stat);
        this.connectLine = connectLine;
        this.headers = headers;
    }

    protected ByteBuffer createOutgoingData() throws IOException {
        StringBuilder sb = new StringBuilder(connectLine.length() + headers.size() * 25);
        sb.append(connectLine).append("\r\n");
        for(Map.Entry<? extends HTTPHeaderName, ? extends HTTPHeaderValue> entry : headers.entrySet())
            sb.append(HTTPUtils.createHeader(entry.getKey(), entry.getValue()));
        sb.append("\r\n");
        return ByteBuffer.wrap(sb.toString().getBytes()); // TODO: conversion?
    }

    protected void processWrittenHeaders() throws IOException {
        // does nothing.
    }

}
