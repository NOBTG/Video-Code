package github.nobtg.Mixins.Proxys;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityProxy {
    @Accessor("removalReason")
    Entity.RemovalReason getRemovalReason();
    @Accessor("removalReason")
    void setRemovalReason(Entity.RemovalReason reason);
}
