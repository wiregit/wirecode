package org.limewire.ui.swing.sharing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

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
        
        musicAction = new SharingAddAction(libraryManager.getLibraryList(), null, Category.AUDIO);
        videoAction = new SharingAddAction(libraryManager.getLibraryList(), null, Category.VIDEO);
        imageAction = new SharingAddAction(libraryManager.getLibraryList(), null, Category.IMAGE);
    }
    
    @Override
    protected void createComponents() {
        libraryButton = new JButton("Library");
        shareButton = new JButton("Share v");
        
        popup = new JPopupMenu();
        popup.add(new JMenuItem("All music in My Library"));
        popup.add(new JMenuItem("All videos in My Library"));
        popup.add(new JMenuItem("All images in My Library"));
        
        shareButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!popup.isVisible())
                    popup.show(shareButton, 0, shareButton.getHeight());
            }
        });
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
    
}
