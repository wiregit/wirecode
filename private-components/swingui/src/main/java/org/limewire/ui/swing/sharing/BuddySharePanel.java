package org.limewire.ui.swing.sharing;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.sharing.table.SharingTable;

import com.google.inject.Inject;

public class BuddySharePanel extends JPanel {
    public static final String NAME = "Buddy Share";
    
    private final JXTable table;
    
    @Inject
    public BuddySharePanel(LibraryManager libraryManager) {
        setLayout(new BorderLayout());
        
        table = new SharingTable(libraryManager.getAllBuddyList());
        add(new JScrollPane(table), BorderLayout.CENTER);
    }
}
