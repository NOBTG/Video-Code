package github.nobtg.Mixins.Proxys;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import net.minecraftforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Set;

@Mixin(ServerLevel.class)
public interface ServerLevelProxy {
    @Accessor("entityManager")
    PersistentEntitySectionManager<Entity> getEntityManager();
    @Accessor("entityTickList")
    EntityTickList getEntityTickList();
    @Accessor("players")
    List<ServerPlayer> getPlayers();
    @Accessor("navigatingMobs")
    Set<Mob> getNavigatingMobs();
    @Accessor("dragonParts")
    Int2ObjectMap<PartEntity<?>> getDragonParts();
    @Accessor("isUpdatingNavigations")
    boolean isUpdatingNavigations();
}
