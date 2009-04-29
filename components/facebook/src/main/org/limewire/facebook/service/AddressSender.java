package org.limewire.facebook.service;

import org.limewire.listener.EventListener;
import org.limewire.net.address.AddressEvent;

import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.FacebookJsonRestClient;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

@Singleton
public class AddressSender implements EventListener<AddressEvent> {
    private final Provider<String> apiKey;
    private final String secret;

    @AssistedInject
    AddressSender(@Named("facebookApiKey") Provider<String> apiKey,
                  @Assisted String secret) {
        this.apiKey = apiKey;
        this.secret = secret;
    }
    
    @Override
    public void handleEvent(AddressEvent event) {
        if(event.getType() == AddressEvent.Type.ADDRESS_CHANGED) {
            FacebookJsonRestClient client = new FacebookJsonRestClient(apiKey.get(), secret);
            try {
                client.liveMessage_send(new Long(0), "", null);
            } catch (FacebookException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
