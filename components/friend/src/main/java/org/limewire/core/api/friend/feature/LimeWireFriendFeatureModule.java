package org.limewire.core.api.friend.feature;

import org.limewire.core.api.friend.feature.features.AddressDispatcher;
import org.limewire.core.api.friend.feature.features.AuthToken;
import org.limewire.core.api.friend.feature.features.AuthTokenDispatcher;
import org.limewire.core.api.friend.feature.features.ConnectBackRequestFeatureTransportHandler;
import org.limewire.core.api.friend.feature.features.FileOfferFeatureTransportHandler;
import org.limewire.core.api.friend.feature.features.LibraryChangedDispatcher;
import org.limewire.core.api.friend.feature.features.LibraryChangedNotifier;
import org.limewire.core.api.friend.client.FileMetaData;
import org.limewire.io.Address;
import org.limewire.net.ConnectBackRequest;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class LimeWireFriendFeatureModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(FeatureRegistry.class).to(FeatureRegistryImpl.class);

        bind(new TypeLiteral<FeatureTransport.Handler<Address>>(){}).to(AddressDispatcher.class);
        bind(new TypeLiteral<FeatureTransport.Handler<AuthToken>>(){}).to(AuthTokenDispatcher.class);
        bind(new TypeLiteral<FeatureTransport.Handler<LibraryChangedNotifier>>(){}).to(LibraryChangedDispatcher.class);
        bind(new TypeLiteral<FeatureTransport.Handler<ConnectBackRequest>>(){}).to(ConnectBackRequestFeatureTransportHandler.class);
        bind(new TypeLiteral<FeatureTransport.Handler<FileMetaData>>(){}).to(FileOfferFeatureTransportHandler.class);
    }
}
