package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.geom.Point2D;

import javax.swing.Icon;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.MattePainter;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.util.GuiUtils;

public class SectionHeading extends JXPanel {
    
    @Resource private int height;
    
    @Resource private Color topGradient;
    @Resource private Color bottomGradient;
    
    @Resource private Color borderColor;
    @Resource private int borderHeight;
    
    @Resource private Color textColor;
    @Resource private Font textFont;
    
    private final JXLabel label;
    
    public SectionHeading(String text) {
        GuiUtils.assignResources(this);
        
        this.label = new JXLabel(text);
        label.setOpaque(false);
        label.setHorizontalTextPosition(SwingConstants.RIGHT);
        label.setIconTextGap(4);
        label.setFont(textFont);
        label.setForeground(textColor);
        
        setLayout(new MigLayout("insets 0", "[grow]", ""));
        add(label, "grow, gapleft 4, gaptop 4, alignx left, aligny center, wrap");
        
        setBackgroundPainter(new MattePainter(
                new GradientPaint(new Point2D.Double(0, 0), topGradient, 
                        new Point2D.Double(0, 1), bottomGradient,
                        false), true));
        
        add(Line.createHorizontalLine(borderColor, borderHeight), "grow");
        
        setMinimumSize(new Dimension(0, height+borderHeight));
        setMaximumSize(new Dimension(Short.MAX_VALUE, height+borderHeight));
        setPreferredSize(new Dimension(Short.MAX_VALUE, height+borderHeight));
    }
    
    public void setIcon(Icon icon) {
        label.setIcon(icon);
    }

    public void setText(String text) {
        label.setText(text);
    }
    
    
    

    
    
}
