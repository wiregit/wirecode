package org.limewire.ui.swing.library.navigator;

import org.limewire.core.api.library.LocalFileList;


public class LibraryNavItem {

    public static enum NavType {
        LIBRARY, PUBLIC_SHARED, LIST
    }
    
    private String tabId;
    private String displayedText;
    private final NavType navType;
    private final LocalFileList localFileList;
    
    public LibraryNavItem(String id, String text, LocalFileList localFileList, NavType type) {
        this.tabId = id;
        this.displayedText = text;
        this.navType = type;
        this.localFileList = localFileList;
    }
    
    public LocalFileList getLocalFileList() {
        return localFileList;
    }
    
    public String getTabID() {
        return tabId;
    }
    
    public String getDisplayedText() {
        return displayedText;
    }
    
    public void setText(String text) {
        this.tabId = text;
        this.displayedText = text;
    }
    
    public boolean canRemove() {
        return navType != NavType.LIBRARY && navType != NavType.PUBLIC_SHARED;
    }
    
    public NavType getType() {
        return navType;
    }
}
