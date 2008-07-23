package org.limewire.ui.swing.util;

import org.jdesktop.application.Application;
import org.limewire.ui.swing.mainframe.AppFrame;


public class GuiUtils {

	/**
	 * Inject fields from AppFrame.properties into object. Fields to be injected
	 * should be annotated <code>@Resource</code> and defined in AppFrame.properties as
	 * <code><Object>.<field> = <data></code>
	 * 
	 * @param object the object whose fields will be injected
	 */
	public static void assignResources(Object object) {
		Application.getInstance().getContext().getResourceMap(AppFrame.class)
				.injectFields(object);
	}
	
    /**
     * Acts as a proxy for the Launcher class so that other classes only need
     * to know about this mediator class.
     *
     * <p>Opens the specified url in a browser.
     *
     * @param url the url to open
     * @return an int indicating the success of the browser launch
     */
    public static final int openURL(String url) {
        // TODO: Fix dependencies so this works!
        throw new RuntimeException("Implement me!");
//        try {
//            return Launcher.openURL(url);
//        } catch(IOException ioe) {
//            // TODO: Show an error
//            //GUIMediator.showError(I18n.tr("LimeWire could not locate your web browser to display the following webpage: {0}.", url));
//            return -1;
//        }
    }

}
