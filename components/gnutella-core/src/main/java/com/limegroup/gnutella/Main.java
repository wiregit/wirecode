package com.limegroup.gnutella;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.Vector;

import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.chat.Chatter;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.version.UpdateInformation;

/**
 * The command-line UI for the Gnutella servent.
 */
public class Main implements ActivityCallback, ErrorCallback {
    
    private static volatile boolean isShuttingDown = false;
    
    private static final String statsFileName = "OUTBOUND/";
    
    private static final String fileName = "stats.csv";
    
    private static int arg = 0;
    
    public static void main(String args[]) {
        Thread shutdownThread = new Thread() {
            public synchronized void run() {
                if(!isShuttingDown) {
                    isShuttingDown = true;
                }
                else {
                    return;
                }
                try{
                    System.out.println("exiting...");
                    RouterService.shutdown();
//                    SearchStatManager.instance().writeStatsToFile(statsFileName+fileName);
//                    SearchStatManager.instance().shutDown();
                    System.exit(0);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownThread);
        //start main
        ActivityCallback callback = new Main();
        //RouterService.setCallback(callback);
        RouterService service = new RouterService(callback);
        RouterService.preGuiInit();
        service.start();    
        try {
            if(args.length==0) {
                System.out.println("args usage: 1 for headless, 2 for console");
                System.exit(0);
            }
            arg = (new Integer(args[0])).intValue();
            if(arg==1) {
                startHeadless();
            }
            else {
                startConsole();
            }
        } catch(NumberFormatException ex) {
            System.out.println("Wrong args. Usage: 1 for headless, 2 for console");
            System.exit(1);
        }
    }
    
    private static void startHeadless() {
        //loop infinite
        while(true) {
            try {
                Thread.sleep(5000);
                continue;
            } catch (InterruptedException doNothing) {
                break;
            }
        }
    }
    
    private static void startConsole() {
        System.out.println("For a command list type help.");
        BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
        for ( ; ;) {
            System.out.print("LimeRouter> ");
            try {
                String command=in.readLine();
                if (command==null) {
                  break;
                }
                else if (command.equals("help")) {
                    System.out.println("catcher                  "+
                                       "Print host catcher.");
                    System.out.println("connect <host> [<port>]  "+
                                       "Connect to a host[:port].");
                    System.out.println("help                     "+
                                       "Print this message.");
                    System.out.println("listen <port>            "+
                                       "Set the port you are listening on.");
                    //              System.out.println("push                     "+
                    //                "Print push routes.");
                    System.out.println("query <string>           "+
                                       "Send a query to the network.");
                    System.out.println("quit                     "+
                                       "Quit the application.");
                    //              System.out.println("route                    "+
                    //                "Print routing tables.");
                    //              System.out.println("stat                     "+
                    //                "Print statistics.");
                    System.out.println("update                   "+
                                       "Send pings to update the statistics.");
                }
                else if (command.equals("quit"))
                    break;
                else if (command.equals("issupernode")) {
                    System.out.println(RouterService.isSupernode());
                    continue;
                }
//                else if (command.equals("searchstats"))
//                    SearchStatManager.instance().writeStatsToFile(fileName);
                else if (command.equals("numconnect")) {
                    System.out.println(RouterService.getConnectionManager().getNumConnections());
                    continue;
                }else if (command.equals("numupconnect")) {
                    //the number of up to up connections
                    System.out.println(RouterService.getConnectionManager().getNumUltrapeerConnections());
                    continue;
                }
                else if (command.equals("dhtInfo")) {
                    if(RouterService.isDHTNode()) {
                        System.out.println("DHT Status: "+(RouterService.isActiveDHTNode()?"Active":"Passive"));
                    } else {
                        System.out.println("Not a DHT node");
                    }
                    continue;
                }
                //Print routing tables
                else if (command.equals("connections")) {
                    RouterService.dumpConnections();
                    continue;
                }
                //          //Print connections
                //          else if (command.equals("push"))
                //              RouterService.dumpPushRouteTable();
                //Print push route
            
                String[] commands=split(command);
                //Connect to remote host (establish outgoing connection)
                if (commands.length>=2 && commands[0].equals("connect")) {
                    try {
                        int port=6346;
                        if (commands.length>=3)
                            port=Integer.parseInt(commands[2]);
                        RouterService.connectToHostBlocking(commands[1], port);
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
                    RouterService.query(RouterService.newQueryGUID(), query);
                } else if (commands.length==2 && commands[0].equals("listen")) {
                    try {
                        int port=Integer.parseInt(commands[1]);
                        RouterService.setListeningPort(port);
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
        isShuttingDown = true;
        System.out.println("Good bye.");
        RouterService.shutdown();
//        SearchStatManager.instance().writeStatsToFile(fileName);
//        SearchStatManager.instance().shutDown();
        System.exit(0);
    }
    
    /////////////////////////// ActivityCallback methods //////////////////////
    
    public void handleAddressStateChanged() {}

    public void handleLifecycleEvent(LifecycleEvent evt) {}

    
//     public void handleQueryReply( QueryReply qr ) {
//      synchronized(System.out) {
//          System.out.println("Query reply from "+qr.getIP()+":"+qr.getPort()+":");
//          try {
//              for (Iterator iter=qr.getResults(); iter.hasNext(); )
//                  System.out.println("   "+((Response)iter.next()).getName());
//          } catch (BadPacketException e) { }
//      }
//     }

    public void handleQueryResult(RemoteFileDesc rfd ,HostData data, Set loc) {
        synchronized(System.out) {
            System.out.println("Query hit from "+rfd.getHost()+":"+rfd.getPort()+":");
            System.out.println("   "+rfd.getFileName());
        }
    }

    /**
     *  Add a query string to the monitor screen
     */
    public void handleQueryString( String query ) {
    }


    public void error(int errorCode) {
        error(errorCode, null);
    }
    
    public void error(Throwable problem, String msg) {
        problem.printStackTrace();
        System.out.println(msg);
    }

    /**
     * Implements ActivityCallback.
     */
    public void error(Throwable problem) {
        problem.printStackTrace();
    }

    public void error(int message, Throwable t) {
        System.out.println("Error: "+message);
        t.printStackTrace();
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


    public boolean overwriteFile(String file) {return false;};

    public void addDownload(Downloader mgr) {}

    public void removeDownload(Downloader mgr) {}

    public void addUpload(Uploader mgr) {}

    public void removeUpload(Uploader mgr) {}

    public void setPort(int port){}

    public int getNumUploads(){ return 0; }
    
    public boolean warnAboutSharingSensitiveDirectory(final File dir) { return false; }
    
    public void handleFileEvent(FileManagerEvent evt) {}
    
    public void handleSharedFileUpdate(File file) {}

    public void fileManagerLoading() {}

    public void acceptChat(Chatter chat) {}

    public void receiveMessage(Chatter chat) {}
    
    public void chatUnavailable(Chatter chatter) {}

    public void chatErrorMessage(Chatter chatter, String st) {}
        
    public void downloadsComplete() {}    
    
    public void fileManagerLoaded() {}    
    
    public void uploadsComplete() {}

    public void promptAboutCorruptDownload(Downloader dloader) {
        dloader.discardCorruptDownload(false);
    }

    public void restoreApplication() {}

    public void showDownloads() {}

    public String getHostValue(String key){
        return null;
    }
    public void browseHostFailed(GUID guid) {}

    public void setAnnotateEnabled(boolean enabled) {}
    
    public void updateAvailable(UpdateInformation update) {
        if (update.getUpdateCommand() != null)
            System.out.println("there's a new version out "+update.getUpdateVersion()+
                    ", to get it shutdown limewire and run "+update.getUpdateCommand());
        else
            System.out.println("You're running an older version.  Get " +
                         update.getUpdateVersion() + ", from " + update.getUpdateURL());
    }  

    public boolean isQueryAlive(GUID guid) {
        return false;
    }
    
    public void componentLoading(String component) {
        System.out.println("Loading component: " + component);
    }
    
    public void addressStateChanged() {}

    public boolean handleMagnets(final MagnetOptions[] magnets) {
        return false;
    }

    public void acceptedIncomingChanged(boolean status) { }

}

