package org.limewire.ui.swing.sharing;

import java.awt.CardLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.sharing.actions.SharingRemoveTableAction;
import org.limewire.ui.swing.sharing.dragdrop.SharingTransferHandler;
import org.limewire.ui.swing.sharing.fancy.SharingFancyPanel;
import org.limewire.ui.swing.sharing.friends.BuddyItem;
import org.limewire.ui.swing.sharing.friends.BuddyItemImpl;
import org.limewire.ui.swing.sharing.friends.BuddyNameTable;
import org.limewire.ui.swing.sharing.friends.BuddyTableFormat;
import org.limewire.ui.swing.sharing.table.SharingTable;
import org.limewire.ui.swing.sharing.table.SharingTableFormat;
import org.limewire.ui.swing.sharing.table.SharingTableModel;
import org.limewire.ui.swing.table.MultiButtonTableCellRendererEditor;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BuddySharePanel extends GenericSharingPanel {
    public static final String NAME = "All Friends";
    
    @Resource
    protected Icon cancelIcon;
    @Resource
    protected Icon sharingIcon;
    
    private ViewSelectionPanel viewSelectionPanel;
       
    private final FileList fileList;
    
    private CardLayout viewCardLayout;

    private BuddyNameTable buddyTable;
    SharingFancyPanel sharingFancyPanel;

    MultiButtonTableCellRendererEditor editor;
    MultiButtonTableCellRendererEditor renderer;

    EventList<BuddyItem> eventList;
    
    private Map<String, FileList> buddyLists;
    
    @Inject
    public BuddySharePanel(LibraryManager libraryManager, SharingBuddyEmptyPanel emptyPanel) {        
        GuiUtils.assignResources(this); 
        
        this.fileList = libraryManager.getAllBuddyList();
        buddyLists = libraryManager.getUniqueLists();

        viewCardLayout = new CardLayout();
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(viewCardLayout);
        cardPanel.add(emptyPanel, ViewSelectionPanel.DISABLED);

        eventList = GlazedLists.threadSafeList(new BasicEventList<BuddyItem>());
        buddyTable = new BuddyNameTable(eventList, new BuddyTableFormat());

        
        SharingHeaderPanel headerPanel = createHeader(cardPanel);
               
        loadBuddies();
        createCenterCards(headerPanel, cardPanel);

        viewCardLayout.show(cardPanel, ViewSelectionPanel.DISABLED);
        
        BuddySelectionListener buddySelectionListener = new BuddySelectionListener(buddyTable, headerPanel, emptyPanel, cardPanel);
        buddyTable.getSelectionModel().addListSelectionListener(buddySelectionListener);
        
        
        setLayout(new MigLayout("insets 0 0 0 0", "[150!]0[grow]","[grow]"));
        
        add(headerPanel, "dock north");
        add(new JScrollPane(buddyTable), "grow");
        add(cardPanel, "grow");
        
    }
    
    private void loadBuddies() {
        for(String name : buddyLists.keySet()) {
            eventList.add(new BuddyItemImpl(name, buddyLists.get(name).getModel()));
        }
    }
    
    private SharingHeaderPanel createHeader(JPanel cardPanel) {
        viewSelectionPanel = new ViewSelectionPanel(new ItemAction(cardPanel, viewCardLayout, ViewSelectionPanel.LIST_SELECTED), 
                new ItemAction(cardPanel, viewCardLayout, ViewSelectionPanel.TABLE_SELECTED));
        
        SharingHeaderPanel headerPanel = new SharingHeaderPanel(sharingIcon, "Sharing with ", "", viewSelectionPanel);
        return headerPanel;
    }
    
    private void createCenterCards(SharingHeaderPanel headerPanel, JPanel cardPanel) {

        FilterList<FileItem> filteredList = new FilterList<FileItem>(fileList.getModel(), 
                new TextComponentMatcherEditor<FileItem>(headerPanel.getFilterBox(), new SharingTextFilterer()));
        

        createTable(filteredList);
        
        JScrollPane scrollPane = new JScrollPane();
        sharingFancyPanel = new SharingFancyPanel(filteredList, scrollPane, fileList);
        scrollPane.setViewportView(sharingFancyPanel);
        
        cardPanel.add(new JScrollPane(table),TABLE);
        cardPanel.add(scrollPane, LIST);
        viewCardLayout.show(cardPanel, LIST);
        
    }
    
    private void createTable(FilterList<FileItem> filteredList) {
        table = new SharingTable(filteredList, fileList, new SharingTableFormat());
        table.setTransferHandler(new SharingTransferHandler(fileList));
        table.setDropMode(DropMode.ON);
        
        editor = new MultiButtonTableCellRendererEditor(20);
        editor.addActions(createActions());
        renderer = new MultiButtonTableCellRendererEditor(20);
        renderer.addActions(createActions());
        
        //TODO: this needs to be fixed, if rows are columns or rows
        //  are removed this stops working
        TableColumn tc = table.getColumn(6);
        tc.setCellEditor(editor);
        tc.setCellRenderer(renderer);
    }
    
    private List<Action> createActions() {
        List<Action> list = new ArrayList<Action>();
        list.add(new SharingRemoveTableAction(table, cancelIcon));
        return list;
    }
    
    private class BuddySelectionListener implements ListSelectionListener, ListEventListener<FileItem> {

        private JTable buddy;
        private SharingHeaderPanel headerPanel;
        private SharingBuddyEmptyPanel emptyPanel;
        private JPanel cardPanel;
        
        private EventList<FileItem> currentList;
        
        public BuddySelectionListener(JTable table, SharingHeaderPanel headerPanel, SharingBuddyEmptyPanel emptyPanel, JPanel cardPanel) {
            this.buddy = table;
            this.headerPanel = headerPanel;
            this.emptyPanel = emptyPanel;
            this.cardPanel = cardPanel;
            currentList = null;
        }
        
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if(!e.getValueIsAdjusting()) {
                int index = buddy.getSelectedRow();
                BuddyItem buddyItem = (BuddyItem) buddy.getModel().getValueAt(index, 0);
                headerPanel.setBuddyName(buddyItem.getName());
                emptyPanel.setBuddyName(buddyItem.getName());
  
                FileList fileList = buddyLists.get(buddyItem.getName());
                table.setModel(new SharingTableModel(fileList.getModel(), fileList, new SharingTableFormat()));
                TableColumn tc = table.getColumn(6);
                tc.setCellEditor(editor);
                tc.setCellRenderer(renderer);
                sharingFancyPanel.setModel(fileList.getModel(), fileList);
                                
                if(currentList != null)
                    currentList.removeListEventListener(this);
                currentList = fileList.getModel();
                currentList.addListEventListener(this);
                
                if(currentList.size() > 0) {
                    viewSelectionPanel.setEnabled(true);
                } else {
                    viewSelectionPanel.setEnabled(false);
                }
                viewCardLayout.show(cardPanel, viewSelectionPanel.getSelectedButton());
            }
        }

        @Override
        public void listChanged(ListEvent<FileItem> listChanges) {
            if(listChanges.getSourceList().size() > 0) {
                viewSelectionPanel.setEnabled(true);
            } else {
                viewSelectionPanel.setEnabled(false);
            }
        }
        
    }
    

}
