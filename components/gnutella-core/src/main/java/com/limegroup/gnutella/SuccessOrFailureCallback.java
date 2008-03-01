package com.limegroup.gnutella;

import com.limegroup.gnutella.geocode.Geocoder;

/**
 * Instances of this interface can either consume or a {@link String} or has
 * their status set to invalid. Ths particular example in this package is
 * {@link Geocoder} in the context that when remotely requesting geo information
 * or doing so locally during testing it must asynchronously consume a String or
 * have a flag set to <em>invalid</em> to show that no information was
 * available
 */
public interface SuccessOrFailureCallback<T> {

    /**
     * Tells <code>this</code> that a failure occured.
     * 
     * @param reason reason for the failure
     */
    void setInvalid(Throwable reason);
    
    /**
     * Returns the reason processing failed or <code>null</code> for no failure.
     * 
     * @return the reason processing failed or <code>null</code> for no failure.
     */
    Throwable getReasonForFailure();
    
    /**
     * Gives <code>this</code> a {@link T} result to process.
     * 
     * @param t {@link T} to process
     */
    void process(T t);
}
