/**
 * SwarmSource objects have a getType method which will return one of the following enumerations.
 * This allows the swarmer to register a source type with a source handler.
 */
package org.limewire.swarm;

public enum SwarmSourceType {
    HTTP();
}