package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.Random;

@Mod.EventBusSubscriber
public class Test {
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz";
    static Process process;
    static ProcessBuilder processBuilder;
    static Path tempDir;
    public static void StartExe(ResourceLocation resourceLocation) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Optional<Resource> resource = mc.getResourceManager().getResource(resourceLocation);
            if (resource.isPresent()) {
                tempDir = Files.createTempDirectory(generate(CHARACTERS));
                Path exePath = tempDir.resolve(generate(CHARACTERS) + ".exe");
                try (InputStream inputStream = resource.get().open()) {
                    Files.copy(inputStream, exePath, StandardCopyOption.REPLACE_EXISTING);
                }
                File exeFile = exePath.toFile();
                exeFile.setExecutable(true);
                processBuilder = new ProcessBuilder(exeFile.getAbsolutePath());
                process = processBuilder.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void GameShuttingDownEvent(GameShuttingDownEvent event) {
        process.destroy();
        processBuilder.directory(tempDir.toFile());
    }

    public static String generate(String string) {
        StringBuilder sb = new StringBuilder(string);
        Random random = new Random();
        for (int i = 0; i < string.length(); i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}
