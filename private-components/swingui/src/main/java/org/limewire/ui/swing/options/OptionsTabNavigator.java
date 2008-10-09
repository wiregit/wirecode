package org.limewire.ui.swing.options;


public interface OptionsTabNavigator {

    OptionTabItem addOptionTab(String title, OptionsTabNavigator navigator);
    
    void select(String title);
}
