/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.limewire.mojito.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.mojito.Context;
import org.limewire.mojito.result.StoreResult;

/**
 * The StorableModelManager manges StorableModels
 */
public class StorableModelManager {
    
    private final Map<DHTValueType, StorableModel> models 
        = Collections.synchronizedMap(new HashMap<DHTValueType, StorableModel>());
    
    /**
     * Registers a StorableModel under the given DHTValueType.
     */
    public StorableModel addStorableModel(DHTValueType valueType, StorableModel model) {
        return models.put(valueType, model);
    }
    
    /**
     * Removes and returns a StorableModel that is registered under the
     * given DHTValueType.
     */
    public StorableModel removeStorableModel(DHTValueType valueType) {
        return models.remove(valueType);
    }
    
    /**
     * Returns all Storables
     */
    Collection<Storable> getStorables() {
        List<Storable> values = new ArrayList<Storable>();
        synchronized (models) {
            for (StorableModel src : models.values()) {
                values.addAll(src.getStorables());
            }
        }
        return values;
    }
    
    /**
     * Notifies a StorableModel about the result of a STORE
     * operation
     */
    void handleStoreResult(Storable value, StoreResult result) {
        DHTValueType type = value.getValue().getValueType();
        StorableModel model = models.get(type);
        if (model != null) {
            model.handleStoreResult(value, result);
        }
    }
    
    /**
     * Notifies all StorableModels that the local
     * Contact's contact information changed
     */
    public void handleContactChange(Context context) {
        synchronized (models) {
            for (StorableModel model : models.values()) {
                model.handleContactChange();
            }
        }
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        synchronized (models) {
            for (DHTValueType type : models.keySet()) {
                StorableModel model = models.get(type);
                buffer.append(type).append(":\n");
                buffer.append(model).append("\n\n");
            }
        }
        return buffer.toString();
    }
}
