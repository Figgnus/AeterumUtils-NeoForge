package me.figgnus.aeterumutils.afk;

import me.figgnus.aeterumutils.Aeterumutils;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.commands.Commands.literal;

public final class AfkHandler {
    private static final double MOVEMENT_EPSILON_SQR = 0.0001D;
    private static final float LOOK_EPSILON_DEGREES = 0.2F;
    private static final Map<UUID, AfkState> AFK_PLAYERS = new HashMap<>();
    private static final Map<UUID, Long> NEXT_AFK_UPKEEP_MILLIS = new HashMap<>();
    private static final Map<UUID, Long> LAST_COUNTDOWN_SECONDS = new HashMap<>();
    private static final Map<UUID, ActivityState> LAST_KNOWN_STATES = new HashMap<>();
    private static final Map<UUID, Long> LAST_ACTIVITY_MILLIS = new HashMap<>();

    private AfkHandler() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            literal("afk")
                .requires(source -> source.hasPermission(0))
                .executes(context -> toggleAfk(context.getSource()))
        );
        event.getDispatcher().register(
            literal(Aeterumutils.MODID)
                .requires(source -> source.hasPermission(2))
                .then(literal("reload")
                    .executes(context -> reloadConfig(context.getSource())))
        );
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        boolean shouldCheckInactivity = event.getServer().getTickCount() % 20 == 0;
        long now = Util.getMillis();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            ActivityState currentState = new ActivityState(player.position(), player.getYRot(), player.getXRot());
            updatePlayerActivity(playerId, currentState, now);
            AfkState afkState = AFK_PLAYERS.get(playerId);

            if (afkState != null) {
                if (hasMovedOrLooked(currentState, afkState)) {
                    AFK_PLAYERS.remove(playerId);
                    NEXT_AFK_UPKEEP_MILLIS.remove(playerId);
                    player.resetLastActionTime();
                    LAST_ACTIVITY_MILLIS.put(playerId, now);
                    player.sendSystemMessage(Component.literal(AfkConfig.messageAfkOffMoved()));
                } else {
                    processAfkUpkeep(player, playerId, now);
                }

                if (!AFK_PLAYERS.containsKey(playerId)) {
                    NEXT_AFK_UPKEEP_MILLIS.remove(playerId);
                }
                continue;
            }

            if (!shouldCheckInactivity) {
                continue;
            }

            long activityMillis = LAST_ACTIVITY_MILLIS.getOrDefault(playerId, now);
            long lastActionMillis = Math.max(player.getLastActionTime(), activityMillis);
            long afkTimeoutMillis = Math.max(1L, AfkConfig.afkTimeoutSeconds()) * 1000L;
            long idleMillis = now - lastActionMillis;
            long remainingMillis = afkTimeoutMillis - idleMillis;

            if (remainingMillis > 0L) {
                maybeSendCountdown(player, playerId, remainingMillis);
                continue;
            }

            LAST_COUNTDOWN_SECONDS.remove(playerId);
            player.connection.disconnect(Component.literal(AfkConfig.messageKicked()));
        }
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        AFK_PLAYERS.remove(playerId);
        NEXT_AFK_UPKEEP_MILLIS.remove(playerId);
        LAST_COUNTDOWN_SECONDS.remove(playerId);
        LAST_KNOWN_STATES.remove(playerId);
        LAST_ACTIVITY_MILLIS.remove(playerId);
    }

    private static int toggleAfk(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal(AfkConfig.messagePlayersOnly()));
            return 0;
        }

        UUID playerId = player.getUUID();
        if (AFK_PLAYERS.containsKey(playerId)) {
            AFK_PLAYERS.remove(playerId);
            NEXT_AFK_UPKEEP_MILLIS.remove(playerId);
            player.resetLastActionTime();
            long now = Util.getMillis();
            LAST_ACTIVITY_MILLIS.put(playerId, now);
            LAST_KNOWN_STATES.put(playerId, new ActivityState(player.position(), player.getYRot(), player.getXRot()));
            LAST_COUNTDOWN_SECONDS.remove(playerId);
            source.sendSuccess(() -> Component.literal(AfkConfig.messageAfkOff()), false);
            return 1;
        }

        int requiredSpurs = Math.max(0, AfkConfig.numismaticsRequiredSpurs());
        if (requiredSpurs > 0) {
            int balanceSpurs = NumismaticsCompat.getPlayerBalanceSpurs(player);
            if (balanceSpurs < 0) {
                source.sendFailure(Component.literal(formatBalanceMessage(AfkConfig.messageBalanceUnavailable(), requiredSpurs, "N/A")));
                return 0;
            }

            if (balanceSpurs < requiredSpurs) {
                source.sendFailure(Component.literal(formatBalanceMessage(AfkConfig.messageNotEnoughBalance(), requiredSpurs, Integer.toString(balanceSpurs))));
                return 0;
            }

            if (!NumismaticsCompat.deductPlayerSpurs(player, requiredSpurs)) {
                int latestBalance = NumismaticsCompat.getPlayerBalanceSpurs(player);
                if (latestBalance >= 0 && latestBalance < requiredSpurs) {
                    source.sendFailure(Component.literal(formatBalanceMessage(AfkConfig.messageNotEnoughBalance(), requiredSpurs, Integer.toString(latestBalance))));
                } else {
                    source.sendFailure(Component.literal(formatBalanceMessage(AfkConfig.messageBalanceUnavailable(), requiredSpurs, "N/A")));
                }
                return 0;
            }
        }

        AFK_PLAYERS.put(playerId, new AfkState(player.position(), player.getYRot(), player.getXRot()));
        NEXT_AFK_UPKEEP_MILLIS.put(playerId, getNextUpkeepDueMillis(Util.getMillis()));
        LAST_KNOWN_STATES.put(playerId, new ActivityState(player.position(), player.getYRot(), player.getXRot()));
        LAST_COUNTDOWN_SECONDS.remove(playerId);
        source.sendSuccess(() -> Component.literal(AfkConfig.messageAfkOn()), false);
        return 1;
    }

    private static int reloadConfig(CommandSourceStack source) {
        try {
            ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.SERVER, FMLPaths.CONFIGDIR.get());
            source.sendSuccess(() -> Component.literal("AeterumUtils server config reloaded."), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to reload AeterumUtils config: " + e.getMessage()));
            return 0;
        }
    }

    private static void maybeSendCountdown(ServerPlayer player, UUID playerId, long remainingMillis) {
        long remainingSeconds = (remainingMillis + 999L) / 1000L;
        long afkTimeoutSeconds = Math.max(1L, AfkConfig.afkTimeoutSeconds());
        long warningWindowSeconds = Math.min(Math.max(1L, AfkConfig.warningWindowSeconds()), afkTimeoutSeconds);
        if (remainingSeconds > warningWindowSeconds) {
            LAST_COUNTDOWN_SECONDS.remove(playerId);
            return;
        }

        Long lastSentSecond = LAST_COUNTDOWN_SECONDS.get(playerId);
        if (lastSentSecond != null && lastSentSecond == remainingSeconds) {
            return;
        }

        String countdownMessage = AfkConfig.messageKickCountdown().replace("{seconds}", Long.toString(remainingSeconds));
        player.displayClientMessage(Component.literal(countdownMessage), true);
        LAST_COUNTDOWN_SECONDS.put(playerId, remainingSeconds);
    }

    private static void updatePlayerActivity(UUID playerId, ActivityState currentState, long now) {
        ActivityState previousState = LAST_KNOWN_STATES.put(playerId, currentState);
        if (previousState == null || hasMovedOrLooked(currentState, previousState)) {
            LAST_ACTIVITY_MILLIS.put(playerId, now);
            LAST_COUNTDOWN_SECONDS.remove(playerId);
        }
    }

    private static boolean hasMovedOrLooked(ActivityState currentState, ActivityState previousState) {
        return currentState.position.distanceToSqr(previousState.position) > MOVEMENT_EPSILON_SQR
            || Math.abs(Mth.wrapDegrees(currentState.yRot - previousState.yRot)) > LOOK_EPSILON_DEGREES
            || Math.abs(currentState.xRot - previousState.xRot) > LOOK_EPSILON_DEGREES;
    }

    private static boolean hasMovedOrLooked(ActivityState currentState, AfkState afkState) {
        return currentState.position.distanceToSqr(afkState.position) > MOVEMENT_EPSILON_SQR
            || Math.abs(Mth.wrapDegrees(currentState.yRot - afkState.yRot)) > LOOK_EPSILON_DEGREES
            || Math.abs(currentState.xRot - afkState.xRot) > LOOK_EPSILON_DEGREES;
    }

    private static String formatBalanceMessage(String template, int requiredSpurs, String balanceSpurs) {
        return template
            .replace("{required}", Integer.toString(requiredSpurs))
            .replace("{balance}", balanceSpurs);
    }

    private static void processAfkUpkeep(ServerPlayer player, UUID playerId, long now) {
        int upkeepSpurs = Math.max(0, AfkConfig.afkUpkeepSpurs());
        if (upkeepSpurs <= 0) {
            NEXT_AFK_UPKEEP_MILLIS.remove(playerId);
            return;
        }

        long upkeepIntervalMillis = Math.max(1L, AfkConfig.afkUpkeepIntervalSeconds()) * 1000L;
        long dueMillis = NEXT_AFK_UPKEEP_MILLIS.getOrDefault(playerId, now + upkeepIntervalMillis);
        if (now < dueMillis) {
            return;
        }

        int balanceSpurs = NumismaticsCompat.getPlayerBalanceSpurs(player);
        if (balanceSpurs < 0) {
            AFK_PLAYERS.remove(playerId);
            player.sendSystemMessage(Component.literal(formatBalanceMessage(AfkConfig.messageAfkCanceledBalanceUnavailable(), upkeepSpurs, "N/A")));
            return;
        }

        if (balanceSpurs < upkeepSpurs || !NumismaticsCompat.deductPlayerSpurs(player, upkeepSpurs)) {
            int latestBalance = NumismaticsCompat.getPlayerBalanceSpurs(player);
            String shownBalance = latestBalance < 0 ? "N/A" : Integer.toString(latestBalance);
            AFK_PLAYERS.remove(playerId);
            player.sendSystemMessage(Component.literal(formatBalanceMessage(AfkConfig.messageAfkCanceledNotEnoughBalance(), upkeepSpurs, shownBalance)));
            return;
        }

        int balanceAfterCharge = NumismaticsCompat.getPlayerBalanceSpurs(player);
        String shownBalanceAfterCharge = balanceAfterCharge < 0 ? "N/A" : Integer.toString(balanceAfterCharge);
        player.sendSystemMessage(Component.literal(formatUpkeepChargedMessage(AfkConfig.messageAfkUpkeepCharged(), upkeepSpurs, shownBalanceAfterCharge)));
        NEXT_AFK_UPKEEP_MILLIS.put(playerId, now + upkeepIntervalMillis);
    }

    private static long getNextUpkeepDueMillis(long now) {
        long upkeepSpurs = Math.max(0, AfkConfig.afkUpkeepSpurs());
        if (upkeepSpurs <= 0) {
            return Long.MAX_VALUE;
        }

        long upkeepIntervalMillis = Math.max(1L, AfkConfig.afkUpkeepIntervalSeconds()) * 1000L;
        return now + upkeepIntervalMillis;
    }

    private static String formatUpkeepChargedMessage(String template, int amountSpurs, String balanceSpurs) {
        return template
            .replace("{amount}", Integer.toString(amountSpurs))
            .replace("{balance}", balanceSpurs);
    }

    private record ActivityState(Vec3 position, float yRot, float xRot) {
    }

    private record AfkState(Vec3 position, float yRot, float xRot) {
    }
}
