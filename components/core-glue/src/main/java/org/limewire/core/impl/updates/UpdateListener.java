package org.limewire.core.impl.updates;

import org.limewire.core.api.updates.AutoUpdateHelper;
import org.limewire.core.api.updates.UpdateInformation;
import org.limewire.i18n.I18nMarker;
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
            String command = autoUpdateHelper.getAutoUpdateCommand();
            String button1Text = I18nMarker.marktr("Update Now");
            String button2Text = I18nMarker.marktr("Later");
            String title = I18nMarker.marktr("<b>A new version of LimeWire is now available!</b>");
            String text = I18nMarker.marktr("With this latest release, we no longer support the version of LimeWire on your computer, and your software will not run properly without a required software update.<br/><br/>Please perform the required update to continue using LimeWire - it's <b>quick, easy, and FREE</b>.");
            String url = "http://www.limewire.com";
            
            UpdateInformation autoUpdateInfo = new UpdateInformationImpl(button1Text, button2Text, command, text, title, url);
            uiListeners.broadcast(new org.limewire.core.api.updates.UpdateEvent(autoUpdateInfo, org.limewire.core.api.updates.UpdateEvent.Type.AUTO_UPDATE));
            
        }
    }
}
