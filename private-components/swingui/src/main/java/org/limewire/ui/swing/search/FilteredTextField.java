package org.limewire.ui.swing.search;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;
import javax.swing.border.Border;


/**
 * This class is a special text field used for filtering search results.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class FilteredTextField extends JTextField implements FocusListener {
    
    private String PROMPT_TEXT = tr("Filter results...");
    
    /**
     * Creates a FilteredTextField that displays a given number of columns.
     * @param columns
     */
    public FilteredTextField(int columns) {
        super(columns);
        init();
    }
    
    public FilteredTextField() {
        super();
        init();
    }
    
    private void init(){
        //focusLost(null);
        addFocusListener(this);
        this.setOpaque(false);
        this.setBorder(new TextBorder());
    }
    
    public void setPromptText(String text){
        PROMPT_TEXT = text;
    }
    
    class TextBorder implements Border{

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(2,10,2,12);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
       
        }
      
    }
    
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
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
        Graphics2D g2 = (Graphics2D) g;
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, getHeight());
  
        g2.setColor(new Color(0,0,0,92));
        
        g2.drawRoundRect(0, 0, this.getWidth()-1, this.getHeight()-1, 10, this.getHeight());     
        
        super.paintComponent(g);

        boolean valueEntered = getText().length() > 0;
        if (!hasFocus() && !valueEntered) {
           
            g.setColor(Color.LIGHT_GRAY);
            FontMetrics fm = g.getFontMetrics();
            int x = 10; // using previous translate
            int y = fm.getAscent() + 2;
            g.drawString(PROMPT_TEXT, x, y);
        }
    }
}
