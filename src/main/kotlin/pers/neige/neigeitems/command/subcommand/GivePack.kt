package pers.neige.neigeitems.command.subcommand

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import pers.neige.neigeitems.command.subcommand.Help.help
import pers.neige.neigeitems.event.ItemPackGiveEvent
import pers.neige.neigeitems.manager.ConfigManager
import pers.neige.neigeitems.manager.HookerManager.getParsedName
import pers.neige.neigeitems.manager.ItemPackManager
import pers.neige.neigeitems.utils.ItemUtils.getNbtOrNull
import pers.neige.neigeitems.utils.ItemUtils.saveToSafe
import pers.neige.neigeitems.utils.LangUtils.getLang
import pers.neige.neigeitems.utils.LangUtils.sendLang
import pers.neige.neigeitems.utils.PlayerUtils.giveItem
import pers.neige.neigeitems.utils.SchedulerUtils.async
import pers.neige.neigeitems.utils.SchedulerUtils.sync
import taboolib.common.platform.command.subCommand

object GivePack {
    // ni givePack [玩家ID] [物品包ID] (数量) (指向数据) > 根据ID给予NI物品包
    val givePack = subCommand {
        execute<CommandSender> { sender, _, _ ->
            async {
                help(sender)
            }
        }
        dynamic {
            suggestion<CommandSender>(uncheck = true) { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            execute<CommandSender> { sender, _, _ ->
                async {
                    help(sender)
                }
            }
            // ni givePack [玩家ID] [物品包ID]
            dynamic {
                suggestion<CommandSender>(uncheck = true) { _, _ ->
                    ItemPackManager.itemPackIds
                }
                execute<CommandSender> { sender, context, argument ->
                    givePackCommandAsync(sender, context.argument(-1), argument, "1")
                }
                // ni givePack [玩家ID] [物品包ID] (数量)
                dynamic(optional = true) {
                    suggestion<CommandSender>(uncheck = true) { _, _ ->
                        arrayListOf("amount")
                    }
                    execute<CommandSender> { sender, context, argument ->
                        givePackCommandAsync(sender, context.argument(-2), context.argument(-1), argument)
                    }
                    // ni givePack [玩家ID] [物品包ID] (数量) (指向数据)
                    dynamic(optional = true) {
                        suggestion<CommandSender>(uncheck = true) { _, _ ->
                            arrayListOf("data")
                        }
                        execute<CommandSender> { sender, context, argument ->
                            givePackCommandAsync(
                                sender,
                                context.argument(-3),
                                context.argument(-2),
                                context.argument(-1),
                                argument
                            )
                        }
                    }
                }
            }
        }
    }

    // ni givePack [玩家ID] [物品包ID] (数量) (指向数据) > 根据ID给予NI物品包
    val givePackSilent = subCommand {
        execute<CommandSender> { sender, _, _ ->
            async {
                help(sender)
            }
        }
        dynamic {
            suggestion<CommandSender>(uncheck = true) { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            execute<CommandSender> { sender, _, _ ->
                async {
                    help(sender)
                }
            }
            // ni givePack [玩家ID] [物品包ID]
            dynamic {
                suggestion<CommandSender>(uncheck = true) { _, _ ->
                    ItemPackManager.itemPackIds
                }
                execute<CommandSender> { sender, context, argument ->
                    givePackCommandAsync(sender, context.argument(-1), argument, "1", tip = false)
                }
                // ni givePack [玩家ID] [物品包ID] (数量)
                dynamic(optional = true) {
                    suggestion<CommandSender>(uncheck = true) { _, _ ->
                        arrayListOf("amount")
                    }
                    execute<CommandSender> { sender, context, argument ->
                        givePackCommandAsync(sender, context.argument(-2), context.argument(-1), argument, tip = false)
                    }
                    // ni givePack [玩家ID] [物品包ID] (数量) (指向数据)
                    dynamic(optional = true) {
                        suggestion<CommandSender>(uncheck = true) { _, _ ->
                            arrayListOf("data")
                        }
                        execute<CommandSender> { sender, context, argument ->
                            givePackCommandAsync(
                                sender,
                                context.argument(-3),
                                context.argument(-2),
                                context.argument(-1),
                                argument,
                                tip = false
                            )
                        }
                    }
                }
            }
        }
    }

    private fun givePackCommandAsync(
        sender: CommandSender,
        player: String,
        id: String,
        repeat: String?,
        data: String? = null,
        tip: Boolean = true
    ) {
        async {
            givePackCommand(sender, player, id, repeat, data, tip)
        }
    }

    private fun givePackCommand(
        // 行为发起人, 用于接收反馈信息
        sender: CommandSender,
        // 给予对象
        player: String,
        // 待给予物品组ID
        id: String,
        // 重复次数
        repeat: String?,
        // 指向数据
        data: String? = null,
        // 是否进行消息提示
        tip: Boolean
    ) {
        givePackCommand(sender, Bukkit.getPlayerExact(player), id, repeat?.toIntOrNull(), data, tip)
    }

    private fun givePackCommand(
        sender: CommandSender,
        player: Player?,
        id: String,
        repeat: Int?,
        data: String? = null,
        tip: Boolean
    ) {
        player?.let {
            ItemPackManager.getItemPack(id)?.let { itemPack ->
                // 如果是按物品提示, 就建立map存储信息
                val dropData = when (getLang("Messages.type.givePackMessage")) {
                    "Items" -> HashMap<String, Int>()
                    else -> null
                }
                repeat(repeat?.coerceAtLeast(1) ?: 1) {
                    // 预定于掉落物列表
                    val dropItems = ArrayList<ItemStack>()
                    // 加载掉落信息
                    dropItems.addAll(itemPack.getItemStacks(player, data))
                    // 物品包给予事件
                    val event = ItemPackGiveEvent(id, player, dropItems)
                    event.call()
                    if (!event.isCancelled) {
                        event.itemStacks.forEach { itemStack ->
                            // 移除一下物品拥有者信息
                            if (ConfigManager.removeNBTWhenGive) {
                                val nbt = itemStack.getNbtOrNull()
                                if (nbt != null) {
                                    nbt.getCompound("NeigeItems")?.remove("owner")
                                    nbt.saveToSafe(itemStack)
                                }
                            }
                            sync {
                                player.giveItem(itemStack)
                            }
                            dropData?.let {
                                dropData[itemStack.getParsedName()] =
                                    dropData[itemStack.getParsedName()]?.let { it + itemStack.amount }
                                        ?: itemStack.amount
                            }
                        }
                    }
                }
                // 信息提示
                if (tip) {
                    dropData?.let {
                        for ((name, amt) in dropData) {
                            sender.sendLang(
                                "Messages.successInfo", mapOf(
                                    Pair("{player}", player.name),
                                    Pair("{amount}", amt.toString()),
                                    Pair("{name}", name)
                                )
                            )
                            player.sendLang(
                                "Messages.givenInfo", mapOf(
                                    Pair("{amount}", amt.toString()),
                                    Pair("{name}", name)
                                )
                            )
                        }
                    } ?: let {
                        sender.sendLang(
                            "Messages.successPackInfo", mapOf(
                                Pair("{player}", player.name),
                                Pair("{amount}", repeat.toString()),
                                Pair("{name}", id)
                            )
                        )
                        player.sendLang(
                            "Messages.givenPackInfo", mapOf(
                                Pair("{amount}", repeat.toString()),
                                Pair("{name}", id)
                            )
                        )
                    }
                }
                // 未知物品包
            } ?: let {
                sender.sendLang(
                    "Messages.unknownItemPack", mapOf(
                        Pair("{packID}", id)
                    )
                )
            }
            // 未知解析对象
        } ?: let {
            sender.sendLang("Messages.invalidParser")
        }
    }
}