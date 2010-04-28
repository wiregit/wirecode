package org.limewire.activation.impl;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.limewire.activation.api.ActivationItem;

/**
 * Class representing response data from the activation server.  Immutable
 * 
 * Response should contain:
 * <pre>
 * - lid              (lookup ID)
 * - valid flag       (is the key valid)
 * - mcode            (data that is to be passed on in future HTTP requests, 
 *                     for instance to client-data)
 * - refresh interval (interval before contacting server again)
 * - modules listing  (list of activation items)
 * </pre>   
 */
class ActivationResponse {
    
    /**
     * Type of response recieved from the server when trying to authenticate
     * a License Key.
     */
    public enum Type {
        /**
         * The License Key is currently valid. This does not guarentee the 
         * License Key has active modules.
         */
        VALID,
        
        /** The License Key could not be found or is not valid. */
        NOTFOUND,
        
        /** The License Key is currently blocked for abuse. */
        BLOCKED,
        
        /** There was some communication error with the server. */
        ERROR,
        
        /** The current License Key should be removed from within the client. */
        REMOVE,
        
        /** The client should no longer contact the server at startup. */
        STOP        
    }
        
    private final String jsonString;
    private final String lid;
    private final String mcode;
    private final String token;
    private final int refreshMinutes;
    private final List<ActivationItem> activationItems;
    private final Type type;
    private final String message;
    
    public ActivationResponse(String jsonString, String lid, Type type, String mcode, String token, 
                              int refreshMinutes, List<ActivationItem> activationItems, String message) {
        this.jsonString = jsonString;
        this.lid = lid;
        this.type = type;
        this.mcode = mcode;
        this.token = token;
        this.refreshMinutes = refreshMinutes;
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
    
    public String getToken() {
        return token;
    }
    
    public int getRefreshIntervalInMinutes() {
        return refreshMinutes;
    }
    
    public List<ActivationItem> getActivationItems() {
        return activationItems;
    }
    
    public String getMessage() {
        return message;
    }
}
