package com.limegroup.gnutella.dht.statistics;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import de.kapsi.net.kademlia.Context;

public abstract class StatisticContainer{

    public StatisticContainer(Context context) {
        context.getDHTStats().addStatisticContainer(this);
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
