package com.limegroup.gnutella.version;

import org.apache.commons.httpclient.URI;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

class UpdateData implements Cloneable, UpdateInformation {
    
    private static final Log LOG = LogFactory.getLog(UpdateData.class);
        
    /**
     * The oldest message this message applies to.
     */
    private Version version;
    
    /**
     * The OSes this message applies to.
     */
    private OS[] os;
    
    /**
     * The URI to send update requests to from this message.
     */
    private URI updateURI;
    
    /**
     * The language this message is intended for.
     */
    private String language;
    
    /**
     * The version this message is updating to.
     */
    private Version updateVersion;
    
    /**
     * The text of the message.
     */
    private String updateText;
    
    /**
     * Constructs a new UpdateData object.
     */
    UpdateData() {}
    
    /**
     * The data.
     */
    public String toString() {
        return "\n{ov: " + version + ", oses: " + OS.toString(os) + ", uri: " + updateURI + ", l: " + language +
                ", uv: " + updateVersion + ", t: " + updateText + "}";
    }
    
    /**
     * Sets the oldest version this applies to.
     */
    void setOldestVersion(Version v) {
        version = v;
    }
    
    /**
     * Gets the oldest version this applies to.
     */
    Version getOldestVersion() {
        return version;
    }
    
    /**
     * Sets the OSes this applies to.
     */
    void setOS(OS[] os) {
        this.os = os;
    }
    
    /**
     * Gets the OS this applies to.
     */
    OS[] getOS() {
        return os;
    }
    
    /**
     * Determines if any of the OSes here are acceptable.
     */
    boolean isOSAcceptable() {
        for(int i = 0; i < os.length; i++)
            if(os[i].isAcceptable())
                return true;
        return false;
    }
    
    /**
     * Sets the update URL.
     */
    void setUpdateURI(URI url) {
        updateURI = url;
    }
    
    /**
     * Gets the update URL.
     */
    public URI getUpdateURI() {
        return updateURI;
    }
    
    /**
     * Sets the language.
     */
    void setLanguage(String lang) {
        language = lang;
    }
    
    /**
     * Gets the language.
     */
    String getLanguage() {
        return language;
    }
    
    /**
     * Sets the update version.
     */
    void setUpdateVersion(Version v) {
        updateVersion = v;
    }
    
    /**
     * Gets the update version as a string.
     */
    public String getVersion() {
        return updateVersion.toString();
    }
    
    /**
     * Gets the update version.
     */
    Version getUpdateVersion() {
        return updateVersion;
    }
    
    /**
     * Sets the update text.
     */
    void setUpdateText(String text) {
        updateText = text;
    }
    
    /**
     * Gets the update text.
     */
    public String getUpdateText() {
        return updateText;
    }
    
    /**
     * Clones a new update data that is exactly like this one.
     */
    public Object clone() {
        Object clone = null;
        try {
            clone = super.clone();
        } catch(CloneNotSupportedException cnse) {
            LOG.error("shoulda cloned", cnse);
        }
        return clone;
    }
}