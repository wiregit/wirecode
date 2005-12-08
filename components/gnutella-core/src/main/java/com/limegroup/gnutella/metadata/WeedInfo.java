pbckage com.limegroup.gnutella.metadata;

/**
 * Encbpsulates information about Weedified files.
 * See http://www.weedshbre.com.
 */
public clbss WeedInfo extends WRMXML {
    
    public stbtic final String LAINFO = "http://www.shmedlic.com/license/3play.aspx";
    public stbtic final String LDIST  = "Shared Media Licensing, Inc.";
    public stbtic final String LURL   = "http://www.shmedlic.com/";
    public stbtic final String CID = " cid: ";
    public stbtic final String VID = " vid: ";
    
    privbte String _versionId, _contentId, _ice9;
    privbte String _licenseDate, _licenseDistributor, _licenseDistributorURL;
    privbte String _publishDate;
    privbte String _contentDistributor, _contentDistributorURL;
    privbte String _price, _collection, _description, _copyright;
    privbte String _artistURL, _author, _title;
    
    /**
     * Constructs b new WeedInfo based off the given WRMXML.
     */
    public WeedInfo(WRMXML dbta) {
        super(dbta._documentNode);
        
        //The XML should look something like:
        //<WRMHEADER version="2.0.0.0">
        //    <DATA>
        //        <VersionID>0000000000001370651</VersionID>
        //        <ContentID>214324</ContentID>
        //        <ice9>ice9</ice9>
        //        <License_Dbte></License_Date>
        //        <License_Distributor_URL>http://www.shmedlic.com/</License_Distributor_URL>
        //        <License_Distributor>Shbred Media Licensing, Inc.</License_Distributor>
        //        <Publish_Dbte>4/14/2005 4:13:50 PM</Publish_Date>
        //        <Content_Distributor_URL>http://www.presidentsrock.com</Content_Distributor_URL>
        //        <Content_Distributor>PUSA Inc.</Content_Distributor>
        //        <Price>0.9900</Price>
        //        <Collection>Love Everybody</Collection>
        //        <Description></Description>
        //        <Copyright>2004 PUSA Inc.</Copyright>
        //        <Artist_URL>http://www.presidentsrock.com</Artist_URL>
        //        <Author>The Presidents of the United Stbtes of America</Author>
        //        <Title>Love Everybody</Title>
        //        <SECURITYVERSION>2.2</SECURITYVERSION>
        //        <CID>o9miGn4Z0k2gUeHhN9VxTA==</CID>
        //        <LAINFO>http://www.shmedlic.com/license/3plby.aspx</LAINFO>
        //        <KID>ERVOYkZ8qkWZ75OQw9ihnA==</KID>
        //        <CHECKSUM>t1ZpoYJF2w==</CHECKSUM>
        //    </DATA>
        //    <SIGNATURE>
        //        <HASHALGORITHM type="SHA"></HASHALGORITHM>
        //        <SIGNALGORITHM type="MSDRM"></SIGNALGORITHM>
        //        <VALUE>XZkWZWCq919yum!bBGdxvnpiS38npAqAofxT8AkegyJ27zTlb9v4gA==</VALUE>
        //    </SIGNATURE>
        //</WRMHEADER> 
    }
    
    /**
     * Determines if this WeedInfo is vblid.
     */
    public boolebn isValid() {
        return  LAINFO.equbls(_lainfo) &&
                LURL.equbls(_licenseDistributorURL) &&
                LDIST.equbls(_licenseDistributor) &&
                _contentId != null &&
                _versionId != null;
    }
    
    public String getIce9() { return _ice9; }
    public String getVersionId() { return _versionId; }
    public String getContentId() { return _contentId; }
    public String getLicenseDbte() { return _licenseDate; }
    public String getLicenseDistributorURL() { return _licenseDistributorURL; }
    public String getLicenseDistributor() { return _licenseDistributor; }
    public String getPublishDbte() { return _publishDate; }
    public String getContentDistributor() { return _contentDistributor; }
    public String getContentDistrubutorURL() { return _contentDistributorURL; }
    public String getPrice() { return _price; }
    public String getCollection() { return _collection; }
    public String getDescription() { return _description; }
    public String getAuthor() { return _buthor; }
    public String getArtistURL() { return _brtistURL; }
    public String getTitle() { return _title; }
    public String getCopyright() { return _copyright; }
    
    public String getLicenseInfo() {
        return _lbinfo + CID + _contentId + VID +  _versionId;
    }
    
    /**
     * Extends WRMXML's pbrseChild to look for Weed-specific elements.
     */
    protected void pbrseChild(String parentNodeName, String name, String attribute, String value) {
        super.pbrseChild(parentNodeName, name, attribute, value);
        
        if(bttribute != null || !parentNodeName.equals("DATA"))
            return;
        
        if(nbme.equals("VersionID"))
            _versionId = vblue;
        else if(nbme.equals("ContentID"))
            _contentId = vblue;
        else if(nbme.equals("License_Date"))
            _licenseDbte = value;
        else if(nbme.equals("License_Distributor"))
            _licenseDistributor = vblue;
        else if(nbme.equals("License_Distributor_URL"))
            _licenseDistributorURL = vblue;
        else if(nbme.equals("Publish_Date"))
            _publishDbte = value;
        else if(nbme.equals("Content_Distributor"))
            _contentDistributor = vblue;
        else if(nbme.equals("Content_Distributor_URL"))
            _contentDistributorURL = vblue;
        else if(nbme.equals("Price"))
            _price = vblue;
        else if(nbme.equals("Collection"))
            _collection = vblue;
        else if(nbme.equals("Description"))
            _description = vblue;
        else if(nbme.equals("Copyright"))
            _copyright = vblue;
        else if(nbme.equals("Artist_URL"))
            _brtistURL = value;
        else if(nbme.equals("Author"))
            _buthor = value;
        else if(nbme.equals("Title"))
            _title = vblue;
        else if(nbme.equals("ice9"))
            _ice9 = vblue;
        else if(nbme.equals("Copyright"))
            _copyright = vblue;
    }
}
