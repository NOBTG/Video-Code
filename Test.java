package com.time_sword;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.time_sword.TimeSwordMod.MODID;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class Test {
    public static class MyEntity extends Monster {
        public MyEntity(PlayMessages.SpawnEntity packet, Level world) {
            this(My_Player.get(), world);
        }

        public MyEntity(EntityType<MyEntity> type, Level world) {
            super(type, world);
            xpReward = 0;
            setNoGravity(true);
            setNoAi(true);
        }

        @Override
        public boolean canAttack(@NotNull LivingEntity living) {
            return false;
        }

        @Override
        public boolean canAttack(@NotNull LivingEntity living, @NotNull TargetingConditions conditions) {
            return false;
        }

        @Override
        public boolean canBeAffected(@NotNull MobEffectInstance instance) {
            return false;
        }

        @Override
        public float getYRot() {
            return 0F;
        }

        @Override
        public boolean hurt(@NotNull DamageSource p_21016_, float p_21017_) {
            return false;
        }

        @Override
        public void die(@NotNull DamageSource p_21014_) {
        }

        @Override
        public @NotNull Component getName() {
            return Component.literal("God Player");
        }

        @Override
        public void canUpdate(boolean value) {
        }

        @Override
        public boolean canUpdate() {
            return true;
        }

        @Override
        public float getHealth() {
            return 20.0F;
        }

        public static final Set<Vec3> list = new HashSet<>();

        @Override
        public void tick() {
            AtomicBoolean b = new AtomicBoolean(true);
            list.forEach((vec3) -> {
                if (vec3.x == this.getX() && vec3.y == this.getY() && vec3.z == this.getZ()) b.set(false);
                if (Math.abs(vec3.x - this.getX()) < 1.0 && Math.abs(vec3.y - this.getY()) < 1.0 && Math.abs(vec3.z - this.getZ()) < 1.0)
                    b.set(false);
            });
            if (b.get()) list.add(new Vec3(this.getX(), this.getY(), this.getZ()));
            super.tick();
            this.removalReason = null;
            this.dead = false;
            this.deathTime = 0;
            this.revive();
            this.reviveCaps();
            this.unsetRemoved();
            this.onAddedToWorld();
        }

        @Override
        public void remove(@NotNull RemovalReason removalReason) {
        }

        @Override
        public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
            return NetworkHooks.getEntitySpawningPacket(this);
        }

        public static void init() {
            SpawnPlacements.register(My_Player.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (entityType, world, reason, pos, random) -> (world.getDifficulty() != Difficulty.PEACEFUL && Monster.isDarkEnoughToSpawn(world, pos, random) && Mob.checkMobSpawnRules(entityType, world, reason, pos, random)));
        }

        public static AttributeSupplier.Builder createAttributes() {
            AttributeSupplier.Builder builder = Mob.createMobAttributes();
            builder = builder.add(Attributes.MAX_HEALTH, 20);
            return builder;
        }
    }

    public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final RegistryObject<EntityType<MyEntity>> My_Player = register(EntityType.Builder.<MyEntity>of(MyEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setCustomClientFactory(MyEntity::new).sized(0.6F, 1.8F).clientTrackingRange(32).updateInterval(2));

    private static <T extends Entity> RegistryObject<EntityType<T>> register(EntityType.Builder<T> entityTypeBuilder) {
        return REGISTRY.register("my_player", () -> entityTypeBuilder.build("my_player"));
    }

    @SubscribeEvent
    public static void init(FMLCommonSetupEvent event) {
        event.enqueueWork(MyEntity::init);
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(My_Player.get(), MyEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(My_Player.get(), MyPlayerRenderer::new);
    }

    @OnlyIn(Dist.CLIENT)
    public static class MyPlayerRenderer extends LivingEntityRenderer<MyEntity, MyPlayerModel<MyEntity>> {
        public MyPlayerRenderer(EntityRendererProvider.Context p_174557_) {
            super(p_174557_, new MyPlayerModel<>(p_174557_.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
            this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(p_174557_.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidArmorModel<>(p_174557_.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), p_174557_.getModelManager()));
        }

        public void render(@NotNull MyEntity p_117788_, float p_117789_, float p_117790_, @NotNull PoseStack p_117791_, @NotNull MultiBufferSource p_117792_, int p_117793_) {
            this.setModelProperties(p_117788_);
            this.font = TimeSwordMod.SuperFont.INSTANCE__;
            super.render(p_117788_, p_117789_, p_117790_, p_117791_, p_117792_, p_117793_);
        }

        public @NotNull Vec3 getRenderOffset(MyEntity p_117785_, float p_117786_) {
            return p_117785_.isCrouching() ? new Vec3(0.0D, -0.125D, 0.0D) : super.getRenderOffset(p_117785_, p_117786_);
        }

        private void setModelProperties(MyEntity p_117819_) {
            MyPlayerModel<MyEntity> playermodel = this.getModel();
            if (p_117819_.isSpectator()) {
                playermodel.setAllVisible(false);
                playermodel.head.visible = true;
                playermodel.hat.visible = true;
            } else {
                playermodel.setAllVisible(true);
                playermodel.hat.visible = true;
                playermodel.jacket.visible = true;
                playermodel.leftPants.visible = true;
                playermodel.rightPants.visible = true;
                playermodel.leftSleeve.visible = true;
                playermodel.rightSleeve.visible = true;
                playermodel.crouching = p_117819_.isCrouching();
                HumanoidModel.ArmPose humanoidmodel$armpose = getArmPose(p_117819_, InteractionHand.MAIN_HAND);
                HumanoidModel.ArmPose humanoidmodel$armpose1 = getArmPose(p_117819_, InteractionHand.OFF_HAND);
                if (humanoidmodel$armpose.isTwoHanded()) {
                    humanoidmodel$armpose1 = p_117819_.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
                }

                if (p_117819_.getMainArm() == HumanoidArm.RIGHT) {
                    playermodel.rightArmPose = humanoidmodel$armpose;
                    playermodel.leftArmPose = humanoidmodel$armpose1;
                } else {
                    playermodel.rightArmPose = humanoidmodel$armpose1;
                    playermodel.leftArmPose = humanoidmodel$armpose;
                }
            }

        }

        private static HumanoidModel.ArmPose getArmPose(MyEntity p_117795_, InteractionHand p_117796_) {
            ItemStack itemstack = p_117795_.getItemInHand(p_117796_);
            if (itemstack.isEmpty()) {
                return HumanoidModel.ArmPose.EMPTY;
            } else {
                if (p_117795_.getUsedItemHand() == p_117796_ && p_117795_.getUseItemRemainingTicks() > 0) {
                    UseAnim useanim = itemstack.getUseAnimation();
                    if (useanim == UseAnim.BLOCK) {
                        return HumanoidModel.ArmPose.BLOCK;
                    }

                    if (useanim == UseAnim.BOW) {
                        return HumanoidModel.ArmPose.BOW_AND_ARROW;
                    }

                    if (useanim == UseAnim.SPEAR) {
                        return HumanoidModel.ArmPose.THROW_SPEAR;
                    }

                    if (useanim == UseAnim.CROSSBOW && p_117796_ == p_117795_.getUsedItemHand()) {
                        return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
                    }

                    if (useanim == UseAnim.SPYGLASS) {
                        return HumanoidModel.ArmPose.SPYGLASS;
                    }

                    if (useanim == UseAnim.TOOT_HORN) {
                        return HumanoidModel.ArmPose.TOOT_HORN;
                    }

                    if (useanim == UseAnim.BRUSH) {
                        return HumanoidModel.ArmPose.BRUSH;
                    }
                } else if (!p_117795_.swinging && itemstack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(itemstack)) {
                    return HumanoidModel.ArmPose.CROSSBOW_HOLD;
                }

                HumanoidModel.ArmPose forgeArmPose = net.minecraftforge.client.extensions.common.IClientItemExtensions.of(itemstack).getArmPose(p_117795_, p_117796_, itemstack);
                if (forgeArmPose != null) return forgeArmPose;

                return HumanoidModel.ArmPose.ITEM;
            }
        }

        public @NotNull ResourceLocation getTextureLocation(@NotNull MyEntity p_117783_) {
            return new ResourceLocation(MODID, "steve.png");
        }

        protected void scale(@NotNull MyEntity p_117798_, PoseStack p_117799_, float p_117800_) {
            p_117799_.scale(0.9375F, 0.9375F, 0.9375F);
        }

        @Override
        protected void renderNameTag(@NotNull MyEntity p_114498_, @NotNull Component p_114499_, @NotNull PoseStack p_114500_, @NotNull MultiBufferSource p_114501_, int p_114502_) {
            double d0 = this.entityRenderDispatcher.distanceToSqr(p_114498_);
            if (net.minecraftforge.client.ForgeHooksClient.isNameplateInRenderDistance(p_114498_, d0)) {
                boolean flag = !p_114498_.isDiscrete();
                float f = p_114498_.getNameTagOffsetY();
                int i = "deadmau5".equals(p_114499_.getString()) ? -10 : 0;
                p_114500_.pushPose();
                p_114500_.translate(0.0F, f, 0.0F);
                p_114500_.mulPose(this.entityRenderDispatcher.cameraOrientation());
                p_114500_.scale(-0.025F, -0.025F, 0.025F);
                Matrix4f matrix4f = p_114500_.last().pose();
                Font font = this.getFont();
                float f2 = (float) (-font.width(p_114499_) / 2);
                font.drawInBatch(p_114499_, f2, (float) i, 553648127, false, matrix4f, p_114501_, flag ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, 0x0000FF, p_114502_);
                if (flag) {
                    font.drawInBatch(p_114499_, f2, (float) i, -1, false, matrix4f, p_114501_, Font.DisplayMode.NORMAL, 0, p_114502_);
                }
                p_114500_.popPose();
            }
        }

        protected void setupRotations(MyEntity p_117802_, @NotNull PoseStack p_117803_, float p_117804_, float p_117805_, float p_117806_) {
            float f = p_117802_.getSwimAmount(p_117806_);
            if (p_117802_.isFallFlying()) {
                super.setupRotations(p_117802_, p_117803_, p_117804_, p_117805_, p_117806_);
                float f1 = (float) p_117802_.getFallFlyingTicks() + p_117806_;
                float f2 = Mth.clamp(f1 * f1 / 100.0F, 0.0F, 1.0F);
                if (!p_117802_.isAutoSpinAttack()) {
                    p_117803_.mulPose(Axis.XP.rotationDegrees(f2 * (-90.0F - p_117802_.getXRot())));
                }

                Vec3 vec3 = p_117802_.getViewVector(p_117806_);
                Vec3 vec31 = p_117802_.getDeltaMovement();
                double d0 = vec31.horizontalDistanceSqr();
                double d1 = vec3.horizontalDistanceSqr();
                if (d0 > 0.0D && d1 > 0.0D) {
                    double d2 = (vec31.x * vec3.x + vec31.z * vec3.z) / Math.sqrt(d0 * d1);
                    double d3 = vec31.x * vec3.z - vec31.z * vec3.x;
                    p_117803_.mulPose(Axis.YP.rotation((float) (Math.signum(d3) * Math.acos(d2))));
                }
            } else if (f > 0.0F) {
                super.setupRotations(p_117802_, p_117803_, p_117804_, p_117805_, p_117806_);
                float f3 = p_117802_.isInWater() || p_117802_.isInFluidType((fluidType, height) -> p_117802_.canSwimInFluidType(fluidType)) ? -90.0F - p_117802_.getXRot() : -90.0F;
                float f4 = Mth.lerp(f, 0.0F, f3);
                p_117803_.mulPose(Axis.XP.rotationDegrees(f4));
                if (p_117802_.isVisuallySwimming()) {
                    p_117803_.translate(0.0F, -1.0F, 0.3F);
                }
            } else {
                super.setupRotations(p_117802_, p_117803_, p_117804_, p_117805_, p_117806_);
            }

        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class MyPlayerModel<T extends LivingEntity> extends HumanoidModel<T> {
        public final ModelPart leftSleeve;
        public final ModelPart rightSleeve;
        public final ModelPart leftPants;
        public final ModelPart rightPants;
        public final ModelPart jacket;
        private final ModelPart cloak;
        private final ModelPart ear;
        private final boolean slim;

        public MyPlayerModel(ModelPart p_170821_, boolean p_170822_) {
            super(p_170821_, RenderType::entityTranslucent);
            this.slim = p_170822_;
            this.ear = p_170821_.getChild("ear");
            this.cloak = p_170821_.getChild("cloak");
            this.leftSleeve = p_170821_.getChild("left_sleeve");
            this.rightSleeve = p_170821_.getChild("right_sleeve");
            this.leftPants = p_170821_.getChild("left_pants");
            this.rightPants = p_170821_.getChild("right_pants");
            this.jacket = p_170821_.getChild("jacket");
        }

        protected @NotNull Iterable<ModelPart> bodyParts() {
            return Iterables.concat(super.bodyParts(), ImmutableList.of(this.leftPants, this.rightPants, this.leftSleeve, this.rightSleeve, this.jacket));
        }

        public void setupAnim(@NotNull T p_103395_, float p_103396_, float p_103397_, float p_103398_, float p_103399_, float p_103400_) {
            super.setupAnim(p_103395_, p_103396_, p_103397_, p_103398_, p_103399_, p_103400_);
            this.leftPants.copyFrom(this.leftLeg);
            this.rightPants.copyFrom(this.rightLeg);
            this.leftSleeve.copyFrom(this.leftArm);
            this.rightSleeve.copyFrom(this.rightArm);
            this.jacket.copyFrom(this.body);
            if (p_103395_.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
                if (p_103395_.isCrouching()) {
                    this.cloak.z = 1.4F;
                    this.cloak.y = 1.85F;
                } else {
                    this.cloak.z = 0.0F;
                    this.cloak.y = 0.0F;
                }
            } else if (p_103395_.isCrouching()) {
                this.cloak.z = 0.3F;
                this.cloak.y = 0.8F;
            } else {
                this.cloak.z = -1.1F;
                this.cloak.y = -0.85F;
            }

        }

        public void setAllVisible(boolean p_103419_) {
            super.setAllVisible(p_103419_);
            this.leftSleeve.visible = p_103419_;
            this.rightSleeve.visible = p_103419_;
            this.leftPants.visible = p_103419_;
            this.rightPants.visible = p_103419_;
            this.jacket.visible = p_103419_;
            this.cloak.visible = p_103419_;
            this.ear.visible = p_103419_;
        }

        public void translateToHand(@NotNull HumanoidArm p_103392_, @NotNull PoseStack p_103393_) {
            ModelPart modelpart = this.getArm(p_103392_);
            if (this.slim) {
                float f = 0.5F * (float) (p_103392_ == HumanoidArm.RIGHT ? 1 : -1);
                modelpart.x += f;
                modelpart.translateAndRotate(p_103393_);
                modelpart.x -= f;
            } else {
                modelpart.translateAndRotate(p_103393_);
            }

        }
    }
}
