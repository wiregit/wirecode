package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.components.LimeHeaderBar;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BarPainterFactory {

    @Resource private Color headerBarGradientTop;
    @Resource private Color headerBarGradientBottom;
    
    // TODO: Talk to Anthony about where this is visable and not
    //        possibly make new painter creation function
    @Resource private Color headerBarBorderTop1;

    @Resource private Color topBarGradientTop;
    @Resource private Color topBarGradientBottom;
    @Resource private Color topBarBorderTop1; 
    @Resource private Color topBarBorderBottom1;
    
    @Resource private Color statusBarTopGradient;
    @Resource private Color statusBarBottomGradient;
    @Resource private Color statusBarTopBorder1;
    @Resource private Color statusBarTopBorder2;
    
    @Resource private Color downloadSummaryBarTopGradient;
    @Resource private Color downloadSummaryBarBottomGradient;
    @Resource private Color downloadSummaryBarTopBorder1;
    
    @Inject
    BarPainterFactory() {
        GuiUtils.assignResources(this);
        
        if (OSUtils.isAnyMac()) {
            this.topBarBorderTop1 = PainterUtils.TRASPARENT;
        }
    }
    
    public GenericBarPainter<LimeHeaderBar> createHeaderBarPainter() {
        return new GenericBarPainter<LimeHeaderBar>(
            new GradientPaint(0,0,this.headerBarGradientTop,0,1,this.headerBarGradientBottom), 
            PainterUtils.TRASPARENT, PainterUtils.TRASPARENT, 
            PainterUtils.TRASPARENT, PainterUtils.TRASPARENT);
    }
    
    // TODO: Get a real non florescent scheme
    public GenericBarPainter<LimeHeaderBar> createSpecialHeaderBarPainter() {
        return new GenericBarPainter<LimeHeaderBar>(
                new GradientPaint(0,0,new Color(209,247,144),0,1,new Color(209,247,144)), 
                this.headerBarBorderTop1, PainterUtils.TRASPARENT,
                PainterUtils.TRASPARENT, PainterUtils.TRASPARENT);
    }
    
    public GenericBarPainter<JXPanel> createTopBarPainter() {
        return new GenericBarPainter<JXPanel>(
            new GradientPaint(0,0,this.topBarGradientTop,0,1,this.topBarGradientBottom), 
            this.topBarBorderTop1, PainterUtils.TRASPARENT, 
            this.topBarBorderBottom1, PainterUtils.TRASPARENT);
    }
    
    public GenericBarPainter<JXPanel> createStatusBarPainter() {
        return new GenericBarPainter<JXPanel>(
            new GradientPaint(0,0,this.statusBarTopGradient,0,1,this.statusBarBottomGradient), 
            this.statusBarTopBorder1, this.statusBarTopBorder2, 
            PainterUtils.TRASPARENT, PainterUtils.TRASPARENT);
    }
    
    public GenericBarPainter<JXPanel> createDownloadSummaryBarPainter() {
        return new GenericBarPainter<JXPanel>(
            new GradientPaint(0,0,this.downloadSummaryBarTopGradient,0,1,this.downloadSummaryBarBottomGradient), 
            this.downloadSummaryBarTopBorder1, PainterUtils.TRASPARENT, 
            PainterUtils.TRASPARENT, PainterUtils.TRASPARENT);
    }
}
