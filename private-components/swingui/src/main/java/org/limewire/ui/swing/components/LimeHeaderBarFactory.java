package org.limewire.ui.swing.components;

import java.awt.Dimension;
import java.awt.Font;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.painter.BarPainterFactory;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LimeHeaderBarFactory {

    private final BarPainterFactory painterFactory;
    
    @Resource private int height;
    @Resource private Font headingFont;
    
    @Inject
    LimeHeaderBarFactory(BarPainterFactory painterFactory) {
        GuiUtils.assignResources(this);
        
        this.painterFactory = painterFactory;
    }
    
    public LimeHeaderBar createBasic(String text) {
        LimeHeaderBar bar = new LimeHeaderBar(text);
        this.decorateBasic(bar);
        return bar;        
    }
    
    public void decorateBasic(JXPanel bar) {
        bar.setBackgroundPainter(this.painterFactory.createHeaderBarPainter());
        bar.setMinimumSize(new Dimension(0, this.height));
        bar.setPreferredSize(new Dimension(3000, this.height));
        bar.setFont(this.headingFont);
    }
    
    public void decorateSpecial(JXPanel bar) {
        bar.setBackgroundPainter(this.painterFactory.createSpecialHeaderBarPainter());
        bar.setMinimumSize(new Dimension(0, this.height));
        bar.setPreferredSize(new Dimension(3000, this.height));
        bar.setFont(this.headingFont);
    }
}
