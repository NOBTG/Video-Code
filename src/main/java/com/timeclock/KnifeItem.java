package com.timeclock;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class KnifeItem extends Item {
    private final Multimap<Attribute, AttributeModifier> attributeModifiers;

    public KnifeItem(Item.Properties p_41383_) {
        super(p_41383_);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 5.0D, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -1.0D, AttributeModifier.Operation.ADDITION));
        this.attributeModifiers = builder.build();
    }

    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) return this.attributeModifiers;
        return super.getAttributeModifiers(slot, stack);
    }

    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack handStack = player.getItemInHand(hand);
        int knivesToThrow = player.isShiftKeyDown() ? handStack.getCount() : 1;
        if (!level.isClientSide()) {
            for (int i = 0; i < knivesToThrow; i++) {
                KnifeEntity knifeEntity = KnifeEntity.create(level, player);
                knifeEntity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F, (i == 0) ? 1.0F : 28.0F);
                level.addFreshEntity(knifeEntity);
            }
            if (!player.isCreative()) handStack.shrink(knivesToThrow);
        }
        player.playSound(TimeClockMod.THROW.get());
        player.swing(hand);
        return super.use(level, player, hand);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return Component.translatable("item.time_clock.knife");
    }
    public static class KnifeEntity extends Arrow {

        public KnifeEntity(EntityType<? extends KnifeEntity> entityEntityType, Level level) {
            super(entityEntityType, level);
        }

        protected net.minecraft.sounds.SoundEvent getDefaultHitGroundSoundEvent() {
            return TimeClockMod.KNIFE_HIT.get();
        }

        public static KnifeEntity create(Level level, Player player) {
            KnifeEntity obj = new KnifeEntity(TimeClockMod.flyingSwordEntity.get(), level);
            obj.setOwner(player);
            obj.setPos(player.position().add(0.0D, player.getEyeHeight(player.getPose()), 0.0D));
            return obj;
        }

        public void tick() {
            if (!Time.get() || (this.tickCount < 5)) {
                super.tick();
                if (!isOnGround() && !getLevel().isClientSide()) {
                    Vec3 posVec = position();
                    BlockHitResult rayTraceResult = getLevel().clip(new ClipContext(posVec, posVec.add(getDeltaMovement()), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this));
                    if (rayTraceResult.getType() == HitResult.Type.BLOCK) {
                        BlockPos blockPos = rayTraceResult.getBlockPos();
                        Block block = getLevel().getBlockState(blockPos).getBlock();
                        if (block.equals(Blocks.COBWEB)) {
                            getLevel().destroyBlock(blockPos, true);
                            setDeltaMovement(getDeltaMovement().scale(0.8D));
                        }
                        if (block.equals(Blocks.TRIPWIRE)) getLevel().destroyBlock(blockPos, true);
                    }
                }
            }
        }

        protected void onHit(@NotNull HitResult rayTraceResult) {
            if (!Time.get() && rayTraceResult instanceof EntityHitResult r) {
                Entity entity = r.getEntity();
                if (entity instanceof LivingEntity l) {
                    l.invulnerableTime = 0;
                }
                super.onHit(rayTraceResult);
            }
        }

        protected @NotNull ItemStack getPickupItem() {
            return new ItemStack(TimeClockMod.Knife.get());
        }
    }

    public static class KnifeRenderer extends EntityRenderer<KnifeEntity> {
        public KnifeRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        public @NotNull ResourceLocation getTextureLocation(@NotNull KnifeEntity entity) {
            return new ResourceLocation(TimeClockMod.MODID, "textures/entity/knife.png");
        }

        public void render(KnifeEntity entity, float yRotation, float partialTick, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
            matrixStack.pushPose();
            matrixStack.scale(1.0F, 1.4F, 1.4F);
            matrixStack.mulPose(Vector3f.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
            matrixStack.mulPose(Vector3f.ZP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
            float tex1x1 = 0.0F;
            float tex1x2 = 0.5F;
            float tex1x2h = 0.125F;
            float tex1y1 = 0.0F;
            float tex1y2 = 0.15625F;
            float tex2x1 = 0.0F;
            float tex2x2 = 0.15625F;
            float tex2y1 = 0.15625F;
            float tex2y2 = 0.3125F;
            float scale = 0.0375F;
            matrixStack.scale(scale, scale, scale);
            matrixStack.translate(-4.0D, 0.0D, 0.0D);
            VertexConsumer ivertexbuilder = buffer.getBuffer(RenderType.entityCutout(getTextureLocation(entity)));
            PoseStack.Pose matrixstack$entry = matrixStack.last();
            Matrix4f matrix4f = matrixstack$entry.pose();
            Matrix3f matrix3f = matrixstack$entry.normal();
            vertex(matrix4f, matrix3f, ivertexbuilder, -4, -2, -2, tex2x1, tex2y1, -1, 0, 0, packedLight);
            vertex(matrix4f, matrix3f, ivertexbuilder, -4, -2, 2, tex2x2, tex2y1, -1, 0, 0, packedLight);
            vertex(matrix4f, matrix3f, ivertexbuilder, -4, 2, 2, tex2x2, tex2y2, -1, 0, 0, packedLight);
            vertex(matrix4f, matrix3f, ivertexbuilder, -4, 2, -2, tex2x1, tex2y2, -1, 0, 0, packedLight);
            vertex(matrix4f, matrix3f, ivertexbuilder, -4, 2, -2, tex2x1, tex2y1, 1, 0, 0, packedLight);
            vertex(matrix4f, matrix3f, ivertexbuilder, -4, 2, 2, tex2x2, tex2y1, 1, 0, 0, packedLight);
            vertex(matrix4f, matrix3f, ivertexbuilder, -4, -2, 2, tex2x2, tex2y2, 1, 0, 0, packedLight);
            vertex(matrix4f, matrix3f, ivertexbuilder, -4, -2, -2, tex2x1, tex2y2, 1, 0, 0, packedLight);
            for (int j = 0; j < 4; j++) {
                matrixStack.mulPose(Vector3f.XP.rotationDegrees(90.0F));
                vertex(matrix4f, matrix3f, ivertexbuilder, -8, -2, 0, tex1x1, tex1y1, 0, 1, 0, packedLight);
                vertex(matrix4f, matrix3f, ivertexbuilder, (j % 2 == 1) ? 8 : -4, -2, 0, (j % 2 == 1) ? tex1x2 : tex1x2h, tex1y1, 0, 1, 0, packedLight);
                vertex(matrix4f, matrix3f, ivertexbuilder, (j % 2 == 1) ? 8 : -4, 2, 0, (j % 2 == 1) ? tex1x2 : tex1x2h, tex1y2, 0, 1, 0, packedLight);
                vertex(matrix4f, matrix3f, ivertexbuilder, -8, 2, 0, tex1x1, tex1y2, 0, 1, 0, packedLight);
            }
            matrixStack.popPose();
        }

        public void vertex(Matrix4f p_254392_, Matrix3f p_254011_, VertexConsumer p_253902_, int p_254058_, int p_254338_, int p_254196_, float p_254003_, float p_254165_, int p_253982_, int p_254037_, int p_254038_, int p_254271_) {
            p_253902_.vertex(p_254392_, p_254058_, p_254338_, p_254196_).color(255, 255, 255, 255).uv(p_254003_, p_254165_).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(p_254271_).normal(p_254011_, p_253982_, p_254038_, p_254037_).endVertex();
        }
    }
}
