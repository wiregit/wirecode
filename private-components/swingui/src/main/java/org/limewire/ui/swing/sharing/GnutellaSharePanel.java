package org.limewire.ui.swing.sharing;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.sharing.table.SharingTable;
import org.limewire.ui.swing.sharing.table.SharingTableModel;
import org.limewire.ui.swing.table.MultiButtonTableCellRendererEditor;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

public class GnutellaSharePanel extends JPanel {

    public static final String NAME = "GnutellaShare";
    
    private SharingHeaderPanel headerPanel;
    
    private final JXTable table;
    
    private final LibraryManager libraryManager;

    @Resource
    private Icon cancelIcon;
    @Resource
    private Icon sharingIcon;
    
    
    MultiButtonTableCellRendererEditor editor;
    MultiButtonTableCellRendererEditor renderer;
    
    public GnutellaSharePanel(LibraryManager libraryManager) {
        setLayout(new BorderLayout());
        
        GuiUtils.assignResources(this); 
        
        this.libraryManager = libraryManager;
               
        headerPanel = new SharingHeaderPanel(sharingIcon, "Sharing with the LimeWire Network");
        
        
        FilterList<FileItem> filteredList = new FilterList<FileItem>(libraryManager.getGnutellaList(), 
                new TextComponentMatcherEditor(headerPanel.getFilterBox(), new SharingTextFilterer()));
        
        table = new SharingTable(filteredList);
        table.setTransferHandler(new SharingTransferHandler(libraryManager));
        table.setDropMode(DropMode.ON);
        
        editor = new MultiButtonTableCellRendererEditor(20);
        editor.addActions(createActions(editor));
        renderer = new MultiButtonTableCellRendererEditor(20);
        renderer.addActions(createActions(renderer));
        
        //TODO: this needs to be fixed, if rows are columns or rows
        //  are removed this stops working
        TableColumn tc = table.getColumn(6);
        tc.setCellEditor(editor);
        tc.setCellRenderer(renderer);

        add(headerPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }
    
    private List<Action> createActions(TableCellEditor editor) {
        List<Action> list = new ArrayList<Action>();
        list.add(new MyAction(editor, libraryManager, table, cancelIcon ));
        return list;
    }
    
    private class MyAction extends AbstractAction {

        private TableCellEditor editor;
        private LibraryManager libraryManager;
        private JXTable table;
        
        public MyAction(TableCellEditor editor, LibraryManager libraryManager, JXTable table, Icon icon) {
            super("", icon);
            
            this.editor = editor;
            this.libraryManager = libraryManager;
            this.table = table;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            editor.cancelCellEditing();

            SharingTableModel model = (SharingTableModel) table.getModel();
            FileItem item = model.getFileItem(table.getSelectedRow());
            libraryManager.removeGnutellaFile(item.getFile());
        }
        
    }

    private class SharingTextFilterer implements TextFilterator<FileItem> {
        @Override
        public void getFilterStrings(List<String> baseList, FileItem element) {
           baseList.add(element.getName());
        }
    }
}
