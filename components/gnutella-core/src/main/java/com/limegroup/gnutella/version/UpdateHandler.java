package com.limegroup.gnutella.version;


import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.ProcessingQueue;
import com.limegroup.gnutella.security.SignatureVerifier;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.UpdateSettings;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;

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
    private int _lastId;
    
    /**
     * The bytes to send on the wire.
     *
     * TODO: Don't store in memory.
     */
    private byte[] _lastBytes;
    
    /**
     * Initializes data as read from disk.
     */
    private void initialize() {
        LOG.trace("Initializing UpdateHandler");
        handleDataInternal(readDataFromDisk(), true);
    }
    
    /**
     * Notification that a new message has arrived.
     */
    public void handleNewData(final byte[] data) {
        if(data != null) {
            QUEUE.add(new Runnable() {
                public void run() {
                    LOG.trace("Parsing new data...");
                    handleDataInternal(data, false);
                }
            });
        }
    }
    
    /**
     * Retrieves the latest version available.
     */
    public int getLatestVersion() {
        return _lastId;
    }
    
    /**
     * Gets the bytes to send on the wire.
     */
    public byte[] getLatestBytes() {
        return _lastBytes;
    }
    
    /**
     * Handles processing a newly arrived message.
     */
    private void handleDataInternal(byte[] data, boolean fromDisk) {
        String xml = SignatureVerifier.getVerifiedData(data, getKeyFile(), "DSA", "SHA1");
        if(xml != null) {
            UpdateCollection uc = UpdateCollection.create(xml);
            if(uc.getId() > _lastId)
                storeAndUpdate(data, uc, fromDisk);
        } else {
            LOG.warn("Couldn't verify signature on data.");
        }
    }
    
    /**
     * Stores the given data to disk & posts an update.
     */
    private void storeAndUpdate(byte[] data, UpdateCollection uc, boolean fromDisk) {
        LOG.trace("Retrieved new data, storing & updating.");
        _lastId = uc.getId();
        _lastBytes = data;
        
        if(!fromDisk) {
            FileUtils.verySafeSave(CommonUtils.getUserSettingsDir(), FILENAME, data);
            CapabilitiesVM.reconstructInstance();
            RouterService.getConnectionManager().sendUpdatedCapabilities();
        }

        Version limeV;
        try {
            limeV = new Version(CommonUtils.getLimeWireVersion());
        } catch(VersionFormatException vfe) {
            LOG.warn("Invalid LimeWire version", vfe);
            return;
        }

        Version javaV = null;        
        try {
            javaV = new Version(CommonUtils.getJavaVersion());
        } catch(VersionFormatException vfe) {
            LOG.warn("Invalid java version", vfe);
        }
        
        // don't allow someone to set the style to be above major.
        int style = Math.min(UpdateInformation.STYLE_MAJOR,
                             UpdateSettings.UPDATE_STYLE.getValue());
        
        _updateInfo = uc.getUpdateDataFor(limeV, 
                                          getLanguage(),
                                          CommonUtils.isPro(),
                                          style,
                                          javaV);

        notifyAboutInfo(uc.getTimestamp(), fromDisk);
    }
    
    /**
     * Determines if we should notify about there being new information.
     */
    private void notifyAboutInfo(long timestamp, boolean fromDisk) {
        if(_updateInfo == null) {
            LOG.warn("No relevant update info to notify about.");
            return;
        }
            
        System.out.println("There is an update available: " + _updateInfo);
        // TODO: pass this to the GUI in a certain amount of time, if necessary.
    }
    
    /**
     * Reads the data from disk.
     */
    private byte[] readDataFromDisk() {
        RandomAccessFile raf = null;
        File source = getStoredFile();
        int length = (int)source.length();
        if(length <= 0)
            return null;
        byte[] data = new byte[length];
        try {
            raf = new RandomAccessFile(source, "r");
            raf.readFully(data);
        } catch(IOException ioe) {
            LOG.warn("Unable to read data file.", ioe);
            return null;
        } finally {
            IOUtils.close(raf);
        }
        return data;
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