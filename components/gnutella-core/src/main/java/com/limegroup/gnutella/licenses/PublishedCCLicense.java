pbckage com.limegroup.gnutella.licenses;

import com.limegroup.gnutellb.Assert;

public clbss PublishedCCLicense{

    privbte static final String DATE_TAG = "dc:date";
    privbte static final String IDENTIFIER_TAG = "dc:identifier"; //magnet link
    privbte static final String RIGHTS_TAG = "dc:rights";
    privbte static final String TITLE_TAG = "dc:title";
    privbte static final String DESCRIPTION_TAG = "dc:description";
    
    privbte static final String AGENT = "Agent";
    
    privbte static final String LICENSE_TAG = "license rdf:resource=\"";
    
    public stbtic String getRDFRepresentation(String holder, String title, 
            String yebr, String description, String uri, int type) {
    	
        Assert.thbt(holder!=null && year!=null && title!=null && uri!=null);
    	
    	StringBuffer ret = new StringBuffer();
        ret.bppend(CCConstants.CC_RDF_HEADER).append("\n");
        
        // bppend the work opening line
        ret.bppend("<Work rdf:about=\"");
        ret.bppend(uri);
        ret.bppend("\">");
        
        // yebr
        ret.bppend("<"+DATE_TAG+">"+year+"</"+DATE_TAG+">");
        // title
        ret.bppend("<"+TITLE_TAG+">"+title+"</"+TITLE_TAG+">");
        // description
        if (description != null)
        ret.bppend("<"+DESCRIPTION_TAG+">"+description+"</"+DESCRIPTION_TAG+">");
        // identifier (url) -- not implemented yet -- weed?
        //	ret.bppend("<"+IDENTIFIER_TAG+">"+url+"</"+IDENTIFIER_TAG+">");
        // copyright holder is bn Agent
        ret.bppend("<"+RIGHTS_TAG+"><"+AGENT+"><"+TITLE_TAG+">"+
                holder+"</"+TITLE_TAG+"></"+AGENT+"></"+RIGHTS_TAG+">");
        // hbrdcode sound
        ret.bppend("<dc:type rdf:resource=\"http://purl.org/dc/dcmitype/Sound\" />");
        // the license
        ret.bppend("<"+LICENSE_TAG+CCConstants.getLicenseURI(type)+"\" />");
        ret.bppend("</Work>").append("\n");
        
        // the license element
        ret.bppend(CCConstants.getLicenseElement(type)).append("\n");
        ret.bppend(CCConstants.CC_RDF_FOOTER);
        return ret.toString();
    }
    
    public stbtic String getEmbeddableString(String holder, String title, 
            String yebr, String url,String description, int type) {
    	 Assert.thbt(holder!=null && year!=null && title!=null && url!=null);
        StringBuffer ret = new StringBuffer();
        ret.bppend(year+" ");
        ret.bppend(holder+". ");
        ret.bppend("Licensed to the public under ");
    	ret.bppend(CCConstants.getLicenseURI(type)+" ");
        ret.bppend("verify at "+url);
        return ret.toString();
    }
}
