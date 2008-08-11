package org.limewire.ui.swing.friends;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.xmpp.api.client.Presence.Mode;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.EventListModel;

import com.google.inject.Inject;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
class FriendsPane extends JPanel {
    private EventList<FriendImpl> friends;
    
    @Inject
    public FriendsPane(IconLibrary icons) {
        super(new BorderLayout());
        friends = new BasicEventList<FriendImpl>();
        ObservableElementList<FriendImpl> observableList = new ObservableElementList<FriendImpl>(friends, GlazedLists.beanConnector(FriendImpl.class));
        SortedList<FriendImpl> sortedObservables = new SortedList<FriendImpl>(observableList,  new FriendAvailabilityComparator());
        JList list = new JList(new EventListModel<FriendImpl>(sortedObservables));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new FriendCellRenderer(icons));
        JScrollPane scroll = new JScrollPane(list);
        add(scroll);
    }
    
    public void addFriend(FriendImpl friend) {
        friends.add(friend);
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
            JXPanel cell = new JXPanel(new MigLayout("", "3[]6[]push[]", "1[]1"));
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
}
