package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendRemoteLibraryEvent;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteFileList;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.ui.swing.mainframe.SectionHeading;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.table.ClearCellRenderer;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LibraryNavigator extends JPanel {

    private final SectionHeading titleLabel;

    private final JXTable itemList;

    private final DefaultTableModel listModel;

    private final Navigator navigator;

    @Inject
    LibraryNavigator(Navigator navigator, MyLibraryPanel libraryPanel,
            ListenerSupport<FriendRemoteLibraryEvent> friendLibrarySupport, ShareListManager libraryManager
            ) {
        GuiUtils.assignResources(this);

        setOpaque(false);
        
        this.navigator = navigator;

        this.titleLabel = new SectionHeading(I18n.tr("Libraries"));
        this.listModel = new DefaultTableModel();
        this.itemList = new JXTable(listModel);
        itemList.setRolloverEnabled(true);
        itemList.setFocusable(false);
        itemList.setEditable(false);
        itemList.setShowGrid(false, false);
        listModel.setColumnCount(3);
        itemList.getColumnExt(0).setPreferredWidth(10);
        itemList.getColumnExt(0).setCellRenderer(new ClearCellRenderer());
        itemList.getColumnExt(1).setPreferredWidth(135);
        itemList.getColumnExt(1).setCellRenderer(new FriendCellRenderer());
        itemList.getColumnExt(2).setVisible(false);
        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemList.addHighlighter(new ColorHighlighter(HighlightPredicate.ROLLOVER_ROW, Color.CYAN,
                Color.BLACK));

        titleLabel.setName("LibraryNavigator.titleLabel");

        itemList.setOpaque(false);

        setLayout(new MigLayout("insets 0"));
        add(titleLabel, "gapbottom 5, growx, alignx left, aligny top,  wrap");
        add(itemList, "alignx left, aligny top");

        final NavItem myLibraryItem = navigator.createNavItem(NavCategory.LIBRARY,
                MyLibraryPanel.NAME, libraryPanel);
        myLibraryItem.addNavItemListener(new NavItemListener() {
            @Override
            public void itemRemoved() {
                removeNavItem(myLibraryItem);
            }

            @Override
            public void itemSelected(boolean selected) {
                if (selected) {
                    selectItem(myLibraryItem);
                } else {
                    clearSelection(myLibraryItem);
                }
            }
        });
        addNavItem(new Me(), myLibraryItem);

        itemList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }

                int idx = itemList.getSelectedRow();
                if (idx != -1) {
                    NavItem item = (NavItem) listModel.getValueAt(idx, 2);
                    item.select();
                }
            }
        });

        friendLibrarySupport.addListener(new EventListener<FriendRemoteLibraryEvent>() {
            @Override
            public void handleEvent(FriendRemoteLibraryEvent event) {
                switch (event.getType()) {
                case FRIEND_LIBRARY_ADDED:
                    addFriend(event.getFriend(), event.getFileList());
                    break;
                case FRIEND_LIBRARY_REMOVED:
                    removeFriend(event.getFriend(), event.getFileList());
                    break;
                }
            }
        });
    }

    private void addNavItem(Friend friend, NavItem navItem) {
        listModel.addRow(new Object[] { null, friend, navItem });
    }

    private void removeNavItem(NavItem navItem) {
        listModel.removeRow(getRowForNavItem(navItem));
    }

    private void clearSelection(NavItem navItem) {
        int row = getRowForNavItem(navItem);
        itemList.getSelectionModel().removeSelectionInterval(row, row);
    }

    private int getRowForNavItem(NavItem navItem) {
        for (int i = 0; i < listModel.getRowCount(); i++) {
            if (listModel.getValueAt(i, 2).equals(navItem)) {
                return i;
            }
        }
        return -1;
    }

    private NavItem getNavItemForFriend(Friend friend) {
        for (int i = 0; i < listModel.getRowCount(); i++) {
            if (((Friend)listModel.getValueAt(i, 1)).getId().equals(friend.getId())) {
                return (NavItem)listModel.getValueAt(i, 2);
            }
        }
        return null;
    }

    private void selectItem(NavItem navItem) {
        int row = getRowForNavItem(navItem);
        itemList.getSelectionModel().setSelectionInterval(row, row);
    }

    private void addFriend(Friend friend, RemoteFileList fileList) {
        FriendLibrary library = new FriendLibrary<RemoteFileItem>(fileList);
        final NavItem item = navigator.createNavItem(NavCategory.LIBRARY, friend.getId(), library);
        item.addNavItemListener(new NavItemListener() {
            @Override
            public void itemRemoved() {
                removeNavItem(item);
            }

            @Override
            public void itemSelected(boolean selected) {
                if (selected) {
                    selectItem(item);
                } else {
                    clearSelection(item);
                }
            }
        });
        addNavItem(friend, item);
    }

    private void removeFriend(Friend friend, RemoteFileList fileList) {
        NavItem item = getNavItemForFriend(friend);
        if(item != null) {
            item.remove();
        }
    }
    
    private static class Me implements Friend {
        @Override
        public String getId() {
            return "_@_internal_@_";
        }

        @Override
        public String getName() {
            return I18n.tr("Me");
        }
        
        @Override
        public String getRenderName() {
            return getName();
        }
    }
    


    private static class FriendCellRenderer extends ClearCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            String render = "";
            Friend friend = (Friend)value;
            if(friend != null) {
                render = friend.getName();
                if(render == null) {
                    render = friend.getId();
                }
            }
            JComponent renderer = (JComponent) super.getTableCellRendererComponent(table,
                    render, isSelected, hasFocus, row, column);
            FontUtils.bold(renderer);
            return renderer;
        }
    }
}
