package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class RootLibraryManagerItem implements LibraryManagerItem {

    private final List<LibraryManagerItem> children;
    
    public RootLibraryManagerItem() {
        this.children = new ArrayList<LibraryManagerItem>();
    }
    
    @Override
    public LibraryManagerItem getParent() {
        return null;
    }
    
    @Override
    public File getFile() {
        return null;
    }
    
    @Override
    public String displayName() {
        return "root";
    }

    @Override
    public List<LibraryManagerItem> getChildren() {
        return children;
    }
    
    public int addChild(LibraryManagerItem item) {
        children.add(item);
        assert item.getParent() == this;
        return children.size() - 1;
    }

    public int removeChild(LibraryManagerItem item) {
        int idx = children.indexOf(item);
        assert idx != -1;
        children.remove(idx);
        return idx;
    }
    
    @Override
    public Collection<? extends File> getExcludedChildren() {
        return Collections.emptyList();
    }
    
    @Override
    public LibraryManagerItem getChildFor(File directory) {
        for(LibraryManagerItem child : children) {
            if(child.getFile().equals(directory)) {
                return child;
            }
        }
        return null;
    }

}
