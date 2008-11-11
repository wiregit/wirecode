package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.painter.BorderPainter;
import org.limewire.ui.swing.util.GuiUtils;

public class PromptTextField extends JTextField implements FocusListener {
    
    private String promptText;
    
    @Resource private Color promptColour;
    @Resource private int arcWidth;
    @Resource private int arcHeight;
    @Resource private Color borderColour;
    @Resource private Color bevelLeft;
    @Resource private Color bevelTop1;
    @Resource private Color bevelTop2;
    @Resource private Color bevelRight;
    @Resource private Color bevelBottom;
    
    private Painter<JTextField> backgroundPainter;
    private Painter<PromptTextField> promptPainter;
    
    /**
     * Controls the visibility of the faint white accent bubble
     *  that is painted under components in some areas. 
     */
    private boolean borderBubbleVisible = false;
    
    
    public PromptTextField() {
        this.init();
    }
    
    public PromptTextField(boolean borderBubbleVisible) {
        this.borderBubbleVisible = borderBubbleVisible;

        this.init();
    }
    
    public PromptTextField(String promptText) {
        this.setPromptText(promptText);
        
        this.init();
    }
    
    public PromptTextField(String promptText, boolean borderBubbleVisible) {
        this.promptText = promptText;
        this.borderBubbleVisible = true;
    
        init();
    }
    
    
    public PromptTextField(String promptText, int columns) {
        this.promptText = promptText;
        this.setColumns(columns);
        
        init();
    }
    
    private void init() {
        GuiUtils.assignResources(this);

        TextFieldClipboardControl.install(this);
        this.addFocusListener(this);
        this.setOpaque(false);
        
        this.setBorder(BorderFactory.createEmptyBorder(2,10,2,12));
        this.backgroundPainter = createBackgroundPainter();
        this.promptPainter = createPromptPainter();        
    }
    
    
    public void setPromptText(String text){
        this.promptText = text;
    }
    
    public String getPromptText() {
        return this.promptText;
    }
    
    /**
     * Controls visibility of the white accent bubble painted
     *  under some components.
     */
    public void setBorderBubbleVisible(boolean visible) {
        this.borderBubbleVisible = visible;
        
        System.out.println(visible);
        
        this.backgroundPainter = createBackgroundPainter();
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
     * Sets the background painter for this component
     */
    public void setBackgroundPainter(Painter<JTextField> painter) {
        this.backgroundPainter = painter;
    }
    
    /**
     * Sets the painter on this component that manages painting of 
     *  the prompt text when the text field is empty
     */
    public void setPromptPainter(Painter<PromptTextField> painter) {
        this.promptPainter = painter;
    }
    
    /**
     * Paints this component, including an icon and
     * the default text when this component has focus and has no text value.
     */
    @Override
    protected void paintComponent(Graphics g) {

        this.backgroundPainter.paint((Graphics2D) g, this, this.getWidth(), this.getHeight());
        
        super.paintComponent(g);
    
        if (!hasFocus() && getText().isEmpty() && this.promptText != null) {
            this.promptPainter.paint((Graphics2D) g, this, this.getWidth(), this.getHeight());
        }

    }
    
    private Painter<PromptTextField> createPromptPainter() {
        AbstractPainter<PromptTextField> painter = new AbstractPainter<PromptTextField>() {

            @Override
            protected void doPaint(Graphics2D g, PromptTextField object, int width, int height) {
                g.setColor(promptColour);
                g.setFont(object.getFont());
                
                int dot  = object.getCaret().getDot();
                Rectangle r = null;
                
                // Find the carat position
                try {
                    r = object.modelToView(dot);
                } catch (BadLocationException e) { return; }
                
                int x = r.x;
                int y = r.y + r.height - 3;
                g.drawString(object.getPromptText(), x, y);
            }
        };
        
        painter.setAntialiasing(true);
        
        return painter;
    }
    
    private Painter<JTextField> createBackgroundPainter() {
        
        CompoundPainter<JTextField> compoundPainter = new CompoundPainter<JTextField>();
        
        RectanglePainter<JTextField> painter = new RectanglePainter<JTextField>();
        
        painter.setRounded(true);
        painter.setFillPaint(Color.WHITE);
        painter.setRoundWidth(this.arcWidth);
        painter.setRoundHeight(this.arcHeight);
        painter.setInsets(new Insets(2,2,2,2));
        painter.setBorderPaint(null);
        painter.setFillVertical(true);
        painter.setFillHorizontal(true);
        painter.setAntialiasing(true);
        
        compoundPainter.setPainters(painter, new BorderPainter<JTextField>(this.arcWidth, this.arcHeight,
                this.borderColour,  this.bevelLeft,  this.bevelTop1,  this.bevelTop2, 
                this.bevelRight,  this.bevelBottom, this.borderBubbleVisible));
        
        return compoundPainter;
    }
}
