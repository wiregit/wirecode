package org.limewire.ui.swing.library;

import org.limewire.core.api.Category;
import org.limewire.util.Objects;

/**
 * Catalog defines an organization of items in a library.  There are two 
 * catalog types: a media category, and a playlist.
 */
class Catalog {
    /** Types of catalogs. */
    public enum Type {
        CATEGORY, PLAYLIST
    };

    /** Catalog type. */
    private final Type type;
    /** Name of catalog. */
    private final String name;
    
    private Category category;

    /**
     * Constructs a Catalog with the specified type and name.
     */
    public Catalog(Type type, String name) {
        this.type = type;
        this.name = name;
    }
    
    /**
     * Constructs a Catalog for the specified media category.
     */
    public Catalog(Category category) {
        this(Type.CATEGORY, category.name());
        this.category = category;
    }
    
    /**
     * Returns the media category associated with this catalog.  May be null.
     */
    public Category getCategory() {
        return category;
    }
    
    /**
     * Returns a unique identifier for this catalog by combining the type and
     * name values.
     */
    public String getId() {
        return (type + "." + name);
    }
    
    public String getName() {
        return name;
    }
    
    public Type getType() {
        return type;
    }
    
    public boolean equals(Object obj) {
        if (obj instanceof Catalog) {
            Catalog item = (Catalog) obj;
            return (Objects.equalOrNull(type, item.type) &&
                    Objects.equalOrNull(name, item.name));
        }
        return false;
    }
    
    public int hashCode() {
        int result = 17;
        result = 31 * result + ((type == null) ? 0 : type.hashCode());
        result = 31 * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }
}
