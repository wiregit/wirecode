package com.limegroup.gnutella.simpp;

import java.util.List;


import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.settings.SimppSettingsManager;

public interface SimppManager {
    
    public void initialize();

    public int getVersion();
    
    public int getNewVersion();
    
    public int getKeyVersion();

    /**
     * @return the cached value of the simpp bytes. 
     */
    public byte[] getSimppBytes();

    public void addSimppSettingsManager(SimppSettingsManager simppSettingsManager);

    public List<SimppSettingsManager> getSimppSettingsManagers();

    public void addListener(SimppListener listener);

    public void removeListener(SimppListener listener);

    public void checkAndUpdate(final ReplyHandler handler, final byte[] data);

    public byte[] getOldUpdateResponse();

    /**
     * @param version advertised simpp version by other client, -1 if client 
     * does not support simpp version
     * @param newVersion advertised newVersion by other client, -1 if client 
     * does not support new simpp version
     * @param keyVersion adtvertised keyVersion by other client, -1 if client
     * does not support key version
     * @return true if a simpp message should be requested
     */
    public boolean shouldRequestSimppMessage(int version, int newVersion, int keyVersion);
}
