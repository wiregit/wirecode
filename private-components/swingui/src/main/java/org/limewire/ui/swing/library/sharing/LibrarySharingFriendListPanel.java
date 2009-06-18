package org.limewire.ui.swing.library.sharing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.inject.LazySingleton;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.table.GlazedJXTable;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@LazySingleton
class LibrarySharingFriendListPanel {

    @Resource private Font labelFont;
    @Resource private Color labelColor;
    @Resource private Font linkFont;
    @Resource private Color backgroundColor;
    
    private final JPanel component;
    private final JLabel headerLabel;
    private final HyperlinkButton editButton;
    private final Map<String, Friend> knownFriends;
    private final List<String> sharedIds;
    
    private final LibrarySharingFriendListRenderer renderer;
    private final Model friendModel;
    private final JXTable friendList;
    private final JScrollPane scrollPane;

    @Inject
    public LibrarySharingFriendListPanel(EditSharingAction sharingAction, @Named("known") Map<String, Friend> knownFriends) {
        GuiUtils.assignResources(this);
        this.knownFriends = knownFriends;        
        this.friendModel = new Model();
        this.friendList = new GlazedJXTable(friendModel);
        this.sharedIds = new ArrayList<String>();
        
        component = new JPanel(new MigLayout("insets 0, gap 0, fillx", "134!", ""));        
        component.setOpaque(false);
        
        headerLabel = new JLabel();
        headerLabel.setFont(labelFont);
        headerLabel.setForeground(labelColor);
        component.add(headerLabel, "aligny top, gaptop 8, gapleft 6, gapbottom 6, wrap");

        friendList.setTableHeader(null);
        friendList.setShowGrid(false, false);
        friendList.setFocusable(false);
        
        scrollPane = new JScrollPane(friendList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setMinimumSize(new Dimension(0,0));
        scrollPane.setBorder(BorderFactory.createEmptyBorder(1,0,1,0));
        scrollPane.setBackground(backgroundColor);
        
        renderer = new LibrarySharingFriendListRenderer(scrollPane);
        friendList.getColumnExt(0).setCellRenderer(renderer);
       
        component.add(scrollPane, "grow, wrap");
        
        editButton = new HyperlinkButton(I18n.tr("Edit Sharing"), sharingAction);
        editButton.setFont(linkFont);
        component.add(editButton, "aligny top, gaptop 5, gapleft 6, gapbottom 5, wrap");
        
        scrollPane.getVerticalScrollBar().addComponentListener(new ComponentListener(){
            @Override
            public void componentHidden(ComponentEvent e) {
                scrollPane.setBorder(BorderFactory.createEmptyBorder(1,0,1,0));
            }

            @Override
            public void componentShown(ComponentEvent e) {
                scrollPane.setBorder(BorderFactory.createMatteBorder(1,0,1,0, Color.BLACK));
            }
            
            @Override
            public void componentMoved(ComponentEvent e) {}
            @Override
            public void componentResized(ComponentEvent e) {}
        });
    }
    
    @Inject void register(@Named("known") ListenerSupport<FriendEvent> friendSupport) {
        friendSupport.addListener(new EventListener<FriendEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendEvent event) {
                switch(event.getType()) {
                case ADDED:
                    setSharedFriendIds(sharedIds);
                    break;
                }
            }
        });
    }
    
    void clear() {
        friendModel.setData(Collections.emptyList());
    }
    
    /** Sets the list of IDs this is shared with. */
    void setSharedFriendIds(List<String> newFriendIds) {
        if(!newFriendIds.isEmpty()) {
            headerLabel.setText(I18n.tr("Sharing list with..."));
        } else {
            headerLabel.setText(I18n.tr("Not Shared"));
        }
        
        // Only set if we're not refreshing..
        if(newFriendIds != sharedIds) {
            sharedIds.clear();
            sharedIds.addAll(new ArrayList<String>(newFriendIds));
        }
        
        List<Object> newModel = new Vector<Object>(newFriendIds.size());
        int unknown = 0;
        for(String id : sharedIds) {
            Friend friend = knownFriends.get(id);
            if(friend != null) {
                newModel.add(friend);
            } else {
                unknown++;
            }
        }
        if(unknown > 0) {
            newModel.add(I18n.tr("{0} friends from another account", unknown));
            // TODO: Do something about row sizes.
            friendList.setRowHeightEnabled(true);            
        } else {
            friendList.setRowHeightEnabled(false);
        }
        friendModel.setData(newModel);
        friendList.setVisibleRowCount(newModel.size());
        component.revalidate();
    }
    
    JComponent getComponent() {
        return component;
    }
    
    private static class Model extends AbstractTableModel {
        private List<Object> data;
        
        Model() {
            this.data = Collections.emptyList();
        }
        
        void setData(List<Object> newData) {
            this.data = newData;
            fireTableDataChanged();
        }
        
        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return data.get(rowIndex);
        }        
    }
}
