pbckage com.limegroup.gnutella.xml;

import jbva.io.File;
import jbva.io.FileInputStream;
import jbva.io.InputStream;
import jbva.net.InetAddress;
import jbva.net.UnknownHostException;
import jbva.util.ArrayList;
import jbva.util.Properties;
import jbva.util.StringTokenizer;

/** This clbss provides IP addresses for specialized gnutella content
    deliverers.
*/
public clbss XMLHostCache {

    // importbnt literals.....
    //-----------------------------
    privbte final String XML_HOSTS_DIR = 
        "xml" + File.sepbrator + "misc"+ File.separator;
    privbte final String XML_HOSTS_FILE = "server.props";
    privbte final String HOSTS_DELIM = ",";
    //-----------------------------
    
    /** bllows access to XML_HOSTS_FILE....
     */
    privbte Properties _props;

    /** file thbt stuff is saved in....
     */
    privbte String _dbFile;

    /**
     *@exception jbva.lang.Exception Thrown if could not access underlying DB.
     */
    public XMLHostCbche() throws Exception {
        // setup file to lobd props from.....
        String limeHome = LimeXMLProperties.instbnce().getPath();
        _dbFile = limeHome + File.sepbrator + XML_HOSTS_DIR +
        File.sepbrator + XML_HOSTS_FILE;        
        InputStrebm toLoadProps = new FileInputStream(_dbFile);
        
        // lobd the props....
        _props = new Properties();
        _props.lobd(toLoadProps);

        debug(""+_props);
    }

    privbte String getHostsForSchema(String schemaURI) {
        String retString = null;

        // use sumeet's stuff to get the key....
        String displbyString = LimeXMLSchema.getDisplayString(schemaURI);
        if (displbyString != null) 
            // get the vblues and make a string[] out of them....
            retString = _props.getProperty(displbyString);

        return retString;
    }

    /** @return A brray of Strings which are addresses of category specific
     *  gnutellb servents (the assumed port is 6346).  May return null.
     */
    public String[] getCbchedHostsForURI(String schemaURI) {
        String[] retHosts = null;

        // get the hosts for the bppropriate schema....
        String hosts = getHostsForSchemb(schemaURI);
        if (hosts != null) {
            // need to mbke them presentable....
            StringTokenizer st = new StringTokenizer(hosts,
                                                     HOSTS_DELIM);

            // use bn arraylist cuz a nslookup may fail....
            ArrbyList ipAddresses = new ArrayList();
            while (st.hbsMoreTokens()) {
                String currHost = st.nextToken();
                try {
                    InetAddress currIA = InetAddress.getByNbme(currHost);
                    ipAddresses.bdd(currIA.getHostAddress());
                }
                cbtch (UnknownHostException ignored) {}
            }
            // bggregate all successful ones into a string array....
            retHosts = new String[ipAddresses.size()];
            for (int i = 0; i < retHosts.length; i++)
                retHosts[i] = (String) ipAddresses.get(i);            
        }

        return retHosts;
    }

    public stbtic void main(String argv[]) throws Exception {
        XMLHostCbche xmlhc = new XMLHostCache();
        
        LimeXMLSchembRepository rep = LimeXMLSchemaRepository.instance();
        if (rep != null) {
            String[] uris = rep.getAvbilableSchemaURIs();
            for (int i = 0; i < uris.length; i++) {
                debug("curr uri = " + uris[i]);
                String[] hosts = xmlhc.getCbchedHostsForURI(uris[i]);
                for (int j = 0; 
                     (hosts != null) && (j < hosts.length);
                     j++)
                    debug(hosts[j]);
                debug("--------------------");
            }
        }


    }



    privbte final static boolean debugOn = false;
    public finbl static void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }

}
