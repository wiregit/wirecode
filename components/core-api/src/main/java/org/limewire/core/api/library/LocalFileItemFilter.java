package org.limewire.core.api.library;

public interface LocalFileItemFilter {
    /**
     * Returns true if the given LocalFileItem matches the filter.
     */
    public boolean accept(LocalFileItem localFileItem);
}
