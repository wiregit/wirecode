package org.limewire.ui.swing.sharing.friends;

public class MockBuddyItem implements BuddyItem {

    private String name;
    private int size;
    
    public MockBuddyItem(String name, int size) {
        this.name = name;
        this.size = size;
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public int size() {
        return size;
    }

}
