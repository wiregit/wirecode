package org.limewire.core.impl.xmpp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.impl.xmpp.XMPPFirewalledAddressConnector.PushedSocketConnectObserver;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.GUID;
import org.limewire.net.SocketsManager;
import org.limewire.net.address.FirewalledAddress;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.client.impl.XMPPFirewalledAddress;

import com.limegroup.gnutella.downloader.PushDownloadManager;
import com.limegroup.gnutella.downloader.PushedSocketHandlerRegistry;

public class XMPPFirewalledAddressConnectorTest extends BaseTestCase {

    public XMPPFirewalledAddressConnectorTest(String name) {
        super(name);
    }

    /** 
     * Test that ensures the register methods will correctly link the connector to the sockets manager and socket
     *  handler register.
     */
    public void testRegister() {

        Mockery context = new Mockery();
        
        final SocketsManager socketsManager = context.mock(SocketsManager.class);
        final PushedSocketHandlerRegistry pushedSocketHandlerRegistry = context.mock(PushedSocketHandlerRegistry.class);
        
        final XMPPFirewalledAddressConnector connector 
            = new XMPPFirewalledAddressConnector(null, null, null, null, null, null);
        
        context.checking(new Expectations() {
            {   exactly(1).of(socketsManager).registerConnector(connector);
                exactly(1).of(pushedSocketHandlerRegistry).register(connector);
            }});
        
        connector.register(socketsManager);
        connector.register(pushedSocketHandlerRegistry);
        
        context.assertIsSatisfied();
        
    }
    
    /**
     * Tries the can connect function under various conditions.
     */
    public void testCanConnect() {
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }};
        
        final PushDownloadManager pushDownloadManager = context.mock(PushDownloadManager.class);
                
        final Address adressNotXmpp = context.mock(Address.class);
        final XMPPFirewalledAddress adressCantConnect = context.mock(XMPPFirewalledAddress.class);
        final XMPPFirewalledAddress adressCanConnect = context.mock(XMPPFirewalledAddress.class);
        
        final Address fwAddressCantConnect = context.mock(FirewalledAddress.class);
        final Address fwAddressCanConnect = context.mock(FirewalledAddress.class);
        
        final XMPPFirewalledAddressConnector connector 
            = new XMPPFirewalledAddressConnector(null, pushDownloadManager, null, null, null, null);
        
        context.checking(new Expectations() {
            {   allowing(adressCantConnect).getFirewalledAddress();
                will(returnValue(fwAddressCantConnect));
                allowing(adressCanConnect).getFirewalledAddress();
                will(returnValue(fwAddressCanConnect));
                
                atLeast(1).of(pushDownloadManager).canConnect(fwAddressCantConnect);
                will(returnValue(false));
                atLeast(1).of(pushDownloadManager).canConnect(fwAddressCanConnect);
                will(returnValue(true));
            }});
        
        assertFalse(connector.canConnect(adressNotXmpp));
        assertFalse(connector.canConnect(adressCantConnect));
        assertTrue(connector.canConnect(adressCanConnect));
        
        context.assertIsSatisfied();
        
    }
    
    /**
     * Ensure ready sockets are delegated to the correct observers 
     */
    public void testAcceptPushedSocket() throws IOException {
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }};
        
            
        final InetAddress inetAddress = context.mock(InetAddress.class);
            
        final ConnectObserver connectObserverA = context.mock(ConnectObserver.class);    
        final FirewalledAddress fwAddressA = context.mock(FirewalledAddress.class);
        final byte[] guidA = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                16 };
        final Socket socketA = context.mock(Socket.class);
        final Connectable connectableA = context.mock(Connectable.class);

        final ConnectObserver connectObserverB = context.mock(ConnectObserver.class);    
        final FirewalledAddress fwAddressB = context.mock(FirewalledAddress.class);
        final byte[] guidB = new byte[] { 1, 2, 3, 4, 5, 6, '\'', 'n', 9, 10, 11, 12, 13, 14, 15,
                16 };
        final Socket socketB = context.mock(Socket.class);
        final Connectable connectableB = context.mock(Connectable.class);
        
        final Socket socketAwithIOE = context.mock(Socket.class);
        final Socket socketAwithBadAddr = context.mock(Socket.class);
        final InputStream isWithIOE = context.mock(InputStream.class);
        final OutputStream osWithIOE = context.mock(OutputStream.class);
        
        final XMPPFirewalledAddressConnector connector 
            = new XMPPFirewalledAddressConnector(null, null, null, null, null, null);
        
        context.checking(new Expectations() {
            {
                // Non critical actions
                allowing(fwAddressA).getClientGuid();
                will(returnValue(new GUID(guidA)));
                allowing(fwAddressA).getPublicAddress();
                will(returnValue(connectableA));
                allowing(socketA).getInetAddress();
                will(returnValue(inetAddress));
                
                allowing(fwAddressB).getClientGuid();
                will(returnValue(new GUID(guidB)));
                allowing(fwAddressB).getPublicAddress();
                will(returnValue(connectableB));
                allowing(socketB).getInetAddress();
                will(returnValue(inetAddress));
                
                allowing(socketAwithBadAddr).getInetAddress();
                will(returnValue(null));
                
                allowing(socketAwithIOE).getInetAddress();
                will(returnValue(inetAddress));
                
                allowing(connectableA).getAddress();
                will(returnValue("127.0.0.1"));
                allowing(connectableA).getPort();
                will(returnValue(80));
                allowing(connectableA).getInetAddress();
                will(returnValue(inetAddress));
                
                allowing(connectableB).getAddress();
                will(returnValue("127.0.0.1"));
                allowing(connectableB).getPort();
                will(returnValue(80));
                allowing(connectableB).getInetAddress();
                will(returnValue(inetAddress));


                // The bad socket should be closed
                allowing(socketAwithIOE).getInputStream();
                will(returnValue(isWithIOE));
                allowing(socketAwithIOE).getOutputStream();
                will(returnValue(osWithIOE));
                exactly(1).of(socketAwithIOE).close();
                exactly(1).of(isWithIOE).close();
                exactly(1).of(osWithIOE).close();
                
                // Assertions
                
                // Forced for testing an IOE
                exactly(1).of(connectObserverA).handleConnect(socketAwithIOE);
                will(throwException(new IOException("forced exception")));
                exactly(1).of(connectObserverA).handleConnect(socketA);
                exactly(1).of(connectObserverB).handleConnect(socketB);
                
            }});
        
        // No observers have been set therefore nothing should be grabbed
        assertFalse(connector.acceptPushedSocket("blah", 10, guidA, socketA));
        
        connector.observers.add(createRandomObserver(context));
        connector.observers.add(createRandomObserver(context));
        
        // No matching observers have been set therefore nothing should be grabbed
        assertFalse(connector.acceptPushedSocket("blah", 10, guidA, socketA));
        
        PushedSocketConnectObserver pushedSocketConnectObserverA 
            = new PushedSocketConnectObserver(fwAddressA, connectObserverA);
        
        connector.observers.add(pushedSocketConnectObserverA);
        connector.observers.add(createRandomObserver(context));
        connector.observers.add(new PushedSocketConnectObserver(fwAddressB, connectObserverB));
        connector.observers.add(createRandomObserver(context));
        
        // These should be handled correctly
        assertTrue(connector.acceptPushedSocket("asdsad", -1, guidA, socketA));
        assertTrue(connector.acceptPushedSocket("asdsadsad", 10, guidB, socketB));
        
        // Repeat should fail
        assertFalse(connector.acceptPushedSocket("asdsad", -1, guidA, socketA));
        
        // Try for A with a bad address
        pushedSocketConnectObserverA.acceptedOrFailed.set(false);
        assertFalse(connector.acceptPushedSocket("blah", 10, guidA, socketAwithBadAddr));
        
        // Try for A with a bad socket -- returns true, exception suppressed, and socket closed
        pushedSocketConnectObserverA.acceptedOrFailed.set(false);
        assertTrue(connector.acceptPushedSocket("blah", 10, guidA, socketAwithIOE));
        
        context.assertIsSatisfied();
        
    }
    
    private PushedSocketConnectObserver createRandomObserver(Mockery context) throws IOException {
        final ConnectObserver connectObserver = context.mock(ConnectObserver.class);    
        final FirewalledAddress fwAddress = context.mock(FirewalledAddress.class);
        final byte[] guid = new byte[] { 'x', (byte) (Math.random()*Byte.MAX_VALUE), 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                'x' };
        
        context.checking(new Expectations() {
            {   allowing(fwAddress).getClientGuid();
                will(returnValue(new GUID(guid)));
                never(connectObserver).handleConnect(with(any(Socket.class)));
            }});
        
        return new PushedSocketConnectObserver(fwAddress, connectObserver);
    }
    
    /* TODO: Finish this text case!
    public void testConnectSimple() {
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }};

        final ConnectObserver observer = context.mock(ConnectObserver.class);    
        final XMPPFirewalledAddress address = context.mock(XMPPFirewalledAddress.class);
        
        final FirewalledAddress fwAddress = context.mock(FirewalledAddress.class);
        final GUID guid = new GUID(new byte[] {'X','x',1,2,3,4,'.','.','.',5,6,9,'n', 10,'x','X'});
        
        final XMPPFirewalledAddressConnector connector 
            = new XMPPFirewalledAddressConnector(null, null, null, null, null, null);
        
        context.checking(new Expectations() {
            {   allowing(address).getFirewalledAddress();
                will(returnValue(fwAddress));
            }});
        
        connector.connect(address, observer);
    }
    */
}
