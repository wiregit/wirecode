package org.limewire.ui.swing.sharing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.FileItem.Category;
import org.limewire.ui.swing.sharing.actions.SharingAddAction;

import net.miginfocom.swing.MigLayout;

public class BuddySharingHeaderPanel extends SharingHeaderPanel {
    
    private JButton libraryButton;
    private JButton shareButton;
    
    private SharingAddAction musicAction;
    private SharingAddAction videoAction;
    private SharingAddAction imageAction;
    
    private JPopupMenu popup;
    
    public BuddySharingHeaderPanel(Icon icon, String staticText, String name,
            ViewSelectionPanel viewPanel, LibraryManager libraryManager) {
        super(icon, staticText, name, viewPanel);
                
        createMenu(libraryManager);
    }
    
    private void createMenu(LibraryManager libraryManager) {
        musicAction = new SharingAddAction(libraryManager.getLibraryList(), Category.AUDIO);
        videoAction = new SharingAddAction(libraryManager.getLibraryList(), Category.VIDEO);
        imageAction = new SharingAddAction(libraryManager.getLibraryList(), Category.IMAGE);
        
        JMenuItem audioMenu = new JMenuItem("All music in My Library");
        audioMenu.addActionListener(musicAction);
        
        JMenuItem videoMenu = new JMenuItem("All videos in My Library");
        videoMenu.addActionListener(videoAction);
        
        JMenuItem imageMenu = new JMenuItem("All images in My Library");
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
        libraryButton = new JButton("Library");
        libraryButton.setEnabled(false);
        shareButton = new JButton("Share v");       
        shareButton.setVisible(false);
    }
    
    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);
        if(value) {
            shareButton.setVisible(true);
        } else {
            shareButton.setVisible(false);
        }
            
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
}
