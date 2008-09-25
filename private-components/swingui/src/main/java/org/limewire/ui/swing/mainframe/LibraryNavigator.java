package org.limewire.ui.swing.mainframe;

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
import org.limewire.ui.swing.library.MyLibraryPanel;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavigationListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LibraryNavigator extends JPanel {    
        
    private final JLabel titleLabel;
    private final JXTable itemList;
    private final DefaultTableModel listModel;
    
    @Inject
    LibraryNavigator(Navigator navigator, MyLibraryPanel libraryPanel) {
        GuiUtils.assignResources(this);
        
        this.titleLabel = new JLabel(I18n.tr("Library"));
        this.listModel = new DefaultTableModel();
        this.itemList = new JXTable(listModel);
        itemList.setRolloverEnabled(true);
        itemList.setFocusable(false);
        itemList.setEditable(false);
        itemList.setShowGrid(false, false);
        listModel.setColumnCount(2);
        itemList.getColumnExt(0).setPreferredWidth(10);
        itemList.getColumnExt(1).setPreferredWidth(135);
        itemList.getColumnExt(1).setCellRenderer(new NavListCellRenderer());
        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemList.addHighlighter(new ColorHighlighter(HighlightPredicate.ROLLOVER_ROW, Color.CYAN, Color.BLACK));
        
        titleLabel.setName("LibraryNavigator.titleLabel");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD).deriveFont(titleLabel.getFont().getSize() + 2f));
        titleLabel.setOpaque(false);
        
        itemList.setOpaque(false);
        
        setLayout(new MigLayout("insets 0"));
        add(titleLabel, "gapleft 5, gapbottom 5, alignx left, wrap");
        add(itemList, "alignx left, aligny top");
        
        navigator.createNavItem(NavCategory.LIBRARY, MyLibraryPanel.NAME, libraryPanel);
        
        navigator.addNavigationListener(new NavigationListener() {
            @Override
            public void itemAdded(NavCategory category, NavItem navItem, JComponent panel) {
                if(category == NavCategory.LIBRARY) {
                    addNavItem(navItem);
                }
            }
            
            @Override
            public void itemRemoved(NavCategory category, NavItem navItem, JComponent panel) {
                if(category == NavCategory.LIBRARY) {
                    removeNavItem(navItem);
                }
            }
            
            @Override
            public void itemSelected(NavCategory category, NavItem navItem, JComponent panel) {
                if(category == NavCategory.LIBRARY) {
                    selectItem(navItem);
                } else {
                    clearSelection();
                }
            }
        });

        itemList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if(e.getValueIsAdjusting()) {
                    return;
                }
                
                int idx = itemList.getSelectedRow();
                if(idx != -1) {
                    NavItem item = (NavItem)listModel.getValueAt(idx, 1);
                    item.select();
                }
            }
        });
    }
    
    
    private void addNavItem(NavItem navItem) {
        listModel.addRow(new Object[] { null, navItem });
    }
    
    private void removeNavItem(NavItem navItem) {
        listModel.removeRow(getRowForNavItem(navItem));
    }
    
    private void clearSelection() {
        itemList.clearSelection();
    }

    private class NavListCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            NavItem navItem = (NavItem)value;
            JComponent renderer = (JComponent) super
                    .getTableCellRendererComponent(table, navItem.getId(), isSelected,
                            hasFocus, row, column);
            renderer.setFont(getFont().deriveFont(Font.BOLD));
            
            return renderer;
        }
    }
    
    private int getRowForNavItem(NavItem navItem) {
        for(int i = 0; i < listModel.getRowCount(); i++) {
            if(itemList.getValueAt(i, 1).equals(navItem)) {
                return i;
            }
        }
        return -1;
    }
    
    private void selectItem(NavItem navItem) {
        int row = getRowForNavItem(navItem);
        itemList.getSelectionModel().setSelectionInterval(row, row);
    }

}
