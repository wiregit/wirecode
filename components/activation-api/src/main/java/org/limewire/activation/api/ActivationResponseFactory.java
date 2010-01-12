package org.limewire.activation.api;

import org.limewire.activation.impl.ActivationResponse;
import org.limewire.io.InvalidDataException;

/**
 * Factory/Parser class for {@link ActivationResponse} objects.
 */
public interface ActivationResponseFactory {
    
    // todo: doc
    public ActivationResponse createFromJson(String json) throws InvalidDataException;
}
