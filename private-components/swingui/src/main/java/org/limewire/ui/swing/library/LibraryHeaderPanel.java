package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.Font;

import javax.swing.Action;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.painter.TextShadowPainter;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class LibraryHeaderPanel extends JXPanel {
    private JXLabel titleLabel;

    private PromptTextField filterField;

    private Friend friend;
    
    private JXButton shareButton;
    
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
        
        titleLabel = new JXLabel(title);
        titleLabel.setForegroundPainter(new TextShadowPainter());

        filterField = new PromptTextField();
        filterField.setPromptText(I18n.tr("Filter"));
        
        add(titleLabel, "gapx 10, gapafter 10");
        add(filterField, "cell 2 0, right, gapafter 10");     
    }

    public JTextComponent getFilterTextField(){
        return filterField;
    }
    
    public void enableButton(Action action, ButtonDecorator buttonDecorator) {
        shareButton = new JXButton(action);
        buttonDecorator.decorateDarkFullButton(shareButton);
                
        add(shareButton, "cell 1 0");
    }
    
    private String getTitle() {
        return (friend == null) ? I18n.tr("My Library") : I18n.tr("{0}'s Library", friend.getRenderName());
    }
    
    private String getSharingTitle() {
        return I18n.tr("Sharing with {0}", friend.getRenderName());
    }
    
    @Override
    public void setFont(Font font) {
        if (titleLabel == null) {
            super.setFont(font);
        }
        else {            
            titleLabel.setFont(font);
        }
    }
    
    @Override
    public void setForeground(Color fg) {
        if (titleLabel == null) {
            super.setForeground(fg);
        }
        else {            
            titleLabel.setForeground(fg);
        }
    }
}
