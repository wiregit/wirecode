package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.friends.FriendsUtil.getIcon;
import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolTip;
import javax.swing.ListSelectionModel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import net.miginfocom.swing.MigLayout;

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
import org.limewire.ui.swing.sharing.BuddySharingDisplay;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.xmpp.api.client.IncomingChatListener;
import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.Presence.Mode;

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
public class FriendsPane extends JPanel implements BuddyRemover {
    
    private static final Color MEDIUM_GRAY = new Color(183, 183, 183);
    private static final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);
    private static final Cursor HAND_CURSOR = new Cursor(Cursor.HAND_CURSOR);
    private static final int PREFERRED_WIDTH = 120;
    private static final int RIGHT_EDGE_PADDING_PIXELS = 2;
    private static final int RIGHT_ADJUSTED_WIDTH = PREFERRED_WIDTH - RIGHT_EDGE_PADDING_PIXELS;
    private static final int ICON_WIDTH_FUDGE_FACTOR = 4;
    private static final Log LOG = LogFactory.getLog(FriendsPane.class);
    private static final String ALL_CHAT_MESSAGES_TOPIC_PATTERN = MessageReceivedEvent.buildTopic(".*");
    private static final String ALL_PRESENCE_UPDATES_TOPIC_PATTERN = PresenceUpdateEvent.buildTopic(".*");
    private static final Color LIGHT_GREY = new Color(218, 218, 218);
    private static final int TWENTY_MINUTES_IN_MILLIS = 1200000;
    
    private final EventList<Friend> friends;
    private final JTable friendsTable;
    private final IconLibrary icons;
    private final WeakHashMap<String, Friend> idToFriendMap;
    private final WeakHashMap<Friend, AlternatingIconTimer> friendTimerMap;
    private final FriendsCountUpdater friendsCountUpdater;
    private final LibraryManager libraryManager;
    private final BuddySharingDisplay buddySharing;
    private final JScrollPane scrollPane;
    private final Timer idleTimer;
    private final JLabel unseenMessageCountPopupLabel = new JLabel();
    private Popup unseenMessageCountPopup;
    private String myID;
    private WeakReference<Friend> activeConversation = new WeakReference<Friend>(null);
    private FriendHoverBean mouseHoverFriend = new FriendHoverBean();

    @Inject
    public FriendsPane(IconLibrary icons, FriendsCountUpdater friendsCountUpdater, LibraryManager libraryManager, BuddySharingDisplay buddySharing) {
        super(new BorderLayout());
        this.icons = icons;
        this.friends = new BasicEventList<Friend>();
        this.idToFriendMap = new WeakHashMap<String, Friend>();
        this.friendTimerMap = new WeakHashMap<Friend, AlternatingIconTimer>();
        this.friendsCountUpdater = friendsCountUpdater;
        this.libraryManager = libraryManager;
        this.buddySharing = buddySharing;
        ObservableElementList<Friend> observableList = new ObservableElementList<Friend>(friends, GlazedLists.beanConnector(Friend.class));
        SortedList<Friend> sortedFriends = new SortedList<Friend>(observableList,  new FriendAvailabilityComparator());
        friendsTable = createFriendsTable(sortedFriends);
        
        addPopupMenus(friendsTable);
        
        scrollPane = new JScrollPane(friendsTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                showUnseenMessageCountPopup();
            }
        });
        add(scrollPane);
        setPreferredSize(new Dimension(PREFERRED_WIDTH, 200));
        
        EventAnnotationProcessor.subscribe(this);
        
        idleTimer = new IdleTimer();
        idleTimer.start();
        
    }

    private static class IdleTimer extends Timer {
        private SelfAvailabilityAction availabilityAction;
        
        public IdleTimer() {
            this(TWENTY_MINUTES_IN_MILLIS, new SelfAvailabilityAction());
        }
        
        public IdleTimer(int delay, SelfAvailabilityAction listener) {
            super(delay, listener);
            this.availabilityAction = listener;
        }
        
        @Override
        public void restart() {
            super.restart();
            availabilityAction.resetAvailability();
        }
    }
    
    private static class SelfAvailabilityAction implements ActionListener {
        private boolean hasChangedAvailability;

        @Override
        public void actionPerformed(ActionEvent e) {
            new SelfAvailabilityUpdateEvent(Mode.xa).publish();
            hasChangedAvailability = true;
        }
        
        public void resetAvailability() {
            if (hasChangedAvailability) {
                new SelfAvailabilityUpdateEvent(Mode.available).publish();
                hasChangedAvailability = false;
            }
        }
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
    
    private JTable createFriendsTable(final EventList<Friend> friendsList) {
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
                return 1;
            }

            @Override
            public String getColumnName(int column) {
                if (column == 0) {
                    return "name";
                }
                throw new UnsupportedOperationException(tr("Too many columns expected in friends chat table. Tried getting column: ") + column);
            }

            @Override
            public Object getColumnValue(Friend friend, int column) {
                if (column == 0) {
                    return friend.getName();
                }
                throw new UnsupportedOperationException(tr("Couldn't find value for unknown friends chat table column: ") + column);
            }
        };
        
        final JXTable table = new CustomTooltipLocationTable(new EventTableModel<Friend>(friendsList, format)); 
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.addMouseListener(new LaunchChatListener());
        //Add as mouse listener and motion listener because it cares about MouseExit and MouseMove events
        CloseChatListener closeChatListener = new CloseChatListener();
        table.addMouseListener(closeChatListener);
        table.addMouseMotionListener(closeChatListener);
        
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
        
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setCellRenderer(new FriendCellRenderer());
        
        table.setShowGrid(false, false);
        
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
            //Clear the border on the renderer because the same panel is being reused for every cell
            //and it will be set to an underlining border if this method returns true
            JComponent panel = (JComponent)renderer;
            panel.setBorder(BorderFactory.createEmptyBorder());

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
    
    @RuntimeTopicPatternEventSubscriber(methodName="getPresenceUpdateTopicPatternName")
    public void handlePresenceUpdate(String topic, PresenceUpdateEvent event) {
        LOG.debugf("handling presence {0}, {1}", event.getPresence().getJID(), event.getPresence().getType());
        final Presence presence = event.getPresence();
        final User user = event.getUser();
        Friend friend = idToFriendMap.get(user.getId());
        switch(presence.getType()) {
            case available:
                if(friend == null) {
                    final FriendImpl newFriend = new FriendImpl(user, presence);
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
                    idToFriendMap.put(user.getId(), friend);
                }
                friend.setStatus(presence.getStatus());
                friend.setMode(presence.getMode());
                break;
            case unavailable:
                if (friend != null) {
                    friend.releasePresence(presence);
                    Presence newPresence = friend.getPresence();
                    if (newPresence != null) {
                        friend.setStatus(newPresence.getStatus());
                        friend.setMode(newPresence.getMode());
                    } else {
                        friends.remove(idToFriendMap.remove(user.getId()));
                    }
                } 
                break;
        }
        friendsCountUpdater.setFriendsCount(friends.size());
    }
    
    @RuntimeTopicPatternEventSubscriber(methodName="getMessagingTopicPatternName")
    public void handleMessageReceived(String topic, MessageReceivedEvent event) {
        Message message = event.getMessage();
        LOG.debugf("All Messages listener: from {0} text: {1} topic: {2}", message.getSenderName(), message.getMessageText(), topic);
        Friend friend = idToFriendMap.get(message.getFriendID());
        if (!friend.isActiveConversation() && message.getType() != Type.Sent) {
            friend.startChat();
            friend.setReceivingUnviewedMessages(true);
            if (!friendTimerMap.containsKey(friend)) {
                AlternatingIconTimer iconTimer = new AlternatingIconTimer(friend);
                friendTimerMap.put(friend, iconTimer);
                iconTimer.start();
            }
            showUnseenMessageCountPopup();
        }
        
        if (message.getType() == Type.Sent) {
            idleTimer.restart();
        }
    }
    
    public String getMessagingTopicPatternName() {
        return ALL_CHAT_MESSAGES_TOPIC_PATTERN;
    }
    
    public String getPresenceUpdateTopicPatternName() {
        return ALL_PRESENCE_UPDATES_TOPIC_PATTERN;
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
        friendTimerMap.remove(friend);
    }
    
    private Icon getChatIcon(Friend friend) {
        AlternatingIconTimer timer = friendTimerMap.get(friend);
        if (timer != null) {
            return timer.getIcon();
        }
        //Change to chatting icon because gtalk doesn't actually set mode to 'chat', so icon won't show chat bubble normally
        if (friend.isChatting()) {
            return icons.getChatting();
        }
        return getIcon(friend, icons);
    }

    private void closeChat(Friend friend) {
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

    private class FriendCellRenderer implements TableCellRenderer {
        private final JXPanel cell; 
        private final JXLabel friendName;
        private final JXLabel chatStatus;
        private final JXLabel endChat;
        private final RectanglePainter activeConversationPainter;
        
        public FriendCellRenderer() {
            cell = new JXPanel(new MigLayout("insets 0 0 0 0", "3[]4[]0:push[]" + Integer.toString(RIGHT_EDGE_PADDING_PIXELS), "1[]0"));
            activeConversationPainter = new RectanglePainter();
            //light-blue gradient
            activeConversationPainter.setFillPaint(new GradientPaint(50.0f, 0.0f, Color.WHITE, 50.0f, 20.0f, new Color(176, 205, 247)));
            activeConversationPainter.setBorderPaint(Color.WHITE);
            activeConversationPainter.setBorderWidth(0f);
            
            this.friendName = new JXLabel();
            this.chatStatus = new JXLabel();
            this.endChat = new JXLabel();

            FontUtils.changeSize(friendName, -2.0f);
            friendName.setMaximumSize(new Dimension(85, 12));
        }

        @Override
        public final Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            cell.removeAll();
            cell.setBackgroundPainter(null);
            
            Friend friend = getFriend(table, row);
            
            //Handle null possible sent in via AccessibleJTable inner class
            value = value == null ? table.getValueAt(row, column) : value;
            boolean isChatHoveredOver = (friend == mouseHoverFriend.getFriend() && friend.isChatting());
            renderComponent(cell, value, friend, isChatHoveredOver);
            
            if (isSelected || isChatHoveredOver) {
                cell.setBackground(LIGHT_GREY);
            } else if (friend.isActiveConversation()) {
                cell.setBackgroundPainter(activeConversationPainter);
            } else  {
                cell.setBackground(table.getBackground());
                cell.setForeground(table.getForeground());
            }

            return cell;
        }

        protected void renderComponent(JPanel panel, Object value, Friend friend, boolean isChatHoveredOver) {
            chatStatus.setIcon(getChatIcon(friend));
            panel.add(chatStatus);

            friendName.setText(value.toString());
            panel.add(friendName);
            
            if (isChatHoveredOver) {
                Point hoverPoint = mouseHoverFriend.getHoverPoint();
                Icon closeChatIcon = icons.getEndChat();
                boolean overCloseIcon = isOverCloseIcon(hoverPoint);
                endChat.setIcon(overCloseIcon ? icons.getEndChatOverIcon() : closeChatIcon);
                panel.add(endChat);
            } else {
                endChat.setIcon(null);
            }
        }
    }
    
    private class CustomTooltipLocationTable extends JXTable {
        private final Color GRAY_BACKGROUND = new Color(172, 172, 172);

        public CustomTooltipLocationTable(TableModel dm) {
            super(dm);
        }

        @Override
        public Point getToolTipLocation(MouseEvent event) {
            Point location = scrollPane.getLocation();
            int width2 = scrollPane.getWidth();
            return new Point(location.x + width2, event.getPoint().y);
        }

        @Override
        public JToolTip createToolTip() {
            JToolTip tooltip = super.createToolTip();
            tooltip.setBackground(GRAY_BACKGROUND);
            return tooltip;
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            int row = friendsTable.rowAtPoint(event.getPoint());
            if (row == -1) {
                return null;
            }
            EventTableModel model = (EventTableModel) friendsTable.getModel();
            Friend friend = (Friend) model.getElementAt(row);
            StringBuilder tooltip = new StringBuilder();
            tooltip.append("<html>")
                .append("<head>")
                .append("<style>body { margin: 2px 10px 2px 4px;}</style>")
                .append("</head>")
                .append("<body>")
                .append("<img src=\"")
                .append(FriendsUtil.getIconURL(friend.getMode())).append("\"/>&nbsp;")
                .append("<b>").append(friend.getName()).append("</b><br/>");
            String status = friend.getStatus();
            if (status != null && status.length() > 0) {
                tooltip.append("<div color=\"rgb(255,255,255)\">").append(status).append("</div>");
            }
            tooltip.append("</body>")
                .append("</html>");
            return tooltip.toString();
        }
    }
    
    private boolean isOverCloseIcon(Point point) {
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        int scrollBarWidthAdjustment = verticalScrollBar.isVisible() ? verticalScrollBar.getWidth() : 0;
        return point.x > RIGHT_ADJUSTED_WIDTH - icons.getEndChat().getIconWidth() - ICON_WIDTH_FUDGE_FACTOR - scrollBarWidthAdjustment && 
                                    point.x < RIGHT_ADJUSTED_WIDTH;
    }

    private class AlternatingIconTimer {
        private WeakReference<Friend> friendRef;
        private int flashCount;
        
        public AlternatingIconTimer(Friend friend) {
            this.friendRef = new WeakReference<Friend>(friend);
        }

        public void start() {
            Timer timer = new Timer(1500, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    Friend friend = friendRef.get();
                    if (friend == null || flashCount == 4) {
                        stopTimer(e);
                        return;
                    }
                    
                    int friendIndex = friends.indexOf(friend);
                    if (friendIndex > -1) {
                        AbstractTableModel model = (AbstractTableModel) friendsTable.getModel();
                        model.fireTableCellUpdated(friendIndex, 0);
                        flashCount++;
                    }
                }
            });
            timer.start();
        }

        private void stopTimer(ActionEvent e) {
            Timer timer = (Timer) e.getSource();
            timer.stop();
        }
        
        public Icon getIcon() {
            return flashCount % 2 == 0 ? icons.getUnviewedMessages() : icons.getChatting();
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
    
    /**
     * Figures out which row  & friend the mouse is hovering over and asks the table
     * to repaint itself.  This information is needed by the cell renderer to decide
     * whether to paint a close chat icon at the right corner of the cell (if appropriate) 
     */
    private class CloseChatListener extends MouseAdapter {

        @Override
        public void mouseMoved(MouseEvent e) {
            Friend friend = getFriendFromPoint(e);
            
            if (friend == null) {
                //Mouse is not hovering over a friend row
                Friend previousMouseHoverFriend = mouseHoverFriend.getFriend();
                if (previousMouseHoverFriend != null) {
                    //Mouse used to be hovering over a friend, but has moved
                    int previousMouseHoverFriendIndex = friends.indexOf(previousMouseHoverFriend);
                    if (previousMouseHoverFriendIndex > -1) {
                        //Friend is still in the list, but is no longer being hovered over
                        mouseHoverFriend.clearHoverDetails();
                        repaintTableCell(previousMouseHoverFriendIndex);
                    }
                }
                setTableCursor(false);
                return;
            }
            
            int friendIndex = friends.indexOf(friend);
            if (friendIndex > -1) {
                mouseHoverFriend.setHoverDetails(friend, e.getPoint());
                repaintTableCell(friendIndex);
                setTableCursor(isOverCloseIcon(e.getPoint()) && friend.isChatting());
            }
        }

        private void repaintTableCell(int friendIndex) {
            AbstractTableModel model = (AbstractTableModel) friendsTable.getModel();
            model.fireTableCellUpdated(friendIndex, 0);
        }

        private Friend getFriendFromPoint(MouseEvent e) {
            JTable table = (JTable)e.getSource();
            
            return getFriend(table, table.rowAtPoint(e.getPoint()));
        }

        /**
         * Clears the mouseHoverFriend when the mouse leaves the table
         */
        @Override
        public void mouseExited(MouseEvent e) {
            Friend friend = mouseHoverFriend.getFriend();
            if (friend != null) {
                mouseHoverFriend.clearHoverDetails();
                int friendIndex = friends.indexOf(friend);
                repaintTableCell(friendIndex);
            }
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
            if (isOverCloseIcon(e.getPoint())) {
                Friend friend = getFriendFromPoint(e);
                if (e.getClickCount() == 1 && friend != null && friend.isChatting()) {
                    closeChat(friend);
                    setTableCursor(false);
                }
            } 
        }
    }
    
    private static class FriendHoverBean {
        private WeakReference<Friend> friend;
        private Point hoverPoint;
        
        public void clearHoverDetails() {
            setHoverDetails(null, null);
        }
        
        public void setHoverDetails(Friend friend, Point point) {
            this.friend = new WeakReference<Friend>(friend);
            this.hoverPoint = point;
        }

        public Friend getFriend() {
            return friend == null ? null : friend.get();
        }
        
        public Point getHoverPoint() {
            return hoverPoint;
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
    
    private static abstract class AbstractContextAction extends AbstractAction {
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
            Friend friend = context.getFriend();
            if (friend != null) {
                //minimize chat
                new DisplayFriendsEvent(false).publish();
                buddySharing.selectBuddyLibrary(friend.getName());
            }
        }
    }
    
    private class ViewSharedFiles extends AbstractContextAction implements ItemNotifyable {
        public ViewSharedFiles(FriendContext context) {
            super("", context);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            //minimize chat
            new DisplayFriendsEvent(false).publish();
            Friend friend = context.getFriend();
            if (friend != null) {
                buddySharing.selectBuddyInFileSharingList(friend.getID());
            }
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
            int sharedFileCount = 0;
            if (libraryManager.containsBuddy(friend.getID())) {
                FileList sharedFileList = libraryManager.getBuddy(friend.getID());
                sharedFileCount = sharedFileList == null ? 0 : sharedFileList.size();
            }
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
            removeBuddy(context.getFriend());
        }
    }
    
    private class CloseChat extends AbstractContextAction {
        public CloseChat(FriendContext context) {
            super("Close chat (closes the current chat window)", context);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Friend friend = context.getFriend();
            closeChat(friend);
        }
    }

    @Override
    public boolean canRemoveSelectedBuddy() {
        Friend selectedFriend = getSelectedFriend();
        return selectedFriend != null && !selectedFriend.isChatting();
    }

    @Override
    public void removeSelectedBuddy() {
        removeBuddy(getSelectedFriend());
    }

    private void removeBuddy(Friend selectedFriend) {
        if (selectedFriend != null) {
            friends.remove(selectedFriend);
            idToFriendMap.remove(selectedFriend.getID());
            new RemoveBuddyEvent(selectedFriend).publish();
        }
    }
    
    private Friend getSelectedFriend() {
        int selectedRow = friendsTable.getSelectedRow();
        if (selectedRow > -1) {
            return getFriend(friendsTable, selectedRow);
        }
        return null;
    }
    
    public boolean isSharingFilesWithFriends() {
        for(Friend friend : friends) {
            if (libraryManager.getBuddy(friend.getID()).size() > 0) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasFriendsOnLimeWire() {
        for(Friend friend : friends) {
            if (friend.isSignedInToLimewire()) {
                return true;
            }
        }
        return false;
    }

    private void setTableCursor(boolean useHandCursor) {
        friendsTable.setCursor(useHandCursor ? HAND_CURSOR : DEFAULT_CURSOR);
    }

    private void showUnseenMessageCountPopup() {
        if (unseenMessageCountPopup != null) {
            unseenMessageCountPopup.hide();
        }

        int firstVisibleRow = friendsTable.rowAtPoint(new Point(0, scrollPane.getViewport().getViewRect().y));
        int unseenMessageFriendsCount = 0;
        EventTableModel model = (EventTableModel) friendsTable.getModel();
        for(int row = 0; row < firstVisibleRow; row++) {
            Friend friend = (Friend) model.getElementAt(row);
            if (friend.isReceivingUnviewedMessages()) {
                unseenMessageFriendsCount++;
            }
        }
        
        if (unseenMessageFriendsCount > 0) {
            Point location = scrollPane.getLocationOnScreen();
            unseenMessageCountPopupLabel.setText("+" + Integer.toString(unseenMessageFriendsCount));
            unseenMessageCountPopupLabel.setBackground(MEDIUM_GRAY);
            int popupX = location.x + (scrollPane.getWidth() / 2);
            int popupY = location.y + 5;
            PopupFactory factory = PopupFactory.getSharedInstance();
            unseenMessageCountPopup = factory.getPopup(friendsTable, unseenMessageCountPopupLabel, popupX, popupY);
            unseenMessageCountPopup.show();
        }
    }
}
