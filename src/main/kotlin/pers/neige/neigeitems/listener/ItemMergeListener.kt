package pers.neige.neigeitems.listener

import org.bukkit.event.EventPriority
import org.bukkit.event.entity.ItemMergeEvent
import pers.neige.neigeitems.annotation.Listener
import pers.neige.neigeitems.utils.PlayerUtils.getMetadataEZ

object ItemMergeListener {
    @JvmStatic
    @Listener(eventPriority = EventPriority.LOWEST)
    fun listener(event: ItemMergeEvent) {
        val entity = event.entity
        val target = event.target
        if (entity.hasMetadata("NI-Owner") && target.hasMetadata("NI-Owner")) {
            if (entity.getMetadataEZ("NI-Owner", "String", "") != target.getMetadataEZ("NI-Owner", "String", "")) {
                event.isCancelled = true
            }
        }
    }
}