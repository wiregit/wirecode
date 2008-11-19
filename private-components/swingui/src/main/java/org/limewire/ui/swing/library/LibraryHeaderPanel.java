package org.limewire.ui.swing.library;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.text.JTextComponent;

import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.components.LimeHeaderBar;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class LibraryHeaderPanel extends LimeHeaderBar {
    
     private PromptTextField filterField;

    private Friend friend;
    
    private JButton shareButton;
    
    /**
     * 
     * @param category
     * @param friend the friend whose library is being viewed.  Null for MyLibrary.
     */
    public LibraryHeaderPanel(Friend friend, boolean isLibraryHeader) {
        super();
        
        GuiUtils.assignResources(this);
        
        this.friend = friend;
        
        this.setText(isLibraryHeader ? this.getTitle() : this.getSharingTitle());
        
        filterField = new PromptTextField();
        filterField.setPromptText(I18n.tr("Filter"));
        
        add(filterField, "cell 2 0, right, gapafter 10");     
    }

    public JTextComponent getFilterTextField(){
        return filterField;
    }
    
    public void enableButton(Action action) {
        shareButton = new JXButton(action);
        shareButton.setBorderPainted(false);
        shareButton.setFocusPainted(false);
        
        add(shareButton, "cell 1 0");
    }
    
    private String getTitle() {
        return (friend == null) ? I18n.tr("My Library") : I18n.tr("{0}'s Library", friend.getRenderName());
    }
    
    private String getSharingTitle() {
        return I18n.tr("Sharing with {0}", friend.getRenderName());
    }
}
