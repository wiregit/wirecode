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
public class NumberedHyperLinkButton extends JButton implements MouseListener {
    private Color mouseOverColor = getForeground();
    private Color foregroundColor = getForeground();
    
    private int displayNumber = 0;
    
    private String text;
    
    private Cursor oldCursor;
    
    public NumberedHyperLinkButton(String text) {
        this.text = text;
        FontUtils.underline(this);
        updateLabel();
        
        setBorder(null);
        setContentAreaFilled(false);
        setFocusable(false);
        addMouseListener(this);
    }
    
    public NumberedHyperLinkButton(String text, Action action) {
        super(action);

        this.text = text;
        FontUtils.underline(this);
        updateLabel();
        
        setBorder(null);
        setContentAreaFilled(false);
        setFocusable(false);
        addMouseListener(this);
    }
    
    public void setMouseOverColor(Color color) {
        this.mouseOverColor = color;
    }
    
    @Override
    public void setForeground(Color color) {
        super.setForeground(color);
        this.foregroundColor = color;
    }        
    
    @Override
    public void setText(String text) {
        this.text = text;
        updateLabel();
    }
    
    public void setDisplayNumber(int value) {
        this.displayNumber = value;
        updateLabel();
    }
    
    private void updateLabel() {
        super.setText(text + "(" + displayNumber + ")");
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
        super.setForeground(mouseOverColor);
        oldCursor = getCursor();
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public void mouseExited(MouseEvent e) {
        super.setForeground(foregroundColor);
        setCursor(oldCursor);
    }

    @Override
    public void mousePressed(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseClicked(MouseEvent e) {}
}