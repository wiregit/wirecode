package org.limewire.ui.swing.components;

import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.io.File;
import java.net.URI;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.util.SystemUtils;


/**
 * A JFrame that uses LimeWire's icon.
 */
public class LimeJFrame extends JFrame {
    
    @Resource private Icon limeIcon;
    @Resource private String limeFrameIconLocation;
    
    public LimeJFrame() throws HeadlessException {
        super();
        initialize();
    }

    public LimeJFrame(GraphicsConfiguration gc) {
        super(gc);
        initialize();
    }

    public LimeJFrame(String title, GraphicsConfiguration gc) {
        super(title, gc);
        initialize();
    }

    public LimeJFrame(String title) throws HeadlessException {
        super(title);
        initialize();
    }

    private void initialize() {
        GuiUtils.assignResources(this);
        setIconImage(((ImageIcon)limeIcon).getImage());
    }

    // Overrides addNotify() to change to a platform specific icon right afterwards.
    @Override
	public void addNotify() {
		super.addNotify();

		// Replace the Swing icon with a prettier platform-specific one
		SystemUtils.setWindowIcon(this, new File(URI.create(ClassLoader.getSystemResource(limeFrameIconLocation).getFile()).getPath()).getAbsoluteFile());
	}
}
