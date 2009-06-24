package org.limewire.core.impl.friend;

import org.limewire.core.api.xmpp.FileMetaDataConverter;
import org.limewire.core.impl.xmpp.FileMetaDataConverterImpl;
import org.limewire.friend.api.LimeWireFriendModule;
import org.limewire.friend.impl.LimeWireFriendImplModule;
import org.limewire.friend.impl.feature.LimeWireFriendFeatureModule;

import com.google.inject.AbstractModule;

public class CoreGlueFriendModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new LimeWireFriendModule());
        install(new LimeWireFriendImplModule());
        install(new LimeWireFriendFeatureModule());
        bind(FriendFirewalledAddressConnector.class).asEagerSingleton();
        bind(FriendRemoteFileDescCreator.class).asEagerSingleton();
        bind(CoreGlueFriendService.class);
                // TODO fberger move classes into friend module
        bind(FileMetaDataConverter.class).to(FileMetaDataConverterImpl.class);
        bind(FriendShareListRefresher.class);
    }

}
