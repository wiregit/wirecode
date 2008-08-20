package org.limewire.ui.swing.sharing;

import java.awt.CardLayout;

import javax.swing.Icon;
import javax.swing.JPanel;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LibraryManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class IndividualSharePanel extends GenericSharingPanel {
    public static final String NAME = "Individual Shared";
    
    @Resource
    protected Icon cancelIcon;
    @Resource
    protected Icon sharingIcon;
    
    private CardLayout overviewCardLayout;
    
    private JPanel nonEmptyPanel;
    
//    private final Map<String, EventList<FileItem>> uniqueLists;
    
    @Inject
    public IndividualSharePanel(LibraryManager libraryManager, SharingSomeBuddyEmptyPanel emptyPanel) {
//        uniqueLists = libraryManager.getUniqueLists();
        
        overviewCardLayout = new CardLayout();
        this.setLayout(overviewCardLayout);
        
        nonEmptyPanel = new JPanel();
        
        add(emptyPanel, EMPTY);
        add(nonEmptyPanel, NONEMPTY);
        overviewCardLayout.show(this,EMPTY);
    }
}
