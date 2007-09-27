package com.limegroup.gnutella.util;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.limewire.inspection.Inspectable;
import org.limewire.util.FileUtils;

public class InspectionTool {

    public static void main(String[] args) throws Exception {
        Map<String, String> props = new TreeMap<String,String>();
        File [] classFiles = FileUtils.getFilesRecursive(new File(args[0]), new String[]{"class"});
        for (File f : classFiles) {
            ClassParser parser = new ClassParser(f.getPath());
            JavaClass clazz = parser.parse();
            if (clazz.isInterface() || clazz.isAnnotation())
                continue;
            
            boolean checkSingleton = false;
            for (Field field : clazz.getFields()) {
                for (AnnotationEntry ae : field.getAnnotationEntries()) {
                    if ((ae.getAnnotationType().contains("InspectablePrimitive") ||
                            ae.getAnnotationType().contains("InspectableForSize")));
                    else if (ae.getAnnotationType().contains("InspectionPoint")) {
                        // check if this implements Inspectable
                        Class c = Class.forName(field.getType().toString());
                        if (!Inspectable.class.isAssignableFrom(c)) 
                            throw new Exception("bad class "+c.getCanonicalName());
                    } else
                        continue;
                    String separator = field.isStatic() ? ":" : ",";
                    props.put(ae.getElementValuePairs()[0].getValue().toString(),clazz.getClassName()+separator+field.getName());
                    checkSingleton |= !field.isStatic();
                }
            }
            
            if (checkSingleton) {
                boolean singleton = false;
                for (AnnotationEntry ae : clazz.getAnnotationEntries()) {
                    String type = ae.getAnnotationType();
                    if (type.contains("Singleton") || type.contains("InspectableContainer")) {
                        singleton = true;
                        break;
                    }
                }
                if (!singleton)
                    throw new Exception("class not singleton "+clazz.getClassName());
            }
        }
        for (String s : props.keySet())
            System.out.println(s+"="+props.get(s));
    }

}
