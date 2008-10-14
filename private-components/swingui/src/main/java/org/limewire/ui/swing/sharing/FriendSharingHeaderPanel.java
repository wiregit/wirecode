package org.limewire.ui.swing.sharing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;

import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.components.HeadingLabel;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.painter.ButtonPainter;
import org.limewire.ui.swing.painter.SubpanelPainter;
import org.limewire.ui.swing.sharing.actions.SharingAddAction;
import org.limewire.ui.swing.sharing.friends.FriendUpdate;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;

/**
 * Header for the Friend Sharing Panel. Displays the friend's name, 
 * a filter box, a library button and a sharing button.
 * 
 * The library button is enabled and directs you to their library
 * if they are signed on through LW. 
 * 
 * The Sharing button displays a list of sharing actions to share
 * file types with them.
 */
public class FriendSharingHeaderPanel extends JXPanel implements FriendUpdate {
    
    private final String staticText;
    
    @Resource
    private Icon downIcon;
    @Resource
    private int buttonHeight;
    @Resource
    private int buttonWidth;
    @Resource
    protected int height;
    @Resource
    private Color fontColor;
    @Resource 
    private int fontSize;
    
    private HeadingLabel titleLabel;
    private JTextField filterBox;
    
    private LibraryButton libraryButton;
    private JXButton shareButton;
    
    private JCheckBoxMenuItem audioMenu;
    private JCheckBoxMenuItem videoMenu;
    private JCheckBoxMenuItem imageMenu;
    
    private SharingAddAction musicAction;
    private SharingAddAction videoAction;
    private SharingAddAction imageAction;
    
    private JPopupMenu popup;
    
    public FriendSharingHeaderPanel(String staticText, String name, LibraryManager libraryManager) {               
        GuiUtils.assignResources(this);
        
        setBackgroundPainter(new SubpanelPainter());
       
        this.staticText = staticText;

        createPopupMenu(libraryManager);
        createComponents(MessageFormat.format(staticText, name));
        layoutComponents();       
        
        setMinimumSize(new Dimension(0, height + 2));
        setMaximumSize(new Dimension(Short.MAX_VALUE, height + 2));
        setPreferredSize(new Dimension(Short.MAX_VALUE, height + 2));
    }
    
    private void createComponents(String text) {     
        titleLabel = new HeadingLabel(text);
        titleLabel.setForeground(fontColor);
        FontUtils.setSize(titleLabel, fontSize);
        FontUtils.changeStyle(titleLabel, Font.PLAIN);
        filterBox = new PromptTextField();
            
        libraryButton = new LibraryButton(I18n.tr("Library"));
        libraryButton.setPreferredSize(new Dimension(buttonWidth, buttonHeight));

        libraryButton.setForeground(fontColor);
        libraryButton.setBackgroundPainter(new ButtonPainter());
        
        shareButton = new JXButton(I18n.tr("Share"), downIcon);       
        shareButton.setVisible(false);
        shareButton.setForeground(fontColor);
        shareButton.setHorizontalTextPosition(SwingConstants.LEFT);
        shareButton.setBackgroundPainter(new ButtonPainter());
        shareButton.addActionListener(new PopupActionListener());
    }
    
    private void createPopupMenu(LibraryManager libraryManager) {
        musicAction = new SharingAddAction(libraryManager.getLibraryManagedList(), Category.AUDIO);
        videoAction = new SharingAddAction(libraryManager.getLibraryManagedList(), Category.VIDEO);
        imageAction = new SharingAddAction(libraryManager.getLibraryManagedList(), Category.IMAGE);
        
        audioMenu = new JCheckBoxMenuItem(I18n.tr("All music in My Library"));
        audioMenu.addActionListener(musicAction);
        
        videoMenu = new JCheckBoxMenuItem(I18n.tr("All videos in My Library"));
        videoMenu.addActionListener(videoAction);
        
        imageMenu = new JCheckBoxMenuItem(I18n.tr("All images in My Library"));
        imageMenu.addActionListener(imageAction);
        
        popup = new JPopupMenu();
        popup.add(audioMenu);
        popup.add(videoMenu);
        popup.add(imageMenu);
    }
    
    public JTextField getFilterBox() {
        return filterBox;
    }
    
    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);
        shareButton.setVisible(value);
    }
    
    protected void layoutComponents() {
        setLayout(new MigLayout("insets 0 0 0 0", "", "align 50%"));

        add(titleLabel, "gapx 10");
        add(libraryButton);
        add(shareButton,"push");
        
        add(filterBox, "gapafter 10");
    }
    
    public void setModel(FriendFileList fileList) {
        musicAction.setUserFileList(fileList);
        videoAction.setUserFileList(fileList);
        imageAction.setUserFileList(fileList);
    }
    
    @Override
    public void setFriendName(String name) {
        titleLabel.setText(MessageFormat.format(staticText, name));
        libraryButton.setFriend(name);
    }

    @Override
    public void setEventList(EventList<LocalFileItem> model) {
    }
    
    private class LibraryButton extends JXButton {
        private String friend;
        
        public LibraryButton(String text) {
            super(text);
            setFocusPainted(false);
        }
        
        @Override
        public void setEnabled(boolean value) {
            super.setEnabled(value);
            setTooltip();
        }
        
        public void setFriend(String friend) {
            this.friend = friend;
            setTooltip();
        }
        
        private void setTooltip() {
            if(isEnabled()) {
                // {0}: name of the friend
                setToolTipText(I18n.tr("View the files {0} is sharing with you.",friend));
            } else {
                setToolTipText(I18n.tr("{0} isn't logged in through LimeWire.",friend));
            }
        }
    }
    
    private class PopupActionListener implements ActionListener {        
        @Override
        public void actionPerformed(ActionEvent e) {
            if(!popup.isVisible()) {
                //set the checkboxes to their selection value
                audioMenu.setSelected(musicAction.getUserFileList().isAddNewAudioAlways());
                videoMenu.setSelected(videoAction.getUserFileList().isAddNewVideoAlways());
                imageMenu.setSelected(imageAction.getUserFileList().isAddNewImageAlways());
                
                popup.show(shareButton, 0, shareButton.getHeight());
            }
        }
    }
}
