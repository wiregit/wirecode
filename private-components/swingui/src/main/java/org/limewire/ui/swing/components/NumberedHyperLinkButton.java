package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseEvent;

import javax.swing.Action;

import org.limewire.ui.swing.util.FontUtils;

/**
 * Creates a button that is undecorated and its text behaves like a hyperlink.
 * On mouse over the text changes colors and the cursor changes to a hand as
 * is expected for a hyperlink. Displays the number of files associated with
 * this hyperlink.
 */
public class NumberedHyperLinkButton extends HyperLinkButton {
    
    private int displayNumber = 0;
    
    private Color disabledColor;
    
    public NumberedHyperLinkButton(String text) {
        super(text);
    }
    
    public NumberedHyperLinkButton(String text, Action action) {
        super(text, action);
    }
    
    public void setDisplayNumber(int value) {
        this.displayNumber = value;
        if(value > 0) {
            FontUtils.underline(this);
            setEnabled(true);
        } else {
            FontUtils.removeUnderline(this);
            setEnabled(false);
        }
        super.setText(text + "(" + displayNumber + ")");
    }
    
    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);
        if(value) {
            super.setForeground(foregroundColor);
        } else {
            super.setForeground(disabledColor);
        }
    }
    
    public void setDisabledColor(Color color) {
        this.disabledColor = color;
    }
    
    @Override
    public void updateLabel(String text) {
        this.text = text;
        super.setText(text + "(" + displayNumber + ")");
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
        if(displayNumber > 0 && isEnabled()) {
            super.setForeground(mouseOverColor);
            oldCursor = getCursor();
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }
}