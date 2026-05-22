package de.tomalbrc.chopchop.entity;

import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class Entities {
    public static final EntityType<AnimatedFallingTree> FALLING_TREE = registerEntity("animated_falling_tree", EntityType.Builder.of(AnimatedFallingTree::new, MobCategory.MISC));
    public static final EntityType<SequentialFallingTree> FALLING_TREE2 = registerEntity("sequential_falling_tree", EntityType.Builder.of(SequentialFallingTree::new, MobCategory.MISC));

    private static <T extends Entity> EntityType<T> registerEntity(String str, EntityType.Builder<T> type) {
        var id = Identifier.fromNamespaceAndPath("chopchop", str);
        return registerEntity(id, type);
    }

    private static <T extends Entity> EntityType<T> registerEntity(Identifier id, EntityType.Builder<T> type) {
        var res = Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type.build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
        PolymerEntityUtils.registerType(res);
        return res;
    }

    public static void init() {}
}
