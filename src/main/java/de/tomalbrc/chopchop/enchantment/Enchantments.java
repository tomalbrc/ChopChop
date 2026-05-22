package de.tomalbrc.chopchop.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

public final class Enchantments {
    public static final ResourceKey<Enchantment> TREE_FELLER = create("chopchop:tree_feller");

    private static ResourceKey<Enchantment> create(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse(name));
    }
}