package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LibraryManagerItemImpl implements LibraryManagerItem {

    private File file;
    private boolean isScanned;
    private List<LibraryManagerItem> children;
    
    public LibraryManagerItemImpl(File file, boolean isScanned) {
        this.file = file;
        this.isScanned = isScanned;
    }
    
    @Override
    public String displayName() {
        if(file.getName() == null || file.getName().length() == 0)
            return file.toString();
        return file.getName();
    }

    @Override
    public List<LibraryManagerItem> getChildren() {
        if(children == null && file.isDirectory()) {
            File[] files = file.listFiles();
            children = new ArrayList<LibraryManagerItem>();
            for(File file : files) {
                // only show children that are folders
                if(file.isDirectory())
                    children.add(new LibraryManagerItemImpl(file, isScanned));
            }
        } else if(children == null)
            children = new ArrayList<LibraryManagerItem>();
        return children;
    }

    @Override
    public boolean isScanned() {
        return isScanned;
    }

    @Override
    public void setScanned(boolean value) {
        if(value != isScanned) {
            isScanned = value;
            setScanChildren(value);
        }
    }
    
    private void setScanChildren(boolean value) {
        for(LibraryManagerItem item : getChildren()) {
            item.setScanned(value);
        }
    }
}
