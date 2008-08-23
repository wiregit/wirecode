package org.limewire.swarm;

/**
 * SwarmSource objects have a getType method which will return one of the
 * following enumerations. This allows implementations of {@link SwarmSourceHandler}
 * to register with a {@link Swarmer}.
 */
public enum SwarmSourceType {
    HTTP();
}