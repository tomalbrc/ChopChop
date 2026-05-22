package de.tomalbrc.chopchop.config;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.tomalbrc.chopchop.Chopchop;
import de.tomalbrc.chopchop.util.CodecDeserializer;
import de.tomalbrc.chopchop.enchantment.Enchantments;
import de.tomalbrc.chopchop.impl.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancements.criterion.*;
import net.minecraft.core.Holder;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    static Path CONFIG_FILE_PATH = FabricLoader.getInstance().getConfigDir().resolve("chopchop.json");
    static ModConfig instance;

    public static final Codec<ModConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("animated").orElse(false).forGetter(c -> c.animated),
                    Codec.INT.fieldOf("animation_duration").orElse(60).forGetter(c -> c.animationDuration),
                    Codec.FLOAT.fieldOf("speed_multiplication").orElse(2f).forGetter(c -> c.speedMultiplication),
                    Codec.FLOAT.fieldOf("max_speed_multiplication").orElse(512f).forGetter(c -> c.maxSpeedMultiplication),
                    TreeConfig.CODEC.codec().listOf().fieldOf("configs").forGetter(c -> c.configs)
            ).apply(instance, (animated, ad, sm, msm, c) -> {
                ModConfig config = new ModConfig();
                config.animated = animated;
                config.animationDuration = ad;
                config.speedMultiplication = sm;
                config.maxSpeedMultiplication = msm;
                config.configs = c;
                return config;
            })
    );

    static Gson JSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(ModConfig.class, new CodecDeserializer<>(CODEC))
            .create();

    public boolean animated = false;
    public int animationDuration = 60;
    public float speedMultiplication = 2;
    public float maxSpeedMultiplication = 512;

    public List<TreeConfig> configs = new ArrayList<>();


    private static ModConfig createDefault() {
        ModConfig config = new ModConfig();
        config.animated = true;
        config.configs = new ArrayList<>();

        Holder<Enchantment> treeFeller = Chopchop.SERVER.registryAccess().getOrThrow(Enchantments.TREE_FELLER);

        ItemPredicate enchantmentPredicate = ItemPredicate.Builder.item()
                .withComponents(DataComponentMatchers.Builder.components()
                        .partial(DataComponentPredicates.ENCHANTMENTS,
                                EnchantmentsPredicate.enchantments(List.of(
                                        new EnchantmentPredicate(treeFeller, MinMaxBounds.Ints.atLeast(1))
                                ))
                        ).build()
                ).build();

        config.configs.add(new TreeConfig.Builder(TreeConfig.Type.GENERIC)
                .enabled(true)
                .requireTool(true)
                .allowedToolFilter(enchantmentPredicate)
                .logFilter(BlockPredicate.Builder.block()
                        .of(BuiltInRegistries.BLOCK, BlockTags.LOGS)
                        .build())
                .leavesFilter(BlockPredicate.Builder.block()
                        .of(BuiltInRegistries.BLOCK, BlockTags.LEAVES)
                        .build())
                .algorithm(new TreeConfig.Algorithm(7, 256, true))
                .build()
        );

        config.configs.add(new TreeConfig.Builder(TreeConfig.Type.VERTICAL)
                .enabled(true)
                .requireTool(true)
                .allowedToolFilter(enchantmentPredicate)
                .logFilter(BlockPredicate.Builder.block()
                        .of(BuiltInRegistries.BLOCK, Blocks.CACTUS, Blocks.BAMBOO)
                        .build())
                .build()
        );

        config.configs.add(new TreeConfig.Builder(TreeConfig.Type.CHORUS)
                .enabled(true)
                .requireTool(false)
                .logFilter(BlockPredicate.Builder.block().of(BuiltInRegistries.BLOCK, Blocks.CHORUS_PLANT).build())
                .leavesFilter(BlockPredicate.Builder.block().of(BuiltInRegistries.BLOCK, Blocks.CHORUS_FLOWER).build())
                .allowedToolFilter(ItemPredicate.Builder.item().build())
                .build()
        );

        config.configs.add(new TreeConfig.Builder(TreeConfig.Type.RED_MUSHROOM)
                .enabled(true)
                .requireTool(true)
                .logFilter(BlockPredicate.Builder.block().of(BuiltInRegistries.BLOCK, Blocks.MUSHROOM_STEM).build())
                .leavesFilter(BlockPredicate.Builder.block().of(BuiltInRegistries.BLOCK, Blocks.RED_MUSHROOM_BLOCK).build())
                .allowedToolFilter(enchantmentPredicate)
                .build()
        );

        config.configs.add(new TreeConfig.Builder(TreeConfig.Type.BROWN_MUSHROOM)
                .enabled(true)
                .requireTool(true)
                .logFilter(BlockPredicate.Builder.block().of(BuiltInRegistries.BLOCK, Blocks.MUSHROOM_STEM).build())
                .leavesFilter(BlockPredicate.Builder.block().of(BuiltInRegistries.BLOCK, Blocks.BROWN_MUSHROOM_BLOCK).build())
                .allowedToolFilter(enchantmentPredicate)
                .build()
        );

        return config;
    }

    public static ModConfig getInstance() {
        if (instance == null) {
            if (!load()) // only save if file wasn't just created
                save(); // save since newer versions may contain new options, also removes old options
        }
        return instance;
    }

    public static boolean load() {
        TreeTypes.TYPES.clear();

        if (!CONFIG_FILE_PATH.toFile().exists()) {
            instance = createDefault();
            try (FileOutputStream stream = new FileOutputStream(CONFIG_FILE_PATH.toFile())) {
                stream.write(JSON.toJson(instance).getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (instance == null) try {
            ModConfig.instance = JSON.fromJson(new FileReader(ModConfig.CONFIG_FILE_PATH.toFile()), ModConfig.class);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        if (instance != null) for (TreeConfig config : instance.configs) {
            TreeType type = switch (config.type) {
                case CHORUS -> new ChorusTree(config);
                case GENERIC -> new GenericTree(config);
                case RED_MUSHROOM -> new RedMushroomTree(config);
                case BROWN_MUSHROOM -> new BrownMushroomTree(config);
                case VERTICAL -> new VerticalTree(config);
            };

            TreeTypes.TYPES.add(type);
        }

        return instance != null;
    }

    private static void save() {
        try (FileOutputStream stream = new FileOutputStream(CONFIG_FILE_PATH.toFile())) {
            stream.write(JSON.toJson(instance).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
