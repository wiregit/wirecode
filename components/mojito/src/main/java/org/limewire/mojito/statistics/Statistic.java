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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.limewire.inspection.Inspectable;

public class Statistic<T extends Number & Comparable<T>> 
        extends History<T> implements Inspectable {
    
    public static final int HISTORY_SIZE = 200;

    public Statistic() {
        this(HISTORY_SIZE);
    }
    
    public Statistic(int historySize) {
        super(historySize);
    }
    
    public synchronized Double getAverage() {
        if (isEmpty()) {
            return null;
        }
        
        double sum = 0;
        for (T num : this) {
            sum += num.doubleValue();
        }
        
        return sum/size();
    }
    
    /**
     * Writes the Statistics to the given Writer
     */
    public synchronized void write(Writer out) throws IOException {
        StringBuilder buffer = new StringBuilder();
        buffer.append(getCurrent()).append("\t");
        buffer.append(getMin()).append("\t");
        buffer.append(getMax()).append("\t");
        buffer.append(getAverage()).append("\t");
        buffer.append(getTotalCount()).append("\t");
        
        // Remove the last tab
        if (buffer.length() > 0) {
            buffer.setLength(buffer.length()-1);
        }
        
        out.write(buffer.toString());
    }
    
    public String toString() {
        StringWriter out = new StringWriter();
        try {
            write(out);
        } catch (IOException err) {
            err.printStackTrace(new PrintWriter(out));
        }
        return out.toString();
    }

    public Object inspect() {
        return toString();
    }
}
