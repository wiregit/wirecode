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
 
package org.limewire.mojito.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.FloatSetting;

/**
 * Setting for Kademlia lookups
 */
public class LookupSettings extends MojitoProps {
    
    private LookupSettings() {}
    
    /**
     * Bootstrapping Node return an empty Collection of Contacts
     * for our FIND_NODE requests. This Setting controls whether or 
     * not such Nodes should be added to the lookup response path.
     */
    public static final BooleanSetting ACCEPT_EMPTY_FIND_NODE_RESPONSES
        = FACTORY.createRemoteBooleanSetting("ACCEPT_EMPTY_FIND_NODE_RESPONSES", 
                true, "Mojito.AcceptEmptyFindNodeResponses");
    
    /**
     * Setting for what percentage of all Contacts in a FIND_NODE
     * response must be valid before the entire response is considered
     * valid.
     */
    public static final FloatSetting CONTACTS_SCRUBBER_REQUIRED_RATIO
        = FACTORY.createRemoteFloatSetting("CONTACTS_SCRUBBER_REQUIRED_RATIO", 
                0.0f, "Mojito.ContactsScrubberRequiredRatio", 0.0f, 1.0f);
}
