package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;

import org.jdesktop.application.Resource;
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
    
    LimeHeaderBarFactory() {
        GuiUtils.assignResources(this);
    }
    
    public LimeHeaderBar createBasic(String text) {
        LimeHeaderBar bar = new LimeHeaderBar(text);
        
        bar.setBackgroundPainter(new GenericBarPainter<LimeHeaderBar>(
                new GradientPaint(0,0,this.gradientTop,0,1,this.gradientBottom), 
                this.borderTop1, this.borderTop2, PainterUtils.TRASPARENT, PainterUtils.TRASPARENT));
        
        bar.setMinimumSize(new Dimension(0, this.height));
        bar.setPreferredSize(new Dimension(3000, this.height));
        
        return bar;        
    }
    
}
