package org.limewire.ui.swing.wizard;

import java.awt.Color;
import java.awt.GradientPaint;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.painter.GenericBarPainter;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

/**
 * A non singleton decorator class for the special components
 *  used during the setup wizard
 */
public class SetupComponentDecorator {
    
    @Resource private Icon largeBox;
    @Resource private Icon largeBoxChecked;
    @Resource private Icon largeRadio;
    @Resource private Icon largeRadioChecked;
    @Resource private Icon smallBox;
    @Resource private Icon smallBoxTicked;
    @Resource private Icon smallBoxXed;
   
    @Resource private Color headerGradientTop;
    @Resource private Color headerGradientBottom;
    @Resource private Color headerTopBorder1 = PainterUtils.TRASPARENT;
    @Resource private Color headerTopBorder2 = PainterUtils.TRASPARENT;
    @Resource private Color headerBottomBorder1 = PainterUtils.TRASPARENT;
    @Resource private Color headerBottomBorder2 = PainterUtils.TRASPARENT;
    
    private final GenericBarPainter<JXPanel> pooledBarPainter;
    
    SetupComponentDecorator() {
        GuiUtils.assignResources(this);
        
        pooledBarPainter = new GenericBarPainter<JXPanel>(
                new GradientPaint(0,0, headerGradientTop, 0,1, headerGradientBottom, false),
                headerTopBorder1, headerTopBorder2, headerBottomBorder1, headerBottomBorder2);
    }

    public void decorateTickCheckBox(JCheckBox box) {
        box.setIcon(smallBox);
        box.setSelectedIcon(smallBoxTicked);
        box.setOpaque(false);
    }
    
    public void decorateXCheckBox(JCheckBox box) {
        box.setIcon(smallBox);
        box.setSelectedIcon(smallBoxXed);
        box.setOpaque(false);
        box.setFocusPainted(false);
    }
    
    public void decorateLargeCheckBox(JCheckBox box) {
        box.setIcon(largeBox);
        box.setSelectedIcon(largeBoxChecked);
        box.setOpaque(false);
        box.setFocusPainted(false);
    }
    
    public void decorateLargeRadioButton(JRadioButton box) {
        box.setIcon(largeRadio);
        box.setSelectedIcon(largeRadioChecked);
        box.setOpaque(false);
        box.setFocusPainted(false);
    }
    
    public void decorateContinueButton(JXButton button) {
        button.setBackground(Color.GREEN);
    }   
    
    public void decorateSetupHeader(JXPanel header) {
        header.setBackgroundPainter(pooledBarPainter);
    }
}
