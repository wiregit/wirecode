package org.limewire.ui.swing.components;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.painter.AbstractPainter;

public class LimeBuisyLabel extends JXLabel {
    
    private final Timer buisyUpdater;
    
    public LimeBuisyLabel() {
        super("test");
        
        this.setForegroundPainter(new BuisySpinnerPainter());
        
        buisyUpdater = new Timer(500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                repaint();
            }
        });
        
        buisyUpdater.start();
        
    }
    
    private static class BuisySpinnerPainter extends AbstractPainter<JXLabel> {

        private static double TICK = (2*Math.PI) / 20;
        private double theta = 0;
        
        public BuisySpinnerPainter() {
            this.setCacheable(false);
            this.setAntialiasing(true);
        }
        
        @Override
        protected void doPaint(Graphics2D g, JXLabel object, int width, int height) {
            g.rotate(theta);
            object.paintAll(g);
            theta = (theta + TICK)  % (2*Math.PI);
        }
        
    }
         
}
