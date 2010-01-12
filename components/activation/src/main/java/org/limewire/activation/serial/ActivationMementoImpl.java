package org.limewire.activation.serial;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.limewire.activation.api.ActivationItem.Status;

public class ActivationMementoImpl implements ActivationMemento, Serializable {

    private static final long serialVersionUID = 0L;
    
    private Map<String, Object> serialObjects = new HashMap<String, Object>();
    
    @Override
    public Date getDateExpired() {
       Long value = (Long)serialObjects.get("expired");
       if(value == null)
           return null;
       else
           return new Date(value);
    }

    @Override
    public Date getDatePurchased() {
        Long value = (Long)serialObjects.get("purchased");
        if(value == null)
            return null;
        else
            return new Date(value);
    }

    @Override
    public int getID() {
        Integer value = (Integer)serialObjects.get("id");
        if(value == null)
            return -1;
        else
            return value;
    }

    @Override
    public String getLicenseName() {
        return (String)serialObjects.get("licenseName");
    }

    @Override
    public Status getStatus() {
        return (Status)serialObjects.get("status");
    }

    @Override
    public void setDateExpired(long dateExpired) {
        serialObjects.put("expired", dateExpired);
    }

    @Override
    public void setDatePurchased(long datePurchased) {
        serialObjects.put("purchased", datePurchased);
    }

    @Override
    public void setID(int id) {
        serialObjects.put("id", id);
    }

    @Override
    public void setLicenseName(String licenseName) {
        serialObjects.put("licenseName", licenseName); 
    }

    @Override
    public void setStatus(Status status) {
        serialObjects.put("status", status);
    }
}
