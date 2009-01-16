package org.limewire.ui.swing.statusbar;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;

import org.limewire.core.api.Application;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ProStatusPanel extends HyperlinkButton {

    private final Set<InvisibilityCondition> conditions = new HashSet<InvisibilityCondition>();
    
    @Inject
    public ProStatusPanel(Application application) {
        super(new AbstractAction("For faster downloads, get LimeWire PRO") {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                NativeLaunchUtils.openURL("http://www.limewire.com/download/pro.php?ref=lwn");
            }
        });

        setName("ProStatusPanel");
        
        if (application.isProVersion()) {
            addCondition(InvisibilityCondition.IS_PRO);
        }
    }
    
    /**
     * Add a new visibility condition, will probably result
     *  in the panel being hidden
     */
    public void addCondition(InvisibilityCondition condition) {
        conditions.add(condition);
        updateVisibility();
    }
    
    /**
     * Remove a visibility condition.  If there are none left the 
     *  panel will be shown.
     */
    public void removeCondition(InvisibilityCondition condition) {
        conditions.remove(condition);
        updateVisibility();
    }
    
    private void updateVisibility() {
        setVisible(conditions.isEmpty());
    }
        
    /**
     * Conditions that cause this panel to be hidden.
     *  If any of these conditions are added to the panel
     *  it will not be visible.
     */
    public static enum InvisibilityCondition {
        NOT_FULLY_CONNECTED, PRO_ADD_SHOWN, IS_PRO ;
    }
}
