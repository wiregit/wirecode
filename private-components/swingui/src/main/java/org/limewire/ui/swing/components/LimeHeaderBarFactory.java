package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.painter.GenericBarPainter;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

import com.google.inject.Singleton;

@Singleton
public class LimeHeaderBarFactory {

    @Resource private int height;
    @Resource private Color gradientTop;
    @Resource private Color gradientBottom;
    @Resource private Color borderTop1;
    @Resource private Color borderTop2;
    @Resource private Font headingFont;
    
    LimeHeaderBarFactory() {
        GuiUtils.assignResources(this);
    }
    
    public LimeHeaderBar createBasic(String text) {
        LimeHeaderBar bar = new LimeHeaderBar(text);
        this.decorateBasic(bar);
        return bar;        
    }
    
    public void decorateBasic(JXPanel bar) {
        bar.setBackgroundPainter(new GenericBarPainter<LimeHeaderBar>(
                new GradientPaint(0,0,this.gradientTop,0,1,this.gradientBottom), 
                this.borderTop1, this.borderTop2, PainterUtils.TRASPARENT, PainterUtils.TRASPARENT));
        
        bar.setMinimumSize(new Dimension(0, this.height));
        bar.setPreferredSize(new Dimension(3000, this.height));
        bar.setFont(this.headingFont);
    }
    
    public void decorateSpecial(JXPanel bar) {
        bar.setBackgroundPainter(new GenericBarPainter<LimeHeaderBar>(
                new GradientPaint(0,0,new Color(209,247,144),0,1,new Color(209,247,144)), 
                this.borderTop1, this.borderTop2, PainterUtils.TRASPARENT, PainterUtils.TRASPARENT));
        
        bar.setMinimumSize(new Dimension(0, this.height));
        bar.setPreferredSize(new Dimension(3000, this.height));
        bar.setFont(this.headingFont);
    }
}
