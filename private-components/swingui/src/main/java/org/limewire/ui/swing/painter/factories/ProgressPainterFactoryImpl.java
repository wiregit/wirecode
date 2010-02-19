package org.limewire.ui.swing.painter.factories;

import java.awt.Color;
import java.awt.GradientPaint;

import javax.swing.JComponent;
import javax.swing.JProgressBar;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.activation.api.ActivationManager;
import org.limewire.ui.swing.painter.ProgressBarBackgroundPainter;
import org.limewire.ui.swing.painter.ProgressBarForegroundPainter;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

import com.google.inject.Inject;

public class ProgressPainterFactoryImpl implements ProgressPainterFactory {
    
    @Resource private Color barBorderDisabled = PainterUtils.TRASPARENT;
    @Resource private Color barBackgroundGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color barBackgroundGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color barDisabledForegroundGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color barDisabledForegroundGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color barUpperAccent = PainterUtils.TRASPARENT;
    
    @Resource private Color basicBarBorder = PainterUtils.TRASPARENT;
    @Resource private Color basicBarForegroundGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color basicBarForegroundGradientBottom = PainterUtils.TRASPARENT;
    
    @Resource private Color proBarBorder = PainterUtils.TRASPARENT;
    @Resource private Color proBarForegroundGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color proBarForegroundGradientBottom = PainterUtils.TRASPARENT;  
    
    private final ActivationManager activationManager;
    
    @Inject
    public ProgressPainterFactoryImpl(ActivationManager activationManager) { 
        this.activationManager = activationManager;
        
        GuiUtils.assignResources(this);
    }
    
    @Override
    public AbstractPainter<JComponent> createBackgroundPainter() {
        if(activationManager.isProActive()) {
            return new ProgressBarBackgroundPainter(
                    new GradientPaint(0,0,this.barBackgroundGradientTop,0,1,this.barBackgroundGradientBottom),
                    this.proBarBorder, this.barBorderDisabled);
        } else {
            return new ProgressBarBackgroundPainter(
                    new GradientPaint(0,0,this.barBackgroundGradientTop,0,1,this.barBackgroundGradientBottom),
                    this.basicBarBorder, this.barBorderDisabled);
        }
    }
    
    @Override
    public AbstractPainter<JProgressBar> createForegroundPainter() {
        if(activationManager.isProActive()) {
            return new ProgressBarForegroundPainter<JProgressBar>(
                    new GradientPaint(0,0,this.proBarForegroundGradientTop,0,1,this.proBarForegroundGradientBottom),
                    new GradientPaint(0,0,this.barDisabledForegroundGradientTop,0,1,this.barDisabledForegroundGradientBottom),
                    barUpperAccent);
        } else {
            return new ProgressBarForegroundPainter<JProgressBar>(
                    new GradientPaint(0,0,this.basicBarForegroundGradientTop,0,1,this.basicBarForegroundGradientBottom),
                    new GradientPaint(0,0,this.barDisabledForegroundGradientTop,0,1,this.barDisabledForegroundGradientBottom));
        }
    }
}
