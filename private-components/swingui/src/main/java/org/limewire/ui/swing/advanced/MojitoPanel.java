package org.limewire.ui.swing.advanced;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.limewire.mojito.MojitoDHT;
import org.limewire.ui.swing.plugin.SwingUiPlugin;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;

/**
 * The Mojito tab panel for the Advanced Tools window.  This displays the
 * Arcs view for the Mojito DHT.
 */
class MojitoPanel extends TabPanel {
    
    private final AtomicBoolean registered = new AtomicBoolean(false);
    
    private final DHTEventListener listener 
            = new DHTEventListener() {
        @Override
        public void handleDHTEvent(DHTEvent evt) {
            MojitoPanel.this.handleDHTEvent(evt);
        }
    };
    
    private final JLabel label = new JLabel();
    
    /** The {@link DHTManager} */
    private final DHTManager manager;
    
    /** Plugin for Mojito Arcs view component. */
    private volatile SwingUiPlugin plugin;
    
    /** Indicator that determines if DHT has started. */
    private volatile boolean started;
    
    private volatile JComponent renderer;

    /**
     * Constructs a MojitoPanel using the specified MojitoManager.
     */
    @Inject
    public MojitoPanel(DHTManager manager) {
        
        this.manager = manager;
        
        setBorder(BorderFactory.createEmptyBorder(3, 12, 12, 12));
        setLayout(new BorderLayout());

        // Install listener to request focus when tab panel is shown.  This
        // allows the Arcs view to begin handling mouse clicks immediately.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if (renderer != null) {
                    renderer.requestFocusInWindow();
                }
            }
        });
        
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
        label.setHorizontalAlignment(JLabel.CENTER);
    }
    
    @Inject(optional=true) void register(
            @Named("MojitoArcsPlugin") SwingUiPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns true if the tab content is enabled.
     */
    @Override
    public boolean isTabEnabled() {
        // Tab enabled if plugin is installed and DHT has started.
        return ((plugin != null) && started);
    }

    /**
     * Performs startup tasks for the tab.  This method is called when the 
     * parent window is opened. 
     */
    @Override
    public void initData() {
        // Render tab content.
        if (plugin != null) {
            renderPlugin();
        } else {
            renderNotAvailable();
        }
        
        DHTEvent.Type type = DHTEvent.Type.STOPPED;
        if (manager.isRunning()) {
            type = DHTEvent.Type.STARTING;
            
            if (manager.isBooting()) {
                type = DHTEvent.Type.CONNECTING;
            } else if (manager.isReady()) {
                type = DHTEvent.Type.CONNECTED;
            }
            
            started = true;
        }
        
        label.setText(createName(type));
        fireEnabledChanged(isTabEnabled());
        
        if (!registered.getAndSet(true)) {
            manager.addEventListener(listener);
        }
    }
    
    private void handleDHTEvent(DHTEvent evt) {
        DHTEvent.Type type = evt.getType();
        
        boolean wasStarted = started;
        started = (type != DHTEvent.Type.STOPPED);
        label.setText(createName(type));
        
        // Render plugin if available and DHT just started. 
        if (started && !wasStarted && (plugin != null)) {
            renderPlugin();
        }
        
        // Notify listeners about enabled state.
        if (wasStarted != started) {
            fireEnabledChanged(isTabEnabled());
        }
    }
    
    /**
     * Displays the Mojito plugin component in the tab.  
     */
    private void renderPlugin() {
        renderer = plugin.getPluginComponent();
        if (renderer != null) {
            removeAll();
            add(label, BorderLayout.NORTH);
            add(renderer, BorderLayout.CENTER);
        } else {
            renderNotAvailable();
        }
        plugin.startPlugin();
    }
    
    /**
     * Displays a "not available" message in the tab.
     */
    private void renderNotAvailable() {
        removeAll();
        renderer = null;
        JLabel naLabel = new JLabel();
        naLabel.setText(I18n.tr("Mojito Arcs View not available"));
        naLabel.setHorizontalAlignment(JLabel.CENTER);
        naLabel.setVerticalAlignment(JLabel.CENTER);
        add(naLabel, BorderLayout.CENTER);
    }

    /**
     * Performs clean up tasks for the tab.  This method is called when the
     * parent window is closed.
     */
    @Override
    public void dispose() {
        // Stop Mojito plugin.
        if (plugin != null) {
            plugin.stopPlugin();
        }
        
        if (registered.getAndSet(false)) {
            manager.removeEventListener(listener);
        }
    }
    
    private String createName(DHTEvent.Type type) {
        MojitoDHT dht = manager.getMojitoDHT();
        String name = dht != null ? dht.getName() : null;
        
        if (name == null) {
            return "(disabled)";
        }
        
        switch (type) {
            case STARTING:
                return name;
            case CONNECTING:
                return name + " (booting)";
            case CONNECTED:
                return name + " (ready)";
            default:
                return name + " (stopped)";
        }
    }
}
