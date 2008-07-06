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

    }

    public void removeNavigablePanel(NavItem target, String key) {

    }

    public void showNavigablePanel(String key) {
        navTarget.showNavigablePanel(key);
    }
}
