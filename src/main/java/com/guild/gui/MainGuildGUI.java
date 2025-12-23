package com.guild.gui;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;

/**
 * GUI Principal da Guilda - Seis entradas principais
 */
public class MainGuildGUI implements GUI {
    
    private final GuildPlugin plugin;
    
    public MainGuildGUI(GuildPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.title", "&6Sistema de Guilda"));
    }
    
    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("main-menu.size", 54);
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // Preencher borda
        fillBorder(inventory);
        
        // Botão de informações da guilda
        ItemStack guildInfo = createItem(
            Material.BOOK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-info.name", "&eInformações da Guilda")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-info.lore.1", "&7Ver detalhes da guilda")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-info.lore.2", "&7Inclui informações básicas, estatísticas, etc."))
        );
        inventory.setItem(20, guildInfo);
        
        // Botão de gerenciamento de membros
        ItemStack memberManagement = createItem(
            Material.PLAYER_HEAD,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.member-management.name", "&eGerenciamento de Membros")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.member-management.lore.1", "&7Gerenciar membros da guilda")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.member-management.lore.2", "&7Convidar, expulsar, gerenciar permissões"))
        );
        inventory.setItem(22, memberManagement);
        
        // Botão de gerenciamento de inscrições
        ItemStack applicationManagement = createItem(
            Material.PAPER,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.application-management.name", "&eGerenciamento de Inscrições")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.application-management.lore.1", "&7Processar inscrições")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.application-management.lore.2", "&7Ver histórico de inscrições"))
        );
        inventory.setItem(24, applicationManagement);
        
        // Botão de configurações da guilda
        ItemStack guildSettings = createItem(
            Material.COMPASS,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-settings.name", "&eConfigurações da Guilda")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-settings.lore.1", "&7Modificar configurações da guilda")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-settings.lore.2", "&7Descrição, tags, permissões, etc."))
        );
        inventory.setItem(29, guildSettings);
        
        // Botão de lista de guildas
        ItemStack guildList = createItem(
            Material.BOOKSHELF,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-list.name", "&eLista de Guildas")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-list.lore.1", "&7Ver todas as guildas")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-list.lore.2", "&7Funções de busca e filtro"))
        );
        inventory.setItem(31, guildList);
        
        // Relações da Guilda
        ItemStack guildRelations = createItem(
            Material.RED_WOOL,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-relations.name", "&eRelações da Guilda")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-relations.lore.1", "&7Gerenciar relações da guilda")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-relations.lore.2", "&7Aliado, Inimigo, Guerra, etc."))
        );
        inventory.setItem(33, guildRelations);
        
        // Criar Guilda
        ItemStack createGuild = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.create-guild.name", "&aCriar Guilda")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.create-guild.lore.1", "&7Criar uma nova guilda"))
        );
        inventory.setItem(4, createGuild);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 20: // Informações da guilda
                openGuildInfoGUI(player);
                break;
            case 22: // Gerenciamento de membros
                openMemberManagementGUI(player);
                break;
            case 24: // Gerenciamento de inscrições
                openApplicationManagementGUI(player);
                break;
            case 29: // Configurações da guilda
                openGuildSettingsGUI(player);
                break;
            case 31: // Lista de guildas
                openGuildListGUI(player);
                break;
            case 33: // Relações da guilda
                openGuildRelationsGUI(player);
                break;
            case 4: // Criar guilda
                openCreateGuildGUI(player);
                break;
        }
    }
    
    /**
     * Abrir GUI de informações da guilda
     */
    private void openGuildInfoGUI(Player player) {
        // Verificar se o jogador tem guilda
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // Garantir execução de operações GUI na thread principal
            CompatibleScheduler.runTask(plugin, () -> {
                if (guild == null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-guild", "&cVocê ainda não tem uma guilda");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // Abrir GUI de informações da guilda
                GuildInfoGUI guildInfoGUI = new GuildInfoGUI(plugin, player, guild);
                plugin.getGuiManager().openGUI(player, guildInfoGUI);
            });
        });
    }
    
    /**
     * Abrir GUI de gerenciamento de membros
     */
    private void openMemberManagementGUI(Player player) {
        // Verificar se o jogador tem guilda
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // Garantir execução de operações GUI na thread principal
            CompatibleScheduler.runTask(plugin, () -> {
                if (guild == null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-guild", "&cVocê ainda não tem uma guilda");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // Abrir GUI de gerenciamento de membros
                MemberManagementGUI memberManagementGUI = new MemberManagementGUI(plugin, guild);
                plugin.getGuiManager().openGUI(player, memberManagementGUI);
            });
        });
    }
    
    /**
     * Abrir GUI de gerenciamento de inscrições
     */
    private void openApplicationManagementGUI(Player player) {
        // Verificar se o jogador tem guilda
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // Garantir execução de operações GUI na thread principal
            CompatibleScheduler.runTask(plugin, () -> {
                if (guild == null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-guild", "&cVocê ainda não tem uma guilda");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // Verificar permissões
                plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
                    // Garantir execução de operações GUI na thread principal
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (member == null || !member.getRole().canInvite()) {
                            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&cPermissão insuficiente");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }
                        
                        // Abrir GUI de gerenciamento de inscrições
                        ApplicationManagementGUI applicationManagementGUI = new ApplicationManagementGUI(plugin, guild);
                        plugin.getGuiManager().openGUI(player, applicationManagementGUI);
                    });
                });
            });
        });
    }
    
    /**
     * Abrir GUI de configurações da guilda
     */
    private void openGuildSettingsGUI(Player player) {
        // Verificar se o jogador tem guilda
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // Garantir execução de operações GUI na thread principal
            CompatibleScheduler.runTask(plugin, () -> {
                if (guild == null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-guild", "&cVocê ainda não tem uma guilda");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // Verificar permissões
                plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
                    // Garantir execução de operações GUI na thread principal
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (member == null || member.getRole() != com.guild.models.GuildMember.Role.LEADER) {
                            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta ação");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }
                        
                        // Abrir GUI de configurações da guilda
                        GuildSettingsGUI guildSettingsGUI = new GuildSettingsGUI(plugin, guild);
                        plugin.getGuiManager().openGUI(player, guildSettingsGUI);
                    });
                });
            });
        });
    }
    
    /**
     * Abrir GUI de lista de guildas
     */
    private void openGuildListGUI(Player player) {
        // Abrir GUI de lista de guildas
        GuildListGUI guildListGUI = new GuildListGUI(plugin);
        plugin.getGuiManager().openGUI(player, guildListGUI);
    }
    
    /**
     * Abrir GUI de relações da guilda
     */
    private void openGuildRelationsGUI(Player player) {
        // Verificar se o jogador tem guilda
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // Garantir execução de operações GUI na thread principal
            CompatibleScheduler.runTask(plugin, () -> {
                if (guild == null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-guild", "&cVocê ainda não tem uma guilda");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // Verificar permissões
                plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
                    // Garantir execução de operações GUI na thread principal
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (member == null || member.getRole() != com.guild.models.GuildMember.Role.LEADER) {
                            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode gerenciar relações");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }
                        
                        // Abrir GUI de relações da guilda
                        GuildRelationsGUI guildRelationsGUI = new GuildRelationsGUI(plugin, guild, player);
                        plugin.getGuiManager().openGUI(player, guildRelationsGUI);
                    });
                });
            });
        });
    }

    /**
     * Abrir GUI de criar guilda
     */
    private void openCreateGuildGUI(Player player) {
        // Verificar se o jogador já tem guilda
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // Garantir execução de operações GUI na thread principal
            CompatibleScheduler.runTask(plugin, () -> {
                if (guild != null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("create.already-in-guild", "&cVocê já está em uma guilda!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // Abrir GUI de criar guilda
                CreateGuildGUI createGuildGUI = new CreateGuildGUI(plugin);
                plugin.getGuiManager().openGUI(player, createGuildGUI);
            });
        });
    }
    
    /**
     * Preencher borda
     */
    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 45, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }
    
    /**
     * Criar item
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
