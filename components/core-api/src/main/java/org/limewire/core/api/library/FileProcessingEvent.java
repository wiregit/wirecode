package org.limewire.core.api.library;

import java.io.File;

public class FileProcessingEvent {

    public static enum Type {
        FINISHED, FILE_PROCESSED
    }

    private final File file;

    private final int index;

    private final int size;

    private final Type type;

    public FileProcessingEvent(Type type, File file, int index, int size) {
        this.type = type;
        this.file = file;
        this.index = index;
        this.size = size;
    }

    public int getIndex() {
        return index;
    }

    public int getSize() {
        return size;
    }

    public File getFile() {
        return file;
    }

    public Type getType() {
        return type;
    }
}
