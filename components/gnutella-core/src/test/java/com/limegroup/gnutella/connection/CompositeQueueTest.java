package com.limegroup.gnutella.connection;

import junit.framework.Test;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.ResetTableMessage;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;

/**
 * Tests all functionality of the CompositeQueue class.
 */
public final class CompositeQueueTest extends BaseTestCase {

    /**
     * Creates a new <tt>CompositeQueueTest</tt>.
     */
    public CompositeQueueTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(CompositeQueueTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void setUp() throws Exception {
        ConnectionSettings.USE_NIO.setValue(false);
    }
    
    /**
     * Tests the method for accessing the next message from the queue.
     * 
     * @throws Exception if an error occurs
     */
    public void testGetMessage() throws Exception {
        Connection conn = new Connection("localhost", 3435);
        MessageWriter proxy = 
            ConnectionSettings.USE_NIO.getValue() ? 
            NIOMessageWriter.createWriter(conn) :
            BIOMessageWriter.createWriter(conn); 

        PrivilegedAccessor.setValue(conn, "_messageWriter", proxy);
        BIOMessageWriter writer = 
            (BIOMessageWriter)PrivilegedAccessor.getValue(proxy, "DELEGATE");
        
        // we will check to make sure queue values are correct
        CompositeQueue queue = 
            (CompositeQueue)PrivilegedAccessor.getValue(writer, "QUEUE");
       
        
        // give the query hit a payload
        byte[] payload = new byte[11+11+16];
        payload[0] = 1;            //Number of results
        payload[1] = 1;            //non-zero port
        payload[11+8] = (byte)65;  //The character 'A'
        
        // create a message of each type to easily fill up the queues
        Message[] messages = new Message[] {    
            QueryRequest.createQuery("test", (byte)3),
            new QueryReply(new byte[16], (byte)3, (byte)0, payload),
            new PingRequest((byte)3),
            PingReply.create(new byte[16], (byte)3),
            new PushRequest(new byte[16], (byte)3, new byte[16], 10, 
                new byte[4], 6346),
            new PingRequest((byte)1),  // this is for watchdog
            new ResetTableMessage(100, (byte)7),
        };
        
        
        // Now, fill up the queues with messages, and make sure they're 
        // accessed correctly.  Note that we go over the queue's capacity
        // in the case of some message types -- they should be dropped
        // appropriately.
        for(int i=0; i<messages.length; i++) {
            for(int j=0; j<300; j++) {
                queue.add(messages[i]);   
            }
        }
        
            
        PrivilegedAccessor.setValue(queue, "_priority", new Integer(0));
        
        // make a first pass through the messages to make sure they're 
        // accessed in the correct order from the queue
        for(int i=0; i<messages.length; i++) {
            int limit = getCycleLimit(i);
            for(int j=0; j<limit; j++) {
                Message curMessage = queue.removeNext();
                assertCorrectType(i, curMessage);
            }
        }
        
        // run through them again a couple of times to make sure it cycles 
        // back to the correct starting priority -- checking boundary 
        // conditions
        for(int r=0; r<3; r++) {
            for(int i=0; i<messages.length; i++) {
                int limit = getCycleLimit(i);
                
                // Some message types only store 1 value, which we've already
                // drained, requiring us to go on to the next type.
                // In particular, this applies to pings and pongs.  It would be
                // nice not to hard code, but it's unfortunately not easy to get
                // the capacity (as opposed to the size) of a queue, even using
                // PrivilegedAccessor
                if(i == 4 || i == 5) continue;
                for(int j=0; j<limit; j++) {
                    Message curMessage = queue.removeNext();
                    assertCorrectType(i, curMessage);
                }
            }
        }
    }
    
    private static int getCycleLimit(int index) {
        switch(index) {
            case 0:
                return 1;
            case 1:
                return 6;
            case 2:
                return 6;
            case 3:
                return 3;
            case 4:
                return 1;
            case 5:
                return 1;
            case 6:
                return 1;
            default:
                // this should never happen
                return -1;
        }    
    }
    
    /**
     * Helper method that makes sure that the specified index is correctly
     * associated with message of that priority.
     * 
     * @param index the priority of the message
     * @param curMessage the message that should be associated with the 
     *  specified priority
     */
    private static void assertCorrectType(int index, Message curMessage) {
        switch(index) {
            case 0:
                assertInstanceof("should be a watchdog ping", 
                    PingRequest.class, curMessage);
                break;
            case 1:
                assertInstanceof("should be a push", 
                                 PushRequest.class, curMessage);
                break;
            case 2:
                assertInstanceof("should be a query hit", 
                                QueryReply.class, curMessage);
                break;
            case 3:
                assertInstanceof("should be a query", 
                                QueryRequest.class, curMessage);
                break;
            case 4:
                assertInstanceof("should be a pong", 
                                PingReply.class, curMessage);
                break;
            case 5:
                assertInstanceof("should be a ping", 
                                PingRequest.class, curMessage);
                break;
            case 6:
                assertInstanceof("should be a route table message", 
                                RouteTableMessage.class, curMessage);
                break;
        }        
    }
}
