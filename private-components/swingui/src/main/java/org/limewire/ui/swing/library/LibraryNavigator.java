package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.core.api.library.BuddyLibraryEvent;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteFileList;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LibraryNavigator extends JPanel {

    private final JLabel titleLabel;

    private final JXTable itemList;

    private final DefaultTableModel listModel;

    private final Navigator navigator;

    @Inject
    LibraryNavigator(Navigator navigator, MyLibraryPanel libraryPanel,
            ListenerSupport<BuddyLibraryEvent> buddyLibrarySupport, LibraryManager libraryManager
            ) {
        GuiUtils.assignResources(this);

        this.navigator = navigator;

        this.titleLabel = new JLabel(I18n.tr("Library"));
        this.listModel = new DefaultTableModel();
        this.itemList = new JXTable(listModel);
        itemList.setRolloverEnabled(true);
        itemList.setFocusable(false);
        itemList.setEditable(false);
        itemList.setShowGrid(false, false);
        listModel.setColumnCount(3);
        itemList.getColumnExt(0).setPreferredWidth(10);
        itemList.getColumnExt(1).setPreferredWidth(135);
        itemList.getColumnExt(1).setCellRenderer(new FriendCellRenderer());
        itemList.getColumnExt(2).setVisible(false);
        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemList.addHighlighter(new ColorHighlighter(HighlightPredicate.ROLLOVER_ROW, Color.CYAN,
                Color.BLACK));

        titleLabel.setName("LibraryNavigator.titleLabel");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD).deriveFont(
                titleLabel.getFont().getSize() + 2f));
        titleLabel.setOpaque(false);

        itemList.setOpaque(false);

        setLayout(new MigLayout("insets 0"));
        add(titleLabel, "gapleft 5, gapbottom 5, alignx left, wrap");
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

        buddyLibrarySupport.addListener(new EventListener<BuddyLibraryEvent>() {
            @Override
            public void handleEvent(BuddyLibraryEvent event) {
                switch (event.getType()) {
                case BUDDY_ADDED:
                    addFriend(event.getId(), event.getFileList());
                    break;
                case BUDDY_REMOVED:
                    removeFriend(event.getId(), event.getFileList());
                    break;
                }
            }
        });
    }

    private void addNavItem(LibraryName name, NavItem navItem) {
        listModel.addRow(new Object[] { null, name, navItem });
    }

    private void removeNavItem(NavItem navItem) {
        listModel.removeRow(getRowForNavItem(navItem));
    }

    private void clearSelection(NavItem navItem) {
        int row = getRowForNavItem(navItem);
        itemList.getSelectionModel().removeSelectionInterval(row, row);
    }

    private class FriendCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            String render = "";
            LibraryName friend = (LibraryName)value;
            if(friend != null) {
                render = friend.getRenderName();
            }
            JComponent renderer = (JComponent) super.getTableCellRendererComponent(table,
                    render, isSelected, hasFocus, row, column);
            FontUtils.bold(renderer);
            return renderer;
        }
    }

    private int getRowForNavItem(NavItem navItem) {
        for (int i = 0; i < listModel.getRowCount(); i++) {
            if (listModel.getValueAt(i, 2).equals(navItem)) {
                return i;
            }
        }
        return -1;
    }

    private NavItem getNavItemForFriend(String id) {
        for (int i = 0; i < listModel.getRowCount(); i++) {
            if (((LibraryName)listModel.getValueAt(i, 1)).getId().equals(id)) {
                return (NavItem)listModel.getValueAt(i, 2);
            }
        }
        return null;
    }

    private void selectItem(NavItem navItem) {
        int row = getRowForNavItem(navItem);
        itemList.getSelectionModel().setSelectionInterval(row, row);
    }

    private void addFriend(String id, RemoteFileList fileList) {
        BuddyLibrary library = new BuddyLibrary<RemoteFileItem>(fileList);
        final NavItem item = navigator.createNavItem(NavCategory.LIBRARY, id, library);
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
        addNavItem(new Friend(id), item);
    }

    private void removeFriend(String id, RemoteFileList fileList) {
        NavItem item = getNavItemForFriend(id);
        if(item != null) {
            item.remove();
        }
    }
    
    private static interface LibraryName {
        String getRenderName();
        String getId();
    }
    
    private class Me implements LibraryName {
        @Override
        public String getRenderName() {
            return I18n.tr("Me");
        }
        
        @Override
        public String getId() {
            return "_@_internal_@_";
        }
    }
    
    private class Friend implements LibraryName {
        private final String id;
        
        Friend(String id) {
            this.id = id;
        }
        
        @Override
        public String getId() {
            return id;
        }
        
        @Override
        public String getRenderName() {
            return id;
        }
    }
}
