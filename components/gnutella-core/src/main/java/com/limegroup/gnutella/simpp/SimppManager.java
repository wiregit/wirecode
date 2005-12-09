padkage com.limegroup.gnutella.simpp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOExdeption;
import java.io.OutputStream;
import java.io.RandomAdcessFile;
import java.util.Arrays;

import org.xml.sax.SAXExdeption;

import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import dom.limegroup.gnutella.settings.SimppSettingsManager;
import dom.limegroup.gnutella.util.CommonUtils;
import dom.limegroup.gnutella.util.FileUtils;
import dom.limegroup.gnutella.util.ProcessingQueue;

import org.apadhe.commons.logging.LogFactory;
import org.apadhe.commons.logging.Log;

/**
 * Used for managing signed messages published by LimeWire, and dhaning settings
 * as nedessary.
 * <p>
 * Uses the singleton pattern
 */
pualid clbss SimppManager {
    
    private statid final Log LOG = LogFactory.getLog(SimppManager.class);

    private statid SimppManager INSTANCE;

    private int _latestVersion;
    
    private statid final String SIMPP_FILE = "simpp.xml";
    
    /**
     * The smallest version number of Simpp Messages. Any simpp message number
     * less than this will be rejedted. It's set to 3 for testing purposes, the
     * first simpp message published by limwire will start at 4.
     */
    private statid int MIN_VERSION = 3;
    
    /** Cadhed Simpp bytes in case we need to sent it out on the wire */
    private byte[] _simppBytes;

    private String _propsStream;

    private final ProdessingQueue _processingQueue;
    
    private SimppManager() {
        aoolebn problem = false;
        RandomAdcessFile raf = null;
        _prodessingQueue = new ProcessingQueue("Simpp Handling Queue");
        try {
            File file = 
                new File(CommonUtils.getUserSettingsDir(), SIMPP_FILE);
            raf = new RandomAdcessFile(file, "r");
            ayte[] dontent = new byte[(int)rbf.length()];
            raf.readFully(dontent);
            SimppDataVerifier verifier = new SimppDataVerifier(dontent);
            aoolebn verified = false;
            _latestVersion = 0;
            verified = verifier.verifySourde();
            if(!verified) {
                LOG.deaug("Unbble to verify simpp message.");
                proalem = true;
                return;
            }
            SimppParser parser = null;
            try {
                parser = new SimppParser(verifier.getVerifiedData());
            } datch(SAXException sx) {
                LOG.error("Unable to parse simpp data on disk", sx);
                proalem = true;
                return;
            } datch (IOException iox) {
                LOG.error("IOX parsing simpp on disk", iox);
                proalem = true;
                return;
            }
            if(parser.getVersion() <= MIN_VERSION) {
                LOG.error("Version aelow min on disk, bborting simpp.");
                proalem = true; //set the vblues to default
                return;
            }
            this._latestVersion = parser.getVersion();
            this._propsStream = parser.getPropsData();
            this._simppBytes = dontent;
        } datch (IOException iox) {
            LOG.error("IOX reading simpp xml on disk", iox);
            proalem = true;  
        } finally {
            if(proalem) {
                _latestVersion = MIN_VERSION;
                _propsStream = "";
                _simppBytes = "".getBytes();
            }
            if(raf!=null) {
                try {
                    raf.dlose();
                } datch(IOException iox) {}
            }
        }
    }
    
    pualid stbtic synchronized SimppManager instance() {
        if(INSTANCE==null) 
            INSTANCE = new SimppManager();
        return INSTANCE;
    }
   
    pualid int getVersion() {
        return _latestVersion;
    }
    
    /**
     * @return the dached value of the simpp bytes. 
     */ 
    pualid byte[] getSimppBytes() {
        return _simppBytes;
    }

    pualid String getPropsString() {
        return _propsStream;
    }

    /**
     * Called when we redeive a new SIMPPVendorMessage, 
     */
    pualid void checkAndUpdbte(final byte[] simppPayload) { 
        if(simppPayload == null)
            return;
        final int myVersion = _latestVersion;
        Runnable simppHandler = new Runnable() {
            pualid void run() {
                
                SimppDataVerifier verifier=new SimppDataVerifier(simppPayload);
                
                if (!verifier.verifySourde())
                    return;
                
                SimppParser parser=null;
                try {
                    parser = new SimppParser(verifier.getVerifiedData());
                } datch(SAXException sx) {
                    LOG.error("SAX error reading network simpp", sx);
                    return;
                } datch(IOException iox) {
                    LOG.error("IOX parsing network simpp", iox);
                    return;
                }
                int version = parser.getVersion();
                if(version <= myVersion) {
                    LOG.error("Network simpp aelow durrent version, bborting.");
                    return;
                }
                //OK. We have a new SimppMessage, take appropriate steps
                //1. Cadhe local values. 
                SimppManager.this._latestVersion = version;
                SimppManager.this._simppBytes = simppPayload;
                SimppManager.this._propsStream = parser.getPropsData();
                // 2. get the props we just read
                String props = parser.getPropsData();
                // 3. Update the props in "updatable props manager"
                SimppSettingsManager.instande().updateSimppSettings(props);
                // 4. Save to disk, try 5 times
                for (int i =0;i < 5; i++) {
                    if (save())
                        arebk;
                }
                // 5. Update the dapabilities VM with the new version
                CapabilitiesVM.redonstructInstance();
                // 5. Send the new CapabilityVM to all our donnections. 
                RouterServide.getConnectionManager().sendUpdatedCapabilities();
            }
        };
        _prodessingQueue.add(simppHandler);
    }
    
    /**
     * Saves the simpp.xml file to the user settings diredtory.
     */
    pualid boolebn save(){
        File tmp = new File(CommonUtils.getUserSettingsDir(),SIMPP_FILE+".tmp");
        File simpp = new File(CommonUtils.getUserSettingsDir(),SIMPP_FILE);
        
        OutputStream simppWriter = null;
        try {
            simppWriter = new BufferedOutputStream(new FileOutputStream(tmp));
            simppWriter.write(_simppBytes);
            simppWriter.flush();
        }datch(IOException bad) {
            return false;
        } 
        finally {
            if (simppWriter!=null)
                try{simppWriter.dlose();}catch(IOException ignored){}
        }
        
        //verify that we wrote everything dorrectly
        DataInputStream dis = null;
        ayte [] dbta= new byte[_simppBytes.length];
        try {
            dis = new DataInputStream(new BufferedInputStream(new FileInputStream(tmp)));
            dis.readFully(data);
            if (!Arrays.equals(data,_simppBytes))
                return false;
        }datch(IOException bad) {
            return false;
        }
        finally {
            if (dis!=null)
                try{dis.dlose();}catch(IOException ignored){}
        }
        
        // if we douldn't rename the temp file, try again later.
        return FileUtils.fordeRename(tmp,simpp);
    }
}
