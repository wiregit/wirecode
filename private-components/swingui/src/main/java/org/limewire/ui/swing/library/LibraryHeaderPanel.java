package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.Font;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.components.LimeHeadingLabel;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class LibraryHeaderPanel extends JXPanel {
    
    @Resource
    private Color fontColor;
    @Resource
    private Font headerFont;
//    @Resource
//    private Font buttonFont;
//    @Resource
//    private int height;

    private JLabel titleLabel;

    private PromptTextField filterField;

    private Friend friend;
    
    private JButton shareButton;
    
    /**
     * 
     * @param category
     * @param friend the friend whose library is being viewed.  Null for MyLibrary.
     */
    public LibraryHeaderPanel(Friend friend, boolean isLibraryHeader) {
        super(new MigLayout("insets 0, gap 0, aligny 50%", "[][]push[]", ""));
        
        GuiUtils.assignResources(this);
        
        this.friend = friend;
         
        String title;
        if(isLibraryHeader) 
            title = getTitle();
        else
            title = getSharingTitle();
        
        titleLabel = new LimeHeadingLabel(title);
        titleLabel.setForeground(fontColor);
        titleLabel.setFont(headerFont);

        filterField = new PromptTextField();
        filterField.setPromptText(I18n.tr("Filter"));
        
        add(titleLabel, "gapx 10, gapafter 10");
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
