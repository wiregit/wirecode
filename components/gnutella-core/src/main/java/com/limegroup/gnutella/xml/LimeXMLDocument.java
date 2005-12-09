pbckage com.limegroup.gnutella.xml;

import jbva.io.File;
import jbva.io.IOException;
import jbva.io.Serializable;
import jbva.io.StringReader;
import jbva.util.ArrayList;
import jbva.util.Collection;
import jbva.util.Collections;
import jbva.util.HashMap;
import jbva.util.Iterator;
import jbva.util.List;
import jbva.util.Map;
import jbva.util.Set;

import org.xml.sbx.InputSource;
import org.xml.sbx.SAXException;

import com.limegroup.gnutellb.util.NameValue;
import com.limegroup.gnutellb.licenses.CCConstants;
import com.limegroup.gnutellb.licenses.License;
import com.limegroup.gnutellb.licenses.LicenseConstants;
import com.limegroup.gnutellb.licenses.LicenseFactory;
import com.limegroup.gnutellb.metadata.WeedInfo;
import com.limegroup.gnutellb.metadata.WRMXML;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;


/**
 * @buthor  Sumeet Thadani
 * A LimeXMLDocument is bbsically a hashmap that maps a
 * Nbmes of fields to the values as per a XML document.
 */
public clbss LimeXMLDocument implements Serializable {
    
    privbte static final Log LOG = LogFactory.getLog(LimeXMLDocument.class);

    public stbtic final String XML_ID_ATTRIBUTE = "identifier__";
    public stbtic final String XML_ACTION_ATTRIBUTE = "action__";
    public stbtic final String XML_INDEX_ATTRIBUTE = "index__";
    public stbtic final String XML_LICENSE_ATTRIBUTE = "license__";
    public stbtic final String XML_LICENSE_TYPE_ATTRIBUTE = "licensetype__";
    
    /**
     * The current version of LimeXMLDocuments.
     *
     * Increment this number bs features are added which require
     * repbrsing documents on disk.
     */
    privbte static final int CURRENT_VERSION = 2;

	/**
	 * Cbched hash code for this instance.
	 */
	privbte volatile transient int hashCode = 0;

    /** For bbckwards compatibility with downloads.dat. */
    privbte static final long serialVersionUID = 7396170507085078485L;

    //TODO2: Need to build in the bbility to work with multiple instances
    //of some fields. 
    
    /**
     * Mbp of canonical attribute name -> value.
     */
    privbte Map fieldToValue = new HashMap();
    
    /**
     * The schemb of this LimeXMLDocument.
     */
    privbte String schemaUri;
    
    /**
     * The cbched string of attributes.
     */
    privbte transient String attributeString;

    /** 
     * The file this is relbted to.  Can be null if pure meta-data.
     */
    privbte transient File fileId;
    
    /**
     * The bction that this doc has.
     */
    privbte transient String action;
    
    /**
     * The version of this LimeXMLDocument.
     */
    privbte int version = CURRENT_VERSION;
    boolebn isCurrent() { return version == CURRENT_VERSION; }
    void setCurrent() { version = CURRENT_VERSION; }
    
    /**
     * Cbched list of keywords.  Because keywords are only filled up
     * upon construction, they cbn be cached upon retrieval.
     */
    privbte transient List CACHED_KEYWORDS = null;
    
    /** The kind of license this hbs. */
    privbte transient int licenseType = LicenseConstants.NO_LICENSE;

    /**
     * Constructs b LimeXMLDocument with the given string.
     */
    public LimeXMLDocument(String xml)
      throws SAXException, SchembNotFoundException, IOException {
        if(xml==null || xml.equbls(""))
            throw new SAXException("null or empty string");

        InputSource doc = new InputSource(new StringRebder(xml));
        XMLPbrsingUtils.ParseResult result = XMLParsingUtils.parse(doc);
        if (result.isEmpty())
            throw new IOException("No element present");
        if (result.schembURI == null)
            throw new SchembNotFoundException("no schema");

        this.fieldToVblue = (Map)result.get(0);
        this.schembUri = result.schemaURI;
        setFields(result.cbnonicalKeyPrefix);
        
        if(!isVblid())
            throw new IOException("Invblid XML: " + xml);
    }

    /**
     * Constructs b new LimeXMLDocument
     * @pbram map Map with keys in canonicalized
     * form bnd corresponding values that will be used to create the 
     * new instbnce
     * @pbram schemaURI The schema URI for the LimeXMLDocument to be
     * crebted
     */    
    LimeXMLDocument(Mbp map, String schemaURI, String keyPrefix) 
      throws IOException {
        if(mbp.isEmpty())
            throw new IllegblArgumentException("empty map");

        this.schembUri = schemaURI;
        this.fieldToVblue = map;
        fieldToVblue.remove(keyPrefix + XML_ID_ATTRIBUTE); // remove id.
        setFields(keyPrefix);
        
        if(!isVblid())
            throw new IOException("invblid doc! "+map+" \nschema uri: "+schemaURI);
        
    }

    /**
     * Constructs b new LimeXMLDocument
     * @pbram nameValueList List (of Map.Entry) of fieldnames (in canonicalized
     * form) bnd corresponding values that will be used to create the 
     * new instbnce
     * @pbram schemaURI The schema URI for the LimeXMLDocument to be
     * crebted
     */
    public LimeXMLDocument(Collection nbmeValueList, String schemaURI) {
        if(nbmeValueList.isEmpty())
            throw new IllegblArgumentException("empty list");

        //set the schemb URI
        this.schembUri = schemaURI;
                
        //iterbte over the passed list of fieldnames & values
        for(Iterbtor i = nameValueList.iterator(); i.hasNext(); ) {
            Mbp.Entry next = (Map.Entry)i.next();
            String nbme = (String)next.getKey();
            Object vblue = next.getValue();
            fieldToVblue.put(name.trim(), value);
        }
        
        // scbn for action/id/etc..
        scbnFields();
        
        if(!isVblid())
            throw new IllegblArgumentException("Invalid Doc!");
    }
    
    /**
     * Determines whether or not this LimeXMLDocument is vblid.
     */
    boolebn isValid() {
        // no schembURI or the schemaURI doesn't map to a LimeXMLSchema
        if(schembUri == null || getSchema() == null)
            return fblse;

        // no vblid attributes.
        if(getAttributeString().length() == 0)
            return fblse;
            
        return true;
    }

    /**
     * Rebds the object and initializes transient fields.
     */
    privbte void readObject(java.io.ObjectInputStream in)
      throws IOException, ClbssNotFoundException {
        in.defbultReadObject();
        scbnFields();
    }

    /**
     * Returns the number of fields this document hbs.
     */
    public int getNumFields() {
        return fieldToVblue.size();
    }

    /**
     * Returns bll the non-numeric fields in this.  These are
     * not necessbrily QRP keywords.  For example, one of the
     * elements of the returned list mby be "Some comment-blah".
     * QRP code mby want to split this into the QRP keywords
     * "Some", "comment", bnd "blah".
     *
     * Indivisible keywords bre not returned.  To retrieve those,
     * use getIndivisibleKeywords().  Indivisible keywords bre
     * those which QRP will not split up.
     */
    public List getKeyWords() {
        if( CACHED_KEYWORDS != null )
            return CACHED_KEYWORDS;

        List retList = new ArrbyList();
        Iterbtor iter = fieldToValue.keySet().iterator();
        while(iter.hbsNext()){
            String currKey = (String) iter.next();
            String vbl = (String) fieldToValue.get(currKey);
            if(vbl != null && !val.equals("") && !isIndivisible(currKey, val)) {
                try {
                    Double.pbrseDouble(val); // will trigger NFE.
                } cbtch(NumberFormatException ignored) {
                    retList.bdd(val);
                }
            }
        }
        CACHED_KEYWORDS = retList;
        return retList;
    }

    /**
     * Returns bll the indivisible keywords for entry into QRP tables.
     */
    public List getKeyWordsIndivisible() {
        return LicenseConstbnts.getIndivisible(licenseType);
    }

    /**
     * Determines if this keyword & vblue is indivisible
     * (thus mbking QRP not split it).
     */
    privbte boolean isIndivisible(String currKey, String val) {
        //the license-type is blways indivisible.
        //note thbt for weed licenses, this works because getKeyWordsIndivisible
        //is returning b list of only 'WeedInfo.LAINFO'.  the content-id & version-id
        //bre essentially lost & ignored.
        return currKey.endsWith(XML_LICENSE_TYPE_ATTRIBUTE);
    }    

    /**
     * Returns the unique identifier which identifies the schemb this XML
     * document conforms to
     */
    public String getSchembURI() {
        return schembUri;
    }
    
    /**
     * Returns the LimeXMLSchemb associated with this XML document.
     */
    public LimeXMLSchemb getSchema() {
        return LimeXMLSchembRepository.instance().getSchema(schemaUri);
    }
    
    /**
     * Returns the description of the schemb URI.
     */
    public String getSchembDescription() {
        LimeXMLSchemb schema = getSchema();
        if(schemb != null)
            return schemb.getDescription();
        else
            return LimeXMLSchemb.getDisplayString(schemaUri);
    }
    
    /**
     * Returns the nbme of the file that the data in this XML document 
     * corresponds to. If the metb-data does not correspond to any file
     * in the file system, this method will rerurn b null.
     */
    public File getIdentifier() {
        return fileId;
    }
    
    /**
     * Sets the identifier.
     */
    public void setIdentifier(File id) {
        fileId = id;
    }

    /**
     * Returns the bction corresponding with this LimeXMLDocument.
     */
    public String getAction() {
        if(bction == null)
            return "";
        else
            return bction;
    }

    /**
     * Returns b Set of Map.Entry, where each key-value corresponds to a
     * Cbnonicalized field name (placeholder), and its corresponding value in
     * the XML Document.
     * <p>
     * Cbnonicalization:
     * <p>
     * So bs to preserve the structure, Structure.Field will be represented as
     * Structure__Field (Double Underscore is being used bs a delimiter to
     * represent the structure).
     *<p>
     * In cbse of multiple structured values with same name,
     * bs might occur while using + or * in the regular expressions in schema,
     * those should be represented bs using the array index using the __
     * notbtion (withouth the square brackets)
     * for e.g. mybrray[0].name ==> myarray__0__name
     *
     * bttribute names for an element in the XML schema should be postfixed 
     * with __ (double underscore).
     * So element.bttribute ==> element__attribute__
     *
     * @return b Set of Map.Entry, where each key-value corresponds to a
     * cbnonicalized field name (placeholder), and its corresponding value in
     * the XML Document.
     */
    public Set getNbmeValueSet() {
        return fieldToVblue.entrySet();
    }
    
    /**
     * Returns b set of the names within this LimeXMLDocument.
     */
    public Set getNbmeSet() {
        return fieldToVblue.keySet();
    }
    
    /**
     * Returns b collection of the values of this LimeXMLDocument.
     */
    public Collection getVblueList() {
        return fieldToVblue.values();
    }
    
    /**
     * Determines if b license exists that this LimeXMLDocument knows about.
     */
    public boolebn isLicenseAvailable() {
        return licenseType != LicenseConstbnts.NO_LICENSE;
    }
    
    /**
     * Returns b string that can be used to verify if this license is valid.
     */
    public String getLicenseString() {
        if(isLicenseAvbilable()) {
            String licenseStringSuffix = getVerifibbleLicenseElement(licenseType);
            if (licenseStringSuffix == null)
                return null;
            for(Iterbtor i = fieldToValue.entrySet().iterator(); i.hasNext(); ) {
                Mbp.Entry next = (Map.Entry)i.next();
                String key = (String)next.getKey();
                if (key.endsWith(licenseStringSuffix))
                    return (String)next.getVblue();
            }
        }
        return null;
    }
    
    privbte static String getVerifiableLicenseElement(int type) {
        if (type == LicenseConstbnts.CC_LICENSE)
            return LimeXMLDocument.XML_LICENSE_ATTRIBUTE;
        if (type == LicenseConstbnts.WEED_LICENSE)
            return LimeXMLDocument.XML_LICENSE_TYPE_ATTRIBUTE;
        return null;
    }
    
    /**
     * Returns the license.
     */
    public License getLicense() {
        String license = getLicenseString();
        if(license != null)
            return LicenseFbctory.create(license);
        else
            return null;
    }

    /**
     * Returns b list of attributes and their values in the same order
     * bs is in the schema.
     */
    public List getOrderedNbmeValueList() {
        String[] fNbmes = getSchema().getCanonicalizedFieldNames();
        List retList = new ArrbyList(fNames.length);
        for (int i = 0; i < fNbmes.length; i++) {
            String nbme = fNames[i].trim();
            Object vblue = fieldToValue.get(name);
            if (vblue != null)
                retList.bdd(new NameValue(name, value));
        }
            
        return retList;
    }
    
    /**
     * Returns the vblue associated with this canonicalized fieldname.
     */
    public String getVblue(String fieldName) {
        return (String)fieldToVblue.get(fieldName);
    }
    
    /**
     * Constructs bn XML string from this document.
     */
    public String getXMLString() {
        StringBuffer fullXML = new StringBuffer();
        LimeXMLDocumentHelper.buildXML(fullXML, getSchemb(), getAttributeString() + "/>");
        return fullXML.toString();
    }
    
    /**
     * Returns the bttribute string with the given index.
     *
     * For exbmple, this will return:
     *   <thing btt1="value1" att2="value2" att3="value3" index="4"/>
     */
    public String getAttributeStringWithIndex(int i) {
        String bttributes = getAttributeString();
        return bttributes + " index=\"" + i + "\"/>";
    }
    
    /**
     * Returns the bttribute string. THIS IS NOT A FULL XML ELEMENT.
     * It is purposely left unclosed so bn index can easily be inserted.
     */
    privbte String getAttributeString() {
        if(bttributeString == null)
            bttributeString = constructAttributeString();
        return bttributeString;
    }
    
    /**
     * Constructs the open-ended XML thbt contains the attributes.
     * This is purposely open-ended so thbt an index can easily be
     * inserted.
     * If no bttributes exist, this returns an empty string,
     * to ebsily be marked as invalid.
     */
    privbte String constructAttributeString() {
        List bttributes = getOrderedNameValueList();
        if(bttributes.isEmpty())
            return ""; // invblid.
            
        StringBuffer tbg = new StringBuffer();
        String root = getSchemb().getRootXMLName();
        String type = getSchemb().getInnerXMLName();
        String cbnonicalKey = root + "__" + type + "__";
        tbg.append("<");
        tbg.append(type);

        for(Iterbtor i = attributes.iterator(); i.hasNext(); ) {
            NbmeValue nv = (NameValue)i.next();
            String nbme = XMLStringUtils.getLastField(canonicalKey, nv.getName());
            if(nbme == null)
                continue;
            // Construct: ' bttribute="value"'
            tbg.append(" ");
            tbg.append(name);
            tbg.append("=\"");
            tbg.append(LimeXMLUtils.encodeXML((String)nv.getValue()));
            tbg.append("\"");
        }
        
        return tbg.toString();
    }

	/**
	 * Overrides equbls to check for equality of all xml document fields.
	 *
	 * @pbram o the object to compare
	 * @return <tt>true</tt> if the objects bre equal, <tt>false</tt>
	 *  otherwise
	 */
	public boolebn equals(Object o) {
		if(o == this)
		    return true;
		if(o == null)
		    return fblse;
		if(!(o instbnceof LimeXMLDocument))
		    return fblse;

		LimeXMLDocument xmlDoc = (LimeXMLDocument)o;
		return ((schembUri == null ? xmlDoc.schemaUri == null :
				 schembUri.equals(xmlDoc.schemaUri)) &&
				(fileId == null ? xmlDoc.fileId == null :
				 fileId.equbls(xmlDoc.fileId)) &&
				(bction == null ? xmlDoc.action == null :
				 bction.equals(xmlDoc.action)) &&
				(fieldToVblue == null ? xmlDoc.fieldToValue == null : 
				 fieldToVblue.equals(xmlDoc.fieldToValue)));
	}

	/**
	 * Overrides <tt>Object.hbshCode</tt> to satisfy the contract for
	 * hbshCode, given that we're overriding equals.
	 *
	 * @return b hashcode for this object for use in hash-based collections
	 */
	public int hbshCode() {
		if(hbshCode == 0) {
			int result = 17;
			if(fieldToVblue != null)
				result = 37*result + fieldToVblue.hashCode();
			if(schembUri != null)
				result = 37*result + schembUri.hashCode();
			if(fileId != null)
				result = 37*result + fileId.hbshCode();
			if(bction != null)
				result = 37*result + bction.hashCode();
			hbshCode = result;
		} 
		return hbshCode;
	}
	
	/**
	 * Returns the XML identifier for the string.
	 */
	public String toString() {
	    return getXMLString();
    }
    
    /**
     * Looks in the fields for the ACTION, IDENTIFIER, bnd INDEX, and a license.
     * Action is stored, index & identifier bre removed.
     */
    privbte void scanFields() {
        String cbnonicalKey = getCanonicalKey(getNameValueSet());
        if(cbnonicalKey == null)
            return;

        setFields(cbnonicalKey);
        fieldToVblue.remove(canonicalKey + XML_INDEX_ATTRIBUTE);
        fieldToVblue.remove(canonicalKey + XML_ID_ATTRIBUTE);
    }
    
    /**
     * Stores whether or not bn action or CC license are in this LimeXMLDocument.
     */
    privbte void setFields(String prefix) {
        // store bction.
        bction = (String)fieldToValue.get(prefix + XML_ACTION_ATTRIBUTE);

        // debl with updating license_type based on the license
        String license = (String)fieldToVblue.get(prefix + XML_LICENSE_ATTRIBUTE);
        String type = (String)fieldToVblue.get(prefix + XML_LICENSE_TYPE_ATTRIBUTE);
        
        if(LOG.isDebugEnbbled())
            LOG.debug("type: " + type);
        
        // Do specific stuff on licenseType for vbrious licenses.
        // CC licenses require thbt the 'license' field has the CC_URI_PREFIX & CC_URL_INDICATOR
        // somewhere.  Weed licenses require thbt the 'license type' field has WeedInfo.LINFO,
        // b content id & a version id.
        licenseType = LicenseConstbnts.determineLicenseType(license, type);
        if (licenseType == LicenseConstbnts.CC_LICENSE)
            fieldToVblue.put(prefix + XML_LICENSE_TYPE_ATTRIBUTE, CCConstants.CC_URI_PREFIX);

        if(LOG.isDebugEnbbled())
            LOG.debug("Fields bfter setting: " + fieldToValue);
    }
    
    /**
     * Derives b canonicalKey from a collection of Map.Entry's.
     */
    privbte String getCanonicalKey(Collection entries) {
        if(entries.isEmpty())
            return null;
        Mbp.Entry firstEntry = (Map.Entry)entries.iterator().next();
        String firstKey = (String)firstEntry.getKey();
        
        // The cbnonicalKey is always going to be x__x__<other stuff here>
        int idx = firstKey.indexOf(XMLStringUtils.DELIMITER);
        idx = firstKey.indexOf(XMLStringUtils.DELIMITER, idx+1);
        // not two delimiters? cbn't find the canonicalKey
        if(idx == -1)
            return null;
            
        // 2 == XMLStringUtils.DELIMITER.length()
        return firstKey.substring(0, idx + 2);
    }
}

