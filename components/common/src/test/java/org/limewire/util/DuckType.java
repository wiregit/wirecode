package org.limewire.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * A generic class that allows you to mimic an interface.
 * See: http://www.coconut-palm-software.com/the_visual_editor/?p=25
 * for more information
 */
public class DuckType implements InvocationHandler {

    protected final Object object;
    protected final Class objectClass;
    
    protected DuckType(Object object) {
        this.object = object;
        this.objectClass = object.getClass();
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method realMethod = objectClass.getMethod(method.getName(), method.getParameterTypes());
        return realMethod.invoke(object, args);
    }
    
    /**
     * Returns an object that implements the given interface and
     * delegates all calls (through reflection) to the given object.
     * 
     * @param interfaceToImplement The interface that must be implemented
     * @param object The object calls will delegate to
     * @return
     */
    public static Object implement(Class interfaceToImplement, Object object) {
        return Proxy.newProxyInstance(interfaceToImplement.getClassLoader(), 
            new Class[] {interfaceToImplement}, new DuckType(object));
    }

}