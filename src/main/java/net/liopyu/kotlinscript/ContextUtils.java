package net.liopyu.kotlinscript;

import java.lang.reflect.Method;

public class ContextUtils {
    public static class ClassContext{
        public final Class<?> clazz;
        public final String simpleName;
        public final String alias;
        public final String fullClassName;

        public ClassContext(Class<?> clazz, String simpleName, String alias, String fullClassName) {
            this.clazz = clazz;
            this.simpleName = simpleName;
            this.alias = alias;
            this.fullClassName = fullClassName;
        }
        public Object createInstance() throws Exception {
            return clazz.getDeclaredConstructor().newInstance();
        }
    }
    public static class MethodReferenceContext {
        private final Class<?> clazz;
        private final String methodName;

        public MethodReferenceContext(Class<?> clazz, String methodName) {
            this.clazz = clazz;
            this.methodName = methodName;
        }

        public void invoke() throws Exception {
            Method method = clazz.getMethod(methodName);
            if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                method.invoke(null);
            } else {
                throw new IllegalStateException("Method " + methodName + " is not static.");
            }
        }
    }
}
