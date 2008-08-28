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
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.border.DropShadowBorder;
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
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.EventListModel;

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
    private static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(1, 1, 1, 1);

    @Inject
    public FriendsPane(IconLibrary icons, FriendsCountUpdater friendsCountUpdater, LibraryManager libraryManager) {
        super(new BorderLayout());
        friends = new BasicEventList<Friend>();
        idToFriendMap = new WeakHashMap<String, FriendImpl>();
        this.friendsCountUpdater = friendsCountUpdater;
        this.libraryManager = libraryManager;
        ObservableElementList<Friend> observableList = new ObservableElementList<Friend>(friends, GlazedLists.beanConnector(Friend.class));
        SortedList<Friend> sortedFriends = new SortedList<Friend>(observableList,  new FriendAvailabilityComparator());
        JList list = newSearchableJList(sortedFriends);
        
        addPopupMenus(list);
        
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new FriendCellRenderer(icons));
        JScrollPane scroll = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll);
        setPreferredSize(new Dimension(120, 200));
        
        list.addMouseListener(new LaunchChatListener());
        
        EventAnnotationProcessor.subscribe(this);
    }

    private void addPopupMenus(JList list) {
        FriendContext context = new FriendContext();
        ViewLibrary viewLibrary = new ViewLibrary(context);
        ViewSharedFiles viewSharedFiles = new ViewSharedFiles(context);
        JPopupMenu nonChattingPopup = PopupUtil.addPopupMenus(list, new FriendPopupDecider(false, context), new OpenChat(context));
        nonChattingPopup.addSeparator();
        nonChattingPopup.add(viewLibrary);
        nonChattingPopup.add(viewSharedFiles);
        nonChattingPopup.addSeparator();
        nonChattingPopup.add(new RemoveBuddy(context));
        
        JPopupMenu chattingPopup = PopupUtil.addPopupMenus(list, new FriendPopupDecider(true, context), viewLibrary, viewSharedFiles);
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
            JList list = (JList) e.getComponent();
            int index = list.locationToIndex(e.getPoint());
            if (index < 0) {
                return false;
            }
            //Popup selects the item (as per spec)
            list.setSelectedIndex(index);
            
            Friend friend = (Friend) list.getModel().getElementAt(index);
            context.setFriend(friend);
            return friend.isChatting() == expected;
        }
    }
    
    private JList newSearchableJList(EventList<Friend> friendsList) {
        TextFilterator<Friend> friendFilterator = new TextFilterator<Friend>() {
            @Override
            public void getFilterStrings(List<String> baseList, Friend element) {
                baseList.add(element.getName());
            }
        };
        
        final TextMatcherEditor<Friend> editor = new TextMatcherEditor<Friend>(friendFilterator);
        final FilterList<Friend> filter = new FilterList<Friend>(friendsList, editor);
        
        final JList list = new JList(new EventListModel<Friend>(friendsList)); 
        
        list.addKeyListener(new KeyAdapter() {
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
                    list.setSelectedValue(firstFriend, true);
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
        
        return list;
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

    private class FriendCellRenderer implements ListCellRenderer {
        private final IconLibrary icons;
        
        public FriendCellRenderer(IconLibrary icons) {
            this.icons = icons;
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JXPanel cell = new JXPanel(new MigLayout("insets 0 2 0 1", "3[]6[]push[]", "1[]1"));
            
            Friend friend = (Friend) value;
            
            JLabel friendIcon = new JLabel(getIcon(friend, icons));
            cell.add(friendIcon);
            
            JLabel friendName = new JLabel(friend.getName());
            FontUtils.changeSize(friendName, -2.8f);
            friendName.setMaximumSize(new Dimension(85, 12));
            cell.add(friendName);
            
            Border border = EMPTY_BORDER;
            
            if (friend.isChatting()) {
                //Change to chatting icon because gtalk doesn't actually set mode to 'chat', so icon won't show chat bubble normally  
                friendIcon.setIcon(friend.isReceivingUnviewedMessages() ? icons.getUnviewedMessages() : icons.getChatting());
                
                //FIXME:  This isn't exactly the right behavior. end chat icon should only 
                //appear on hover during a chat.
                if (cellHasFocus) {
                    cell.add(new JLabel(icons.getEndChat()));
                }

                ListModel model = list.getModel();
                if (model.getSize() > (index + 1)) {
                    Friend nextFriend = (Friend) model.getElementAt(index + 1);
                    if (!nextFriend.isChatting()) {
                        //Light-grey separator line between last chatting friend and non-chatting friends
                        border = new DropShadowBorder(new Color(194, 194, 194), 1, 1.0f, 1, false, false, true, false);
                    }
                }
            }
            
            cell.setComponentOrientation(list.getComponentOrientation());
            
            cell.setEnabled(list.isEnabled());

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
                cell.setBackground(list.getBackground());
                cell.setForeground(list.getForeground());
            }
            
            cell.setBorder(border);
            
            return cell;
        }
    }

    private class LaunchChatListener extends MouseAdapter {
        
        @Override
        public void mouseClicked(MouseEvent e) {
            JList list = (JList)e.getSource();
            Friend friend = (Friend)list.getSelectedValue();
            
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
            
        }
    }
    
    private class ViewSharedFiles extends AbstractContextAction implements ItemNotifyable {
        public ViewSharedFiles(FriendContext context) {
            super("", context);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            
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
            
        }
    }
    
    private class CloseChat extends AbstractContextAction {
        public CloseChat(FriendContext context) {
            super("Close chat (closes the current chat window)", context);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            
        }
    }
}
