package github.nobtg.Mixins.Proxys;

import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;
import java.util.UUID;

@Mixin(PersistentEntitySectionManager.class)
public interface PersistentEntitySectionManagerProxy  {
    @Accessor("visibleEntityStorage")
    EntityLookup<EntityAccess> getVisibleEntityStorage();
    @Accessor("knownUuids")
    Set<UUID> getKnownUuids();
}
