/* Copyright (c) 2007 Timothy Wall, All Rights Reserved
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.  
 */
package com.limegroup.gnutella.library.monitor.win32.api;

import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIFunctionMapper;
import com.sun.jna.win32.W32APITypeMapper;

/**
 * Base type for most W32 API libraries. Provides standard options for
 * unicode/ASCII mappings. Set the system property <code>w32.ascii</code> to
 * <code>true</code> to default to the ASCII mappings.
 */
public interface W32API extends StdCallLibrary, W32Errors {

    /** Standard options to use the unicode version of a w32 API. */
    public Map<String, Object> UNICODE_OPTIONS = new HashMap<String, Object>() {
        {
            put(OPTION_TYPE_MAPPER, W32APITypeMapper.UNICODE);
            put(OPTION_FUNCTION_MAPPER, W32APIFunctionMapper.UNICODE);
        }
    };

    /** Standard options to use the ASCII/MBCS version of a w32 API. */
    public Map<String, Object> ASCII_OPTIONS = new HashMap<String, Object>() {
        {
            put(OPTION_TYPE_MAPPER, W32APITypeMapper.ASCII);
            put(OPTION_FUNCTION_MAPPER, W32APIFunctionMapper.ASCII);
        }
    };

    public Map DEFAULT_OPTIONS = Boolean.getBoolean("w32.ascii") ? ASCII_OPTIONS : UNICODE_OPTIONS;

    /** Special HWND value. */
    public HWND HWND_BROADCAST = new HWND() {
        {
            super.setPointer(Pointer.createConstant(0xFFFF));
        }

        public void setPointer(Pointer p) {
            throw new UnsupportedOperationException("Immutable reference");
        }
    };
}
