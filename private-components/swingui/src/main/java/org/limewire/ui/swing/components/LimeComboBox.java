package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jdesktop.swingx.JXButton;

import com.limegroup.gnutella.gui.actions.AbstractAction;

public class LimeComboBox extends JXButton {

    private Action[] actions;
    private Action   selectedAction;
            
    private Color pressedTextColour = null;
    private Color rolloverTextColour = null;
    
    private boolean hasSize = false;
    
    LimeComboBox(Action...actions) {
        this.setText(null);
        
        this.actions = actions;
        
        if (this.actions.length > 0)
            this.selectedAction = actions[0];
        else
            this.selectedAction = null;
        
    }

    @Override
    public void setText(String promptText) {
        this.hasSize = false;
        
        super.setText(promptText);
    }
    
    public void setIcons(Icon regular, Icon hover, Icon down) {
        this.setIcon(regular);
        this.setRolloverIcon(hover);
        this.setPressedIcon(down);
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
            labelRect = this.getLongestTextArea((Object[])this.actions);
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
                icon.paintIcon(this, g2, this.getWidth() - ix2 - icon.getIconWidth(), 
                        this.getHeight()/2 - icon.getIconHeight()/2);
            }
        }
    }
    
    
    
    
    public static void main(String[] args) {
        
        JFrame window = new JFrame();
        
        JPanel panel = new JPanel(new FlowLayout());
        panel.setBackground(Color.CYAN);
        
        Action one = new AbstractAction("hola") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                
            }
        };
        Action two = new AbstractAction("ayer") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                
            }
        };
        AbstractAction three = new AbstractAction("palabrasssssssss") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                
                
                
            }
            
             
            
        };
        
        Action[] actions = new Action[] {one,two,three};
        
        panel.add(new LimeComboBoxFactory().createFullComboBox(actions));
        
        panel.add(new LimeComboBoxFactory().createMiniComboBox("hello",actions));
        
        window.add(panel);
        window.pack();
        window.validate();
        window.setVisible(true);
        window.setSize(new Dimension(500,500));
    }
    
}
