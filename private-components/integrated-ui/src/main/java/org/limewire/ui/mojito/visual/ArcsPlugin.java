package org.limewire.ui.mojito.visual;

import javax.swing.JComponent;

import org.limewire.mojito.DHT;
import org.limewire.mojito.visual.ArcsVisualizer;
import org.limewire.ui.swing.plugin.SwingUiPlugin;

import com.google.inject.Inject;
import com.limegroup.gnutella.dht.Controller;
import com.limegroup.gnutella.dht.DHTManager;

class ArcsPlugin implements SwingUiPlugin {

    private final DHTManager dhtManager;

    private volatile ArcsVisualizer arcsVisualizer;

    @Inject
    public ArcsPlugin(DHTManager dhtManager) {
        this.dhtManager = dhtManager;
    }

    @Override
    public JComponent getPluginComponent() {
        if (arcsVisualizer != null) {
            arcsVisualizer.stopArcs();
            arcsVisualizer = null;
        }

        DHT dht = null;
        synchronized (dhtManager) {
            Controller controller 
                = dhtManager.getController();
            dht = controller.getMojitoDHT();
        }
        
        if (dht == null) {
            return null;
        }
        
        arcsVisualizer = new ArcsVisualizer(
                dht, dht.getLocalhost().getContactId());
        return arcsVisualizer;
    }

    @Override
    public void startPlugin() {
        if (arcsVisualizer != null) {
            arcsVisualizer.startArcs();
        }
    }

    @Override
    public void stopPlugin() {
        if (arcsVisualizer != null) {
            arcsVisualizer.stopArcs();
        }
    }

    @Override
    public String getPluginName() {
        return "Mojito Arcs Visualizer";
    }

}
