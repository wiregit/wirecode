package org.limewire.ui.swing.sharing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.painter.ButtonPainter;
import org.limewire.ui.swing.sharing.actions.SharingAddAction;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class FriendSharingHeaderPanel extends SharingHeaderPanel {
    
    @Resource
    private Icon downIcon;
    @Resource
    private int buttonHeight;
    @Resource
    private int buttonWidth;
    
    private LibraryButton libraryButton;
    private JXButton shareButton;
    
    private final SharingAddAction musicAction;
    private final SharingAddAction videoAction;
    private final SharingAddAction imageAction;
    
    private final JPopupMenu popup;
    
    public FriendSharingHeaderPanel(Icon icon, String staticText, String name,
            ViewSelectionPanel viewPanel, LibraryManager libraryManager) {
        super(icon, staticText, name, viewPanel);
                
        GuiUtils.assignResources(this);
        
        musicAction = new SharingAddAction(libraryManager.getLibraryManagedList(), Category.AUDIO);
        videoAction = new SharingAddAction(libraryManager.getLibraryManagedList(), Category.VIDEO);
        imageAction = new SharingAddAction(libraryManager.getLibraryManagedList(), Category.IMAGE);
        
        JMenuItem audioMenu = new JMenuItem(I18n.tr("All music in My Library"));
        audioMenu.addActionListener(musicAction);
        
        JMenuItem videoMenu = new JMenuItem(I18n.tr("All videos in My Library"));
        videoMenu.addActionListener(videoAction);
        
        JMenuItem imageMenu = new JMenuItem(I18n.tr("All images in My Library"));
        imageMenu.addActionListener(imageAction);
        
        popup = new JPopupMenu();
        popup.add(audioMenu);
        popup.add(videoMenu);
        popup.add(imageMenu);
        
        shareButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!popup.isVisible())
                    popup.show(shareButton, 0, shareButton.getHeight());
            }
        });
    }
    
    @Override
    protected void createComponents() {
        libraryButton = new LibraryButton(I18n.tr("Library"));
        libraryButton.setPreferredSize(new Dimension(buttonWidth, buttonHeight));
//        libraryButton.setEnabled(false);
        libraryButton.setForeground(Color.WHITE);
        libraryButton.setBackgroundPainter(new ButtonPainter());
        
        shareButton = new JXButton(I18n.tr("Share"), downIcon);       
        shareButton.setVisible(false);
        shareButton.setHorizontalTextPosition(SwingConstants.LEFT);
        shareButton.setBackgroundPainter(new ButtonPainter());
    }
    
    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);
        shareButton.setEnabled(value);
    }
    
    @Override
    protected void layoutComponents() {
        setLayout(new MigLayout());

        add(descriptionLabel);
        add(libraryButton);
        add(shareButton,"push");
        
        add(filterBox);
        add(viewSelectionPanel);
    }
    
    public void setModel(LocalFileList fileList) {
        musicAction.setUserFileList(fileList);
        videoAction.setUserFileList(fileList);
        imageAction.setUserFileList(fileList);
    }
    
    @Override
    public void setFriendName(String name) {
        super.setFriendName(name);
        libraryButton.setFriend(name);
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
}
