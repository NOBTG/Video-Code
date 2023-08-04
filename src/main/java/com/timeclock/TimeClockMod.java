package com.timeclock;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod(TimeClockMod.MODID)
public class TimeClockMod {
    static CreativeModeTab tab;
    public static float PERCENT = 20F;
    public static double millisF = 0F;
    public static long millis = 0;
    public static Timer timer;
    public static ScheduledExecutorService service;
    public static final String MODID = "time_clock";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<Item> CLOCK = ITEMS.register("time_clock", () -> new ClockItem((new Item.Properties()).rarity(Rarity.UNCOMMON).fireResistant().stacksTo(1).tab(tab)));
    public static final RegistryObject<Item> Knife = ITEMS.register("knife", () -> new KnifeItem((new Item.Properties()).rarity(Rarity.UNCOMMON).fireResistant().stacksTo(16).tab(tab)));
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);
    public static final RegistryObject<SoundEvent> STOP = SOUNDS.register("stop", () -> new SoundEvent(new ResourceLocation(MODID, "stop")));
    public static final RegistryObject<SoundEvent> THROW = SOUNDS.register("knife_throw", () -> new SoundEvent(new ResourceLocation(MODID, "knife_throw")));
    public static final RegistryObject<SoundEvent> KNIFE_HIT = SOUNDS.register("knife_hit", () -> new SoundEvent(new ResourceLocation(MODID, "knife_hit")));
    public static final RegistryObject<EntityType<KnifeItem.KnifeEntity>> flyingSwordEntity = ENTITIES.register("flying_sword", () -> EntityType.Builder.of(KnifeItem.KnifeEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(25).build("flying_sword"));

    public TimeClockMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modEventBus);
        ENTITIES.register(modEventBus);
        SOUNDS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            public void run() {
                if (!Time.get()) Time.millis++;
                Time.realMillis++;
            }
        };
        timer.scheduleAtFixedRate(task, 1L, 1L);
        Time.PacketHandler.registerPackets();
        tab = new CreativeModeTab(Component.translatable("tab.mts.name").getString()) {
            @Override
            public @NotNull ItemStack makeIcon() {
                return new ItemStack(CLOCK.get());
            }

            @Override
            public @NotNull Component getDisplayName() {
                return Component.translatable("tab.mts.name").withStyle(ChatFormatting.YELLOW);
            }

            @Override
            public boolean hasSearchBar() {
                return false;
            }

            @Override
            public boolean canScroll() {
                return false;
            }
        };
        create();
    }
    public static void create() {
        if (service == null)
            service = Executors.newSingleThreadScheduledExecutor();

        if (timer == null)
            timer = new Timer();
        try {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    service.scheduleAtFixedRate(TimeClockMod::update, 1L, 1L, TimeUnit.MILLISECONDS);
                }
            }, 1L);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void update() {
        float p = PERCENT / 20.0F;
        millisF = p + millisF;
        millis = (long) millisF;
    }
    public static void changeAll(float percent) {
        PERCENT = percent;
    }
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEntityRenderers {
        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(TimeClockMod.flyingSwordEntity.get(), KnifeItem.KnifeRenderer::new);
        }
    }
}
