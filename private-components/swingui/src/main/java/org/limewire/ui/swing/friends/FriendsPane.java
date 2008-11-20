package org.limewire.ui.swing.friends;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;

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

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.border.DropShadowBorder;
import org.jdesktop.swingx.decorator.BorderHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.action.ItemNotifyable;
import org.limewire.ui.swing.action.PopupDecider;
import org.limewire.ui.swing.action.PopupUtil;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.event.RuntimeTopicPatternEventSubscriber;
import static org.limewire.ui.swing.friends.FriendsUtil.getIcon;
import org.limewire.ui.swing.friends.Message.Type;
import org.limewire.ui.swing.sharing.FriendSharingDisplay;
import org.limewire.ui.swing.table.AbstractTableFormat;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.I18n;
import static org.limewire.ui.swing.util.I18n.tr;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.Presence.Mode;

import com.google.inject.Inject;
import com.google.inject.Singleton;

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
import net.miginfocom.swing.MigLayout;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
@Singleton
public class FriendsPane extends JPanel {
    
    private static final Color MEDIUM_GRAY = new Color(183, 183, 183);
    private static final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);
    private static final Cursor HAND_CURSOR = new Cursor(Cursor.HAND_CURSOR);
    private static final int PREFERRED_WIDTH = 120;
    private static final int RIGHT_EDGE_PADDING_PIXELS = 2;
    private static final int RIGHT_ADJUSTED_WIDTH = PREFERRED_WIDTH - RIGHT_EDGE_PADDING_PIXELS;
    private static final int ICON_WIDTH_FUDGE_FACTOR = 4;
    private static final Log LOG = LogFactory.getLog(FriendsPane.class);
    private static final String ALL_CHAT_MESSAGES_TOPIC_PATTERN = MessageReceivedEvent.buildTopic(".*");
    private static final Color LIGHT_GREY = new Color(218, 218, 218);
    private static final int TWENTY_MINUTES_IN_MILLIS = 1200000;
    
    private final EventList<ChatFriend> chatFriends;
    private final JTable friendsTable;
    private final IconLibrary icons;
    private final Map<String, ChatFriend> idToFriendMap;
    private final WeakHashMap<ChatFriend, AlternatingIconTimer> friendTimerMap;
    private final FriendsCountUpdater friendsCountUpdater;
    private final ShareListManager libraryManager;
    private final FriendSharingDisplay friendSharing;
    private final JScrollPane scrollPane;
    private final IdleTimer idleTimer;
    private final JLabel unseenMessageCountPopupLabel = new JLabel();
    private Popup unseenMessageCountPopup;
    private String myID;
    private WeakReference<ChatFriend> activeConversation = new WeakReference<ChatFriend>(null);
    private FriendHoverBean mouseHoverFriend = new FriendHoverBean();

    @Inject
    public FriendsPane(IconLibrary icons, FriendsCountUpdater friendsCountUpdater,
            ShareListManager libraryManager, FriendSharingDisplay friendSharing,
            ListenerSupport<FriendPresenceEvent> presenceSupport) {
        super(new BorderLayout());
        this.icons = icons;
        this.chatFriends = new BasicEventList<ChatFriend>();
        this.idToFriendMap = new HashMap<String, ChatFriend>();
        this.friendTimerMap = new WeakHashMap<ChatFriend, AlternatingIconTimer>();
        this.friendsCountUpdater = friendsCountUpdater;
        this.libraryManager = libraryManager;
        this.friendSharing = friendSharing;
        ObservableElementList<ChatFriend> observableList = GlazedListsFactory.observableElementList(chatFriends, GlazedLists.beanConnector(ChatFriend.class));
        SortedList<ChatFriend> sortedFriends = GlazedListsFactory.sortedList(observableList,  new FriendAvailabilityComparator());
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
        presenceSupport.addListener(new EventListener<FriendPresenceEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendPresenceEvent event) {
                handlePresenceEvent(event);
            }
        });
        
        idleTimer = new IdleTimer();
        idleTimer.start();
        
    }

    @EventSubscriber
    public void handlePresenceChange(SelfAvailabilityUpdateEvent event) {
        idleTimer.setCurrentMode(event.getNewMode());
    }

    @EventSubscriber
    public void handleSignoff(SignoffEvent event) {
        closeAllChats();
    }    

    private static class IdleTimer extends Timer {
        private AutomatedAvailabilityAction availabilityAction;
        
        public IdleTimer() {
            this(TWENTY_MINUTES_IN_MILLIS, new AutomatedAvailabilityAction());
        }
        
        public IdleTimer(int delay, AutomatedAvailabilityAction listener) {
            super(delay, listener);
            this.availabilityAction = listener;
        }
        
        @Override
        public void restart() {
            super.restart();
            availabilityAction.resetAvailability();
        }

        public void setCurrentMode(Mode newMode) {
            availabilityAction.setCurrentMode(newMode);
        }
    }
    
    private static class AutomatedAvailabilityAction implements ActionListener {
        private AtomicReference<Mode> currentMode = new AtomicReference<Mode>();

        @Override
        public void actionPerformed(ActionEvent e) {
            Mode mode = currentMode.get();
            if(mode != Mode.dnd) {
                new SelfAvailabilityUpdateEvent(Mode.xa).publish();
            }
        }
        
        public void resetAvailability() {
            Mode mode = currentMode.get();
            if(mode != Mode.available) {
                new SelfAvailabilityUpdateEvent(Mode.available).publish();
            }
        }

        public void setCurrentMode(Mode newMode) {
            currentMode.set(newMode);
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
            
            ChatFriend chatFriend = getFriend(table, index);
            
            context.setFriend(chatFriend);
            return chatFriend.isChatting() == expected;
        }
    }
    
    private static ChatFriend getFriend(JTable table, int index) {
        EventTableModel model = (EventTableModel)table.getModel();
        return index < 0 ? null : (ChatFriend) model.getElementAt(index);
    }
    
    private JTable createFriendsTable(final EventList<ChatFriend> friendsList) {
        TextFilterator<ChatFriend> friendFilterator = new TextFilterator<ChatFriend>() {
            @Override
            public void getFilterStrings(List<String> baseList, ChatFriend element) {
                baseList.add(element.getName());
            }
        };
        
        final TextMatcherEditor<ChatFriend> editor = new TextMatcherEditor<ChatFriend>(friendFilterator);
        final FilterList<ChatFriend> filter = GlazedListsFactory.filterList(friendsList, editor);
        
        TableFormat<ChatFriend> format = new AbstractTableFormat<ChatFriend>(tr("Name")) {
            @Override
            public Object getColumnValue(ChatFriend chatFriend, int column) {
                if (column == 0) {
                    return chatFriend.getName();
                }
                throw new IllegalArgumentException("Couldn't find value for unknown friends chat table column: " + column);
            }
        };
        
        final JXTable table = new CustomTooltipLocationTable(new EventTableModel<ChatFriend>(friendsList, format)); 
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
                    ChatFriend firstFriend = filter.get(0);
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
            ChatFriend chatFriend = getFriend(table, row);
            
            if (chatFriend.isChatting()) {
                EventTableModel model = (EventTableModel) table.getModel();
                if (model.getRowCount() > (row + 1)) {
                    ChatFriend nextFriend = (ChatFriend) model.getElementAt(row + 1);
                    if (!nextFriend.isChatting()) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
    
    private void handlePresenceEvent(FriendPresenceEvent event) {
        LOG.debugf("handling presence event {0}", event);
        FriendPresence presence = event.getSource();
        Friend friend = presence.getFriend();
        ChatFriend chatFriend = idToFriendMap.get(friend.getId());
        switch(event.getType()) {
        case ADDED:
        case UPDATE:
            if(chatFriend == null) {
                chatFriend = new ChatFriendImpl((Presence)presence, myID);
                chatFriends.add(chatFriend);
                idToFriendMap.put(friend.getId(), chatFriend);
            }
            chatFriend.update();
            break;
        case REMOVED:
            if (chatFriend != null) {
                if (shouldRemoveFromFriendsList(chatFriend)) {
                    chatFriends.remove(idToFriendMap.remove(friend.getId()));
                }
                chatFriend.update();
            }
            break;
        }
        friendsCountUpdater.setFriendsCount(chatFriends.size());
    }

    @RuntimeTopicPatternEventSubscriber(methodName="getMessagingTopicPatternName")
    public void handleMessageReceived(String topic, MessageReceivedEvent event) {
        Message message = event.getMessage();
        LOG.debugf("All Messages listener: from {0} text: {1} topic: {2}", message.getSenderName(), message.toString(), topic);
        ChatFriend chatFriend = idToFriendMap.get(message.getFriendID());
        if (!chatFriend.isActiveConversation() && message.getType() != Type.Sent) {
            chatFriend.setReceivingUnviewedMessages(true);
            if (!friendTimerMap.containsKey(chatFriend)) {
                AlternatingIconTimer iconTimer = new AlternatingIconTimer(chatFriend);
                friendTimerMap.put(chatFriend, iconTimer);
                iconTimer.start();
            }
            showUnseenMessageCountPopup();
        }
        
        if (message.getType() == Type.Sent) {
            idleTimer.restart();
        }
    }

    /**
     * Remove from the friends list only when:
     *
     * 1. The user (buddy) associated with the chatfriend is no longer signed in, AND
     * 2. The chat has been closed (by clicking on the "x" on the friend in the friend's list)
     *
     * @param chatFriend the ChatFriend to decide whether to remove (no null check)
     * @return true if chatFriend should be removed.
     */
    private boolean shouldRemoveFromFriendsList(ChatFriend chatFriend) {
        return (!chatFriend.isChatting()) && (!chatFriend.isSignedIn());
    }
    
    public String getMessagingTopicPatternName() {
        return ALL_CHAT_MESSAGES_TOPIC_PATTERN;
    }
    
    public void setLoggedInID(String id) {
        this.myID = id;
    }

    String getLoggedInID() {
        return myID;
    }
    
    public void fireConversationStarted(Friend friend) {
        ChatFriend chatFriend = idToFriendMap.get(friend.getId());
        if(chatFriend != null) {
            startOrSelectConversation(chatFriend);
            new DisplayFriendsToggleEvent(Boolean.TRUE).publish();
        } else {
            //TODO notify that chat no longer possible.
        }
    }

    private void startOrSelectConversation(ChatFriend chatFriend) {
        MessageWriter writerWithEventDispatch = null;
        if (!chatFriend.isChatting() && chatFriend.isSignedIn()) {
            MessageWriter writer = chatFriend.createChat(new MessageReaderImpl(chatFriend));
            writerWithEventDispatch = new MessageWriterImpl(myID, chatFriend, writer);
        }
        new ConversationSelectedEvent(chatFriend, writerWithEventDispatch, true).publish();
        setActiveConversation(chatFriend);
    }

    private void setActiveConversation(ChatFriend chatFriend) {
        LOG.debugf("Setting active conversation: {0}", chatFriend.getName());
        ChatFriend oldActiveConversation = activeConversation.get();
        if (oldActiveConversation != null) {
            oldActiveConversation.setActiveConversation(false);
        }
        activeConversation = new WeakReference<ChatFriend>(chatFriend);
        chatFriend.setReceivingUnviewedMessages(false);
        chatFriend.setActiveConversation(true);
        friendTimerMap.remove(chatFriend);
    }
    
    private Icon getChatIcon(ChatFriend chatFriend) {
        AlternatingIconTimer timer = friendTimerMap.get(chatFriend);
        if (timer != null) {
            return timer.getIcon();
        }
        //Change to chatting icon because gtalk doesn't actually set mode to 'chat', so icon won't show chat bubble normally
        if (chatFriend.isChatting()) {
            return icons.getChatting();
        }
        return getIcon(chatFriend, icons);
    }

    private void closeChat(ChatFriend chatFriend) {
        if (chatFriend != null) {

            chatFriend.stopChat();
            if (!chatFriend.isSignedIn()) {
                chatFriends.remove(idToFriendMap.remove(chatFriend.getID()));
            }
            new CloseChatEvent(chatFriend).publish();
            
            ChatFriend nextFriend = null;
            for(ChatFriend tmpFriend : chatFriends) {
                if (tmpFriend == chatFriend || !tmpFriend.isChatting()) {
                    continue;
                }
                if (nextFriend == null || tmpFriend.getChatStartTime() > nextFriend.getChatStartTime()) {
                    nextFriend = tmpFriend;
                }
            }

            if (nextFriend != null) {
                startOrSelectConversation(nextFriend);
            }
        }
    }

    private void closeAllChats() {
        for (ChatFriend chatFriend : chatFriends) {
            String userId = chatFriend.getID();
            if (idToFriendMap.get(userId) != null) {
                idToFriendMap.remove(userId);
            }
            chatFriend.stopChat();
            new CloseChatEvent(chatFriend).publish();
        }
        chatFriends.clear();
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
            
            ChatFriend chatFriend = getFriend(table, row);
            
            //Handle null possible sent in via AccessibleJTable inner class
            value = value == null ? table.getValueAt(row, column) : value;
            boolean isChatHoveredOver = (chatFriend == mouseHoverFriend.getFriend() && chatFriend.isChatting());
            renderComponent(cell, value, chatFriend, isChatHoveredOver);
            
            if (isSelected || isChatHoveredOver) {
                cell.setBackground(LIGHT_GREY);
            } else if (chatFriend.isActiveConversation()) {
                cell.setBackgroundPainter(activeConversationPainter);
            } else  {
                cell.setBackground(table.getBackground());
                cell.setForeground(table.getForeground());
            }

            return cell;
        }

        protected void renderComponent(JPanel panel, Object value, ChatFriend chatFriend, boolean isChatHoveredOver) {
            chatStatus.setIcon(getChatIcon(chatFriend));
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
            ChatFriend chatFriend = (ChatFriend) model.getElementAt(row);
            StringBuilder tooltip = new StringBuilder();
            tooltip.append("<html>")
                .append("<head>")
                .append("<style>body { margin: 2px 10px 2px 4px;}</style>")
                .append("</head>")
                .append("<body>")
                .append("<img src=\"")
                .append(FriendsUtil.getIconURL(chatFriend.getMode())).append("\"/>&nbsp;")
                .append("<b>").append(chatFriend.getName()).append("</b><br/>");
            String status = chatFriend.getStatus();
            if (status != null && status.length() > 0) {
                //using width to limit the size of the tooltip, unfortunatley looks like max-width does not work 
                tooltip.append("<div color=\"rgb(255,255,255)\" style=\"width: 300px;\">").append(status).append("</div>");
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
        private WeakReference<ChatFriend> friendRef;
        private int flashCount;
        
        public AlternatingIconTimer(ChatFriend chatFriend) {
            this.friendRef = new WeakReference<ChatFriend>(chatFriend);
        }

        public void start() {
            Timer timer = new Timer(1500, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    ChatFriend chatFriend = friendRef.get();
                    if (chatFriend == null || flashCount == 4) {
                        stopTimer(e);
                        return;
                    }
                    
                    int friendIndex = chatFriends.indexOf(chatFriend);
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
            
            ChatFriend chatFriend = getFriend(table, table.rowAtPoint(e.getPoint()));
            
            if (chatFriend == null) {
                return;
            }
            
            if (e.getClickCount() == 2 || chatFriend.isChatting()) {
                startOrSelectConversation(chatFriend);
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
            ChatFriend friend = getFriendFromPoint(e);
            
            if (friend == null) {
                //Mouse is not hovering over a friend row
                ChatFriend previousMouseHoverFriend = mouseHoverFriend.getFriend();
                if (previousMouseHoverFriend != null) {
                    //Mouse used to be hovering over a friend, but has moved
                    int previousMouseHoverFriendIndex = chatFriends.indexOf(previousMouseHoverFriend);
                    if (previousMouseHoverFriendIndex > -1) {
                        //Friend is still in the list, but is no longer being hovered over
                        mouseHoverFriend.clearHoverDetails();
                        repaintTableCell(previousMouseHoverFriendIndex);
                    }
                }
                setTableCursor(false);
                return;
            }
            
            int friendIndex = chatFriends.indexOf(friend);
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

        private ChatFriend getFriendFromPoint(MouseEvent e) {
            JTable table = (JTable)e.getSource();
            
            return getFriend(table, table.rowAtPoint(e.getPoint()));
        }

        /**
         * Clears the mouseHoverFriend when the mouse leaves the table
         */
        @Override
        public void mouseExited(MouseEvent e) {
            ChatFriend chatFriend = mouseHoverFriend.getFriend();
            if (chatFriend != null) {
                mouseHoverFriend.clearHoverDetails();
                int friendIndex = chatFriends.indexOf(chatFriend);
                repaintTableCell(friendIndex);
            }
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
            if (isOverCloseIcon(e.getPoint())) {
                ChatFriend chatFriend = getFriendFromPoint(e);
                if (e.getClickCount() == 1 && chatFriend != null && chatFriend.isChatting()) {
                    closeChat(chatFriend);
                    setTableCursor(false);
                }
            } 
        }
    }
    
    private static class FriendHoverBean {
        private WeakReference<ChatFriend> chatFriend;
        private Point hoverPoint;
        
        public void clearHoverDetails() {
            setHoverDetails(null, null);
        }
        
        public void setHoverDetails(ChatFriend chatFriend, Point point) {
            this.chatFriend = new WeakReference<ChatFriend>(chatFriend);
            this.hoverPoint = point;
        }

        public ChatFriend getFriend() {
            return chatFriend == null ? null : chatFriend.get();
        }
        
        public Point getHoverPoint() {
            return hoverPoint;
        }
    }
    
    private class FriendContext {
        private WeakReference<ChatFriend> weakFriend;
        
        public ChatFriend getFriend() {
            return weakFriend == null ? null : weakFriend.get();
        }
        
        public void setFriend(ChatFriend chatFriend) {
            weakFriend = new WeakReference<ChatFriend>(chatFriend);
        }
    }
    
    private static abstract class AbstractContextAction extends AbstractAction {
        protected final FriendContext context;
        
        public AbstractContextAction(String name, FriendContext context) {
            super(name);
            this.context = context;
        }
    }
    
    private class OpenChat extends AbstractContextAction {
        public OpenChat(FriendContext context) {
            super(I18n.tr("Open Chat"), context);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ChatFriend chatFriend = context.getFriend();
            if (chatFriend != null) {
                startOrSelectConversation(chatFriend);
            }
        }
    }
    
    private class ViewLibrary extends AbstractContextAction {
        public ViewLibrary(FriendContext context) {
            super(I18n.tr("View Library"), context);
        }
        
        @Override
        public boolean isEnabled() {
            ChatFriend chatFriend = context.getFriend();
            return chatFriend != null && chatFriend.isSignedInToLimewire();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ChatFriend chatFriend = context.getFriend();
            if (chatFriend != null) {
                //minimize chat
                new DisplayFriendsEvent(false).publish();
                friendSharing.selectFriendLibrary(chatFriend.getFriend());
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
            ChatFriend chatFriend = context.getFriend();
            if (chatFriend != null) {
                friendSharing.selectFriendInFileSharingList(chatFriend.getFriend());
            }
        }

        @Override
        public boolean isEnabled() {
            ChatFriend chatFriend = context.getFriend();
            
            if (chatFriend == null) {
                return false;
            }
            
            return getSharedFileCount(chatFriend) > 0 && chatFriend.isSignedInToLimewire();
        }

        @Override
        public void notifyItem(JMenuItem item) {
            ChatFriend chatFriend = context.getFriend();
            
            if (chatFriend == null) {
                return;
            }
            
            int sharedFileCount = getSharedFileCount(chatFriend);
            item.setText(tr("View Files I'm sharing with them ({0})", sharedFileCount));
            if (sharedFileCount == 0) {
                item.setToolTipText(tr("{0} isn't using LimeWire. Tell them about it to see their Library", chatFriend.getName()));
            }
        }

        private int getSharedFileCount(ChatFriend chatFriend) {
            if (!chatFriend.isSignedInToLimewire()) {
                return 0;
            }
            int sharedFileCount = 0;
            FileList sharedFileList = libraryManager.getOrCreateFriendShareList(chatFriend.getFriend());
            sharedFileCount = sharedFileList == null ? 0 : sharedFileList.size();
            return sharedFileCount;
        }
    }
    
    private class CloseChat extends AbstractContextAction {
        public CloseChat(FriendContext context) {
            super(I18n.tr("Close chat (closes the current chat window)"), context);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ChatFriend chatFriend = context.getFriend();
            closeChat(chatFriend);
        }
    }

    public boolean isSharingFilesWithFriends() {
        for(ChatFriend chatFriend : chatFriends) {
            if (libraryManager.getOrCreateFriendShareList(chatFriend.getFriend()).size() > 0) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasFriendsOnLimeWire() {
        for(ChatFriend chatFriend : chatFriends) {
            if (chatFriend.isSignedInToLimewire()) {
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
            ChatFriend chatFriend = (ChatFriend) model.getElementAt(row);
            if (chatFriend.isReceivingUnviewedMessages()) {
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
