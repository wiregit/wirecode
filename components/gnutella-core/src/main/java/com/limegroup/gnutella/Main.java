package com.limegroup.gnutella;

import java.io.*;
import java.util.*;

/**
 * The command-line UI for the Gnutella servent.
 */
public class Main implements ActivityCallback {
    public static void main(String args[]) {
    //Start thread to accept connections.  Optional first arg is the
    //listening port number.
    RouterService service;
    ActivityCallback callback = new Main();
    if (args.length==1) {
        service=new RouterService(Integer.parseInt(args[0]),
                                  callback,
                                  new StandardMessageRouter(callback));
    } else {
        service=new RouterService(callback,
                                  new StandardMessageRouter(callback));

    }

    BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
    for ( ; ;) {
        System.out.print("LimeRouter> ");
        try {
        String command=in.readLine();
        if (command==null)
            break;
        else if (command.equals("quit"))
            break;
        //Print routing tables
        else if (command.equals("route"))
            service.dumpRouteTable();
        //Print connections
        else if (command.equals("push"))
            service.dumpPushRouteTable();
        //Print push route
        else if (command.equals("stat")) {
            service.dumpConnections();
            System.out.println("Number of hosts: "+service.getNumHosts());
            System.out.println("Number of files: "+service.getNumFiles());
            System.out.println("Size of files: "+service.getTotalFileSize());
        }
        //Send pings to everyone
        else if (command.equals("update"))
            service.updateHorizon();
        //Print hostcatcher
        else if (command.equals("catcher")) {
            for (Iterator iter=service.getHosts(); iter.hasNext(); )
            System.out.println(iter.next().toString());
        }
        String[] commands=split(command);
        //Connect to remote host (establish outgoing connection)
        if (commands.length>=2 && commands[0].equals("connect")) {
            try {
            int port=6346;
            if (commands.length>=3)
                port=Integer.parseInt(commands[2]);
            service.connectToHostBlocking(commands[1], port);
            } catch (IOException e) {
            System.out.println("Couldn't establish connection.");
            } catch (NumberFormatException e) {
            System.out.println("Please specify a valid port.");
            }
        } else if (commands.length>=2 && commands[0].equals("query")) {
            //Get query string from command (possibly multiple words)
            int i=command.indexOf(' ');
            Assert.that(i!=-1 && i<command.length());
            String query=command.substring(i+1);
            service.query(query,0);
        } else if (commands.length==2 && commands[0].equals("listen")) {
            try {
            int port=Integer.parseInt(commands[1]);
            service.setListeningPort(port);
            } catch (NumberFormatException e) {
            System.out.println("Please specify a valid port.");
            } catch (IOException e) {
            System.out.println("Couldn't change port.  Try another value.");
            }
        }
        } catch (IOException e) {
        System.exit(1);
        }
    }
    System.out.println("Good bye.");
    service.shutdown(); //write gnutella.net
    }

    /////////////////////////// ActivityCallback methods //////////////////////

    public void connectionInitializing(Connection c) {
    String host = c.getOrigHost();
    int    port = c.getOrigPort();
    String direction=null;
    String direction2=null;
    if (c.isOutgoing()) {
        direction="outgoing";
        direction2="to ";
    } else {
        direction="incoming";
        direction2="from ";
    }

    ;//System.out.println("Creating "+direction+" connection "+direction2+host+":"+port+"...");
    }

    public void connectionInitialized(Connection c) {
    String host = c.getOrigHost();
    int    port = c.getOrigPort();
    ;//System.out.println("Connected to "+host+":"+port+".");
    }

    public void connectionClosed(Connection c) {
    String host = c.getOrigHost();
    int    port = c.getOrigPort();
    //System.out.println("Connection to "+host+":"+port+" closed.");
    }

    public void knownHost(Endpoint e) {
    //Do nothing.
    }

    public void handleQueryReply( QueryReply qr ) {
    synchronized(System.out) {
        System.out.println("Query reply from "+qr.getIP()+":"+qr.getPort()+":");
        try {
        for (Iterator iter=qr.getResults(); iter.hasNext(); )
            System.out.println("   "+((Response)iter.next()).getName());
        } catch (BadPacketException e) { }
    }
    }

    /**
     *  Add a query string to the monitor screen
     */
    public void handleQueryString( String query ) {
    }


    public void error(int message) {
    error(message, null);
    }

    public void error(int message, Throwable t) {
    System.out.println("Error: "+message);
    }

    ///////////////////////////////////////////////////////////////////////////


    /** Returns an array of strings containing the words of s, where
     *  a word is any sequence of characters not containing a space.
     */
    public static String[] split(String s) {
    s=s.trim();
    int n=s.length();
    if (n==0)
        return new String[0];
    Vector buf=new Vector();

    //s[i] is the start of the word to add to buf
    //s[j] is just past the end of the word
    for (int i=0; i<n; ) {
        Assert.that(s.charAt(i)!=' ');
        int j=s.indexOf(' ',i+1);
        if (j==-1)
        j=n;
        buf.add(s.substring(i,j));
        //Skip past whitespace (if any) following s[j]
        for (i=j+1; j<n ; ) {
        if (s.charAt(i)!=' ')
            break;
        i++;
        }
    }
    String[] ret=new String[buf.size()];
    for (int i=0; i<ret.length; i++)
        ret[i]=(String)buf.get(i);
    return ret;
    }


    /*******************************/

    public boolean overwriteFile(String file) {return false;};

    public void addDownload(Downloader mgr) {}

    public void removeDownload(Downloader mgr) {}

    public void addUpload(HTTPUploader mgr) {}

    public void removeUpload(HTTPUploader mgr) {}

    public void setPort(int port){}

    public int getNumUploads(){ return 0; }

}

