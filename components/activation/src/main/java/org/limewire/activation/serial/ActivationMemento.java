package org.limewire.activation.serial;

import org.limewire.activation.api.ActivationItem.Status;

public interface ActivationMemento {

    public void setID(int id);
    
    public void setLicenseName(String licenseName);
    
    public void setDatePurchased(long datePurchased);
    
    public void setDateExpired(long dateExpired);
    
    public void setStatus(Status status);
    
    public int getID();
    
    public String getLicenseName();
    
    public long getDatePurchased();
    
    public long getDateExpired();
    
    public Status getStatus();
}
