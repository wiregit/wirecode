package org.limewire.store.server;

import java.lang.reflect.Field;
import java.util.Date;

/**
 * This will create the parameter and command names that we use in the
 * javascript so they are consistant. We do this so obfuscation will be better.
 */
final class CreateJavascriptVariables {

    public static void main(String[] args) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException {
        new CreateJavascriptVariables().realMain(args);
    }

    private void realMain(String[] args) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException {
        outln("/** --- Generated on " + new Date() + " by " + getClass().getName() + " DO NOT EDIT --- */");
        outputClass("Parameters", "PARAM");
        outputClass("Commands", "COMMAND");
        outln("/** --- end of server constants --- */");
    }
    
    private void outputClass(String classSuffix, String suffix) throws SecurityException, IllegalArgumentException, ClassNotFoundException, IllegalAccessException {
        for (Field f : Class.forName(getClass().getPackage().getName() + ".Dispatcher$" + classSuffix).getFields()) output(f, suffix);
        outln("");
    }
    
    private void output(Field f, String suffix) throws IllegalArgumentException, IllegalAccessException {
        outln("var " + f.getName() + "_" + suffix + " = \'" + f.get(null) + "';");
    }

    private void outln(Object msg) { System.out.println(msg); }

}
