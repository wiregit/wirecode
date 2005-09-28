package com.limegroup.gnutella.licenses;

import com.limegroup.gnutella.Assert;

public class PublishedCCLicense implements RDFLicense, EmbeddableLicense {

    private final String holder, year, title, url, description, uri;
    private final int type;
    
    private static final String DATE_TAG = "dc:date";
    private static final String IDENTIFIER_TAG = "dc:identifier"; //magnet link
    private static final String RIGHTS_TAG = "dc:rights";
    private static final String TITLE_TAG = "dc:title";
    private static final String DESCRIPTION_TAG = "dc:description";
    
    private static final String AGENT = "Agent";
    
    private static final String LICENSE_TAG = "license rdf:resource=\"";
    
    public PublishedCCLicense(String holder, String title, 
            String year, String url, String description, String uri, int type) {
        this.holder = holder;
        this.year = year;
        this.title = title;
        this.url = url;
        this.description = description;
        this.type = type;
        this.uri = uri;
    }
    
    public String getRDFRepresentation() {
    	
        Assert.that(holder!=null && year!=null && title!=null && url!=null 
        		&& uri!=null);
    	
    	StringBuffer ret = new StringBuffer();
        ret.append(CCConstants.CC_RDF_HEADER).append("\n");
        
        // append the work opening line
        ret.append("<Work rdf:about=\"");
        if (uri != null)
            ret.append(uri);
        ret.append("\">");
        
        // year
        if (year != null)
            ret.append("<"+DATE_TAG+">"+year+"</"+DATE_TAG+">");
        // title
        if (title != null)
            ret.append("<"+TITLE_TAG+">"+title+"</"+TITLE_TAG+">");
        // description
        if (description != null)
            ret.append("<"+DESCRIPTION_TAG+">"+description+"</"+DESCRIPTION_TAG+">");
        // identifier (url)
        if (url != null)
            ret.append("<"+IDENTIFIER_TAG+">"+url+"</"+IDENTIFIER_TAG+">");
        // copyright holder is an Agent
        if (holder != null)
            ret.append("<"+RIGHTS_TAG+"><"+AGENT+"><"+TITLE_TAG+">"+
                    holder+"</"+TITLE_TAG+"></"+AGENT+"></"+RIGHTS_TAG+">");
        // hardcode sound
        ret.append("<dc:type rdf:resource=\"http://purl.org/dc/dcmitype/Sound\" />");
        // the license
        ret.append("<"+LICENSE_TAG+CCConstants.getLicenseURI(type)+"\" />");
        ret.append("</Work>").append("\n");
        
        // the license element
        ret.append(CCConstants.getLicenseElement(type)).append("\n");
        ret.append(CCConstants.CC_RDF_FOOTER);
        return ret.toString();
    }
    
    public String getEmbeddableString() {
        Assert.that(url != null);
        StringBuffer ret = new StringBuffer();
        if (holder != null)
            ret.append("Holder: "+holder+"\n");
        if (title != null)
            ret.append("Title: "+title+"\n");
        if (description != null)
            ret.append("Description: "+description+"\n");
        if (holder != null)
            ret.append("Holder: "+holder+"\n");
        	ret.append("Licensed under: "+ CCConstants.getLicenseURI(type)+" ");
        ret.append("verify at "+url);
        return ret.toString();
    }
}
