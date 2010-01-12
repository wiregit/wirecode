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
public class ActivationResponse {
    
    private static final String SUCCESSFUL_IS_VALID = "valid";
    
    private final String lid;
    private final String validFlag;
    private final String mcode;
    private final int refreshSeconds;
    private final List<ActivationItem> activationItems; 
    
    public ActivationResponse(String lid, String validFlag, String mcode, 
                              int refreshSeconds, List<ActivationItem> activationItems) {
        this.lid = lid;
        this.validFlag = validFlag;
        this.mcode = mcode;
        this.refreshSeconds = refreshSeconds;
        this.activationItems = Collections.unmodifiableList(new ArrayList<ActivationItem>(activationItems));
    }

    public String getLid() {
        return lid;
    }
    
    public boolean isValidResponse() {
        return (validFlag.equals(SUCCESSFUL_IS_VALID));
    }
    
    public String getMcode() {
        return mcode;
    }
    
    public int getRefreshInterval() {
        return refreshSeconds;
    }
    
    public List<ActivationItem> getActivationItems() {
        return activationItems;
    }
}
