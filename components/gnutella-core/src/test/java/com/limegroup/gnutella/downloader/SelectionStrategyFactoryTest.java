package com.limegroup.gnutella.downloader;

import java.util.Locale;

import org.limewire.swarm.file.BiasedRandomDownloadStrategy;
import org.limewire.swarm.file.SelectionStrategy;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.settings.DownloadSettings;

public class SelectionStrategyFactoryTest extends BaseTestCase {

    public SelectionStrategyFactoryTest(String name) {
        super(name);
    }


    public void testGetStrategyForWithAllLocales() {
        for (Locale locale : Locale.getAvailableLocales()) {
            Locale.setDefault(locale);
            for (String extension : DownloadSettings.PREVIEWABLE_EXTENSIONS.getValue()) {
                String upperCaseExtension = extension.toUpperCase(Locale.US);
                SelectionStrategy strategy = SelectionStrategyFactory.getStrategyFor(upperCaseExtension, 500);
                assertTrue("Failed for locale: " + locale, strategy instanceof BiasedRandomDownloadStrategy);
            }
        }
    }

}
