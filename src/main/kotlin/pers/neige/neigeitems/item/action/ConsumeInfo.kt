package pers.neige.neigeitems.item.action

import org.bukkit.configuration.ConfigurationSection
import pers.neige.neigeitems.action.Action
import pers.neige.neigeitems.manager.ActionManager

/**
 * 物品动作中的物品消耗信息
 *
 * @param config 物品消耗信息配置
 */
class ConsumeInfo(config: ConfigurationSection) {
    val pre: Action = ActionManager.compile(config.get("pre"))
    val condition: String? = config.getString("condition")
    val amount: String? = config.getString("amount")
    val deny: Action = ActionManager.compile(config.get("deny"))
}