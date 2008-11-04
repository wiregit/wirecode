package org.limewire.core.impl.library;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.MetaDataManager;

import com.google.inject.Singleton;

@Singleton
public class MetaDataManagerImpl implements MetaDataManager {

    @Override
    public void save(LocalFileItem localFileItem) {
        // TODO save the file meta data updates

        // TODO read the properties from the file item and save them in the
        // appropriate lime xml field.
    }

}
