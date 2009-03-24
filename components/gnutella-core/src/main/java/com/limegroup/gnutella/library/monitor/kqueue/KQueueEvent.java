/**
 * 
 */
package com.limegroup.gnutella.library.monitor.kqueue;


public class KQueueEvent {
    private final KQueueEventType type;

    private final String path;

    public KQueueEvent(KQueueEventType type, String path) {
        this.type = type;
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public KQueueEventType getType() {
        return type;
    }
}