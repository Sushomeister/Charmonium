package svenhjol.charmonium.module.biome_ambience.sounds;

import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;
import svenhjol.charmonium.handler.SoundHandler;
import svenhjol.charmonium.helper.WorldHelper;
import svenhjol.charmonium.module.biome_ambience.BiomeSound;
import svenhjol.charmonium.module.biome_ambience.SurfaceBiomeSound;
import svenhjol.charmonium.registry.ClientRegistry;

import java.util.function.Predicate;

public class Desert {
    public static SoundEvent DAY;
    public static SoundEvent NIGHT;
    public static final Predicate<Biome> VALID_BIOME = (biome) -> biome.getBiomeCategory() == Biome.BiomeCategory.DESERT;

    public static void register() {
        DAY = ClientRegistry.sound("ambience.desert.day");
        NIGHT = ClientRegistry.sound("ambience.desert.night");
    }

    public static void init(SoundHandler<BiomeSound> handler) {

        // Register desert day.
        handler.getSounds().add(new SurfaceBiomeSound(handler.getPlayer(), true) {
            @Nullable
            @Override
            public SoundEvent getSound() {
                return DAY;
            }

            @Override
            public boolean isValidPlayerCondition() {
                return super.isValidPlayerCondition() && WorldHelper.isDay(player);
            }

            @Override
            public boolean isValidBiomeCondition(ResourceKey<Biome> biomeKey, Biome biome) {
                return VALID_BIOME.test(biome);
            }
        });

        // Register desert night.
        handler.getSounds().add(new SurfaceBiomeSound(handler.getPlayer(), true) {
            @Nullable
            @Override
            public SoundEvent getSound() {
                return NIGHT;
            }

            @Override
            public boolean isValidPlayerCondition() {
                return super.isValidPlayerCondition() && WorldHelper.isNight(player);
            }

            @Override
            public boolean isValidBiomeCondition(ResourceKey<Biome> biomeKey, Biome biome) {
                return VALID_BIOME.test(biome);
            }
        });
    }
}