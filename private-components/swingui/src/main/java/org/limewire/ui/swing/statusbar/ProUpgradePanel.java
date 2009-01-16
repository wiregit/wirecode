package org.limewire.ui.swing.statusbar;

import java.util.HashSet;
import java.util.Set;

import org.jdesktop.swingx.JXLabel;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ProUpgradePanel extends JXLabel {

    private final Set<InvisibilityCondition> conditions = new HashSet<InvisibilityCondition>();
    
    @Inject
    public ProUpgradePanel() { //boolean isPro) {
        super("");
        
        setVisible(false);
        
        if (true) { // isPro) {
            addCondition(InvisibilityCondition.IS_PRO);
        }
    }
    
    public void addCondition(InvisibilityCondition condition) {
        conditions.add(condition);
        updateVisibility();
    }
    
    public void removeCondition(InvisibilityCondition condition) {
        conditions.remove(condition);
        updateVisibility();
    }
    
    private void updateVisibility() {
        setVisible(conditions.isEmpty());
    }
        
    public static enum InvisibilityCondition {
        PANELS_SHOWN, PRO_ADD_SHOWN, IS_PRO ;
    }
}
