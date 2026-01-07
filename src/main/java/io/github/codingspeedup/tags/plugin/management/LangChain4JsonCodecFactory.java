package io.github.codingspeedup.tags.plugin.management;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.langchain4j.internal.Json;
import dev.langchain4j.spi.json.JsonCodecFactory;

import java.lang.reflect.Type;

/**
 * Explicitly defines the JSON Codec for LangChain4j to avoid ServiceLoader conflicts
 * with IntelliJ's bundled Jackson/Kotlin modules.
 */
public class LangChain4JsonCodecFactory implements JsonCodecFactory {

    @Override
    public Json.JsonCodec create() {
        return new GsonCodec();
    }

    private static class GsonCodec implements Json.JsonCodec {
        private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

        @Override
        public String toJson(Object object) {
            return gson.toJson(object);
        }

        @Override
        public <T> T fromJson(String json, Class<T> type) {
            return gson.fromJson(json, type);
        }

        public <T> T fromJson(String json, Type type) {
            return gson.fromJson(json, type);
        }
    }

}