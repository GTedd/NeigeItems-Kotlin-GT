package pers.neige.neigeitems.item.lore;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.neige.neigeitems.action.ActionContext;
import pers.neige.neigeitems.item.lore.impl.ConditionLore;
import pers.neige.neigeitems.item.lore.impl.ListLore;
import pers.neige.neigeitems.item.lore.impl.NullLore;
import pers.neige.neigeitems.item.lore.impl.StringLore;
import pers.neige.neigeitems.libs.bot.inker.bukkit.nbt.Nbt;
import pers.neige.neigeitems.libs.bot.inker.bukkit.nbt.NbtList;
import pers.neige.neigeitems.libs.bot.inker.bukkit.nbt.NbtString;
import pers.neige.neigeitems.libs.bot.inker.bukkit.nbt.internal.annotation.CbVersion;
import pers.neige.neigeitems.libs.bot.inker.bukkit.nbt.neigeitems.utils.TranslationUtils;
import pers.neige.neigeitems.manager.ActionManager;
import pers.neige.neigeitems.manager.BaseActionManager;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface Lore {
    Function<String, String> converterNull = String::valueOf;
    Function<String, Nbt<?>> converterV12 = NbtString::valueOf;
    Function<String, Nbt<?>> converterV16ToV20 = (text) -> NbtString.valueOf(TranslationUtils.fromStringToJSON(text));

    static Lore compile(
            @NotNull BaseActionManager manager,
            @Nullable Object action
    ) {
        if (action instanceof String) {
            return new StringLore((String) action);
        } else if (action instanceof List<?>) {
            return new ListLore(manager, (List<?>) action);
        } else if (action instanceof Map<?, ?>) {
            return new ConditionLore(manager, (Map<?, ?>) action);
        } else if (action instanceof ConfigurationSection) {
            return new ConditionLore(manager, (ConfigurationSection) action);
        }
        return NullLore.INSTANCE;
    }

    @NotNull
    default <T, R extends List<T>> R getLore(@NotNull R result, @NotNull BaseActionManager manager, @NotNull ActionContext context, Function<String, T> converter) {
        return result;
    }

    @NotNull
    default NbtList getLoreNbt(@NotNull BaseActionManager manager, @NotNull ActionContext context) {
        if (CbVersion.v1_20_R4.isSupport()) {
            throw new InvalidParameterException("1.20.5+版本, 你拿你妈了个逼的NBT形式Lore啊? 你Lore是NBT形式吗你就拿?");
        } else if (CbVersion.current() == CbVersion.v1_12_R1) {
            return getLore(new NbtList(), manager, context, converterV12);
        } else if (CbVersion.v1_16_R3.isSupport()) {
            return getLore(new NbtList(), manager, context, converterV16ToV20);
        } else {
            throw new InvalidParameterException("不支持1.13-1.16.4版本");
        }
    }
}
