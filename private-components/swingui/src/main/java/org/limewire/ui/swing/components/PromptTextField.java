package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;
import javax.swing.border.Border;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PaintUtils;

public class PromptTextField extends JTextField implements FocusListener {
    
    private String promptText;
    
    @Resource
    private int arcWidth;
    
    @Resource
    private int arcHeight;
        
    @Resource 
    private Color borderColour;
    
    private final Color borderBevel1;
    private final Color borderBevel2;
    private final Color borderBevel3;
    
    public PromptTextField() {
        GuiUtils.assignResources(this);
        
        this.borderBevel1 = PaintUtils.lighten(this.borderColour, 120);
        this.borderBevel2 = PaintUtils.lighten(this.borderColour, 80); 
        this.borderBevel3 = PaintUtils.lighten(this.borderColour, 100);
        
        TextFieldClipboardControl.install(this);
        addFocusListener(this);
        this.setOpaque(false);
        this.setBorder(new TextBorder());

        this.setPreferredSize(new Dimension(200, 22));
    }
    
    public PromptTextField(String promptText) {
        this();
        
        this.setPromptText(promptText);
    }
    
    public PromptTextField(String promptText, int columns) {
        this(promptText);
        
        this.setColumns(columns);
    }
    
    public void setPromptText(String text){
        this.promptText = text;
    }
    
    private class TextBorder implements Border {

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
        
        paintTextArea(g2);        
        super.paintComponent(g);
        paintPrompt(g2);
    }
    
    protected void paintTextArea(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, getHeight());
        
        PaintUtils.drawRoundedBorder(g2, 0, 0, getWidth()-1, getHeight()-1, 
                this.arcWidth, this.arcHeight, this.borderColour,
                this.borderBevel1, this.borderBevel2, this.borderBevel3);
    }
    
    protected void paintPrompt(Graphics2D g2) {
        if (!hasFocus() && getText().isEmpty() && this.promptText != null) {
            g2.setColor(Color.LIGHT_GRAY);
            FontMetrics fm = g2.getFontMetrics();
            Border border = getBorder();
            int x = border != null ? border.getBorderInsets(this).left : 0;
            PaintUtils.drawSmoothString(g2, this.promptText, x, fm.getAscent() + 3);
        }
    }
}
