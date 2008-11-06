package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.library.LibraryData;

public class LibraryManagerItemImpl implements LibraryManagerItem {

    private final File file;
    private final LibraryData libraryData;
    
    private boolean isScanned;
    private List<LibraryManagerItem> children;
    
    public LibraryManagerItemImpl(LibraryData libraryData, File file, boolean isScanned) {
        this.libraryData = libraryData;
        this.file = file;
        this.isScanned = isScanned;
    }
    
    @Override
    public String displayName() {
        if(file.getName() == null || file.getName().length() == 0) {
            return file.toString();
        }
        return file.getName();
    }

    @Override
    public List<LibraryManagerItem> getChildren() {
        if(children == null) {
            children = new ArrayList<LibraryManagerItem>();
            File[] files = file.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return libraryData.isDirectoryAllowed(file);
                }
            });
            if(files != null) {
                for(File file : files) {
                    children.add(new LibraryManagerItemImpl(libraryData, file, !libraryData.isDirectoryExcluded(file)));
                }
            }
        }
        
        return children;
    }

    @Override
    public boolean isScanned() {
        return isScanned;
    }

    @Override
    public void setScanned(boolean value) {
        isScanned = value;
        setScanChildren(value);
    }
    
    private void setScanChildren(boolean value) {
        for(LibraryManagerItem item : getChildren()) {
            item.setScanned(value);
        }
    }
}
