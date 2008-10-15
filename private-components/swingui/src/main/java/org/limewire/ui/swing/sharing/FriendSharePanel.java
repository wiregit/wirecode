package org.limewire.ui.swing.sharing;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Resource;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.SignoffEvent;
import org.limewire.ui.swing.sharing.dragdrop.ShareDropTarget;
import org.limewire.ui.swing.sharing.fancy.SharingFancyPanel;
import org.limewire.ui.swing.sharing.fancy.SharingFancyPanelFactory;
import org.limewire.ui.swing.sharing.friends.FriendItem;
import org.limewire.ui.swing.sharing.friends.FriendItemImpl;
import org.limewire.ui.swing.sharing.friends.FriendNameTable;
import org.limewire.ui.swing.sharing.friends.FriendTableFormat;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.ObservableElementList.Connector;
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
    private Color leftBorderColor;
    
    private final CardLayout viewCardLayout;

    private final FriendNameTable friendTable;
    private SharingFancyPanel sharingFancyPanel;

    private final EventList<FriendItem> friendsList;

    private final ShareListManager shareListManager;
    private final SharingFancyPanelFactory sharingFancyPanelFactory;
        
    private final FriendSharingHeaderPanel headerPanel;
    
    @Inject
    public FriendSharePanel(LibraryManager libraryManager, ShareListManager shareListManager, FriendNameTable friendNameTable, SharingFriendEmptyPanel emptyPanel, SharingFancyPanelFactory sharingFancyPanelFactory) {        
        GuiUtils.assignResources(this); 
        EventAnnotationProcessor.subscribe(this);

        this.shareListManager = shareListManager;
        this.sharingFancyPanelFactory = sharingFancyPanelFactory;
        
        viewCardLayout = new CardLayout();
        JPanel cardPanel = new JPanel();
        cardPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, leftBorderColor));
        cardPanel.setLayout(viewCardLayout);
        cardPanel.add(emptyPanel, EMPTY);

        Connector<FriendItem> connector = GlazedLists.beanConnector(FriendItem.class); 
        friendsList = GlazedListsFactory.observableElementList(new BasicEventList<FriendItem>(), connector);               

        this.friendTable = friendNameTable;
        friendTable.setTableModel(friendsList, new FriendTableFormat());
        
        headerPanel = new FriendSharingHeaderPanel(I18n.tr("Sharing with {0}"), "", libraryManager);

        createCenterCards(headerPanel, cardPanel);

        viewCardLayout.show(cardPanel, EMPTY);
        
        FriendSelectionListener friendSelectionListener = new FriendSelectionListener(friendTable, headerPanel, emptyPanel, cardPanel);
        friendTable.getSelectionModel().addListSelectionListener(friendSelectionListener);
        friendTable.setPreferredSize(new Dimension(141, friendTable.getPreferredSize().height));
        //if the list is populated for the first time, select the first friend
        friendsList.addListEventListener(new ListEventListener<FriendItem>(){
            int oldSize = 0;
            @Override
            public void listChanged(ListEvent<FriendItem> listChanges) {
                if(listChanges.getSourceList().size() != oldSize) {
                    if(oldSize == 0) {
                        SwingUtilities.invokeLater(new Runnable(){
                            public void run() {
                                if(friendTable.getModel().getRowCount() > 0)
                                    friendTable.getSelectionModel().setSelectionInterval(0, 0);
                            }
                        });
                    }
                    oldSize = listChanges.getSourceList().size();
                }
            }});
        
        JScrollPane friendTableScrollPane = new JScrollPane();
        friendTableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        friendTableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        friendTableScrollPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        friendTableScrollPane.setViewportView(friendTable);
        
        friendTable.addKeyListener(friendSelectionListener);
        
        setLayout(new MigLayout("insets 0 0 0 0", "[150!]0[grow]","[grow]"));
        
        add(headerPanel, "dock north");
        add(friendTableScrollPane, "grow");
        add(cardPanel, "grow");
    }
    
    private void createCenterCards(FriendSharingHeaderPanel headerPanel, JPanel cardPanel) {
        EventList<LocalFileItem> tempList = new BasicEventList<LocalFileItem>();

        sharingFancyPanel = sharingFancyPanelFactory.create(tempList, scrollPane, null);
        scrollPane.setViewportView(sharingFancyPanel);
        cardPanel.add(scrollPane, NONEMPTY);       
    }
    
    // TODO: There's a lot of funkiness with disposing of listeners here --
    //       all of this is necessary because the change that triggers
    //       selection (and thus this' valueChanged method) also triggers
    //       a listChanged on all the filters & listeners that are installed
    //       here.  However, the selection-listening change is installed first
    //       (and there's no way around that, because the selection listening
    //        is what triggers other listeners), so it hears the changes first.
    //       In most cases, a selectionChanged will cause a subsequent list
    //       to want to dispose its listening or remove a listener -- usually
    //       this is OK, except GlazedLists keeps track of what wanted to listen
    //       when the event was triggered and will *always* inform those listeners,
    //       even if they're unregistered by the time it comes around to notifying
    //       them.  In some cases, this causes an NPE (such as FilterList), in others
    //       it causes incorrect logic temporarily (setting properties based on
    //       an old source list).
    //       To workaround this, we have to invokeLater the listener removal and
    //       have special checks that make sure when we hear a listChanged event,
    //       the source list is correct.
    //       Another approach would be to add an in-between EventList that just forwards
    //       list-changed events, but does *not* forward them if the list unregistered
    //       for listening by the time it comes to forwarding the event.
    private class FriendSelectionListener implements ListSelectionListener , ListEventListener<LocalFileItem>, KeyListener {

        private final JTable friend;
        private final FriendSharingHeaderPanel headerPanel;
        private final SharingFriendEmptyPanel emptyPanel;
        private final JPanel cardPanel;
        
        private ShareDropTarget emptyDropTarget;
        private ShareDropTarget dropTarget;
        
        private boolean isPressed = false;
        
        private EventList<LocalFileItem> currentList;
        private TransformedList<LocalFileItem, LocalFileItem> filteredList;
        private FriendFileList fileList;
        
        private String lastRenderedId = "";
        
        public FriendSelectionListener(JTable table, FriendSharingHeaderPanel headerPanel, SharingFriendEmptyPanel emptyPanel, JPanel cardPanel) {
            this.friend = table;
            this.headerPanel = headerPanel;
            this.emptyPanel = emptyPanel;
            this.cardPanel = cardPanel;
            currentList = null;
        }
        
        @Override
        public void valueChanged(ListSelectionEvent e) { 
            if(e.getValueIsAdjusting()) {
                return;
            }
        
            int index = friend.getSelectedRow();
            if(index == -1) {
                return;
            }
            
            FriendItem friendItem = (FriendItem) friend.getModel().getValueAt(index, 0);
            if(friendItem.getFriend().getId().equals(lastRenderedId)) {
                return;
            }
            
            lastRenderedId = friendItem.getFriend().getId();
            
            headerPanel.setFriendName(friendItem.getFriend().getRenderName());
            emptyPanel.setFriendName(friendItem.getFriend().getRenderName());

            fileList = shareListManager.getOrCreateFriendShareList(friendItem.getFriend());
            if(currentList != null) {
                assert filteredList != null;
                final TransformedList<LocalFileItem, LocalFileItem> toDispose = filteredList;
                final EventList<LocalFileItem> toDispose2 = currentList;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        toDispose.dispose();
                        toDispose2.removeListEventListener(FriendSelectionListener.this);
                    }
                });
            } else {
                assert filteredList == null;
            }
            
            currentList = fileList.getSwingModel();
            filteredList = GlazedListsFactory.filterList(currentList, 
                    new TextComponentMatcherEditor<LocalFileItem>(headerPanel.getFilterBox(), new SharingTextFilterer()));
            currentList.addListEventListener(this);
            
            emptyPanel.setUserFileList(fileList);
            if(!isPressed)
                sharingFancyPanel.setModel(filteredList, fileList);
            headerPanel.setModel(fileList);           
            
            if(dropTarget == null) {
                dropTarget = new ShareDropTarget(FriendSharePanel.this, fileList, true);
                emptyDropTarget = new ShareDropTarget(emptyPanel, fileList, true);                
            } else {
                dropTarget.setModel(fileList);
                emptyDropTarget.setModel(fileList);
            }

            headerPanel.setEnabled(currentList.size() > 0); 
            if(currentList.size() > 0) {
                viewCardLayout.show(cardPanel, NONEMPTY);
            } else {
                viewCardLayout.show(cardPanel, EMPTY);
            }
        }

        @Override
        public void listChanged(ListEvent<LocalFileItem> listChanges) {
            // We only care about change events on the current list --
            // due to the way we reuse 'this' as a listener and how
            // events are queued up & sent from GlazedLists, it's possible
            // that we get an event for a list that's no longer ours.
            if(listChanges.getSourceList() == currentList) {
                if(currentList.size() > 0) {
                    viewCardLayout.show(cardPanel, NONEMPTY);
                } else {
                    viewCardLayout.show(cardPanel, EMPTY);
                }
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            isPressed = true;
        }

        @Override
        public void keyReleased(KeyEvent e) {
            isPressed = false;
            sharingFancyPanel.setModel(filteredList, fileList);
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }
    }
    
    @Inject
    public void register(ListenerSupport<RosterEvent> rosterEventListenerSupport) {
        rosterEventListenerSupport.addListener(this);
    }

    @Override
    @SwingEDTEvent
    public void handleEvent(final RosterEvent event) {
        if(event.getType().equals(User.EventType.USER_ADDED)) {
            LocalFileList fileList = shareListManager.getOrCreateFriendShareList(event.getSource());
            friendsList.add(new FriendItemImpl(event.getSource(), fileList.getSwingModel()));
        } else if(event.getType().equals(User.EventType.USER_REMOVED)) {
            shareListManager.removeFriendShareList(event.getSource());
        } else if(event.getType().equals(User.EventType.USER_UPDATED)) {
        }
    }   
    
    @EventSubscriber
    public void handleSignoff(SignoffEvent event) {
        friendsList.clear();
    }
    
    /**
     * If the friend exists in the list, this selects
     * that friend in the table and shows the appropriate 
     * information on the right.
     */
    public void selectFriend(Friend friend) {
        for(int i = 0; i < friendTable.getModel().getRowCount(); i++) {
            FriendItem item = (FriendItem) friendTable.getModel().getValueAt(i, 0);
            if(item.getFriend().getId().equals(friend.getId())) {
                friendTable.setRowSelectionInterval(i, i);
                break;
            }
        }
    }
}
