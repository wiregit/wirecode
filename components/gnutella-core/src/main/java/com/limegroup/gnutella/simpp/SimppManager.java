package com.limegroup.gnutella.simpp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;

import org.xml.sax.SAXException;

import com.limegroup.gnutella.NetworkUpdateSanityChecker;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.settings.SimppSettingsManager;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.ProcessingQueue;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Used for managing signed messages published by LimeWire, and chaning settings
 * as necessary.
 * <p>
 * Uses the singleton pattern
 */
public class SimppManager {
    
    private static final Log LOG = LogFactory.getLog(SimppManager.class);

    private static SimppManager INSTANCE;

    private int _latestVersion;
    
    private static final String SIMPP_FILE = "simpp.xml";
    
    /**
     * The smallest version number of Simpp Messages. Any simpp message number
     * less than this will be rejected. It's set to 3 for testing purposes, the
     * first simpp message published by limwire will start at 4.
     */
    private static int MIN_VERSION = 3;
    
    /** Cached Simpp bytes in case we need to sent it out on the wire */
    private byte[] _simppBytes;

    private String _propsStream;

    private final ProcessingQueue _processingQueue;
    
    private SimppManager() {
        boolean problem = false;
        RandomAccessFile raf = null;
        _processingQueue = new ProcessingQueue("Simpp Handling Queue");
        try {
            File file = 
                new File(CommonUtils.getUserSettingsDir(), SIMPP_FILE);
            raf = new RandomAccessFile(file, "r");
            byte[] content = new byte[(int)raf.length()];
            raf.readFully(content);
            SimppDataVerifier verifier = new SimppDataVerifier(content);
            boolean verified = false;
            _latestVersion = 0;
            verified = verifier.verifySource();
            if(!verified) {
                LOG.debug("Unable to verify simpp message.");
                problem = true;
                return;
            }
            SimppParser parser = null;
            try {
                parser = new SimppParser(verifier.getVerifiedData());
            } catch(SAXException sx) {
                LOG.error("Unable to parse simpp data on disk", sx);
                problem = true;
                return;
            } catch (IOException iox) {
                LOG.error("IOX parsing simpp on disk", iox);
                problem = true;
                return;
            }
            if(parser.getVersion() <= MIN_VERSION) {
                LOG.error("Version below min on disk, aborting simpp.");
                problem = true; //set the values to default
                return;
            }
            this._latestVersion = parser.getVersion();
            this._propsStream = parser.getPropsData();
            this._simppBytes = content;
        } catch (IOException iox) {
            LOG.error("IOX reading simpp xml on disk", iox);
            problem = true;  
        } finally {
            if(problem) {
                _latestVersion = MIN_VERSION;
                _propsStream = "";
                _simppBytes = "".getBytes();
            }
            if(raf!=null) {
                try {
                    raf.close();
                } catch(IOException iox) {}
            }
        }
    }
    
    public static synchronized SimppManager instance() {
        if(INSTANCE==null) 
            INSTANCE = new SimppManager();
        return INSTANCE;
    }
   
    public int getVersion() {
        return _latestVersion;
    }
    
    /**
     * @return the cached value of the simpp bytes. 
     */ 
    public byte[] getSimppBytes() {
        return _simppBytes;
    }

    public String getPropsString() {
        return _propsStream;
    }

    /**
     * Called when we receive a new SIMPPVendorMessage, 
     */
    public void checkAndUpdate(final ReplyHandler handler, final byte[] simppPayload) { 
        if(simppPayload == null) {
            RouterService.getNetworkSanityChecker().handleInvalidResponse(handler, NetworkUpdateSanityChecker.SIMPP);
            return;
        }
        final int myVersion = _latestVersion;
        Runnable simppHandler = new Runnable() {
            public void run() {
                
                SimppDataVerifier verifier=new SimppDataVerifier(simppPayload);
                
                if (!verifier.verifySource()) {
                    RouterService.getNetworkSanityChecker().handleInvalidResponse(handler, NetworkUpdateSanityChecker.SIMPP);
                    return;
                } else
                    RouterService.getNetworkSanityChecker().handleValidResponse(handler, NetworkUpdateSanityChecker.SIMPP);
                
                SimppParser parser=null;
                try {
                    parser = new SimppParser(verifier.getVerifiedData());
                } catch(SAXException sx) {
                    LOG.error("SAX error reading network simpp", sx);
                    return;
                } catch(IOException iox) {
                    LOG.error("IOX parsing network simpp", iox);
                    return;
                }
                int version = parser.getVersion();
                if(version <= myVersion) {
                    LOG.error("Network simpp below current version, aborting.");
                    return;
                }
                //OK. We have a new SimppMessage, take appropriate steps
                //1. Cache local values. 
                SimppManager.this._latestVersion = version;
                SimppManager.this._simppBytes = simppPayload;
                SimppManager.this._propsStream = parser.getPropsData();
                // 2. get the props we just read
                String props = parser.getPropsData();
                // 3. Update the props in "updatable props manager"
                SimppSettingsManager.instance().updateSimppSettings(props);
                // 4. Save to disk, try 5 times
                for (int i =0;i < 5; i++) {
                    if (save())
                        break;
                }
                // 5. Update the capabilities VM with the new version
                CapabilitiesVM.reconstructInstance();
                // 5. Send the new CapabilityVM to all our connections. 
                RouterService.getConnectionManager().sendUpdatedCapabilities();
                // 6. Refresh ip filters
                RouterService.adjustSpamFilters();
            }
        };
        _processingQueue.add(simppHandler);
    }
    
    /**
     * Saves the simpp.xml file to the user settings directory.
     */
    public boolean save(){
        File tmp = new File(CommonUtils.getUserSettingsDir(),SIMPP_FILE+".tmp");
        File simpp = new File(CommonUtils.getUserSettingsDir(),SIMPP_FILE);
        
        OutputStream simppWriter = null;
        try {
            simppWriter = new BufferedOutputStream(new FileOutputStream(tmp));
            simppWriter.write(_simppBytes);
            simppWriter.flush();
        }catch(IOException bad) {
            return false;
        } 
        finally {
            if (simppWriter!=null)
                try{simppWriter.close();}catch(IOException ignored){}
        }
        
        //verify that we wrote everything correctly
        DataInputStream dis = null;
        byte [] data= new byte[_simppBytes.length];
        try {
            dis = new DataInputStream(new BufferedInputStream(new FileInputStream(tmp)));
            dis.readFully(data);
            if (!Arrays.equals(data,_simppBytes))
                return false;
        }catch(IOException bad) {
            return false;
        }
        finally {
            if (dis!=null)
                try{dis.close();}catch(IOException ignored){}
        }
        
        // if we couldn't rename the temp file, try again later.
        return FileUtils.forceRename(tmp,simpp);
    }
}
