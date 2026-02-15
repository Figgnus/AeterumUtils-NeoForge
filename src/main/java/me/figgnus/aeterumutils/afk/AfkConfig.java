package me.figgnus.aeterumutils.afk;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class AfkConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.IntValue AFK_TIMEOUT_SECONDS;
    private static final ModConfigSpec.IntValue KICK_WARNING_SECONDS;
    private static final ModConfigSpec.IntValue NUMISMATICS_REQUIRED_SPURS;
    private static final ModConfigSpec.IntValue AFK_PRICE_COOLDOWN_SECONDS;
    private static final ModConfigSpec.IntValue AFK_UPKEEP_SPURS;
    private static final ModConfigSpec.IntValue AFK_UPKEEP_INTERVAL_SECONDS;
    private static final ModConfigSpec.ConfigValue<String> MESSAGE_AFK_ON;
    private static final ModConfigSpec.ConfigValue<String> MESSAGE_AFK_OFF;
    private static final ModConfigSpec.ConfigValue<String> MESSAGE_AFK_OFF_MOVED;
    private static final ModConfigSpec.ConfigValue<String> MESSAGE_KICKED;
    private static final ModConfigSpec.ConfigValue<String> MESSAGE_PLAYERS_ONLY;
    private static final ModConfigSpec.ConfigValue<String> MESSAGE_KICK_COUNTDOWN;
    private static final ModConfigSpec.ConfigValue<String> MESSAGE_NOT_ENOUGH_BALANCE;
    private static final ModConfigSpec.ConfigValue<String> MESSAGE_BALANCE_UNAVAILABLE;
    private static final ModConfigSpec.ConfigValue<String> MESSAGE_AFK_CANCELED_NOT_ENOUGH_BALANCE;
    private static final ModConfigSpec.ConfigValue<String> MESSAGE_AFK_CANCELED_BALANCE_UNAVAILABLE;
    private static final ModConfigSpec.ConfigValue<String> MESSAGE_AFK_UPKEEP_CHARGED;
    private static final ModConfigSpec.ConfigValue<String> MESSAGE_AFK_PRICE_COOLDOWN_STARTED;
    private static final ModConfigSpec.ConfigValue<String> MESSAGE_AFK_PRICE_COOLDOWN_ACTIVE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("afk");
        AFK_TIMEOUT_SECONDS = builder
            .comment("Seconds of inactivity before a non-/afk player is disconnected.")
            .defineInRange("timeoutSeconds", 300, 1, Integer.MAX_VALUE);
        KICK_WARNING_SECONDS = builder
            .comment("How many seconds before disconnect to start showing countdown messages.")
            .defineInRange("warningWindowSeconds", 10, 1, Integer.MAX_VALUE);
        NUMISMATICS_REQUIRED_SPURS = builder
            .comment("Minimum Create: Numismatics balance (in spur) required to enable /afk.")
            .defineInRange("numismaticsRequiredSpurs", 50, 0, Integer.MAX_VALUE);
        AFK_PRICE_COOLDOWN_SECONDS = builder
            .comment("Cooldown for /afk activation cost. If > 0, re-enabling /afk during this cooldown does not charge again. Set to 0 to disable.")
            .defineInRange("afkPriceCooldownSeconds", 3600, 0, Integer.MAX_VALUE);
        AFK_UPKEEP_SPURS = builder
            .comment("Create: Numismatics spur cost charged while AFK after each upkeep interval. Set to 0 to disable upkeep charging.")
            .defineInRange("afkUpkeepSpurs", 50, 0, Integer.MAX_VALUE);
        AFK_UPKEEP_INTERVAL_SECONDS = builder
            .comment("Seconds between AFK upkeep charges.")
            .defineInRange("afkUpkeepIntervalSeconds", 3600, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.push("messages");
        MESSAGE_AFK_ON = builder
            .comment("Shown when player enables /afk.")
            .define("afkOn", "You are now AFK. You will not be kicked for inactivity.");
        MESSAGE_AFK_OFF = builder
            .comment("Shown when player disables /afk with the command.")
            .define("afkOff", "AFK disabled.");
        MESSAGE_AFK_OFF_MOVED = builder
            .comment("Shown when AFK mode is disabled by movement.")
            .define("afkOffMoved", "AFK disabled because you moved.");
        MESSAGE_KICKED = builder
            .comment("Disconnect reason shown to kicked players.")
            .define("kicked", "Kicked for being AFK too long. Use /afk if you need to stay connected.");
        MESSAGE_PLAYERS_ONLY = builder
            .comment("Shown when /afk is run by non-player command source.")
            .define("playersOnly", "Only players can use /afk.");
        MESSAGE_KICK_COUNTDOWN = builder
            .comment("Countdown text shown in action bar. Use {seconds} placeholder.")
            .define("kickCountdown", "AFK kick in {seconds} seconds.");
        MESSAGE_NOT_ENOUGH_BALANCE = builder
            .comment("Shown when player does not have enough Create: Numismatics balance. Supports {required} and {balance}.")
            .define("notEnoughBalance", "You need at least {required} spur to use /afk. Your balance: {balance} spur.");
        MESSAGE_BALANCE_UNAVAILABLE = builder
            .comment("Shown when Create: Numismatics balance cannot be read. Supports {required}.")
            .define("balanceUnavailable", "Cannot verify your Create: Numismatics balance right now. Required: {required} spur.");
        MESSAGE_AFK_CANCELED_NOT_ENOUGH_BALANCE = builder
            .comment("Shown when AFK upkeep payment fails due to low balance. Supports {required} and {balance}.")
            .define("afkCanceledNotEnoughBalance", "AFK protection canceled. You need {required} spur for upkeep, but your balance is {balance} spur.");
        MESSAGE_AFK_CANCELED_BALANCE_UNAVAILABLE = builder
            .comment("Shown when AFK upkeep payment fails because balance cannot be read. Supports {required}.")
            .define("afkCanceledBalanceUnavailable", "AFK protection canceled because balance verification failed. Required upkeep: {required} spur.");
        MESSAGE_AFK_UPKEEP_CHARGED = builder
            .comment("Shown whenever AFK upkeep is charged. Supports {amount} and {balance}.")
            .define("afkUpkeepCharged", "AFK upkeep charged: {amount} spur. New balance: {balance} spur.");
        MESSAGE_AFK_PRICE_COOLDOWN_STARTED = builder
            .comment("Shown when AFK activation price cooldown starts. Supports {remaining} and {seconds}.")
            .define("afkPriceCooldownStarted", "AFK price cooldown started: {remaining}.");
        MESSAGE_AFK_PRICE_COOLDOWN_ACTIVE = builder
            .comment("Shown whenever /afk is used while AFK activation price cooldown is active. Supports {remaining} and {seconds}.")
            .define("afkPriceCooldownActive", "AFK activation charge is on cooldown. No charge applied. Time left: {remaining}.");
        builder.pop();

        SPEC = builder.build();
    }

    private AfkConfig() {
    }

    public static long afkTimeoutSeconds() {
        return AFK_TIMEOUT_SECONDS.get();
    }

    public static long warningWindowSeconds() {
        return KICK_WARNING_SECONDS.get();
    }

    public static int numismaticsRequiredSpurs() {
        return NUMISMATICS_REQUIRED_SPURS.get();
    }

    public static long afkPriceCooldownSeconds() {
        return AFK_PRICE_COOLDOWN_SECONDS.get();
    }

    public static int afkUpkeepSpurs() {
        return AFK_UPKEEP_SPURS.get();
    }

    public static long afkUpkeepIntervalSeconds() {
        return AFK_UPKEEP_INTERVAL_SECONDS.get();
    }

    public static String messageAfkOn() {
        return MESSAGE_AFK_ON.get();
    }

    public static String messageAfkOff() {
        return MESSAGE_AFK_OFF.get();
    }

    public static String messageAfkOffMoved() {
        return MESSAGE_AFK_OFF_MOVED.get();
    }

    public static String messageKicked() {
        return MESSAGE_KICKED.get();
    }

    public static String messagePlayersOnly() {
        return MESSAGE_PLAYERS_ONLY.get();
    }

    public static String messageKickCountdown() {
        return MESSAGE_KICK_COUNTDOWN.get();
    }

    public static String messageNotEnoughBalance() {
        return MESSAGE_NOT_ENOUGH_BALANCE.get();
    }

    public static String messageBalanceUnavailable() {
        return MESSAGE_BALANCE_UNAVAILABLE.get();
    }

    public static String messageAfkCanceledNotEnoughBalance() {
        return MESSAGE_AFK_CANCELED_NOT_ENOUGH_BALANCE.get();
    }

    public static String messageAfkCanceledBalanceUnavailable() {
        return MESSAGE_AFK_CANCELED_BALANCE_UNAVAILABLE.get();
    }

    public static String messageAfkUpkeepCharged() {
        return MESSAGE_AFK_UPKEEP_CHARGED.get();
    }

    public static String messageAfkPriceCooldownStarted() {
        return MESSAGE_AFK_PRICE_COOLDOWN_STARTED.get();
    }

    public static String messageAfkPriceCooldownActive() {
        return MESSAGE_AFK_PRICE_COOLDOWN_ACTIVE.get();
    }
}
