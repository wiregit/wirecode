package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.ui.swing.painter.ButtonPainter;
import org.limewire.ui.swing.painter.LightButtonPainter;
import org.limewire.ui.swing.painter.PopupButtonPainter;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LimeComboBoxFactory {
    
    @Resource
    private Font  miniTextFont;
    @Resource
    private Color miniRegTextColour;
    @Resource 
    private Color miniHoverTextColour;
    @Resource
    private Color miniDownTextColour;
    @Resource
    private Icon  miniRegIcon;
    @Resource
    private Icon  miniHoverIcon;
    @Resource
    private Icon  miniDownIcon;
    
    @Resource
    private Font  fullTextFont;
    @Resource
    private Color fullTextColour;
    @Resource
    private Icon  fullIcon;
    
    
    private final Painter<JXButton> darkButtonPainter;
    private final Painter<JXButton> lightButtonPainter;
    private final Painter<JXButton> popupButtonPainter;
    
    @Inject
    LimeComboBoxFactory(ButtonPainter darkButtonPainter, 
            LightButtonPainter lightButtonPainter,
            PopupButtonPainter popupButtonPainter) {
        GuiUtils.assignResources(this);    
        
        this.darkButtonPainter = darkButtonPainter;
        this.lightButtonPainter = lightButtonPainter;
        this.popupButtonPainter = popupButtonPainter;
    }
    
    public LimeComboBox createDarkFullComboBox() {
        return this.createDarkFullComboBox(null);
    }
    
    private LimeComboBox createFullComboBox(List<Action> items, Painter<JXButton> painter) {
        
        LimeComboBox box = new LimeComboBox(items);
        
        box.setBackgroundPainter(painter);
        box.setBorder(BorderFactory.createEmptyBorder(3,10,3,20));
        box.setIcon(this.fullIcon);
        box.setFont(this.fullTextFont);
        box.setForeground(this.fullTextColour);
        
        return box;
        
    }
    
    public LimeComboBox createLightFullComboBox(List<Action> items) {
        
        LimeComboBox box = new LimeComboBox(items);
        
        box.setBackgroundPainter(this.lightButtonPainter);
        box.setBorder(BorderFactory.createEmptyBorder(2,10,2,20));
        box.setIcon(this.miniRegIcon);
        box.setFont(this.fullTextFont);
        box.setForeground(this.miniRegTextColour);
        
        return box;
    }
    
    public LimeComboBox createDarkFullComboBox(List<Action> items) {
        return createFullComboBox(items, darkButtonPainter);
    }
    

    
    public LimeComboBox createMiniComboBox() {
        return createMiniComboBox(null,null);
    }
    
    public LimeComboBox createMiniComboBox(String promptText, List<Action> items) {
    
        LimeComboBox box = new LimeComboBox(items);
        
        box.setBackgroundPainter(popupButtonPainter);
        box.setBorder(BorderFactory.createEmptyBorder(2,6,2,15));
        box.setFont(this.miniTextFont);
        box.setForegrounds(this.miniRegTextColour, 
                this.miniHoverTextColour, this.miniDownTextColour);
        box.setIcons(this.miniRegIcon, this.miniHoverIcon, this.miniDownIcon);
        box.setText(promptText);
        
        return box;
        
    }
}
