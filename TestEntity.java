package com.time_sword;

import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.minecraft.world.entity.LivingEntity.DATA_HEALTH_ID;

@Mod.EventBusSubscriber
public class TestEntity<E extends LivingEntity> {
    boolean kill = false;

    public void FakeDied(E entity) {
        this.kill = true;
        for (int i = 0;i <= 3;i++) {
            if (entity.level() instanceof ServerLevel serverLevel) {
                ServerLevel level = serverLevel;
                entity.getEntityData().set(DATA_HEALTH_ID, 0.0F - (i * Float.MAX_VALUE));
                DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC_KILL));
                ClientboundSetHealthPacket packet = new ClientboundSetHealthPacket(0.0F, 0, 0.0F);
                ClientboundDamageEventPacket packet_ = new ClientboundDamageEventPacket(entity, source);
                level.getChunkSource().broadcastAndSend(entity, packet);
                level.getChunkSource().broadcastAndSend(entity, packet_);
                entity.die(source);
                entity.deathTime = (int) (20F + (i * Float.MAX_VALUE));
                entity.setPose(Pose.DYING);
                entity.setInvisible(false);
                entity.setInvulnerable(false);
                entity.invulnerableTime = 0;
                entity.hurt(source, Float.POSITIVE_INFINITY * i);
            }
        }
    }

    @SubscribeEvent
    public void GuiOpen(ScreenEvent.Opening event) {
        if (kill && (event.getCurrentScreen() instanceof DeathScreen || event.getScreen() instanceof DeathScreen)) event.setCanceled(true);
    }
    //public-f net.minecraft.world.entity.LivingEntity f_20961_ # DATA_HEALTH_ID
}
