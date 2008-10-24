package org.limewire.ui.swing.library;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.components.HeadingLabel;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.library.sharing.CategoryShareModel;
import org.limewire.ui.swing.library.sharing.LibrarySharePanel;
import org.limewire.ui.swing.painter.ButtonPainter;
import org.limewire.ui.swing.painter.SubpanelPainter;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class LibraryHeaderPanel extends JXPanel {
    
    @Resource
    private Color fontColor;
    
    @Resource
    private Icon shareIcon;
    @Resource
    private Font headerFont;
    @Resource
    private Font buttonFont;

    private JLabel titleLabel;

    private PromptTextField filterField;

    private JXButton shareAllButton;

    private Category category;

    private Friend friend;
    
    private JPopupMenu shareAllPopup;

    /**
     * 
     * @param category
     * @param friend the friend whose library is being viewed.  Null for MyLibrary.
     */
    public LibraryHeaderPanel(Category category, Friend friend) {
        super(new MigLayout());
        
        GuiUtils.assignResources(this);
        
        this.category = category;
        this.friend = friend;
         
        
        titleLabel = new HeadingLabel(getTitle());
        titleLabel.setForeground(fontColor);
        titleLabel.setFont(headerFont);

        filterField = new PromptTextField();
        filterField.setPromptText(I18n.tr("Filter"));
        
        add(titleLabel);
        add(filterField, "pushx, right");        
        setBackgroundPainter(new SubpanelPainter());
    }

    public JTextComponent getFilterTextField(){
        return filterField;
    }
    
    public void enableShareAll(final LibrarySharePanel sharePanel){
        
        shareAllPopup = new JPopupMenu();
        shareAllPopup.setLayout(new BorderLayout());
        shareAllPopup.add(sharePanel);
        
        sharePanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (shareAllPopup.isVisible()) {
                    //TODO: better way of doing this.  Popup flashes.
                    shareAllPopup.pack();
                }
            }
        });
        
        shareAllButton = new JXButton(I18n.tr("Share All"), shareIcon);
        shareAllButton.setForeground(fontColor);
        shareAllButton.setHorizontalTextPosition(SwingConstants.LEFT);
        shareAllButton.setBackgroundPainter(new ButtonPainter());
        shareAllButton.setBorderPainted(false);
        shareAllButton.setFont(buttonFont);
        shareAllButton.setOpaque(false);
        shareAllButton.setFocusPainted(false);
        shareAllButton.setContentAreaFilled(false);
        shareAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ((CategoryShareModel)sharePanel.getShareModel()).setCategory(category);
                sharePanel.adjustSize();
                shareAllPopup.show(shareAllButton, 0, 0);
            }
        });
        add(shareAllButton);
    }
    
    private String getTitle() {
        String libraryString = (friend == null) ? I18n.tr("My Library") : I18n.tr("{0}'s Library", friend.getRenderName());
        
        switch (category) {
        case AUDIO:
            return I18n.tr("{0} - Audio", libraryString);
        case DOCUMENT:
            return I18n.tr("{0} - Documents", libraryString);
        case IMAGE:
            return I18n.tr("{0} - Images", libraryString);
        case OTHER:
            return I18n.tr("{0} - Other", libraryString);
        case PROGRAM:
            return I18n.tr("{0} - Programs", libraryString);
        case VIDEO:
            return I18n.tr("{0} - Video", libraryString);
        }
        throw new IllegalArgumentException("Unknown category: " + category);
    }
    
}
