package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.friends.FriendsUtil.getIcon;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.WeakHashMap;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.bushe.swing.event.annotation.EventTopicPatternSubscriber;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.xmpp.api.client.IncomingChatListener;
import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.Presence.Mode;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
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
        SortedList<Friend> sortedObservables = new SortedList<Friend>(observableList,  new FriendAvailabilityComparator());
        JList list = new JList(new EventListModel<Friend>(sortedObservables));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new FriendCellRenderer(icons));
        JScrollPane scroll = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll);
        setPreferredSize(new Dimension(120, 200));
        
        list.addMouseListener(new LaunchChatListener());
        
        EventAnnotationProcessor.subscribe(this);
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
            cell.add(new JLabel(getIcon(friend, icons)));
            
            JLabel friendName = new JLabel();
            friendName.setMaximumSize(new Dimension(85, 12));
            cell.add(friendName);
            
            friendName.setText(friend.getName());
            friendName.setFont(list.getFont());

            cell.add(new JLabel(icons.getEndChat()));
            
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
            cell.setBorder(EMPTY_BORDER);
            
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
