package org.limewire.ui.swing.library.navigator;

public class LibraryNavItem {

    private final String tabId;
    private final String displayedText;
    private final boolean canRemove;
    
    public LibraryNavItem(String id, String text, boolean canRemove) {
        this.tabId = id;
        this.displayedText = text;
        this.canRemove = canRemove;
    }
    
    public String getTabID() {
        return tabId;
    }
    
    public String getDisplayedText() {
        return displayedText;
    }
    
    public boolean canRemove() {
        return canRemove;
    }
}
