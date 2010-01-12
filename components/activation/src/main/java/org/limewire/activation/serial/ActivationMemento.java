package org.limewire.activation.serial;

import java.util.Date;

import org.limewire.activation.api.ActivationItem.Status;

public interface ActivationMemento {

    public void setID(int id);
    
    public void setLicenseName(String licenseName);
    
    public void setDatePurchased(long datePurchased);
    
    public void setDateExpired(long dateExpired);
    
    public void setStatus(Status status);
    
    public int getID();
    
    public String getLicenseName();
    
    public Date getDatePurchased();
    
    public Date getDateExpired();
    
    public Status getStatus();
}
