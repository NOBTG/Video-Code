package com.example.examplemod;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

import static com.example.examplemod.ExampleMod.PACKET_HANDLER;
import static com.example.examplemod.ExampleMod.addNetworkMessage;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModVariables {
    @SubscribeEvent
    public static void init(FMLCommonSetupEvent event) {
        addNetworkMessage(VariablesSyncMessage.class, VariablesSyncMessage::buffer, VariablesSyncMessage::new, VariablesSyncMessage::handler);
    }

    @SubscribeEvent
    public static void init(RegisterCapabilitiesEvent event) {
        event.register(Variables.class);
    }

    @Mod.EventBusSubscriber
    public static class EventBusVariableHandlers {
        @SubscribeEvent
        public static void EntityLeaveWorld(EntityLeaveWorldEvent event) {
            if (!event.getEntity().level.isClientSide()) event.getEntity().getCapability(VARIABLES_CAPABILITY, null).orElse(new Variables()).syncVariables(event.getEntity());
        }

        @SubscribeEvent
        public static void EntityJoinWorldEvent(EntityJoinWorldEvent event) {
            if (!event.getEntity().level.isClientSide()) event.getEntity().getCapability(VARIABLES_CAPABILITY, null).orElse(new Variables()).syncVariables(event.getEntity());
        }
    }

    public static final Capability<Variables> VARIABLES_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });

    static Object getCapability(Entity entity) {
        return (entity.getCapability(VARIABLES_CAPABILITY, null).orElse(new Variables())).az;
        // 注意这边如果不同步的话要填指定Dist
    }

    static void setCapability(Entity entity, Object obj) {
        // 注意这边如果不同步的话要填指定Dist
        entity.getCapability(VARIABLES_CAPABILITY, null).ifPresent(capability -> {
            capability.az = (boolean) obj;
            capability.syncVariables(entity);
        });
    }

    @Mod.EventBusSubscriber
    private static class VariablesProvider implements ICapabilitySerializable<Tag> {
        @SubscribeEvent
        public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
            event.addCapability(new ResourceLocation(ExampleMod.MODID, "variables"), new VariablesProvider());
        }

        private final Variables variables = new Variables();
        private final LazyOptional<Variables> instance = LazyOptional.of(() -> variables);

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, Direction side) {
            return cap == VARIABLES_CAPABILITY ? instance.cast() : LazyOptional.empty();
        }

        @Override
        public Tag serializeNBT() {
            return variables.writeNBT();
        }

        @Override
        public void deserializeNBT(Tag nbt) {
            variables.readNBT(nbt);
        }
    }

    public static class Variables {
        boolean az = false;

        public void syncVariables(Entity entity) {
            if (!entity.level.isClientSide) PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), new VariablesSyncMessage(this));
        }

        public Tag writeNBT() {
            CompoundTag nbt = new CompoundTag();
            nbt.putBoolean("az", az);
            return nbt;
        }

        public void readNBT(Tag Tag) {
            CompoundTag nbt = (CompoundTag) Tag;
            az = nbt.getBoolean("az");
        }
    }

    public static class VariablesSyncMessage {
        public Variables data;

        public VariablesSyncMessage(FriendlyByteBuf buffer) {
            this.data = new Variables();
            this.data.readNBT(buffer.readNbt());
        }

        public VariablesSyncMessage(Variables data) {
            this.data = data;
        }

        public static void buffer(VariablesSyncMessage message, FriendlyByteBuf buffer) {
            buffer.writeNbt((CompoundTag) message.data.writeNBT());
        }

        public static void handler(VariablesSyncMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                if (!context.getDirection().getReceptionSide().isServer()) {
                    // 客户端逻辑
                } else {
                    // 服务端逻辑
                }
            });
            context.setPacketHandled(true);
        }
    }
}


  /*  public static <T> void addNetworkMessage(Class<T> messageType, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkEvent.Context>> messageConsumer) {
        PACKET_HANDLER.registerMessage(messageID, messageType, encoder, decoder, messageConsumer);
        messageID++;
    }
    public static final String MODID = "lj";
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, MODID), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
    private static int messageID = 0;*/