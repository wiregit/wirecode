package com.limegroup.gnutella.library.monitor.kqueue;

public enum KQueueEventMask {

    /**
     * vnode was removed
     */
    NOTE_DELETE(0x00000001),

    /**
     * data contents changed
     */
    NOTE_WRITE(0x00000002),

    /**
     * size increased
     */
    NOTE_EXTEND(0x00000004),

    /**
     * attributes changed
     */
    NOTE_ATTRIB(0x00000008),

    /**
     * link count changed
     */
    NOTE_LINK(0x00000010),

    /**
     * vnode was renamed
     */
    NOTE_RENAME(0x00000020),

    /**
     * vnode access was revoked
     */
    NOTE_REVOKE(0x00000040),

    /**
     * Mask that is the super set of all known events.
     */
    ALL_EVENTS(NOTE_DELETE.value | NOTE_WRITE.value | NOTE_EXTEND.value | NOTE_ATTRIB.value
            | NOTE_LINK.value | NOTE_RENAME.value | NOTE_REVOKE.value);

    private int value;

    private KQueueEventMask(int mask) {
        this.value = mask;
    }

    public int getMask() {
        return value;
    }

    public boolean isSet(int mask) {
        return value == (value & mask);
    }
}
