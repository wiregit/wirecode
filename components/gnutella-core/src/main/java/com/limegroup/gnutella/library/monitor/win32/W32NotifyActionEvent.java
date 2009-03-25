package com.limegroup.gnutella.library.monitor.win32;

public class W32NotifyActionEvent {

    private final int mask;

    private final String path;

    public W32NotifyActionEvent(int mask, String path) {
        this.mask = mask;
        this.path = path;
    }

    public int getMask() {
        return mask;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return mask + " - " + path;
    }

    boolean isDelete() {
        return W32NotifyActionEventMask.FILE_ACTION_REMOVED.isSet(mask);
    }

    boolean isCreate() {
        return W32NotifyActionEventMask.FILE_ACTION_ADDED.isSet(mask);
    }

    boolean isModify() {
        return W32NotifyActionEventMask.FILE_ACTION_MODIFIED.isSet(mask);
    }

    boolean isRenamedNewName() {
        return W32NotifyActionEventMask.FILE_ACTION_RENAMED_NEW_NAME.isSet(mask);
    }

    boolean isRenamedOldName() {
        return W32NotifyActionEventMask.FILE_ACTION_RENAMED_OLD_NAME.isSet(mask);
    }
}
