package com.limegroup.gnutella.version;


import java.io.File;
import java.io.UnsupportedEncodingException;

import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.ProcessingQueue;
import com.limegroup.gnutella.security.SignatureVerifier;
import com.limegroup.gnutella.settings.ApplicationSettings;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public class UpdateHandler {
    
    private static final Log LOG = LogFactory.getLog(UpdateHandler.class);

    /**
     * The filename on disk where data is stored.
     */
    private static final String FILENAME = "version.xml";
    
    /**
     * The filename on disk where the public key is stored.
     */
    private static final String KEY = "version.key";
    
    private static final UpdateHandler INSTANCE = new UpdateHandler();
    private UpdateHandler() { initialize(); }
    public static UpdateHandler instance() { return INSTANCE; }
    
    /**
     * The queue that handles all incoming data.
     */
    private final ProcessingQueue QUEUE = new ProcessingQueue("UpdateHandler");
    
    /**
     * The most recent update info for this machine.
     */
    private volatile UpdateInformation _updateInfo;
    
    /**
     * The most recent id of the update info.
     */
    private long _lastId;
    
    /**
     * Initializes data as read from disk.
     */
    private void initialize() {
        String data = SignatureVerifier.getVerifiedData(getStoredFile(), getKeyFile(), "DSA");
        UpdateCollection uc = UpdateCollection.create(data);
        if(uc != null)
            setData(uc);
    }
    
    /**
     * Notification that a new message has arrived.
     */
    public void handleNewData(final long id, final byte[] data) {
        QUEUE.add(new Runnable() {
            public void run() {
                handleDataInternal(id, data);
            }
        });
    }
    
    /**
     * Handles processing a newly arrived message.
     */
    private void handleDataInternal(long id, byte[] data) {
        String xml = SignatureVerifier.getVerifiedData(data, getKeyFile(), "DSA");
        if(xml != null) {
            UpdateCollection uc = UpdateCollection.create(xml);
            if(uc.getId() == id && id > _lastId)
                storeAndUpdate(data, uc);
        }
    }
    
    /**
     * Stores the given data to disk & posts an update.
     */
    private void storeAndUpdate(byte[] data, UpdateCollection info) {
        FileUtils.verySafeSave(CommonUtils.getUserSettingsDir(), FILENAME, data);
        setData(info);
    }   
    
    /**
     * Sets the most recent data for the given UpdateCollection.
     */
    private void setData(UpdateCollection uc) {
        _lastId = uc.getId();
        Version me;
        try {
            me = new Version(CommonUtils.getLimeWireVersion());
        } catch(VersionFormatException vfe) {
            LOG.warn("Invalid version", vfe);
            return;
        }
        
        _updateInfo = uc.getUpdateDataFor(me, getLanguage());
        notifyAboutInfo(uc.getTimestamp());
    }
    
    /**
     * Determines if we should notify about there being new information.
     */
    private void notifyAboutInfo(long timestamp) {
        if(_updateInfo == null)
            return;
            
        // TODO: pass this to the GUI in a certain amount of time, if necessary.
    }
    
    /**
     * Gets the current language setting.
     */
    private String getLanguage() {
        String lc = ApplicationSettings.LANGUAGE.getValue();
        String cc = ApplicationSettings.COUNTRY.getValue();
        String lv = ApplicationSettings.LOCALE_VARIANT.getValue();
        String lang = lc;
        if(cc != null && !cc.equals(""))
            lang += "_" + cc;
        if(lv != null && !lv.equals(""))
            lang += "_" + lv;
        return lang;
    }
    
    /**
     * Simple accessor for the stored file.
     */
    private File getStoredFile() {
        return new File(CommonUtils.getUserSettingsDir(), FILENAME);
    }
    
    /**
     * Simple accessor for the key file.
     */
    private File getKeyFile() {
        return new File(CommonUtils.getUserSettingsDir(), KEY);
    }
}