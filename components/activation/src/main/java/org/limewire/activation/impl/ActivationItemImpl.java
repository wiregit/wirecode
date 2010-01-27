package org.limewire.activation.impl;

import java.util.Date;

import org.limewire.activation.api.ActSettings;
import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;
import org.limewire.util.OSUtils;

class ActivationItemImpl implements ActivationItem {

    private final ActSettings activationSettings;
    
    private final int intID;
    private final ActivationID moduleID;
    private final String licenseName;
    private final Date datePurchased;
    private final Date dateExpired;
    private final Status currentStatus;
    
    public ActivationItemImpl(ActSettings activationSettings, int intID, String licenseName, Date datePurchased, 
            Date dateExpired, Status currentStatus) {
        this(activationSettings, intID, licenseName, datePurchased, dateExpired, currentStatus, false);
    }
    
    public ActivationItemImpl(ActSettings activationSettings, int intID, String licenseName, Date datePurchased, 
            Date dateExpired, Status currentStatus, boolean isLoadedFromDisk) {
        this.activationSettings = activationSettings;
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
    public Date getDateExpired() {
        return dateExpired;
    }
    
    @Override
    public Date getDatePurchased() {
        return datePurchased;
    }
    
    @Override
    public String getLicenseName() {
        return licenseName;
    }
    
    @Override
    public String getURL() {
        return activationSettings.getActivationRenewalHost() + Integer.toString(intID);
    }

    @Override
    public Status getStatus() {
        return currentStatus;
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
            if(isLoadedFromDisk && System.currentTimeMillis() > dateExpired.getTime())
                return Status.EXPIRED;
            return status;
        } else {
            return status;
        }
    }
    
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ActivationItemImpl)) {
            return false;    
        }
        ActivationItemImpl item = (ActivationItemImpl)object;
        return item.getDateExpired().equals(dateExpired) &&
               item.getDatePurchased().equals(datePurchased) &&
               item.getLicenseName().equals(licenseName) &&
               item.getModuleID().equals(moduleID) &&
               item.getStatus().equals(currentStatus) &&
               item.getURL().equals(getURL());
    }
}
