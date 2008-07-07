package org.limewire.ui.swing.nav;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;

class NavList extends JPanel {
    
    private final JLabel titleLabel;
    private final JList itemList;
    private final DefaultListModel listModel;
    private final Navigator.NavItem navTarget;
    
    NavList(String title, Navigator.NavItem target) {
        setOpaque(false);
        
        this.navTarget = target;
        this.titleLabel = new JLabel(title);
        this.listModel = new DefaultListModel();
        this.itemList = new JList(listModel);
        itemList.setFocusable(false);
        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD).deriveFont(titleLabel.getFont().getSize() + 2f));
        titleLabel.setForeground(Color.GRAY);
        titleLabel.setOpaque(false);
        
        itemList.setCellRenderer(new NavListCellRenderer());
        itemList.setOpaque(false);
        itemList.setFixedCellWidth(500);
        
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
    
    public void addNavItem(String name) {
        listModel.addElement(name);
    }
    
    public void removeNavItem(String name) {
        listModel.removeElement(name);
    }
    
    public void addListSelectionListener(ListSelectionListener listener) {
        itemList.addListSelectionListener(listener);
    }
    
    public void removeListSelectionListener(ListSelectionListener listener) {
        itemList.removeListSelectionListener(listener);
    }
    
    public boolean isListSourceFrom(Object source) {
        return source == itemList;
    }
    
    public void clearSelection() {
        itemList.clearSelection();
    }
    
    private class NavListCellRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

            JComponent renderer = (JComponent) super
                    .getListCellRendererComponent(list, value, index,
                            isSelected, cellHasFocus);
            
            setFont(getFont().deriveFont(Font.BOLD));
            setIcon(new SpaceIcon(10));
            
            return renderer;
        }
    }
    
    private static class SpaceIcon implements Icon {
        private final int width;
        SpaceIcon(int width) {
            this.width = width;
        }
        
        @Override
        public int getIconHeight() {
            return 0;
        }
        @Override
        public int getIconWidth() {
            return width;
        }
        
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
        }
        
    }
    
    public void selectItem(String name) {
        itemList.setSelectedValue(name, true);
    }

}
