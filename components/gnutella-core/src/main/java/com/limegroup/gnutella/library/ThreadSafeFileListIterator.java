package com.limegroup.gnutella.library;

import java.util.Iterator;
import java.util.NoSuchElementException;

class ThreadSafeFileListIterator implements Iterator<FileDesc> {
    
    // TODO: Fail on revision changes in FileManager
    
    private final FileListImpl fileList;
    
    /** Points to the index that is to be examined next. */
    private int index = 0;
    private FileDesc preview;
    
    public ThreadSafeFileListIterator(FileListImpl fileList) {
        this.fileList = fileList;
        this.index = fileList.getMinIndex();
    }
    
    private boolean preview() {
        assert preview == null;
        
//        if (_revision != startRevision) {
//            return false;
//        }

        synchronized (fileList) {
            while (index <= fileList.getMaxIndex()) {
                preview = fileList.getFileDescForIndex(index);
                index++;
                if (preview != null) {
                    return true;
                }
            }
            
            return false;
        }
    }
    
    @Override
    public boolean hasNext() {
//        if (_revision != startRevision) {
//            return false;
//        }

        if (preview != null) {
            if (!fileList.contains(preview)) {
                // file was removed in the meantime
                preview = null;
            }
        }
        return preview != null || preview();
    }
    
    @Override
    public FileDesc next() {
        if (hasNext()) {
            FileDesc item = preview;
            preview = null;
            return item;
        }
        throw new NoSuchElementException();     
    }
    
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
