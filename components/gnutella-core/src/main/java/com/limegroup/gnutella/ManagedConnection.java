package com.limegroup.gnutella;

import java.io.*;
import java.net.*;

public class ManagedConnection
        extends Connection {
    private ConnectionManager _manager;
    private volatile SpamFilter _routeFilter = SpamFilter.newRouteFilter();
    private volatile SpamFilter _personalFilter =
        SpamFilter.newPersonalFilter();

    protected int dropped=0;
    protected int lastReceived=0;
    protected int lastDropped=0;

    /** Statistics about horizon size. */
    public long totalFileSize;
    public long numFiles;
    public long numHosts;

    /**
     * Creates an outgoing connection.
     * ManagedConnections should only be constructed within ConnectionManager.
     */
    ManagedConnection(String host, int port, ConnectionManager manager) {
        super(host, port);
        _manager = manager;
    }

    /**
     * Creates an incoming connection.
     * ManagedConnections should only be constructed within ConnectionManager.
     * @requires the word "GNUTELLA " and nothing else has just been read
     *  from socket
     * @effects wraps a connection around socket and does the rest of the Gnutella
     *  handshake.  Throws IOException if the connection couldn't be established.
     *  If such an error happens, the socket is properly closed.
     */
    ManagedConnection(Socket socket, ConnectionManager manager)
            throws IOException {
        super(socket);
        _manager = manager;
    }

    public void initialize() throws IOException {
        super.initialize();

        if(isOutgoing()) {
            try {
                //Send initial ping request.  HACK: use routeTable to
                //designate that replies are for me.  Do this *before*
                //sending message.
                PingRequest pr=new PingRequest(
                    SettingsManager.instance().getTTL());
                _manager.fromMe(pr);
                send(pr);
            } catch (IOException e) {
                shutdown();
                throw e;
            }
        }
    }

    /**
     * Override of send to do ConnectionManager stats
     */
    public void send(Message m) throws IOException {
        super.send(m);
        _manager.total++;
    }

    /**
     * Override of receive to do ConnectionManager stats
     */
    public Message receive() throws IOException, BadPacketException {
        Message m = super.receive();
        _manager.total++;
        return m;
    }

    private final boolean isRouteSpam(Message m) {
        if (!_routeFilter.allow(m)) {
            _manager.totDropped++;
            dropped++;
            return true;
        } else
            return false;
    }

    /**
     * Handles core Gnutella request/reply protocol.  This call
     * will run until the connection is closed.  Note this this is called
     * from the run methods of several different thread implementations
     * that are inner classes of ConnectionManager.  This allows a single
     * thread to be used for initialization and for the request/reply loop.
     *
     * @requires this is initialized
     * @modifies the network underlying this, manager
     * @effects receives request and sends appropriate replies.
     *   Returns if either the connection is closed or an error happens.
     *   If this happens, removes itself from the manager's connection list,
     *   so no further cleanup is necessary.  No exception is thrown
     */
    void loopForMessages() {
        try {
            while (true) {
                Message m=null;
                try {
                    m=receive();
                    if (m==null)
                        continue;
                }
                catch (BadPacketException e) {
                    //System.out.println("Discarding bad packet ("+e.getMessage()+")");
                    //dropped++;
                    continue;
                }
                if(m instanceof PingRequest){
                    Connection inConnection =
                        _manager.getRouteTable().get(m.getGUID());
                    //connection has never been encountered before...
                    if (inConnection==null && !isRouteSpam(m)){
                        //reduce TTL, increment hops. If old val of TTL was 0
                        // drop message
                        _manager.PReqCount++;
                        if (m.hop()!=0){
                            _manager.getRouteTable().put(m.getGUID(),this);
                            //broadcast to other hosts
                            _manager.sendToAllExcept(m, this);
                            byte[] ip = getLocalAddress().getAddress();

                            FileManager fm = FileManager.getFileManager();

                            int kilobytes = fm.getSize()/1024;
                            int num_files = fm.getNumFiles();

                            Message pingReply =
                            new PingReply(m.getGUID(),
                                          (byte)(m.getHops()+1),
                                          _manager.getListeningPort(),
                                          ip, num_files, kilobytes);

                            send(pingReply);
                            _manager.PRepCount++;
                        }
                        else{//TTL is zero
                            //do nothing (drop the message).
                        }
                    }
                    else{// message has already been processed before
                        //do nothing (drop message)
                    }
                }
                else if (m instanceof PingReply){
                    // System.out.println("Sumeet: Getting ping reply");
                    Connection outConnection =
                        _manager.getRouteTable().get(m.getGUID());
                    _manager.getCatcher().spy(m);//update hostcatcher (even if this isn't for me)
                    if(outConnection!=null && _routeFilter.allow(m)){ //we have a place to route it
                        _manager.PRepCount++;
                        //HACK: is the reply for me?
                        if (outConnection.equals(_manager.ME_CONNECTION)) {
                            //System.out.println("Sumeet: I am the destination");
                            //Update horizon stats.
                            //This is not necessarily atomic, but that doesn't matter.
                            totalFileSize += ((PingReply)m).getKbytes();
                            numFiles += ((PingReply)m).getFiles();
                            numHosts++;
                            //System.out.println("Sumeet : updated stats");
                        }
                        else{//message needs to routed
                            m.hop();// It's Ok to route even if TTL is zero since this is a reply
                            outConnection.send(m);
                        }
                    }
                    else { //Route Table does not know what to do with message
                        //do nothing...drop the message
                        _manager.totDropped++;
                        dropped++;
                    }
                }
                else if (m instanceof QueryRequest){
                    Connection inConnection =
                        _manager.getRouteTable().get(m.getGUID());
                    if (inConnection==null && !isRouteSpam(m)){

                        // Feed to the UI Monitor
                        if (_personalFilter.allow(m))
                            _manager.getCallback().handleQueryString(
                                ((QueryRequest)m).getQuery());

                        //reduce TTL,increment hops, If old val of TTL was 0 drop message
                        //NOTE: This is Num Local Searches so always count it
                        _manager.QReqCount++;

                        if (m.hop()!=0){
                            _manager.getRouteTable().put(m.getGUID(),this);
                            _manager.sendToAllExcept(m,this); //broadcast to other hosts

                            FileManager fm = FileManager.getFileManager();
                            Response[] responses = fm.query((QueryRequest)m);

                            if (responses.length > 0) {
                                byte[] guid = m.getGUID();
                                byte ttl = (byte)(m.getHops() +1);
                                int port = _manager.getListeningPort();
                                byte[] ip= getLocalAddress().getAddress(); //little endian
                                long speed = SettingsManager.instance().getConnectionSpeed();
                                byte[] clientGUID = _manager.getClientGUID();

                                // changing the port here to test push:

                                //Modified by Sumeet Thadani
                                // If the number of responses is more 255, we are going to
                                // drop the responses after index 255. This can be corrected
                                //post beta, so that the extra responses can be sent along as
                                //another query reply.
                                if (responses.length > 255){
                                    Response[] res = new Response[255];
                                    for(int i=0; i<255;i++)
                                        res[i] = responses[i]; //copy first 255 elements of old array
                                    responses = res;//old array will be garbage collected
                                }
                                QueryReply qreply = new QueryReply(guid, ttl, port, ip,
                                                                   speed, responses, clientGUID);
                                //QueryReply qreply = new QueryReply(guid, ttl, 1234, ip,
                                //               speed, responses, clientGUID);
                                send(qreply);
                                _manager.QRepCount++;
                            }
                        }
                        else{//TTL is zero
                            //do nothing(drop the message)
                        }
                    }
                    else{//message has been entry in Route Table, has already been processed.
                        //do nothing (drop message)
                    }
                }
                else if (m instanceof QueryReply){
                    Connection outConnection =
                        _manager.getRouteTable().get(m.getGUID());
                    if(outConnection!=null && _routeFilter.allow(m)){ //we have a place to route it
                        _manager.QRepCount++;
                        //System.out.println("Sumeet:found connection");
                        QueryReply qrep = (QueryReply)m;
                        _manager.getPushRouteTable().put(qrep.getClientGUID(),
                                                         this);
                        //HACK: is the reply for me?
                        if (outConnection.equals(_manager.ME_CONNECTION)) {
                            //Unpack message and present it to user via
                            //ActivityCallback, if it is not spam.
                            if (_personalFilter.allow(m))
                                _manager.getCallback().handleQueryReply(
                                    (QueryReply)m);
                        }
                        else {//message needs to be routed.
                            m.hop(); // It's Ok to route even if TTL is zero since this is a reply
                            outConnection.send(m);//send the message along on its route
                        }
                    }
                    else{//route table does not know what to do this message
                        //do nothing...drop the message
                        _manager.totDropped++;
                        dropped++;
                    }
                }
                else if (m instanceof PushRequest){

                    //System.out.println("Requested a push");

                    _manager.pushCount++;
                    PushRequest req = (PushRequest)m;

                    byte[] req_guid = req.getClientGUID();
                    String req_guid_hexstring = (new GUID(req_guid)).toString();
                    Connection nextHost = _manager.getPushRouteTable().get(req_guid);

                    if (nextHost!=null && _routeFilter.allow(m)){//we have a place to route this message
                        m.hop(); // Ok to send even if ttl =0 since the message has a specific place to go
                        nextHost.send(m); //send the message to appropriate host
                    }

                    // This comparison doesn't work:
                    // // if (_manager.ClientId.equals(DestinationId) ){}
                    //I am the destination
                    else if (_manager.isClient(req_guid)) {
                        //unpack message
                        //make HTTP connection with originator
                        String host = new String(req.getIP());
                        byte[] ip = req.getIP();
                        StringBuffer buf=new StringBuffer();
                        buf.append(ByteOrder.ubyte2int(ip[0])+".");
                        buf.append(ByteOrder.ubyte2int(ip[1])+".");
                        buf.append(ByteOrder.ubyte2int(ip[2])+".");
                        buf.append(ByteOrder.ubyte2int(ip[3])+"");
                        String h = buf.toString();
                        int port = req.getPort();

                        FileManager fmanager = FileManager.getFileManager();
                        int index = (int)req.getIndex();

                        FileDesc desc;
                        try {
                            desc =(FileDesc)fmanager._files.get(index);
                        }
                        catch (Exception e) {
                            //I'm catching Exception because I don't know
                            //exactly which IndexOutOfBoundsException is
                            //thrown: from normal util package or com.sun...

                            //TODO?: You could connect and send 404 file
                            //not found....but why bother?
                            continue;
                        }
                        String file = desc._name;

                        HTTPUploader up = new
                        HTTPUploader(h, port, index, req_guid_hexstring, _manager);
                        Thread t=new Thread(up);
                        t.setDaemon(true);
                        t.run();
                    }
                    else{// the message has arrived in error or is spam
                        //do nothing.....drop the message
                        _manager.totRouteError++;
                        _manager.totDropped++;
                        dropped++;
                    }
                }// else if
            }//while
        }//try
        catch (IOException e){
            _manager.remove(this);

        }
        catch (Exception e) {
            //Internal error!  Cleanup this connection and notify the user
            //of the problem.
            _manager.remove(this);
            _manager.getCallback().error(ActivityCallback.ERROR_20, e);
        }
    }//run

    /** Clears the statistics about files reachable from me. */
    public void clearHorizonStats() {
        totalFileSize=0;
        numHosts=0;
        numFiles=0;
    }

    /** Returns the number of hosts reachable from me. */
    public long getNumHosts() {
        return numHosts;
    }

    /** Returns the number of files reachable from me. */
    public long getNumFiles() {
        return numFiles;
    }

    /** Returns the size of all files reachable from me. */
    public long getTotalFileSize() {
        return totalFileSize;
    }

    /** Returns the number of messages dropped on this connection.<p>
     *
     * Here, dropped messages mean replies to requests I didn't make,
     * bad packets, broadcasted pushes, or spam.  It does not include:
     * TTL's of zero, duplicate requests (it's not their fault), or
     * buffer overflows in sendToAll.
     */
    public long getNumDropped() {
        return dropped;
    }

    /** Return the percentage of messages dropped on this connection since
     *  the last call to getPercentDropped. */
    public float getPercentDropped() {
        int rdiff=getNumMessagesReceived()-lastReceived;
        int ddiff=dropped-lastDropped;
        float percent=(rdiff==0) ? 0.f : ((float)ddiff/(float)rdiff*100.f);

        lastReceived=getNumMessagesReceived();
        lastDropped=dropped;
        return percent;
    }

    /**
     * @modifies this
     * @effects sets the underlying routing filter.   Note that
     *  most filters are not thread-safe, so they should not be shared
     *  among multiple connections.
     */
    public void setRouteFilter(SpamFilter filter) {
        _routeFilter = filter;
    }

    /**
     * @modifies this
     * @effects sets the underlying personal filter.   Note that
     *  most filters are not thread-safe, so they should not be shared
     *  among multiple connections.
     */
    public void setPersonalFilter(SpamFilter filter) {
        _personalFilter = filter;
    }
}
