package com.limegroup.gnutella.simpp;

import java.io.*;
import org.xml.sax.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.settings.*;

/**
 * Used for managing signed messages published by LimeWire, and chaning settings
 * as necessary.
 * <p>
 * Uses the singleton pattern
 */
public class SimppManager {

    private static SimppManager INSTANCE;

    private int _latestVersion;
    
    /**
     * The smallest version number of Simpp Messages. Any simpp message number
     * less than this will be rejected. It's set to 3 for testing purposes, the
     * first simpp message published by limwire will start at 4.
     */
    private static int MIN_VERSION = 3;
    
    /** Cached Simpp bytes in case we need to sent it out on the wire */
    private byte[] _simppBytes;

    private String _propsStream;

    private SimppManager() {
        boolean problem = false;
        RandomAccessFile raf = null;
        try {
            File file = 
                new File(CommonUtils.getUserSettingsDir(), "simpp.xml");
            raf = new RandomAccessFile(file, "r");
            byte[] content = new byte[(int)raf.length()];
            raf.readFully(content);
            SimppDataVerifier verifier = new SimppDataVerifier(content);
            boolean verified = false;
            _latestVersion = 0;
            try {
                verified = verifier.verifySource();
            } catch (ClassCastException ccx) {
                verified = false;
            }
            if(!verified) {
                return;
            }
            SimppParser parser = null;
            try {
                parser = new SimppParser(verifier.getVerifiedData());
            } catch(SAXException sx) {
                return;
            } catch (IOException iox) {
                return;
            }
            if(parser.getVersion() <= MIN_VERSION) {
                return;
            }
            this._latestVersion = parser.getVersion();
            this._propsStream = parser.getPropsData();
            this._simppBytes = content;
        } catch (VerifyError ve) {
            problem = true;
        } catch (IOException iox) {
            problem = true;        
        } finally {
            if(problem) {
                //TODO: do we need to do anything here?
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
    public void checkAndUpdate(final byte[] simppPayload) { 
        if(simppPayload == null)
            return;
        final int myVersion = _latestVersion;
        Thread simppHandler = new ManagedThread("SimppFileHandler") {
            public void managedRun() {
                SimppDataVerifier verifier=new SimppDataVerifier(simppPayload);
                boolean verified = false;
                try {
                    verified = verifier.verifySource();
                } catch (ClassCastException ccx) {
                    verified = false;
                }
                if(!verified) 
                    return;
                SimppParser parser=null;
                try {
                    parser = new SimppParser(verifier.getVerifiedData());
                } catch(SAXException sx) {
                    return;
                } catch(IOException iox) {
                    return;
                }
                int version = parser.getVersion();
                if(version <= myVersion)
                    return;
                //OK. We have a new SimppMessage, take appropriate steps
                //1. Cache local values. 
                SimppManager.this._latestVersion = version;
                SimppManager.this._simppBytes = simppPayload;
                SimppManager.this._propsStream = parser.getPropsData();
                // 2. get the props we just read
                String props = parser.getPropsData();
                // 3. Update the props in "updatable props manager"
                SimppSettingsManager.instance().updateSimppSettings(props,true);
                // 4. Update the capabilities VM with the new version
                CapabilitiesVM.updateSimppVersion(version);
                // 5. Send the new CapabilityVM to all our connections. 
                RouterService.getConnectionManager().sendUpdatedCapabilities();
            }
        };
        simppHandler.setDaemon(true);
        simppHandler.start();
    }
}
