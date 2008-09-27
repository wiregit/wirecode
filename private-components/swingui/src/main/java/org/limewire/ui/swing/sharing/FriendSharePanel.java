package org.limewire.ui.swing.sharing;

import java.awt.CardLayout;
import java.util.ArrayList;
import java.util.List;

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
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.SignoffEvent;
import org.limewire.ui.swing.images.ThumbnailManager;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.sharing.actions.SharingRemoveTableAction;
import org.limewire.ui.swing.sharing.dragdrop.ShareDropTarget;
import org.limewire.ui.swing.sharing.fancy.SharingFancyPanel;
import org.limewire.ui.swing.sharing.friends.FriendItem;
import org.limewire.ui.swing.sharing.friends.FriendItemImpl;
import org.limewire.ui.swing.sharing.friends.FriendNameTable;
import org.limewire.ui.swing.sharing.friends.FriendTableFormat;
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
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FriendSharePanel extends GenericSharingPanel implements RegisteringEventListener<RosterEvent> {
    public static final String NAME = "All Friends";
    
    @Resource
    protected Icon cancelIcon;
    @Resource
    protected Icon sharingIcon;
    
    private ViewSelectionPanel viewSelectionPanel;
    
    private final CardLayout viewCardLayout;

    private final FriendNameTable friendTable;
    private SharingFancyPanel sharingFancyPanel;

    private MultiButtonTableCellRendererEditor editor;
    private MultiButtonTableCellRendererEditor renderer;

    private final EventList<FriendItem> friendsList;
    
    private final LibraryManager libraryManager;
    private final ShareListManager shareListManager;
    private final IconManager iconManager;
    private final ThumbnailManager thumbnailManager;
    
    private final FriendSharingHeaderPanel headerPanel;
    
    private SharingTableFormat sharingTableFormat;
    private IconLabelRenderer iconLabelRenderer;
    
    @Inject
    public FriendSharePanel(LibraryManager libraryManager, ShareListManager shareListManager,
            SharingFriendEmptyPanel emptyPanel, Navigator navigator, IconManager iconManager,
            ThumbnailManager thumbnailManager) {        
        GuiUtils.assignResources(this); 
        EventAnnotationProcessor.subscribe(this);
        
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        this.iconManager = iconManager;
        this.thumbnailManager = thumbnailManager;

        viewCardLayout = new CardLayout();
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(viewCardLayout);
        cardPanel.add(emptyPanel, ViewSelectionPanel.DISABLED);

        friendsList = new BasicEventList<FriendItem>();               
        friendTable = new FriendNameTable(friendsList, new FriendTableFormat(), libraryManager, shareListManager, navigator);
        
        headerPanel = createHeader(cardPanel);

        createCenterCards(headerPanel, cardPanel);

        viewCardLayout.show(cardPanel, ViewSelectionPanel.DISABLED);
        
        FriendSelectionListener friendSelectionListener = new FriendSelectionListener(friendTable, headerPanel, emptyPanel, cardPanel);
        friendTable.getSelectionModel().addListSelectionListener(friendSelectionListener);
        
        
        setLayout(new MigLayout("insets 0 0 0 0", "[150!]0[grow]","[grow]"));
        
        add(headerPanel, "dock north");
        add(new JScrollPane(friendTable), "grow");
        add(cardPanel, "grow");
    }
    
    private FriendSharingHeaderPanel createHeader(JPanel cardPanel) {
        viewSelectionPanel = new ViewSelectionPanel(new ItemAction(cardPanel, viewCardLayout, ViewSelectionPanel.LIST_SELECTED), 
                new ItemAction(cardPanel, viewCardLayout, ViewSelectionPanel.TABLE_SELECTED));
        
        FriendSharingHeaderPanel headerPanel = new FriendSharingHeaderPanel(sharingIcon, "Sharing with ", "", viewSelectionPanel, libraryManager);
        return headerPanel;
    }
    
    private void createCenterCards(SharingHeaderPanel headerPanel, JPanel cardPanel) {
        EventList<LocalFileItem> tempList = new BasicEventList<LocalFileItem>();
        createTable(tempList);
        
        JScrollPane scrollPane = new JScrollPane();
        sharingFancyPanel = new SharingFancyPanel(tempList, scrollPane, null, iconManager, thumbnailManager);
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
        
        editor = new MultiButtonTableCellRendererEditor();
        editor.addActions(createActions());
        renderer = new MultiButtonTableCellRendererEditor();
        renderer.addActions(createActions());
        table.setRowHeight(20);
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
    
    private class FriendSelectionListener implements ListSelectionListener, ListEventListener<LocalFileItem> {

        private JTable friend;
        private FriendSharingHeaderPanel headerPanel;
        private SharingFriendEmptyPanel emptyPanel;
        private JPanel cardPanel;
        private String name = "";
        
        private ShareDropTarget emptyDropTarget;
        private ShareDropTarget dropTarget;
        
        private EventList<LocalFileItem> currentList;
        
        public FriendSelectionListener(JTable table, FriendSharingHeaderPanel headerPanel, SharingFriendEmptyPanel emptyPanel, JPanel cardPanel) {
            this.friend = table;
            this.headerPanel = headerPanel;
            this.emptyPanel = emptyPanel;
            this.cardPanel = cardPanel;
            currentList = null;
        }
        
        @Override
        public void valueChanged(ListSelectionEvent e) { 
            if(!e.getValueIsAdjusting()) {
                int index = friend.getSelectedRow();
                if( index >= 0 && index < friend.getModel().getRowCount()) {
                    FriendItem friendItem = (FriendItem) friend.getModel().getValueAt(index, 0);
                    if(friendItem.getFriend().equals(name))
                        return;
                    
                    name = friendItem.getFriend().getRenderName();
                    headerPanel.setFriendName(friendItem.getFriend().getRenderName());
                    emptyPanel.setFriendName(friendItem.getFriend().getRenderName());
      
                    LocalFileList fileList = shareListManager.getOrCreateFriendShareList(friendItem.getFriend());
                    emptyPanel.setUserFileList(fileList);
                    EventList<LocalFileItem> filteredList = filter(fileList.getSwingModel());
                    table.setModel(new SharingTableModel(filteredList, fileList,sharingTableFormat));
                    TableColumn tc = table.getColumn(6);
                    tc.setCellEditor(editor);
                    tc.setCellRenderer(renderer);
                    tc = table.getColumn(0);
                    tc.setCellRenderer(iconLabelRenderer);
                    sharingFancyPanel.setModel(filteredList, fileList);
                    headerPanel.setModel(fileList);           
                    
                    if(dropTarget == null) {
                        dropTarget = new ShareDropTarget(FriendSharePanel.this, fileList, true);
                        emptyDropTarget = new ShareDropTarget(emptyPanel, fileList, true);
                        
                    } else {
                        dropTarget.setModel(fileList);
                        emptyDropTarget.setModel(fileList);
                    }
                    
                    if(currentList != null) {
                        currentList.removeListEventListener(this);
                    }
                    currentList = fileList.getSwingModel();
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
    public void handleEvent(final RosterEvent event) {
        if(event.getType().equals(User.EventType.USER_ADDED)) {
            final LocalFileList fileList = shareListManager.getOrCreateFriendShareList(event.getSource());
            SwingUtils.invokeLater(new Runnable() {
                public void run() {
                    friendsList.add(new FriendItemImpl(event.getSource(), fileList.getSwingModel()));                    
                }
            });
        } else if(event.getType().equals(User.EventType.USER_REMOVED)) {
            shareListManager.removeFriendShareList(event.getSource());
        } else if(event.getType().equals(User.EventType.USER_UPDATED)) {
        }
    }   
    
    @EventSubscriber
    public void handleSignoff(SignoffEvent event) {
        friendsList.clear();
    }
    
    private EventList<LocalFileItem> filter(EventList<LocalFileItem> list) {
        return new FilterList<LocalFileItem>(list, 
              new TextComponentMatcherEditor<LocalFileItem>(headerPanel.getFilterBox(), new SharingTextFilterer()));

    }
    
    /**
     * If the friend name exists in the list, this selects
     * that friend in the table and shows the appropriate 
     * information on the right.
     * 
     * @param name - friend name
     */
    public void selectFriend(String name) {
        for(int i = 0; i < table.getModel().getRowCount(); i++) {
            FriendItem item = (FriendItem) table.getModel().getValueAt(i, 0);
            if(item.getFriend().equals(name)) {
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
