package com.limegroup.gnutella;

import com.sun.java.util.collections.*;
import java.io.*;
import java.net.InetAddress;

/**
 * The host catcher.  This peeks at pong messages coming on the
 * network and snatches IP addresses of other Gnutella peers.  IP
 * addresses may also be added to it from a file (usually
 * "gnutella.net").  The servent may then connect to these addresses
 * as necessary to maintain full connectivity.<p>
 *
 * Generally speaking, IP addresses from pongs are preferred to those
 * from the gnutella.net file, since they more likely to be live.
 * Hosts we are connected to are not added to the host catcher, but
 * they will be written to disk.
 */
public class HostCatcher {
    /* Our representation consists of both a set and a queue.  The
     * set lets us quickly check if there are duplicates, while the
     * queue provides ordering.  The elements at the END of the queue
     * have the highest priority.
     *
     * INVARIANT: queue contains no duplicates and contains exactly the
     * same elements as set.
     * LOCKING: obtain this' monitor before modifying either.
     */
    private List /* of Endpoint */ queue=new ArrayList();
    private Set /* of Endpoint */ set=new HashSet();
    private static final byte[] LOCALHOST={(byte)127, (byte)0, (byte)0,
                                           (byte)1};

    private Acceptor acceptor;
    private ConnectionManager manager;
    private ActivityCallback callback;

    /**
     * Creates an empty host catcher.  Must call initialize before using.
     */
    public HostCatcher(ActivityCallback callback) {
        this.callback=callback;
    }

    /**
     * Links the HostCatcher up with the other back end pieces
     */
    public void initialize(Acceptor acceptor, ConnectionManager manager) {
        this.acceptor = acceptor;
        this.manager = manager;
    }

    /**
     * Links the HostCatcher up with the other back end pieces, and, if quick
     * connect is not specified in the SettingsManager, loads
     * the hosts in the host list into the maybe set.  (The likelys set is
     * empty.)  If filename does not exist, then no error message is printed and * this is initially empty.  The file is expected to contain a sequence of
     * lines in the format "<host>:port\n".  Lines not in this format
     * are silently ignored.
     */
    public void initialize(Acceptor acceptor, ConnectionManager manager,
                           String filename) {
        this.acceptor = acceptor;
        this.manager = manager;
    }



    /**
     * Reads in endpoints from the given file
     * @modifies this
     * @effects read hosts from the given file.
     */
    public synchronized void read(String filename)
            throws FileNotFoundException, IOException {
        BufferedReader in=null;
        in=new BufferedReader(new FileReader(filename));
        while (true) {
            String line=in.readLine();
            if (line==null)   //nothing left to read?  Done.
                break;

            //Break the line into host and port.  Skip if badly formatted.
            int index=line.indexOf(':');
            if (index==-1) {
                continue;
            }
            String host=line.substring(0,index);
            int port=0;
            try {
                port=Integer.parseInt(line.substring(index+1));
            } catch (NumberFormatException e) {
                continue;
            } catch (ArrayIndexOutOfBoundsException e) {
                continue;
            }

            //Everything passed!  Add it.  No need to synchronize because
            //this is a constructor.
            Endpoint e = new Endpoint(host, port);
            if ((! set.contains(e)) && (! isMe(host, port))) {
                queue.add(0,e); //add e to the head.  Order matters!
            //no need to call notify since nothing can be waiting on this.
            set.add(e);
            }
        }
    }

    /**
     * @modifies the file named filename
     * @effects writes this to the given file.  The file
     *  is prioritized by rough probability of being good.
     */
    public synchronized void write(String filename) throws IOException {
        FileWriter out=new FileWriter(filename);
        //1) Write connections we're connected to--in no particular order.
        //   Also add the connections to a set for step (2).  Ignore incoming
        //   connections, since the remote host's port is ephemeral.
        Set connections=new HashSet();
        for (Iterator iter=manager.getInitializedConnections().iterator();
             iter.hasNext(); ) {
            Connection c=(Connection)iter.next();
            if (! c.isOutgoing()) //ignore incoming
                continue;
            Endpoint e=new Endpoint(c.getInetAddress().getHostAddress(),
                        c.getPort());
            connections.add(e);
            writeInternal(out, e);
        }

        //2.) Write hosts in this that are not in connections--in order.
        for (int i=queue.size()-1; i>=0; i--) {
            Endpoint e=(Endpoint)queue.get(i);
            if (connections.contains(e))
                continue;
            writeInternal(out, e);
        }
        out.close();
    }

    private void writeInternal(Writer out, Endpoint e) throws IOException {
        out.write(e.getHostname()+":"+e.getPort()+"\n");
    }

    //////////////////////////////////////////////////////////////////////


    /**
     * @modifies this
     * @effects may choose to add hosts listed in pr to this
     */
    public void spy(PingReply pr) {
        //We could also get ip addresses from query hits and push
        //requests, but then we have to guess the ports for incoming
        //connections.  (These only give ports for HTTP.)

        Endpoint e=new Endpoint(pr.getIP(), pr.getPort(),
                    pr.getFiles(), pr.getKbytes());

        //Skip if we're connected to it.
        if (manager.isConnected(e))
            return;

        //Skip if this would connect us to our listening port.
        if (isMe(e.getHostname(), e.getPort()))
            return;

        synchronized(this) {
            if (! (set.contains(e))) {
                set.add(e);
                queue.add(e);  //add to tail of the queue.  Order matters!
                this.notify(); //notify anybody waiting for a connection
            }
        }

        callback.knownHost(e);
    }


    /**
     * @modifies this
     * @effects atomically returns the highest priority host from this
     *  and removes it from this.  Throws NoSuchElementException if
     *  no host could be found.
     */
    public synchronized Endpoint getAnEndpoint() throws NoSuchElementException {
        if (! queue.isEmpty()) {
            //pop e from queue and remove from set.
            Endpoint e=(Endpoint)queue.remove(queue.size()-1);
            boolean ok=set.remove(e);
            //check that e actually was in set.
            Assert.that(ok, "Rep. invariant for HostCatcher broken.");
            return e;
        } else
            throw new NoSuchElementException();
        }

    /**
     *  Return the number of good hosts
     */
    public int getNumHosts() {
        return( queue.size() );
    }

    /**
     * Returns an iterator of the hosts in this, in order of priority.
     * This can be modified while iterating through the result, but
     * the modifications will not be observed.
     */
    public synchronized Iterator getHosts() {
        return getBestHosts(queue.size());
    }

    /**
     * @requires n>0
     * @effects returns an iterator that yields up the best n endpoints of this.
     *  It's not guaranteed that these are reachable.
     */
    public synchronized Iterator getBestHosts(int n) {
        Assert.that(n>=0);
        //Note that we have to add things to everything is REVERSE ORDER
        //so that they will be yielded from highest priority to lowest.
        List everything=new ArrayList();
        int length=queue.size();
        int last=(length > n) ? (length-n) : 0; //queue[last] is last host yielded
        for (int i=length-1; i>=last; i--) {
            Endpoint e=(Endpoint)queue.get(i);
            everything.add(e);
        }
        return( everything.iterator() );
    }

    /**
     *  Remove unwanted or used entries
     */
    public synchronized void removeHost(String host, int port) {
        Endpoint e=new Endpoint(host, port);
        boolean removed1=set.remove(e);
        boolean removed2=queue.remove(e);
        //Check that set.contains(e) <==> queue.contains(e)
        Assert.that(removed1==removed2, "Rep. invariant for HostCatcher broken.");
    }

    /**
     * @modifies this
     * @effects removes all entries from this
     */
    public synchronized void clear() {
        queue.clear();
        set.clear();
    }

    /**
     * If host is not a valid host address, returns false.
     * Otherwise, returns true if connecting to host:port would connect to
     *  the manager's listening port.
     */
    private boolean isMe(String host, int port) {
        //Don't allow connections to yourself.  We have to special
        //case connections to "localhost" or "127.0.0.1" since
        //they are aliases this machine.
        byte[] cIP;
        try {
            cIP=InetAddress.getByName(host).getAddress();
        } catch (IOException e) {
            return false;
        }

        if (Arrays.equals(cIP, LOCALHOST)) {
            return port == acceptor.getPort();
        } else {
            byte[] managerIP = acceptor.getAddress();
            return (Arrays.equals(cIP, managerIP) &&
                    (port==acceptor.getPort()));
        }
    }

    public String toString() {
        return queue.toString();
    }

//      /** Unit test. */
//      public static void main(String args[]) {
//      HostCatcher hc=new HostCatcher(null);
//      PingRequest ping=new PingRequest((byte)3);
//      Iterator iter=null;
//      Endpoint e=null;

//      iter=hc.getHosts();
//      Assert.that(! iter.hasNext());
//      iter=hc.getBestHosts(1);
//      Assert.that(! iter.hasNext());

//      //add and remove this
//      PingReply pr0=new PingReply(ping.getGUID(), (byte)5, 6346,
//                     new byte[] {(byte)127, (byte)0, (byte)0, (byte)0},
//                     10, 20);
//      hc.spy(pr0);
//      hc.removeHost("127.0.0.0", 6346);

//      //add these.  pr2 becomes the highest priority.
//      Endpoint e1=new Endpoint("127.0.0.1", 6347);
//      PingReply pr1=new PingReply(ping.getGUID(), (byte)5, 6347,
//                      new byte[] {(byte)127, (byte)0, (byte)0, (byte)1},
//                      0, 0);
//      hc.spy(pr1);
//      Endpoint e2=new Endpoint("127.0.0.2", 6348);
//      PingReply pr2=new PingReply(ping.getGUID(), (byte)5, 6348,
//                     new byte[] {(byte)127, (byte)0, (byte)0, (byte)2},
//                     10, 20);
//      hc.spy(pr2);

//      iter=hc.getBestHosts(10);
//      Assert.that(iter.hasNext());
//      e=(Endpoint)iter.next();
//      Assert.that(e.getFiles()==10);
//      Assert.that(e.getKbytes()==20);
//      Assert.that(e.equals(e2));
//      Assert.that(iter.hasNext());
//      Assert.that(iter.next().equals(e1));
//      Assert.that(!iter.hasNext());

//      iter=hc.getBestHosts(1);
//      Assert.that(iter.hasNext());
//      Assert.that(iter.next().equals(e2));
//      Assert.that(!iter.hasNext());

//      iter=hc.getHosts();
//      Assert.that(iter.hasNext());
//      Assert.that(iter.next().equals(e2));
//      Assert.that(iter.hasNext());
//      Assert.that(iter.next().equals(e1));
//      Assert.that(!iter.hasNext());

//      hc.clear();
//      Assert.that(!hc.getHosts().hasNext());
//      }
}

