package pers.neige.neigeitems

import bot.inker.bukkit.nbt.NbtItemStack
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import pers.neige.neigeitems.lang.LocaleI18n
import pers.neige.neigeitems.manager.ConfigManager
import pers.neige.neigeitems.scanner.ClassScanner
import taboolib.common.platform.Plugin
import taboolib.platform.BukkitPlugin

/**
 * 插件主类
 */
object NeigeItems : Plugin() {
    val plugin by lazy { BukkitPlugin.getInstance() }

    private var scanner: ClassScanner? = null

    override fun onEnable() {
        try {
            val itemStack = ItemStack(Material.STONE)
            val nbtItemStack = NbtItemStack(itemStack)
            val nbt = nbtItemStack.orCreateTag
            nbt.putString("test", "test")
            nbt.getString("test")
        } catch (error: Throwable) {
            plugin.logger.warning("插件NBT前置库未正常加载依赖, 本插件不支持包括但不限于 Mohist/Catserver/Arclight 等混合服务端, 对于每个大版本, 本插件仅支持最新小版本, 如支持 1.19.4 但不支持 1.19.2, 请选用正确的服务端, 或卸载本插件")
            plugin.logger.warning("The plugin's NBT pre-requisite library failed to load. This plugin does not support mixed server platforms including but not limited to Mohist/Catserver/Arclight, etc. For each major version, this plugin only supports the latest minor version. For example, it supports 1.19.4 but not 1.19.2. Please use the correct server platform or uninstall this plugin.")
            val pluginManager = Bukkit.getPluginManager()
            pluginManager.getPlugin("NeigeItems")?.let {
                pluginManager.disablePlugin(it)
            }
            return
        }

        ConfigManager.saveResource()
        LocaleI18n.init()
        scanner = ClassScanner(
            plugin,
            "pers.neige.neigeitems",
            mutableSetOf(
                "pers.neige.neigeitems.libs",
                "pers.neige.neigeitems.taboolib"
            )
        )
        scanner!!.onEnable()
    }

    override fun onDisable() {
        scanner!!.onDisable()
    }
}