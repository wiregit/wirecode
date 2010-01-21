package org.limewire.activation.impl;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.limewire.activation.api.ActivationItem;

/**
 * Class representing response data from the activation server.  Immutable
 * 
 * Response should contain:
 * 
 * - lid              (lookup ID)
 * - valid flag       (is the key valid)
 * - mcode            (data that is to be passed on in future HTTP requests, 
 *                     for instance to client-data)
 * - refresh interval (interval before contacting server again)
 * - modules listing  (list of activation items)
 *    
 */
class ActivationResponse {
    
    public enum Type {
        VALID,
        NOTFOUND,
        BLOCKED,
        ERROR,
        REMOVE,
        STOP;        
    }
        
    private final String jsonString;
    private final String lid;
    private final String mcode;
    private final int refreshSeconds;
    private final List<ActivationItem> activationItems;
    private final Type type;
    private final String message;
    
    public ActivationResponse(String jsonString, String lid, Type type, String mcode, 
                              int refreshSeconds, List<ActivationItem> activationItems, String message) {
        this.jsonString = jsonString;
        this.lid = lid;
        this.type = type;
        this.mcode = mcode;
        this.refreshSeconds = refreshSeconds;
        this.activationItems = Collections.unmodifiableList(new ArrayList<ActivationItem>(activationItems));
        this.message = message;
    }

    public String getJSONString() {
        return jsonString;
    }
    
    public String getLid() {
        return lid;
    }
    
    boolean isValidResponse() {
        return type == Type.VALID;
    }
    
    public Type getResponseType() {
        return type;
    }
    
    public String getMCode() {
        return mcode;
    }
    
    public int getRefreshInterval() {
        return refreshSeconds;
    }
    
    public List<ActivationItem> getActivationItems() {
        return activationItems;
    }
    
    public String getMessage() {
        return message;
    }
}
