package org.limewire.ui.swing.sharing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.ui.swing.components.HeadingLabel;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.painter.ButtonPainter;
import org.limewire.ui.swing.painter.SubpanelPainter;
import org.limewire.ui.swing.sharing.actions.AddNewAudioAction;
import org.limewire.ui.swing.sharing.actions.AddNewImageAction;
import org.limewire.ui.swing.sharing.actions.AddNewVideoAction;
import org.limewire.ui.swing.sharing.actions.GoToLibraryAction;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

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
@Singleton
public class FriendSharingHeaderPanel extends JXPanel {
    
    private final String staticText = I18n.tr("Sharing with {0}", "");
    
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
    
    private final RemoteLibraryManager remoteLibraryManager;
    private final Navigator navigator;
    
    private HeadingLabel titleLabel;
    private JTextField filterBox;
    
    private LibraryButton libraryButton;
    private JXButton shareButton;
    
    private JCheckBoxMenuItem audioMenu;
    private JCheckBoxMenuItem videoMenu;
    private JCheckBoxMenuItem imageMenu;
    
    private AddNewAudioAction audioAction;
    private AddNewImageAction imageAction;
    private AddNewVideoAction videoAction;
    
    private JPopupMenu popup;
    
    @Inject
    public FriendSharingHeaderPanel(LibraryManager libraryManager, RemoteLibraryManager remoteLibraryManager, Navigator navigator, AddNewAudioAction audioAction, AddNewImageAction imageAction, AddNewVideoAction videoAction) {               
        GuiUtils.assignResources(this);
        
        this.remoteLibraryManager = remoteLibraryManager;
        this.navigator = navigator;
        
        this.audioAction = audioAction;
        this.imageAction = imageAction;
        this.videoAction = videoAction;
        
        setBackgroundPainter(new SubpanelPainter());

        createPopupMenu(libraryManager);
        createComponents();
        layoutComponents();       
        
        setMinimumSize(new Dimension(0, height + 2));
        setMaximumSize(new Dimension(Short.MAX_VALUE, height + 2));
        setPreferredSize(new Dimension(Short.MAX_VALUE, height + 2));
    }
    
    private void createComponents() {     
        titleLabel = new HeadingLabel(staticText);
        titleLabel.setForeground(fontColor);
        FontUtils.setSize(titleLabel, fontSize);
        FontUtils.changeStyle(titleLabel, Font.PLAIN);
        filterBox = new PromptTextField();
            
        libraryButton = new LibraryButton(new GoToLibraryAction(navigator, null));
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
        audioMenu = new JCheckBoxMenuItem(I18n.tr("All music in My Library"));
        audioMenu.addActionListener(audioAction);
        
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
    
    public void setFriendName(Friend friend) {
        titleLabel.setText(staticText + friend.getRenderName());
        libraryButton.setFriend(friend);
    }
    
    private class LibraryButton extends JXButton {
        
        private GoToLibraryAction action;
        private Friend friend;
        
        
        public LibraryButton(GoToLibraryAction action) {
            super(action);
            this.action = action;
            setFocusPainted(false);
        }
        
        public void setFriend(Friend friend) {
            this.friend = friend;
            action.setFriend(friend);
            setEnabled(remoteLibraryManager.hasFriendLibrary(friend));
            setTooltip();
            
            //TODO: add a presence change listener to friend, if they are added this button should
            //   get enabled, if they were there and leave this button should get disabled
        }
        
        private void setTooltip() {
            if(isEnabled()) {
                // {0}: name of the friend
                setToolTipText(I18n.tr("View the files {0} is sharing with you.",friend.getRenderName()));
            } else {
                setToolTipText(I18n.tr("{0} isn't logged in through LimeWire.",friend.getRenderName()));
            }
        }
    }
    
    private class PopupActionListener implements ActionListener {        
        @Override
        public void actionPerformed(ActionEvent e) {
            if(!popup.isVisible()) {
                audioMenu.setSelected(audioAction.isSelected());
                videoMenu.setSelected(videoAction.isSelected());
                imageMenu.setSelected(imageAction.isSelected());
                
                popup.show(shareButton, 0, shareButton.getHeight());
            }
        }
    }
}
