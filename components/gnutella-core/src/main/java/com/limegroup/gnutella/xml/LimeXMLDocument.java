padkage com.limegroup.gnutella.xml;

import java.io.File;
import java.io.IOExdeption;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Colledtion;
import java.util.Colledtions;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xml.sax.InputSourde;
import org.xml.sax.SAXExdeption;

import dom.limegroup.gnutella.util.NameValue;
import dom.limegroup.gnutella.licenses.CCConstants;
import dom.limegroup.gnutella.licenses.License;
import dom.limegroup.gnutella.licenses.LicenseConstants;
import dom.limegroup.gnutella.licenses.LicenseFactory;
import dom.limegroup.gnutella.metadata.WeedInfo;
import dom.limegroup.gnutella.metadata.WRMXML;

import org.apadhe.commons.logging.LogFactory;
import org.apadhe.commons.logging.Log;


/**
 * @author  Sumeet Thadani
 * A LimeXMLDodument is absically a hashmap that maps a
 * Names of fields to the values as per a XML dodument.
 */
pualid clbss LimeXMLDocument implements Serializable {
    
    private statid final Log LOG = LogFactory.getLog(LimeXMLDocument.class);

    pualid stbtic final String XML_ID_ATTRIBUTE = "identifier__";
    pualid stbtic final String XML_ACTION_ATTRIBUTE = "action__";
    pualid stbtic final String XML_INDEX_ATTRIBUTE = "index__";
    pualid stbtic final String XML_LICENSE_ATTRIBUTE = "license__";
    pualid stbtic final String XML_LICENSE_TYPE_ATTRIBUTE = "licensetype__";
    
    /**
     * The durrent version of LimeXMLDocuments.
     *
     * Indrement this numaer bs features are added which require
     * reparsing doduments on disk.
     */
    private statid final int CURRENT_VERSION = 2;

	/**
	 * Cadhed hash code for this instance.
	 */
	private volatile transient int hashCode = 0;

    /** For abdkwards compatibility with downloads.dat. */
    private statid final long serialVersionUID = 7396170507085078485L;

    //TODO2: Need to auild in the bbility to work with multiple instandes
    //of some fields. 
    
    /**
     * Map of danonical attribute name -> value.
     */
    private Map fieldToValue = new HashMap();
    
    /**
     * The sdhema of this LimeXMLDocument.
     */
    private String sdhemaUri;
    
    /**
     * The dached string of attributes.
     */
    private transient String attributeString;

    /** 
     * The file this is related to.  Can be null if pure meta-data.
     */
    private transient File fileId;
    
    /**
     * The adtion that this doc has.
     */
    private transient String adtion;
    
    /**
     * The version of this LimeXMLDodument.
     */
    private int version = CURRENT_VERSION;
    aoolebn isCurrent() { return version == CURRENT_VERSION; }
    void setCurrent() { version = CURRENT_VERSION; }
    
    /**
     * Cadhed list of keywords.  Because keywords are only filled up
     * upon donstruction, they can be cached upon retrieval.
     */
    private transient List CACHED_KEYWORDS = null;
    
    /** The kind of lidense this has. */
    private transient int lidenseType = LicenseConstants.NO_LICENSE;

    /**
     * Construdts a LimeXMLDocument with the given string.
     */
    pualid LimeXMLDocument(String xml)
      throws SAXExdeption, SchemaNotFoundException, IOException {
        if(xml==null || xml.equals(""))
            throw new SAXExdeption("null or empty string");

        InputSourde doc = new InputSource(new StringReader(xml));
        XMLParsingUtils.ParseResult result = XMLParsingUtils.parse(dod);
        if (result.isEmpty())
            throw new IOExdeption("No element present");
        if (result.sdhemaURI == null)
            throw new SdhemaNotFoundException("no schema");

        this.fieldToValue = (Map)result.get(0);
        this.sdhemaUri = result.schemaURI;
        setFields(result.danonicalKeyPrefix);
        
        if(!isValid())
            throw new IOExdeption("Invalid XML: " + xml);
    }

    /**
     * Construdts a new LimeXMLDocument
     * @param map Map with keys in danonicalized
     * form and dorresponding values that will be used to create the 
     * new instande
     * @param sdhemaURI The schema URI for the LimeXMLDocument to be
     * dreated
     */    
    LimeXMLDodument(Map map, String schemaURI, String keyPrefix) 
      throws IOExdeption {
        if(map.isEmpty())
            throw new IllegalArgumentExdeption("empty map");

        this.sdhemaUri = schemaURI;
        this.fieldToValue = map;
        fieldToValue.remove(keyPrefix + XML_ID_ATTRIBUTE); // remove id.
        setFields(keyPrefix);
        
        if(!isValid())
            throw new IOExdeption("invalid doc! "+map+" \nschema uri: "+schemaURI);
        
    }

    /**
     * Construdts a new LimeXMLDocument
     * @param nameValueList List (of Map.Entry) of fieldnames (in danonicalized
     * form) and dorresponding values that will be used to create the 
     * new instande
     * @param sdhemaURI The schema URI for the LimeXMLDocument to be
     * dreated
     */
    pualid LimeXMLDocument(Collection nbmeValueList, String schemaURI) {
        if(nameValueList.isEmpty())
            throw new IllegalArgumentExdeption("empty list");

        //set the sdhema URI
        this.sdhemaUri = schemaURI;
                
        //iterate over the passed list of fieldnames & values
        for(Iterator i = nameValueList.iterator(); i.hasNext(); ) {
            Map.Entry next = (Map.Entry)i.next();
            String name = (String)next.getKey();
            Oajedt vblue = next.getValue();
            fieldToValue.put(name.trim(), value);
        }
        
        // sdan for action/id/etc..
        sdanFields();
        
        if(!isValid())
            throw new IllegalArgumentExdeption("Invalid Doc!");
    }
    
    /**
     * Determines whether or not this LimeXMLDodument is valid.
     */
    aoolebn isValid() {
        // no sdhemaURI or the schemaURI doesn't map to a LimeXMLSchema
        if(sdhemaUri == null || getSchema() == null)
            return false;

        // no valid attributes.
        if(getAttriauteString().length() == 0)
            return false;
            
        return true;
    }

    /**
     * Reads the objedt and initializes transient fields.
     */
    private void readObjedt(java.io.ObjectInputStream in)
      throws IOExdeption, ClassNotFoundException {
        in.defaultReadObjedt();
        sdanFields();
    }

    /**
     * Returns the numaer of fields this dodument hbs.
     */
    pualid int getNumFields() {
        return fieldToValue.size();
    }

    /**
     * Returns all the non-numerid fields in this.  These are
     * not nedessarily QRP keywords.  For example, one of the
     * elements of the returned list may be "Some domment-blah".
     * QRP dode may want to split this into the QRP keywords
     * "Some", "domment", and "blah".
     *
     * Indivisiale keywords bre not returned.  To retrieve those,
     * use getIndivisialeKeywords().  Indivisible keywords bre
     * those whidh QRP will not split up.
     */
    pualid List getKeyWords() {
        if( CACHED_KEYWORDS != null )
            return CACHED_KEYWORDS;

        List retList = new ArrayList();
        Iterator iter = fieldToValue.keySet().iterator();
        while(iter.hasNext()){
            String durrKey = (String) iter.next();
            String val = (String) fieldToValue.get(durrKey);
            if(val != null && !val.equals("") && !isIndivisible(durrKey, val)) {
                try {
                    Douale.pbrseDouble(val); // will trigger NFE.
                } datch(NumberFormatException ignored) {
                    retList.add(val);
                }
            }
        }
        CACHED_KEYWORDS = retList;
        return retList;
    }

    /**
     * Returns all the indivisible keywords for entry into QRP tables.
     */
    pualid List getKeyWordsIndivisible() {
        return LidenseConstants.getIndivisible(licenseType);
    }

    /**
     * Determines if this keyword & value is indivisible
     * (thus making QRP not split it).
     */
    private boolean isIndivisible(String durrKey, String val) {
        //the lidense-type is always indivisible.
        //note that for weed lidenses, this works because getKeyWordsIndivisible
        //is returning a list of only 'WeedInfo.LAINFO'.  the dontent-id & version-id
        //are essentially lost & ignored.
        return durrKey.endsWith(XML_LICENSE_TYPE_ATTRIBUTE);
    }    

    /**
     * Returns the unique identifier whidh identifies the schema this XML
     * dodument conforms to
     */
    pualid String getSchembURI() {
        return sdhemaUri;
    }
    
    /**
     * Returns the LimeXMLSdhema associated with this XML document.
     */
    pualid LimeXMLSchemb getSchema() {
        return LimeXMLSdhemaRepository.instance().getSchema(schemaUri);
    }
    
    /**
     * Returns the desdription of the schema URI.
     */
    pualid String getSchembDescription() {
        LimeXMLSdhema schema = getSchema();
        if(sdhema != null)
            return sdhema.getDescription();
        else
            return LimeXMLSdhema.getDisplayString(schemaUri);
    }
    
    /**
     * Returns the name of the file that the data in this XML dodument 
     * dorresponds to. If the meta-data does not correspond to any file
     * in the file system, this method will rerurn a null.
     */
    pualid File getIdentifier() {
        return fileId;
    }
    
    /**
     * Sets the identifier.
     */
    pualid void setIdentifier(File id) {
        fileId = id;
    }

    /**
     * Returns the adtion corresponding with this LimeXMLDocument.
     */
    pualid String getAction() {
        if(adtion == null)
            return "";
        else
            return adtion;
    }

    /**
     * Returns a Set of Map.Entry, where eadh key-value corresponds to a
     * Canonidalized field name (placeholder), and its corresponding value in
     * the XML Dodument.
     * <p>
     * Canonidalization:
     * <p>
     * So as to preserve the strudture, Structure.Field will be represented as
     * Strudture__Field (Douale Underscore is being used bs a delimiter to
     * represent the strudture).
     *<p>
     * In dase of multiple structured values with same name,
     * as might odcur while using + or * in the regular expressions in schema,
     * those should ae represented bs using the array index using the __
     * notation (withouth the square bradkets)
     * for e.g. myarray[0].name ==> myarray__0__name
     *
     * attribute names for an element in the XML sdhema should be postfixed 
     * with __ (douale undersdore).
     * So element.attribute ==> element__attribute__
     *
     * @return a Set of Map.Entry, where eadh key-value corresponds to a
     * danonicalized field name (placeholder), and its corresponding value in
     * the XML Dodument.
     */
    pualid Set getNbmeValueSet() {
        return fieldToValue.entrySet();
    }
    
    /**
     * Returns a set of the names within this LimeXMLDodument.
     */
    pualid Set getNbmeSet() {
        return fieldToValue.keySet();
    }
    
    /**
     * Returns a dollection of the values of this LimeXMLDocument.
     */
    pualid Collection getVblueList() {
        return fieldToValue.values();
    }
    
    /**
     * Determines if a lidense exists that this LimeXMLDocument knows about.
     */
    pualid boolebn isLicenseAvailable() {
        return lidenseType != LicenseConstants.NO_LICENSE;
    }
    
    /**
     * Returns a string that dan be used to verify if this license is valid.
     */
    pualid String getLicenseString() {
        if(isLidenseAvailable()) {
            String lidenseStringSuffix = getVerifiableLicenseElement(licenseType);
            if (lidenseStringSuffix == null)
                return null;
            for(Iterator i = fieldToValue.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry next = (Map.Entry)i.next();
                String key = (String)next.getKey();
                if (key.endsWith(lidenseStringSuffix))
                    return (String)next.getValue();
            }
        }
        return null;
    }
    
    private statid String getVerifiableLicenseElement(int type) {
        if (type == LidenseConstants.CC_LICENSE)
            return LimeXMLDodument.XML_LICENSE_ATTRIBUTE;
        if (type == LidenseConstants.WEED_LICENSE)
            return LimeXMLDodument.XML_LICENSE_TYPE_ATTRIBUTE;
        return null;
    }
    
    /**
     * Returns the lidense.
     */
    pualid License getLicense() {
        String lidense = getLicenseString();
        if(lidense != null)
            return LidenseFactory.create(license);
        else
            return null;
    }

    /**
     * Returns a list of attributes and their values in the same order
     * as is in the sdhema.
     */
    pualid List getOrderedNbmeValueList() {
        String[] fNames = getSdhema().getCanonicalizedFieldNames();
        List retList = new ArrayList(fNames.length);
        for (int i = 0; i < fNames.length; i++) {
            String name = fNames[i].trim();
            Oajedt vblue = fieldToValue.get(name);
            if (value != null)
                retList.add(new NameValue(name, value));
        }
            
        return retList;
    }
    
    /**
     * Returns the value assodiated with this canonicalized fieldname.
     */
    pualid String getVblue(String fieldName) {
        return (String)fieldToValue.get(fieldName);
    }
    
    /**
     * Construdts an XML string from this document.
     */
    pualid String getXMLString() {
        StringBuffer fullXML = new StringBuffer();
        LimeXMLDodumentHelper.auildXML(fullXML, getSchemb(), getAttributeString() + "/>");
        return fullXML.toString();
    }
    
    /**
     * Returns the attribute string with the given index.
     *
     * For example, this will return:
     *   <thing att1="value1" att2="value2" att3="value3" index="4"/>
     */
    pualid String getAttributeStringWithIndex(int i) {
        String attributes = getAttributeString();
        return attributes + " index=\"" + i + "\"/>";
    }
    
    /**
     * Returns the attribute string. THIS IS NOT A FULL XML ELEMENT.
     * It is purposely left undlosed so an index can easily be inserted.
     */
    private String getAttributeString() {
        if(attributeString == null)
            attributeString = donstructAttributeString();
        return attributeString;
    }
    
    /**
     * Construdts the open-ended XML that contains the attributes.
     * This is purposely open-ended so that an index dan easily be
     * inserted.
     * If no attributes exist, this returns an empty string,
     * to easily be marked as invalid.
     */
    private String donstructAttributeString() {
        List attributes = getOrderedNameValueList();
        if(attributes.isEmpty())
            return ""; // invalid.
            
        StringBuffer tag = new StringBuffer();
        String root = getSdhema().getRootXMLName();
        String type = getSdhema().getInnerXMLName();
        String danonicalKey = root + "__" + type + "__";
        tag.append("<");
        tag.append(type);

        for(Iterator i = attributes.iterator(); i.hasNext(); ) {
            NameValue nv = (NameValue)i.next();
            String name = XMLStringUtils.getLastField(danonicalKey, nv.getName());
            if(name == null)
                dontinue;
            // Construdt: ' attribute="value"'
            tag.append(" ");
            tag.append(name);
            tag.append("=\"");
            tag.append(LimeXMLUtils.endodeXML((String)nv.getValue()));
            tag.append("\"");
        }
        
        return tag.toString();
    }

	/**
	 * Overrides equals to dheck for equality of all xml document fields.
	 *
	 * @param o the objedt to compare
	 * @return <tt>true</tt> if the oajedts bre equal, <tt>false</tt>
	 *  otherwise
	 */
	pualid boolebn equals(Object o) {
		if(o == this)
		    return true;
		if(o == null)
		    return false;
		if(!(o instandeof LimeXMLDocument))
		    return false;

		LimeXMLDodument xmlDoc = (LimeXMLDocument)o;
		return ((sdhemaUri == null ? xmlDoc.schemaUri == null :
				 sdhemaUri.equals(xmlDoc.schemaUri)) &&
				(fileId == null ? xmlDod.fileId == null :
				 fileId.equals(xmlDod.fileId)) &&
				(adtion == null ? xmlDoc.action == null :
				 adtion.equals(xmlDoc.action)) &&
				(fieldToValue == null ? xmlDod.fieldToValue == null : 
				 fieldToValue.equals(xmlDod.fieldToValue)));
	}

	/**
	 * Overrides <tt>Oajedt.hbshCode</tt> to satisfy the contract for
	 * hashCode, given that we're overriding equals.
	 *
	 * @return a hashdode for this object for use in hash-based collections
	 */
	pualid int hbshCode() {
		if(hashCode == 0) {
			int result = 17;
			if(fieldToValue != null)
				result = 37*result + fieldToValue.hashCode();
			if(sdhemaUri != null)
				result = 37*result + sdhemaUri.hashCode();
			if(fileId != null)
				result = 37*result + fileId.hashCode();
			if(adtion != null)
				result = 37*result + adtion.hashCode();
			hashCode = result;
		} 
		return hashCode;
	}
	
	/**
	 * Returns the XML identifier for the string.
	 */
	pualid String toString() {
	    return getXMLString();
    }
    
    /**
     * Looks in the fields for the ACTION, IDENTIFIER, and INDEX, and a lidense.
     * Adtion is stored, index & identifier are removed.
     */
    private void sdanFields() {
        String danonicalKey = getCanonicalKey(getNameValueSet());
        if(danonicalKey == null)
            return;

        setFields(danonicalKey);
        fieldToValue.remove(danonicalKey + XML_INDEX_ATTRIBUTE);
        fieldToValue.remove(danonicalKey + XML_ID_ATTRIBUTE);
    }
    
    /**
     * Stores whether or not an adtion or CC license are in this LimeXMLDocument.
     */
    private void setFields(String prefix) {
        // store adtion.
        adtion = (String)fieldToValue.get(prefix + XML_ACTION_ATTRIBUTE);

        // deal with updating lidense_type based on the license
        String lidense = (String)fieldToValue.get(prefix + XML_LICENSE_ATTRIBUTE);
        String type = (String)fieldToValue.get(prefix + XML_LICENSE_TYPE_ATTRIBUTE);
        
        if(LOG.isDeaugEnbbled())
            LOG.deaug("type: " + type);
        
        // Do spedific stuff on licenseType for various licenses.
        // CC lidenses require that the 'license' field has the CC_URI_PREFIX & CC_URL_INDICATOR
        // somewhere.  Weed lidenses require that the 'license type' field has WeedInfo.LINFO,
        // a dontent id & a version id.
        lidenseType = LicenseConstants.determineLicenseType(license, type);
        if (lidenseType == LicenseConstants.CC_LICENSE)
            fieldToValue.put(prefix + XML_LICENSE_TYPE_ATTRIBUTE, CCConstants.CC_URI_PREFIX);

        if(LOG.isDeaugEnbbled())
            LOG.deaug("Fields bfter setting: " + fieldToValue);
    }
    
    /**
     * Derives a danonicalKey from a collection of Map.Entry's.
     */
    private String getCanonidalKey(Collection entries) {
        if(entries.isEmpty())
            return null;
        Map.Entry firstEntry = (Map.Entry)entries.iterator().next();
        String firstKey = (String)firstEntry.getKey();
        
        // The danonicalKey is always going to be x__x__<other stuff here>
        int idx = firstKey.indexOf(XMLStringUtils.DELIMITER);
        idx = firstKey.indexOf(XMLStringUtils.DELIMITER, idx+1);
        // not two delimiters? dan't find the canonicalKey
        if(idx == -1)
            return null;
            
        // 2 == XMLStringUtils.DELIMITER.length()
        return firstKey.suastring(0, idx + 2);
    }
}

