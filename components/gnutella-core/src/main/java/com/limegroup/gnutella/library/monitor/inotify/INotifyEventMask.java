package com.limegroup.gnutella.library.monitor.inotify;

/**
 * the following are legal, implemented events that user-space can watch for
 */
public enum INotifyEventMask {

    /**
     * File was accessed
     */
    IN_ACCESS(0x00000001),

    /**
     * File was modified
     */
    IN_MODIFY(0x00000002),

    /**
     * Metadata changed
     */
    IN_ATTRIB(0x00000004),

    /**
     * Writtable file was closed
     */
    IN_CLOSE_WRITE(0x00000008),

    /**
     * Unwrittable file closed
     */
    IN_CLOSE_NOWRITE(0x00000010),

    /**
     * File was opened
     */
    IN_OPEN(0x00000020),

    /**
     * File was moved from X
     */
    IN_MOVED_FROM(0x00000040),

    /**
     * Self was moved.
     */
    IN_MOVE_SELF(0x00000800),

    /**
     * File was moved to Y
     */
    IN_MOVED_TO(0x00000080),

    /**
     * Subfile was created
     */
    IN_CREATE(0x00000100),

    /**
     * Subfile was deleted
     */
    IN_DELETE(0x00000200),

    /**
     * Self was deleted
     */
    IN_DELETE_SELF(0x00000400),

    /* the following are legal events. they are sent as needed to any watch */

    /**
     * Backing fs was unmounted
     */
    IN_UNMOUNT(0x00002000),

    /**
     * Event queued overflowed
     */
    IN_Q_OVERFLOW(0x00004000),

    /**
     * File was ignored
     */
    IN_IGNORED(0x00008000),

    /* special flags */

    /**
     * event occurred against dir
     */
    IN_ISDIR(0x40000000),

    /**
     * only send event once
     */
    IN_ONESHOT(0x80000000),

    /**
     * Only watch the path if it is a directory.
     */
    IN_ONLYDIR(0x01000000),

    /**
     * Do not follow a sym link.
     */
    IN_DONT_FOLLOW(0x02000000),

    /**
     * Add to the mask of an already existing watch.
     */
    IN_MASK_ADD(0x20000000),

    ALL_EVENTS(IN_ACCESS.value | IN_MOVED_FROM.value | IN_OPEN.value | IN_MOVED_TO.value
            | IN_DELETE.value | IN_CREATE.value | IN_DELETE_SELF.value | IN_CLOSE_WRITE.value
            | IN_CLOSE_NOWRITE.value | IN_MODIFY.value | IN_ATTRIB.value | IN_MOVE_SELF.value);

    private int value;

    private INotifyEventMask(int mask) {
        this.value = mask;
    }

    public int getMask() {
        return value;
    }

    public boolean isSet(int mask) {
        return value == (value & mask);
    }

}
