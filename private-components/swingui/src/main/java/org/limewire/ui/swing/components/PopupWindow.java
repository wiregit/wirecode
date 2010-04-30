package org.limewire.ui.swing.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;

/**
 * An extension of JWindow that can be used as a popup window.
 */
public class PopupWindow extends JWindow {
    private static final String CLOSE_ACTION_KEY = "closeWindow";

    private final OwnerListener ownerListener = new OwnerListener();
    
    /**
     * Creates a window with no specified owner.
     */
    public PopupWindow() {
        super();
        initialize();
    }

    /**
     * Creates a window with the specified owner frame.
     */
    public PopupWindow(Frame owner) {
        super(owner);
        initialize();
    }

    /**
     * Creates a window with the specified GraphicsConfiguration of a screen
     * device.
     */
    public PopupWindow(GraphicsConfiguration gc) {
        super(gc);
        initialize();
    }

    /**
     * Creates a window with the specified owner window.
     */
    public PopupWindow(Window owner) {
        super(owner);
        initialize();
    }

    /**
     * Creates a window with the specified owner window and GraphicsConfiguration
     * of a screen device.
     */
    public PopupWindow(Window owner, GraphicsConfiguration gc) {
        super(owner, gc);
        initialize();
    }
    
    /**
     * Initializes the window by installing listeners.
     */
    private void initialize() {
        // Add window listener to install actions when opened, and clean up
        // listeners when closed.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                uninstallInputActions();
                Window owner = getOwner();
                if (owner instanceof RootPaneContainer) {
                    Component glassPane = ((RootPaneContainer) owner).getGlassPane();
                    owner.removeComponentListener(ownerListener);
                    glassPane.removeMouseListener(ownerListener);
                    glassPane.setVisible(false);
                }
            }

            @Override
            public void windowOpened(WindowEvent e) {
                installInputActions();
                Window owner = getOwner();
                if (owner instanceof RootPaneContainer) {
                    Component glassPane = ((RootPaneContainer) owner).getGlassPane();
                    owner.addComponentListener(ownerListener);
                    glassPane.addMouseListener(ownerListener);
                    glassPane.setVisible(true);
                }
            }
        });
    }
    
    /**
     * Installs input actions in content pane.
     */
    private void installInputActions() {
        Container container = getContentPane();
        if (container instanceof JComponent) {
            JComponent contentPane = (JComponent) container;
            // Create Escape key binding to close window.
            contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CLOSE_ACTION_KEY);
            contentPane.getActionMap().put(CLOSE_ACTION_KEY, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });
        }
    }
    
    /**
     * Uninstalls input actions in content pane.
     */
    private void uninstallInputActions() {
        Container container = getContentPane();
        if (container instanceof JComponent) {
            JComponent contentPane = (JComponent) container;
            contentPane.getActionMap().remove(CLOSE_ACTION_KEY);
        }
    }
    
    /**
     * Creates a popup window with the specified parent component, content
     * pane and screen location.
     */
    public static PopupWindow createPopupWindow(JComponent parent, 
            JComponent contentPane, Point location) {
        // Declare new popup window.
        PopupWindow popupWindow;

        // Create popup window for parent container.
        Container ancestor = parent.getTopLevelAncestor();
        if (ancestor instanceof Frame) {
            popupWindow = new PopupWindow((Frame) ancestor);
        } else if (ancestor instanceof Window) {
            popupWindow = new PopupWindow((Window) ancestor);
        } else {
            popupWindow = new PopupWindow();
        }

        // Calculate window size and location.
        popupWindow.setContentPane(contentPane);
        popupWindow.pack();
        popupWindow.setLocation(location);

        // Return the window.
        return popupWindow;
    }
    
    /**
     * Listener to handle events on the popup owner.  The popup is closed
     * when the owner is moved, or when the mouse is pressed on the owner's
     * glass pane.
     */
    private class OwnerListener extends MouseAdapter implements ComponentListener {

        @Override
        public void componentHidden(ComponentEvent e) {
            dispose();
        }

        @Override
        public void componentMoved(ComponentEvent e) {
            dispose();
        }

        @Override
        public void componentResized(ComponentEvent e) {
            dispose();
        }

        @Override
        public void componentShown(ComponentEvent e) {
        }
        
        @Override
        public void mousePressed(MouseEvent e) {
            dispose();
        }
    }
}
