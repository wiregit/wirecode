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
	public static void injectFields(Object object) {
		Application.getInstance().getContext().getResourceMap(AppFrame.class)
				.injectFields(object);
	}
}
