package com.limegroup.gnutella.auth;

import org.limewire.io.URNImpl;
import org.limewire.listener.EventListener;


/** 
 * Validates URNs with a content authority.
 */
public interface UrnValidator {

    /** Attempts to validate this URN. */
    public void validate(URNImpl urn);

    /** Returns true is this URN is invalid. */
    public boolean isInvalid(URNImpl urn);

    /** Returns true if this URN is valid or unknown. */
    public boolean isValid(URNImpl urn);

    /** Adds a listener to any URN validation events. */
    public void addListener(EventListener<ValidationEvent> eventListener);
}
