package org.limewire.ui.swing.library.navigator;


public class LibraryNavItem {

    public static enum NavType {
        LIBRARY, PUBLIC_SHARED, LIST
    }
    
    private final String tabId;
    private final String displayedText;
    private final NavType navType;
    //TODO: add LocalFileList?? 
    
    public LibraryNavItem(String id, String text, NavType type) {
        this.tabId = id;
        this.displayedText = text;
        this.navType = type;
    }
    
    public String getTabID() {
        return tabId;
    }
    
    public String getDisplayedText() {
        return displayedText;
    }
    
    public boolean canRemove() {
        return navType != NavType.LIBRARY && navType != NavType.PUBLIC_SHARED;
    }
    
    public NavType getType() {
        return navType;
    }
}
