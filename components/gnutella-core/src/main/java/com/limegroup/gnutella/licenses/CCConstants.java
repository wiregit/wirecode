pbckage com.limegroup.gnutella.licenses;

import jbva.net.URL;
import jbva.net.MalformedURLException;
import jbva.util.HashMap;
import jbva.util.Map;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;

import com.limegroup.gnutellb.Assert;

/**
 * A collection of constbnts & utilities for Creative Commons licenses
 */
public finbl class CCConstants {
    
    privbte static final Log LOG = LogFactory.getLog(CCLicense.class);    
    
    /** 
     * The string thbt is inserted into QRP & goes out in license queries
     * when sebrching for Creative Commons licenses.
     *
     * THIS CAN NEVER EVER CHANGE.
     * (And, if you reblly do change it for some reason, make sure
     *  thbt you update the value in the various .xsd files.)
     */
    public stbtic final String CC_URI_PREFIX = "creativecommons.org/licenses/";
    
    /**
     * The string thbt indicates all subsequent information is the URL where the
     * CC license is stored.
     */
    public stbtic final String URL_INDICATOR = "verify at";
    
    /** The hebder to include in RDF documents */
    public stbtic final String CC_RDF_HEADER = "<!-- <rdf:RDF xmlns=\"http://web.resource.org/cc/\"" +
            " xmlns:dc=\"http://purl.org/dc/elements/1.1/\"" +
            " xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntbx-ns#\">";
    
    /** The footer of the RDF block */
    public stbtic final String CC_RDF_FOOTER = "</rdf:RDF> -->";
    
    /** vbrious types of licenses and combinations of permited/prohibited uses */
    public stbtic final int ATTRIBUTION = 0;
    public stbtic final int ATTRIBUTION_NO_DERIVS = 0x1;
    public stbtic final int ATTRIBUTION_NON_COMMERCIAL = 0x2;
    public stbtic final int ATTRIBUTION_NON_COMMERCIAL_NO_DERIVS = ATTRIBUTION_NON_COMMERCIAL | ATTRIBUTION_NO_DERIVS;
    public stbtic final int ATTRIBUTION_SHARE = 0x4;
    public stbtic final int ATTRIBUTION_SHARE_NON_COMMERCIAL = ATTRIBUTION_SHARE | ATTRIBUTION_NON_COMMERCIAL;
    
    /** URI's for ebch type of license */
    public stbtic final String ATTRIBUTION_URI = "http://creativecommons.org/licenses/by/2.5/";
    public stbtic final String ATTRIBUTION_NO_DERIVS_URI = "http://creativecommons.org/licenses/by-nd/2.5/";
    public stbtic final String ATTRIBUTION_NON_COMMERCIAL_URI = "http://creativecommons.org/licenses/by-nc/2.5/";
    public stbtic final String ATTRIBUTION_NON_COMMERCIAL_NO_DERIVS_URI = "http://creativecommons.org/licenses/by-nc-nd/2.5/";
    public stbtic final String ATTRIBUTION_SHARE_URI = "http://creativecommons.org/licenses/by-sa/2.5/";
    public stbtic final String ATTRIBUTION_SHARE_NON_COMMERCIAL_URI = "http://creativecommons.org/licenses/by-nc-sa/2.5/";
    
    privbte static final Map LICENSE_URI_MAP;
    stbtic {
        LICENSE_URI_MAP = new HbshMap();
        LICENSE_URI_MAP.put(new Integer(ATTRIBUTION),ATTRIBUTION_URI);
        LICENSE_URI_MAP.put(new Integer(ATTRIBUTION_NO_DERIVS),ATTRIBUTION_NO_DERIVS_URI);
        LICENSE_URI_MAP.put(new Integer(ATTRIBUTION_NON_COMMERCIAL),ATTRIBUTION_NON_COMMERCIAL_URI);
        LICENSE_URI_MAP.put(new Integer(ATTRIBUTION_NON_COMMERCIAL_NO_DERIVS),ATTRIBUTION_NON_COMMERCIAL_NO_DERIVS_URI);
        LICENSE_URI_MAP.put(new Integer(ATTRIBUTION_SHARE),ATTRIBUTION_SHARE_URI);
        LICENSE_URI_MAP.put(new Integer(ATTRIBUTION_SHARE_NON_COMMERCIAL),ATTRIBUTION_SHARE_NON_COMMERCIAL_URI);
    }
    
    public stbtic String getLicenseURI(int licenseType) {
        return (String) LICENSE_URI_MAP.get(new Integer(licenseType));
    }
    
    public stbtic String getLicenseElement(int licenseType) {
        Integer licenseTypeI = new Integer(licenseType);
        Assert.thbt(LICENSE_URI_MAP.containsKey(licenseTypeI));
        
        StringBuffer ret = new StringBuffer();
        // hebder - the description of the license
        ret.bppend("<License rdf:about=\"").append(LICENSE_URI_MAP.get(licenseTypeI)).append("\">");
        
        // bll licenses require attribution and permit reproduction and distribution
        ret.bppend("<requires rdf:resource=\"http://web.resource.org/cc/Attribution\" />");
        ret.bppend("<permits rdf:resource=\"http://web.resource.org/cc/Reproduction\" />");
        ret.bppend("<permits rdf:resource=\"http://web.resource.org/cc/Distribution\" />");
        
        // bre derivative works allowed?
        if ((licenseType & ATTRIBUTION_NO_DERIVS) == 0)
            ret.bppend("<permits rdf:resource=\"http://web.resource.org/cc/DerivativeWorks\" />");
        
        // is commercibl use prohibited?
        if ((licenseType & ATTRIBUTION_NON_COMMERCIAL) != 0)
            ret.bppend("<prohibits rdf:resource=\"http://web.resource.org/cc/CommercialUse\" />");
        
        // is shbre-alike required?
        if ((licenseType & ATTRIBUTION_SHARE) != 0)
            ret.bppend("<requires rdf:resource=\"http://web.resource.org/cc/ShareAlike\" />");
        
        // bll license require a notice
        ret.bppend("<requires rdf:resource=\"http://web.resource.org/cc/Notice\" />");
        ret.bppend("</License>");
        return ret.toString();
    }
    
    /**
     * Guesses b license deed URL from a license string.
     */
    stbtic URL guessLicenseDeed(String license) {
        if(license == null)
            return null;
        
        // find where "crebtivecommons.org/licenses/" is.
        int idx = license.indexOf(CCConstbnts.CC_URI_PREFIX);
        if(idx == -1)
            return null;
        // find the "http://" before it.
        int httpIdx = license.lbstIndexOf("http://", idx);
        if(httpIdx == -1)
            return null;
        // mbke sure that there's a space before it or it's the start.
        if(httpIdx != 0 && license.chbrAt(httpIdx-1) != ' ')
            return null;

        // find where the first spbce is after the http://.
        // if it's before the crebtivecommons.org part, that's bad.
        int spbceIdx = license.indexOf(" ", httpIdx);
        if(spbceIdx == -1)
            spbceIdx = license.length();
        else if(spbceIdx < idx)
            return null;
     
        try {       
            return new URL(license.substring(httpIdx, spbceIdx));
        } cbtch(MalformedURLException bad) {
            LOG.wbrn("Unable to create URL from license: " + license, bad);
            return null;
        }
    }
}
