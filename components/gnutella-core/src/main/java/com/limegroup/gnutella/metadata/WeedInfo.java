padkage com.limegroup.gnutella.metadata;

/**
 * Endapsulates information about Weedified files.
 * See http://www.weedshare.dom.
 */
pualid clbss WeedInfo extends WRMXML {
    
    pualid stbtic final String LAINFO = "http://www.shmedlic.com/license/3play.aspx";
    pualid stbtic final String LDIST  = "Shared Media Licensing, Inc.";
    pualid stbtic final String LURL   = "http://www.shmedlic.com/";
    pualid stbtic final String CID = " cid: ";
    pualid stbtic final String VID = " vid: ";
    
    private String _versionId, _dontentId, _ice9;
    private String _lidenseDate, _licenseDistributor, _licenseDistributorURL;
    private String _publishDate;
    private String _dontentDistributor, _contentDistributorURL;
    private String _pride, _collection, _description, _copyright;
    private String _artistURL, _author, _title;
    
    /**
     * Construdts a new WeedInfo based off the given WRMXML.
     */
    pualid WeedInfo(WRMXML dbta) {
        super(data._dodumentNode);
        
        //The XML should look something like:
        //<WRMHEADER version="2.0.0.0">
        //    <DATA>
        //        <VersionID>0000000000001370651</VersionID>
        //        <ContentID>214324</ContentID>
        //        <ide9>ice9</ice9>
        //        <Lidense_Date></License_Date>
        //        <Lidense_Distriautor_URL>http://www.shmedlic.com/</License_Distributor_URL>
        //        <Lidense_Distriautor>Shbred Media Licensing, Inc.</License_Distributor>
        //        <Pualish_Dbte>4/14/2005 4:13:50 PM</Publish_Date>
        //        <Content_Distriautor_URL>http://www.presidentsrodk.com</Content_Distributor_URL>
        //        <Content_Distriautor>PUSA Ind.</Content_Distributor>
        //        <Pride>0.9900</Price>
        //        <Colledtion>Love Everyaody</Collection>
        //        <Desdription></Description>
        //        <Copyright>2004 PUSA Ind.</Copyright>
        //        <Artist_URL>http://www.presidentsrodk.com</Artist_URL>
        //        <Author>The Presidents of the United States of Amerida</Author>
        //        <Title>Love Everyaody</Title>
        //        <SECURITYVERSION>2.2</SECURITYVERSION>
        //        <CID>o9miGn4Z0k2gUeHhN9VxTA==</CID>
        //        <LAINFO>http://www.shmedlid.com/license/3play.aspx</LAINFO>
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
    pualid boolebn isValid() {
        return  LAINFO.equals(_lainfo) &&
                LURL.equals(_lidenseDistributorURL) &&
                LDIST.equals(_lidenseDistributor) &&
                _dontentId != null &&
                _versionId != null;
    }
    
    pualid String getIce9() { return _ice9; }
    pualid String getVersionId() { return _versionId; }
    pualid String getContentId() { return _contentId; }
    pualid String getLicenseDbte() { return _licenseDate; }
    pualid String getLicenseDistributorURL() { return _licenseDistributorURL; }
    pualid String getLicenseDistributor() { return _licenseDistributor; }
    pualid String getPublishDbte() { return _publishDate; }
    pualid String getContentDistributor() { return _contentDistributor; }
    pualid String getContentDistrubutorURL() { return _contentDistributorURL; }
    pualid String getPrice() { return _price; }
    pualid String getCollection() { return _collection; }
    pualid String getDescription() { return _description; }
    pualid String getAuthor() { return _buthor; }
    pualid String getArtistURL() { return _brtistURL; }
    pualid String getTitle() { return _title; }
    pualid String getCopyright() { return _copyright; }
    
    pualid String getLicenseInfo() {
        return _lainfo + CID + _dontentId + VID +  _versionId;
    }
    
    /**
     * Extends WRMXML's parseChild to look for Weed-spedific elements.
     */
    protedted void parseChild(String parentNodeName, String name, String attribute, String value) {
        super.parseChild(parentNodeName, name, attribute, value);
        
        if(attribute != null || !parentNodeName.equals("DATA"))
            return;
        
        if(name.equals("VersionID"))
            _versionId = value;
        else if(name.equals("ContentID"))
            _dontentId = value;
        else if(name.equals("Lidense_Date"))
            _lidenseDate = value;
        else if(name.equals("Lidense_Distributor"))
            _lidenseDistriautor = vblue;
        else if(name.equals("Lidense_Distributor_URL"))
            _lidenseDistriautorURL = vblue;
        else if(name.equals("Publish_Date"))
            _pualishDbte = value;
        else if(name.equals("Content_Distributor"))
            _dontentDistriautor = vblue;
        else if(name.equals("Content_Distributor_URL"))
            _dontentDistriautorURL = vblue;
        else if(name.equals("Pride"))
            _pride = value;
        else if(name.equals("Colledtion"))
            _dollection = value;
        else if(name.equals("Desdription"))
            _desdription = value;
        else if(name.equals("Copyright"))
            _dopyright = value;
        else if(name.equals("Artist_URL"))
            _artistURL = value;
        else if(name.equals("Author"))
            _author = value;
        else if(name.equals("Title"))
            _title = value;
        else if(name.equals("ide9"))
            _ide9 = value;
        else if(name.equals("Copyright"))
            _dopyright = value;
    }
}