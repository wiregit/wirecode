package org.limewire.ui.swing.search.model;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;

public class MockSimilarResultsFactory {

    public static VisualSearchResult newMockVisualSearchResult(VisualSearchResult parent, String description) {
        return (VisualSearchResult) Proxy.newProxyInstance(MockSimilarResultsFactory.class.getClassLoader(), 
                new Class[] {VisualSearchResult.class}, new MockVisualResultHandler(description, parent));
    }
    
    private static class MockVisualResultHandler implements InvocationHandler {
        private final VisualSearchResult parent;
        private final String description;
        private boolean visible = true;
        
        public MockVisualResultHandler(String description, VisualSearchResult parent) {
            this.description = description;
            this.parent = parent;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if (methodName.equals("getDescription")) {
                return description;
            } else if (methodName.equals("isVisible")) {
                return visible;
            } else if (methodName.equals("setVisible")) {
                visible = ((Boolean)args[0]).booleanValue();
            } else if (methodName.equals("getSimilarityParent")) {
                return parent;
            } else if (methodName.equals("getSimilarResults")) {
                return Collections.emptyList();
            } else {
                return method.invoke(parent, args);
            }
            return null;
        }
    }
}
