package org.limewire.ui.swing.friends;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.WeakHashMap;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.xmpp.PresenceUpdateEvent;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.Presence.Mode;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.EventListModel;
import net.miginfocom.swing.MigLayout;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public class FriendsPane extends JPanel {
    
    private static final Log LOG = LogFactory.getLog(FriendsPane.class);
    
    private EventList<Friend> friends;
    private final WeakHashMap<String, FriendImpl> idToFriendMap;    

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
        
        AnnotationProcessor.process(this);
    }
    
    @EventSubscriber
    public void handlePresenceUpdate(PresenceUpdateEvent event) {
        LOG.debugf("handling presence {0}, {1}", event.getPresence().getJID(), event.getPresence().getType());
        Presence presence = event.getPresence();
        FriendImpl friend = idToFriendMap.get(presence.getJID());
        switch(presence.getType()) {
            case available:
                if(friend == null) {
                    friend = new FriendImpl(event.getUser(), presence);
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
    
    @EventSubscriber
    public void handleMessageReceived(MessageReceivedEvent event) {
        //TODO
    }
    
    private static class FriendCellRenderer implements ListCellRenderer {
        private static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(1, 1, 1, 1);
        private final IconLibrary icons;
        
        public FriendCellRenderer(IconLibrary icons) {
            this.icons = icons;
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JXPanel cell = new JXPanel(new MigLayout("insets 0 2 0 1", "3[]6[]push[]", "1[]1"));
            Friend friend = (Friend) value;
            cell.add(new JLabel(getIcon(friend)));
            
            JLabel friendName = new JLabel();
            cell.add(friendName);
            
            friendName.setText(friend.getName());
            friendName.setFont(list.getFont());

            cell.add(new JLabel(icons.getEndChat()));
            
            cell.setComponentOrientation(list.getComponentOrientation());
            
            cell.setEnabled(list.isEnabled());

            if (isSelected) {
                if (friend.getMode() == Mode.chat) {
                    RectanglePainter painter = new RectanglePainter();
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
        
        private Icon getIcon(Friend friend) {
            switch(friend.getMode()) {
            case available:
                return icons.getAvailable();
            case chat:
                return icons.getChatting();
            case dnd:
                return icons.getDoNotDisturb();
            }
            return icons.getAway();
        }
    }

    private static class LaunchChatListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                JList list = (JList)e.getSource();
                Friend friend = (Friend)list.getSelectedValue();
                new ConversationStartedEvent(friend).publish();
             }
        }
    }
}
