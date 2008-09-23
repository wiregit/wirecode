package org.limewire.ui.swing.nav;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.ui.swing.util.GuiUtils;

class ListNavList extends JXPanel implements NavList {
    
    private final JLabel titleLabel;
    private final JXTable itemList;
    private final DefaultTableModel listModel;
    private final NavCategory navCategory;
    private final List<SelectionListener> listeners = new ArrayList<SelectionListener>();
    private boolean vizSet = false;
    
    ListNavList(String title, NavCategory target) {
        GuiUtils.assignResources(this);
        
        this.navCategory = target;
        this.titleLabel = new JLabel(title);
        this.listModel = new DefaultTableModel();
        this.itemList = new JXTable(listModel);
        itemList.setRolloverEnabled(true);
        itemList.setFocusable(false);
        itemList.setEditable(false);
        itemList.getSelectionModel().addListSelectionListener(new DelegateListener());
        itemList.setShowGrid(false, false);
        listModel.setColumnCount(2);
        itemList.getColumnExt(0).setPreferredWidth(10);
        itemList.getColumnExt(1).setPreferredWidth(115);
        itemList.getColumnExt(1).setCellRenderer(new NavListCellRenderer());
        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemList.addHighlighter(new ColorHighlighter(HighlightPredicate.ROLLOVER_ROW, Color.CYAN, Color.BLACK));
        
        titleLabel.setName("NavList.titleLabel");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD).deriveFont(titleLabel.getFont().getSize() + 2f));
        titleLabel.setOpaque(false);
        
        itemList.setOpaque(false);
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 5, 0);
        gbc.gridheight = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(titleLabel, gbc);

        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(0, 0, 0, 0);
        add(itemList, gbc);
        
        // Start out invisible - will become visible
        // when an item is added.
        super.setVisible(false);
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.nav.NavList#getCategory()
     */
    @Override
    public NavCategory getCategory() {
        return navCategory;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.nav.NavList#addNavItem(org.limewire.ui.swing.nav.NavItem)
     */
    @Override
    public void addNavItem(NavItem navItem) {
        listModel.addRow(new Object[] { null, navItem });
        if(listModel.getRowCount() == 1) {
            if(!vizSet) {
                super.setVisible(true);
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.nav.NavList#removeNavItem(org.limewire.ui.swing.nav.NavItem)
     */
    @Override
    public void removeNavItem(NavItem navItem) {
        listModel.removeRow(getRowForNavItem(navItem));
        if(listModel.getRowCount() == 0) {
            if(!vizSet) {
                super.setVisible(false);
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.nav.NavList#setVisible(boolean)
     */
    @Override
    public void setVisible(boolean flag) {
        vizSet = true;
        super.setVisible(flag);
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.nav.NavList#addListSelectionListener(javax.swing.event.ListSelectionListener)
     */
    @Override
    public void addSelectionListener(final SelectionListener listener) {
        listeners.add(listener);
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.nav.NavList#clearSelection()
     */
    @Override
    public void clearSelection() {
        itemList.clearSelection();
    }

    /* (non-Javadoc)
     * @see org.limewire.ui.swing.nav.NavList#getNavItem(java.lang.String)
     */
    @Override
    public NavItem getNavItem(String name) {
        int row = getRowForName(name);
        return (NavItem)listModel.getValueAt(row, 1);
    }

    /* (non-Javadoc)
     * @see org.limewire.ui.swing.nav.NavList#hasNavItem(java.lang.String)
     */
    @Override
    public boolean hasNavItem(String name) {
        return getRowForName(name) != -1;
    }

    private class NavListCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            NavItem navItem = (NavItem)value;
            JComponent renderer = (JComponent) super
                    .getTableCellRendererComponent(table, navItem.getName(), isSelected,
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
    
    private int getRowForName(String name) {
        for(int i = 0; i < listModel.getRowCount(); i++) {
            if(((NavItem)itemList.getValueAt(i, 1)).getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.nav.NavList#selectItem(org.limewire.ui.swing.nav.NavItem)
     */
    @Override
    public void selectItem(NavItem navItem) {
        int row = getRowForNavItem(navItem);
        itemList.getSelectionModel().setSelectionInterval(row, row);
    }

    /* (non-Javadoc)
     * @see org.limewire.ui.swing.nav.NavList#selectItemByName(java.lang.String)
     */
    @Override
    public void selectItemByName(String name) {
        int row = getRowForName(name);
        itemList.getSelectionModel().setSelectionInterval(row, row);
    }

    /* (non-Javadoc)
     * @see org.limewire.ui.swing.nav.NavList#getSelectedIndex()
     */
    @Override
    public boolean isNavItemSelected() {
        return itemList.getSelectedRow() != -1;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.nav.NavList#getNavItem()
     */
    @Override
    public NavItem getSelectedNavItem() {
        return (NavItem)listModel.getValueAt(itemList.getSelectedRow(), 1);
    }

    /* (non-Javadoc)
     * @see org.limewire.ui.swing.nav.NavList#getSelectionKey()
     */
    public String getSelectionKey() {
        return listModel.getValueAt(itemList.getSelectedRow(), 1).toString();
    }

    @Override
    public Component getComponent() {
        return this;
    }
    
    private class DelegateListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if(e.getValueIsAdjusting()) {
                return;
            }
            
            for(SelectionListener listener : listeners) {
                listener.selectionChanged(ListNavList.this);
            }
        }
    }

}
