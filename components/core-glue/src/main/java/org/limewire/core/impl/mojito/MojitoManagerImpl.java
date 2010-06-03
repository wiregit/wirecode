package org.limewire.core.impl.mojito;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.limewire.core.api.mojito.MojitoManager;
import org.limewire.listener.SwingSafePropertyChangeSupport;
import org.limewire.mojito.MojitoDHT;

import com.google.inject.Inject;
import com.limegroup.gnutella.dht.Controller;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;

/**
 * Live implementation of MojitoManager.
 */
public class MojitoManagerImpl implements MojitoManager {

    /** DHT manager object. */
    private final DHTManager manager;
    
    /** Property change support object. */
    private final PropertyChangeSupport changeSupport 
        = new SwingSafePropertyChangeSupport(this);

    /**
     * Constructs the live implementation of MojitoManager using the specified
     * DHT manager.
     */
    @Inject
    public MojitoManagerImpl(DHTManager manager) {
        this.manager = manager;
    }
    
    @Override
    public String getName() {
        MojitoDHT dht = manager.getMojitoDHT();
        return (dht != null) ? dht.getName() : null;
    }
    
    @Override
    public boolean isBooting() {
        return manager.isBooting();
    }

    @Override
    public boolean isReady() {
        return manager.isReady();
    }
    
    @Override
    public boolean isRunning() {
        return manager.isRunning();
    }

    /**
     * Add listener to fire property change on DHT event.
     */
    @Inject
    public void registerListener() {
        manager.addEventListener(new DHTEventListener() {
            @Override
            public void handleDHTEvent(DHTEvent evt) {
                String name = evt.getType().name();
                changeSupport.firePropertyChange(name, false, true);
            }
        });
    }

    /**
     * Adds the specified listener to the list that is notified when a 
     * property value changes.  Listeners added from the Swing UI thread will 
     * always receive notification events on the Swing UI thread. 
     */
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }
    
    /**
     * Removes the specified listener from the list that is notified when a 
     * property value changes.
     */
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    
    /**
     * Invokes the specified command on the Mojito DHT, and forwards output
     * to the specified PrintWriter.
     * @return true if command was successfully invoked
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean handle(String command, PrintWriter out) {
        // Define result.
        boolean result = false;
        
        try {
            // Get DHT using manager.
            Controller controller = manager.getController();
            MojitoDHT dht = controller.getMojitoDHT();
            if (dht == null) {
                out.println("Mojito is not running");
                return result;
            }

            // Get command handler method.
            Class cmdHandler = Class.forName("org.limewire.mojito.CommandHandler");
            Method handle = cmdHandler.getMethod("handle", 
                    new Class[]{MojitoDHT.class, String.class, PrintWriter.class});

            // Invoke method to pass command to DHT.
            result = ((Boolean) handle.invoke(null, 
                    new Object[]{dht, command, out})).booleanValue();

        } catch (ClassNotFoundException e) {
            e.printStackTrace(out);
        } catch (NoSuchMethodException e) {
            e.printStackTrace(out);
        } catch (IllegalAccessException e) {
            e.printStackTrace(out);
        } catch (InvocationTargetException e) {
            e.printStackTrace(out);
        }
        
        // Return result.
        return result;
    }
}
