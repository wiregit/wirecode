package org.limewire.ui.swing.sharing;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.sharing.table.SharingTable;

import com.google.inject.Inject;

public class GnutellaSharePanel extends JPanel {

    public static final String NAME = "GnutellaShare";
    
    private final JXTable table;
    
    @Inject
    public GnutellaSharePanel(LibraryManager libraryManager) {
        setLayout(new BorderLayout());
        
        table = new SharingTable(libraryManager.getGnutellaList());

        add(new JScrollPane(table), BorderLayout.CENTER);
    }
}
