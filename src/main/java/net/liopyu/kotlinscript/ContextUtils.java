package net.liopyu.kotlinscript;

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
    }
}
