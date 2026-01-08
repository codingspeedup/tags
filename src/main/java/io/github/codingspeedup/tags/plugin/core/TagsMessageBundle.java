package io.github.codingspeedup.tags.plugin.core;


import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public final class TagsMessageBundle {
    public static final String BUNDLE = "messages.TagsMessageBundle";
    private static final DynamicBundle INSTANCE = new DynamicBundle(TagsMessageBundle.class, BUNDLE);

    private TagsMessageBundle() {
    }

    @NotNull
    @Nls
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }

    @NotNull
    public static Supplier<String> lazyMessage(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getLazyMessage(key, params);
    }

}