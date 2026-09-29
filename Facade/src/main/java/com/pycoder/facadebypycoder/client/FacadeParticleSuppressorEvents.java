package com.pycoder.facadebypycoder.client;

import com.pycoder.facadebypycoder.FacadeByPycoder;
import com.pycoder.facadebypycoder.block.FacadeExtensionBlock;
import com.pycoder.facadebypycoder.block.SimpleFacadeBlock;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FacadeByPycoder.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FacadeParticleSuppressorEvents {
    private static final int SCAN_RADIUS = 12;
    private static final double SUPPRESS_RADIUS_SQUARED = 14.0D * 14.0D;
    private static final double PLAYER_SUPPRESS_RADIUS_SQUARED = 32.0D * 32.0D;
    private static Field particlesField;
    private static List<Field> particleQueueFields;
    private static Field createSoulPulseHandlerField;
    private static Method createSoulPulseRefreshMethod;
    private static boolean reflectionFailed;
    private static boolean createReflectionFailed;
    private static boolean loggedActive;
    private static boolean loggedCreateSuppression;

    private FacadeParticleSuppressorEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        suppressCreateSoulPulses();
        List<BlockPos> facadePositions = nearbyFacadePositions(minecraft);
        if (!facadePositions.isEmpty() && !loggedActive) {
            loggedActive = true;
            FacadeByPycoder.LOGGER.info("Facade particle suppressor active near {} facade blocks", facadePositions.size());
        }
        suppressParticles(minecraft, facadePositions);
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        reflectionFailed = false;
        createReflectionFailed = false;
        loggedActive = false;
        loggedCreateSuppression = false;
    }

    private static List<BlockPos> nearbyFacadePositions(Minecraft minecraft) {
        BlockPos center = minecraft.player.blockPosition();
        List<BlockPos> positions = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS), center.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS))) {
            BlockState state = minecraft.level.getBlockState(pos);
            if (state.getBlock() instanceof SimpleFacadeBlock || state.getBlock() instanceof FacadeExtensionBlock) {
                positions.add(pos.immutable());
            }
        }
        return positions;
    }

    private static void suppressParticles(Minecraft minecraft, List<BlockPos> facadePositions) {
        if (reflectionFailed) {
            return;
        }
        try {
            ensureParticleFields();
            Object particlesObject = particlesField.get(minecraft.particleEngine);
            if (particlesObject instanceof Map<?, ?> particles) {
                for (Object queue : particles.values()) {
                    if (queue instanceof Queue<?> particleQueue) {
                        removeFacadeParticles(particleQueue, minecraft, facadePositions);
                    }
                }
            }
            for (Field queueField : particleQueueFields) {
                Object queueObject = queueField.get(minecraft.particleEngine);
                if (queueObject instanceof Queue<?> particleQueue) {
                    removeFacadeParticles(particleQueue, minecraft, facadePositions);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            reflectionFailed = true;
            FacadeByPycoder.LOGGER.warn("Failed to suppress facade-related client particles", exception);
        }
    }

    private static void ensureParticleFields() throws NoSuchFieldException {
        if (particlesField == null) {
            particlesField = findField("particles", Map.class);
            particlesField.setAccessible(true);
        }
        if (particleQueueFields == null) {
            particleQueueFields = findFields(Queue.class);
            for (Field queueField : particleQueueFields) {
                queueField.setAccessible(true);
            }
        }
    }

    private static Field findField(String namedField, Class<?> expectedType) throws NoSuchFieldException {
        try {
            Field field = ParticleEngine.class.getDeclaredField(namedField);
            if (expectedType.isAssignableFrom(field.getType())) {
                return field;
            }
        } catch (NoSuchFieldException ignored) {
        }
        for (Field field : ParticleEngine.class.getDeclaredFields()) {
            if (expectedType.isAssignableFrom(field.getType())) {
                return field;
            }
        }
        throw new NoSuchFieldException(namedField);
    }

    private static List<Field> findFields(Class<?> expectedType) throws NoSuchFieldException {
        List<Field> fields = new ArrayList<>();
        for (Field field : ParticleEngine.class.getDeclaredFields()) {
            if (expectedType.isAssignableFrom(field.getType())) {
                fields.add(field);
            }
        }
        if (fields.isEmpty()) {
            throw new NoSuchFieldException(expectedType.getName());
        }
        return fields;
    }

    private static void suppressCreateSoulPulses() {
        if (createReflectionFailed) {
            return;
        }
        try {
            ensureCreateSoulPulseReflection();
            Object handler = createSoulPulseHandlerField.get(null);
            createSoulPulseRefreshMethod.invoke(handler);
            if (!loggedCreateSuppression) {
                loggedCreateSuppression = true;
                FacadeByPycoder.LOGGER.info("Create soul pulse handler suppression is active");
            }
        } catch (ClassNotFoundException exception) {
            createReflectionFailed = true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            createReflectionFailed = true;
            FacadeByPycoder.LOGGER.warn("Failed to suppress Create soul pulse effects near facade blocks", exception);
        }
    }

    private static void ensureCreateSoulPulseReflection() throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException {
        if (createSoulPulseHandlerField == null) {
            Class<?> createClientClass = Class.forName("com.simibubi.create.CreateClient");
            createSoulPulseHandlerField = createClientClass.getDeclaredField("SOUL_PULSE_EFFECT_HANDLER");
            createSoulPulseHandlerField.setAccessible(true);
        }
        if (createSoulPulseRefreshMethod == null) {
            createSoulPulseRefreshMethod = createSoulPulseHandlerField.getType().getDeclaredMethod("refresh");
            createSoulPulseRefreshMethod.setAccessible(true);
        }
    }

    private static void removeFacadeParticles(Queue<?> queue, Minecraft minecraft, List<BlockPos> facadePositions) {
        queue.removeIf(value -> value instanceof Particle particle && shouldSuppress(particle, minecraft, facadePositions));
    }

    private static boolean shouldSuppress(Particle particle, Minecraft minecraft, List<BlockPos> facadePositions) {
        Vec3 particlePos = particle.getPos();
        for (BlockPos facadePos : facadePositions) {
            if (particlePos.distanceToSqr(Vec3.atCenterOf(facadePos)) <= SUPPRESS_RADIUS_SQUARED) {
                particle.remove();
                return true;
            }
        }
        if (minecraft.player != null && particlePos.distanceToSqr(minecraft.player.position()) <= PLAYER_SUPPRESS_RADIUS_SQUARED) {
            particle.remove();
            return true;
        }
        return false;
    }
}
