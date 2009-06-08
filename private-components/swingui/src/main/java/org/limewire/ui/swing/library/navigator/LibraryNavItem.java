package org.limewire.ui.swing.library.navigator;

public class LibraryNavItem {

    private final String tabId;
    private final String displayedText;
    
    public LibraryNavItem(String id, String text) {
        this.tabId = id;
        this.displayedText = text;
    }
    
    public String getTabID() {
        return tabId;
    }
    
    public String getDisplayedText() {
        return displayedText;
    }
}
