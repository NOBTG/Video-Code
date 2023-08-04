package com.timeclock;

import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class ClockItem extends Item {
    public static int stopping_time = 0;

    public ClockItem(Item.Properties properties) {
        super(properties);
    }

    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        player.setItemInHand(hand, player.getItemInHand(hand));
        if (level.isClientSide) {
            Timer timer = new Timer();
            Speed.isSpeed = true;
            if (TimeClockMod.PERCENT > 1 && !Time.get()) {
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        TimeClockMod.changeAll(5);
                    }
                };
                timer.scheduleAtFixedRate(task, 250, Long.MAX_VALUE);
            }
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    Speed.isSpeed = false;
                    Time.abs(player);
                    Time.setIsTimeStop(!Time.get());
                    timer.cancel();
                }
            };
            timer.scheduleAtFixedRate(task, 1000, Long.MAX_VALUE);
        } else {
            Time.PacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> player), new Time.TimeStopPacket(Time.get(), player.getId()));
        }
        player.getCooldowns().addCooldown(this, 4);
        return super.use(level, player, hand);
    }


    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return Component.translatable("item.time_clock.time_clock");
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public @NotNull Font getFont(ItemStack stack, FontContext context) {
                return SuperFont.INSTANCE;
            }
        });
    }

    static boolean isOddMillisecond() {
        return (System.nanoTime() / 1250000000L) % 2 != 0;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> list, @NotNull TooltipFlag flag) {
        if (isOddMillisecond()) {
            list.add(Component.literal("星海辰光，时空交织。"));
        } else {
            list.add(Component.literal("宇宙の星々、時の輪舞。"));
        }
    }
}
