package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.*;
import java.io.*;
import java.net.*;
import java.text.ParseException;

import com.sun.java.util.collections.*;

public final class Connector {
    
    private static final Connector INSTANCE = new Connector();

    private final File HOST_FILE = new File(CommonUtils.getUserSettingsDir(), "hosts.txt");

    /**
     * Constant for the <tt>Set</tt> of hosts to attempt to connect to.
     */
    private final Set HOSTS = new HashSet();

    public static Connector instance() {
        return INSTANCE;
    }

    private Connector() {}

    public void start() {
        Runnable hostFileReader = new Runnable() {
                public void run() {
                    try {
                        read(HOST_FILE);
                    } catch (FileNotFoundException e) {
                    } catch (IOException e) {
                    }

                    pingHosts(); 
                }
            };
        Thread hostFileReaderThread = new Thread(hostFileReader, "host file reader");
        hostFileReaderThread.setDaemon(true);
        hostFileReaderThread.start();
    }

    /**
     * Reads in endpoints from the given file.
     *
     * @param hostFile the <tt>File</tt> to read from
     */
    private void read(File hostFile) throws FileNotFoundException, 
                                            IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(hostFile));
            while (true) {
                String line = in.readLine();
                if (line==null)
                    break;                  
    
                //Is it a normal endpoint?
                try {
                    HOSTS.add(ExtendedEndpoint.read(line));
                } catch (ParseException pe) {
                    continue;
                }
            }
        } finally {
            try {
                if( in != null )
                    in.close();
            } catch(IOException e) {}
        }
    }


    private void pingHosts() {
        System.out.println("Connector::pingHosts::hosts: "+HOSTS.size()); 
        PingRequest pr = new PingRequest();
        Iterator iter = HOSTS.iterator();
        int i = 0;
        while(iter.hasNext() && i < 1000) {
            ExtendedEndpoint ee = (ExtendedEndpoint)iter.next();
            try {
                InetAddress ia = InetAddress.getByAddress(ee.getHostBytes());
                UDPService.instance().send(pr, ia, ee.getPort());
            } catch(UnknownHostException e) {
                // no problem -- keep going
                continue;
            }
            i++;
        }
    }
}
