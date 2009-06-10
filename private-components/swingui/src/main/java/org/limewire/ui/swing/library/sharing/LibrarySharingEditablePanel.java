package org.limewire.ui.swing.library.sharing;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.friend.FriendManager;
import org.limewire.inject.LazySingleton;
import org.limewire.listener.ListenerSupport;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.library.sharing.actions.ApplySharingAction;
import org.limewire.ui.swing.library.sharing.actions.CancelSharingAction;
import org.limewire.ui.swing.library.sharing.actions.SelectAllAction;
import org.limewire.ui.swing.library.sharing.actions.SelectNoneAction;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.RosterEvent;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class LibrarySharingEditablePanel {
    
    private final JPanel component;
    private PromptTextField filterTextField;
    private HyperlinkButton allButton;
    private HyperlinkButton noneButton;
    private JXButton applyButton;
    private HyperlinkButton cancelButton;
    
    private Provider<LibrarySharingTable<EditableSharingData>> sharingTableProvider;
    private Provider<LibrarySharingEditableRendererEditor> renderer;
    private Provider<LibrarySharingEditableRendererEditor> editor;
    
    private LibrarySharingTable<EditableSharingData> sharingTable;
    private EventList<EditableSharingData> eventList;
    private FilterList<EditableSharingData> filterList;
    
    @Inject
    public LibrarySharingEditablePanel(Provider<LibrarySharingTable<EditableSharingData>> sharingTableProvider,
            Provider<LibrarySharingEditableRendererEditor> renderer,
            Provider<LibrarySharingEditableRendererEditor> editor,
            ApplySharingAction applyAction, CancelSharingAction cancelAction, 
            SelectAllAction selectAllAction, SelectNoneAction selectNoneAction) {
        component = new JPanel(new MigLayout("insets 0, gap 0, fillx", "[125!]", ""));
        
        this.sharingTableProvider = sharingTableProvider;
        this.renderer = renderer;
        this.editor = editor;
        
        component.setOpaque(false);
        
        component.add(new JLabel(I18n.tr("Share list with...")), "gapleft 5, gaptop 5, wrap");
        
        filterTextField = new PromptTextField(I18n.tr("Find..."));
        
        component.add(filterTextField, "gapleft 5, gaptop 5, wmax 115, wrap");

        component.add(new JLabel(I18n.tr("Select")), "gapleft 5, gaptop 5, wrap");
        allButton = new HyperlinkButton(selectAllAction);
        noneButton = new HyperlinkButton(selectNoneAction);
        component.add(allButton, "gapleft 15, gaptop 5, wrap");
        component.add(noneButton, "gapleft 15, gaptop 5, wrap");
        
        initTable();
        
        applyButton = new JXButton(applyAction);
        cancelButton = new HyperlinkButton(cancelAction);
        
        component.add(applyButton, "split 2, gaptop 5, gapright unrelated, alignx center");
        component.add(cancelButton, "gaptop 5, wrap");
    }
    
    @Inject
    void register(FriendManager friendManager, ListenerSupport<RosterEvent> rosterListeners) {
        if(eventList == null) {
            filterList = createEventListChain();
            SwingUtilities.invokeLater(new Runnable(){
                public void run() {
                    sharingTable.setEventList(filterList);
                    sharingTable.getColumnModel().getColumn(0).setCellRenderer(renderer.get());            
                    sharingTable.getColumnModel().getColumn(0).setCellEditor(editor.get());       
                }
            });
        }
        
        // the table is strictly the size of the number of rows or full screen with a scrollbar
        // if it surpasses available space. When adding/removing friends or filtering need to
        // revalidate the size to correctly update the panel and table sizing.
        filterList.addListEventListener(new ListEventListener<EditableSharingData>(){
            @Override
            public void listChanged(ListEvent<EditableSharingData> listChanges) {
                SwingUtilities.invokeLater(new Runnable(){
                    public void run() {
                        component.revalidate();
                    }
                });
            }
        });
        
        eventList.add(new EditableSharingData("this is fake data", false));
        eventList.add(new EditableSharingData("remove when", false));
        eventList.add(new EditableSharingData("friend login", false));
        eventList.add(new EditableSharingData("works again", false));
//        for(Friend friend : friendManager.getKnownFriends()) {
//            eventList.add(new EditableSharingData(friend.getRenderName(), false));
//        }
        
//        rosterListeners.addListener(new EventListener<RosterEvent>() {
//            @Override
//            public void handleEvent(RosterEvent event) {
//                XMPPFriend user = event.getData();
//
//                switch(event.getType()) {
//                case USER_ADDED:
//                    eventList.add(new EditableSharingData(user.getRenderName(), false));
//                case USER_UPDATED:
////                    if (user.isSubscribed()) {
////                        addKnownFriend(user);
////                    } else {
////                        removeKnownFriend(user, true);
////                    }
//                    break;
//                case USER_DELETED:
////                    removeKnownFriend(user, true);
//                    break;
//                }
//            }
//        });
    }
    
    /**
     * Sets up the EventList chain for displaying, filtering, sorting friends.
     */
    private FilterList<EditableSharingData> createEventListChain() {
        eventList = GlazedLists.threadSafeList(new BasicEventList<EditableSharingData>());
        MatcherEditor<EditableSharingData> matcher = new TextComponentMatcherEditor<EditableSharingData>(filterTextField, new FriendFilterator());
        FilterList<EditableSharingData> filterList = new FilterList<EditableSharingData>(eventList, matcher);
        return filterList;
    }
    
    private void initTable() {
        sharingTable = sharingTableProvider.get();
        sharingTable.enableEditing(true);
        sharingTable.setEventList(new BasicEventList<EditableSharingData>());
                
        JScrollPane scrollPane = new JScrollPane(sharingTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); 
                
        component.add(scrollPane, "growx, gaptop 5, wrap");
    }
    
    public JComponent getComponent() {
        return component;
    }
    
    public void updateTableModel(List<String> sharingEventList) {
        filterTextField.setText("");
        eventList.getReadWriteLock().writeLock().lock();
        try {
            for(EditableSharingData data : eventList) {
                data.setIsSelected(sharingEventList.contains(data.getName()));
            }
        } finally {
            eventList.getReadWriteLock().writeLock().lock();
        }
    }
    
    /**
     * Returns a list of Friends who this list is shared with.
     */
    public List<String> getSelectedFriends() {
        List<String> friends = new ArrayList<String>();
        eventList.getReadWriteLock().readLock().lock();
        try {
            for(EditableSharingData data : eventList) {
                if(data.isSelected())
                    friends.add(data.getName());
            }
        } finally {
            eventList.getReadWriteLock().readLock().unlock();
        }
        return friends;
    }
    
    public void selectAllFriends() {
        selectAll(true);
    }
    
    public void deselectAllFriends() {
        selectAll(false);
    }
    
    private void selectAll(boolean isSelected) {
        eventList.getReadWriteLock().readLock().lock();
        try {
            for(EditableSharingData data : filterList) {
                data.setIsSelected(isSelected);
            }
        } finally {
            eventList.getReadWriteLock().readLock().unlock();
        }
        sharingTable.repaint();
    }
    
    /**
     * Filters on the displayed name of a Friend.
     */
    private class FriendFilterator implements TextFilterator<EditableSharingData> {
        @Override
        public void getFilterStrings(List<String> baseList, EditableSharingData data) {
            baseList.add(data.getName());
        }
    }
}
