package com.bgsoftware.wildtools.nms;

import com.bgsoftware.wildtools.utils.items.ToolItemStack;
import net.minecraft.server.v1_7_R3.AxisAlignedBB;
import net.minecraft.server.v1_7_R3.Block;
import net.minecraft.server.v1_7_R3.BlockCarrots;
import net.minecraft.server.v1_7_R3.BlockCocoa;
import net.minecraft.server.v1_7_R3.BlockCrops;
import net.minecraft.server.v1_7_R3.BlockNetherWart;
import net.minecraft.server.v1_7_R3.BlockPotatoes;
import net.minecraft.server.v1_7_R3.Blocks;
import net.minecraft.server.v1_7_R3.Chunk;
import net.minecraft.server.v1_7_R3.EnchantmentManager;
import net.minecraft.server.v1_7_R3.EntityItem;
import net.minecraft.server.v1_7_R3.EntityLiving;
import net.minecraft.server.v1_7_R3.EntityPlayer;
import net.minecraft.server.v1_7_R3.GameProfileSerializer;
import net.minecraft.server.v1_7_R3.Item;
import net.minecraft.server.v1_7_R3.ItemBow;
import net.minecraft.server.v1_7_R3.ItemStack;
import net.minecraft.server.v1_7_R3.Items;
import net.minecraft.server.v1_7_R3.NBTTagCompound;
import net.minecraft.server.v1_7_R3.PacketPlayOutCollect;
import net.minecraft.server.v1_7_R3.PacketPlayOutMultiBlockChange;
import net.minecraft.server.v1_7_R3.StatisticList;
import net.minecraft.server.v1_7_R3.TileEntity;
import net.minecraft.server.v1_7_R3.TileEntitySkull;
import net.minecraft.server.v1_7_R3.World;

import net.minecraft.server.v1_7_R3.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NetherWartsState;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_7_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_7_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_7_R3.entity.CraftItem;
import org.bukkit.craftbukkit.v1_7_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_7_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R3.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_7_R3.inventory.CraftInventoryPlayer;
import org.bukkit.craftbukkit.v1_7_R3.inventory.CraftItemStack;

import org.bukkit.CropState;
import org.bukkit.craftbukkit.v1_7_R3.util.CraftMagicNumbers;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.material.CocoaPlant;
import org.bukkit.material.Crops;
import org.bukkit.material.NetherWarts;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@SuppressWarnings({"unused", "deprecation"})
public final class NMSAdapter_v1_7_R3 implements NMSAdapter {

    private static Field customItemStackHandleField = null;

    static {
        try {
            customItemStackHandleField = CraftItemStack.class.getDeclaredField("handle");
            customItemStackHandleField.setAccessible(true);
        }catch (Exception ignored){}
    }

    @Override
    public String getVersion() {
        return "v1_7_R3";
    }

    @Override
    public List<org.bukkit.inventory.ItemStack> getBlockDrops(Player pl, org.bukkit.block.Block bl, boolean silkTouch) {
        List<org.bukkit.inventory.ItemStack> drops = new ArrayList<>();

        EntityPlayer player = ((CraftPlayer) pl).getHandle();
        World world = player.world;
        Block block = world.getType(bl.getX(), bl.getY(), bl.getZ());

        //Checks if player cannot break the block or player in creative mode
        if(!player.a(block) || player.playerInteractManager.isCreative())
            return drops;

        TileEntity tileEntity = world.getTileEntity(bl.getX(), bl.getY(), bl.getZ());

        if(tileEntity instanceof TileEntitySkull){
            TileEntitySkull tileEntitySkull = (TileEntitySkull) tileEntity;
            if(tileEntitySkull.getSkullType() == 3){
                ItemStack itemStack = new ItemStack(Items.SKULL, 1, 3);
                NBTTagCompound nbtTagCompound = itemStack.hasTag() ? itemStack.getTag() : new NBTTagCompound();
                assert nbtTagCompound != null;
                NBTTagCompound skullOwnerTag = new NBTTagCompound();
                GameProfileSerializer.a(skullOwnerTag, tileEntitySkull.getGameProfile());
                nbtTagCompound.set("SkullOwner", skullOwnerTag);
                itemStack.setTag(nbtTagCompound);
                drops.add(CraftItemStack.asBukkitCopy(itemStack));
                return drops;
            }
        }

        //Checks if player has silk touch
        if ((block.d() && !block.isTileEntity()) && (silkTouch || EnchantmentManager.hasSilkTouchEnchantment(player))) {
            int data = 0;
            Item item = Item.getItemOf(block);
            //Checks if item not null and something else?
            if (item != null && item.n()) {
                data = Block.b(block);
            }
            //Adds item to drops
            drops.add(CraftItemStack.asBukkitCopy(new ItemStack(item, 1, data)));
        }

        else{
            int fortuneLevel = getItemInHand(pl).getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS),
                    dropCount = block.getDropCount(fortuneLevel, world.random),
                    blockId = Block.b(block);

            Item item = block.getDropType(blockId, world.random, fortuneLevel);
            if (item != null) {
                drops.add(CraftItemStack.asBukkitCopy(new ItemStack(item, dropCount, block.getDropData(blockId))));
            }
        }

        return drops;
    }

    @Override
    public List<org.bukkit.inventory.ItemStack> getCropDrops(Player pl, org.bukkit.block.Block bl) {
        List<org.bukkit.inventory.ItemStack> drops = new ArrayList<>();

        EntityPlayer player = ((CraftPlayer) pl).getHandle();
        World world = player.world;
        Block block = world.getType(bl.getX(), bl.getY(), bl.getZ());

        int age = ((CraftBlock) bl).getData();
        int fortuneLevel = getItemInHand(pl).getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);

        if(block instanceof BlockCrops){
            if (age >= 7) {
                //Give the item itself to the player
                if(block instanceof BlockCarrots) {
                    drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.CARROT, 1, 0)));
                }else if(block instanceof BlockPotatoes){
                    drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.POTATO, 1, 0)));
                }else{
                    drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.WHEAT, 1, 0)));
                }
                //Give the "seeds" to the player. I run -1 iteration for "replant"
                for(int i = 0; i < (fortuneLevel + 3) - 1; i++) {
                    if (world.random.nextInt(15) <= age) {
                        if(block instanceof BlockCarrots) {
                            drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.CARROT, 1, 0)));
                        }else if(block instanceof BlockPotatoes){
                            drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.POTATO, 1, 0)));
                            if (world.random.nextInt(50) == 0) {
                                drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.POTATO_POISON, 1, 0)));
                            }
                        }
                        else{
                            drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.SEEDS, 1, 0)));
                        }
                    }
                }
            }
        }
        else if(block instanceof BlockCocoa){
            if(age >= 2) {
                drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.INK_SACK, 3, 3)));
            }
        }
        else if(block instanceof BlockNetherWart){
            if (age >= 3) {
                int amount = 2 + world.random.nextInt(3);
                if (fortuneLevel > 0) {
                    amount += world.random.nextInt(fortuneLevel + 1);
                }
                drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.NETHER_STALK, amount)));
            }
        }

        return drops;
    }

    @Override
    public int getExpFromBlock(org.bukkit.block.Block block, Player player) {
        World world = ((CraftWorld) block.getWorld()).getHandle();
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        return world.getType(block.getX(), block.getY(), block.getZ())
                .getExpDrop(world, block.getData(), EnchantmentManager.getBonusBlockLootEnchantmentLevel(entityPlayer));
    }

    @Override
    public int getTag(ToolItemStack toolItemStack, String key, int def) {
        ItemStack nmsStack = (ItemStack) toolItemStack.getNMSItem();
        NBTTagCompound tagCompound = nmsStack.getTag();
        if(tagCompound == null){
            nmsStack.setTag(new NBTTagCompound());
            tagCompound = nmsStack.getTag();
        }
        return tagCompound.hasKey(key)  ? tagCompound.getInt(key) : def;
    }

    @Override
    public void setTag(ToolItemStack toolItemStack, String key, int value) {
        ItemStack nmsStack = (ItemStack) toolItemStack.getNMSItem();
        NBTTagCompound tagCompound = nmsStack.getTag();
        if(tagCompound == null){
            nmsStack.setTag(new NBTTagCompound());
            tagCompound = nmsStack.getTag();
        }
        tagCompound.setInt(key, value);
    }

    @Override
    public String getTag(ToolItemStack toolItemStack, String key, String def) {
        ItemStack nmsStack = (ItemStack) toolItemStack.getNMSItem();
        NBTTagCompound tagCompound = nmsStack.getTag();
        if(tagCompound == null){
            nmsStack.setTag(new NBTTagCompound());
            tagCompound = nmsStack.getTag();
        }
        return tagCompound.hasKey(key)  ? tagCompound.getString(key) : def;
    }

    @Override
    public void setTag(ToolItemStack toolItemStack, String key, String value) {
        ItemStack nmsStack = (ItemStack) toolItemStack.getNMSItem();
        NBTTagCompound tagCompound = nmsStack.getTag();
        if(tagCompound == null){
            nmsStack.setTag(new NBTTagCompound());
            tagCompound = nmsStack.getTag();
        }
        tagCompound.setString(key, value);
    }

    @Override
    public void clearTasks(ToolItemStack toolItemStack) {
        ItemStack nmsStack = (ItemStack) toolItemStack.getNMSItem();
        NBTTagCompound tagCompound = nmsStack.getTag();
        if(tagCompound == null){
            nmsStack.setTag(new NBTTagCompound());
            tagCompound = nmsStack.getTag();
        }
        tagCompound.remove("task-id");
    }

    @Override
    public void breakTool(ToolItemStack toolItemStack, Player player) {
        ItemStack nmsStack = (ItemStack) toolItemStack.getNMSItem();
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();


        entityPlayer.a(nmsStack);
        nmsStack.count -= 1;

        entityPlayer.a(StatisticList.BREAK_ITEM_COUNT[Item.b(nmsStack.getItem())]);
        if (nmsStack.count == 0 && nmsStack.getItem() instanceof ItemBow) {
            entityPlayer.bF();
        }

        if (nmsStack.count < 0)
            nmsStack.count = 0;

        if (nmsStack.count == 0)
            CraftEventFactory.callPlayerItemBreakEvent(entityPlayer, nmsStack);

        nmsStack.setData(0);
    }

    @Override
    public Object[] createSyncedItem(org.bukkit.inventory.ItemStack other) {
        CraftItemStack craftItemStack;
        ItemStack handle = null;
        if(other instanceof CraftItemStack){
            craftItemStack = (CraftItemStack) other;
            try {
                handle = (ItemStack) customItemStackHandleField.get(other);
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }else{
            handle = CraftItemStack.asNMSCopy(other);
            craftItemStack = CraftItemStack.asCraftMirror(handle);
        }

        return new Object[] {craftItemStack, handle};
    }

    @Override
    public org.bukkit.inventory.ItemStack getItemInHand(Player player) {
        ItemStack itemStack = ((CraftInventoryPlayer) player.getInventory()).getInventory().getItemInHand();
        return CraftItemStack.asBukkitCopy(itemStack);
    }

    @Override
    public org.bukkit.inventory.ItemStack getItemInHand(Player player, Event e) {
        return getItemInHand(player);
    }

    @Override
    public boolean isFullyGrown(org.bukkit.block.Block block) {
        if(block.getState().getData() instanceof Crops)
            return ((Crops) block.getState().getData()).getState() == CropState.RIPE;
        else if(block.getState().getData() instanceof CocoaPlant)
            return ((CocoaPlant) block.getState().getData()).getSize() == CocoaPlant.CocoaPlantSize.LARGE;
        else if(block.getState().getData() instanceof NetherWarts)
            return ((NetherWarts) block.getState().getData()).getState() == NetherWartsState.RIPE;
        else if(block.getType() == Material.CARROT || block.getType() == Material.POTATO)
            return ((CraftBlock) block).getData() == CropState.RIPE.getData();

        return true;
    }

    @Override
    public void setCropState(org.bukkit.block.Block block, CropState cropState) {
        if(block.getType() == Material.COCOA){
            CocoaPlant cocoaPlant = (CocoaPlant) block.getState().getData();
            switch (cropState){
                case SEEDED:
                case GERMINATED:
                case VERY_SMALL:
                case SMALL:
                    cocoaPlant.setSize(CocoaPlant.CocoaPlantSize.SMALL);
                    break;
                case MEDIUM:
                    cocoaPlant.setSize(CocoaPlant.CocoaPlantSize.MEDIUM);
                    break;
                case TALL:
                case VERY_TALL:
                case RIPE:
                    cocoaPlant.setSize(CocoaPlant.CocoaPlantSize.LARGE);
                    break;
            }
            ((CraftBlock) block).setData(cocoaPlant.getData());
        }else if(block.getType() == Material.MELON_BLOCK || block.getType() == Material.PUMPKIN){
            block.setType(Material.AIR);
        }else {
            ((CraftBlock) block).setData(cropState.getData());
        }
    }

    @Override
    public Collection<Player> getOnlinePlayers() {
        return Collections.unmodifiableCollection(Arrays.asList(Bukkit.getOnlinePlayers()));
    }

    @Override
    public void setBlockFast(Location location, int combinedId) {
        World world = ((CraftWorld) location.getWorld()).getHandle();
        Chunk chunk = world.getChunkAt(location.getChunk().getX(), location.getChunk().getZ());

        int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();

        if(combinedId == 0)
            world.a(null, 2001, x, y, z, Block.b(world.getType(x, y, z)) + (world.getData(x, y, z) << 12));

        chunk.a(x & 0x0f, y, z & 0x0f, Block.e(combinedId), 2);
    }

    @Override
    public void refreshChunk(org.bukkit.Chunk bukkitChunk, Set<Location> blocksList) {
        Chunk chunk = ((CraftChunk) bukkitChunk).getHandle();
        int blocksAmount = blocksList.size();
        short[] values = new short[blocksAmount];

        Location firstLocation = null;

        int counter = 0;
        for(Location location : blocksList) {
            if(firstLocation == null)
                firstLocation = location;

            values[counter++] = (short) ((location.getBlockX() & 15) << 12 | (location.getBlockZ() & 15) << 8 | location.getBlockY());
        }

        PacketPlayOutMultiBlockChange multiBlockChange = new PacketPlayOutMultiBlockChange(blocksAmount, values, chunk);

        assert firstLocation != null;
        AxisAlignedBB bb = AxisAlignedBB.a(firstLocation.getX() - 60, firstLocation.getY() - 200, firstLocation.getZ() - 60,
                firstLocation.getX() + 60, firstLocation.getY() + 200, firstLocation.getZ() + 60);

        //noinspection unchecked
        for(Entity entity : (List<Entity>) ((CraftWorld) bukkitChunk.getWorld()).getHandle().getEntities(null, bb)){
            if(entity instanceof EntityPlayer)
                ((EntityPlayer) entity).playerConnection.sendPacket(multiBlockChange);
        }
    }

    @Override
    public int getCombinedId(Location location) {
        World world = ((CraftWorld) location.getWorld()).getHandle();
        return Block.b(world.getType(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
    }

    @Override
    public int getFarmlandId() {
        return Block.b(Blocks.SOIL);
    }

    @Override
    public void setCombinedId(Location location, int combinedId) {
        World world = ((CraftWorld) location.getWorld()).getHandle();
        world.setTypeAndData(location.getBlockX(), location.getBlockY(), location.getBlockZ(), Block.e(combinedId), 2,18);
    }

    @Override
    public Enchantment getGlowEnchant() {
        return new Enchantment(101) {
            @Override
            public String getName() {
                return "WildToolsGlow";
            }

            @Override
            public int getMaxLevel() {
                return 1;
            }

            @Override
            public int getStartLevel() {
                return 0;
            }

            @Override
            public EnchantmentTarget getItemTarget() {
                return null;
            }

            @Override
            public boolean conflictsWith(Enchantment enchantment) {
                return false;
            }

            @Override
            public boolean canEnchantItem(org.bukkit.inventory.ItemStack itemStack) {
                return true;
            }
        };
    }

    @Override
    public boolean isOutsideWorldborder(Location location) {
        return false;
    }

    @Override
    public BlockPlaceEvent getFakePlaceEvent(Player player, Location location, org.bukkit.block.Block copyBlock) {
        FakeCraftBlock fakeBlock = FakeCraftBlock.at(location, copyBlock.getType());
        org.bukkit.block.Block original = location.getBlock();
        return new BlockPlaceEvent(
                fakeBlock,
                original.getState(),
                fakeBlock.getRelative(BlockFace.DOWN),
                new org.bukkit.inventory.ItemStack(copyBlock.getType()),
                player,
                true
        );
    }

    @Override
    public void playPickupAnimation(LivingEntity livingEntity, org.bukkit.entity.Item item) {
        EntityLiving entityLiving = ((CraftLivingEntity) livingEntity).getHandle();
        EntityItem entityItem = (EntityItem) ((CraftItem) item).getHandle();
        ((WorldServer) entityLiving.world).getTracker().a(entityItem, new PacketPlayOutCollect(entityItem.getId(), entityLiving.getId()));
    }

    @Override
    public boolean isAxeType(Material material) {
        return Items.DIAMOND_AXE.getDestroySpeed(new ItemStack(Items.DIAMOND_AXE), CraftMagicNumbers.getBlock(material)) == 8.0F;
    }

    @Override
    public boolean isShovelType(Material material) {
        return Items.DIAMOND_SPADE.getDestroySpeed(new ItemStack(Items.DIAMOND_SPADE), CraftMagicNumbers.getBlock(material)) == 8.0F;
    }

    private static class FakeCraftBlock extends CraftBlock{

        private Material blockType;

        FakeCraftBlock(CraftChunk craftChunk, int x, int y, int z, Material material){
            super(craftChunk, x, y, z);
            this.blockType = material;
        }

        @Override
        public Material getType() {
            return blockType;
        }

        @Override
        public void setType(Material type) {
            this.blockType = type;
            super.setType(type);
        }

        static FakeCraftBlock at(Location location, Material type){
            CraftChunk craftChunk = (CraftChunk) location.getChunk();
            return new FakeCraftBlock(craftChunk, location.getBlockX(), location.getBlockY(), location.getBlockZ(), type);
        }

    }

}
