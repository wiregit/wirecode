package org.limewire.activation.impl;

import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.serial.ActivationMemento;
import org.limewire.activation.serial.ActivationMementoImpl;
import org.limewire.setting.ActivationSettings;
import org.limewire.util.OSUtils;

public class ActivationItemImpl implements ActivationItem {

    private final int intID;
    private final ActivationID moduleID;
    private final String licenseName;
    private final long datePurchased;
    private final long dateExpired;
    private final Status currentStatus;
    
    public ActivationItemImpl(int intID, String licenseName, long datePurchased, long dateExpired,
            Status currentStatus) {
        this.intID = intID;
        this.moduleID = ActivationID.getActivationID(intID);
        this.licenseName = licenseName;
        this.datePurchased = datePurchased;
        this.dateExpired = dateExpired;
        this.currentStatus = updateActivationStatus(currentStatus);   
        
        // when loaded from disk, we compare the date expired with the current time of the system
        // if the user's system clock is in a different time zone or extremely messed up this might
        // fail but this is quite an edge case
//        if(isLoadedFromDisk) {
//            currentStatus = System.currentTimeMillis() > dateExpired;
//        } else {
//            this.currentStatus = currentStatus;            
//        }
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
    public Status getStatus() {
        return currentStatus;
    }
    
    public boolean isMementoSupported() {
        return true;
    }
    
    public ActivationMemento toActivationMemento() {
        ActivationMemento memento = new ActivationMementoImpl();
        memento.setID(intID);
        memento.setDatePurchased(datePurchased);
        memento.setDateExpired(dateExpired);
        memento.setLicenseName(licenseName);
        memento.setStatus(currentStatus);
        return memento;
    }
    
    /**
     * This allows the status to be changed depending on the OS, LW version installed.
     */
    private Status updateActivationStatus(Status status) {
        if(status == Status.ACTIVE) {
            if(moduleID == ActivationID.UNKNOWN_MODULE)
                return Status.UNUSEABLE_LW;
            if(moduleID == ActivationID.AVG_MODULE && !OSUtils.isAVGCompatibleWindows())
                return Status.UNUSEABLE_OS;
            return status;
        } else {
            return status;
        }
    }
}
