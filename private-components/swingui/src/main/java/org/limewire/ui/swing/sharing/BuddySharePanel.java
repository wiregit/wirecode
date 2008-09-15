package org.limewire.ui.swing.sharing;

import java.awt.CardLayout;
import java.util.ArrayList;
import java.util.HashMap;
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

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Resource;
import org.limewire.core.api.library.BuddyFileList;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.SignoffEvent;
import org.limewire.ui.swing.nav.NavigableTree;
import org.limewire.ui.swing.sharing.actions.SharingRemoveTableAction;
import org.limewire.ui.swing.sharing.dragdrop.ShareDropTarget;
import org.limewire.ui.swing.sharing.fancy.SharingFancyPanel;
import org.limewire.ui.swing.sharing.friends.BuddyItem;
import org.limewire.ui.swing.sharing.friends.BuddyItemImpl;
import org.limewire.ui.swing.sharing.friends.BuddyNameTable;
import org.limewire.ui.swing.sharing.friends.BuddyTableFormat;
import org.limewire.ui.swing.sharing.table.SharingTable;
import org.limewire.ui.swing.sharing.table.SharingTableFormat;
import org.limewire.ui.swing.sharing.table.SharingTableModel;
import org.limewire.ui.swing.table.IconLabelRenderer;
import org.limewire.ui.swing.table.MultiButtonTableCellRendererEditor;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BuddySharePanel extends GenericSharingPanel implements RegisteringEventListener<RosterEvent> {
    public static final String NAME = "All Friends";
    
    @Resource
    protected Icon cancelIcon;
    @Resource
    protected Icon sharingIcon;
    
    private ViewSelectionPanel viewSelectionPanel;
    
    private CardLayout viewCardLayout;

    private BuddyNameTable buddyTable;
    private SharingFancyPanel sharingFancyPanel;

    private MultiButtonTableCellRendererEditor editor;
    private MultiButtonTableCellRendererEditor renderer;

    private EventList<BuddyItem> eventList;
    
    private Map<String, FileList> buddyLists;
    
    private LibraryManager libraryManager;
    private IconManager iconManager;
    
    private BuddySharingHeaderPanel headerPanel;
    
    private SharingTableFormat sharingTableFormat;
    private IconLabelRenderer iconLabelRenderer;
    
    @Inject
    public BuddySharePanel(LibraryManager libraryManager, SharingBuddyEmptyPanel emptyPanel, NavigableTree navTree, IconManager iconManager) {        
        GuiUtils.assignResources(this); 
        EventAnnotationProcessor.subscribe(this);
        
        this.libraryManager = libraryManager;
        this.iconManager = iconManager;
        
        buddyLists = new HashMap<String,FileList>();

        viewCardLayout = new CardLayout();
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(viewCardLayout);
        cardPanel.add(emptyPanel, ViewSelectionPanel.DISABLED);

        //TODO: fix this. ObservableElementList is an easy way to update the table when a list size changes
        //  however it is making dynamic filtering of multiple lists very slow
        ObservableElementList.Connector<BuddyItem> buddyConnector = GlazedLists.beanConnector(BuddyItem.class);
        eventList = new ObservableElementList<BuddyItem>(GlazedLists.threadSafeList(new BasicEventList<BuddyItem>()), buddyConnector);
               
        buddyTable = new BuddyNameTable(eventList, new BuddyTableFormat(), libraryManager, navTree);
        
        headerPanel = createHeader(cardPanel);

        createCenterCards(headerPanel, cardPanel);

        viewCardLayout.show(cardPanel, ViewSelectionPanel.DISABLED);
        
        BuddySelectionListener buddySelectionListener = new BuddySelectionListener(buddyTable, headerPanel, emptyPanel, cardPanel);
        buddyTable.getSelectionModel().addListSelectionListener(buddySelectionListener);
        
        
        setLayout(new MigLayout("insets 0 0 0 0", "[150!]0[grow]","[grow]"));
        
        add(headerPanel, "dock north");
        add(new JScrollPane(buddyTable), "grow");
        add(cardPanel, "grow");
    }
    
    private BuddySharingHeaderPanel createHeader(JPanel cardPanel) {
        viewSelectionPanel = new ViewSelectionPanel(new ItemAction(cardPanel, viewCardLayout, ViewSelectionPanel.LIST_SELECTED), 
                new ItemAction(cardPanel, viewCardLayout, ViewSelectionPanel.TABLE_SELECTED));
        
        BuddySharingHeaderPanel headerPanel = new BuddySharingHeaderPanel(sharingIcon, "Sharing with ", "", viewSelectionPanel, libraryManager);
        return headerPanel;
    }
    
    private void createCenterCards(SharingHeaderPanel headerPanel, JPanel cardPanel) {
        EventList<LocalFileItem> tempList = new BasicEventList<LocalFileItem>();
        createTable(tempList);
        
        JScrollPane scrollPane = new JScrollPane();
        sharingFancyPanel = new SharingFancyPanel(tempList, scrollPane, null, iconManager);
        scrollPane.setViewportView(sharingFancyPanel);
        
        cardPanel.add(new JScrollPane(table),TABLE);
        cardPanel.add(scrollPane, LIST);
        viewCardLayout.show(cardPanel, LIST);
        
    }
    
    private void createTable(EventList<LocalFileItem> eventList) {
        sharingTableFormat = new SharingTableFormat();
        table = new SharingTable(eventList, null, sharingTableFormat);
//        table.setTransferHandler(new SharingTransferHandler(fileList));
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
        
        tc = table.getColumn(0);
        iconLabelRenderer = new IconLabelRenderer(iconManager);
        tc.setCellRenderer(iconLabelRenderer);
    }
    
    private List<Action> createActions() {
        List<Action> list = new ArrayList<Action>();
        list.add(new SharingRemoveTableAction(table, cancelIcon));
        return list;
    }
    
    private class BuddySelectionListener implements ListSelectionListener, ListEventListener<LocalFileItem> {

        private JTable buddy;
        private BuddySharingHeaderPanel headerPanel;
        private SharingBuddyEmptyPanel emptyPanel;
        private JPanel cardPanel;
        private String name = "";
        
        private ShareDropTarget emptyDropTarget;
        private ShareDropTarget dropTarget;
        
        private EventList<LocalFileItem> currentList;
        
        public BuddySelectionListener(JTable table, BuddySharingHeaderPanel headerPanel, SharingBuddyEmptyPanel emptyPanel, JPanel cardPanel) {
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
                if( index >= 0 && index < buddy.getModel().getRowCount()) {
                    BuddyItem buddyItem = (BuddyItem) buddy.getModel().getValueAt(index, 0);
                    if(buddyItem.getName().equals(name))
                        return;
                    
                    name = buddyItem.getName();
                    headerPanel.setBuddyName(buddyItem.getName());
                    emptyPanel.setBuddyName(buddyItem.getName());
      
                    BuddyFileList fileList = (BuddyFileList) buddyLists.get(buddyItem.getName());
                    emptyPanel.setUserFileList(fileList);
                    table.setModel(new SharingTableModel(fileList.getFilteredModel(), fileList,sharingTableFormat));
                    TableColumn tc = table.getColumn(6);
                    tc.setCellEditor(editor);
                    tc.setCellRenderer(renderer);
                    tc = table.getColumn(0);
                    tc.setCellRenderer(iconLabelRenderer);
                    sharingFancyPanel.setModel(fileList.getFilteredModel(), fileList);
                    headerPanel.setModel(fileList);           
                    
                    if(dropTarget == null) {
                        dropTarget = new ShareDropTarget(BuddySharePanel.this, fileList, true);
                        emptyDropTarget = new ShareDropTarget(emptyPanel, fileList, true);
                        
                    } else {
                        dropTarget.setModel(fileList);
                        emptyDropTarget.setModel(fileList);
                    }
                    
                    if(currentList != null)
                        currentList.removeListEventListener(this);
                    currentList = fileList.getModel();
                    currentList.addListEventListener(this);
                    
                    if(currentList.size() > 0) {
                        viewSelectionPanel.setEnabled(true);
                        headerPanel.setEnabled(true);
                    } else {
                        viewSelectionPanel.setEnabled(false);
                        headerPanel.setEnabled(false);
                    }
                    viewCardLayout.show(cardPanel, viewSelectionPanel.getSelectedButton());
                }
            }
        }

        @Override
        public void listChanged(ListEvent<LocalFileItem> listChanges) {
            if(listChanges.getSourceList().size() > 0) {
                viewSelectionPanel.setEnabled(true);
            } else {
                viewSelectionPanel.setEnabled(false);
            }
        }
    }
    
    @Inject
    public void register(ListenerSupport<RosterEvent> rosterEventListenerSupport) {
        rosterEventListenerSupport.addListener(this);
    }

    @Override
    public void handleEvent(RosterEvent event) {
        if(event.getType().equals(User.EventType.USER_ADDED)) {
            if(!libraryManager.containsBuddy(event.getSource().getId())) {
                libraryManager.addBuddy(event.getSource().getId());
            }
            addBuddy(event.getSource().getId());
        } else if(event.getType().equals(User.EventType.USER_REMOVED)) {
            libraryManager.removeBuddy(event.getSource().getId());
        } else if(event.getType().equals(User.EventType.USER_UPDATED)) {
        }
    }   
    
    @EventSubscriber
    public void handleSignoff(SignoffEvent event) {
        eventList.clear();
    }
    
    private void addBuddy(String name) {
        BuddyFileList fileList = (BuddyFileList) libraryManager.getBuddy(name);
        buddyLists.put(name, fileList);
        FilterList<LocalFileItem> filteredList = new FilterList<LocalFileItem>(fileList.getModel(), 
              new TextComponentMatcherEditor<LocalFileItem>(headerPanel.getFilterBox(), new SharingTextFilterer()));
        fileList.setFilteredModel(filteredList);

        eventList.add(new BuddyItemImpl(name, fileList.getFilteredModel()));
    }
    
    /**
     * If the buddy name exists in the list, this selects
     * that buddy in the table and shows the appropriate 
     * information on the right.
     * 
     * @param name - buddy name
     */
    public void selectBuddy(String name) {
        for(int i = 0; i < table.getModel().getRowCount(); i++) {
            BuddyItem item = (BuddyItem) table.getModel().getValueAt(i, 0);
            if(item.getName().equals(name)) {
                final int index = i;
                SwingUtils.invokeLater(new Runnable(){
                    public void run() {
                        table.setRowSelectionInterval(index, index);                        
                    }
                });
                return;
            }
        }
    }
}
