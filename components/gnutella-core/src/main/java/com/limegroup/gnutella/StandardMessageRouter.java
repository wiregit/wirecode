package com.limegroup.gnutella;

import java.io.*;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.sun.java.util.collections.*;

public class StandardMessageRouter
    extends MessageRouter
{
    private ActivityCallback _callback;
    private FileManager _fileManager;
    private DownloadManager _downloader;

    public StandardMessageRouter(ActivityCallback callback, FileManager fm)
    {
        _callback = callback;
        _fileManager = fm;
    }

    /**
     * Override of handleQueryRequest to send query strings to the callback.
     */
    protected void handleQueryRequest(QueryRequest queryRequest,
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
     * and the Acceptor.  However, it only sends a Ping Reply back if we
     * can currently accept incoming connections or the hops + ttl <= 2 (to allow
     * for crawler pings).
     */
    protected void respondToPingRequest(PingRequest pingRequest,
                                        Acceptor acceptor)
    {
        //If this wasn't a handshake or crawler ping, check if we can accept
        //incoming connection for old-style unrouted connections, ultrapeers, or
        //leaves.  TODO: does this mean leaves always respond to pings?
        int hops = (int)pingRequest.getHops();
        int ttl = (int)pingRequest.getTTL();
        if (   (hops+ttl > 2) 
            && !_manager.hasAnyAvailableIncoming())
            return;

        //SPECIAL CASE: for Crawle ping
        if(hops ==1 && ttl==1) {
            handleCrawlerPing(pingRequest);
            //Note that the while handling crawler ping, we dont send our own
            //pong, as that is unnecessary, since crawler already has our
            //address. We though return it below for compatibility with old
            //ConnectionWatchdogPing which had TTL=2 (instead of 1).
        }
        
        //send its own ping in all the cases
        int newTTL = hops+1;
        if ( (hops+ttl) <=2)
            newTTL = 1;
        
        int num_files = _fileManager.getNumFiles();
        int kilobytes = _fileManager.getSize()/1024;

        //We mark our ping replies if currently in the supernode state.
        boolean markPong=_manager.isSupernode();
        PingReply pingReply = new PingReply(pingRequest.getGUID(),
                                            (byte)newTTL,
                                            acceptor.getPort(),
                                            acceptor.getAddress(),
                                            num_files,
                                            kilobytes,
                                            markPong);

        try
        {
            sendPingReply(pingReply);
        }
        catch(IOException e) {}
    }

    /**
     * Handles the crawler ping of Hops=0 & TTL=2, by sending pongs 
     * corresponding to all its leaves
     * @param m The ping request received
     * @exception In case any I/O error occurs while writing Pongs over the
     * connection
     */
    private void handleCrawlerPing(PingRequest m) {
        //send the pongs for leaves
        List /*<ManagedConnection>*/ leafConnections 
            = _manager.getInitializedClientConnections2();
        
        for(Iterator iterator = leafConnections.iterator(); 
            iterator.hasNext();) {
            //get the next connection
            ManagedConnection connection = (ManagedConnection)iterator.next();
            //create the pong for this connection
            PingReply pr = new PingReply(m.getGUID(), (byte)2,
                connection.getOrigPort(),
                connection.getInetAddress().getAddress(), 0, 0); 
            
            //hop the message, as it is ideally coming from the connected host
            pr.hop();
            
            try
            {
                sendPingReply(pr);
            }
            catch(IOException e) {}
        }
        
        //pongs for the neighbors will be sent by neighbors themselves
        //as ping will be broadcasted to them (since TTL=2)        
    }
    
    protected void handlePingReply(PingReply pingReply,
                                ManagedConnection receivingConnection)
    {
        //We override the super's method so the receiving connection's
        //statistics are updated whether or not this is for me.
        receivingConnection.updateHorizonStats(pingReply);
        super.handlePingReply(pingReply, receivingConnection);
    }

    /**
     * Allow the controlled creation of a GroupPingRequest
     */
    public GroupPingRequest createGroupPingRequest(String group)
    {
        int num_files = _fileManager.getNumFiles();
        int kilobytes = _fileManager.getSize()/1024;
        
        //also append everSupernodeCapable flag to the group
        GroupPingRequest pingRequest =
          new GroupPingRequest(SettingsManager.instance().getTTL(),
            _acceptor.getPort(), _acceptor.getAddress(),
            num_files, kilobytes, group + ":"
            + SettingsManager.instance().getEverSupernodeCapable());
        return( pingRequest );
    }

    


    /**
     * Handles the PingReply by updating horizon stats.
     */
    protected void handlePingReplyForMe(
        PingReply pingReply,
        ManagedConnection receivingConnection)
    {
        SettingsManager settings=SettingsManager.instance();
        //Kill incoming connections that don't share.  Note that we randomly
        //allow some freeloaders.  (Hopefully they'll get some stuff and then
        //share!)  Note that we only consider killing them on the first ping.
        //(Message 1 is their ping, message 2 is their reply to our ping.)
        if ((pingReply.getHops()<=1)
               && (receivingConnection.getNumMessagesReceived()<=2)
               && (! receivingConnection.isOutgoing())
               && (receivingConnection.isKillable())
               && (pingReply.getFiles()<settings.getFreeloaderFiles())
               && ((int)(Math.random()*100.f) >
                       settings.getFreeloaderAllowed())) {
            _manager.remove(receivingConnection);
        }
    }

    /**
     * Responds to the QueryRequest by calling FileManager.query()
     */
  //    protected void respondToQueryRequest(QueryRequest queryRequest,
//                                           Acceptor acceptor,
//                                           byte[] clientGUID)
//      {
//          // Run the local query
//          Response[] responses = FileManager.instance().query(queryRequest);
//          // If we have responses, send back a QueryReply
//          if (responses!=null && (responses.length>0))
//          {
//              byte[] guid = queryRequest.getGUID();
//              byte ttl = (byte)(queryRequest.getHops() + 1);
//              int port = acceptor.getPort();
//              byte[] ip = acceptor.getAddress();
//              long speed = SettingsManager.instance().getConnectionSpeed();
//              if (responses.length > 255)
//              {
//                  Response[] res = new Response[255];
//                  for(int i=0; i<255;i++)
//                      res[i] = responses[i];
//                  responses = res;
//              }
//              QueryReply queryReply = new QueryReply(guid, ttl, port, ip, speed,
//                                                     responses, clientGUID);
//              try
//              {
//                  sendQueryReply(queryReply);
//              }
//              catch(IOException e) {}
//          }
//      }
    protected void respondToQueryRequest(QueryRequest queryRequest,
                                         Acceptor acceptor,
                                         byte[] clientGUID)
    {

        // Run the local query
        //FileManager fm = FileManager.instance();
        Response[] responses = _fileManager.query(queryRequest);

        sendResponses(responses, queryRequest, acceptor, clientGUID);
        
    }

    public void sendResponses(Response[] responses, 
                                        QueryRequest queryRequest,
                                         Acceptor acceptor,
                                         byte[] clientGUID)
    {
        // if either there are no responses or, the
        // response array came back null for some reason,
        // exit this method
        if ( (responses == null) || ((responses.length < 1)) )
            return;

        // get the appropriate queryReply information
        byte[] guid = queryRequest.getGUID();
        byte ttl = (byte)(queryRequest.getHops() + 1);
        int port = acceptor.getPort();
        byte[] ip = acceptor.getAddress();

        //Return measured speed if possible, or user's speed otherwise.
        long speed = _uploadManager.measuredUploadSpeed();
        boolean measuredSpeed=true;
        if (speed==-1) {
            speed=SettingsManager.instance().getConnectionSpeed();
            measuredSpeed=false;
        }

        int numResponses = responses.length;
        int index = 0;

        int numHops = queryRequest.getHops();

        QueryReply queryReply;

        // modified by rsoule, 11/16/00

        while (numResponses > 0)
        {
            int arraySize;
            // if there are more than 255 responses,
            // create an array of 255 to send in the queryReply
            // otherwise, create an array of whatever size is left.
            if (numResponses < 255) {
                // break;
                arraySize = numResponses;
            }
            else
                arraySize = 255;

            Response[] res;
            // a special case.  in the common case where there
            // are less than 256 responses being sent, there
            // is no need to copy one array into another.
            if ( (index == 0) && (arraySize < 255) )
            {
                res = responses;
            }
            else
            {
                res = new Response[arraySize];
                // copy the reponses into bite-size chunks
                for(int i =0; i < arraySize; i++) {
                    res[i] = responses[index];
                    index++;
                }
            }

            // decrement the number of responses we have left
            numResponses-= arraySize;

			// see id there are any open slots
			boolean busy = _uploadManager.isBusy();
            boolean uploaded = _uploadManager.hadSuccesfulUpload();

			// see if we have ever accepted an incoming connection
			boolean incoming = _acceptor.acceptedIncoming();

			boolean chat = SettingsManager.instance().getChatEnabled();

            // create the new queryReply
            queryReply = new QueryReply(guid, ttl, port, ip,
                                        speed, res, clientGUID, 
										!incoming, busy, uploaded, measuredSpeed,
                                        chat);  //supports chat

            // try to send the new queryReply
            try {
                sendQueryReply(queryReply);
            } catch (IOException e) {
                // if there is an error, do nothing..
            }

            // we only want to send multiple queryReplies
            // if the number of hops is small.
            if (numHops > 2) {
                break;
            }

        }//end of while

    }

    /** @see MessageRouter.addQueryRoutingEntries */
    protected void addQueryRoutingEntries(QueryRouteTable qrt) {
        Iterator words = _fileManager.getKeyWords().iterator();
        while(words.hasNext())
            qrt.add((String)words.next());
        // get 'indivisible' words and handle appropriately - you don't want the
        // qrt to divide these guys up....
        Iterator indivisibleWords = _fileManager.getIndivisibleKeyWords().iterator();
        while (indivisibleWords.hasNext()) 
            qrt.addIndivisible((String) indivisibleWords.next());
        /*
          File[] files = _fileManager.getSharedFiles(null);
          for (int i=0; i<files.length; i++)
            qrt.add(files[i].getAbsolutePath());
        */
    }

    /**
     * Handles the QueryReply by starting applying the personal filter and then
     * displaying the result.
     */
    protected void handleQueryReplyForMe(
        QueryReply queryReply,
        ManagedConnection receivingConnection)
    {
        if (_downloader == null) 
            if (RouterService.instance() != null)
                _downloader = RouterService.instance().getDownloadManager();

        if (!receivingConnection.isPersonalSpam(queryReply)) {
            _callback.handleQueryReply(queryReply);
            _downloader.handleQueryReply(queryReply);
        }
    }

    /**
     * Handles the PushRequest by starting an HTTPUploader
     */
    protected void handlePushRequestForMe(
        PushRequest pushRequest,
        ManagedConnection receivingConnection)
    {
        //Ignore push request from banned hosts.
        if (receivingConnection.isPersonalSpam(pushRequest))
            return;

        // Ignore excess upload requests
        //if (_callback.getNumUploads() >=
/*
        if (HTTPUploader.getUploadCount() >=
                SettingsManager.instance().getMaxUploads())
            return;
*/
		
        // Unpack the message
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
            desc = _fileManager.get(index);
        }
        catch (IndexOutOfBoundsException e)
        {
            //You could connect and send 404 file
            //not found....but why bother?
            return;
        }

        String file = desc._name;

        if (!_acceptor.isBannedIP(h))	
            _uploadManager.acceptPushUpload(file, h, port, 
                                            index, req_guid_hexstring);

//          HTTPUploader up = new HTTPUploader(h, port, index, req_guid_hexstring,
//                                             _callback);
//          Thread t=new Thread(up);
//          t.setDaemon(true);
//          t.start();
    }
}
