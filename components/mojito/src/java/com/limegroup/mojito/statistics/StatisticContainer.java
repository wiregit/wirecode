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
 
package com.limegroup.mojito.statistics;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.limegroup.mojito.MojitoDHT;


public abstract class StatisticContainer{

    public StatisticContainer(MojitoDHT dht) {
        dht.getDHTStats().addStatisticContainer(this);
    }
    
    protected StatisticContainer() {}
    
    public void writeStats(Writer writer) throws IOException {
        String delimiter = DHTNodeStat.FILE_DELIMITER;
        Class superclass = getClass().getSuperclass();
        Class declaringClass = getClass().getDeclaringClass();
        List fieldsList = new LinkedList();
        if(superclass != null) {
            fieldsList.addAll(Arrays.asList(superclass.getFields()));
        }
        if(declaringClass != null) {
            fieldsList.addAll(Arrays.asList(declaringClass.getFields()));
        }
        fieldsList.addAll(Arrays.asList(getClass().getFields()));
        Field[] fields = (Field[])fieldsList.toArray(new Field[0]);
        for(int i=0; i<fields.length; i++) {
            try {
                if(Modifier.isTransient(fields[i].getModifiers())){
                    continue;
                }
                Object fieldObject = fields[i].get(this);
                if(fieldObject instanceof Statistic) {
                    Statistic stat = (Statistic) fieldObject;
                    writer.write(fields[i].getName()+delimiter);
                    stat.storeStats(writer);
                    writer.write("\n");
                }
            } catch(IllegalAccessException e) {
                continue;
            }
        }
    }
    
}
