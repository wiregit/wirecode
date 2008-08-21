package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.friends.FriendsUtil.getIcon;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.bushe.swing.event.annotation.EventTopicPatternSubscriber;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.border.DropShadowBorder;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.xmpp.api.client.IncomingChatListener;
import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.Presence.Mode;

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
    
    private EventList<Friend> friends;
    private String myID;
    private final WeakHashMap<String, FriendImpl> idToFriendMap;

    @Inject
    public FriendsPane(IconLibrary icons) {
        super(new BorderLayout());
        friends = new BasicEventList<Friend>();
        idToFriendMap = new WeakHashMap<String, FriendImpl>();
        ObservableElementList<Friend> observableList = new ObservableElementList<Friend>(friends, GlazedLists.beanConnector(Friend.class));
        SortedList<Friend> sortedFriends = new SortedList<Friend>(observableList,  new FriendAvailabilityComparator());
        JList list = newSearchableJList(sortedFriends);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new FriendCellRenderer(icons));
        JScrollPane scroll = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll);
        setPreferredSize(new Dimension(120, 200));
        
        list.addMouseListener(new LaunchChatListener());
        
        EventAnnotationProcessor.subscribe(this);
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
                            LOG.debugf("incomingChat started from: {0}", presence.getJID());
                            MessageWriter writerWrapper = new MessageWriterImpl(myID, newFriend, writer);
                            ConversationStartedEvent event = new ConversationStartedEvent(newFriend, writerWrapper);
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
    }
    
    @EventTopicPatternSubscriber(topicPattern=MessageReceivedEvent.TOPIC_PREFIX + ".*")
    public void handleMessageReceived(String topic, MessageReceivedEvent event) {
        LOG.debugf("Message: from {0} text: {1}", event.getMessage().getSenderName(), event.getMessage().getMessageText());
    }
    
    public void setLoggedInID(String id) {
        this.myID = id;
    }
    
    private class FriendCellRenderer implements ListCellRenderer {
        private final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(1, 1, 1, 1);
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
                friendIcon.setIcon(icons.getChatting());
                
                //FIXME:  This isn't exactly the right behavior. end chat icon should only 
                //appear on hover during a chat.
                if (cellHasFocus) {
                    cell.add(new JLabel(icons.getEndChat()));
                }

                ListModel model = list.getModel();
                if (model.getSize() > (index + 1)) {
                    Friend nextFriend = (Friend) model.getElementAt(index + 1);
                    if (!nextFriend.isChatting()) {
                        //Light-grey
                        border = new DropShadowBorder(new Color(194, 194, 194), 1, 1.0f, 1, false, false, true, false);
                    }
                }
            }
            
            cell.setComponentOrientation(list.getComponentOrientation());
            
            cell.setEnabled(list.isEnabled());

            if (isSelected) {
                if (friend.getMode() == Mode.chat) {
                    RectanglePainter painter = new RectanglePainter();
                    //light-blue gradient
                    painter.setFillPaint(new GradientPaint(50.0f, 0.0f, Color.WHITE, 50.0f, 20.0f, new Color(176, 205, 247)));
                    painter.setBorderPaint(Color.WHITE);
                    painter.setBorderWidth(0f);
                    cell.setBackgroundPainter(painter);
                } else {
                    cell.setBackground(new Color(218, 218, 218));
                }
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
            if (e.getClickCount() == 2) {
                JList list = (JList)e.getSource();
                Friend friend = (Friend)list.getSelectedValue();
                MessageWriter writer = friend.createChat(new MessageReaderImpl(friend));
                MessageWriter writerWrapper = new MessageWriterImpl(myID, friend, writer);
                new ConversationStartedEvent(friend, writerWrapper).publish();
             }
        }
    }
}
