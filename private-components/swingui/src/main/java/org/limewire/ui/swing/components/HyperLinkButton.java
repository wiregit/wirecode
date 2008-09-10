package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Action;
import javax.swing.JButton;

import org.limewire.ui.swing.util.FontUtils;

/**
 * Creates a button that is undecorated and its text behaves like a hyperlink.
 * On mouse over the text changes colors and the cursor changes to a hand as
 * is expected for a hyperlink.
 */
public class HyperLinkButton extends JButton implements MouseListener {
    protected Color mouseOverColor = getForeground();
    protected Color foregroundColor = getForeground();
    
    protected String text;
    
    protected Cursor oldCursor;
    
    public HyperLinkButton(String text) {
        this.text = text;
        FontUtils.underline(this);
        updateLabel(text);
        
        setBorder(null);
        setContentAreaFilled(false);
        setFocusable(false);
        setOpaque(false);
        addMouseListener(this);
    }
    
    public HyperLinkButton(String text, Action action) {
        super(action);

        this.text = text;
        FontUtils.underline(this);
        updateLabel(text);
        
        setBorder(null);
        setContentAreaFilled(false);
        setFocusable(false);
        addMouseListener(this);
    }
    
    public void setMouseOverColor(Color color) {
        this.mouseOverColor = color;
    }
    
    public void setForegroundColor(Color color) {
        super.setForeground(color);
        this.foregroundColor = color;
    }        
    
    public void updateLabel(String text) {
        this.text = text;
        super.setText(text);
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
        if(isEnabled()) {
            super.setForeground(mouseOverColor);
            oldCursor = getCursor();
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if(isEnabled()) {
            super.setForeground(foregroundColor);
            setCursor(oldCursor);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseClicked(MouseEvent e) {}
}