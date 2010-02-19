package org.limewire.core.impl.rest;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.i18n.I18nMarker;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.rest.RestPrefix;
import org.limewire.rest.RestRequestHandlerFactory;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.browser.LocalHTTPAcceptor;

/**
 * REST API service for the live core.
 */
@EagerSingleton
public class CoreGlueRestService implements Service {

    private static final String REMOTE_PREFIX = "/remote/";
    
    private final Provider<LocalHTTPAcceptor> localHttpAcceptorFactory;
    private final RestRequestHandlerFactory restRequestHandlerFactory;
    
    private SettingListener localSettingListener;
    
    @Inject
    public CoreGlueRestService(Provider<LocalHTTPAcceptor> localHttpAcceptorFactory,
            RestRequestHandlerFactory restRequestHandlerFactory) {
        this.localHttpAcceptorFactory = localHttpAcceptorFactory;
        this.restRequestHandlerFactory = restRequestHandlerFactory;
    }
    
    @Inject
    void register(ServiceRegistry registry) {
        registry.register(this);
    }
    
    @Override
    public String getServiceName() {
        return I18nMarker.marktr("REST Service");
    }

    @Override
    public void initialize() {
    }

    @Override
    public void start() {
        // Install setting listener.
        if (localSettingListener == null) {
            localSettingListener = new SettingListener() {
                @Override
                public void settingChanged(SettingEvent evt) {
                    if (((BooleanSetting) evt.getSetting()).getValue()) {
                        registerLocalHandlers();
                    } else {
                        unregisterLocalHandlers();
                    }
                }
            };
            ApplicationSettings.LOCAL_REST_ACCESS_ENABLED.addSettingListener(localSettingListener);
        }
        
        // Register request handlers if enabled.
        if (ApplicationSettings.LOCAL_REST_ACCESS_ENABLED.getValue()) {
            registerLocalHandlers();
        }
    }

    @Override
    public void stop() {
        // Uninstall setting listener.
        if (localSettingListener != null) {
            ApplicationSettings.LOCAL_REST_ACCESS_ENABLED.removeSettingListener(localSettingListener);
            localSettingListener = null;
        }
        
        unregisterLocalHandlers();
    }

    /**
     * Registers local handlers for all REST targets.
     */
    private void registerLocalHandlers() {
        for (RestPrefix restPrefix : RestPrefix.values()) {
            localHttpAcceptorFactory.get().registerHandler(createPattern(restPrefix.pattern()),
                    restRequestHandlerFactory.createRequestHandler(restPrefix));
        }
    }
    
    /**
     * Unregisters local handlers for all REST targets.
     */
    private void unregisterLocalHandlers() {
        for (RestPrefix restPrefix : RestPrefix.values()) {
            localHttpAcceptorFactory.get().unregisterHandler(createPattern(restPrefix.pattern()));
        }
    }
    
    /**
     * Creates the remote access URI pattern for the specified target.
     */
    private String createPattern(String target) {
        return REMOTE_PREFIX + target + "*";
    }
}
