package org.limewire.ui.swing.player;

import java.awt.Component;

/** Defines a factory for creating the video player panel.
*/
interface VideoPanelFactory {
   
   /**
    * Creates a new SearchResultsPanel using the specified search results data
    * model.
    */
   public VideoPanel createVideoPanel(Component videoRenderer);
}
