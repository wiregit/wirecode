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
 * This class' main method tests basic request/reply and routing capabilities
 * by instantiating three RouterService objects using a special MessageRouter
 * and sending messages between them.
 *
 * The three RouterServices are connected in a chain:
 *  1 -- 2 -- 3.
 * 1 generates requests, 2 forwards them and routes replies, and 3 replies
 * to requests.
 *
 * Note that this test uses notifications -- there are no timeouts on a
 * successful test run.
 */
public class MessageRouterTest
{
    /**
     * This class is never instantiated.  It's just a holder for a main method.
     */
    private MessageRouterTest() {}

    /**
     * The IP address to use in various messages.
     */
    public static final byte[] IP = new byte[]{127, 0, 0, 1};
    /**
     * The ports to use for each RouterService
     */
    public static final int PORT_1 = 1000;
    public static final int PORT_2 = PORT_1 + 1;
    public static final int PORT_3 = PORT_2 + 1;

    /**
     * The index to use for a QueryReply.  The QueryReply and the PushRequest
     * have to agree on this number.
     */
    public static final int TEST_INDEX = 0;
    /**
     * The guid to use for a QueryReply.  The QueryReply and the PushRequest
     * have to agree on this number.
     */
    public static final byte[] TEST_GUID = GUID.makeGuid();

    /**
     * The amount of time to wait for an action before deciding that none is
     * forthcoming.  This is purely for error handling.  If the tests run
     * successfully, the timeout will never be hit.
     */
    public static final int TIMEOUT = 10000;

    // Action IDs
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

        // Turn off filters
        SettingsManager.instance().setBannedIps(new String[0]);
        SettingsManager.instance().setBannedWords(new String[0]);
        SettingsManager.instance().setFilterAdult(false);
        SettingsManager.instance().setFilterDuplicates(false);
        SettingsManager.instance().setFilterHtml(false);
        SettingsManager.instance().setFilterVbs(false);
        SettingsManager.instance().setFilterGreedyQueries(false);


        // Set up the message routers.  Only configure 3 to generate replies
        TestMessageRouter messageRouter1 = new TestMessageRouter(1, false);
        TestMessageRouter messageRouter2 = new TestMessageRouter(2, false);
        TestMessageRouter messageRouter3 = new TestMessageRouter(3, true);

        // Start the three router services
        RouterService routerService1 = new RouterService(
            PORT_1,
            new TestActivityCallback(),
            messageRouter1);
        RouterService routerService2 = new RouterService(
            PORT_2,
            new TestActivityCallback(),
            messageRouter2);
        RouterService routerService3 = new RouterService(
            PORT_3,
            new TestActivityCallback(),
            messageRouter3);

        // Set up the the connection from 1 to 2.  1 will ping and 2 won't
        // reply
        System.out.println("Setting up first connection (and testing " +
                           "initial ping)");
        messageRouter1.setExpectedAction(ACTION_NONE);
        messageRouter2.setExpectedAction(ACTION_PING_REQUEST);
        messageRouter3.setExpectedAction(ACTION_NONE);
        ManagedConnection connection1;
        try
        {
            connection1 =
                routerService1.connectToHostBlocking("127.0.0.1", PORT_2);
        }
        catch(IOException e)
        {
            System.out.println("Connection failed.");
            e.printStackTrace();
            return;
        }
        messageRouter1.waitForAction();
        messageRouter2.waitForAction();
        messageRouter3.waitForAction();

        // Set up the the connection from 2 to 3.  2 will ping, and 3
        // will reply.
        System.out.println("Setting up second connection (and testing " +
                           "initial ping)");
        ManagedConnection connection2;
        messageRouter1.setExpectedAction(ACTION_NONE);
        messageRouter2.setExpectedAction(ACTION_PING_REPLY);
        messageRouter3.setExpectedAction(ACTION_PING_REQUEST);
        try
        {
            connection2 =
                routerService2.connectToHostBlocking("127.0.0.1", PORT_3);
        }
        catch(IOException e)
        {
            System.out.println("Connection failed.");
            e.printStackTrace();
            return;
        }
        messageRouter1.waitForAction();
        messageRouter2.waitForAction();
        messageRouter3.waitForAction();

        // Send a ping from 1 with a TTL of 2.  2 and 3 will receive it, and 3
        // will reply. 1 should receive the reply
        System.out.println("Testing ping request/reply");
        messageRouter1.setExpectedAction(ACTION_PING_REPLY);
        messageRouter2.setExpectedAction(ACTION_PING_REQUEST);
        messageRouter3.setExpectedAction(ACTION_PING_REQUEST);
        messageRouter1.broadcastPingRequest(
            new PingRequest((byte)2));
        messageRouter1.waitForAction();
        messageRouter2.waitForAction();
        messageRouter3.waitForAction();

        // Send a query from 1 with a TTL of 2.  2 and 3 will receive it, and 3
        // will reply. 1 should receive the reply
        System.out.println("Testing query request/reply");
        messageRouter1.setExpectedAction(ACTION_QUERY_REPLY);
        messageRouter2.setExpectedAction(ACTION_QUERY_REQUEST);
        messageRouter3.setExpectedAction(ACTION_QUERY_REQUEST);
        messageRouter1.broadcastQueryRequest(
            new QueryRequest((byte)2, 0, "bogus.mp3"));
        messageRouter1.waitForAction();
        messageRouter2.waitForAction();
        messageRouter3.waitForAction();

        // Send a push request from 1.  3 will receive it. (2 routes it,
        // but that's not considered an action).
        System.out.println("Testing push request -- this depends on success " +
                           "of the query request/reply");
        messageRouter1.setExpectedAction(ACTION_NONE);
        messageRouter2.setExpectedAction(ACTION_NONE);
        messageRouter3.setExpectedAction(ACTION_PUSH_REQUEST);
        try
        {
            messageRouter1.sendPushRequest(
                new PushRequest(GUID.makeGuid(), (byte)5, TEST_GUID, TEST_INDEX,
                                IP, PORT_1));
        }
        catch(IOException e)
        {
            System.out.println("Error sending PushRequest");
        }
        messageRouter1.waitForAction();
        messageRouter2.waitForAction();
        messageRouter3.waitForAction();

        // Send a ping from 1 with a TTL of 1.  2 will receive it, but won't
        // reply or forward.
        System.out.println("Testing TTL 1 ping -- no forwarding");
        messageRouter1.setExpectedAction(ACTION_NONE);
        messageRouter2.setExpectedAction(ACTION_PING_REQUEST);
        messageRouter3.setExpectedAction(ACTION_NONE);
        messageRouter1.broadcastPingRequest(
            new PingRequest((byte)1));
        messageRouter1.waitForAction();
        messageRouter2.waitForAction();
        messageRouter3.waitForAction();

        // Send a ping from 1 with a TTL of 0.  2 will receive it, but won't
        // reply or forward.
        System.out.println("Testing TTL 0 ping -- no forwarding");
        messageRouter1.setExpectedAction(ACTION_NONE);
        messageRouter2.setExpectedAction(ACTION_PING_REQUEST);
        messageRouter3.setExpectedAction(ACTION_NONE);
        messageRouter1.broadcastPingRequest(
            new PingRequest((byte)0));
        messageRouter1.waitForAction();
        messageRouter2.waitForAction();
        messageRouter3.waitForAction();

        // Send a ping from 2 with a TTL of 0.  1 and 3 receive it, and 3
        // replies even though the TTL is 0.
        System.out.println("Testing TTL 0 ping -- reply occurs");
        messageRouter1.setExpectedAction(ACTION_PING_REQUEST);
        messageRouter2.setExpectedAction(ACTION_PING_REPLY);
        messageRouter3.setExpectedAction(ACTION_PING_REQUEST);
        messageRouter2.broadcastPingRequest(
            new PingRequest((byte)0));
        messageRouter1.waitForAction();
        messageRouter2.waitForAction();
        messageRouter3.waitForAction();

        // Send a query from 1 with a TTL of 1.  2 will receive it, but won't
        // reply or forward.
        System.out.println("Testing TTL 1 query -- no forwarding");
        messageRouter1.setExpectedAction(ACTION_NONE);
        messageRouter2.setExpectedAction(ACTION_QUERY_REQUEST);
        messageRouter3.setExpectedAction(ACTION_NONE);
        messageRouter1.broadcastQueryRequest(
            new QueryRequest((byte)1, 0, "bogus.mp3"));
        messageRouter1.waitForAction();
        messageRouter2.waitForAction();
        messageRouter3.waitForAction();

        // Send a query from 1 with a TTL of 0.  2 will receive it, but won't
        // reply or forward.
        System.out.println("Testing TTL 0 query -- no forwarding");
        messageRouter1.setExpectedAction(ACTION_NONE);
        messageRouter2.setExpectedAction(ACTION_QUERY_REQUEST);
        messageRouter3.setExpectedAction(ACTION_NONE);
        messageRouter1.broadcastQueryRequest(
            new QueryRequest((byte)0, 0, "bogus.mp3"));
        messageRouter1.waitForAction();
        messageRouter2.waitForAction();
        messageRouter3.waitForAction();

        // Send a query from 2 with a TTL of 0.  1 and 3 receive it, and 3
        // replies even though the TTL is 0.
        System.out.println("Testing TTL 0 query -- reply occurs");
        messageRouter1.setExpectedAction(ACTION_QUERY_REQUEST);
        messageRouter2.setExpectedAction(ACTION_QUERY_REPLY);
        messageRouter3.setExpectedAction(ACTION_QUERY_REQUEST);
        messageRouter2.broadcastQueryRequest(
            new QueryRequest((byte)0, 0, "bogus.mp3"));
        messageRouter1.waitForAction();
        messageRouter2.waitForAction();
        messageRouter3.waitForAction();

        routerService1.removeConnection(connection1);
        routerService2.removeConnection(connection2);

        System.out.println("Tests complete.  If there are no error " +
                           "messages, the tests were successful");
    }

    /**
     * A special subclass of MessageRouter that performs action checking
     * and sends prescribed replies
     */
    private static final class TestMessageRouter
        extends MessageRouter
    {
        /**
         * An identifier for printing error messages
         */
        private int _id;
        /**
         * A flag that indicates whether this router actually replies
         * to requests or just forwards them.
         */
        private boolean _reply;
        /**
         * The action we're expecting.
         */
        private int _expectedAction;
        /**
         * A marker that indicates that an action occurred before
         * the waitForExceptedAction() happened
         */
        private boolean _actionOccurred;


        public TestMessageRouter(int id, boolean reply)
        {
            _id = id;
            _reply = reply;
            _expectedAction = ACTION_NONE;
        }

        public synchronized void setExpectedAction(int expectedAction)
        {
            _expectedAction = expectedAction;
            _actionOccurred = false;
        }

        /**
         * Blocks until an action occurs.  If an action has occurred since
         * setExpectedAction was called or if no action is expected, returns
         * immediately.
         */
        public synchronized void waitForAction()
        {
            if(_expectedAction == ACTION_NONE)
                return;
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
            if(_actionOccurred)
                System.out.println("Extra action on router " +
                                   _id +
                                   ": Got " +
                                   getActionString(actualAction) +
                                   ", while expecting" +
                                   getActionString(_expectedAction));

            if(_expectedAction != actualAction)
                System.out.println("Error on router " +
                                   _id +
                                   ": Got " +
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

            if(_reply)
            {
                try
                {
                    sendPingReply(
                        new PingReply(pingRequest.getGUID(),
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
        }

        protected void respondToQueryRequest(QueryRequest queryRequest,
                                             Acceptor acceptor,
                                             byte[] clientGUID)
        {
            checkExpectedAction(ACTION_QUERY_REQUEST);

            if(_reply)
            {
                Response[] responses = new Response[1];
                responses[0] = new Response(TEST_INDEX, 100, "bogus.mp3");
                try
                {
                    sendQueryReply(
                        new QueryReply(queryRequest.getGUID(),
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
