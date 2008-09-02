package org.limewire.ui.swing.library;

import java.awt.BorderLayout;

import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.library.FileList;
import org.limewire.ui.swing.library.table.LibraryTable;

public class BuddyLibrary extends JTable {

    private final String name;
    
    public final JXTable table;
    
    public BuddyLibrary(String name, FileList fileList) {
        this.name = name;
        
        setLayout(new BorderLayout());

        table = new LibraryTable(fileList.getModel()); 
        JScrollPane scrollPane = new JScrollPane(table);
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    public String getName() {
        return name;
    }
}
