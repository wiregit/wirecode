package org.limewire.core.impl.updates;

import org.limewire.core.api.updates.AutoUpdateHelper;
import org.limewire.core.api.updates.UpdateInformation;
import org.limewire.inject.EagerSingleton;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;

import com.google.inject.Inject;
import com.limegroup.gnutella.simpp.SimppListener;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.version.UpdateEvent;
import com.limegroup.gnutella.version.UpdateHandler;

@EagerSingleton
public class UpdateListener implements EventListener<UpdateEvent>, SimppListener {

    private final EventBroadcaster<org.limewire.core.api.updates.UpdateEvent> uiListeners;
    
    private final AutoUpdateHelper autoUpdateHelper;
    
    @Inject
    public UpdateListener(UpdateHandler updateHandler, AutoUpdateHelper autoUpdateHandler,
            EventBroadcaster<org.limewire.core.api.updates.UpdateEvent> listeners) {
        this.uiListeners = listeners;
        this.autoUpdateHelper = autoUpdateHandler;
        updateHandler.addListener(this);
    }
    
    @Inject
    void register(SimppManager simppManager) {
        simppManager.addListener(this);
    }
    
    @Override
    public void handleEvent(UpdateEvent event) {
        com.limegroup.gnutella.version.UpdateInformation eventData = event.getData();
        UpdateInformation info = new UpdateInformationImpl(eventData.getButton1Text(),
                eventData.getButton2Text(), eventData.getUpdateCommand(),
                eventData.getUpdateText(), eventData.getUpdateTitle(), eventData.getUpdateURL());

        uiListeners.broadcast(new org.limewire.core.api.updates.UpdateEvent(info, org.limewire.core.api.updates.UpdateEvent.Type.UPDATE));
    }

    @Override
    public void simppUpdated() {
        if( autoUpdateHelper.isUpdateAvailable() ){
            UpdateInformation autoUpdateInfo = new UpdateInformationImpl(null, null, null, null, null, null);
            uiListeners.broadcast(new org.limewire.core.api.updates.UpdateEvent(autoUpdateInfo, org.limewire.core.api.updates.UpdateEvent.Type.AUTO_UPDATE));
        }
    }
}
