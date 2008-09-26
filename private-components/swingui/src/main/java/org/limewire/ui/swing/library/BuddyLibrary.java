package org.limewire.ui.swing.library;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;
import org.limewire.ui.swing.library.table.LibraryTable;

public class BuddyLibrary<T extends FileItem> extends JPanel {
   
    public final JXTable table;
    
    public BuddyLibrary(FileList<T> fileList) {
        setLayout(new BorderLayout());

        table = new LibraryTable<T>(fileList.getModel()); 
        JScrollPane scrollPane = new JScrollPane(table);
        
        add(scrollPane, BorderLayout.CENTER);
    }
}
