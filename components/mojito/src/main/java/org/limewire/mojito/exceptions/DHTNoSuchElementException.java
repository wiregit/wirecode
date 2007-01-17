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

package org.limewire.mojito.exceptions;

import org.limewire.mojito.messages.ResponseMessage;

/**
 * The DHTNoSuchElementException is thrown if an attempt was made to get
 * a DHTValue from a remote Node and the Node did not have the requested
 * value.
 */
@SuppressWarnings("serial")
public class DHTNoSuchElementException extends DHTResponseException {

    public DHTNoSuchElementException(ResponseMessage response, String message) {
        super(response, message);
    }
}
