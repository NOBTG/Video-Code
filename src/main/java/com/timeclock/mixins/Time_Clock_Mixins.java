package com.timeclock.mixins;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.TimerQuery;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.timeclock.*;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.*;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FrameTimer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.metrics.profiling.MetricsRecorder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Time_Clock_Mixins {

    @Mixin({Camera.class})
    public static class CameraMixin {
        @ModifyVariable(method = {"setup"}, at = @At("HEAD"), ordinal = 0, argsOnly = true)
        public float render(float val) {
            return Time.get() ? Time.timer.partialTick : (Minecraft.getInstance()).realPartialTick;
        }
    }

    @Mixin({GameRenderer.class})
    public abstract static class GameRendererMixin {

        @Shadow
        @Nullable
        private PostChain postEffect;

        @Shadow
        @Final
        private Minecraft minecraft;

        @Shadow
        private boolean effectActive;

        @Redirect(method = {"render"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;effectActive:Z"))
        private boolean effectActive(GameRenderer gameRenderer) {
            ResourceLocation timeClock$DESATURATE = new ResourceLocation("shaders/post/desaturate.json");
            ClientLevel level = (Minecraft.getInstance()).level;
            if (level == null) return this.effectActive;
            if (Time.get() && this.postEffect != null && this.postEffect.getName().equals(timeClock$DESATURATE.toString())) return true;
            return this.effectActive;
        }

        @Inject(method = {"shutdownEffect"}, at = {@At("HEAD")}, cancellable = true)
        public void se(CallbackInfo ci) {
            if (Time.get() && this.minecraft.level != null && this.postEffect != null && this.postEffect.getName().endsWith("the_world.json")) ci.cancel();
        }

        @Inject(method = {"checkEntityPostEffect"}, at = {@At("HEAD")}, cancellable = true)
        public void checkEntityPostEffect(Entity p_109107_, CallbackInfo ci) {
            if (Time.get() && this.minecraft.level != null && this.postEffect != null && this.postEffect.getName().endsWith("the_world.json")) ci.cancel();
        }

        @ModifyVariable(method = {"renderItemInHand"}, at = @At("HEAD"), ordinal = 0, argsOnly = true)
        public float partials(float val) {
            return Time.get() ? Time.timer.partialTick : val;
        }

        @ModifyVariable(method = {"render"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;render(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"), ordinal = 0, argsOnly = true)
        public float render(float val) {
            return Time.get() ? Time.timer.partialTick : val;
        }
    }

    @Mixin({LivingEntity.class})
    public abstract static class LivingEntityMixin extends Entity {

        public LivingEntityMixin(EntityType<?> p_19870_, Level p_19871_) {
            super(p_19870_, p_19871_);
        }

        @Inject(method = {"tickEffects"}, at = {@At("HEAD")}, cancellable = true)
        public void tickEffects(CallbackInfo ci) {
            if (Time.get())
                ci.cancel();
        }

        @Inject(method = {"hurt"}, at = {@At("HEAD")})
        public void hurt(DamageSource p_21016_, float p_21017_, CallbackInfoReturnable<Boolean> cir) {
            if (Time.get() && p_21016_.getEntity() instanceof net.minecraft.world.entity.player.Player) this.invulnerableTime = 0;
        }

        @Inject(method = {"hurt"}, at = {@At(value = "RETURN", ordinal = 6)})
        public void hurt_last(DamageSource p_21016_, float p_21017_, CallbackInfoReturnable<Boolean> cir) {
            if (Time.get() && p_21016_.getEntity() instanceof net.minecraft.world.entity.player.Player) this.invulnerableTime = 0;
        }
    }

    @Mixin({Minecraft.class})
    public abstract static class MinecraftMixin {
        @Shadow
        public static int fps;

        @Shadow
        public int rightClickDelay;

        @Shadow
        public ProfilerFiller profiler;

        @Shadow
        public Gui gui;

        @Shadow
        public volatile boolean pause;

        @Shadow
        @Nullable
        public Screen screen;

        @Shadow
        @Nullable
        public LocalPlayer player;

        @Shadow
        @Nullable
        public ClientLevel level;

        @Shadow
        public int missTime;

        @Shadow
        @Nullable
        public Overlay overlay;

        @Shadow
        public GameRenderer gameRenderer;

        @Mutable
        @Shadow
        public Timer timer;

        @Shadow
        public float pausePartialTick;

        @Shadow
        public float realPartialTick;

        @Shadow
        public Window window;

        @Shadow
        public int frames;

        @Shadow
        public long lastNanoTime;

        @Shadow
        public Options options;

        @Shadow
        public MetricsRecorder metricsRecorder;

        @Shadow
        public double gpuUtilization;

        @Shadow
        @Nullable
        public TimerQuery.FrameProfile currentFrameProfile;

        @Shadow
        public long savedCpuDuration;

        @Shadow
        public FrameTimer frameTimer;

        @Shadow
        public long lastTime;

        @Shadow
        public String fpsString;

        @Inject(method = {"tick"}, at = {@At("HEAD")}, cancellable = true)
        public void tick(CallbackInfo ci) {
            if (Time.get()) {
                this.pause = true;
                ClockItem.stopping_time++;
                if (this.rightClickDelay > 0) this.rightClickDelay--;
                this.profiler.push("gui");
                this.profiler.pop();
                if (this.screen == null && this.player != null) {
                    if (this.player.isDeadOrDying() && !(this.screen instanceof net.minecraft.client.gui.screens.DeathScreen)) {
                        setScreen(null);
                    } else if (this.player.isSleeping() && this.level != null) {
                        setScreen(new InBedChatScreen());
                    }
                } else {
                    Screen $$4 = this.screen;
                    if ($$4 instanceof InBedChatScreen inbedchatscreen) {
                        if (this.player != null && !this.player.isSleeping()) inbedchatscreen.onPlayerWokeUp();
                    }
                }
                if (this.screen != null) this.missTime = 10000;
                if (this.screen != null) Screen.wrapScreenError(() -> this.screen.tick(), "Ticking screen", this.screen.getClass().getCanonicalName());
                if (this.overlay == null && this.screen == null) {
                    handleKeybinds();
                    if (this.missTime > 0) this.missTime--;
                }
                if (this.level != null) this.level.tickingEntities.forEach(s -> {
                    if (!s.isRemoved() && !s.isPassenger()) {
                        if (s instanceof net.minecraft.world.entity.player.Player && s != this.player) {
                            Objects.requireNonNull(this.level);
                            this.level.guardEntityTick(this.level::tickNonPassenger, s);
                        }
                        if (s.tickCount < 1) {
                            Objects.requireNonNull(this.level);
                            this.level.guardEntityTick(this.level::tickNonPassenger, s);
                        }
                    }
                });
                this.realPartialTick = this.pausePartialTick;
                ci.cancel();
            } else {
                ClockItem.stopping_time = 0;
                this.timer.msPerTick = 50.0F;
            }
        }

        @Inject(method = {"run"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/metrics/profiling/MetricsRecorder;endTick()V")})
        public void run(CallbackInfo ci) {
            if (Time.get()) for (int i = 0; i < Time.timer.advanceTime(Time.realMillis); i++) {
                if (this.level != null && this.player != null) {
                    Objects.requireNonNull(this.level);
                    this.level.guardEntityTick(this.level::tickNonPassenger, (Entity) this.player);
                    this.gui.tick((Minecraft.getInstance()).pause);
                    this.gameRenderer.itemInHandRenderer.tick();
                    this.gameRenderer.tick();
                    if (this.screen != null) this.screen.tick();
                    tick();
                }
            }
            if ((Minecraft.getInstance()).level == null && Time.realMillis > 3000L) Time.setIsTimeStop(false);
        }

        @Inject(method = {"runTick"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V")})
        public void postEvent(boolean p_91384_, CallbackInfo ci) {
            MinecraftForge.EVENT_BUS.post(new Time.RenderEvent(TickEvent.Phase.START, this.realPartialTick));
        }

        @Inject(method = {"runTick"}, at = {@At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setErrorSection(Ljava/lang/String;)V", ordinal = 2)}, cancellable = true)
        public void pauseGame(boolean p_91384_, CallbackInfo ci) {
            if (Time.get()) {
                boolean flag;
                this.window.setErrorSection("Post render");
                this.frames++;
                if (!this.options.renderDebug && !this.metricsRecorder.isRecording()) {
                    flag = false;
                    this.gpuUtilization = 0.0D;
                } else {
                    flag = (this.currentFrameProfile == null || this.currentFrameProfile.isDone());
                    if (flag) TimerQuery.getInstance().ifPresent(TimerQuery::beginProfile);
                }
                long l = Util.getNanos();
                long i1 = l - this.lastNanoTime;
                if (flag) this.savedCpuDuration = i1;
                this.frameTimer.logFrameDuration(i1);
                this.lastNanoTime = l;
                this.profiler.push("fpsUpdate");
                if (this.currentFrameProfile != null && this.currentFrameProfile.isDone()) this.gpuUtilization = this.currentFrameProfile.get() * 100.0D / this.savedCpuDuration;
                while (Util.getMillis() >= this.lastTime + 1000L) {
                    String s;
                    if (this.gpuUtilization > 0.0D) {
                        s = " GPU: " + ((this.gpuUtilization > 100.0D) ? (ChatFormatting.RED + "100%") : (Math.round(this.gpuUtilization) + "%"));
                    } else {
                        s = "";
                    }
                    int k1 = getFramerateLimit();
                    fps = this.frames;
                    this.fpsString = String.format(Locale.ROOT, "%d fps T: %s%s%s%s B: %d%s", fps, (k1 == 260) ? "inf" : Integer.valueOf(k1), this.options.enableVsync().get() ? " vsync" : "", this.options.graphicsMode().get(), (this.options.cloudStatus().get() == CloudStatus.OFF) ? "" : ((this.options.cloudStatus().get() == CloudStatus.FAST) ? " fast-clouds" : " fancy-clouds"), this.options.biomeBlendRadius().get(), s);
                    this.lastTime += 1000L;
                    this.frames = 0;
                }
                this.profiler.pop();
                ci.cancel();
            }
        }

        @Shadow
        public abstract void setScreen(@Nullable Screen paramScreen);

        @Shadow
        protected abstract void handleKeybinds();

        @Shadow
        public abstract void tick();

        @Shadow
        protected abstract int getFramerateLimit();
    }

    @Mixin(value = {RenderLevelStageEvent.class}, remap = false)
    public static class RenderLevelStageEventMixin {
        @Inject(method = {"getPartialTick"}, at = {@At("HEAD")}, cancellable = true)
        private void getPartialTick(CallbackInfoReturnable<Float> cir) {
            ClientLevel level = (Minecraft.getInstance()).level;
            if (level == null) return;
            if (Time.get()) cir.setReturnValue(0.0F);
        }
    }

    @Mixin(value = {ForgeEventFactory.class}, remap = false)
    public static class ForgeEventFactoryMixin {
        private static boolean timeClock$timestop;

        @Inject(method = {"onRenderTickEnd"}, at = {@At("HEAD")}, cancellable = true)
        private static void onRenderTickEnd(CallbackInfo ci) {
            ResourceLocation timeClock$DESATURATE = new ResourceLocation("shaders/post/desaturate.json");
            ClientLevel level = (Minecraft.getInstance()).level;
            if (level == null) return;
            Minecraft mc = Minecraft.getInstance();
            if (Time.get()) {
                timeClock$timestop = true;
                if (mc.gameRenderer.currentEffect() == null) mc.gameRenderer.loadEffect(timeClock$DESATURATE);
                ci.cancel();
            } else if (timeClock$timestop) {
                timeClock$timestop = false;
                mc.gameRenderer.checkEntityPostEffect(mc.getCameraEntity());
            }
        }
    }

    @Mixin({MinecraftServer.class})
    public abstract static class MinecraftServerMixin {
        @Shadow
        public abstract void tickChildren(BooleanSupplier paramBooleanSupplier);

        @Inject(method = {"runServer"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tickServer(Ljava/util/function/BooleanSupplier;)V")})
        public void run(CallbackInfo ci) {
            if (Time.get()) tickChildren(() -> true);
        }
    }

    @Mixin({ItemEntity.class})
    public static class ItemEntityMixin {
        @Inject(method = {"playerTouch"}, at = {@At(value = "HEAD")}, cancellable = true)
        public void playerTouch(CallbackInfo ci) {
            if (Time.get()) ci.cancel();
        }
    }

    @Mixin({ExperienceOrb.class})
    public static class ExperienceOrbMixin {
        @Inject(method = {"playerTouch"}, at = {@At(value = "HEAD")}, cancellable = true)
        public void playerTouch(CallbackInfo ci) {
            if (Time.get()) ci.cancel();
        }
    }

    @Mixin({RenderStateShard.class})
    public static class RenderMixin {
        @Redirect(method = {"setupGlintTexturing"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getMillis()J", ordinal = 0))
        private static long setupGlint() {
            return Time.millis;
        }
    }

    @Mixin({ChatListener.class})
    public static class ChatListenerMixin {
        @Inject(method = {"tick"}, at = {@At(value = "HEAD")}, cancellable = true)
        public void tick(CallbackInfo ci) {
            if (Time.get()) ci.cancel();
        }
    }

    @Mixin(Screen.class)
    public static class ScreenMixin extends GuiComponent {
        @Shadow
        private ItemStack tooltipStack;
        @Shadow
        protected ItemRenderer itemRenderer;

        @Inject(method = {"renderTooltipInternal"}, at = {@At("HEAD")}, cancellable = true)
        public void renderTooltipInternal(PoseStack p_169384_, java.util.List<ClientTooltipComponent> p_169385_, int p_169386_, int p_169387_, CallbackInfo ci) {
            if (tooltipStack.getItem() == TimeClockMod.CLOCK.get()) {
                ci.cancel();
                Screen screen = (Screen) (Object) (this);
                if (!p_169385_.isEmpty()) {
                    net.minecraftforge.client.event.RenderTooltipEvent.Pre preEvent = net.minecraftforge.client.ForgeHooksClient.onRenderTooltipPre(tooltipStack, p_169384_, p_169386_, p_169387_, screen.width, screen.height, p_169385_, SuperFont.INSTANCE, SuperFont.INSTANCE);
                    if (preEvent.isCanceled()) return;
                    int i = 0;
                    int j = p_169385_.size() == 1 ? -2 : 0;

                    for (ClientTooltipComponent clienttooltipcomponent : p_169385_) {
                        int k = clienttooltipcomponent.getWidth(preEvent.getFont());
                        if (k > i) {
                            i = k;
                        }

                        j += clienttooltipcomponent.getHeight();
                    }

                    int j2 = preEvent.getX() + 12;
                    int k2 = preEvent.getY() - 12;
                    if (j2 + i > screen.width) {
                        j2 -= 28 + i;
                    }

                    if (k2 + j + 6 > screen.height) {
                        k2 = screen.height - j - 6;
                    }

                    p_169384_.pushPose();
                    float f = itemRenderer.blitOffset;
                    itemRenderer.blitOffset = 400.0F;
                    Tesselator tesselator = Tesselator.getInstance();
                    BufferBuilder bufferbuilder = tesselator.getBuilder();
                    RenderSystem.setShader(GameRenderer::getPositionColorShader);
                    bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                    Matrix4f matrix4f = p_169384_.last().pose();
                    int empty = 0x00FFFFFF;
                    fillGradient(matrix4f, bufferbuilder, j2 - 3, k2 - 4, j2 + i + 3, k2 - 3, 400, empty, empty);
                    fillGradient(matrix4f, bufferbuilder, j2 - 3, k2 + j + 3, j2 + i + 3, k2 + j + 4, 400, empty, empty);
                    fillGradient(matrix4f, bufferbuilder, j2 - 3, k2 - 3, j2 + i + 3, k2 + j + 3, 400, empty, empty);
                    fillGradient(matrix4f, bufferbuilder, j2 - 4, k2 - 3, j2 - 3, k2 + j + 3, 400, empty, empty);
                    fillGradient(matrix4f, bufferbuilder, j2 + i + 3, k2 - 3, j2 + i + 4, k2 + j + 3, 400, empty, empty);
                    float hue = SuperFont.INSTANCE.nextColorHue(0);
                    int startColor = Color.HSBtoRGB(hue / 100.0f, 0.4f, 0.3f);
                    hue = SuperFont.INSTANCE.nextColorHue(1);
                    int endColor = Color.HSBtoRGB(hue / 100.0f, 0.4f, 0.3f);
                    fillGradient(matrix4f, bufferbuilder, j2 - 3, k2 - 3 + 1, j2 - 3 + 1, k2 + j + 3 - 1, 400, startColor, -endColor);
                    fillGradient(matrix4f, bufferbuilder, j2 + i + 2, k2 - 3 + 1, j2 + i + 3, k2 + j + 3 - 1, 400, startColor, -endColor);
                    fillGradient(matrix4f, bufferbuilder, j2 - 3, k2 - 3, j2 + i + 3, k2 - 3 + 1, 400, startColor, startColor);
                    fillGradient(matrix4f, bufferbuilder, j2 - 3, k2 + j + 2, j2 + i + 3, k2 + j + 3, 400, endColor, endColor);
                    RenderSystem.enableDepthTest();
                    RenderSystem.disableTexture();
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    BufferUploader.drawWithShader(bufferbuilder.end());
                    RenderSystem.disableBlend();
                    RenderSystem.enableTexture();
                    MultiBufferSource.BufferSource multibuffersource$buffersource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
                    p_169384_.translate(0.0D, 0.0D, 400.0D);
                    int l1 = k2;

                    for (int i2 = 0; i2 < p_169385_.size(); ++i2) {
                        ClientTooltipComponent clienttooltipcomponent1 = p_169385_.get(i2);
                        clienttooltipcomponent1.renderText(preEvent.getFont(), j2, l1, matrix4f, multibuffersource$buffersource);
                        l1 += clienttooltipcomponent1.getHeight() + (i2 == 0 ? 2 : 0);
                    }

                    multibuffersource$buffersource.endBatch();
                    p_169384_.popPose();
                    l1 = k2;

                    for (int l2 = 0; l2 < p_169385_.size(); ++l2) {
                        ClientTooltipComponent clienttooltipcomponent2 = p_169385_.get(l2);
                        clienttooltipcomponent2.renderImage(preEvent.getFont(), j2, l1, p_169384_, itemRenderer, 400);
                        l1 += clienttooltipcomponent2.getHeight() + (l2 == 0 ? 2 : 0);
                    }
                    itemRenderer.blitOffset = f;
                }
            }
        }
    }

    @Mixin({ServerLevel.class})
    public abstract static class ServerMixin extends Level {
        @Shadow
        @Final
        private ServerChunkCache chunkSource;

        protected ServerMixin(WritableLevelData p_220352_, ResourceKey<Level> p_220353_, Holder<DimensionType> p_220354_, Supplier<ProfilerFiller> p_220355_, boolean p_220356_, boolean p_220357_, long p_220358_, int p_220359_) {
            super(p_220352_, p_220353_, p_220354_, p_220355_, p_220356_, p_220357_, p_220358_, p_220359_);
        }

        public <T extends Entity> void guardEntityTick(@NotNull Consumer<T> p_46654_, @NotNull T p_46655_) {
            if (Time.get()) {
                if (!(p_46655_ instanceof net.minecraft.world.entity.player.Player) && p_46655_.tickCount > 0) return;
                this.chunkSource.tick(() -> true, true);
            }
            super.guardEntityTick(p_46654_, p_46655_);
        }

        @Inject(method = {"tickTime"}, at = {@At("HEAD")}, cancellable = true)
        public void tickTime(CallbackInfo ci) {
            if (Time.get()) ci.cancel();
        }

        @Inject(method = {"tickBlock"}, at = {@At("HEAD")}, cancellable = true)
        public void tickBlock(BlockPos p_184113_, Block p_184114_, CallbackInfo ci) {
            if (Time.get()) ci.cancel();
        }

        public void updateNeighborsAt(@NotNull BlockPos p_46673_, @NotNull Block p_46674_) {
            if (!Time.get()) super.updateNeighborsAt(p_46673_, p_46674_);
        }
    }

    @Mixin({IntegratedServer.class})
    public static class Server_Pause {
        @Shadow
        private boolean paused;

        @Inject(method = {"tickServer"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/server/IntegratedServer;getProfiler()Lnet/minecraft/util/profiling/ProfilerFiller;")})
        public void tick(BooleanSupplier p_120049_, CallbackInfo ci) {
            if (Time.get()) this.paused = false;
        }
    }

    @Mixin({Util.class})
    public static class TimeMixin {
        @Inject(method = {"getMillis"}, at = {@At("HEAD")}, cancellable = true)
        private static void getMillis(CallbackInfoReturnable<Long> cir) {
            if (!Speed.isSpeed) cir.setReturnValue(Time.realMillis);
            else cir.setReturnValue(TimeClockMod.millis);
        }
    }
}