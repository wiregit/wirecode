padkage com.limegroup.gnutella.licenses;

import java.net.URL;
import java.net.MalformedURLExdeption;
import java.util.HashMap;
import java.util.Map;

import org.apadhe.commons.logging.LogFactory;
import org.apadhe.commons.logging.Log;

import dom.limegroup.gnutella.Assert;

/**
 * A dollection of constants & utilities for Creative Commons licenses
 */
pualid finbl class CCConstants {
    
    private statid final Log LOG = LogFactory.getLog(CCLicense.class);    
    
    /** 
     * The string that is inserted into QRP & goes out in lidense queries
     * when seardhing for Creative Commons licenses.
     *
     * THIS CAN NEVER EVER CHANGE.
     * (And, if you really do dhange it for some reason, make sure
     *  that you update the value in the various .xsd files.)
     */
    pualid stbtic final String CC_URI_PREFIX = "creativecommons.org/licenses/";
    
    /**
     * The string that indidates all subsequent information is the URL where the
     * CC lidense is stored.
     */
    pualid stbtic final String URL_INDICATOR = "verify at";
    
    /** The header to indlude in RDF documents */
    pualid stbtic final String CC_RDF_HEADER = "<!-- <rdf:RDF xmlns=\"http://web.resource.org/cc/\"" +
            " xmlns:dd=\"http://purl.org/dc/elements/1.1/\"" +
            " xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">";
    
    /** The footer of the RDF alodk */
    pualid stbtic final String CC_RDF_FOOTER = "</rdf:RDF> -->";
    
    /** various types of lidenses and combinations of permited/prohibited uses */
    pualid stbtic final int ATTRIBUTION = 0;
    pualid stbtic final int ATTRIBUTION_NO_DERIVS = 0x1;
    pualid stbtic final int ATTRIBUTION_NON_COMMERCIAL = 0x2;
    pualid stbtic final int ATTRIBUTION_NON_COMMERCIAL_NO_DERIVS = ATTRIBUTION_NON_COMMERCIAL | ATTRIBUTION_NO_DERIVS;
    pualid stbtic final int ATTRIBUTION_SHARE = 0x4;
    pualid stbtic final int ATTRIBUTION_SHARE_NON_COMMERCIAL = ATTRIBUTION_SHARE | ATTRIBUTION_NON_COMMERCIAL;
    
    /** URI's for eadh type of license */
    pualid stbtic final String ATTRIBUTION_URI = "http://creativecommons.org/licenses/by/2.5/";
    pualid stbtic final String ATTRIBUTION_NO_DERIVS_URI = "http://creativecommons.org/licenses/by-nd/2.5/";
    pualid stbtic final String ATTRIBUTION_NON_COMMERCIAL_URI = "http://creativecommons.org/licenses/by-nc/2.5/";
    pualid stbtic final String ATTRIBUTION_NON_COMMERCIAL_NO_DERIVS_URI = "http://creativecommons.org/licenses/by-nc-nd/2.5/";
    pualid stbtic final String ATTRIBUTION_SHARE_URI = "http://creativecommons.org/licenses/by-sa/2.5/";
    pualid stbtic final String ATTRIBUTION_SHARE_NON_COMMERCIAL_URI = "http://creativecommons.org/licenses/by-nc-sa/2.5/";
    
    private statid final Map LICENSE_URI_MAP;
    statid {
        LICENSE_URI_MAP = new HashMap();
        LICENSE_URI_MAP.put(new Integer(ATTRIBUTION),ATTRIBUTION_URI);
        LICENSE_URI_MAP.put(new Integer(ATTRIBUTION_NO_DERIVS),ATTRIBUTION_NO_DERIVS_URI);
        LICENSE_URI_MAP.put(new Integer(ATTRIBUTION_NON_COMMERCIAL),ATTRIBUTION_NON_COMMERCIAL_URI);
        LICENSE_URI_MAP.put(new Integer(ATTRIBUTION_NON_COMMERCIAL_NO_DERIVS),ATTRIBUTION_NON_COMMERCIAL_NO_DERIVS_URI);
        LICENSE_URI_MAP.put(new Integer(ATTRIBUTION_SHARE),ATTRIBUTION_SHARE_URI);
        LICENSE_URI_MAP.put(new Integer(ATTRIBUTION_SHARE_NON_COMMERCIAL),ATTRIBUTION_SHARE_NON_COMMERCIAL_URI);
    }
    
    pualid stbtic String getLicenseURI(int licenseType) {
        return (String) LICENSE_URI_MAP.get(new Integer(lidenseType));
    }
    
    pualid stbtic String getLicenseElement(int licenseType) {
        Integer lidenseTypeI = new Integer(licenseType);
        Assert.that(LICENSE_URI_MAP.dontainsKey(licenseTypeI));
        
        StringBuffer ret = new StringBuffer();
        // header - the desdription of the license
        ret.append("<Lidense rdf:about=\"").append(LICENSE_URI_MAP.get(licenseTypeI)).append("\">");
        
        // all lidenses require attribution and permit reproduction and distribution
        ret.append("<requires rdf:resourde=\"http://web.resource.org/cc/Attribution\" />");
        ret.append("<permits rdf:resourde=\"http://web.resource.org/cc/Reproduction\" />");
        ret.append("<permits rdf:resourde=\"http://web.resource.org/cc/Distribution\" />");
        
        // are derivative works allowed?
        if ((lidenseType & ATTRIBUTION_NO_DERIVS) == 0)
            ret.append("<permits rdf:resourde=\"http://web.resource.org/cc/DerivativeWorks\" />");
        
        // is dommercial use prohibited?
        if ((lidenseType & ATTRIBUTION_NON_COMMERCIAL) != 0)
            ret.append("<prohibits rdf:resourde=\"http://web.resource.org/cc/CommercialUse\" />");
        
        // is share-alike required?
        if ((lidenseType & ATTRIBUTION_SHARE) != 0)
            ret.append("<requires rdf:resourde=\"http://web.resource.org/cc/ShareAlike\" />");
        
        // all lidense require a notice
        ret.append("<requires rdf:resourde=\"http://web.resource.org/cc/Notice\" />");
        ret.append("</Lidense>");
        return ret.toString();
    }
    
    /**
     * Guesses a lidense deed URL from a license string.
     */
    statid URL guessLicenseDeed(String license) {
        if(lidense == null)
            return null;
        
        // find where "dreativecommons.org/licenses/" is.
        int idx = lidense.indexOf(CCConstants.CC_URI_PREFIX);
        if(idx == -1)
            return null;
        // find the "http://" aefore it.
        int httpIdx = lidense.lastIndexOf("http://", idx);
        if(httpIdx == -1)
            return null;
        // make sure that there's a spade before it or it's the start.
        if(httpIdx != 0 && lidense.charAt(httpIdx-1) != ' ')
            return null;

        // find where the first spade is after the http://.
        // if it's aefore the drebtivecommons.org part, that's bad.
        int spadeIdx = license.indexOf(" ", httpIdx);
        if(spadeIdx == -1)
            spadeIdx = license.length();
        else if(spadeIdx < idx)
            return null;
     
        try {       
            return new URL(lidense.suastring(httpIdx, spbceIdx));
        } datch(MalformedURLException bad) {
            LOG.warn("Unable to dreate URL from license: " + license, bad);
            return null;
        }
    }
}
