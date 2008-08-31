package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.friends.FriendsUtil.getIcon;
import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.border.DropShadowBorder;
import org.jdesktop.swingx.decorator.BorderHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.action.ItemNotifyable;
import org.limewire.ui.swing.action.PopupDecider;
import org.limewire.ui.swing.action.PopupUtil;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.event.RuntimeTopicPatternEventSubscriber;
import org.limewire.ui.swing.friends.Message.Type;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.xmpp.api.client.IncomingChatListener;
import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.EventTableModel;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
@Singleton
public class FriendsPane extends JPanel {
    
    private static final Log LOG = LogFactory.getLog(FriendsPane.class);
    private static final String ALL_CHAT_MESSAGES_TOPIC_PATTERN = MessageReceivedEvent.buildTopic(".*");
    
    private EventList<Friend> friends;
    private String myID;
    private final WeakHashMap<String, FriendImpl> idToFriendMap;
    private final FriendsCountUpdater friendsCountUpdater;
    private final LibraryManager libraryManager;
    private WeakReference<Friend>  activeConversation = new WeakReference<Friend>(null);
    private static final Color LIGHT_GREY = new Color(218, 218, 218);

    @Inject
    public FriendsPane(IconLibrary icons, FriendsCountUpdater friendsCountUpdater, LibraryManager libraryManager) {
        super(new BorderLayout());
        friends = new BasicEventList<Friend>();
        idToFriendMap = new WeakHashMap<String, FriendImpl>();
        this.friendsCountUpdater = friendsCountUpdater;
        this.libraryManager = libraryManager;
        ObservableElementList<Friend> observableList = new ObservableElementList<Friend>(friends, GlazedLists.beanConnector(Friend.class));
        SortedList<Friend> sortedFriends = new SortedList<Friend>(observableList,  new FriendAvailabilityComparator());
        JTable table = newSearchableJTable(sortedFriends, icons);
        
        addPopupMenus(table);
        
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll);
        setPreferredSize(new Dimension(120, 200));
        
        table.addMouseListener(new LaunchChatListener());
        
        EventAnnotationProcessor.subscribe(this);
    }

    private void addPopupMenus(JComponent comp) {
        FriendContext context = new FriendContext();
        ViewLibrary viewLibrary = new ViewLibrary(context);
        ViewSharedFiles viewSharedFiles = new ViewSharedFiles(context);
        JPopupMenu nonChattingPopup = PopupUtil.addPopupMenus(comp, new FriendPopupDecider(false, context), new OpenChat(context));
        nonChattingPopup.addSeparator();
        nonChattingPopup.add(viewLibrary);
        nonChattingPopup.add(viewSharedFiles);
        nonChattingPopup.addSeparator();
        nonChattingPopup.add(new RemoveBuddy(context));
        
        JPopupMenu chattingPopup = PopupUtil.addPopupMenus(comp, new FriendPopupDecider(true, context), viewLibrary, viewSharedFiles);
        chattingPopup.addSeparator();
        chattingPopup.add(new CloseChat(context));
    }
    
    private static class FriendPopupDecider implements PopupDecider {
        private final boolean expected;
        private final FriendContext context;
        
        public FriendPopupDecider(boolean expected, FriendContext context) {
            this.expected = expected;
            this.context = context; 
        }

        @Override
        public boolean shouldDisplay(MouseEvent e) {
            JTable table = (JTable) e.getComponent();
            int index = table.rowAtPoint(e.getPoint());
            if (index < 0) {
                return false;
            }
            //Popup selects the item (as per spec)
            table.getSelectionModel().setSelectionInterval(index, index);
            
            Friend friend = getFriend(table, index);
            
            context.setFriend(friend);
            return friend.isChatting() == expected;
        }
    }
    
    private static Friend getFriend(JTable table, int index) {
        EventTableModel model = (EventTableModel)table.getModel();
        return index < 0 ? null : (Friend) model.getElementAt(index);
    }
    
    private JTable newSearchableJTable(final EventList<Friend> friendsList, final IconLibrary icons) {
        TextFilterator<Friend> friendFilterator = new TextFilterator<Friend>() {
            @Override
            public void getFilterStrings(List<String> baseList, Friend element) {
                baseList.add(element.getName());
            }
        };
        
        final TextMatcherEditor<Friend> editor = new TextMatcherEditor<Friend>(friendFilterator);
        final FilterList<Friend> filter = new FilterList<Friend>(friendsList, editor);
        
        TableFormat<Friend> format = new TableFormat<Friend>() {
            @Override
            public int getColumnCount() {
                return 3;
            }

            @Override
            public String getColumnName(int column) {
                switch(column) {
                case 0:
                    return "status";
                case 1:
                    return "name";
                case 2:
                    return "close";
                }
                throw new UnsupportedOperationException(tr("Too many columns expected in friends chat table. Tried getting column: ") + column);
            }

            @Override
            public Object getColumnValue(Friend friend, int column) {
                switch(column) {
                case 0:
                    //Change to chatting icon because gtalk doesn't actually set mode to 'chat', so icon won't show chat bubble normally
                    if (friend.isChatting()) {
                        return friend.isReceivingUnviewedMessages() ? icons.getUnviewedMessages() : icons.getChatting();
                    }
                    Icon icon = getIcon(friend, icons);
                    return icon;
                case 1:
                    return friend.getName();
                case 2:
                    return icons.getEndChat();
                }
                throw new UnsupportedOperationException(tr("Couldn't find value for unknown friends chat table column: ") + column);
            }
        };
        
        final JXTable table = new JXTable(new EventTableModel<Friend>(friendsList, format)); 
        
        table.addKeyListener(new KeyAdapter() {
            ArrayList<String> keysPressed = new ArrayList<String>();
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if (keyCode == KeyEvent.VK_DELETE) {
                    keysPressed.clear();
                } else if (keyCode == KeyEvent.VK_BACK_SPACE && keysPressed.size() > 0) { 
                    keysPressed.remove(keysPressed.size() - 1);
                } else {
                    keysPressed.add(Character.toString(e.getKeyChar()));
                }
                
                editor.setFilterText(keysPressed.toArray(new String[0]));
                
                if (LOG.isDebugEnabled()) {
                    LOG.debugf("FriendsPane keyPressed(): {0} {1} ", KeyEvent.getKeyText(e.getKeyCode()), getKeyPressed());
                }
                
                if (filter.size() > 0) {
                    Friend firstFriend = filter.get(0);
                    int index = friendsList.indexOf(firstFriend);
                    table.getSelectionModel().setSelectionInterval(index, index);
                }
            }
            
            private String getKeyPressed() {
                StringBuilder builder = new StringBuilder();
                for(String s : keysPressed) {
                    builder.append(s);
                }
                return builder.toString();
            }
        });
        
        IconCellRenderer iconRenderer = new IconCellRenderer();
        TableColumnModel columnModel = table.getColumnModel();
        TableColumn statusIconColumn = columnModel.getColumn(0);
        statusIconColumn.setCellRenderer(iconRenderer);
        statusIconColumn.setPreferredWidth(11);
        columnModel.getColumn(1).setCellRenderer(new FriendNameCellRenderer());
        TableColumn closeIconColumn = columnModel.getColumn(2);
        closeIconColumn.setCellRenderer(iconRenderer);
        closeIconColumn.setPreferredWidth(9);
        
        table.setShowVerticalLines(false);
        
        table.setTableHeader(null);
        table.setColumnMargin(0);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        table.addHighlighter(new BorderHighlighter(new ChattingUnderlineHighlightPredicate(), 
                new DropShadowBorder(new Color(194, 194, 194), 1, 1.0f, 1, false, false, true, false)));
        
        return table;
    }
    
    private class ChattingUnderlineHighlightPredicate implements HighlightPredicate {

        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            JXTable table = (JXTable) adapter.getComponent();
            
            int row = adapter.row;
            Friend friend = getFriend(table, row);
            
            if (friend.isChatting()) {
                EventTableModel model = (EventTableModel) table.getModel();
                if (model.getRowCount() > (row + 1)) {
                    Friend nextFriend = (Friend) model.getElementAt(row + 1);
                    if (!nextFriend.isChatting()) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
    
    @EventSubscriber
    public void handlePresenceUpdate(PresenceUpdateEvent event) {
        LOG.debugf("handling presence {0}, {1}", event.getPresence().getJID(), event.getPresence().getType());
        final Presence presence = event.getPresence();
        FriendImpl friend = idToFriendMap.get(presence.getJID());
        switch(presence.getType()) {
            case available:
                if(friend == null) {
                    final FriendImpl newFriend = new FriendImpl(event.getUser(), presence);
                    presence.setIncomingChatListener(new IncomingChatListener() {
                        public MessageReader incomingChat(MessageWriter writer) {
                            LOG.debugf("{0} is typing a message", presence.getJID());
                            MessageWriter writerWrapper = new MessageWriterImpl(myID, newFriend, writer);
                            ConversationStartedEvent event = new ConversationStartedEvent(newFriend, writerWrapper, false);
                            event.publish();
                            //Hang out until a responder has processed this event
                            event.await();
                            return new MessageReaderImpl(newFriend);
                        }
                    });
                    friend = newFriend;
                    friends.add(friend);
                    idToFriendMap.put(presence.getJID(), friend);
                }
                friend.setStatus(presence.getStatus());
                friend.setMode(presence.getMode());
                break;
            case unavailable:
                if (friend != null) {
                    friends.remove(idToFriendMap.remove(presence.getJID()));
                } 
                break;
        }
        friendsCountUpdater.setFriendsCount(friends.size());
    }
    
    //FIXME This subscription is not working...
    @RuntimeTopicPatternEventSubscriber
    public void handleMessageReceived(String topic, MessageReceivedEvent event) {
        Message message = event.getMessage();
        LOG.debugf("All Messages listener: from {0} text: {1} topic: {2}", message.getSenderName(), message.getMessageText(), topic);
        Friend friend = message.getFriend();
        if (!friend.isActiveConversation() && message.getType() == Type.Received) {
            friend.setReceivingUnviewedMessages(true);
        }
    }
    
    public String getTopicPatternName() {
        return ALL_CHAT_MESSAGES_TOPIC_PATTERN;
    }
    
    public void setLoggedInID(String id) {
        this.myID = id;
    }
    
    private void fireConversationStarted(Friend friend) {
        MessageWriter writer = friend.createChat(new MessageReaderImpl(friend));
        MessageWriter writerWrapper = new MessageWriterImpl(myID, friend, writer);
        new ConversationStartedEvent(friend, writerWrapper, true).publish();
        setActiveConversation(friend);
    }

    private void setActiveConversation(Friend friend) {
        LOG.debugf("Setting active conversation: {0}", friend.getName());
        Friend oldActiveConversation = activeConversation.get();
        if (oldActiveConversation != null) {
            oldActiveConversation.setActiveConversation(false);
        }
        activeConversation = new WeakReference<Friend>(friend);
        friend.setReceivingUnviewedMessages(false);
        friend.setActiveConversation(true);
    }
    
    private class IconCellRenderer extends JLabelCellRenderer {
        
        @Override
        protected JXLabel getJLabel(Object value) {
            return new JXLabel((Icon)value);
        }

        @Override
        protected String getPreferredBorderLayout() {
            return BorderLayout.CENTER;
        }
    }
    
    private abstract class JLabelCellRenderer implements TableCellRenderer {

        @Override
        public final Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JXPanel cell = new JXPanel(new BorderLayout());
            
            JLabel label = getJLabel(value);
            cell.add(label, getPreferredBorderLayout());
            
            Friend friend = getFriend(table, row);
            
            if (isSelected) {
                cell.setBackground(LIGHT_GREY);
            } else if (friend.isActiveConversation()) {
                RectanglePainter painter = new RectanglePainter();
                //light-blue gradient
                painter.setFillPaint(new GradientPaint(50.0f, 0.0f, Color.WHITE, 50.0f, 20.0f, new Color(176, 205, 247)));
                painter.setBorderPaint(Color.WHITE);
                painter.setBorderWidth(0f);
                cell.setBackgroundPainter(painter);
            } else  {
                cell.setBackground(table.getBackground());
                cell.setForeground(table.getForeground());
            }

            return cell;
        }

        protected abstract JXLabel getJLabel(Object value);
        protected abstract String getPreferredBorderLayout();
    }

    private class FriendNameCellRenderer extends JLabelCellRenderer {

        @Override
        protected JXLabel getJLabel(Object value) {
            JXLabel friendName = new JXLabel(value.toString());
            FontUtils.changeSize(friendName, -2.8f);
            friendName.setMaximumSize(new Dimension(85, 12));
            return friendName;
        }

        @Override
        protected String getPreferredBorderLayout() {
            return BorderLayout.WEST;
        }
    }
    
    private class LaunchChatListener extends MouseAdapter {
        
        @Override
        public void mouseClicked(MouseEvent e) {
            JTable table = (JTable)e.getSource();
            
            Friend friend = getFriend(table, table.rowAtPoint(e.getPoint()));
            
            if (friend == null) {
                return;
            }
            
            if (e.getClickCount() == 2 || friend.isChatting()) {
                fireConversationStarted(friend);
             }
        }
    }
    
    private class FriendContext {
        private WeakReference<Friend> weakFriend;
        
        public Friend getFriend() {
            return weakFriend == null ? null : weakFriend.get();
        }
        
        public void setFriend(Friend friend) {
            weakFriend = new WeakReference<Friend>(friend);
        }
    }
    
    private abstract class AbstractContextAction extends AbstractAction {
        protected final FriendContext context;
        
        public AbstractContextAction(String name, FriendContext context) {
            super(tr(name));
            this.context = context;
        }
    }
    
    private class OpenChat extends AbstractContextAction {
        public OpenChat(FriendContext context) {
            super("Open Chat", context);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Friend friend = context.getFriend();
            if (friend != null) {
                fireConversationStarted(friend);
            }
        }
    }
    
    private class ViewLibrary extends AbstractContextAction {
        public ViewLibrary(FriendContext context) {
            super("View Library", context);
        }
        
        @Override
        public boolean isEnabled() {
            Friend friend = context.getFriend();
            return friend != null && friend.isSignedInToLimewire();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            //TODO: How do you switch to library view?
        }
    }
    
    private class ViewSharedFiles extends AbstractContextAction implements ItemNotifyable {
        public ViewSharedFiles(FriendContext context) {
            super("", context);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            //TODO: How do you view shared files?
        }

        @Override
        public boolean isEnabled() {
            Friend friend = context.getFriend();
            
            if (friend == null) {
                return false;
            }
            
            return getSharedFileCount(friend) > 0 && friend.isSignedInToLimewire();
        }

        @Override
        public void notifyItem(JMenuItem item) {
            Friend friend = context.getFriend();
            
            if (friend == null) {
                return;
            }
            
            int sharedFileCount = getSharedFileCount(friend);
            item.setText(buildString(tr("View Files I'm sharing with them")," (", sharedFileCount, ")"));
            if (sharedFileCount == 0) {
                item.setToolTipText(buildString(friend.getName(), " ", tr("isn't using LimeWire. Tell them about it to see their Library")));
            }
        }

        private int getSharedFileCount(Friend friend) {
            if (!friend.isSignedInToLimewire()) {
                return 0;
            }
            FileList sharedFileList = libraryManager.getBuddy(friend.getName());
            int sharedFileCount = sharedFileList == null ? 0 : sharedFileList.size();
            return sharedFileCount;
        }
        
        private String buildString(Object... arguments) {
            StringBuilder bldr = new StringBuilder();
            for(Object arg : arguments) {
                bldr.append(arg);
            }
            return bldr.toString();
        }
    }
    
    private class RemoveBuddy extends AbstractContextAction {
        public RemoveBuddy(FriendContext context) {
            super("Remove buddy from list", context);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            //TODO: How do you remove a buddy?
        }
    }
    
    private class CloseChat extends AbstractContextAction {
        public CloseChat(FriendContext context) {
            super("Close chat (closes the current chat window)", context);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Friend friend = context.getFriend();
            if (friend != null) {

                friend.stopChat();
                new CloseChatEvent(friend).publish();
                
                Friend nextFriend = null;
                for(Friend tmpFriend : friends) {
                    if (tmpFriend == friend || !tmpFriend.isChatting()) {
                        continue;
                    }
                    if (nextFriend == null || tmpFriend.getChatStartTime() > nextFriend.getChatStartTime()) {
                        nextFriend = tmpFriend;
                    }
                }

                if (nextFriend != null) {
                    fireConversationStarted(nextFriend);
                }
            }
        }
    }
}
