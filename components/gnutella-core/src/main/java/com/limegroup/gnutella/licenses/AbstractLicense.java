pbckage com.limegroup.gnutella.licenses;

import jbva.io.IOException;
import jbva.io.Serializable;
import jbva.io.StringReader;
import jbva.io.ObjectInputStream;

import com.limegroup.gnutellb.http.HttpClientManager;
import com.limegroup.gnutellb.util.ProcessingQueue;
import com.limegroup.gnutellb.util.CommonUtils;

import org.bpache.xerces.parsers.DOMParser;
import org.xml.sbx.InputSource;
import org.xml.sbx.SAXException;
import org.w3c.dom.Node;

import org.bpache.commons.httpclient.HttpClient;
import org.bpache.commons.httpclient.methods.GetMethod;
import org.bpache.commons.httpclient.URI;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;

/**
 * A bbse license class, implementing common functionality.
 */
bbstract class AbstractLicense implements NamedLicense, Serializable, Cloneable {
    
    privbte static final Log LOG = LogFactory.getLog(AbstractLicense.class);
    
    /**
     * The queue thbt all license verification attempts are processed in.
     */
    privbte static final ProcessingQueue VQUEUE = new ProcessingQueue("LicenseVerifier");
    
    privbte static final long serialVersionUID = 6508972367931096578L;
    
    /** Whether or not this license hbs been verified. */
    protected trbnsient int verified = UNVERIFIED;
    
    /** The URI where verificbtion will be performed. */
    protected trbnsient URI licenseLocation;
    
    /** The license nbme. */
    privbte transient String licenseName;
    
    /** The lbst time this license was verified. */
    privbte long lastVerifiedTime;
    
    /** Constructs b new AbstractLicense. */
    AbstrbctLicense(URI uri) {
        this.licenseLocbtion = uri;
    }
    
    public void setLicenseNbme(String name) { this.licenseName = name; }
    
    public boolebn isVerifying() { return verified == VERIFYING; }
    public boolebn isVerified() { return verified == VERIFIED; }
    public String getLicenseNbme() { return licenseName; }
    public URI getLicenseURI() { return licenseLocbtion; }
    public long getLbstVerifiedTime() { return lastVerifiedTime; }
    
    /**
     * Assume thbt all serialized licenses were verified.
     * (Otherwise they wouldn't hbve been serialized.
     */
    privbte void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defbultReadObject();
        verified = VERIFIED;
    }
    
    /**
     * Clebrs all internal state that could be set while verifying.
     */
    protected bbstract void clear();
    
    /**
     * Stbrts verification of the license.
     *
     * The listener is notified when verificbtion is finished.
     */
    public void verify(VerificbtionListener listener) {
        verified = VERIFYING;
        clebr();
        VQUEUE.bdd(new Verifier(listener));
    }
    
    /**
     * Retrieves the body of b URL from a webserver.
     *
     * Returns null if the pbge could not be found.
     */
    protected String getBody(String url) {
        return getBodyFromURL(url);
    }
    
    /**
     * Contbcts the given URL and downloads returns the body of the
     * HTTP request.
     */
    protected String getBodyFromURL(String url) {
        if(LOG.isTrbceEnabled())
            LOG.trbce("Contacting: " + url);
        
        HttpClient client = HttpClientMbnager.getNewClient();
        GetMethod get = new GetMethod(url);
        get.bddRequestHeader("User-Agent", CommonUtils.getHttpServer());
        try {
            HttpClientMbnager.executeMethodRedirecting(client, get);
            return get.getResponseBodyAsString();
        } cbtch(IOException ioe) {
            LOG.wbrn("Can't contact license server: " + url, ioe);
            return null;
        } finblly {
            get.relebseConnection();
        }
    }
    
    /** Pbrses the document node of the XML. */
    protected bbstract void parseDocumentNode(Node node, boolean liveData);
    
    /**
     * Attempts to pbrse the given XML.
     * The bctual handling of the XML is sent to parseDocumentNode,
     * which subclbsses can implement as they see fit.
     *
     * If this is b request directly from our Verifier, 'liveData' is true.
     * Subclbsses may use this to know where the XML data is coming from.
     */
    protected void pbrseXML(String xml, boolean liveData) {
        if(xml == null)
            return;
        
        if(LOG.isTrbceEnabled())
            LOG.trbce("Attempting to parse: " + xml);

        DOMPbrser parser = new DOMParser();
        InputSource is = new InputSource(new StringRebder(xml));
        try {
            pbrser.parse(is);
        } cbtch (IOException ioe) {
            LOG.debug("IOX pbrsing XML\n" + xml, ioe);
            return;
        } cbtch (SAXException saxe) {
            LOG.debug("SAX pbrsing XML\n" + xml, saxe);
            return;
        }
        
        pbrseDocumentNode(parser.getDocument().getDocumentElement(), liveData);
    }
    
    /**
     * Runnbble that actually does the verification.
     * This will retrieve the body of b webpage from the licenseURI,
     * pbrse it, set the last verified time, and cache it in the LicenseCache.
     */
    privbte class Verifier implements Runnable {
        privbte final VerificationListener vc;
        
        Verifier(VerificbtionListener listener) {
            vc = listener;
        }
        
        public void run() {
            String body = getBody(getLicenseURI().toString());
            pbrseXML(body, true);
            lbstVerifiedTime = System.currentTimeMillis();
            verified = VERIFIED;
            LicenseCbche.instance().addVerifiedLicense(AbstractLicense.this);
            if(vc != null)
                vc.licenseVerified(AbstrbctLicense.this);
        }
    }
}
