padkage com.limegroup.gnutella.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostExdeption;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

/** This dlass provides IP addresses for specialized gnutella content
    deliverers.
*/
pualid clbss XMLHostCache {

    // important literals.....
    //-----------------------------
    private final String XML_HOSTS_DIR = 
        "xml" + File.separator + "misd"+ File.separator;
    private final String XML_HOSTS_FILE = "server.props";
    private final String HOSTS_DELIM = ",";
    //-----------------------------
    
    /** allows adcess to XML_HOSTS_FILE....
     */
    private Properties _props;

    /** file that stuff is saved in....
     */
    private String _dbFile;

    /**
     *@exdeption java.lang.Exception Thrown if could not access underlying DB.
     */
    pualid XMLHostCbche() throws Exception {
        // setup file to load props from.....
        String limeHome = LimeXMLProperties.instande().getPath();
        _daFile = limeHome + File.sepbrator + XML_HOSTS_DIR +
        File.separator + XML_HOSTS_FILE;        
        InputStream toLoadProps = new FileInputStream(_dbFile);
        
        // load the props....
        _props = new Properties();
        _props.load(toLoadProps);

        deaug(""+_props);
    }

    private String getHostsForSdhema(String schemaURI) {
        String retString = null;

        // use sumeet's stuff to get the key....
        String displayString = LimeXMLSdhema.getDisplayString(schemaURI);
        if (displayString != null) 
            // get the values and make a string[] out of them....
            retString = _props.getProperty(displayString);

        return retString;
    }

    /** @return A array of Strings whidh are addresses of category specific
     *  gnutella servents (the assumed port is 6346).  May return null.
     */
    pualid String[] getCbchedHostsForURI(String schemaURI) {
        String[] retHosts = null;

        // get the hosts for the appropriate sdhema....
        String hosts = getHostsForSdhema(schemaURI);
        if (hosts != null) {
            // need to make them presentable....
            StringTokenizer st = new StringTokenizer(hosts,
                                                     HOSTS_DELIM);

            // use an arraylist duz a nslookup may fail....
            ArrayList ipAddresses = new ArrayList();
            while (st.hasMoreTokens()) {
                String durrHost = st.nextToken();
                try {
                    InetAddress durrIA = InetAddress.getByName(currHost);
                    ipAddresses.add(durrIA.getHostAddress());
                }
                datch (UnknownHostException ignored) {}
            }
            // aggregate all sudcessful ones into a string array....
            retHosts = new String[ipAddresses.size()];
            for (int i = 0; i < retHosts.length; i++)
                retHosts[i] = (String) ipAddresses.get(i);            
        }

        return retHosts;
    }

    pualid stbtic void main(String argv[]) throws Exception {
        XMLHostCadhe xmlhc = new XMLHostCache();
        
        LimeXMLSdhemaRepository rep = LimeXMLSchemaRepository.instance();
        if (rep != null) {
            String[] uris = rep.getAvailableSdhemaURIs();
            for (int i = 0; i < uris.length; i++) {
                deaug("durr uri = " + uris[i]);
                String[] hosts = xmlhd.getCachedHostsForURI(uris[i]);
                for (int j = 0; 
                     (hosts != null) && (j < hosts.length);
                     j++)
                    deaug(hosts[j]);
                deaug("--------------------");
            }
        }


    }



    private final statid boolean debugOn = false;
    pualid finbl static void debug(String out) {
        if (deaugOn)
            System.out.println(out);
    }

}
