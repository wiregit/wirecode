/*
 * Mojito Distributed Hash Tabe (DHT)
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
 
package com.limegroup.mojito.settings;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.settings.AbstractSettings;
import com.limegroup.gnutella.settings.SettingsFactory;


/**
 * 
 */
public class MojitoProps extends AbstractSettings {
    
    private static final MojitoProps INSTANCE = new MojitoProps();
    
    // The FACTORY is used for subclasses of LimeProps, so they know
    // which factory to add classes to.
    protected static final SettingsFactory FACTORY = INSTANCE.getFactory();
    
    // This is protected so that subclasses can extend from it, but
    // subclasses should NEVER instantiate a copy themselves.
    protected MojitoProps() {
        super("mojito.props", "Mojito properties file");
        Assert.that( getClass() == MojitoProps.class,
            "should not have a subclass instantiate");
    }
    
    /**
     * Returns the only instance of this class.
     */
    public static MojitoProps instance() { return INSTANCE; }
}
