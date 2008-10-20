package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.event.ChangeListener;

import org.jdesktop.swingx.JXButton;
import org.limewire.ui.swing.action.AbstractAction;

public class LimeComboBox extends JXButton {
    
    private final List<Action> actions;
    private Action             selectedAction;
    private JMenuItem          selectedComponent;
            
    private Color pressedTextColour  = null;
    private Color rolloverTextColour = null;
    
    private boolean hasSize = false;
    private boolean isMenuUpdated = false;
    
    private final JPopupMenu menu;
    
    LimeComboBox(List<Action> actions) {
        
        final Color ITEM_BACK = (Color)UIManager.get("Menu.selectionBackground");
        
        this.setText(null);
        
        this.actions = new LinkedList<Action>();
        this.addActions(actions);
        
        if (!this.actions.isEmpty())
            this.selectedAction = actions.get(0);
        else
            this.selectedAction = null;
        
        this.setModel(this.getModel());
        
        this.menu = new JPopupMenu();
        this.initMenu();
        
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {}

            @Override
            public void mouseExited(MouseEvent e) {}

            @Override
            public void mousePressed(MouseEvent e) {
                updateMenu();
        
                selectedComponent.setBackground(ITEM_BACK);
                
                if (getText() == null)
                    menu.setPreferredSize(new Dimension(getWidth(), 
                            (int) menu.getPreferredSize().getHeight()));
                
                menu.show((Component) e.getSource(), 0, getHeight()-1);
                
            }
        });
    }

    
    public void  addActions(List<Action> actions) {
        this.isMenuUpdated = false;
        
        this.actions.addAll(actions);
    }
    
    public void  addAction(Action action) {
        this.isMenuUpdated = false;
        
        this.actions.add(action);                
    }
    
    public void  removeAction(Action action) {
        this.isMenuUpdated = false;
        
        this.actions.remove(action);
    }
    
    public void setSelectedAction(Action action) {
        this.selectedAction = action;
    }
    
    public Action getSelectedAction() {
        return this.selectedAction;
    }
    
    
    @Override
    public void setText(String promptText) {
        this.hasSize = false;
        
        super.setText(promptText);
    }

    @Override 
    public void setModel(final ButtonModel delegate) {
        super.setModel(new ButtonModel() {
            public boolean isArmed() { return delegate.isArmed(); }
            public boolean isSelected() { return delegate.isSelected(); }
            public boolean isEnabled() { return delegate.isEnabled(); }
            public boolean isPressed() { 
                return delegate.isPressed() || menu.isVisible(); 
            }
            public boolean isRollover() { return delegate.isRollover(); }
            public void setArmed(boolean b) { delegate.setArmed(b); }
            public void setSelected(boolean b) { delegate.setSelected(b); }
            public void setEnabled(boolean b) { delegate.setEnabled(b); }
            public void setPressed(boolean b) { delegate.setPressed(b); }
            public void setRollover(boolean b) { delegate.setRollover(b); }
            public void setMnemonic(int i) { delegate.setMnemonic(i); }
            public int getMnemonic() { return delegate.getMnemonic(); }
            public void setActionCommand(String string) { delegate.setActionCommand(string); }
            public String getActionCommand() { return delegate.getActionCommand(); }
            public void setGroup(ButtonGroup buttonGroup) { delegate.setGroup(buttonGroup); }
            public void addActionListener(ActionListener actionListener) { 
                delegate.addActionListener(actionListener);
            }
            public void removeActionListener(ActionListener actionListener) {
                delegate.removeActionListener(actionListener);
            }
            public Object[] getSelectedObjects() { return delegate.getSelectedObjects(); }
            public void addItemListener(ItemListener itemListener) {
                delegate.addItemListener(itemListener);
            }
            public void removeItemListener(ItemListener itemListener) { 
                delegate.removeItemListener(itemListener);
            }
            public void addChangeListener(ChangeListener changeListener) { 
                delegate.addChangeListener(changeListener);
            }
            public void removeChangeListener(ChangeListener changeListener) {
                delegate.removeChangeListener(changeListener);
            }
        });
    }

    
    public void setForegrounds(Color regular, Color hover, Color down) {
        this.setForeground(regular);
        this.setRolloverForeground(hover);
        this.setPressedForeground(down);
    }
    
    public void setIcons(Icon regular, Icon hover, Icon down) {
        this.setIcon(regular);
        this.setRolloverIcon(hover);
        this.setPressedIcon(down);
    }
    
    @Override
    public Icon getRolloverIcon() {
        Icon icon = super.getRolloverIcon();
        if (icon== null) 
            return this.getIcon();
        else
            return icon;
    }
    
    @Override
    public Icon getPressedIcon() {
        Icon icon = super.getPressedIcon();
        if (icon== null) 
            return this.getIcon();
        else
            return icon;
    }
    
    public void setRolloverForeground(Color colour) {
        this.rolloverTextColour = colour;
    }
    
    public Color getRolloverForeground() {
        if (this.rolloverTextColour == null) 
            return this.getForeground();
        else
            return this.rolloverTextColour;
    }
    
    public void setPressedForeground(Color colour) {
        this.pressedTextColour = colour;
    }
    
    public Color getPressedForeground() {
        if (this.pressedTextColour == null) 
            return this.getForeground();
        else
            return this.pressedTextColour;
    }
    
    private String unpackText(Object object) {
        if (object instanceof Action) 
            return ((Action) object).getValue("Name").toString();
        else
            return object.toString();
    }
    
    private Rectangle2D getLongestTextArea(Object... objects) {
        Graphics2D g2 = (Graphics2D) this.getGraphics();
        
        Rectangle2D largestRect = this.getFont().getStringBounds(unpackText(objects[0]), 
                g2.getFontRenderContext());
        
        for ( int i=1 ; i<objects.length ; i++ ) {
            
            Rectangle2D currentRect = this.getFont().getStringBounds(unpackText(objects[i]), 
                    g2.getFontRenderContext());
            
            if (currentRect.getWidth() > largestRect.getWidth()) {
                largestRect = currentRect;
            }
        }        
        
        return largestRect;
    }
    
    private void updateSize() {
        this.hasSize = true;
        
        Rectangle2D labelRect = null;
                
        if (this.getText() != null && !this.getText().isEmpty()) {
            labelRect = this.getLongestTextArea(this.getText());
        } 
        else {
            labelRect = this.getLongestTextArea(this.actions.toArray());
        }    
        
        int ix1 = 0;
        int ix2 = 0;
        int iy1 = 0;
        int iy2 = 0;
        
        if (this.getBorder() != null) {
            Insets insets = this.getBorder().getBorderInsets(this);
            ix1 = insets.left;
            ix2 = insets.right;
            iy1 = insets.top;
            iy2 = insets.bottom;
        }
        
        this.setPreferredSize(new Dimension((int)labelRect.getWidth() + ix1 + ix2, 
                (int)labelRect.getHeight()  + iy1 + iy2));
        
        this.setSize(this.getPreferredSize());
    }
    
        
    @Override
    public boolean isOpaque() {
        return false;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        if (!this.hasSize) this.updateSize();
        
        Graphics2D g2 = (Graphics2D) g;        
        
        this.getBackgroundPainter().paint(g2, this, this.getWidth(), this.getHeight());  
        
        g2.setFont(this.getFont());
        
        FontMetrics fm = g2.getFontMetrics();
        
        int ix1 = 0;
        int ix2 = 0;
        
        if (this.getBorder() != null) {
            Insets insets = this.getBorder().getBorderInsets(this);
            ix1 = insets.left;
            ix2 = insets.right;
        }
        
        Icon icon = this.getIcon();
                
        if (this.getModel().isPressed()) {
            icon = this.getPressedIcon();
            g2.setColor(this.getPressedForeground());
        }
        else if (this.getModel().isRollover()) {
            icon = this.getRolloverIcon();
            g2.setColor(this.getRolloverForeground());
        }
        else {
            g2.setColor(this.getForeground());
        }
            
        
        if (this.getText() != null) {
            g2.drawString(this.getText(), ix1, fm.getAscent()+1);
            
            if (icon != null) {
                icon.paintIcon(this, g2, this.getWidth() - ix2 + 3, 
                        this.getHeight()/2 - icon.getIconHeight()/2-1);
            }
        } else {
            if (this.selectedAction != null) {
                g2.drawString(this.unpackText(this.selectedAction), ix1, fm.getAscent()+2);
            }
            
            if (icon != null) {
                
                icon.paintIcon(this, g2, this.getWidth() - ix2 + icon.getIconWidth(), 
                        this.getHeight()/2 - icon.getIconHeight()/2);
            }
        }
    }
    
    
    // Wraps an action to provide selection listening for the combo box
    
    private class SelectionActionWrapper extends AbstractAction {
        

        private final Action wrappedAction;
        
        public SelectionActionWrapper(Action actionToWrap) {
            this.wrappedAction = actionToWrap;
        }
        
        @Override 
        public Object getValue(String s) {
            return this.wrappedAction.getValue(s);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            // Change selection in parent combo box
            selectedAction = this.wrappedAction;
            selectedComponent = (JMenuItem) e.getSource();
            
            // Call original action
            this.wrappedAction.actionPerformed(e);   
            
            repaint();
        }
        
    }
    
    
    private void updateMenu() {
        if (this.isMenuUpdated) return;
        this.isMenuUpdated = true;
        
        this.menu.removeAll();
        
        int ix1 = 0;
        
        if (this.getBorder() != null) {
            Insets insets = this.getBorder().getBorderInsets(this);
            ix1 = insets.left - 4;
        }
        
        for ( Action action : this.actions ) {
        
            Action compoundAction = action;
            
            // Wrap the action if this combo box has room for selection
            if (this.getText() == null)
                compoundAction = new SelectionActionWrapper(compoundAction);
            
            JMenuItem menuItem = new JMenuItem(compoundAction);
            menuItem.setBackground(Color.WHITE);
            menuItem.setForeground(Color.BLACK);
            menuItem.setFont(this.getFont());
            
            menuItem.setBorder(BorderFactory.createEmptyBorder(0,ix1,0,0));
            
            this.menu.add(menuItem);
            
            if (action == this.selectedAction)
                this.selectedComponent = menuItem;
        }

    }
    
    private void initMenu() {
        this.menu.setBorder(BorderFactory.createLineBorder(Color.BLACK,1));
        this.menu.setBackground(Color.WHITE);
        this.menu.setForeground(Color.BLACK);
        
        this.menu.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                selectedComponent.setBackground(Color.WHITE);
                selectedComponent.updateUI();
            }
            @Override
            public void mouseExited(MouseEvent e) {
            }
            @Override
            public void mousePressed(MouseEvent e) {
            }
            @Override
            public void mouseReleased(MouseEvent e) {
            } 
        });
    }
    
    
    public static void main(String[] args) {
        
        JFrame window = new JFrame();
        
        JPanel panel = new JPanel(new FlowLayout());
        panel.setBackground(Color.WHITE);
        
        Action one = new AbstractAction("  one") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                
            }
        };
        Action two = new AbstractAction("two two    ") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                
            }
        };
        AbstractAction three = new AbstractAction("threethree three") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                
                
                
            }
            
             
            
        };
        
        Action[] actions = new Action[] {one,two,three};
        
        LinkedList<Action> actList = new LinkedList<Action>();
        
        for ( Action a : actions)
            actList.add(a);
        
        panel.add(new LimeComboBoxFactory().createFullComboBox(actList));
        
        panel.add(new LimeComboBoxFactory().createMiniComboBox("hello",actList));
        
        window.add(panel);
        window.pack();
        window.validate();
        window.setVisible(true);
        window.setSize(new Dimension(500,500));
    }
}
