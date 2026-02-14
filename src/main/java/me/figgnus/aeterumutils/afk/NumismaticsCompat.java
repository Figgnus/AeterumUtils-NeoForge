package me.figgnus.aeterumutils.afk;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class NumismaticsCompat {
    private static final String MOD_ID = "numismatics";
    private static final String NUMISMATICS_CLASS = "dev.ithundxr.createnumismatics.Numismatics";
    private static final String BANK_ACCOUNT_CLASS = "dev.ithundxr.createnumismatics.content.backend.BankAccount";

    private static boolean initialized;
    private static boolean available;
    private static Object bankManager;
    private static Method getAccountMethod;
    private static Method getBalanceMethod;
    private static Method deductMethod;

    private NumismaticsCompat() {
    }

    public static boolean isModLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static int getPlayerBalanceSpurs(ServerPlayer player) {
        if (!isModLoaded() || !ensureInitialized()) {
            return -1;
        }

        try {
            Object account = getAccountMethod.invoke(bankManager, player);
            if (account == null) {
                return 0;
            }

            Object rawBalance = getBalanceMethod.invoke(account);
            if (rawBalance instanceof Number number) {
                return number.intValue();
            }
        } catch (Exception ignored) {
            return -1;
        }

        return -1;
    }

    public static boolean deductPlayerSpurs(ServerPlayer player, int amount) {
        if (amount < 0 || !isModLoaded() || !ensureInitialized()) {
            return false;
        }

        try {
            Object account = getAccountMethod.invoke(bankManager, player);
            if (account == null) {
                return false;
            }

            Object result = deductMethod.invoke(account, amount);
            return result instanceof Boolean deducted && deducted;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean ensureInitialized() {
        if (initialized) {
            return available;
        }

        initialized = true;
        try {
            Class<?> numismaticsClass = Class.forName(NUMISMATICS_CLASS);
            Field bankField = numismaticsClass.getField("BANK");
            bankManager = bankField.get(null);
            getAccountMethod = bankManager.getClass().getMethod("getAccount", Player.class);

            Class<?> bankAccountClass = Class.forName(BANK_ACCOUNT_CLASS);
            getBalanceMethod = bankAccountClass.getMethod("getBalance");
            deductMethod = bankAccountClass.getMethod("deduct", int.class);
            available = true;
        } catch (Exception ignored) {
            available = false;
        }

        return available;
    }
}
