package org.limewire.ui.swing.components;

import java.awt.Cursor;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LimeComboBoxFactory {
    
    private final ButtonDecorator buttonDecorator;
    
    @Resource private Icon  miniRegIcon;
    @Resource private Icon  miniHoverIcon;
    @Resource private Icon  miniDownIcon;
    @Resource private Icon  lightFullIcon;
    @Resource private Icon  darkFullIcon;
    
    @Inject
    LimeComboBoxFactory(ButtonDecorator buttonDecorator) {
        GuiUtils.assignResources(this);  
        
        this.buttonDecorator = buttonDecorator;        
    }
    
    public LimeComboBox createDarkFullComboBox() {
        return this.createDarkFullComboBox(null);
    }
    
    public LimeComboBox createDarkFullComboBox(List<Action> items) {
        LimeComboBox box = new LimeComboBox(items);
        buttonDecorator.decorateDarkFullButton(box);
        box.setIcon(this.darkFullIcon);
        box.setBorder(BorderFactory.createEmptyBorder(2,10,2,20));
        return box;
    }
    
    public LimeComboBox createLightFullComboBox(List<Action> items) {
        LimeComboBox box = new LimeComboBox(items);
        buttonDecorator.decorateLightFullButton(box);
        box.setIcon(this.lightFullIcon);
        box.setBorder(BorderFactory.createEmptyBorder(2,10,2,20));
        return box;
    }
    
    public LimeComboBox createMiniComboBox() {
        return createMiniComboBox(null,null);
    }
    
    public LimeComboBox createMiniComboBox(String promptText, List<Action> items) {
        LimeComboBox box = new LimeComboBox(items);
        this.decorateMiniComboBox(box, promptText);
        return box;
    }
    
    public void decorateMiniComboBox(LimeComboBox box, String promptText) {
        buttonDecorator.decorateMiniButton(box);
        box.setIcons(this.miniRegIcon, this.miniHoverIcon, this.miniDownIcon);
        box.setBorder(BorderFactory.createEmptyBorder(2,6,2,15));
        box.setText(promptText);
        box.setMouseOverCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
