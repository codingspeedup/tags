package io.github.codingspeedup.tags.ai.boundary;

public interface ToolboxSupport {

    default ClassLoader getToolboxClassLoader() {
        return ToolboxSupport.class.getClassLoader();
    }

    default void warn(String message) {
        System.err.println(message);
    }

}
