package org.limewire.ui.swing.friends;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.EventListModel;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public class FriendsPane extends JPanel {
    private EventList<FriendImpl> friends;
    
    public FriendsPane() {
        super(new BorderLayout());
        friends = new BasicEventList<FriendImpl>();
        ObservableElementList<FriendImpl> observableList = new ObservableElementList<FriendImpl>(friends, GlazedLists.beanConnector(FriendImpl.class));
        SortedList<FriendImpl> sortedObservables = new SortedList<FriendImpl>(observableList,  new FriendAvailabilityComparator());
        JList list = new JList(new EventListModel<FriendImpl>(sortedObservables));
        list.setCellRenderer(new FriendCellRenderer());
        JScrollPane scroll = new JScrollPane(list);
        add(scroll);
    }
    
    public void addFriend(FriendImpl friend) {
        friends.add(friend);
    }
    
    private static class FriendCellRenderer implements ListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JPanel cell = new JPanel();
            Friend friend = (Friend)value;
            cell.add(new JLabel(friend.getName()));
            return cell;
        }
    }
}
