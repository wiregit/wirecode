package org.limewire.ui.swing.sharing;

import javax.swing.JComponent;

import org.limewire.ui.swing.nav.NavItem;

public interface SharingNavigator {

    NavItem addSharingItem(String title, JComponent sharingPanel);
}
