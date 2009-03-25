package com.limegroup.gnutella.library.monitor.inotify;

import java.io.File;
import java.util.Map;

import com.sun.jna.Memory;
import com.sun.jna.Structure;

public class INotifyEvent extends Structure {
    public int wd;

    public int mask;

    public int cookie;

    public int len;

    private String path = null;

    private String watchPath = null;

    public int readStruct(Memory m, int offset, Map<Integer, String> watchDescriptorFiles) {
        useMemory(m, offset);
        read();
        int baseSize = size();
        int byteOffset = offset + baseSize;
        this.path = m.getString(byteOffset);
        this.watchPath = watchDescriptorFiles.get(wd);
        return baseSize + len;
    }

    public String getPath() {
        return path;
    }

    public String getWatchPath() {
        return watchPath;
    }

    public String getFullPath() {
        return getWatchPath() + File.separator + getPath();
    }

    boolean isAccessEvent() {
        return INotifyEventMask.IN_ACCESS.isSet(mask);
    }

    boolean isModifyEvent() {
        return INotifyEventMask.IN_MODIFY.isSet(mask);
    }

    boolean isAttributeEvent() {
        return INotifyEventMask.IN_ATTRIB.isSet(mask);
    }

    boolean isCloseWriteEvent() {
        return INotifyEventMask.IN_CLOSE_WRITE.isSet(mask);
    }

    boolean isCloseNoWriteEvent() {
        return INotifyEventMask.IN_CLOSE_NOWRITE.isSet(mask);
    }

    boolean isOpenEvent() {
        return INotifyEventMask.IN_OPEN.isSet(mask);
    }

    boolean isMovedFromEvent() {
        return INotifyEventMask.IN_MOVED_FROM.isSet(mask);
    }

    boolean isMovedToEvent() {
        return INotifyEventMask.IN_MOVED_TO.isSet(mask);
    }

    boolean isMovedSelfEvent() {
        return INotifyEventMask.IN_MOVE_SELF.isSet(mask);
    }

    boolean isCreateEvent() {
        return INotifyEventMask.IN_CREATE.isSet(mask);
    }

    boolean isDeleteSelfEvent() {
        return INotifyEventMask.IN_DELETE_SELF.isSet(mask);
    }

    boolean isDeleteEvent() {
        return INotifyEventMask.IN_DELETE.isSet(mask);
    }

    boolean isUnmountEvent() {
        return INotifyEventMask.IN_UNMOUNT.isSet(mask);
    }

    boolean isDirEvent() {
        return INotifyEventMask.IN_ISDIR.isSet(mask);
    }

    boolean isOneShotEvent() {
        return INotifyEventMask.IN_ONESHOT.isSet(mask);
    }

    boolean isQOverFlowEvent() {
        return INotifyEventMask.IN_Q_OVERFLOW.isSet(mask);
    }

    boolean isIgnoredEvent() {
        return INotifyEventMask.IN_IGNORED.isSet(mask);
    }

    boolean isOnlyWatchDir() {
        return INotifyEventMask.IN_ONLYDIR.isSet(mask);
    }

    boolean isDontFollowSymLink() {
        return INotifyEventMask.IN_DONT_FOLLOW.isSet(mask);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("watchpath=").append(watchPath).append(" ").append("path=").append(
                path).append(" ").append("wd=").append(wd).append(" ").append("mask=").append(mask)
                .append(" ").append("cookie=").append(cookie).append(" ").append("len=")
                .append(len).append("|");

        if (isAccessEvent()) {
            stringBuilder.append(",").append("isAccessEvent");
        }
        if (isAttributeEvent()) {
            stringBuilder.append(",").append("isAttributeEvent");
        }
        if (isCloseNoWriteEvent()) {
            stringBuilder.append(",").append("isCloseNoWriteEvent");
        }
        if (isCloseWriteEvent()) {
            stringBuilder.append(",").append("isCloseWriteEvent");
        }
        if (isCreateEvent()) {
            stringBuilder.append(",").append("isCreateEvent");
        }
        if (isDeleteEvent()) {
            stringBuilder.append(",").append("isDeleteEvent");
        }
        if (isDeleteSelfEvent()) {
            stringBuilder.append(",").append("isDeleteSelfEvent");
        }
        if (isDirEvent()) {
            stringBuilder.append(",").append("isDirEvent");
        }
        if (isDontFollowSymLink()) {
            stringBuilder.append(",").append("isDontFollowSymLink");
        }
        if (isIgnoredEvent()) {
            stringBuilder.append(",").append("isIgnoredEvent");
        }
        if (isModifyEvent()) {
            stringBuilder.append(",").append("isModifyEvent");
        }
        if (isMovedFromEvent()) {
            stringBuilder.append(",").append("isMovedFromEvent");
        }
        if (isMovedSelfEvent()) {
            stringBuilder.append(",").append("isMovedSelfEvent");
        }
        if (isMovedToEvent()) {
            stringBuilder.append(",").append("isMovedToEvent");
        }
        if (isOneShotEvent()) {
            stringBuilder.append(",").append("isOneShotEvent");
        }
        if (isOnlyWatchDir()) {
            stringBuilder.append(",").append("isOnlyWatchDir");
        }
        if (isOpenEvent()) {
            stringBuilder.append(",").append("isOpenEvent");
        }
        if (isQOverFlowEvent()) {
            stringBuilder.append(",").append("isQOverFlowEvent");
        }
        if (isUnmountEvent()) {
            stringBuilder.append(",").append("isUnmountEvent");
        }
        return stringBuilder.toString();

    }
}
