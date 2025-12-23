package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.GUIUtils;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.services.GuildService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GUI de Informações da Guilda
 */
public class GuildInfoGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Player player;
    private final Guild guild;
    private Inventory inventory;
    
    public GuildInfoGUI(GuildPlugin plugin, Player player, Guild guild) {
        this.plugin = plugin;
        this.player = player;
        this.guild = guild;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-info.title", "&6Informações da Guilda"));
    }
    
    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("guild-info.size", 54);
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        this.inventory = inventory;
        
        // Obter configuração da GUI
        ConfigurationSection config = plugin.getConfigManager().getGuiConfig().getConfigurationSection("guild-info.items");
        if (config == null) {
            setupDefaultItems();
            return;
        }
        
        // Configurar itens da configuração
        for (String key : config.getKeys(false)) {
            ConfigurationSection itemConfig = config.getConfigurationSection(key);
            if (itemConfig != null) {
                setupConfigItem(itemConfig);
            }
        }
    }
    
    private void setupConfigItem(ConfigurationSection itemConfig) {
        String materialName = itemConfig.getString("material", "STONE");
        Material material = Material.valueOf(materialName.toUpperCase());
        int slot = itemConfig.getInt("slot", 0);
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Definir nome
            String name = itemConfig.getString("name", "");
            if (!name.isEmpty()) {
                // Usar GUIUtils para processar variáveis
                GUIUtils.processGUIVariablesAsync(name, guild, player, plugin).thenAccept(processedName -> {
                    CompatibleScheduler.runTask(plugin, () -> {
                        meta.setDisplayName(processedName);
                        
                        // Definir descrição
                        List<String> lore = itemConfig.getStringList("lore");
                        if (!lore.isEmpty()) {
                            GUIUtils.processGUILoreAsync(lore, guild, player, plugin).thenAccept(processedLore -> {
                                CompatibleScheduler.runTask(plugin, () -> {
                                    meta.setLore(processedLore);
                                    item.setItemMeta(meta);
                                    inventory.setItem(slot, item);
                                });
                            });
                        } else {
                            item.setItemMeta(meta);
                            inventory.setItem(slot, item);
                        }
                    });
                });
            } else {
                // Se não houver nome, definir descrição diretamente
                List<String> lore = itemConfig.getStringList("lore");
                if (!lore.isEmpty()) {
                                    GUIUtils.processGUILoreAsync(lore, guild, player, plugin).thenAccept(processedLore -> {
                    CompatibleScheduler.runTask(plugin, () -> {
                        meta.setLore(processedLore);
                        item.setItemMeta(meta);
                        inventory.setItem(slot, item);
                    });
                });
                } else {
                    item.setItemMeta(meta);
                    inventory.setItem(slot, item);
                }
            }
        } else {
            inventory.setItem(slot, item);
        }
    }
    
    private void setupDefaultItems() {
        // Status da guilda
        String status = guild.isFrozen() ? "§cCongelada" : "§aNormal";

        // Nome da guilda
        ItemStack nameItem = createItem(Material.NAME_TAG, "§6Nome da Guilda", 
            "§e" + guild.getName(),
            "§7Status: " + status);
        inventory.setItem(10, nameItem);
        
        // Tag da guilda
        if (guild.getTag() != null && !guild.getTag().isEmpty()) {
            ItemStack tagItem = createItem(Material.OAK_SIGN, "§6Tag da Guilda", 
                "§e[" + guild.getTag() + "]");
            inventory.setItem(12, tagItem);
        }
        
        // Descrição da guilda
        if (guild.getDescription() != null && !guild.getDescription().isEmpty()) {
            ItemStack descItem = createItem(Material.BOOK, "§6Descrição da Guilda", 
                "§e" + guild.getDescription());
            inventory.setItem(14, descItem);
        }
        
        // Informações do líder
        ItemStack leaderItem = createItem(Material.GOLDEN_HELMET, "§6Líder", 
            "§e" + guild.getLeaderName());
        inventory.setItem(16, leaderItem);
        
        // Quantidade de membros - Usar método assíncrono
        plugin.getGuildService().getGuildMemberCountAsync(guild.getId()).thenAccept(memberCount -> {
            CompatibleScheduler.runTask(plugin, () -> {
                ItemStack memberItem = createItem(Material.PLAYER_HEAD, "§6Membros", 
                    "§e" + memberCount + "/" + guild.getMaxMembers() + " pessoas");
                inventory.setItem(29, memberItem);
            });
        });
        
        // Nível da guilda
        ItemStack levelItem = createItem(Material.EXPERIENCE_BOTTLE, "§6Nível da Guilda", 
            "§eNível " + guild.getLevel(),
            "§7Máx Membros: " + guild.getMaxMembers() + " pessoas");
        inventory.setItem(31, levelItem);
        

        // Data de criação (usar formato de tempo real)
        String createdTime = guild.getCreatedAt() != null
            ? guild.getCreatedAt().format(com.guild.core.time.TimeProvider.FULL_FORMATTER)
            : "Desconhecido";
        ItemStack timeItem = createItem(Material.CLOCK, "§6Criada em", "§e" + createdTime);
        inventory.setItem(33, timeItem);
        
        // Botão de voltar
        ItemStack backItem = createItem(Material.ARROW, "§cVoltar", 
            "§eClique para voltar ao menu principal");
        inventory.setItem(49, backItem);
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            meta.setLore(loreList);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private String replacePlaceholders(String text) {
        return PlaceholderUtils.replaceGuildPlaceholders(text, guild, player);
    }

    private String replacePlaceholdersAsync(String text, int memberCount) {
        // Primeiro usar PlaceholderUtils para processar variáveis básicas
        String result = PlaceholderUtils.replaceGuildPlaceholders(text, guild, player);
        
        // Depois processar variáveis dinâmicas
        return result
            .replace("{member_count}", String.valueOf(memberCount))
            .replace("{online_member_count}", String.valueOf(memberCount)); // Usar temporariamente o número total de membros
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 49) {
            // Voltar ao menu principal
            plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
        }
    }
    
    @Override
    public void onClose(Player player) {
        // Processamento ao fechar
    }
    
    @Override
    public void refresh(Player player) {
        setupInventory(inventory);
    }
    
    public Inventory getInventory() {
        return inventory;
    }
    
}
