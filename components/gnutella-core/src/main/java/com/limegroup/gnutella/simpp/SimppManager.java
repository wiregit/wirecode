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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.NetworkUpdateSanityChecker;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.NetworkUpdateSanityChecker.RequestType;
import com.limegroup.gnutella.settings.SimppSettingsManager;

/**
 * Used for managing signed messages published by LimeWire, and chaning settings
 * as necessary.
 * <p>
 * Uses the singleton pattern
 */
public class SimppManager {
    
    private static final Log LOG = LogFactory.getLog(SimppManager.class);
   
    private static final String SIMPP_FILE = "simpp.xml";
    
    /**
     * The smallest version number of Simpp Messages. Any simpp message number
     * less than this will be rejected. It's set to 3 for testing purposes, the
     * first simpp message published by limwire will start at 4.
     */
    private static int MIN_VERSION = 3;
    
    private static SimppManager INSTANCE;
    
    /** Listeners for simpp updates */
    private final List<SimppListener> listeners = new CopyOnWriteArrayList<SimppListener>();
   
    /** Cached Simpp bytes in case we need to sent it out on the wire */
    private byte[] _simppBytes;

    private String _propsStream;

    private final ExecutorService _processingQueue;
    
    private final CopyOnWriteArrayList<SimppSettingsManager> simppSettingsManagers;
    
    private int _latestVersion;
    
    @Inject // DPINJ: GET RID OF STATIC INJECTION!
    private static Provider<NetworkUpdateSanityChecker> networkUpdateSanityChecker;
    
    private SimppManager() {
        this.simppSettingsManagers = new CopyOnWriteArrayList<SimppSettingsManager>();
        boolean problem = false;
        RandomAccessFile raf = null;
        _processingQueue = ExecutorsHelper.newProcessingQueue("Simpp Handling Queue");
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
            }catch (IOException iox) {
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
            networkUpdateSanityChecker.get().handleInvalidResponse(handler, RequestType.SIMPP);
            return;
        }
        
        final int myVersion = _latestVersion;
        Runnable simppHandler = new Runnable() {
            public void run() {
                
                SimppDataVerifier verifier=new SimppDataVerifier(simppPayload);
                
                if (!verifier.verifySource()) {
                    networkUpdateSanityChecker.get().handleInvalidResponse(handler, RequestType.SIMPP);
                    return;
                } else {
                    networkUpdateSanityChecker.get().handleValidResponse(handler, RequestType.SIMPP);
                }
                
                SimppParser parser=null;
                try {
                    parser = new SimppParser(verifier.getVerifiedData());
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
                _latestVersion = version;
                _simppBytes = simppPayload;
                _propsStream = parser.getPropsData();
                // 2. get the props we just read
                String props = parser.getPropsData();
                // 3. Update the props in "updatable props manager"
                for(SimppSettingsManager ssm : simppSettingsManagers)
                    ssm.updateSimppSettings(props);
                // 4. Save to disk, try 5 times
                for (int i =0;i < 5; i++) {
                    if (save())
                        break;
                }
                // 5. Notify listeners
                for (SimppListener listener : listeners)
                    listener.simppUpdated(version);
            }
        };
        _processingQueue.execute(simppHandler);
    }
    
    public void addSimppSettingsManager(SimppSettingsManager simppSettingsManager) {
        simppSettingsManagers.add(simppSettingsManager);
    }

    public List<SimppSettingsManager> getSimppSettingsManagers() {
        return simppSettingsManagers;
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
    
    public void addListener(SimppListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(SimppListener listener) {
        listeners.remove(listener);
    }
}
