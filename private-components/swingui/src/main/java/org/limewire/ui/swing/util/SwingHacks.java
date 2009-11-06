package org.limewire.ui.swing.util;

import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.Timer;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.limewire.util.OSUtils;

import com.google.inject.internal.cglib.proxy.Enhancer;
import com.google.inject.internal.cglib.proxy.MethodInterceptor;
import com.google.inject.internal.cglib.proxy.MethodProxy;

/**
 * Hacks for some specific Swing vs OS issues.
 */
public class SwingHacks {
    
    private static PopupMenuListener menuHackListener = null; 
    
    /**
     *  LWC-3970 : Popup menus grey at times (Windows)
     *  
     *  <p>Toggles popup visibility a few mills after shown.  
     *   This is the only thing I have found to fix the problem since
     *   everything seems to function properly within java.  The paints are
     *   called properly but sometimes the window just does not render on screen.
     */
    public static void fixPopupMenuForWindows(JPopupMenu menu) {
        
        if (!OSUtils.isWindows()) {
            return;
        }
        
        if (menuHackListener == null) {
            menuHackListener = new PopupMenuListener() {
                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                }
                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    final JPopupMenu menu = (JPopupMenu)e.getSource();
                    Timer flashTimer = new Timer(20, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if (!menu.isVisible()) {
                                return;
                            }
                           
                            menu.removePopupMenuListener(menuHackListener);
                            menu.setVisible(false);
                            menu.setVisible(true);
                            menu.addPopupMenuListener(menuHackListener);
                        }
                    });
                    flashTimer.setRepeats(false);
                    flashTimer.start();
                    
                }
            };
        }
        
        menu.addPopupMenuListener(menuHackListener);
    }
    
    
    /**
     * LWC-3706 : DnD on KDE broken (KDE wm)
     * 
     * <p> Hack to fix one of the many Swing DnD + KDE bugs.  Prevents
     *  drag and drop from being disabled after a cancelled drop by
     *  resetting the internal state after the initial null pointer. 
     */
    public static void fixDnDforKDE(final JComponent c) {
        
        // TODO: should be KDE only, but is safe for Gnome in the meantime.
        if (!OSUtils.isUnix()) {
            return;
        }
        
        final DropTarget originalTarget = c.getDropTarget();
        
        if (originalTarget == null || 
                !originalTarget.getClass().getName()
                    .equals("javax.swing.TransferHandler$SwingDropTarget")) {
            return;
        }
        
        // Create a proxy to the original SwingDropTarget that will catch NPE's,
        //  from dragOver() and reset the DropTarget.
        Enhancer e = new Enhancer();
        e.setSuperclass(DropTarget.class);

        e.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object object, Method method, Object[] args, MethodProxy proxy)
                    throws Throwable {

                try {
                    Method originalMethod = originalTarget.getClass().getMethod(method.getName(), method.getParameterTypes());
                    originalMethod.setAccessible(true);
                    return originalMethod.invoke(originalTarget, args);
                }
                catch (NoSuchMethodException e) {
                    return null;
                } catch (InvocationTargetException e) {
                    if (method.getName().equals("dragOver")) {
                        DropTargetDragEvent event = (DropTargetDragEvent)args[0];
                        originalTarget.dragExit(event);
                        originalTarget.dragEnter(event);
                        originalTarget.dragOver(event);
                        return null;
                    }
                    throw e;
                }
                
            }
            
        });
        
        c.setDropTarget((DropTarget)e.create());
    }

}
