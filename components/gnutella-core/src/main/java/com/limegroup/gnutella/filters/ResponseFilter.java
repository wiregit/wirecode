package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.Response;

/**
 * Both an interface for implementing and a factory for obtaining 
 * a single instance of ResponseFilter. Currently there is only
 * one implementation - the URNResponseFilter, in the future there
 * could be more, and a composite instance would likely be returned
 * by the factory.
 */
public abstract class ResponseFilter {
    private static URNResponseFilter urnResponseFilter_ = new URNResponseFilter();
    /**
     * Used to obtain the only ResponseFilter instance
     */
    public static ResponseFilter instance() {
        //could return composite in future
        return urnResponseFilter_;
    }
    /**
     * Should this Response be allowed or not? If not, it will be filtered out
     */
    public abstract boolean allow(Response m);
    /**
     * Update yourself as necessary
     */
    public abstract void refresh();
}
