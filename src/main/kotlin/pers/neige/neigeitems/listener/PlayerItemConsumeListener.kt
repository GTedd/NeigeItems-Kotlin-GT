package pers.neige.neigeitems.listener

import org.bukkit.event.player.PlayerItemConsumeEvent
import pers.neige.neigeitems.item.ItemDurability
import pers.neige.neigeitems.manager.ActionManager
import pers.neige.neigeitems.utils.ItemUtils.isNiItem
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.nms.ItemTag

object PlayerItemConsumeListener {
    @SubscribeEvent(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun listener(event: PlayerItemConsumeEvent) {
        // 获取玩家
        val player = event.player
        // 获取手持物品
        val itemStack = event.item.clone()
        // 获取NI物品信息(不是NI物品就停止操作)
        val itemInfo = itemStack.isNiItem(true) ?: return
        // 物品NBT
        val itemTag: ItemTag = itemInfo.itemTag
        // NI物品数据
        val neigeItems: ItemTag = itemInfo.neigeItems
        // NI物品id
        val id: String = itemInfo.id
        // NI节点数据
        val data: HashMap<String, String> = itemInfo.data!!

        // 检测已损坏物品
        ItemDurability.consume(player, neigeItems, event)
        if (event.isCancelled) return
        // 执行物品动作
        ActionManager.eatListener(player, itemStack, itemTag, neigeItems, id, data, event)

        // 设置物品
        if (event.item == player.inventory.itemInMainHand) {
            player.inventory.setItemInMainHand(itemStack)
        } else {
            player.inventory.setItemInOffHand(itemStack)
        }
    }
}