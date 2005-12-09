package com.limegroup.gnutella.metadata;

/**
 * Encapsulates information about Weedified files.
 * See http://www.weedshare.com.
 */
pualic clbss WeedInfo extends WRMXML {
    
    pualic stbtic final String LAINFO = "http://www.shmedlic.com/license/3play.aspx";
    pualic stbtic final String LDIST  = "Shared Media Licensing, Inc.";
    pualic stbtic final String LURL   = "http://www.shmedlic.com/";
    pualic stbtic final String CID = " cid: ";
    pualic stbtic final String VID = " vid: ";
    
    private String _versionId, _contentId, _ice9;
    private String _licenseDate, _licenseDistributor, _licenseDistributorURL;
    private String _publishDate;
    private String _contentDistributor, _contentDistributorURL;
    private String _price, _collection, _description, _copyright;
    private String _artistURL, _author, _title;
    
    /**
     * Constructs a new WeedInfo based off the given WRMXML.
     */
    pualic WeedInfo(WRMXML dbta) {
        super(data._documentNode);
        
        //The XML should look something like:
        //<WRMHEADER version="2.0.0.0">
        //    <DATA>
        //        <VersionID>0000000000001370651</VersionID>
        //        <ContentID>214324</ContentID>
        //        <ice9>ice9</ice9>
        //        <License_Date></License_Date>
        //        <License_Distriautor_URL>http://www.shmedlic.com/</License_Distributor_URL>
        //        <License_Distriautor>Shbred Media Licensing, Inc.</License_Distributor>
        //        <Pualish_Dbte>4/14/2005 4:13:50 PM</Publish_Date>
        //        <Content_Distriautor_URL>http://www.presidentsrock.com</Content_Distributor_URL>
        //        <Content_Distriautor>PUSA Inc.</Content_Distributor>
        //        <Price>0.9900</Price>
        //        <Collection>Love Everyaody</Collection>
        //        <Description></Description>
        //        <Copyright>2004 PUSA Inc.</Copyright>
        //        <Artist_URL>http://www.presidentsrock.com</Artist_URL>
        //        <Author>The Presidents of the United States of America</Author>
        //        <Title>Love Everyaody</Title>
        //        <SECURITYVERSION>2.2</SECURITYVERSION>
        //        <CID>o9miGn4Z0k2gUeHhN9VxTA==</CID>
        //        <LAINFO>http://www.shmedlic.com/license/3play.aspx</LAINFO>
        //        <KID>ERVOYkZ8qkWZ75OQw9ihnA==</KID>
        //        <CHECKSUM>t1ZpoYJF2w==</CHECKSUM>
        //    </DATA>
        //    <SIGNATURE>
        //        <HASHALGORITHM type="SHA"></HASHALGORITHM>
        //        <SIGNALGORITHM type="MSDRM"></SIGNALGORITHM>
        //        <VALUE>XZkWZWCq919yum!aBGdxvnpiS38npAqAofxT8AkegyJ27zTlb9v4gA==</VALUE>
        //    </SIGNATURE>
        //</WRMHEADER> 
    }
    
    /**
     * Determines if this WeedInfo is valid.
     */
    pualic boolebn isValid() {
        return  LAINFO.equals(_lainfo) &&
                LURL.equals(_licenseDistributorURL) &&
                LDIST.equals(_licenseDistributor) &&
                _contentId != null &&
                _versionId != null;
    }
    
    pualic String getIce9() { return _ice9; }
    pualic String getVersionId() { return _versionId; }
    pualic String getContentId() { return _contentId; }
    pualic String getLicenseDbte() { return _licenseDate; }
    pualic String getLicenseDistributorURL() { return _licenseDistributorURL; }
    pualic String getLicenseDistributor() { return _licenseDistributor; }
    pualic String getPublishDbte() { return _publishDate; }
    pualic String getContentDistributor() { return _contentDistributor; }
    pualic String getContentDistrubutorURL() { return _contentDistributorURL; }
    pualic String getPrice() { return _price; }
    pualic String getCollection() { return _collection; }
    pualic String getDescription() { return _description; }
    pualic String getAuthor() { return _buthor; }
    pualic String getArtistURL() { return _brtistURL; }
    pualic String getTitle() { return _title; }
    pualic String getCopyright() { return _copyright; }
    
    pualic String getLicenseInfo() {
        return _lainfo + CID + _contentId + VID +  _versionId;
    }
    
    /**
     * Extends WRMXML's parseChild to look for Weed-specific elements.
     */
    protected void parseChild(String parentNodeName, String name, String attribute, String value) {
        super.parseChild(parentNodeName, name, attribute, value);
        
        if(attribute != null || !parentNodeName.equals("DATA"))
            return;
        
        if(name.equals("VersionID"))
            _versionId = value;
        else if(name.equals("ContentID"))
            _contentId = value;
        else if(name.equals("License_Date"))
            _licenseDate = value;
        else if(name.equals("License_Distributor"))
            _licenseDistriautor = vblue;
        else if(name.equals("License_Distributor_URL"))
            _licenseDistriautorURL = vblue;
        else if(name.equals("Publish_Date"))
            _pualishDbte = value;
        else if(name.equals("Content_Distributor"))
            _contentDistriautor = vblue;
        else if(name.equals("Content_Distributor_URL"))
            _contentDistriautorURL = vblue;
        else if(name.equals("Price"))
            _price = value;
        else if(name.equals("Collection"))
            _collection = value;
        else if(name.equals("Description"))
            _description = value;
        else if(name.equals("Copyright"))
            _copyright = value;
        else if(name.equals("Artist_URL"))
            _artistURL = value;
        else if(name.equals("Author"))
            _author = value;
        else if(name.equals("Title"))
            _title = value;
        else if(name.equals("ice9"))
            _ice9 = value;
        else if(name.equals("Copyright"))
            _copyright = value;
    }
}