package org.limewire.ui.swing.sharing;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.Icon;
import javax.swing.JComponent;
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

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

public class GnutellaSharePanel extends JPanel {

    public static final String NAME = "GnutellaShare";
    
    private static final String TABLE = "TABLE";
    private static final String LIST = "LIST";
    
    private SharingHeaderPanel headerPanel;
    
    private ViewSelectionPanel viewSelectionPanel;
    
    private JPanel cardPanel;
    
    private JXTable table;
    
    private SharingFancyPanel sharingFancyPanel;
    
    private final FileList fileList;
    
    private final CardLayout cardLayout;

    @Resource
    private Icon cancelIcon;
    @Resource
    private Icon sharingIcon;
    
    
    MultiButtonTableCellRendererEditor editor;
    MultiButtonTableCellRendererEditor renderer;
    
    public GnutellaSharePanel(FileList fileList) {
        setLayout(new BorderLayout());
        
        GuiUtils.assignResources(this); 
        
        this.fileList = fileList;
        
        cardLayout = new CardLayout();
        cardPanel = new JPanel();
        cardPanel.setLayout(cardLayout);

        
        viewSelectionPanel = new ViewSelectionPanel(new ItemAction(cardPanel, cardLayout, LIST), 
                new ItemAction(cardPanel, cardLayout, TABLE));
        
        headerPanel = new SharingHeaderPanel(sharingIcon, "Sharing with the LimeWire Network", viewSelectionPanel);
        
        
        createCenterCards();


        add(headerPanel, BorderLayout.NORTH);
        add(cardPanel);
    }
    
    private void createCenterCards() {

        
        FilterList<FileItem> filteredList = new FilterList<FileItem>(fileList.getModel(), 
                new TextComponentMatcherEditor<FileItem>(headerPanel.getFilterBox(), new SharingTextFilterer()));
        
        table = new SharingTable(filteredList);
        table.setTransferHandler(new SharingTransferHandler(fileList));
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
        
        JScrollPane scrollPane = new JScrollPane();
        sharingFancyPanel = new SharingFancyPanel(filteredList, scrollPane, fileList);
        scrollPane.setViewportView(sharingFancyPanel);
        
        cardPanel.add(new JScrollPane(table),TABLE);
        cardPanel.add(scrollPane, LIST);
        cardLayout.show(cardPanel, LIST);
        
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

    private class ItemAction implements ItemListener {

        private final JComponent component;
        private final CardLayout cardLayout;
        private final String cardName;
        
        public ItemAction(JComponent component, CardLayout cardLayout, String cardName) {
            this.component = component;
            this.cardLayout = cardLayout;
            this.cardName = cardName;
        }
        
        @Override
        public void itemStateChanged(ItemEvent event) {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                cardLayout.show(component, cardName);
            }
        } 
        
    }
    
    private class SharingTextFilterer implements TextFilterator<FileItem> {
        @Override
        public void getFilterStrings(List<String> baseList, FileItem element) {
           baseList.add(element.getName());
        }
    }
}
