package com.limegroup.gnutella.library.monitor.win32;

public enum W32NotifyActionEventMask {
    FILE_ACTION_ADDED(1), 
    FILE_ACTION_REMOVED(2), 
    FILE_ACTION_MODIFIED(3), 
    FILE_ACTION_RENAMED_OLD_NAME(4), 
    FILE_ACTION_RENAMED_NEW_NAME(5);

    private int value;

    private W32NotifyActionEventMask(int mask) {
        this.value = mask;
    }

    public int getMask() {
        return value;
    }

    public boolean isSet(int mask) {
        return value == (value & mask);
    }
}
