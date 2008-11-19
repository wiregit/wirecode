package org.limewire.ui.swing.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.LayoutManager;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.util.GuiUtils;

public class LimeHeaderBar extends JXPanel {
    
    private final JXLabel headerLabel;
    
    private final JPanel componentContainer;
    
    public LimeHeaderBar() {
        this("");
    }
    
    public LimeHeaderBar(String title) {
        GuiUtils.assignResources(this);
        
        this.headerLabel = new LimeHeadingLabel(title);
        this.componentContainer = new JPanel();
        
        this.headerLabel.setBorder(BorderFactory.createEmptyBorder(0,10,0,2));
        this.componentContainer.setOpaque(false);
        
        super.setLayout(new BorderLayout());
        super.add(this.headerLabel, BorderLayout.WEST);
        super.add(this.componentContainer, BorderLayout.EAST);
    }

    @Override
    public Component add(Component comp) {
        return this.componentContainer.add(comp);
    }
    
    @Override
    public Component add(Component comp, int index) {
        return this.componentContainer.add(comp, index);
    }
    
    @Override
    public void add(Component comp, Object constraints) {
        this.componentContainer.add(comp, constraints);
    }
    
    @Override
    public void add(Component comp, Object constraints, int index) {
        this.componentContainer.add(comp, constraints, index);
    }
    
    @Override
    public void setLayout(LayoutManager mgr) {
        if (this.componentContainer == null) {
            super.setLayout(mgr);
        } 
        else {
            this.componentContainer.setLayout(mgr);
        }
    }
    
    public void setText(String text) {
        this.headerLabel.setText(text);
    }
    
    @Override
    public void setFont(Font font) {
        if (this.headerLabel == null) {
            super.setFont(font);
        }
        else {            
            this.headerLabel.setFont(font);
        }
    }
    
}
