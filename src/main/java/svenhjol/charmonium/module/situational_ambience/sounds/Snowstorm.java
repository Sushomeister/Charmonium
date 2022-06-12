package svenhjol.charmonium.module.situational_ambience.sounds;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.biome.Biome.BiomeCategory;
import org.jetbrains.annotations.Nullable;
import svenhjol.charmonium.handler.SoundHandler;
import svenhjol.charmonium.helper.WorldHelper;
import svenhjol.charmonium.module.situational_ambience.RepeatedSituationalSound;
import svenhjol.charmonium.module.situational_ambience.SituationalSound;
import svenhjol.charmonium.registry.ClientRegistry;

public class Snowstorm {
    public static SoundEvent SNOWSTORM;

    public static void register() {
        SNOWSTORM = ClientRegistry.sound("situational.snowstorm");
    }

    public static void init(SoundHandler<SituationalSound> handler) {
        handler.getSounds().add(new RepeatedSituationalSound(handler.getPlayer()) {
            @Override
            public boolean isValidSituationCondition() {
                return getBiome(player.blockPosition()).getBiomeCategory() == BiomeCategory.ICY;
            }

            @Override
            public boolean isValidPlayerCondition() {
                return WorldHelper.isOutside(player)
                    && getLevel().isThundering();
            }

            @Nullable
            @Override
            public SoundEvent getSound() {
                return SNOWSTORM;
            }

            @Override
            public int getDelay() {
                return level.random.nextInt(250) + 250;
            }

            @Override
            public float getVolume() {
                return 0.9F;
            }
        });
    }
}