package com.limegroup.bittorrent.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.Assert;

import org.limewire.collection.NECallable;
import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.observer.IOErrorObserver;
import org.limewire.util.BaseTestCase;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTMessageHandler;
import com.limegroup.bittorrent.BTPiece;
import com.limegroup.bittorrent.messages.BTCancel;
import com.limegroup.bittorrent.messages.BTChoke;
import com.limegroup.bittorrent.messages.BTHave;
import com.limegroup.bittorrent.messages.BTInterested;
import com.limegroup.bittorrent.messages.BTMessage;
import com.limegroup.bittorrent.messages.BTNotInterested;
import com.limegroup.bittorrent.messages.BTPieceMessage;
import com.limegroup.bittorrent.messages.BTRequest;
import com.limegroup.bittorrent.messages.BTUnchoke;

public class BTMessageReaderTest extends BaseTestCase {

    private BTMessage parsedMessage = null;

    public BTMessageReaderTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        parsedMessage = null;
        super.setUp();
    }

    public void testBTCancel() throws Exception {

        int startIndex = 0;
        int endIndex = 16;
        int pieceIndex = 0;
        int length = endIndex - startIndex + 1;

        final ByteBuffer payload = buildPayload(startIndex, pieceIndex, length);
        payload.clear();

        final ByteBuffer message = buildMessage(13, BTMessage.CANCEL, payload);
        message.clear();
        payload.clear();

        BTMessageReader messageReader = runTest(message);
        messageReader.handleRead();
        messageReader.dataConsumed(true);

        Assert.assertTrue(parsedMessage instanceof BTCancel);
        BTCancel btMessage = (BTCancel) parsedMessage;
        assertMessage(BTMessage.CANCEL, btMessage, payload);

    }

    public void testBTRequest() throws Exception {

        int startIndex = 0;
        int endIndex = 16;
        int pieceIndex = 0;
        int length = endIndex - startIndex + 1;

        final ByteBuffer payload = buildPayload(startIndex, pieceIndex, length);
        payload.clear();

        final ByteBuffer message = buildMessage(13, BTMessage.REQUEST, payload);
        message.clear();
        payload.clear();

        BTMessageReader messageReader = runTest(message);
        messageReader.handleRead();
        messageReader.dataConsumed(true);

        Assert.assertTrue(parsedMessage instanceof BTRequest);
        BTRequest btMessage = (BTRequest) parsedMessage;
        assertMessage(BTMessage.REQUEST, btMessage, payload);

    }

    public void testBTChoke() throws Exception {

        final ByteBuffer payload = ByteBuffer.allocate(0);
        payload.clear();

        final ByteBuffer message = buildMessage(1, BTMessage.CHOKE, payload);
        message.clear();
        payload.clear();

        BTMessageReader messageReader = runTest(message);
        messageReader.handleRead();
        messageReader.dataConsumed(true);

        Assert.assertTrue(parsedMessage instanceof BTChoke);
        BTChoke btMessage = (BTChoke) parsedMessage;
        assertMessage(BTMessage.CHOKE, btMessage, payload);

    }

    public void testBTUnChoke() throws Exception {

        final ByteBuffer payload = ByteBuffer.allocate(0);
        payload.clear();

        final ByteBuffer message = buildMessage(1, BTMessage.UNCHOKE, payload);
        message.clear();
        payload.clear();

        BTMessageReader messageReader = runTest(message);
        messageReader.handleRead();
        messageReader.dataConsumed(true);

        Assert.assertTrue(parsedMessage instanceof BTUnchoke);
        BTUnchoke btMessage = (BTUnchoke) parsedMessage;
        assertMessage(BTMessage.UNCHOKE, btMessage, payload);

    }

    public void testBTInterested() throws Exception {

        final ByteBuffer payload = ByteBuffer.allocate(0);
        payload.clear();

        final ByteBuffer message = buildMessage(1, BTMessage.INTERESTED, payload);
        message.clear();
        payload.clear();

        BTMessageReader messageReader = runTest(message);
        messageReader.handleRead();
        messageReader.dataConsumed(true);

        Assert.assertTrue(parsedMessage instanceof BTInterested);
        BTInterested btMessage = (BTInterested) parsedMessage;
        assertMessage(BTMessage.INTERESTED, btMessage, payload);

    }

    public void testBTNotInterested() throws Exception {

        final ByteBuffer payload = ByteBuffer.allocate(0);
        payload.clear();

        final ByteBuffer message = buildMessage(1, BTMessage.NOT_INTERESTED, payload);
        message.clear();
        payload.clear();

        BTMessageReader messageReader = runTest(message);
        messageReader.handleRead();
        messageReader.dataConsumed(true);

        Assert.assertTrue(parsedMessage instanceof BTNotInterested);
        BTNotInterested btMessage = (BTNotInterested) parsedMessage;
        assertMessage(BTMessage.NOT_INTERESTED, btMessage, payload);

    }

    public void testBTHave() throws Exception {

        int pieceIndex = 0;
        final ByteBuffer payload = ByteBuffer.allocate(4);
        payload.putInt(pieceIndex);
        payload.clear();

        final ByteBuffer message = buildMessage(5, BTMessage.HAVE, payload);
        message.clear();
        payload.clear();

        BTMessageReader messageReader = runTest(message);
        messageReader.handleRead();
        messageReader.dataConsumed(true);

        Assert.assertTrue(parsedMessage instanceof BTHave);
        BTHave btMessage = (BTHave) parsedMessage;
        assertMessage(BTMessage.HAVE, btMessage, payload);
        Assert.assertEquals(pieceIndex, btMessage.getPieceNum());

    }

    public void testBTPieceMessage() throws Exception {
        //TODO find out how to test
//        byte[] data = { 1, 2, 3, 4, 5, 6 };
//        int pieceIndex = 0;
//        int offset = 0;
//        final ByteBuffer payload = ByteBuffer.allocate(8 + data.length);
//        payload.putInt(pieceIndex);
//        payload.putInt(offset);
//        payload.put(data);
//        payload.clear();
//
//        final ByteBuffer message = buildMessage(9 + data.length, BTMessage.PIECE, payload);
//        message.clear();
//        payload.clear();
//
//        BTMessageReader messageReader = runTest(message);
//        messageReader.handleRead();
//        messageReader.dataConsumed(true);
//
//        
//        //TODO does this jsut call finish receiving peice instead?
//        Assert.assertTrue(parsedMessage instanceof BTPieceMessage);
//        BTPieceMessage btMessage = (BTPieceMessage) parsedMessage;
//        assertMessage(BTMessage.PIECE, btMessage, payload);
//        BTInterval btInterval = btMessage.getInterval();
//        Assert.assertEquals(new BTInterval(0, data.length - 1, pieceIndex), btInterval);
//        Assert.assertEquals(data, btMessage.getData());

    }

    private ByteBuffer buildMessage(int length, byte messageType, final ByteBuffer payload) {
        final ByteBuffer message = ByteBuffer.allocate(5 + payload.limit());
        message.order(ByteOrder.BIG_ENDIAN);
        message.putInt(length);
        message.put(messageType);
        message.put(payload);
        return message;
    }

    private ByteBuffer buildPayload(int startIndex, int pieceIndex, int length) {
        final ByteBuffer payload = ByteBuffer.allocate(12);
        payload.order(ByteOrder.BIG_ENDIAN);
        payload.putInt(pieceIndex);
        payload.putInt(startIndex);
        payload.putInt(length);
        return payload;
    }

    private void assertMessage(byte messageType, BTMessage message, final ByteBuffer testPayload) {
        Assert.assertEquals(messageType, message.getType());
        Assert.assertEquals(testPayload, message.getPayload());
    }

    private BTMessageReader runTest(final ByteBuffer testBuffer) {
        BTMessageReader messageReader = new BTMessageReader(new IOErrorObserver() {

            public void handleIOException(IOException iox) {
                System.out.println("handleIOException");
            }

            public void shutdown() {
                System.out.println("shutdown");

            }
        }, new BTMessageHandler() {

            public void finishReceivingPiece() {
                System.out.println("finishReceivingPiece");
            }

            public void handlePiece(NECallable<BTPiece> factory) {
                System.out.println("handlePiece");

            }

            public void processMessage(BTMessage message) {
                parsedMessage = message;

                System.out.println("processMessage");
            }

            public void readBytes(int read) {
                System.out.println("readBytes");
            }

            public boolean startReceivingPiece(BTInterval piece) {
                System.out.println("startReceivingPiece");
                return false;
            }
        }, new ScheduledExecutorService() {

            public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
                System.out.println("schedule");
                return null;
            }

            public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
                System.out.println("schedule");
                return null;
            }

            public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay,
                    long period, TimeUnit unit) {
                System.out.println("scheduleAtFixedRate");
                return null;
            }

            public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
                    long delay, TimeUnit unit) {
                System.out.println("scheduleWithFixedDelay");
                return null;
            }

            public boolean awaitTermination(long arg0, TimeUnit arg1) throws InterruptedException {
                System.out.println("awaitTermination");
                return false;
            }

            public <T> List<Future<T>> invokeAll(Collection<Callable<T>> arg0)
                    throws InterruptedException {
                System.out.println("invokeAll");
                return null;
            }

            public <T> List<Future<T>> invokeAll(Collection<Callable<T>> arg0, long arg1,
                    TimeUnit arg2) throws InterruptedException {
                System.out.println("invokeAll");
                return null;
            }

            public <T> T invokeAny(Collection<Callable<T>> arg0) throws InterruptedException,
                    ExecutionException {
                System.out.println("invokeAny");
                return null;
            }

            public <T> T invokeAny(Collection<Callable<T>> arg0, long arg1, TimeUnit arg2)
                    throws InterruptedException, ExecutionException, TimeoutException {
                System.out.println("invokeAny");
                return null;
            }

            public boolean isShutdown() {
                System.out.println("isShutdown");
                return false;
            }

            public boolean isTerminated() {
                System.out.println("isTerminated");
                return false;
            }

            public void shutdown() {
                System.out.println("shutdown");

            }

            public List<Runnable> shutdownNow() {
                System.out.println("shutdownNow");
                return null;
            }

            public <T> Future<T> submit(Callable<T> arg0) {
                System.out.println("submit");
                return null;
            }

            public Future<?> submit(Runnable arg0) {
                System.out.println("submit");
                return null;
            }

            public <T> Future<T> submit(Runnable arg0, T arg1) {
                System.out.println("submit");
                return null;
            }

            public void execute(Runnable arg0) {
                System.out.println("execute");
                arg0.run();

            }
        }, new ByteBufferCache());

        messageReader.setReadChannel(new InterestReadableByteChannel() {
            public void interestRead(boolean status) {
                System.out.println("interestRead");

            }

            public int read(ByteBuffer arg0) throws IOException {
                arg0.put(testBuffer);
                System.out.println("read");
                return 0;
            }

            public void close() throws IOException {
                System.out.println("close");

            }

            public boolean isOpen() {
                System.out.println("isOpen");
                return false;
            }
        });
        return messageReader;
    }

}
