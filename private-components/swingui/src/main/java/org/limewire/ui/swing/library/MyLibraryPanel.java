/**
 * 
 */
package org.limewire.ui.swing.library;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.library.sharing.LibrarySharePanel;
import org.limewire.ui.swing.library.table.LibraryTable;

import com.google.inject.Inject;

/**
 *
 */
public class MyLibraryPanel extends JPanel {
    public static final String NAME = "My Library";
    public final LibraryTable table;
    
    @Inject
    public MyLibraryPanel(LibraryManager libraryManager, LibrarySharePanel sharePanel){
        setLayout(new BorderLayout());

        table = new LibraryTable<LocalFileItem>(libraryManager.getLibraryList().getModel()); 
        table.enableSharing(sharePanel);
        
        JScrollPane scrollPane = new JScrollPane(table);
        
        add(scrollPane, BorderLayout.CENTER);
    }
}
