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
import org.limewire.core.api.library.FileList;
import org.limewire.ui.swing.sharing.table.SharingTable;
import org.limewire.ui.swing.sharing.table.SharingTableModel;
import org.limewire.ui.swing.table.MultiButtonTableCellRendererEditor;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

public class BuddySharePanel extends JPanel {
    public static final String NAME = "Buddy Share";
    
    private final SharingHeaderPanel headerPanel;
    
    private final JXTable table;
    
    private final FileList fileList;
    
    MultiButtonTableCellRendererEditor editor;
    MultiButtonTableCellRendererEditor renderer;
    
    @Resource
    private Icon cancelIcon;
    @Resource
    private Icon sharingIcon;
    
    @Inject
    public BuddySharePanel(FileList fileList) {
        this.fileList = fileList;
        
        setLayout(new BorderLayout());
        GuiUtils.assignResources(this); 
        
        headerPanel = new SharingHeaderPanel(sharingIcon, "Sharing with All Friends",  new ViewSelectionPanel(null, null));
        
        table = new SharingTable(fileList.getModel());
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
        list.add(new MyAction(editor, fileList, table, cancelIcon ));
        return list;
    }
    
    private class MyAction extends AbstractAction {

        private TableCellEditor editor;
        private FileList fileList;
        private JXTable table;
        
        public MyAction(TableCellEditor editor, FileList fileList, JXTable table, Icon icon) {
            super("", icon);
            
            this.editor = editor;
            this.fileList = fileList;
            this.table = table;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            editor.cancelCellEditing();

            SharingTableModel model = (SharingTableModel) table.getModel();
            FileItem item = model.getFileItem(table.getSelectedRow());
            fileList.removeFile(item.getFile());
        }
        
    }
}
