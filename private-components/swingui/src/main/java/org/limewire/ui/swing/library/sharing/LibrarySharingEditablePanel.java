package org.limewire.ui.swing.library.sharing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.inject.LazySingleton;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.TextFieldDecorator;
import org.limewire.ui.swing.library.sharing.actions.ApplySharingAction;
import org.limewire.ui.swing.library.sharing.actions.CancelSharingAction;
import org.limewire.ui.swing.library.sharing.actions.SelectAllAction;
import org.limewire.ui.swing.library.sharing.actions.SelectNoneAction;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

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
import com.google.inject.name.Named;

@LazySingleton
public class LibrarySharingEditablePanel {
    
    @Resource Color borderColor;
    @Resource Font sharingLabelFont;
    @Resource Color sharingLabelColor;
    @Resource Font selectFont;
    @Resource Color selectColor;
    
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
            SelectAllAction selectAllAction, SelectNoneAction selectNoneAction,
            TextFieldDecorator textFieldDecorator, ButtonDecorator buttonDecorator) {
        GuiUtils.assignResources(this);
        
        component = new JPanel(new MigLayout("insets 0, gap 0, fillx", "[134!]", ""));
        
        this.sharingTableProvider = sharingTableProvider;
        this.renderer = renderer;
        this.editor = editor;
        
        component.setOpaque(false);
        
        JLabel shareLabel = new JLabel(I18n.tr("Share list with..."));
        shareLabel.setFont(sharingLabelFont);
        shareLabel.setForeground(sharingLabelColor);
        component.add(shareLabel, "gapleft 5, gaptop 6, wrap");
        
        filterTextField = new PromptTextField(I18n.tr("Find..."));
        textFieldDecorator.decorateClearablePromptField(filterTextField, AccentType.NONE);
        
        component.add(filterTextField, "gapleft 5, gaptop 7, gapright 5, wmax 124, wrap");

        JLabel selectLabel = new JLabel(I18n.tr("Select"));
        selectLabel.setFont(selectFont);
        selectLabel.setForeground(selectColor);
        component.add(selectLabel, "gapleft 5, gaptop 5, wrap");
        
        allButton = new HyperlinkButton(selectAllAction);
        allButton.setFont(selectFont);
        noneButton = new HyperlinkButton(selectNoneAction);
        noneButton.setFont(selectFont);
        component.add(allButton, "gapleft 15, gaptop 5, wrap");
        component.add(noneButton, "gapleft 15, gaptop 5, wrap");
        
        initTable();
        
        applyButton = new JXButton(applyAction);
        applyButton.setFont(selectFont);
        buttonDecorator.decorateDarkFullButton(applyButton, AccentType.NONE);
        cancelButton = new HyperlinkButton(cancelAction);
        cancelButton.setFont(selectFont);
        
        component.add(applyButton, "split 2, gaptop 5, gapbottom 5, gapright unrelated, alignx center");
        component.add(cancelButton, "gaptop 5, gapbottom 5, wrap");
    }
    
    @Inject
    void register(@Named("known") Collection<Friend> knownFriends, @Named("known") ListenerSupport<FriendEvent> knownSupport) {
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
                if(LibrarySharingEditablePanel.this.getComponent().isShowing()) {
                    SwingUtilities.invokeLater(new Runnable(){
                        public void run() {
                            component.revalidate();
                        }
                    });
                }
            }
        });
        
        for(Friend friend : knownFriends) {
            eventList.add(new EditableSharingData(friend, false));
        }
        
        //TODO: this changed on head, see how
        // FYI: Never use RosterEvent, Ever.
        
        // I don't understand why this is necessary at all --
        // the ShareList contains a model of people it is currently shared with,
        // and the checklist model doesn't need to be in real-time, just grab
        // the current list of known friends from @Named("known") Collection<Friend>
        // and add to it any IDs that are in the shared list that aren't known.
        // We don't need to keep it synced throughout the lifetime of the application.
        // Just build it once it needs to be shown and get rid of it after that.
        // Don't see why it needs to be a singleton or anything like that.
        knownSupport.addListener(new EventListener<FriendEvent>() {
            @Override
            public void handleEvent(FriendEvent event) {
                  switch(event.getType()) { 
                  case ADDED:
                      eventList.add(new EditableSharingData(event.getData(), false));
                      break;
                  case DELETE:
                  case REMOVED:
                      eventList.remove(new EditableSharingData(event.getData(), false));
                      break;
                  }                    
            }
        });
        
        //TODO: depending on how we handle offline mode, may want to add/remove
        // presencelistener here and repopulate the list at startup each time
//        component.addComponentListener(new ComponentListener(){
//
//            @Override
//            public void componentHidden(ComponentEvent e) {
//            }
//
//            @Override
//            public void componentMoved(ComponentEvent e) {
//            }
//
//            @Override
//            public void componentResized(ComponentEvent e) {
//            }
//
//            @Override
//            public void componentShown(ComponentEvent e) {
//            }
//            
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
        scrollPane.setMinimumSize(new Dimension(0,0));
        scrollPane.setBorder(BorderFactory.createMatteBorder(1,0,1,0, borderColor)); 
                
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
                data.setIsSelected(sharingEventList.contains(data.getId()));
            }
        } finally {
            eventList.getReadWriteLock().writeLock().unlock();
        }
    }
    
    /**
     * Returns a list of Friends who this list is shared with.
     */
    public List<String> getSelectedFriendIds() {
        List<String> friends = new ArrayList<String>();
        eventList.getReadWriteLock().readLock().lock();
        try {
            for(EditableSharingData data : eventList) {
                if(data.isSelected()) {
                    friends.add(data.getId());
                }
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
            baseList.add(data.getId());
        }
    }
}
