package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JLabel;
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
    @Resource
    private int height;

    private JLabel titleLabel;

    private PromptTextField filterField;

    private JXButton shareAllButton;

    private Category category;

    private Friend friend;
    

    /**
     * 
     * @param category
     * @param friend the friend whose library is being viewed.  Null for MyLibrary.
     */
    public LibraryHeaderPanel(Category category, Friend friend) {
        super(new MigLayout("insets 0, gap 0, aligny 50%", "[][]push[]", ""));
        
        GuiUtils.assignResources(this);
        
        this.category = category;
        this.friend = friend;
         
        String title;
        if(friend == null) 
            title = getTitle();
        else
            title = getSharingTitle();
        
        titleLabel = new HeadingLabel(title);
        titleLabel.setForeground(fontColor);
        titleLabel.setFont(headerFont);

        filterField = new PromptTextField();
        filterField.setPromptText(I18n.tr("Filter"));
        
        add(titleLabel, "gapx 10, gapafter 10");
        add(filterField, "cell 2 0, right, gapafter 10");        
        setBackgroundPainter(new SubpanelPainter());
        
        setMinimumSize(new Dimension(0, height + 2));
        setMaximumSize(new Dimension(getMaximumSize().width, height + 2));
        setPreferredSize(new Dimension(getMaximumSize().width, height + 2));
    }

    public JTextComponent getFilterTextField(){
        return filterField;
    }
    
    public void enableShareAll(final LibrarySharePanel sharePanel){        
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
                sharePanel.setBottomLabel(
                        I18n.tr("Sharing your {0} collection automatically shares new {0} files added to your Library", getCategoryString()));
                sharePanel.show(shareAllButton);
            }
        });
        add(shareAllButton, "cell 1 0");
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
    
    private String getCategoryString(){
        switch (category) {
        case AUDIO:
            return I18n.tr("audio");
        case DOCUMENT:
            return I18n.tr("document");
        case IMAGE:
            return I18n.tr("image");
        case OTHER:
            return I18n.tr("other");
        case PROGRAM:
            return I18n.tr("program");
        case VIDEO:
            return I18n.tr("video");
        }
        throw new IllegalStateException("Unknown category: " + category);
    }
    
    private String getSharingTitle() {
        String libraryString = I18n.tr("Sharing with {0}", friend.getRenderName());
        
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
    

    public void setCategory(Category category) {
        this.category = category;
        if(friend == null)
            titleLabel.setText(getTitle());
        else
            titleLabel.setText(getSharingTitle());
        
        displayCategory();
    }
    
    public void displayCategory() {
        if(shareAllButton == null)
            return;
        
        if(category == Category.AUDIO || category == Category.IMAGE || category == Category.VIDEO)
            shareAllButton.setVisible(true);
        else
            shareAllButton.setVisible(false);
    }

}
