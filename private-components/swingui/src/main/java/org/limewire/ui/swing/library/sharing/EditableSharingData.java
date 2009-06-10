package org.limewire.ui.swing.library.sharing;

public class EditableSharingData {

    private final String name;
    private boolean isSelected;
    
    public EditableSharingData(String name, boolean isSelected) {
        this.name = name;
        this.isSelected = isSelected;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isSelected() {
        return isSelected;
    }
    
    public void setIsSelected(boolean value) {
        this.isSelected = value;
    }
}
