package org.limewire.ui.swing.components;

import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;
import javax.swing.JSlider;

import org.jdesktop.swingx.painter.AbstractPainter;

public class LimeSliderBar extends JSlider {
    private AbstractPainter<JComponent> backgroundPainter;
    private AbstractPainter<JSlider> foregroundPainter;
    
    public LimeSliderBar(AbstractPainter<JSlider> foregroundPainter,
            AbstractPainter<JComponent> backgroundPainter) {
        
        this.backgroundPainter = backgroundPainter;
        this.foregroundPainter = foregroundPainter;        
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        backgroundPainter.paint((Graphics2D) g, this, getWidth(), getHeight());
        foregroundPainter.paint((Graphics2D) g, this, getWidth(), getHeight());
    }
}
