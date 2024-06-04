package pers.neige.neigeitems.item.builder;

import kotlin.text.StringsKt;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.neige.neigeitems.libs.bot.inker.bukkit.nbt.*;
import pers.neige.neigeitems.libs.bot.inker.bukkit.nbt.internal.annotation.CbVersion;
import pers.neige.neigeitems.libs.bot.inker.bukkit.nbt.neigeitems.utils.EnchantmentUtils;
import pers.neige.neigeitems.libs.bot.inker.bukkit.nbt.neigeitems.utils.TranslationUtils;
import pers.neige.neigeitems.utils.ItemUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class ItemBuilder {
    public static final ItemStack air = new ItemStack(Material.AIR);
    public static final Map<String, Enchantment> byFieldName = new HashMap<>();

    static {
        Field[] fields = Enchantment.class.getDeclaredFields();
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (!Enchantment.class.isAssignableFrom(field.getType())) continue;
            try {
                byFieldName.put(field.getName().toUpperCase(), (Enchantment) field.get(null));
            } catch (IllegalAccessException exception) {
                exception.printStackTrace();
            }
        }
    }

    @Nullable
    private Material material = null;
    @Nullable
    private ItemStack itemStack = null;
    @Nullable
    private Short damage = null;
    @NotNull
    private Map<Enchantment, Short> enchantments = new HashMap<>();
    @NotNull
    private Map<Short, Short> enchantmentIdToLevel = new HashMap<>();
    @NotNull
    private Map<String, Short> enchantmentKeyToLevel = new HashMap<>();
    @Nullable
    private Integer customModelData = null;
    @Nullable
    private NbtString name = null;
    @Nullable
    private NbtList lore = null;
    @Nullable
    private Integer color = null;
    @Nullable
    private Boolean unbreakable = null;
    @Nullable
    private Integer hideFlag = null;
    @Nullable
    private NbtCompound coverNbt = null;
    private boolean hasBuild = false;
    @Nullable
    private BiConsumer<ItemStack, NbtCompound> preCover = null;

    public ItemBuilder() {
    }

    public ItemBuilder(
            @Nullable Material material
    ) {
        this.material = material;
    }

    public ItemBuilder(
            @Nullable ItemStack itemStack
    ) {
        this.itemStack = itemStack == null ? null : NbtUtils.asCraftCopy(itemStack);
    }

    public ItemBuilder(
            @Nullable ConfigurationSection config
    ) {
        load(config);
    }

    public void load(
            @Nullable ConfigurationSection config
    ) {
        if (config == null) return;
        for (String key : config.getKeys(false)) {
            switch (key.toLowerCase()) {
                case "type":
                case "material": {
                    String materialString = config.getString(key);
                    if (materialString != null) {
                        this.material = Material.matchMaterial(materialString.toUpperCase());
                    }
                    break;
                }
                case "damage": {
                    this.damage = (short) config.getInt(key);
                    break;
                }
                case "enchantments": {
                    ConfigurationSection enchantSection = config.getConfigurationSection(key);
                    if (enchantSection != null) {
                        for (String enchantId : enchantSection.getKeys(false)) {
                            String uppercaseEnchantId = enchantId.toUpperCase();
                            short level = (short) enchantSection.getInt(enchantId);
                            Enchantment enchant = Enchantment.getByName(uppercaseEnchantId);
                            if (enchant == null) {
                                enchant = this.byFieldName.get(uppercaseEnchantId);
                            }
                            if (level > 0 && enchant != null) {
                                this.enchantments.put(enchant, level);
                                if (CbVersion.v1_13_R1.isSupport()) {
                                    this.enchantmentKeyToLevel.put(enchant.getKey().toString(), level);
                                } else {
                                    this.enchantmentIdToLevel.put((short) EnchantmentUtils.getId(enchant), level);
                                }
                            }
                        }
                    }
                    break;
                }
                case "custommodeldata":
                case "custom-model-data": {
                    this.customModelData = config.getInt(key);
                    break;
                }
                case "name": {
                    String rawName = config.getString(key);
                    if (rawName != null) {
                        this.name = NbtString.valueOf(TranslationUtils.toJsonText(ChatColor.translateAlternateColorCodes('&', rawName)));
                    }
                    break;
                }
                case "lore": {
                    List<String> originLore = config.getStringList(key);
                    NbtList finalLore = new NbtList();
                    for (String rawLore : originLore) {
                        for (String loreText : ChatColor.translateAlternateColorCodes('&', rawLore).split("\n")) {
                            finalLore.add(NbtString.valueOf(TranslationUtils.toJsonText(loreText)));
                        }
                    }
                    if (!finalLore.isEmpty()) {
                        this.lore = finalLore;
                    }
                    break;
                }
                case "color": {
                    Object value = config.get(key);
                    if (value instanceof String) {
                        color = StringsKt.toIntOrNull((String) value, 16);
                    } else if (value instanceof Integer) {
                        color = Math.min(0xFFFFFF, Math.max(0, (int) value));
                    }
                    break;
                }
                case "unbreakable": {
                    this.unbreakable = config.getBoolean(key);
                    break;
                }
                case "item-flags":
                case "itemflags":
                case "hide-flags":
                case "hideflags": {
                    List<String> flags = config.getStringList(key);
                    if (!flags.isEmpty()) {
                        this.hideFlag = 0;
                        for (String flagText : flags) {
                            ItemFlag flag = ItemFlag.valueOf(flagText);
                            this.hideFlag |= (1 << flag.ordinal());
                        }
                    }
                    break;
                }
                case "nbt": {
                    ConfigurationSection nbtConfig = config.getConfigurationSection(key);
                    if (nbtConfig != null) {
                        this.coverNbt = ItemUtils.toNbtCompound(nbtConfig);
                    }
                    break;
                }
            }
        }
    }

    /**
     * 根据已有内容构建并返回物品.
     * 返回前会将当前ItemBuilder实例的itemStack字段设置为返回值.
     * 构建过程中可能会对已有内容进行修改, 比如清空所有Enchantment.
     * 构建结果永远不为null, 构建失败将返回AIR.
     * 禁止多次调用build方法, 为防止出现问题, 此时将抛出异常.
     *
     * @return 构建产物.
     */
    @NotNull
    public ItemStack build() {
        if (hasBuild) {
            throw new UnsupportedOperationException("Already build");
        }
        this.hasBuild = true;
        if (this.material == Material.AIR) {
            this.itemStack = air;
            return air;
        }
        ItemStack result;
        if (this.itemStack != null) {
            result = this.itemStack;
            if (this.material != null) {
                result.setType(this.material);
            }
        } else {
            if (this.material != null) {
                result = NbtUtils.asCraftCopy(new ItemStack(this.material));
            } else {
                this.itemStack = air;
                return air;
            }
        }
        if (result.getType() == Material.AIR) {
            this.itemStack = air;
            return air;
        }
        if (CbVersion.v1_20_R3.isUpTo()) {
            NbtItemStack nbtItemStack = new NbtItemStack(result);
            NbtCompound nbt = nbtItemStack.getOrCreateTag();
            if (this.damage != null) {
                if (CbVersion.v1_13_R1.isSupport()) {
                    nbt.putInt(NbtUtils.getDamageNbtKeyOrThrow(), damage);
                } else {
                    result.setDurability(damage);
                }
            }
            if (!this.enchantments.isEmpty()) {
                if (CbVersion.v1_13_R1.isSupport()) {
                    NbtList list = nbt.getOrCreateList(NbtUtils.getEnchantmentsNbtKey());

                    for (Nbt<?> tag : list) {
                        if (!(tag instanceof NbtCompound)) continue;
                        NbtCompound compound = (NbtCompound) tag;
                        String id = compound.getString(NbtUtils.getEnchantmentIdNbtKey());
                        Short lvl = enchantmentKeyToLevel.get(id);
                        if (lvl != null) {
                            compound.putShort(NbtUtils.getEnchantmentLvlNbtKey(), lvl);
                            enchantmentKeyToLevel.remove(id);
                        }
                    }

                    enchantmentKeyToLevel.forEach((id, lvl) -> {
                        NbtCompound tag = new NbtCompound();
                        tag.putString(NbtUtils.getEnchantmentIdNbtKey(), id);
                        tag.putShort(NbtUtils.getEnchantmentLvlNbtKey(), lvl);
                        list.add(tag);
                    });
                } else {
                    NbtList list = nbt.getOrCreateList(NbtUtils.getEnchantmentsNbtKey());

                    for (Nbt<?> tag : list) {
                        if (!(tag instanceof NbtCompound)) continue;
                        NbtCompound compound = (NbtCompound) tag;
                        short id = compound.getShort(NbtUtils.getEnchantmentIdNbtKey());
                        Short lvl = enchantmentIdToLevel.get(id);
                        if (lvl != null) {
                            compound.putShort(NbtUtils.getEnchantmentLvlNbtKey(), lvl);
                            enchantmentIdToLevel.remove(id);
                        }
                    }
                    enchantmentIdToLevel.forEach((id, lvl) -> {
                        NbtCompound tag = new NbtCompound();
                        tag.putShort(NbtUtils.getEnchantmentIdNbtKey(), id);
                        tag.putShort(NbtUtils.getEnchantmentLvlNbtKey(), lvl);
                        list.add(tag);
                    });
                }
            }
            if (customModelData != null) {
                String nbtKey = NbtUtils.getCustomModelDataNbtKeyOrNull();
                if (nbtKey != null) {
                    nbt.putInt(nbtKey, customModelData);
                }
            }
            if (name != null || lore != null || color != null) {
                NbtCompound display = nbt.getOrCreateCompound(NbtUtils.getDisplayNbtKey());
                if (name != null) {
                    display.put(NbtUtils.getNameNbtKey(), name);
                }
                if (lore != null) {
                    display.put(NbtUtils.getLoreNbtKey(), lore);
                }
                if (color != null) {
                    display.putInt("color", color);
                }
            }
            if (unbreakable != null) {
                nbt.putBoolean(NbtUtils.getUnbreakableNbtKey(), unbreakable);
            }
            if (hideFlag != null) {
                nbt.putInt(NbtUtils.getHideFlagsNbtKey(), hideFlag);
            }
            if (preCover != null) {
                preCover.accept(result, nbt);
            }
            if (coverNbt != null) {
                nbt.coverWith(coverNbt);
            }
            this.itemStack = result;
        }
        return result;
    }

    @Nullable
    public Material getType() {
        return material;
    }

    public void setType(@Nullable Material material) {
        this.material = material;
    }

    @Nullable
    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemStack(@Nullable ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Nullable
    public Short getDamage() {
        return damage;
    }

    public void setDamage(short damage) {
        this.damage = damage;
    }

    @NotNull
    public Map<Enchantment, Short> getEnchantments() {
        return enchantments;
    }

    public void setEnchantments(@NotNull Map<Enchantment, Short> enchantments) {
        this.enchantments = enchantments;
    }

    @Nullable
    public Integer getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }

    public boolean getUnbreakable() {
        return unbreakable != null && unbreakable;
    }

    public void setUnbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
    }

    @Nullable
    public Integer getHideFlag() {
        return hideFlag;
    }

    public void setHideFlag(@Nullable Integer hideFlag) {
        this.hideFlag = hideFlag;
    }

    @Nullable
    public NbtCompound getCoverNbt() {
        return coverNbt;
    }

    public void setCoverNbt(@Nullable NbtCompound coverNbt) {
        this.coverNbt = coverNbt;
    }

    @Nullable
    public BiConsumer<ItemStack, NbtCompound> getPreCover() {
        return preCover;
    }

    public void setPreCover(@Nullable BiConsumer<ItemStack, NbtCompound> preCover) {
        this.preCover = preCover;
    }
}
