package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.WritableByteChannel;
import java.util.Properties;

import com.limegroup.gnutella.statistics.BandwidthStat;
import com.limegroup.gnutella.statistics.HandshakingStat;

/** Superclass for HandshakeStates that are written out. */
public abstract class WriteHandshakeState extends HandshakeState {
    /** The outgoing buffer, if we've made it.  (Null if we haven't.) */
    private ByteBuffer outgoing;

    /** Creates a new WriteHandshakeState using the given support. */
    public WriteHandshakeState(HandshakeSupport support) {
        super(support);
    }

    /** Returns true. */
    boolean isWriting() {
        return true;
    }

    /** Returns false. */
    boolean isReading() {
        return false;
    }

    /**
     * Writes output to the channel.  This farms out the creation of the output
     * to the abstract method createOutgoingData().  That method will only be called once
     * to get the initial outgoing data.  Once all data has been written, the abstract
     * processWrittenHeaders() method will be called, so that subclasses can act upon
     * what they've just written.
     * 
     * This will return true if it needs to be called again to continue writing.
     * If it returns false, all data has been written and you can proceed to the next state.
     */
    boolean process(Channel channel, ByteBuffer buffer) throws IOException {
        if(outgoing == null) {
            outgoing = createOutgoingData();
        }
        
        int written = ((WritableByteChannel)channel).write(outgoing);
        BandwidthStat.GNUTELLA_HEADER_UPSTREAM_BANDWIDTH.addData(written);
        
        if(!outgoing.hasRemaining()) {
            processWrittenHeaders();
            return false;
        } else {
            return true;
        }
    }
    
    /** Returns a ByteBuffer of data to write. */
    protected abstract ByteBuffer createOutgoingData() throws IOException;
    
    /** Processes the headers we wrote, after writing them.  May throw IOException if we need to disco. */
    protected abstract void processWrittenHeaders() throws IOException;

    /** The second state in an incoming handshake, or the third state in an outgoing handshake. */
    static class WriteResponseState extends WriteHandshakeState {
        private HandshakeResponder responder;
        private HandshakeResponse response;
        private boolean outgoing;
        
        /**
         * Constructs a new WriteResponseState using the given support, responder,
         * and whether or not we're responding to an outgoing or incoming request.
         * 
         * @param support
         * @param responder
         * @param outgoing
         */
        WriteResponseState(HandshakeSupport support, HandshakeResponder responder, boolean outgoing) {
            super(support);
            this.responder = responder;
            this.outgoing = outgoing;
        }

        /**
         * Creates a response using the responder and wraps it into a ByteBuffer.
         */
        protected ByteBuffer createOutgoingData() throws IOException {
            // The distinction between requests is not necessary for correctness,
            // but is useful.  The getReadHandshakeRemoteResponse() method will
            // contain the correct response status code & msg, whereas
            // the getReadHandshakeResponse() method assumes '200 OK'.
            HandshakeResponse theirResponse;
            if(outgoing)
                theirResponse = support.getReadHandshakeRemoteResponse();
            else
                theirResponse = support.getReadHandshakeResponse();
            response = responder.respond(theirResponse, outgoing);
            StringBuffer sb = new StringBuffer();
            support.appendResponse(response, sb);
            return ByteBuffer.wrap(sb.toString().getBytes()); // TODO: conversion??
        }

        /**
         * Throws an IOException if we wrote a code other than 'OK'.
         * Increments the appropriate statistics also.
         */
        protected void processWrittenHeaders() throws IOException {
            if(outgoing) {
                switch(response.getStatusCode()) {
                case HandshakeResponse.OK:
                    HandshakingStat.SUCCESSFUL_OUTGOING.incrementStat();
                    break;
                case HandshakeResponse.SLOTS_FULL:
                    HandshakingStat.OUTGOING_CLIENT_REJECT.incrementStat();
                    throw NoGnutellaOkException.CLIENT_REJECT;
                case HandshakeResponse.LOCALE_NO_MATCH:
                    //if responder's locale preferencing was set 
                    //and didn't match the locale this code is used.
                    //(currently in use by the dedicated connectionfetcher)
                    throw NoGnutellaOkException.CLIENT_REJECT_LOCALE;
                default:
                    HandshakingStat.OUTGOING_CLIENT_UNKNOWN.incrementStat();
                    throw NoGnutellaOkException.createClientUnknown(response.getStatusCode());
                }
            } else {
               switch(response.getStatusCode()) {
                   case HandshakeResponse.OK:
                   case HandshakeResponse.CRAWLER_CODE: // let the crawler IOX in ReadResponse
                        break;
                    case HandshakeResponse.SLOTS_FULL:
                        HandshakingStat.INCOMING_CLIENT_REJECT.incrementStat();
                        throw NoGnutellaOkException.CLIENT_REJECT;
                    default: 
                        HandshakingStat.INCOMING_CLIENT_UNKNOWN.incrementStat();
                        throw NoGnutellaOkException.createClientUnknown(response.getStatusCode());
                }
            }
        }
    }
    
    /** The first state in an outgoing handshake. */
    static class WriteRequestState extends WriteHandshakeState {
        private Properties request;
        
        /** Creates a new WriteRequestState using the given support & initial set of properties. */
        WriteRequestState(HandshakeSupport support, Properties request) {
            super(support);
            this.request = request;
        }

        /** Returns a ByteBuffer of the initial connect request & headers. */
        protected ByteBuffer createOutgoingData() {
            StringBuffer sb = new StringBuffer();
            support.appendConnectLine(sb);
            support.appendHeaders(request, sb);
            return ByteBuffer.wrap(sb.toString().getBytes()); // TODO: conversion??
        }
        
        /** Does nothing. */
        protected void processWrittenHeaders() {}
    }
    
}
