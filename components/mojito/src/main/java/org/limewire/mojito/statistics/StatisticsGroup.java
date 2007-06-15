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

package org.limewire.mojito.statistics;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class StatisticsGroup {
    
    private final Log log = LogFactory.getLog(getClass());
    
    public void write(Writer out) throws IOException {
        List<Field> fields = new ArrayList<Field>();
        
        Class<?> clazz = getClass();
        while(clazz != null) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        
        Class<?> declaringClazz = getClass().getDeclaringClass();
        if (declaringClazz != null) {
            fields.addAll(Arrays.asList(declaringClazz.getDeclaredFields()));
        }
        
        String name = getClass().getSimpleName();
        
        for (Field field : fields) {
            try {
                if (Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                
                field.setAccessible(true);
                Object value = field.get(this);
                if (!(value instanceof Statistic)) {
                    continue;
                }
                
                Statistic statistic = (Statistic) value;
                out.write(name + "." + field.getName() + "\t");
                statistic.write(out);
                out.write("\n");
                
            } catch (IllegalAccessException err) {
                log.error("IllegalAccessException", err);
                continue;
            }
        }
    }
    
    public String toString() {
        StringWriter out = new StringWriter();
        try {
            write(out);
        } catch (IOException err) {
            // Not possible!
            throw new RuntimeException(err);
        }
        return out.toString();
    }
}
