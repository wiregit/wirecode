package com.limegroup.gnutella;

import java.io.IOException;

public class StandardMessageRouter
    extends MessageRouter
{
    private ActivityCallback _callback;

    public StandardMessageRouter(ActivityCallback callback)
    {
        _callback = callback;
    }

    /**
     * Override of handleQueryRequest to send query strings to the callback.
     */
    public void handleQueryRequest(QueryRequest queryRequest,
                                   ManagedConnection receivingConnection)
    {
        // Apply the personal filter to decide whether the callback
        // should be informed of the query
        if (!receivingConnection.isPersonalSpam(queryRequest))
        {
            _callback.handleQueryString(queryRequest.getQuery());
        }

        super.handleQueryRequest(queryRequest, receivingConnection);
    }


    /**
     * Responds to the PingRequest by getting information from the FileManager
     * and the Acceptor.
     */
    protected void respondToPingRequest(PingRequest pingRequest,
                                        Acceptor acceptor)
    {
        FileManager fm = FileManager.getFileManager();
        int num_files = fm.getNumFiles();
        int kilobytes = fm.getSize()/1024;

        PingReply pingReply = new PingReply(pingRequest.getGUID(),
                                            (byte)(pingRequest.getHops()+1),
                                            acceptor.getPort(),
                                            acceptor.getAddress(),
                                            num_files,
                                            kilobytes);

        try
        {
            sendPingReply(pingReply);
        }
        catch(IOException e) {}
    }

    /**
     * Handles the PingReply by updating horizon stats.
     */
    protected void handlePingReplyForMe(
        PingReply pingReply,
        ManagedConnection receivingConnection)
    {
        receivingConnection.updateHorizonStats(pingReply);
    }

    /**
     * Responds to the QueryRequest by calling FileManager.query()
     */
    protected void respondToQueryRequest(QueryRequest queryRequest,
                                         Acceptor acceptor,
                                         byte[] clientGUID)
    {
        // Run the local query
        Response[] responses = FileManager.getFileManager().query(queryRequest);

        // If we have responses, send back a QueryReply
        if (responses!=null && (responses.length>0))
        {
            byte[] guid = queryRequest.getGUID();
            byte ttl = (byte)(queryRequest.getHops() + 1);
            int port = acceptor.getPort();
            byte[] ip = acceptor.getAddress();
            long speed = SettingsManager.instance().getConnectionSpeed();

            // Modified by Sumeet Thadani
            // If the number of responses is more 255, we
            // are going to drop the responses after index
            // 255. This can be corrected post beta, so
            // that the extra responses can be sent along as
            // another query reply.
            if (responses.length > 255)
            {
                Response[] res = new Response[255];
                //copy first 255 elements of old array
                for(int i=0; i<255;i++)
                    res[i] = responses[i];
                responses = res;
            }

            QueryReply queryReply = new QueryReply(guid, ttl, port, ip, speed,
                                                   responses, clientGUID);
            try
            {
                sendQueryReply(queryReply);
            }
            catch(IOException e) {}
        }
    }

    /**
     * Handles the QueryReply by starting applying the personal filter and then
     * displaying the result.
     */
    protected void handleQueryReplyForMe(
        QueryReply queryReply,
        ManagedConnection receivingConnection)
    {
        if (!receivingConnection.isPersonalSpam(queryReply))
            _callback.handleQueryReply(queryReply);
    }

    /**
     * Handles the PushRequest by starting an HTTPUploader
     */
    protected void handlePushRequestForMe(
        PushRequest pushRequest,
        ManagedConnection receivingConnection)
    {
        // Ignore excess upload requests
        //if (_callback.getNumUploads() >=
        if (HTTPUploader.getUploadCount() >=
                SettingsManager.instance().getMaxUploads())
            return;

        // Unpack the message
        String host = new String(pushRequest.getIP());
        byte[] ip = pushRequest.getIP();
        StringBuffer buf = new StringBuffer();
        buf.append(ByteOrder.ubyte2int(ip[0])+".");
        buf.append(ByteOrder.ubyte2int(ip[1])+".");
        buf.append(ByteOrder.ubyte2int(ip[2])+".");
        buf.append(ByteOrder.ubyte2int(ip[3])+"");
        String h = buf.toString();
        int port = pushRequest.getPort();
        int index = (int)pushRequest.getIndex();
        String req_guid_hexstring =
            (new GUID(pushRequest.getClientGUID())).toString();

        FileDesc desc;
        try
        {
            desc = FileManager.getFileManager().get(index);
        }
        catch (IndexOutOfBoundsException e)
        {
            //You could connect and send 404 file
            //not found....but why bother?
            return;
        }
        String file = desc._name;

        HTTPUploader up = new HTTPUploader(h, port, index, req_guid_hexstring,
                                           _callback);
        Thread t=new Thread(up);
        t.setDaemon(true);
        t.start();
    }
}
