package com.timeclock;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Timer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.function.Supplier;

public class Time {
    public static long millis = 0L;

    public static long realMillis = 0L;

    public static Timer timer = new Timer(20.0F, 0L);

    private static boolean isTimeStop = false;

    public static void abs(final Player e) {
        if (!get()) {
            e.playSound(TimeClockMod.STOP.get(), 1.0F, 1.0F);
            AreaParticle p = new AreaParticle((Minecraft.getInstance()).level, e.getX(), e.getY(), e.getZ(), 0.0D, 0.0D, 0.0D, RendererUtils.beam.toString(), 0.62F, 0.3F, 0.3F, 0.3F, 0.4F, false, 1.4D) {
                public Vec3 getPos() {
                    return e.position();
                }
            };
            p.setLifetime(80);
            (Minecraft.getInstance()).particleEngine.add(p);
        }
        if (!get()) (Minecraft.getInstance()).gameRenderer.shutdownEffect();
        (Minecraft.getInstance()).particleEngine.tick();
    }

    public static void setIsTimeStop(boolean isTimeStop) {
        Time.isTimeStop = isTimeStop;
        (Minecraft.getInstance()).pause = isTimeStop;
    }

    public static boolean get() {
        return isTimeStop;
    }

    public static class AreaParticle extends Particle {
        public static int MAX_LIFE = 100;

        public boolean growing = true;

        public String loc;

        public float[] rgba = new float[4];

        public float sz;

        public double slow;

        public double grow;

        public boolean shaders;

        public int image;

        public AreaParticle(ClientLevel worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double vx, double vy, double vz, String loc, float sz, float r, float g, float b, float a, boolean shaders, double grow) {
            super(worldIn, xCoordIn, yCoordIn, zCoordIn, vx, vy, vz);
            this.xd = vx;
            this.yd = vy;
            this.zd = vz;
            this.rCol = 1.0F;
            this.gCol = 1.0F;
            this.bCol = 1.0F;
            this.alpha = 0.4F;
            this.lifetime = 100;
            MAX_LIFE = this.lifetime;
            this.rgba[0] = r;
            this.rgba[1] = g;
            this.rgba[2] = b;
            this.rgba[3] = a;
            this.loc = loc;
            this.sz = sz;
            this.image = this.random.nextInt(6) + 1;
            this.shaders = shaders;
            this.slow = 0.6D;
            this.grow = grow;
        }

        public static void render(AreaParticle particle, double d3, double d4, double d5, PoseStack matrix, float partialTicks) {
            double d0 = particle.xo + (particle.x - particle.xo) * partialTicks;
            double d1 = particle.yo + (particle.y - particle.yo) * partialTicks;
            double d2 = particle.zo + (particle.z - particle.zo) * partialTicks;
            matrix.pushPose();
            matrix.translate(d0 - d3, d1 - d4, d2 - d5);
            Entity e = Minecraft.getInstance().getCameraEntity();
            if (e != null) {
                float r = particle.rgba[0];
                float g = particle.rgba[1];
                float b = particle.rgba[2];
                float a = particle.rgba[3];
                MultiBufferSource.BufferSource buf = Minecraft.getInstance().renderBuffers().bufferSource();
                ResourceLocation loc = new ResourceLocation(particle.loc);
                GlowRenderLayer glowRenderLayer = new GlowRenderLayer(new CullWrappedRenderLayer(MegaRenderType.createSphereRenderType(loc, 0)));
                double size = particle.growing ? Math.max(0.0D, particle.sz - particle.grow + particle.grow * partialTicks) : Math.max(0.0D, particle.sz + particle.grow - particle.grow * partialTicks);
                RendererUtils.renderSphere(matrix, buf, (float) size, 20, 240, 240, r, g, b, a, glowRenderLayer);
                buf.endBatch(glowRenderLayer);
            }
            matrix.popPose();
        }

        public void setLifetime(int p_107258_) {
            super.setLifetime(p_107258_);
            MAX_LIFE = p_107258_;
        }

        public void tick() {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            if (this.age++ >= this.lifetime) remove();
            if (this.rgba[3] <= 0.0F) remove();
            this.xd *= this.slow;
            this.yd *= this.slow;
            this.zd *= this.slow;
            if (this.age > MAX_LIFE / 2 + 10) this.growing = false;
            if (this.growing) {
                this.sz = (float) (this.sz + this.grow);
            } else {
                this.sz = (float) (this.sz - this.grow);
            }
            this.oRoll = this.roll;
            setPos((getPos()).x, (getPos()).y, (getPos()).z);
        }

        public void render(@NotNull VertexConsumer p_107261_, @NotNull Camera p_107262_, float p_107263_) {
        }

        public Vec3 getPos() {
            return new Vec3(this.x, this.y, this.z);
        }

        public @NotNull ParticleRenderType getRenderType() {
            return ParticleRenderType.NO_RENDER;
        }
    }

    @Mod.EventBusSubscriber
    public static class RendererUtils {
        public static final ResourceLocation beam = new ResourceLocation("time_clock:textures/item/white.png");

        static Map<ParticleRenderType, Queue<Particle>> ps = null;

        public static void renderSphere(PoseStack matrix, MultiBufferSource buf, float radius, int gradation, int lx, int ly, float r, float g, float b, float a, RenderType type, float percentage) {
            float PI = 3.1415927F;
            VertexConsumer bb = buf.getBuffer(type);
            Matrix4f m = matrix.last().pose();
            float alpha;
            for (alpha = 0.0F; alpha < PI; alpha += PI / gradation) {
                float beta;
                for (beta = 0.0F; beta < PI * 2.0F * percentage; beta += PI / gradation) {
                    float x = (float) (radius * Math.cos(beta) * Math.sin(alpha));
                    float y = (float) (radius * Math.sin(beta) * Math.sin(alpha));
                    float z = (float) (radius * Math.cos(alpha));
                    bb.vertex(m, x, y, z).color(r, g, b, a).uv(0.0F, 1.0F).uv2(lx, ly).endVertex();
                    double sin = Math.sin((alpha + PI / gradation));
                    x = (float) (radius * Math.cos(beta) * sin);
                    y = (float) (radius * Math.sin(beta) * sin);
                    z = (float) (radius * Math.cos((alpha + PI / gradation)));
                    bb.vertex(m, x, y, z).color(r, g, b, a).uv(0.0F, 1.0F).uv2(lx, ly).endVertex();
                }
            }
        }

        public static void renderSphere(PoseStack matrix, MultiBufferSource buf, float radius, int gradation, int lx, int ly, float r, float g, float b, float a, RenderType type) {
            renderSphere(matrix, buf, radius, gradation, lx, ly, r, g, b, a, type, 1.0F);
        }

        public static void particleRenders(PoseStack matrix, float partialTicks) {
            Vec3 proj = (Minecraft.getInstance()).gameRenderer.getMainCamera().getPosition();
            double d3 = proj.x;
            double d4 = proj.y;
            double d5 = proj.z;
            HashSet<Particle> particles = getNoRenderParticles((Minecraft.getInstance()).particleEngine);
            if (particles.size() > 0) for (Particle particle : particles) {
                if (particle instanceof AreaParticle p) {
                    p.tick();
                    AreaParticle.render(p, d3, d4, d5, matrix, partialTicks);
                }
            }
        }

        public static HashSet<Particle> getNoRenderParticles(ParticleEngine manager) {
            if (ps == null) ps = manager.particles;
            Queue<Particle> q = ps.get(ParticleRenderType.NO_RENDER);
            if (q != null) return new HashSet<>(q);
            return new HashSet<>();
        }

        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void gui(RenderGuiOverlayEvent event) {
            PoseStack stack = new PoseStack();
            stack.pushPose();
            stack.scale(0.01F, 0.01F, 0.01F);
            stack.popPose();
        }

        @SubscribeEvent
        public static void renderLevel(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                Entity re = Minecraft.getInstance().getCameraEntity();
                if (re != null) particleRenders(event.getPoseStack(), event.getPartialTick());
            }
        }
    }

    public static class GlowRenderLayer extends RenderType {

        private final RenderType delegate;

        public GlowRenderLayer(RenderType delegate) {
            super("magic" + delegate.toString() + "_with_framebuffer", delegate.format(), delegate.mode(), delegate.bufferSize(), true, delegate.isOutline(), () -> {
                delegate.setupRenderState();
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(770, 771);
                GL11.glDepthFunc(513);
                GL11.glDepthMask(false);
            }, () -> {

            });
            this.delegate = delegate;
        }

        public @NotNull Optional<RenderType> outline() {
            return this.delegate.outline();
        }

        public boolean equals(@Nullable Object other) {
            return (other instanceof GlowRenderLayer && this.delegate.equals(((GlowRenderLayer) other).delegate));
        }

        public int hashCode() {
            return Objects.hash(this.delegate);
        }
    }

    public static class CullWrappedRenderLayer extends RenderType {
        private final RenderType delegate;

        public CullWrappedRenderLayer(RenderType delegate) {
            super("magic" + delegate.toString() + "_with_cull", delegate.format(), delegate.mode(), delegate.bufferSize(), true, delegate.isOutline(), () -> {
                delegate.setupRenderState();
                RenderSystem.disableBlend();
            }, () -> {
                RenderSystem.enableCull();
                delegate.clearRenderState();
            });
            this.delegate = delegate;
        }

        public @NotNull Optional<RenderType> outline() {
            return this.delegate.outline();
        }

        public boolean equals(@Nullable Object other) {
            return (other instanceof CullWrappedRenderLayer && this.delegate.equals(((CullWrappedRenderLayer) other).delegate));
        }

        public int hashCode() {
            return Objects.hash(this.delegate);
        }
    }

    public static class MegaRenderType extends RenderType {
        public MegaRenderType(String p_173178_, VertexFormat p_173179_, VertexFormat.Mode p_173180_, int p_173181_, boolean p_173182_, boolean p_173183_, Runnable p_173184_, Runnable p_173185_) {
            super(p_173178_, p_173179_, p_173180_, p_173181_, p_173182_, p_173183_, p_173184_, p_173185_);
        }

        public static RenderType createSphereRenderType(ResourceLocation r, int x) {
            return create("magic_sphere3" + x, DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.TRIANGLE_STRIP, 256, false, false, CompositeState.builder().setLayeringState(RenderStateShard.POLYGON_OFFSET_LAYERING).setShaderState(new ShaderStateShard(GameRenderer::getPositionColorTexShader)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).setCullState(NO_CULL).setLightmapState(NO_LIGHTMAP).setTextureState(new TextureStateShard(r, false, false)).createCompositeState(true));
        }
    }

    public static class PacketHandler {

        public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation("megatimestop", "main"), () -> "1", "1"::equals, "1"::equals);

        public static void registerPackets() {
            int id = 0;
            INSTANCE.registerMessage(id++, TimeStopPacket.class, TimeStopPacket::encode, TimeStopPacket::decode, TimeStopPacket::handle);
        }
    }

    public static class TimeStopPacket {
        private final boolean paused;

        private final int id;

        public TimeStopPacket(boolean pressed, int id) {
            this.paused = pressed;
            this.id = id;
        }

        public static TimeStopPacket decode(FriendlyByteBuf buf) {
            return new TimeStopPacket(buf.readBoolean(), buf.readInt());
        }

        public static void encode(TimeStopPacket msg, FriendlyByteBuf buf) {
            buf.writeBoolean(msg.paused);
            buf.writeInt(msg.id);
        }

        public static void handle(TimeStopPacket msg, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> {
                if (msg != null) context.get().enqueueWork(() -> {
                });
            });
            context.get().setPacketHandled(true);
        }
    }

    public static class RenderEvent extends TickEvent {
        public final float renderTickTime;

        public RenderEvent(TickEvent.Phase phase, float renderTickTime) {
            super(TickEvent.Type.RENDER, LogicalSide.CLIENT, phase);
            this.renderTickTime = renderTickTime;
        }
    }

}