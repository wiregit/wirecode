package org.limewire.activation.impl;

import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationID;
import org.limewire.setting.ActivationSettings;

public class ActivationItemImpl implements ActivationItem {

    private final int intID;
    private final ActivationID moduleID;
    private final String licenseName;
    private final long datePurchased;
    private final long dateExpired;
    private final boolean isExpired;
    
    public ActivationItemImpl(int intID, String licenseName, long datePurchased, long dateExpired,
            boolean isExpired, boolean isLoadedFromDisk) {
        this.intID = intID;
        this.moduleID = ActivationID.getActivationID(intID);
        this.licenseName = licenseName;
        this.datePurchased = datePurchased;
        this.dateExpired = dateExpired;

        // when loaded from disk, we compare the date expired with the current time of the system
        // if the user's system clock is in a different time zone or extremely messed up this might
        // fail but this is quite an edge case
        if(isLoadedFromDisk) {
             this.isExpired = System.currentTimeMillis() > dateExpired;
        } else {
            this.isExpired = isExpired;            
        }
    }
    
    @Override
    public ActivationID getModuleID() {
        return moduleID;
    }
    
    @Override
    public long getDateExpired() {
        return dateExpired;
    }
    @Override
    public long getDatePurchased() {
        return datePurchased;
    }
    @Override
    public String getLicenseName() {
        return licenseName;
    }
    @Override
    public String getURL() {
        return ActivationSettings.ACTIVATION_RENEWAL_HOST + Integer.toString(intID);
    }
    @Override
    public boolean isUseable() {
        return moduleID != ActivationID.UNKNOWN_MODULE;
    }
    @Override
    public boolean isActive() {
        return !isExpired;
    }
}
