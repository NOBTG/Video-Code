package github.nobtg;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraftforge.entity.PartEntity;

public abstract class Util {
    public static void Remove_Entity(Entity entity) {
        EntityProxy proxy = (EntityProxy) entity;
        if (proxy.getRemovalReason() == null)
            proxy.setRemovalReason(Entity.RemovalReason.KILLED);
        if (proxy.getRemovalReason().shouldDestroy())
            entity.stopRiding();
        entity.getPassengers().forEach(Entity::stopRiding);
        if (entity.level() instanceof ServerLevel serverLevel) {
            ServerLevelProxy proxy1 = (ServerLevelProxy) serverLevel;
            proxy1.getEntityTickList().remove(entity);
            serverLevel.getChunkSource().removeEntity(entity);
            if (entity instanceof ServerPlayer serverplayer) {
                proxy1.getPlayers().remove(serverplayer);
                serverLevel.updateSleepingPlayerList();
            }
            if (entity instanceof Mob mob) {
                if (proxy1.isUpdatingNavigations())
                    net.minecraft.Util.logAndPauseIfInIde("onTrackingStart called during navigation iteration", new IllegalStateException("onTrackingStart called during navigation iteration"));
                proxy1.getNavigatingMobs().remove(mob);
            }
            if (entity.isMultipartEntity())
                for (PartEntity<?> part : entity.getParts())
                    if (part != null)
                        proxy1.getDragonParts().remove(part.getId());
            entity.updateDynamicGameEventListener(DynamicGameEventListener::remove);
            entity.onRemovedFromWorld();
            PersistentEntitySectionManagerProxy proxy2 = ((PersistentEntitySectionManagerProxy) proxy1.getEntityManager());
            proxy2.getVisibleEntityStorage().remove(entity);
            serverLevel.getScoreboard().entityRemoved(entity);
            proxy2.getKnownUuids().remove(entity.getUUID());
        }
        if (Minecraft.getInstance().level != null) {
            ClientLevel level = Minecraft.getInstance().level;
            ClientLevelProxy proxy1 = (ClientLevelProxy) level;
            if (entity.isAlwaysTicking())
                proxy1.getTickingEntities().remove(entity);
            entity.unRide();
            proxy1.getPlayers().remove(entity);
            entity.onRemovedFromWorld();
            if (entity.isMultipartEntity())
                for (net.minecraftforge.entity.PartEntity<?> part : entity.getParts())
                    proxy1.getPartEntities().remove(part.getId());
            ((TransientEntitySectionManagerProxy) proxy1.getEntityStorage()).getEntityStorage().remove(entity);
        }
        entity.invalidateCaps();
    }
}