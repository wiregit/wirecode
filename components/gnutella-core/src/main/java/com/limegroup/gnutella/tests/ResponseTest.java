package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.HTTPDownloader;
import com.limegroup.gnutella.HTTPUploader;
import com.limegroup.gnutella.ManagedConnection;
import com.limegroup.gnutella.Message;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.PingRequest;
import com.limegroup.gnutella.PingReply;
import com.limegroup.gnutella.PushRequest;
import com.limegroup.gnutella.QueryRequest;
import com.limegroup.gnutella.QueryReply;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.SettingsManager;
import java.io.IOException;

/**
 * This class' main method tests basic request/reply capabilities
 * by instantiating two RouterService objects using a special MessageRouter and
 * sending messages between them.
 */
public class ResponseTest
{
    public static final byte[] IP = new byte[]{127, 0, 0, 1};
    public static final int PORT_1 = 1000;
    public static final int PORT_2 = 1001;

    public static final String TEST_QUERY = "bogus.mp3";
    public static final int TEST_INDEX = 0;
    public static final byte[] TEST_GUID = GUID.makeGuid();

    public static final int TIMEOUT = 10000;

    public static final int ACTION_NONE = 0;
    public static final int ACTION_PING_REQUEST = ACTION_NONE + 1;
    public static final int ACTION_PING_REPLY = ACTION_PING_REQUEST + 1;
    public static final int ACTION_QUERY_REQUEST = ACTION_PING_REPLY + 1;
    public static final int ACTION_QUERY_REPLY = ACTION_QUERY_REQUEST + 1;
    public static final int ACTION_PUSH_REQUEST = ACTION_QUERY_REPLY + 1;

    public static void main(String[] args)
    {
        // Make sure all the settings changes we make do not persist.
        SettingsManager.instance().setWrite(false);

        // Keep connections from launching
        SettingsManager.instance().setKeepAlive(0);
        SettingsManager.instance().setUseQuickConnect(false);

        // Set up the two message routers
        TestMessageRouter messageRouter1 = new TestMessageRouter();
        TestMessageRouter messageRouter2 = new TestMessageRouter();

        // Start the two router services
        RouterService routerService1 = new RouterService(
            PORT_1,
            new TestActivityCallback(),
            messageRouter1);
        RouterService routerService2 = new RouterService(
            PORT_2,
            new TestActivityCallback(),
            messageRouter2);

        // Set up the connection
        ManagedConnection connection;
        try
        {
            connection =
                routerService1.connectToHostBlocking("127.0.0.1", PORT_2);
        }
        catch(IOException e)
        {
            System.out.println("Connection failed.");
            e.printStackTrace();
            return;
        }

        System.out.println("Testing ping request/reply");
        messageRouter1.setExpectedAction(ACTION_PING_REPLY);
        messageRouter2.setExpectedAction(ACTION_PING_REQUEST);
        messageRouter1.broadcastPingRequest(
            new PingRequest((byte)1));
        messageRouter1.waitForAction();
        messageRouter2.waitForAction();

        System.out.println("Testing query request/reply");
        messageRouter1.setExpectedAction(ACTION_QUERY_REPLY);
        messageRouter2.setExpectedAction(ACTION_QUERY_REQUEST);
        messageRouter1.broadcastQueryRequest(
            new QueryRequest((byte)1, 0, TEST_QUERY));
        messageRouter1.waitForAction();
        messageRouter2.waitForAction();

        System.out.println("Testing push request");
        messageRouter1.setExpectedAction(ACTION_NONE);
        messageRouter2.setExpectedAction(ACTION_PUSH_REQUEST);
        try
        {
            messageRouter1.sendPushRequest(
                new PushRequest(GUID.makeGuid(), (byte)1, TEST_GUID, TEST_INDEX,
                                IP, PORT_1));
            messageRouter2.waitForAction();
        }
        catch(IOException e)
        {
            System.out.println("Error sending PushRequest");
        }

        routerService1.removeConnection(connection);

        System.out.println("Tests complete.  If there are no error " +
                           "messages, the tests were successful");
    }

    /**
     * A special subclass of MessageRouter that performs action checking
     * and sends prescribed responses
     */
    private static final class TestMessageRouter
        extends MessageRouter
    {
        // The action we're expecting
        private int _expectedAction;
        // A marker that indicates that an action occurred before
        // the waitForExceptedAction() happened
        private boolean _actionOccurred;

        public TestMessageRouter()
        {
            _expectedAction = ACTION_NONE;
        }

        public synchronized void setExpectedAction(int expectedAction)
        {
            _expectedAction = expectedAction;
            _actionOccurred = false;
        }

        public synchronized void waitForAction()
        {
            if(_actionOccurred)
                return;
            try
            {
                wait(TIMEOUT);
            }
            catch(InterruptedException e) {}

            if(!_actionOccurred)
                checkExpectedAction(ACTION_NONE);
        }

        private synchronized void checkExpectedAction(int actualAction)
        {
            if(_expectedAction != actualAction)
                System.out.println("Error: Got " +
                                   getActionString(actualAction) +
                                   ", expected " +
                                   getActionString(_expectedAction));

            _actionOccurred = true;
            notifyAll();
        }

        private String getActionString(int action)
        {
            if(action == ACTION_NONE)
                return "No action";
            else if (action == ACTION_PING_REQUEST)
                return "PingRequest";
            else if (action == ACTION_PING_REPLY)
                return "PingReply";
            else if (action == ACTION_QUERY_REQUEST)
                return "QueryRequest";
            else if (action == ACTION_QUERY_REPLY)
                return "QueryReply";
            else if (action == ACTION_PUSH_REQUEST)
                return "PushRequest";
            else
                return null; // Error
        }

        protected void respondToPingRequest(PingRequest pingRequest,
                                            Acceptor acceptor)
        {
            checkExpectedAction(ACTION_PING_REQUEST);

            try
            {
                sendPingReply(new PingReply(pingRequest.getGUID(),
                                            (byte)(pingRequest.getHops()+1),
                                            PORT_2,
                                            IP,
                                            0,
                                            0));
            }
            catch(IOException e)
            {
                System.out.println("Error sending ping reply");
            }
        }

        protected void respondToQueryRequest(QueryRequest queryRequest,
                                             Acceptor acceptor,
                                             byte[] clientGUID)
        {
            checkExpectedAction(ACTION_QUERY_REQUEST);

            Response[] responses = new Response[1];
            responses[0] = new Response(TEST_INDEX, 100, TEST_QUERY);
            try
            {
                sendQueryReply(new QueryReply(queryRequest.getGUID(),
                                              (byte)(queryRequest.getHops()+1),
                                              PORT_2,
                                              IP,
                                              56000,
                                              responses,
                                              TEST_GUID));
            }
            catch(IOException e)
            {
                System.out.println("Error sending ping reply");
            }
        }



        protected void handlePingReplyForMe(
            PingReply pingReply,
            ManagedConnection receivingConnection)
        {
            checkExpectedAction(ACTION_PING_REPLY);
        }

        protected void handleQueryReplyForMe(
            QueryReply queryReply,
            ManagedConnection receivingConnection)
        {
            checkExpectedAction(ACTION_QUERY_REPLY);
        }

        protected void handlePushRequestForMe(
            PushRequest pushRequest,
            ManagedConnection receivingConnection)
        {
            checkExpectedAction(ACTION_PUSH_REQUEST);
        }
    }

    /**
     * An implementation of ActivityCallback that does nothing, except
     * on error, when it prints an error.
     */
    private static final class TestActivityCallback
        implements ActivityCallback
    {
        public TestActivityCallback() {}

        public void connectionInitializing(Connection c) {}

        public void connectionInitialized(Connection c) {}

        public void connectionClosed(Connection c) {}

        public void knownHost(Endpoint e) {}

        public void handleQueryReply( QueryReply qr ) {}

        public void handleQueryString( String query ) {}

        public boolean overwriteFile(String file) { return false; }

        public void addDownload(HTTPDownloader d) {}

        public void removeDownload(HTTPDownloader d) {}

        public void addUpload(HTTPUploader u) {}

        public void removeUpload(HTTPUploader u) {}

        public int getNumUploads() { return -1; }

        public void setPort(int port) {}

        public void error(int errorCode)
        {
            System.out.println("Error " + errorCode);
        }

        public void error(int errorCode, Throwable t)
        {
            System.out.println("Error " + errorCode);
            t.printStackTrace();
        }
    }
}
