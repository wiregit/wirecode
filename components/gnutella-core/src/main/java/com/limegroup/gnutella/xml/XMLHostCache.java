package com.limegroup.gnutella.xml;

import java.io.*;
import com.sun.java.util.collections.*;
import java.util.*;
import java.net.*;

/** This class provides IP addresses for specialized gnutella content
    deliverers.
*/
public class XMLHostCache {

    // important literals.....
    //-----------------------------
    private final String XML_HOSTS_DIR = 
        "xml" + File.separator + "misc"+ File.separator;
    private final String XML_HOSTS_FILE = "server.props";
    private final String HOSTS_DELIM = ",";
    //-----------------------------
    
    /** allows access to XML_HOSTS_FILE....
     */
    private Properties _props;

    /** file that stuff is saved in....
     */
    private String _dbFile;

    /**
     *@exception java.lang.Exception Thrown if could not access underlying DB.
     */
    public XMLHostCache() throws Exception {
        // setup file to load props from.....
        String limeHome = LimeXMLProperties.instance().getPath();
        _dbFile = limeHome + File.separator + XML_HOSTS_DIR +
        File.separator + XML_HOSTS_FILE;        
        InputStream toLoadProps = new FileInputStream(_dbFile);
        
        // load the props....
        _props = new Properties();
        _props.load(toLoadProps);

        debug(""+_props);
    }

    private String getHostsForSchema(String schemaURI) {
        String retString = null;

        // use sumeet's stuff to get the key....
        String displayString = LimeXMLSchema.getDisplayString(schemaURI);
        if (displayString != null) 
            // get the values and make a string[] out of them....
            retString = _props.getProperty(displayString);

        return retString;
    }

    private boolean setHostsForSchema(String schemaURI, String hosts) 
    throws IOException {
        boolean success = false;
        // use sumeet's stuff to get the key....
        String displayString = LimeXMLSchema.getDisplayString(schemaURI);
        if (displayString != null) {
            // get the values and make a string[] out of them....
            _props.put(displayString, hosts);
            success = true;
        }
        return success;
    }


    /** @return A array of Strings which are addresses of category specific
     *  gnutella servents (the assumed port is 6346).  May return null.
     */
    public String[] getCachedHostsForURI(String schemaURI) {
        String[] retHosts = null;

        // get the hosts for the appropriate schema....
        String hosts = getHostsForSchema(schemaURI);
        if (hosts != null) {
            // need to make them presentable....
            StringTokenizer st = new StringTokenizer(hosts,
                                                     HOSTS_DELIM);

            // use an arraylist cuz a nslookup may fail....
            com.sun.java.util.collections.ArrayList ipAddresses = 
            new com.sun.java.util.collections.ArrayList();
            while (st.hasMoreTokens()) {
                String currHost = st.nextToken();
                try {
                    InetAddress currIA = InetAddress.getByName(currHost);
                    ipAddresses.add(currIA.getHostAddress());
                }
                catch (Exception ignored) {}
            }
            // aggregate all successful ones into a string array....
            retHosts = new String[ipAddresses.size()];
            for (int i = 0; i < retHosts.length; i++)
                retHosts[i] = (String) ipAddresses.get(i);            
        }

        return retHosts;
    }

    /** Adds a host for the given schema to the 'db'.
     */
    private void cacheHostForURI(String schemaURI, String host) {
        String hosts = getHostsForSchema(schemaURI);
        if (hosts != null) {
            hosts = hosts + HOSTS_DELIM + host;
        }
        else
            hosts = host;
        try {
            if (setHostsForSchema(schemaURI, hosts)) {
                OutputStream os = new FileOutputStream(_dbFile);
                _props.save(os,null);
            }
        }
        catch (Exception ignored) {
        }
    }


    public static void main(String argv[]) throws Exception {
        XMLHostCache xmlhc = new XMLHostCache();
        
        LimeXMLSchemaRepository rep = LimeXMLSchemaRepository.instance();
        if (rep != null) {
            String[] uris = rep.getAvailableSchemaURIs();
            for (int i = 0; i < uris.length; i++) {
                debug("curr uri = " + uris[i]);
                String[] hosts = xmlhc.getCachedHostsForURI(uris[i]);
                for (int j = 0; 
                     (hosts != null) && (j < hosts.length);
                     j++)
                    debug(hosts[j]);
                debug("--------------------");
            }
        }


    }



    private final static boolean debugOn = false;
    public final static void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }

}
