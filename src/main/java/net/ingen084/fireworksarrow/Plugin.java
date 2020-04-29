package net.ingen084.fireworksarrow;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;

public class Plugin extends JavaPlugin implements Listener
{
    NamespacedKey recipeKey;
    NamespacedKey nbtKey;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        nbtKey = new NamespacedKey(this, "fireworks_data");
        recipeKey = new NamespacedKey(this, "fireworks_arrow");
        var recipe = new ShapelessRecipe(recipeKey, new ItemStack(Material.TIPPED_ARROW))
                        .addIngredient(Material.FIREWORK_ROCKET)
                        .addIngredient(Material.ARROW);
        Bukkit.addRecipe(recipe);
    }

    @EventHandler
    public void onPrepareCraftEvent(PrepareItemCraftEvent event) {
        var inv = event.getInventory();
        if (event.getRecipe() instanceof ShapelessRecipe && ((ShapelessRecipe)event.getRecipe()).getKey().equals(recipeKey)) {
            FireworkMeta fmeta = null;
            for (var i : inv.getStorageContents()) {
                if (i.getType() == Material.FIREWORK_ROCKET) {
                    fmeta = (FireworkMeta)i.getItemMeta();
                    break;
                }
            }
            if (fmeta == null) {
                inv.setResult(null);
                return;
            }

            var res = inv.getResult();
            var meta = res.getItemMeta();
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS);
            meta.addEnchant(Enchantment.DURABILITY, 1, false);
            meta.setDisplayName(ChatColor.RESET + "" + ChatColor.AQUA + "花火がつけられた矢");
            meta.getPersistentDataContainer().set(nbtKey, PersistentDataType.BYTE_ARRAY, FireworkMetaSerializer.serialize(fmeta));
            res.setItemMeta(meta);
        }
    }

    @EventHandler
    public void shootBowEvent(EntityShootBowEvent event) {
        var meta = event.getArrowItem().getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(nbtKey, PersistentDataType.BYTE_ARRAY))
            return;
        var ent = event.getProjectile();
        ent.getPersistentDataContainer().set(nbtKey, PersistentDataType.INTEGER, 1);
        var firework = (Firework)ent.getWorld().spawn(ent.getLocation(), Firework.class);
        firework.setFireworkMeta(FireworkMetaSerializer.deserialize(meta.getPersistentDataContainer().get(nbtKey, PersistentDataType.BYTE_ARRAY)));
        ent.addPassenger(firework);
        event.setConsumeArrow(true);
    }

    @EventHandler
    public void arrowHitEvent(ProjectileHitEvent event) {
        if (event.getEntityType() != EntityType.ARROW
         || event.getEntity().getPassengers().size() <= 0
         || !event.getEntity().getPersistentDataContainer().has(nbtKey, PersistentDataType.INTEGER))
            return;
        for (var e : event.getEntity().getPassengers()) {
            if (e instanceof Firework)
                ((Firework)e).detonate();
        }
        event.getEntity().remove();
    }
}
