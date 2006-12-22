package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Properties;

import junit.framework.Test;

import com.limegroup.gnutella.connection.ReadBufferChannel;
import com.limegroup.gnutella.connection.WriteBufferChannel;
import com.limegroup.gnutella.io.ChannelReadObserver;
import com.limegroup.gnutella.io.ChannelWriter;
import com.limegroup.gnutella.io.InterestReadChannel;
import com.limegroup.gnutella.io.InterestWriteChannel;
import com.limegroup.gnutella.io.NIOMultiplexor;
import com.limegroup.gnutella.io.ReadObserver;
import com.limegroup.gnutella.io.WriteObserver;
import com.limegroup.gnutella.util.LimeTestCase;

public class AsyncOutgoingHandshakerTest extends LimeTestCase {
    
    public AsyncOutgoingHandshakerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AsyncOutgoingHandshakerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testSimpleSuccess() throws Exception {
        ReadBufferChannel reader = new ReadBufferChannel(
                ("GNUTELLA/0.6 200 OK DOKIE\r\n" +
                 "ResponseHeader: ResponseValue\r\n" +
                 "\r\n").getBytes());
        WriteBufferChannel writer = new WriteBufferChannel(2048);
        MultiplexingSocket socket = new MultiplexingSocket(reader, writer);
        Properties outRequestProps = new Properties();
        outRequestProps.put("OutRequest", "OutRequestValue");
        Properties outResponseProps = new Properties();
        outResponseProps.put("OutResponse", "OutResponseValue");
        StubHandshakeResponder responder = new StubHandshakeResponder(new StubHandshakeResponse(200, "OK!", outResponseProps));
        StubHandshakeObserver observer = new StubHandshakeObserver();
        Handshaker shaker = new AsyncOutgoingHandshaker(outRequestProps, responder, socket, observer);
        shaker.shake();
        
        socket.exchange(); // simulates NIODispatcher setting interest

        assertFalse(observer.isNoGOK());
        assertFalse(observer.isBadHandshake());
        assertTrue(observer.isHandshakeFinished());
        assertEquals(shaker, observer.getShaker());     
        
        HandshakeResponse responseTo = responder.getRespondedTo();
        Map respondedTo = responder.getRespondedToProps();
        assertEquals(1, respondedTo.size());
        assertEquals("ResponseValue", respondedTo.get("ResponseHeader"));
        assertEquals(200, responseTo.getStatusCode());
        assertEquals("OK DOKIE", responseTo.getStatusMessage());
        
        assertTrue(responder.isOutgoing());
        
        HandshakeResponse read = shaker.getReadHeaders();
        assertEquals(1, read.props().size());
        assertEquals("ResponseValue", read.props().get("ResponseHeader"));
        
        HandshakeResponse written = shaker.getWrittenHeaders();
        assertEquals(2, written.props().size());
        assertEquals("OutRequestValue", written.props().get("OutRequest"));
        assertEquals("OutResponseValue", written.props().get("OutResponse"));
        ByteBuffer buffer = writer.getBuffer();
        assertEquals("GNUTELLA CONNECT/0.6\r\nOutRequest: OutRequestValue\r\n\r\n" +
                     "GNUTELLA/0.6 200 OK!\r\nOutResponse: OutResponseValue\r\n\r\n",
                     new String(buffer.array(), 0, buffer.limit()));
    }

    public void testDiscoOnBadResponder() throws Exception {
        ReadBufferChannel reader = new ReadBufferChannel(
                ("GNUTELLA/0.6 200 OK DOKIE\r\n" +
                 "ResponseHeader: ResponseValue\r\n" +
                 "\r\n").getBytes());
        WriteBufferChannel writer = new WriteBufferChannel(2048);
        MultiplexingSocket socket = new MultiplexingSocket(reader, writer);
        Properties outRequestProps = new Properties();
        outRequestProps.put("OutRequest", "OutRequestValue");
        Properties outResponseProps = new Properties();
        outResponseProps.put("OutResponse", "OutResponseValue");
        StubHandshakeResponder responder = new StubHandshakeResponder(new StubHandshakeResponse(322, "AARGH!", outResponseProps));
        StubHandshakeObserver observer = new StubHandshakeObserver();
        Handshaker shaker = new AsyncOutgoingHandshaker(outRequestProps, responder, socket, observer);
        shaker.shake();
        
        socket.exchange(); // simulates NIODispatcher setting interest
        
        assertTrue(observer.isNoGOK());
        assertEquals(322, observer.getCode());
        assertFalse(observer.isBadHandshake());
        assertFalse(observer.isHandshakeFinished());
        assertNull(observer.getShaker());        
        
        HandshakeResponse responseTo = responder.getRespondedTo();
        Map respondedTo = responder.getRespondedToProps();
        assertEquals(1, respondedTo.size());
        assertEquals("ResponseValue", respondedTo.get("ResponseHeader"));
        assertEquals(200, responseTo.getStatusCode());
        assertEquals("OK DOKIE", responseTo.getStatusMessage());
        
        assertTrue(responder.isOutgoing());
        
        HandshakeResponse read = shaker.getReadHeaders();
        assertEquals(1, read.props().size());
        assertEquals("ResponseValue", read.props().get("ResponseHeader"));
        
        HandshakeResponse written = shaker.getWrittenHeaders();
        assertEquals(2, written.props().size());
        assertEquals("OutRequestValue", written.props().get("OutRequest"));
        assertEquals("OutResponseValue", written.props().get("OutResponse"));
        ByteBuffer buffer = writer.getBuffer();
        assertEquals("GNUTELLA CONNECT/0.6\r\nOutRequest: OutRequestValue\r\n\r\n" +
                     "GNUTELLA/0.6 322 AARGH!\r\nOutResponse: OutResponseValue\r\n\r\n",
                     new String(buffer.array(), 0, buffer.limit()));
    }
    
    public void testDiscoOnBadResponseCode() throws Exception {
        ReadBufferChannel reader = new ReadBufferChannel(
                ("GNUTELLA/0.6 544 SHUCKS\r\n" +
                 "ResponseHeader: ResponseValue\r\n" +
                 "\r\n").getBytes());
        WriteBufferChannel writer = new WriteBufferChannel(2048);
        MultiplexingSocket socket = new MultiplexingSocket(reader, writer);
        Properties outRequestProps = new Properties();
        outRequestProps.put("OutRequest", "OutRequestValue");
        StubHandshakeResponder responder = new StubHandshakeResponder();
        StubHandshakeObserver observer = new StubHandshakeObserver();
        Handshaker shaker = new AsyncOutgoingHandshaker(outRequestProps, responder, socket, observer);
        shaker.shake();
        
        socket.exchange(); // simulates NIODispatcher setting interest
        
        assertTrue(observer.isNoGOK());
        assertEquals(544, observer.getCode());
        assertFalse(observer.isBadHandshake());
        assertFalse(observer.isHandshakeFinished());
        assertNull(observer.getShaker());
        
        HandshakeResponse responseTo = responder.getRespondedTo();
        assertNull(responseTo);
        
        HandshakeResponse read = shaker.getReadHeaders();
        assertEquals(1, read.props().size());
        assertEquals("ResponseValue", read.props().get("ResponseHeader"));
        
        HandshakeResponse written = shaker.getWrittenHeaders();
        assertEquals(1, written.props().size());
        assertEquals("OutRequestValue", written.props().get("OutRequest"));
        ByteBuffer buffer = writer.getBuffer();
        assertEquals("GNUTELLA CONNECT/0.6\r\nOutRequest: OutRequestValue\r\n\r\n",
                     new String(buffer.array(), 0, buffer.limit())); 
    }

    public void testDiscoOnBadResponseConnectLine() throws Exception {
        ReadBufferChannel reader = new ReadBufferChannel(
                ("HTTP/1.1 345 GET OFF\r\n" +
                 "ResponseHeader: ResponseValue\r\n" +
                 "\r\n").getBytes());
        WriteBufferChannel writer = new WriteBufferChannel(2048);
        MultiplexingSocket socket = new MultiplexingSocket(reader, writer);
        Properties outRequestProps = new Properties();
        outRequestProps.put("OutRequest", "OutRequestValue");
        StubHandshakeResponder responder = new StubHandshakeResponder();
        StubHandshakeObserver observer = new StubHandshakeObserver();
        Handshaker shaker = new AsyncOutgoingHandshaker(outRequestProps, responder, socket, observer);
        shaker.shake();
        
        socket.exchange(); // simulates NIODispatcher setting interest
        
        assertFalse(observer.isNoGOK());
        assertTrue(observer.isBadHandshake());
        assertFalse(observer.isHandshakeFinished());
        assertNull(observer.getShaker());         
        
        HandshakeResponse responseTo = responder.getRespondedTo();
        assertNull(responseTo);
        
        HandshakeResponse read = shaker.getReadHeaders();
        assertEquals(0, read.props().size());
        
        HandshakeResponse written = shaker.getWrittenHeaders();
        assertEquals(1, written.props().size());
        assertEquals("OutRequestValue", written.props().get("OutRequest"));
        ByteBuffer buffer = writer.getBuffer();
        assertEquals("GNUTELLA CONNECT/0.6\r\nOutRequest: OutRequestValue\r\n\r\n",
                     new String(buffer.array(), 0, buffer.limit()));   
    }

    private static class MultiplexingSocket extends Socket implements NIOMultiplexor {
        private InterestReadChannel baseReader;
        private InterestWriteChannel baseWriter;
        private ReadObserver reader;
        private WriteObserver writer;
        
        MultiplexingSocket(InterestReadChannel baseReader, InterestWriteChannel baseWriter) {
            this.baseReader = baseReader;
            this.baseWriter = baseWriter;
        }
        
        public InetAddress getInetAddress() {
            try {
                return InetAddress.getByName("127.0.0.1");
            } catch(IOException iox) {
                return null;
            }
        }

        public void setReadObserver(ChannelReadObserver reader) {
            reader.setReadChannel(baseReader);
            this.reader = reader; 
        }

        public void setWriteObserver(ChannelWriter writer) {
            writer.setWriteChannel(baseWriter);
            this.writer = writer;
        }
        
        public void exchange() throws IOException {
            writer.handleWrite();
            reader.handleRead();
            writer.handleWrite();
        }
    }
    
    
}
