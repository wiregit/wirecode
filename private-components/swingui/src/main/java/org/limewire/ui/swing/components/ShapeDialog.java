package org.limewire.ui.swing.components;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Singleton;

@Singleton
public class ShapeDialog extends JXPanel implements Resizable {


    private AWTEventListener eventListener;

    private Component component;

    private ComponentListener componentListener;

    private Component owner;
    

    @Resource
    private boolean isPositionedRelativeToOwner;

    public ShapeDialog() {
        super(new BorderLayout());
        GuiUtils.assignResources(this);
        
        setOpaque(false);
        setVisible(false);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                addListeners();
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                removeListeners();
            }
        });
              
        componentListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                //TODO: better way of handling this
                if (component != null) {
                    setSize(component.getPreferredSize());
                }
            }
        };
    }

    public void show(Component c, Component owner) {
        removeAll();
        add(c);
        this.component = c;
        this.owner = owner;
        setVisible(true);
        resize();
    }
    
    private void positionRelativeToOwner(){
        Point ownerLocation = SwingUtilities.convertPoint(owner.getParent(), owner.getLocation(), getParent());            
        setBounds(ownerLocation.x + owner.getWidth() - component.getWidth(), ownerLocation.y + owner.getHeight(), component.getPreferredSize().width, component.getPreferredSize().height);
    }


    private void addListeners() {
        if (eventListener == null) {
            initializeEventListener();
        }

        Toolkit.getDefaultToolkit().addAWTEventListener(eventListener, AWTEvent.MOUSE_EVENT_MASK);
        Toolkit.getDefaultToolkit().addAWTEventListener(eventListener, AWTEvent.KEY_EVENT_MASK);   
        if (component != null) {
            component.addComponentListener(componentListener);
        }
    }

    private void removeListeners() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(eventListener);
        if (component != null) {
            component.removeComponentListener(componentListener);
        }
    }

    private void initializeEventListener() {
        // make sharePanel disappear when the user clicks elsewhere
        eventListener = new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (isVisible()) {

                    if ((event.getID() == MouseEvent.MOUSE_PRESSED)) {
                        MouseEvent e = (MouseEvent) event;
                        
                        if ((getMousePosition() == null || !contains(getMousePosition())) && component != e.getComponent()
                                && (!(component instanceof ShapeDialogComponent) || !((ShapeDialogComponent) component)
                                        .contains(e.getComponent()))) {
                            setVisible(false);
                        }

                    } else if (event.getID() == KeyEvent.KEY_TYPED) {
                        KeyEvent e = (KeyEvent) event;

                        if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
                            setVisible(false);
                        }
                    }

                }
            }
        };
    }

    @Override
    public void resize() {
        if (isVisible() && component != null) {
            if (!isPositionedRelativeToOwner || owner == null) {
                Rectangle parentBounds = getParent().getBounds();
                Dimension childPreferredSize = getPreferredSize();
                int w = childPreferredSize.width;
                int h = childPreferredSize.height;
                setBounds(parentBounds.width / 2 - w / 2, parentBounds.height / 2 - h / 2, w, h);
            } else {
                positionRelativeToOwner();
            }
        }
    }

}
