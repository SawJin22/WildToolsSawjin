package com.bgsoftware.wildtools.objects.tools;

import com.bgsoftware.wildtools.api.events.CrowbarWandUseEvent;
import com.bgsoftware.wildtools.api.objects.ToolMode;
import com.bgsoftware.wildtools.api.objects.tools.CrowbarTool;
import com.bgsoftware.wildtools.utils.BukkitUtils;
import com.bgsoftware.wildtools.utils.items.ItemUtils;
import com.bgsoftware.wildtools.utils.items.ToolTaskManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public final class WCrowbarTool extends WTool implements CrowbarTool {

    private final List<String> commandsOnUse;

    public WCrowbarTool(Material type, String name, List<String> commandsOnUse){
        super(type, name, ToolMode.CROWBAR);
        this.commandsOnUse = commandsOnUse;
    }

    @Override
    public List<String> getCommandsOnUse() {
        return commandsOnUse;
    }

    @Override
    public boolean onBlockInteract(PlayerInteractEvent e) {
        if(!e.getClickedBlock().getType().name().contains("SPAWNER"))
            return false;

        UUID taskId = ToolTaskManager.generateTaskId(e.getItem(), e.getPlayer());

        if(!BukkitUtils.canBreakBlock(e.getClickedBlock(), this))
            return true;

        CreatureSpawner creatureSpawner = (CreatureSpawner) e.getClickedBlock().getState();

        if(!BukkitUtils.breakBlock(e.getPlayer(), null, e.getClickedBlock(), e.getItem(), this, itemStack -> null))
            return true;

        if(commandsOnUse.isEmpty()) {
            List<ItemStack> itemsToDrop = plugin.getProviders().getBlockDrops(e.getPlayer(), e.getClickedBlock(), true);
            if(!itemsToDrop.isEmpty()) {
                ItemStack dropItem = itemsToDrop.get(0);
                if (isAutoCollect())
                    ItemUtils.addItem(dropItem, e.getPlayer().getInventory(), e.getClickedBlock().getLocation());
                else
                    e.getClickedBlock().getWorld().dropItemNaturally(e.getClickedBlock().getLocation(), dropItem);
            }
        }
        else{
            commandsOnUse.forEach(commandOnUse -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandOnUse
                    .replace("%player%", e.getPlayer().getName())
                    .replace("%entity%", creatureSpawner.getSpawnedType().name())
            ));
        }

        CrowbarWandUseEvent crowbarWandUseEvent = new CrowbarWandUseEvent(e.getPlayer(), this, e.getClickedBlock());
        Bukkit.getPluginManager().callEvent(crowbarWandUseEvent);

        reduceDurablility(e.getPlayer(), 1, taskId);

        return true;
    }
}
