package org.limewire.activation.impl;

import org.limewire.io.InvalidDataException;

/**
 * Factory/Parser class for {@link ActivationResponse} objects.
 */
public interface ActivationResponseFactory {
    
    /**
     * Takes a json String and attempts to parse it into an ActivationResponse. If
     * the String is not a valid json String an InvalidDataException will be thrown.
     */
    public ActivationResponse createFromJson(String json) throws InvalidDataException;
    
    /** 
     * Takes a json String that was saved on disk from the last startup and 
     * attempts to parse it into an ActivationRepsonse. If the String is not 
     * a valid json String an InvalidDataException will be thrown.
     */
    public ActivationResponse createFromDiskJson(String json) throws InvalidDataException;
}
