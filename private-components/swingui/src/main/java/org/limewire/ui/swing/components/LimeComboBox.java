package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.ChangeListener;

import org.jdesktop.swingx.JXButton;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.PainterUtils;

public class LimeComboBox extends JXButton {
    
    private final List<Action> actions;
    private Action             selectedAction;

    private final List<SelectionListener> selectionListeners 
        = new LinkedList<SelectionListener>();
    
    private Color pressedTextColour  = null;
    private Color rolloverTextColour = null;
    
    private boolean isMenuOverrided = false;
    private boolean isMenuUpdated = false;
    
    private JPopupMenu menu = null;
    private Cursor mouseOverCursor = Cursor.getDefaultCursor();
    
    private UpdateHandler updateHandler = null;
    
    LimeComboBox(List<Action> actions) {        
        this.setText(null);
        
        this.actions = new LinkedList<Action>();
        this.addActions(actions);
        
        if (!this.actions.isEmpty())
            this.selectedAction = actions.get(0);
        else
            this.selectedAction = null;

        
        this.initModel();
    }

    public void overrideMenu(JPopupMenu menu) {
        this.isMenuOverrided = true;
        this.menu = menu;
        this.initMenu();
    }

    public void  addActions(List<Action> actions) {
        if (actions == null) return;
        
        this.isMenuUpdated = false;
        
        this.actions.addAll(actions);
        
        if (this.selectedAction == null)
            this.selectedAction = actions.get(0);

        this.updateSize();
        
        if (this.menu == null) 
            this.createPopupMenu();
    }
    
    public void  addAction(Action action) {
        if (action == null)  throw new IllegalArgumentException("Null Action added");
        
        this.isMenuUpdated = false;
        
        this.actions.add(action);
        
        if (this.selectedAction == null)
            this.selectedAction = actions.get(0);
        
        this.updateSize();
        
        if (this.menu == null) 
            this.createPopupMenu();
    }
    
    public void  removeAllActions() {
        this.isMenuUpdated = false;
        
        this.actions.clear();
        
        this.selectedAction = null;
    }
    
    public void  removeAction(Action action) {
        this.isMenuUpdated = false;
        
        this.actions.remove(action);
        
        if (action == this.selectedAction) {
            this.selectedAction = null;
        }
    }
    
    public void setSelectedAction(Action action) {
        
        // Make sure the selected action is in the list
        if (!this.actions.contains(action))  return;
        
        this.selectedAction = action;
        
        this.isMenuUpdated = false;
    }
    
    public Action getSelectedAction() {
        return this.selectedAction;
    }
    
    
    @Override
    public void setText(String promptText) {
        super.setText(promptText);
        
        if (promptText != null)        
            this.updateSize();
    }

    public void setMouseOverCursor(Cursor cursor) {
        this.mouseOverCursor = cursor;
    }
    
    public void addUpdateHandler(UpdateHandler updateHandler) {
        this.updateHandler = updateHandler;        
    }

    
    public void addSelectionListener(SelectionListener listener) {
        selectionListeners.add(listener);
    }
    
    @Override 
    public void setModel(final ButtonModel delegate) {
        super.setModel(new ButtonModel() {
            public boolean isArmed() { return delegate.isArmed(); }
            public boolean isSelected() { return delegate.isSelected(); }
            public boolean isEnabled() { return delegate.isEnabled(); }
            public boolean isPressed() {
                return delegate.isPressed() || menu != null && menu.isVisible(); 
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
    
    @Override
    public boolean isOpaque() {
        return false;
    }
    
    
    @Override
    public void setFont(Font f) {
        super.setFont(f);
        this.updateSize();
    }
    
    
    
    
    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;        

        if (this.getBackgroundPainter() != null) {
            Object origHints = g2.getRenderingHints();
            this.getBackgroundPainter().paint(g2, this, this.getWidth(), this.getHeight());  
            g2.setRenderingHints((Map<?, ?>) origHints);
        }
        
        g2.setFont(this.getFont());
        
        int ix1 = 0;
        int ix2 = 0;
        int iy2 = 0;
        
        if (this.getBorder() != null) {
            Insets insets = this.getBorder().getBorderInsets(this);
            ix1 = insets.left;
            ix2 = insets.right;
            iy2 = insets.bottom;
        }
        
        int y = this.getHeight() - this.getHeight()/2 + g.getFontMetrics().getAscent()/2 - iy2;
        
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
            PainterUtils.drawSmoothString(g2, this.getText(), ix1,  
                    y);
            
            if (icon != null) {
                icon.paintIcon(this, g2, this.getWidth() - ix2 + 3, 
                        this.getHeight()/2 - icon.getIconHeight()/2);
            }
        } else {
            if (this.selectedAction != null) {
                PainterUtils.drawSmoothString(g2, this.selectedAction.getValue("Name").toString(), ix1, 
                        y);
            }
            
            if (icon != null) {
                
                icon.paintIcon(this, g2, this.getWidth() - ix2 + icon.getIconWidth(), 
                        this.getHeight()/2 - icon.getIconHeight()/2);
            }
        }

    }

    private void initModel() {
        this.setModel(this.getModel());
        
        this.addMouseListener(new MouseAdapter() {
            private boolean hide = false;
            
            @Override
            public void mouseEntered(MouseEvent e) {
                setCursor(mouseOverCursor);
                
                if (menu == null)  return;
                
                if (menu.isVisible()) this.hide = true;                
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
                
                this.hide = false;      
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                
                if (menu == null)  return;
                
                if (this.hide) {
                    this.hide = false;
                    return;
                }
                
                updateMenu();
                        
                if (getText() == null)
                    menu.setPreferredSize(new Dimension(getWidth(), 
                            (int) menu.getPreferredSize().getHeight()));
                
                menu.show((Component) e.getSource(), 0, getHeight()-1);
                
                this.hide = true;
            }
        });
    }

    private void createPopupMenu() {
        this.menu = new JPopupMenu();
        this.initMenu();
    }
    
    private void updateSize() {
        
        if (this.getText() == null && (this.actions == null || this.actions.isEmpty()))
            return;
        
        Rectangle2D labelRect = null;
                
        if (this.getText() != null && !this.getText().isEmpty()) {
            labelRect = FontUtils.getLongestTextArea(this.getFont(), this.getText());
        } 
        else {
            labelRect = FontUtils.getLongestTextArea(this.getFont(), this.actions.toArray());
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

        this.setMinimumSize(new Dimension((int)labelRect.getWidth() + ix1 + ix2, 
                (int)labelRect.getHeight()+iy1+iy2));
        
        int height = (int)this.getPreferredSize().getHeight();
        if (height < this.getMinimumSize().getHeight()) height = (int)this.getMinimumSize().getHeight();
        
        this.setPreferredSize(new Dimension((int)labelRect.getWidth() + ix1 + ix2, 
                height));
        
        this.setSize(this.getPreferredSize());
                
        this.revalidate();
        this.repaint();

    }
    
    private void updateMenu() {

        if (this.isMenuOverrided) {

            if (this.updateHandler == null) {
                return;
            }

            // Notify that the overrided menu
            // is ready to be updated
            this.updateHandler.fireUpdate();

            return;
        }

        if (this.isMenuUpdated) {
            return;
        }
        this.isMenuUpdated = true;
        
        this.menu.removeAll();
        
        for ( Action action : this.actions ) {
        
            Action compoundAction = action;
            
            // Wrap the action if this combo box has room for selection
            if (this.getText() == null)
                compoundAction = new SelectionActionWrapper(compoundAction);
            
            JMenuItem menuItem = new JMenuItem(compoundAction);
            menuItem.setBackground(Color.WHITE);
            menuItem.setForeground(Color.BLACK);
            menuItem.setFont(this.getFont());
            
            menuItem.setBorder(BorderFactory.createEmptyBorder(0,1,0,0));
            
            this.menu.add(menuItem);
        }

    }
    
    private void initMenu() {
        this.menu.setBorder(BorderFactory.createLineBorder(Color.BLACK,1));
        this.menu.setBackground(Color.WHITE);
        this.menu.setForeground(Color.BLACK);
        this.menu.setFont(this.getFont());
    }
    

    
    public interface SelectionListener {
        public void selectionChanged(Action item);
    }
    
    public interface UpdateHandler {
        public void fireUpdate();
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

            // Fire the parent listeners
            for ( SelectionListener listener : selectionListeners )
                listener.selectionChanged(wrappedAction);
            
            // Call original action
            this.wrappedAction.actionPerformed(e);   
            
            repaint();
        }
        
    }
}
