package pers.neige.neigeitems.listener

import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import pers.neige.neigeitems.annotation.Listener
import pers.neige.neigeitems.item.ItemDurability
import pers.neige.neigeitems.libs.bot.inker.bukkit.nbt.NbtCompound
import pers.neige.neigeitems.manager.ActionManager
import pers.neige.neigeitems.utils.ItemUtils.isNiItem

object PlayerInteractListener {
    @JvmStatic
    @Listener(eventPriority = EventPriority.LOWEST, ignoreCancelled = false)
    private fun listener(event: PlayerInteractEvent) {
        // 获取玩家
        val player = event.player
        // 获取操作物品
        val itemStack = event.item
        // 类型不对劲/物品为空则终止操作
        if (event.action == Action.PHYSICAL || itemStack == null) return
        // 获取NI物品信息(不是NI物品就停止操作)
        val itemInfo = itemStack.isNiItem() ?: return
        // 物品NBT
        val itemTag: NbtCompound = itemInfo.itemTag
        // NI物品数据
        val neigeItems: NbtCompound = itemInfo.neigeItems

        // 检测已损坏物品
        if (ItemDurability.interact(player, neigeItems, event)) return
        // 执行物品动作
        ActionManager.interactListener(player, itemStack, itemInfo, event)
        if (event.isCancelled) return
        // 消耗火焰弹耐久
        ItemDurability.igniteTNT(player, itemStack, itemTag, neigeItems, event)
    }
}