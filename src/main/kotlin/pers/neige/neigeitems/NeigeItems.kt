package pers.neige.neigeitems

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.inksnow.ankhinvoke.bukkit.AnkhInvokeBukkit
import org.inksnow.ankhinvoke.bukkit.injector.JarTransformInjector
import pers.neige.neigeitems.lang.LocaleI18n
import pers.neige.neigeitems.libs.bot.inker.bukkit.nbt.NbtItemStack
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
            val nbtItemStack =
                NbtItemStack(itemStack)
            val nbt = nbtItemStack.orCreateTag
            nbt.putString("test", "test")
            nbt.getString("test")
        } catch (error: Throwable) {
            plugin.logger.warning("插件NBT前置库未正常加载依赖, 本插件不支持包括但不限于 Mohist/Catserver/Arclight 等混合服务端, 对于每个大版本, 本插件仅支持最新小版本, 如支持 1.19.4 但不支持 1.19.2, 请选用正确的服务端, 或卸载本插件")
            plugin.logger.warning("The plugin's NBT pre-requisite library failed to load. This plugin does not support mixed server platforms including but not limited to Mohist/Catserver/Arclight, etc. For each major version, this plugin only supports the latest minor version. For example, it supports 1.19.4 but not 1.19.2. Please use the correct server platform or uninstall this plugin.")
            error.printStackTrace()
            val pluginManager = Bukkit.getPluginManager()
            pluginManager.getPlugin("NeigeItems")?.let {
                pluginManager.disablePlugin(it)
            }
            return
        }

        var safe = true

        if (!checkMagicUtils("DamageEventUtils")) safe = false
        if (!checkMagicUtils("EnchantmentUtils")) safe = false
        if (!checkMagicUtils("EntityItemUtils")) safe = false
        if (!checkMagicUtils("EntityPlayerUtils")) safe = false
        if (!checkMagicUtils("EntityUtils")) safe = false
        if (!checkMagicUtils("InventoryUtils")) safe = false
        if (!checkMagicUtils("SpigotInventoryUtils")) safe = false
        if (!checkMagicUtils("PaperInventoryUtils")) safe = false
        if (!checkMagicUtils("ServerUtils")) safe = false
        if (!checkMagicUtils("TranslationUtils")) safe = false
        if (!checkMagicUtils("WorldUtils")) safe = false

        if (!safe) {
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

    private fun checkMagicUtils(className: String): Boolean {
        try {
            Class.forName("pers.neige.neigeitems.libs.bot.inker.bukkit.nbt.neigeitems.utils.$className")
            return true
        } catch (error: Throwable) {
            plugin.logger.warning("$className 类未正常加载, 这可能造成不可预知的错误, 请联系作者修复")
            plugin.logger.warning("class $className did not load properly, which may cause unpredictable errors. Please contact the author for repair.")
            error.printStackTrace()
            return false
        }
    }

    override fun onDisable() {
        scanner!!.onDisable()
    }

    @JvmStatic
    fun init() {
        try {
            println("[NeigeItems] loading ankh-invoke")
            val bukkitPluginClass = Class.forName("pers.neige.neigeitems.taboolib.platform.BukkitPlugin")
                .asSubclass(JavaPlugin::class.java)
            AnkhInvokeBukkit.forBukkit(bukkitPluginClass)
                .reference()
                .appendPackage("pers.neige.neigeitems.ref")
                .build()
                .inject()
                .injector(
                    JarTransformInjector.Builder()
                        .classLoader(bukkitPluginClass.classLoader)
                        .transformPackage("pers.neige.neigeitems.libs.bot.inker.bukkit.nbt.")
                        .build()
                )
                .build()
                .referenceRemap()
                .setApplyMapRegistry("neigeitems")
                .build()
                .build()
            println("[NeigeItems] ankh-invoke loaded")
        } catch (e: ClassNotFoundException) {
            println("[NeigeItems] failed to load ankh-invoke")
            e.printStackTrace()
        }
    }
}