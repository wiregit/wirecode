package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;

/**
 * This class is a special text field used for filtering search results.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
class FilteredTextField extends JTextField implements FocusListener {
    
    private String PROMPT_TEXT = "Filter results...";
    
    /**
     * Creates a FilteredTextField that displays a given number of columns.
     * @param columns
     */
    FilteredTextField(int columns) {
        super(columns);
        
        //focusLost(null);
        addFocusListener(this);
    }
    
    /**
     * Repaints this component when focus is gained
     * so default text can be removed.
     */
    @Override
    public void focusGained(FocusEvent e) {
        repaint();
    }
    
    /**
     * Repaints this component when focus is lost
     * so default text can be displayed if no text has been entered.
     */
    @Override
    public void focusLost(FocusEvent e) {
        repaint();
    }
    
    /**
     * Paints this component, including an icon and
     * the default text when this component has focus and has no text value.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        boolean valueEntered = getText().length() > 0;
        if (!hasFocus() && !valueEntered) {
            g.setColor(Color.LIGHT_GRAY);
            FontMetrics fm = g.getFontMetrics();
            int x = 2; // using previous translate
            int y = fm.getAscent() + 2;
            g.drawString(PROMPT_TEXT, x, y);
        }
    }
}
