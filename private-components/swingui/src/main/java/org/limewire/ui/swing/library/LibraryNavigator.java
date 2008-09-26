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
import org.limewire.core.api.library.Buddy;
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
        itemList.getColumnExt(1).setCellRenderer(new BuddyCellRenderer());
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
        addNavItem(new Buddy() {
            @Override
            public String getId() {
                return "_@_internal_@_";
            }

            @Override
            public String getName() {
                return I18n.tr("Me");
            }
        }, myLibraryItem);

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
                    addBuddy(event.getBuddy(), event.getFileList());
                    break;
                case BUDDY_REMOVED:
                    removeBuddy(event.getBuddy(), event.getFileList());
                    break;
                }
            }
        });
    }

    private void addNavItem(Buddy buddy, NavItem navItem) {
        listModel.addRow(new Object[] { null, buddy, navItem });
    }

    private void removeNavItem(NavItem navItem) {
        listModel.removeRow(getRowForNavItem(navItem));
    }

    private void clearSelection(NavItem navItem) {
        int row = getRowForNavItem(navItem);
        itemList.getSelectionModel().removeSelectionInterval(row, row);
    }

    private class BuddyCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            String render = "";
            Buddy buddy = (Buddy)value;
            if(buddy != null) {
                render = buddy.getName();
                if(render == null) {
                    render = buddy.getId();
                }
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

    private int getRowForBuddy(Buddy buddy) {
        for (int i = 0; i < listModel.getRowCount(); i++) {
            if (((Buddy) listModel.getValueAt(i, 1)).getId().equals(buddy.getId())) {
                return i;
            }
        }
        return -1;
    }

    private void selectItem(NavItem navItem) {
        int row = getRowForNavItem(navItem);
        itemList.getSelectionModel().setSelectionInterval(row, row);
    }

    private void addBuddy(Buddy buddy, RemoteFileList fileList) {
        BuddyLibrary library = new BuddyLibrary<RemoteFileItem>(fileList);
        final NavItem item = navigator.createNavItem(NavCategory.LIBRARY, buddy.getId(), library);
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
        addNavItem(buddy, item);
    }

    private void removeBuddy(Buddy buddy, RemoteFileList fileList) {
        int row = getRowForBuddy(buddy);
        if(row != -1) {
            listModel.removeRow(row);
        }
    }
}
