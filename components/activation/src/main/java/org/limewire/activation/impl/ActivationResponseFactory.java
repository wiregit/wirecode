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
}
