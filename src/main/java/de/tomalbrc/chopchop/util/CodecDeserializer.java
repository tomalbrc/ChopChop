package de.tomalbrc.chopchop.util;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import de.tomalbrc.chopchop.Chopchop;
import net.minecraft.resources.RegistryOps;

import java.lang.reflect.Type;

public class CodecDeserializer<T> implements JsonDeserializer<T>, JsonSerializer<T> {
    private final Codec<T> codec;

    public CodecDeserializer(Codec<T> codec) {
        this.codec = codec;

    }

    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return codec.parse(RegistryOps.create(JsonOps.INSTANCE, Chopchop.SERVER.registryAccess()), json)
                .getOrThrow(error ->
                        new JsonParseException("Failed to deserialize using Codec: " + error)
                );
    }

    @Override
    public JsonElement serialize(T t, Type type, JsonSerializationContext jsonSerializationContext) {
        return codec.encodeStart(RegistryOps.create(JsonOps.INSTANCE, Chopchop.SERVER.registryAccess()), t).getOrThrow(error ->
                new JsonParseException("Failed to serialize using Codec: " + error)
        );
    }
}