package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BorderFactory;
import javax.swing.JTextField;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.painter.BorderPainter;
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
    
    private Painter<JTextField> backgroundPainter;
    private Painter<PromptTextField> promptPainter;
    
    public PromptTextField() {
        GuiUtils.assignResources(this);
        
        this.borderBevel1 = PaintUtils.lighten(this.borderColour, 120);
        this.borderBevel2 = PaintUtils.lighten(this.borderColour, 80); 
        this.borderBevel3 = PaintUtils.lighten(this.borderColour, 100);
        
        TextFieldClipboardControl.install(this);
        addFocusListener(this);
        this.setOpaque(false);
        
        this.setPreferredSize(new Dimension(200, 22));
        
        this.setBorder(BorderFactory.createEmptyBorder(2,10,2,12));
        this.backgroundPainter = createBackgroundPainter();
        this.promptPainter = createPromptPainter();
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
    
    public String getPromptText() {
        return this.promptText;
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
    
    private static Painter<PromptTextField> createPromptPainter() {
        AbstractPainter<PromptTextField> painter = new AbstractPainter<PromptTextField>() {

            @Override
            protected void doPaint(Graphics2D g, PromptTextField object, int width, int height) {
                g.setColor(Color.LIGHT_GRAY);
                FontMetrics fm = g.getFontMetrics();
                int x = object.getInsets().left;
                g.drawString(object.getPromptText(), x, fm.getAscent() + 3);
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
        painter.setFillVertical(true);
        painter.setFillHorizontal(true);
        painter.setAntialiasing(true);
        
        compoundPainter.setPainters(painter, new BorderPainter<JTextField>(this.arcWidth, this.arcHeight,
                this.borderColour, this.borderBevel1, this.borderBevel2, this.borderBevel3));
        
        return compoundPainter;
    }
}
