/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.event;

public interface BootstrapListener {
    public void bootstrap(boolean succeeded, long time);
}
