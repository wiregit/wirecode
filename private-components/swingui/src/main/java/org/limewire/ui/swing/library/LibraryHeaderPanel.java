package org.limewire.ui.swing.library;

import javax.swing.Action;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.components.LimeHeaderBar;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.I18n;

public class LibraryHeaderPanel extends LimeHeaderBar {
    private PromptTextField filterField;

    private Friend friend;
    
    private JXButton shareButton;
    
    /**
     * 
     * @param category
     * @param friend the friend whose library is being viewed.  Null for MyLibrary.
     */
    public LibraryHeaderPanel(Friend friend, boolean isLibraryHeader) {
        this.friend = friend;
        
        String title;
        if(isLibraryHeader) 
            title = getTitle();
        else
            title = getSharingTitle();
        
        setLayout(new MigLayout("insets 0, gap 0, fillx, filly", "[]push[]", ""));
        setText(title);
        
        filterField = new PromptTextField();
        filterField.setPromptText(I18n.tr("Filter"));
        
        add(filterField, "cell 1 0, right, gapafter 10");     
    }

    public JTextComponent getFilterTextField(){
        return filterField;
    }
    
    public void enableButton(Action action, ButtonDecorator buttonDecorator) {
        shareButton = new JXButton(action);
        buttonDecorator.decorateDarkFullButton(shareButton);
                
        add(shareButton, "cell 0 0, left");
    }
    
    private String getTitle() {
        return (friend == null) ? I18n.tr("My Library") : I18n.tr("{0}'s Library", friend.getRenderName());
    }
    
    private String getSharingTitle() {
        return I18n.tr("Sharing with {0}", friend.getRenderName());
    }
}
