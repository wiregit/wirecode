package com.limegroup.gnutella.simpp;

import java.io.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.vendor.*;

/**
 * Used for managing signed messages published by LimeWire, and chaning settings
 * as necessary.
 * <p>
 * Uses the singleton pattern
 */
public class SimppManager {

    private static SimppManager INSTANCE;

    private int _latestVersion;
    

    /** Cached Simpp bytes in case we need to sent it out on the wire */
    private byte[] _simppBytes;

    private SimppManager() {
        boolean problem = false;
        RandomAccessFile raf = null;
        try {
            SettablePropsHandler propsHandler = SettablePropsHandler.instance();
            File file = 
                new File(CommonUtils.getUserSettingsDir(), "settableprops.xml");
            raf = new RandomAccessFile(file, "r");
            byte[] content = new byte[(int)raf.length()];
            raf.readFully(content);
            SimppDataVerifier verifier = new SimppDataVerifier(content);
            boolean verified = false;
            try {
                verified = verifier.verifySource();
            } catch (ClassCastException ccx) {
                verified = false;
            }
            if(!verified) {
                _latestVersion = propsHandler.useDefaultProps();
                return;
            }
            SimppParser parser = new SimppParser(verifier.getVerifiedData());
            if(parser.getVersion() < 0) {
                latestVersion = propsHandler.useDefaultProps();
                return;
            }
            _latestVersion = parser.getVersion();
            this._simppBytes = content;
            String propsInXML = parser.getPropsData();
            propsHandler.setProps(propsInXML);
        } catch (VerifyError ve) {
            problem = true;
        } catch (IOException iox) {
            problem = true;        
        } finally {
            if(problem)
                _latestVersion = propsHandler.useDefaultProps();
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
                SimppParser parser=new SimppParser(verifier.getVerifiedData());
                int version = parser.getVersion();
                if(version <= myVersion)
                    return;
                //OK. We have a new SimppMessage, take appropriate steps
                //1. Cache local values. 
                SimppManager.this._latestVersion = version;
                SimppManager.this._simppBytes = simppPayload;
                // 2. get the props we just read
                String props = parser.getPropsData();
                // 3. Update the props in "updatable props manager"
                SettablePropsHandler.instance().setProps(props);
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
