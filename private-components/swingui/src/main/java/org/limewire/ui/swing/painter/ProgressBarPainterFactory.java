package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;

import javax.swing.JComponent;
import javax.swing.JProgressBar;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Singleton;

@Singleton
public class ProgressBarPainterFactory {

    @Resource private Color border;
    @Resource private Color borderDisabled;
    
    @Resource private Color barBackgroundGradientTop;
    @Resource private Color barBackgroundGradientBottom;
    
    @Resource private Color barForegroundGradientTop;
    @Resource private Color barForegroundGradientBottom;
    @Resource private Color barDisabledForegroundGradientTop;
    @Resource private Color barDisabledForegroundGradientBottom;
    
    public ProgressBarPainterFactory() { 
        GuiUtils.assignResources(this);
    }
    
    public AbstractPainter<JComponent> createBackgroundPainter() {
        return new ProgressBarBackgroundPainter(
             new GradientPaint(0,0,this.barBackgroundGradientTop,0,1,this.barBackgroundGradientBottom),
             this.border, this.borderDisabled);
    }
    
    public AbstractPainter<JProgressBar> createForegroundPainter() {
        return new ProgressBarForegroundPainter(
             new GradientPaint(0,0,this.barForegroundGradientTop,0,1,this.barForegroundGradientBottom),
             new GradientPaint(0,0,this.barDisabledForegroundGradientTop,0,1,this.barDisabledForegroundGradientBottom));
    }
}
