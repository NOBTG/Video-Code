package com.timeclock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.function.Function;

public class SuperFont extends Font {
    public static final SuperFont INSTANCE = new SuperFont(Minecraft.getInstance().fontManager.createFont().fonts, true);
    static int Mode = 0;
    boolean C = false;
    boolean D = false;
    public int alpha = 255;
    private static Matrix4f matrix4ff;
    private static MultiBufferSource source;
    private static boolean bb1;
    private static boolean displayMode;
    private static int ii;
    private static int ii1;
    private static PoseStack stackk;
    public float tick = 0.0f;

    public SuperFont(Function<ResourceLocation, FontSet> function, boolean boolean1) {
        super(function, boolean1);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent evt) {
        if (evt.phase == TickEvent.Phase.START) {
            float speed = 0.8f;
            this.tick += speed;
            if (this.tick >= 720.0f) {
                this.tick = 0.0f;
            }
            if (this.alpha >= 255) {
                this.alpha -= 5;
            } else {
                this.alpha += 5;
            }
        }
    }

    public float nextColorHue(float the_step) {
        float tick = (this.tick + the_step) % 720.0f;
        if (tick >= 360.0f) {
            return 720.0f - tick;
        }
        return tick;
    }

    static String replacetoString(String s) {
        return s.replaceAll("(?i)§[0-9A-FK-OR]", "");
    }

    static long milliTime() {
        return System.nanoTime() / 1000000L;
    }

    private int CoreRender(String text, float x, float y, boolean isStack, boolean isDrawInBatch, boolean isShadow, boolean NoRender) {
        boolean r = false;
        if (!NoRender) {
            text = replacetoString(text);
            if (Mode == 0) {
                float the_step = 0.0f;
                float centerX = x + (float) this.width(text) / 2;
                float centerY = y + (float) this.lineHeight / 2;
                float radius = 4.0f;
                float rotationSpeed = 0.1f;
                float rotationOffset = tick * rotationSpeed;
                char[] chars = text.toCharArray();
                float j = 0.0f;
                for (char c : chars) {
                    String s = String.valueOf(c);
                    j += 1.0f;
                    float angle = (2 * (float) Math.PI * j / chars.length) + rotationOffset;
                    float offset = (float) Math.sin((tick + j) * 0.1f) * 5.0f;
                    float charX = centerX + radius * (float) Math.cos(angle) + offset;
                    float charY = centerY + radius * (float) Math.sin(angle) + offset;
                    int removeInterval = 10;
                    int restoreInterval = 20;
                    if (tick % (removeInterval + restoreInterval) < removeInterval) {
                        r = true;
                    }
                    int rgb = Color.HSBtoRGB(this.nextColorHue(the_step) / 100.0f, 0.8f, 0.8f);
                    float yOffset = (float) Math.sin((j + (float) milliTime() / 300.0F));
                    if (!r && C) {
                        render(charX, charY, isStack, isDrawInBatch, isShadow, s, rgb);
                    } else if (D) {
                        render(x + offset, y + offset, isStack, isDrawInBatch, isShadow, s, rgb);
                    } else {
                        render(x, y + yOffset, isStack, isDrawInBatch, isShadow, s, rgb);
                    }
                    x += (float) this.width(s);
                    the_step += 1.0f;
                }
            }
        } else {
            for (char c : (replacetoString(text)).toCharArray()) {
                x += (float) this.width(String.valueOf(c));
            }
        }
        return (int) x;
    }

    private void render(float x, float y, boolean isStack, boolean isDrawInBatch, boolean isShadow, String s, int rgb) {
        render_text(isStack, isDrawInBatch, isShadow, s, rgb, x, y);
    }

    private void render_text(boolean isStack, boolean isDrawInBatch, boolean isShadow, String s, int rgb, float new_x, float new_y) {
        if (isStack && !isShadow) {
            super.draw(stackk, s, new_x, new_y, rgb);
            super.drawShadow(stackk, s, new_x, new_y, rgb);
            super.draw(stackk, s, new_x - 0.75f, new_y - 0.75f, rgb);
            super.drawShadow(stackk, s, new_x - 0.75f, new_y - 0.75f, rgb);
        } else if (isStack) {
            super.drawShadow(stackk, s, new_x, new_y, rgb);
            super.drawShadow(stackk, s, new_x - 0.75f, new_y - 0.75f, rgb);
        } else if (isDrawInBatch && matrix4ff != null && source != null) {
            super.drawInBatch(s, new_x, new_y, rgb, bb1, matrix4ff, source, displayMode, ii, ii1);
            super.drawInBatch(s, new_x - 0.75f, new_y - 0.75f, rgb, bb1, matrix4ff, source, displayMode, ii, ii1);
        }
    }

    @Override
    public int drawInBatch(@NotNull FormattedCharSequence formattedCharSequence, float x, float y, int color, boolean b1, @NotNull Matrix4f matrix4f, @NotNull MultiBufferSource multiBufferSource, boolean mode, int i, int i1) {
        bb1 = b1;
        matrix4ff = matrix4f;
        source = multiBufferSource;
        displayMode = mode;
        ii = i;
        ii1 = i1;
        return CoreRender(toString(formattedCharSequence), x, y, false, true, false, false);
    }

    @Override
    public int draw(@NotNull PoseStack stack, @NotNull String text, float x, float y, int color) {
        stackk = stack;
        CoreRender(text, x, y, true, false, true, false);
        return CoreRender(text, x, y, false, false, false, true);
    }

    @Override
    public int drawShadow(@NotNull PoseStack stack, @NotNull FormattedCharSequence formattedCharSequence, float x, float y, int color) {
        stackk = stack;
        return CoreRender(toString(formattedCharSequence), x, y, true, false, true, false);
    }

    @Override
    public int drawShadow(@NotNull PoseStack stack, @NotNull String string, float x, float y, int color) {
        stackk = stack;
        return CoreRender(string, x, y, true, false, true, false);
    }

    static String toString(FormattedCharSequence formattedCharSequence) {
        StringBuilder stringBuilder = new StringBuilder();
        formattedCharSequence.accept((index, style, codePoint) -> {
            stringBuilder.appendCodePoint(codePoint);
            return true;
        });
        return stringBuilder.toString();
    }
}
