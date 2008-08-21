package org.limewire.ui.swing.sharing.friends;

public class MockBuddyItem implements BuddyItem {

    private String name;
    private boolean isOnline;
    private int size;
    
    public MockBuddyItem(String name, boolean isOnline, int size) {
        this.name = name;
        this.isOnline = isOnline;
        this.size = size;
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isOnline() {
        return isOnline;
    }

    @Override
    public void setOnline(boolean value) {
        isOnline = value;
    }

    @Override
    public int size() {
        return size;
    }

}
