/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006 LimeWire LLC
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

package com.limegroup.mojito;

import com.limegroup.mojito.settings.ContextSettings;

/**
 * A Factory class to create or load MojitoDHTs
 */
public class MojitoFactory {
    
    private static final String DEFAULT_NAME = "DHT";
    
    private MojitoFactory() {}
    
    /**
     * Creates a MojitoDHT with default settings
     */
    public static MojitoDHT createDHT() {
        return createDHT(DEFAULT_NAME);
    }
    
    /**
     * Creates a MojitoDHT with the given name
     */
    public static MojitoDHT createDHT(String name) {
        return create(name, false);
    }
    
    /**
     * Creates a MojitoDHT with the given name, vendor code and version
     */
    public static MojitoDHT createDHT(String name, int vendor, int version) {
        return create(name, vendor, version, false);
    }
    
    /**
     * Creates a firewalled MojitoDHT
     */
    public static MojitoDHT createFirewalledDHT() {
        return createFirewalledDHT(DEFAULT_NAME);
    }
    
    /**
     * Creates a firewalled MojitoDHT with the given name
     */
    public static MojitoDHT createFirewalledDHT(String name) {
        return create(name, true);
    }
    
    /**
     * Creates a firewalled MojitoDHT with the given name, vendor code and version
     */
    public static MojitoDHT createFirewalledDHT(String name, int vendor, int version) {
        return create(name, vendor, version, true);
    }
    
    /**
     * Creates a MojitoDHT with the given arguments
     */
    private static Context create(String name, boolean firewalled) {
        return create(name, 
                ContextSettings.VENDOR.getValue(), 
                ContextSettings.VERSION.getValue(),
                firewalled);
    }
    
    /**
     * Creates a MojitoDHT with the given arguments
     */
    private static Context create(String name, int vendor, int version, boolean firewalled) {
        
        if (name == null) {
            name = DEFAULT_NAME;
        }
        
        return new Context(name, vendor, version, firewalled);
    }
}
