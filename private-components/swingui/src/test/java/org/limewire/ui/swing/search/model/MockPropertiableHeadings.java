package org.limewire.ui.swing.search.model;

import org.limewire.core.api.library.PropertiableFile;
import org.limewire.ui.swing.util.PropertiableHeadings;

public class MockPropertiableHeadings implements PropertiableHeadings {
    public String heading;
    public String subheading;
    public int getHeadingCalledCount;
    public int getSubHeadingCalledCount;
    
    @Override
    public String getHeading(PropertiableFile propertiable) {
        getHeadingCalledCount++;
        return heading;
    }

    @Override
    public String getSubHeading(PropertiableFile propertiable) {
        return getSubHeading(propertiable, false);
    }
    
    @Override
    public String getSubHeading(PropertiableFile propertiable, boolean album) {
        getSubHeadingCalledCount++;
        return subheading;
    }
    
    @Override
    public String getFileSize(PropertiableFile propertiable) {
        return null;
    }
}
