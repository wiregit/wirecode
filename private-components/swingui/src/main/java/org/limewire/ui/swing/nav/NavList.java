package org.limewire.ui.swing.nav;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.RolloverProducer;
import org.jdesktop.swingx.RolloverRenderer;
import org.limewire.ui.swing.util.GuiUtils;

class NavList extends JPanel {
    
    private final JLabel titleLabel;
    private final JXTable itemList;
    private final DefaultTableModel listModel;
    private final Navigator.NavItem navTarget;
    private final List<ListSelectionListener> listeners = new ArrayList<ListSelectionListener>();
    
    @Resource
    private Icon killIcon;
    
    @Resource
    private Icon rolloverKillIcon;
    
    NavList(String title, Navigator.NavItem target) {
        GuiUtils.injectFields(this);
        setOpaque(false);
        
        this.navTarget = target;
        this.titleLabel = new JLabel(title);
        this.listModel = new DefaultTableModel();
        this.itemList = new JXTable(listModel);
        itemList.setRolloverEnabled(true);
        itemList.setFocusable(false);
        itemList.setEditable(false);
        itemList.getSelectionModel().addListSelectionListener(new DelegateListener());
        itemList.setShowGrid(false, false);
        listModel.setColumnCount(3);
        itemList.getColumnExt(0).setPreferredWidth(10);
        itemList.getColumnExt(1).setPreferredWidth(115);
        itemList.getColumnExt(2).setPreferredWidth(20);
        itemList.getColumnExt(1).setCellRenderer(new NavListCellRenderer());
        itemList.getColumnExt(2).setCellRenderer(new KillerRenderer());
        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
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
    }
    
    public Navigator.NavItem getTarget() {
        return navTarget;
    }
    
    public void addNavItem(String name, boolean userRemovable) {
        listModel.addRow(new Object[] { null, name, userRemovable });
    }
    
    public void removeNavItem(String name) {
        listModel.removeRow(getRowForName(name));
    }
    
    public void addListSelectionListener(ListSelectionListener listener) {
        listeners.add(listener);
    }
    
    public void removeListSelectionListener(ListSelectionListener listener) {
        listeners.remove(listener);
    }
    
    public void clearSelection() {
        itemList.clearSelection();
    }
    
    private class NavListCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            JComponent renderer = (JComponent) super
                    .getTableCellRendererComponent(table, value, isSelected,
                            hasFocus, row, column);
            renderer.setFont(getFont().deriveFont(Font.BOLD));
            
            return renderer;
        }
    }
    
    private class KillerRenderer extends DefaultTableCellRenderer implements RolloverRenderer {
        
        private int lastRow = -1;
        
        @Override
        public void doClick() {
            listModel.removeRow(lastRow);
        }
        
        @Override
        public boolean isEnabled() {
            return getIcon() != null;
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            lastRow = row;
            Point p = (Point) table
                    .getClientProperty(RolloverProducer.ROLLOVER_KEY);

            JComponent renderer = (JComponent) super
                    .getTableCellRendererComponent(table, value, isSelected,
                            hasFocus, row, column);
            setText(null);
            if (value == Boolean.TRUE) {
                if(p != null && p.x == column && p.y == row ) {
                    setIcon(rolloverKillIcon);
                } else {
                    setIcon(killIcon);
                }
            } else {
                setIcon(null);
            }

            return renderer;
        }
    }
    
    private int getRowForName(String name) {
        for(int i = 0; i < listModel.getRowCount(); i++) {
            if(itemList.getValueAt(i, 1).equals(name)) {
                return i;
            }
        }
        return -1;
    }
    
    public void selectItem(String name) {
        int row = getRowForName(name);
        itemList.getSelectionModel().setSelectionInterval(row, row);
    }

    public int getSelectedIndex() {
        return itemList.getSelectedRow();
    }

    public String getSelectionKey() {
        return listModel.getValueAt(itemList.getSelectedRow(), 1).toString();
    }
    
    private class DelegateListener implements ListSelectionListener {
        
        public DelegateListener() {
        }
        
        @Override
        public void valueChanged(ListSelectionEvent e) {
            ListSelectionEvent newEvent = new ListSelectionEvent(NavList.this, e.getFirstIndex(), e.getLastIndex(), e.getValueIsAdjusting());
            for(ListSelectionListener listener : listeners) {
                listener.valueChanged(newEvent);
            }
        }
    }

}
