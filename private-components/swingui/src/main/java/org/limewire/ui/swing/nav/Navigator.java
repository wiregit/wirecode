/**
 * 
 */
package org.limewire.ui.swing.nav;

import javax.swing.JPanel;

public class Navigator {

    public static enum NavItem {
        LIBRARY, LIMEWIRE
    };

    private final NavigableTarget navTarget;
    
    public Navigator(NavigableTarget navTarget) {
        this.navTarget = navTarget;
    } 

    public void addNavigablePanel(NavItem target, String key, JPanel panel) {
        navTarget.addNavigablePanel(target + key, panel);
    }

    public void removeNavigablePanel(NavItem target, String key) {
        navTarget.removeNavigablePanel(target + key);
    }

    public void showNavigablePanel(NavItem target, String key) {
        navTarget.showNavigablePanel(target + key);
    }
}
