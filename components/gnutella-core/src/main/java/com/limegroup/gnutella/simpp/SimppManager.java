package com.limegroup.gnutella.simpp;

import java.io.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;

/**
 * Used for managing signed messages published by LimeWire, and chaning settings
 * as necessary.
 * <p>
 * Uses the singleton pattern
 */
public class SimppManager {

    private static final SimppManager INSTANCE;

    private int latestVersion;

    private SimppManager() {
        boolean problem = false;
        try {
            SettablePropsHandler propsHandler = SettablePropsHandler.instance();
            File file = 
                new File(CommonUtils.getUserSettingsDir(), "settableprops.xml");
            raf = new RandomAccessFile(file, "r");
            byte[] content = new byte[(int)raf.length()];
            raf.readFully(content);
            latestVersion = verifier.getVersion();
            SimppFileVerifier verifier = new SimppFileVerifier(content);
            boolean verified = false;
            try {
                verified = verifier.verifySource();
            } catch (ClassCastException ccx) {
                verified = false;
            }
            if(!verified) {
                latestVersion = propsHandler.useDefaultProps();
                return;
            }
            
            String propsInXML = new String(verifier.getPropsData(), "UTF-8");
            propsHandler.setProps(propsInXML);
        } catch (VerifyError ve) {
            problem = true;
        } catch (IOException iox) {
            problem = true;
        } catch (SAXException sax) {
            problem = true;
        } finally {
            if(problem)
                latestVersion = propsHandler.useDefaultProps();
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
        return latestVersion;
    }
    
    /**
     * Called when we receive a new SIMPPVendorMessage, 
     */
    public void checkAndUpdate(final byte[] simppPayload) {        
        final int myVersion = latestVersion;
        //TODO: Should this be in a different thread??
        //Thread simppHandler = new ManagedThread("SimppFileHandler") {
        //public void managedRun() {
        if(simppPayload == null)
            return;
        SimppFileVerifier verifier=new SimppFileVerifier(simppPayload);
        boolean verified = false;
        try {
            verified = verifier.verifySource();
        } catch (ClassCastException ccx) {
            verified = false;
        }
        if(!verified) 
            return;
        int version = verifier.getVersion();
        if(version <= myVersion)
            return;
        SimppManager.this.latestVersion = version;
        String props = new String(verifier.getPropsData(),"UTF-8");
        SettablePropsHandler.instance().setProps(props);
        //}
        //};
        //simppHandler.setDaemon(true);
        //  simppHandler.start();
    }
}
