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

import org.limewire.ui.swing.nav.Navigator.NavItem;

class NavList extends JPanel {
    
    private final JLabel titleLabel;
    private final JList itemList;
    private final DefaultListModel listModel;
    private final NavItem navTarget;
    private final Navigator navigator;
    
    NavList(String title, NavItem target, Navigator navigator) {
        setOpaque(false);
        
        this.navigator = navigator;
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
    
    public NavItem getTarget() {
        return navTarget;
    }
    
    public void addNavItem(JPanel component, String name) {
        navigator.addNavigablePanel(getTarget(), name, component);
        listModel.addElement(name);
    }
    
    public void removeNavItem(String name) {
        navigator.removeNavigablePanel(getTarget(), name);
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
    
    public void navigateToItem(String name) {
        itemList.setSelectedValue(name, true);
    }

    public void navigateToSelection() {
        navigator.showNavigablePanel(getTarget(), itemList.getSelectedValue().toString());
    }

}
