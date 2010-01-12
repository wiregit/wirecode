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
        this(intID, licenseName, datePurchased, dateExpired, currentStatus, false);
    }
    
    public ActivationItemImpl(int intID, String licenseName, long datePurchased, long dateExpired,
            Status currentStatus, boolean isLoadedFromDisk) {
        this.intID = intID;
        this.moduleID = ActivationID.getActivationID(intID);
        this.licenseName = licenseName;
        this.datePurchased = datePurchased;
        this.dateExpired = dateExpired;
        this.currentStatus = updateActivationStatus(currentStatus, isLoadedFromDisk);   
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
    private Status updateActivationStatus(Status status, boolean isLoadedFromDisk) {
        if(status == Status.ACTIVE) {
            if(moduleID == ActivationID.UNKNOWN_MODULE)
                return Status.UNUSEABLE_LW;
            if(moduleID == ActivationID.AVG_MODULE && !OSUtils.isAVGCompatibleWindows())
                return Status.UNUSEABLE_OS;
            if(isLoadedFromDisk && System.currentTimeMillis() > dateExpired)
                return Status.EXPIRED;
            return status;
        } else {
            return status;
        }
    }
}
