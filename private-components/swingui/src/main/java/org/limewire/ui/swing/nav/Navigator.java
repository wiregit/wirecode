/**
 * 
 */
package org.limewire.ui.swing.nav;

import javax.swing.JPanel;

import org.limewire.ui.swing.mainframe.MainPanel;

/**
 * 
 */
public class Navigator {

    public static enum TREE {
        LIBRARY, LIMEWIRE
    };

    private static final Navigator INSTANCE = new Navigator();
    
    private MainPanel mainPanel;

    private Navigator() {

    }
    
    public static Navigator getInstance(){
        return INSTANCE;
    }

    public void setMainPanel(MainPanel mainPanel){
        this.mainPanel = mainPanel;
    }
    public void addMainPanel(TREE target, String key, JPanel panel) {

    }

    public void removeMainPanel(TREE target, String key) {

    }

    public void showMainPanel(String key) {
         mainPanel.showPanel(key);
    }
}
