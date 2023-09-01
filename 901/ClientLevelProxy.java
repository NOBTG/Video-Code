package github.nobtg.Mixins.Proxys;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import net.minecraftforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ClientLevel.class)
public interface ClientLevelProxy {
    @Accessor("tickingEntities")
    EntityTickList getTickingEntities();
    @Accessor("players")
    List<AbstractClientPlayer> getPlayers();
    @Accessor("partEntities")
    Int2ObjectMap<PartEntity<?>> getPartEntities();
    @Accessor("entityStorage")
    TransientEntitySectionManager<Entity> getEntityStorage();
}
