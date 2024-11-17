package nanonestor.cursedearth;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class CursedEarthConfig {
    public static final ModConfigSpec GENERAL_SPEC;
    public static final General GENERAL;
    public static final ModConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        final Pair<General, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(General::new);
        GENERAL = specPair.getLeft();
        GENERAL_SPEC = specPair.getRight();
        final Pair<Client, ModConfigSpec> specPair1 = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT = specPair1.getLeft();
        CLIENT_SPEC = specPair1.getRight();
    }

    public static class Client {

        public final ModConfigSpec.ConfigValue<String> color_cursed_earth;
        public final ModConfigSpec.ConfigValue<String> color_blessed_earth;

        public Client(ModConfigSpec.Builder builder) {
            builder.push("Client");
            color_cursed_earth = builder
                    .comment("Color of cursed earth, pick #CC00FF classic style color, pick #222222 for brighter newage color, or any hex code color you would like.")
                    .define("color_cursed_earth", "#CC00FF", String.class::isInstance);
            color_blessed_earth = builder
                    .comment("Color of blessed earth, default value is #00BCD4")
                    .define("color_blessed_earth", "#00BCD4", String.class::isInstance);
            builder.pop();
        }
    }

    public static class General {

        public final ModConfigSpec.IntValue minTickTime;
        public final ModConfigSpec.IntValue maxTickTime;
        public final ModConfigSpec.IntValue burnLightLevel;
        public final ModConfigSpec.BooleanValue forceSpawn;
        public final ModConfigSpec.BooleanValue diesFromLightLevel;
        public final ModConfigSpec.BooleanValue naturallySpreads;
        public final ModConfigSpec.IntValue spawnRadius;
        public final ModConfigSpec.BooleanValue doItemsMakeEarth;
        public final ModConfigSpec.ConfigValue<String> cursedItem;
        public final ModConfigSpec.ConfigValue<String> blessedItem;

        public General(ModConfigSpec.Builder builder) {
            builder.push("General");

            minTickTime = builder
                    .comment("minimum time between spawns in ticks")
                    .defineInRange("min tick time", 75, 1, Integer.MAX_VALUE);
            maxTickTime = builder
                    .comment("maximum time between spawns in ticks")
                    .defineInRange("max tick time", 600, 1, Integer.MAX_VALUE);
            burnLightLevel = builder
                    .comment("the light level above which cursed earth blocks burn - default 7 - allowed values 1 to 15")
                    .defineInRange("burn light level", 7, 1, 15);
            forceSpawn = builder
                    .comment("Force spawns to occur regardless of conditions such as light level and elevation")
                    .define("force spawns", false);
            diesFromLightLevel = builder
                    .comment("does cursed earth die from light levels")
                    .define("dies from light level", true);
            naturallySpreads = builder
                    .comment("does cursed earth naturally spread")
                    .define("naturally spreads", true);
            doItemsMakeEarth = builder
                    .comment("do the items set as 'cursed item' and 'blessed item' make earths - set false to disable")
                    .define("do items make earth", true);
            spawnRadius = builder
                    .comment("minimum distance cursed earth has to be away from players before it spawns mobs")
                    .defineInRange("spawn radius", 1, 1, Integer.MAX_VALUE);
            cursedItem = builder
                    .comment("item used to create cursed earth")
                    .define("cursed item", BuiltInRegistries.ITEM.getKey(Items.WITHER_ROSE).toString()); //, o -> o instanceof String s&&
                         //   BuiltInRegistries.ITEM.getOptional(new ResourceLocation(s).isPresent());
            blessedItem = builder
                    .comment("item used to create blessed earth")
                    .define("blessed item", ("cursedearth:blessed_flower"), o -> o instanceof String); //&&
                          //  BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(MODID)).isPresent());

            builder.pop();
    }

    }
}
