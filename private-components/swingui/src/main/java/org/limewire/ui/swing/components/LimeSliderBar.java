package org.limewire.ui.swing.components;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;
import javax.swing.JSlider;

import org.jdesktop.swingx.painter.AbstractPainter;

public class LimeSliderBar extends JSlider implements MouseListener {
    private AbstractPainter<JComponent> backgroundPainter;
    private AbstractPainter<JSlider> foregroundPainter;
    
    public LimeSliderBar() {
        addMouseListener(this);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        if (backgroundPainter != null && foregroundPainter != null) {
            backgroundPainter.paint((Graphics2D) g, this, getWidth(), getHeight());
            foregroundPainter.paint((Graphics2D) g, this, getWidth(), getHeight());
        }
        else {
            super.paintComponent(g);
        }
    }
    
    public void setForegroundPainter(AbstractPainter<JSlider> painter) {
        this.foregroundPainter = painter;
    }
    
    public void setBackgroundPainter(AbstractPainter<JComponent> painter) {
        this.backgroundPainter = painter;
    }


    @Override
    public void mouseClicked(MouseEvent e) {
    }
    @Override
    public void mouseEntered(MouseEvent e) {
        repaint();
    }
    @Override
    public void mouseExited(MouseEvent e) {
        repaint();
    }
    @Override
    public void mousePressed(MouseEvent e) {
    }
    @Override
    public void mouseReleased(MouseEvent e) {
    }
}
