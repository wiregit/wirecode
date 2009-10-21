package org.limewire.ui.swing.player;

import java.awt.Component;

/** Defines a factory for creating the video player panel.
*/
interface VideoPanelFactory {
   
   /**
    * Creates a new VideoPanel using the specified videoRenderer.
    */
   public VideoPanel createVideoPanel(Component videoRenderer);
}
