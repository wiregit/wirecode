package org.limewire.ui.swing.library.manager;

import java.util.List;

public class RootLibraryManagerItem implements LibraryManagerItem {

    private List<LibraryManagerItem> children;
    
    public RootLibraryManagerItem(List<LibraryManagerItem> item) {
        this.children = item;
    }
    
    @Override
    public String displayName() {
        return "root";
    }

    @Override
    public List<LibraryManagerItem> getChildren() {
        return children;
    }

    @Override
    public boolean isScanned() {
        return false;
    }

    @Override
    public void setScanned(boolean value) {       
    }
}
