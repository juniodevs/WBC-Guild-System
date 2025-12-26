package com.guild.gui;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.models.GuildMember;

/**
 * GUI de Configurações da Guilda
 */
public class GuildSettingsGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    private final int page;
    
    public GuildSettingsGUI(GuildPlugin plugin, Guild guild) {
        this(plugin, guild, 1);
    }

    public GuildSettingsGUI(GuildPlugin plugin, Guild guild, int page) {
        this.plugin = plugin;
        this.guild = guild;
        this.page = page;
    }
    
    @Override
    public String getTitle() {
        String title = plugin.getConfigManager().getGuiConfig().getString("guild-settings.title", "&6Configurações da Guilda - Página {page}");
        return ColorUtils.colorize(title
            .replace("{guild_name}", guild.getName() != null ? guild.getName() : "Guilda Desconhecida")
            .replace("{page}", String.valueOf(page)));
    }
    
    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("guild-settings.size", 54);
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // Preenche a borda
        fillBorder(inventory);
        
        if (page == 1) {
            setupPage1(inventory);
        } else if (page == 2) {
            setupPage2(inventory);
        }
        
        setupNavigation(inventory);
    }

    private void setupPage1(Inventory inventory) {
        // Alterar Nome
        setItem(inventory, "change-name", 20, Material.NAME_TAG, "&eAlterar Nome");
        
        // Alterar Descrição
        setItem(inventory, "change-description", 21, Material.BOOK, "&eAlterar Descrição");
        
        // Alterar Tag
        setItem(inventory, "change-tag", 22, Material.OAK_SIGN, "&eAlterar Tag");
        
        // Alterar Estandarte
        setupBannerButton(inventory);
        
        // Logs
        setItem(inventory, "guild-logs", 31, Material.PAPER, "&eLogs da Guilda");
    }

    private void setupPage2(Inventory inventory) {
        // Convidar Membro
        setItem(inventory, "invite-member", 20, Material.EMERALD_BLOCK, "&aConvidar Membro");
        
        // Expulsar Membro
        setItem(inventory, "kick-member", 21, Material.REDSTONE_BLOCK, "&cExpulsar Membro");
        
        // Promover Membro
        setItem(inventory, "promote-member", 22, Material.GOLD_INGOT, "&6Promover Membro");
        
        // Rebaixar Membro
        setItem(inventory, "demote-member", 23, Material.IRON_INGOT, "&7Rebaixar Membro");
        
        // Aplicações
        setItem(inventory, "applications", 24, Material.BOOK, "&eGerenciar Aplicações");
        
        // Relações
        setItem(inventory, "relations", 29, Material.RED_WOOL, "&eRelações da Guilda");
        
        // Sair da Guilda
        setItem(inventory, "leave-guild", 31, Material.BARRIER, "&cSair da Guilda");
        
        // Excluir Guilda
        setItem(inventory, "delete-guild", 33, Material.TNT, "&4Excluir Guilda");
    }

    private void setupNavigation(Inventory inventory) {
        // Voltar ao menu principal
        setItem(inventory, "back", 49, Material.ARROW, "&7Voltar");

        if (page == 1) {
            // Próxima Página
            setItem(inventory, "next-page", 50, Material.PAPER, "&aPróxima Página");
        } else if (page == 2) {
            // Página Anterior
            setItem(inventory, "previous-page", 48, Material.PAPER, "&cPágina Anterior");
        }
    }

    private void setItem(Inventory inventory, String key, int defaultSlot, Material defaultMaterial, String defaultName) {
        String path = "guild-settings.items." + key;
        int slot = plugin.getConfigManager().getGuiConfig().getInt(path + ".slot", defaultSlot);
        String name = plugin.getConfigManager().getGuiConfig().getString(path + ".name", defaultName);
        List<String> lore = plugin.getConfigManager().getGuiConfig().getStringList(path + ".lore");
        String materialName = plugin.getConfigManager().getGuiConfig().getString(path + ".material", defaultMaterial.name());
        Material material = Material.getMaterial(materialName);
        if (material == null) material = defaultMaterial;

        // Process placeholders in name and lore
        name = replacePlaceholders(name);
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, ColorUtils.colorize(replacePlaceholders(lore.get(i))));
        }

        ItemStack item = createItem(material, ColorUtils.colorize(name), lore);
        inventory.setItem(slot, item);
    }

    private String replacePlaceholders(String text) {
        return text
            .replace("{guild_name}", guild.getName() != null ? guild.getName() : "Sem Nome")
            .replace("{guild_description}", guild.getDescription() != null ? guild.getDescription() : "Sem Descrição")
            .replace("{guild_tag}", guild.getTag() != null ? guild.getTag() : "Sem Tag");
    }
    
    private void setupBannerButton(Inventory inventory) {
        ItemStack bannerItem;
        
        // Se a guilda tem um banner, mostrar o banner atual
        if (guild.getBanner() != null) {
            bannerItem = guild.getBanner().clone();
            ItemMeta meta = bannerItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtils.colorize("&eAlterar Estandarte"));
                meta.setLore(Arrays.asList(
                    ColorUtils.colorize("&7Alterar estandarte da guilda"),
                    ColorUtils.colorize("&7Atual: &aDefinido")
                ));
                bannerItem.setItemMeta(meta);
            }
        } else {
            // Se não tem banner, mostrar um banner branco padrão
            bannerItem = new ItemStack(Material.WHITE_BANNER);
            ItemMeta meta = bannerItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtils.colorize("&eAlterar Estandarte"));
                meta.setLore(Arrays.asList(
                    ColorUtils.colorize("&7Alterar estandarte da guilda"),
                    ColorUtils.colorize("&7Atual: &cNenhum")
                ));
                bannerItem.setItemMeta(meta);
            }
        }
        
        inventory.setItem(24, bannerItem);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 49) { // Back
            plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
            return;
        }

        if (page == 1) {
            switch (slot) {
                case 20: handleChangeName(player); break;
                case 21: handleChangeDescription(player); break;
                case 22: handleChangeTag(player); break;
                case 24: handleChangeBanner(player); break;
                case 31: handleGuildLogs(player); break;
                case 50: // Next Page
                    plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, 2));
                    break;
            }
        } else if (page == 2) {
            switch (slot) {
                case 20: handleInviteMember(player); break;
                case 21: handleKickMember(player); break;
                case 22: handlePromoteMember(player); break;
                case 23: handleDemoteMember(player); break;
                case 24: handleApplications(player); break;
                case 29: handleRelations(player); break;
                case 31: handleLeaveGuild(player); break;
                case 33: handleDeleteGuild(player); break;
                case 48: // Previous Page
                    plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, 1));
                    break;
            }
        }
    }
    
    /**
     * Preenche a borda
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
     * Processa alteração de nome
     */
    private void handleChangeName(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação")));
            return;
        }
        plugin.getGuiManager().openGUI(player, new GuildNameInputGUI(plugin, guild, player));
    }
    
    private void handleChangeDescription(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação")));
            return;
        }
        plugin.getGuiManager().openGUI(player, new GuildDescriptionInputGUI(plugin, guild));
    }
    
    private void handleChangeTag(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação")));
            return;
        }
        plugin.getGuiManager().openGUI(player, new GuildTagInputGUI(plugin, guild));
    }
    
    private void handlePermissions(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação")));
            return;
        }
        plugin.getGuiManager().openGUI(player, new GuildPermissionsGUI(plugin, guild));
    }
    
    private void handleInviteMember(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.officer-or-higher", "&cRequer cargo de Oficial ou superior")));
            return;
        }
        plugin.getGuiManager().openGUI(player, new InviteMemberGUI(plugin, guild));
    }
    
    private void handleKickMember(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.officer-or-higher", "&cRequer cargo de Oficial ou superior")));
            return;
        }
        plugin.getGuiManager().openGUI(player, new KickMemberGUI(plugin, guild));
    }
    
    private void handlePromoteMember(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação")));
            return;
        }
        plugin.getGuiManager().openGUI(player, new PromoteMemberGUI(plugin, guild));
    }
    
    private void handleDemoteMember(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação")));
            return;
        }
        plugin.getGuiManager().openGUI(player, new DemoteMemberGUI(plugin, guild));
    }
    
    private void handleApplications(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.officer-or-higher", "&cRequer cargo de Oficial ou superior")));
            return;
        }
        plugin.getGuiManager().openGUI(player, new ApplicationManagementGUI(plugin, guild));
    }
    
    private void handleRelations(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("relation.only-leader", "&cApenas o líder da guilda pode gerenciar relações!")));
            return;
        }
        plugin.getGuiManager().openGUI(player, new GuildRelationsGUI(plugin, guild, player));
    }
    
    private void handleGuildLogs(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&cSem permissão")));
            return;
        }
        plugin.getGuiManager().openGUI(player, new GuildLogsGUI(plugin, guild, player));
    }
    
    private void handleChangeBanner(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação")));
            return;
        }
        
        // Verificar se o jogador está segurando um banner na mão
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        
        if (!com.guild.core.utils.BannerSerializer.isBanner(itemInHand)) {
            player.sendMessage(ColorUtils.colorize("&cVocê precisa estar segurando um estandarte na mão para definir como estandarte da guilda!"));
            player.closeInventory();
            return;
        }
        
        // Abrir GUI de confirmação
        plugin.getGuiManager().openGUI(player, new GuildBannerConfirmGUI(plugin, guild, itemInHand));
    }
    
    private void handleLeaveGuild(Player player) {
        plugin.getGuiManager().openGUI(player, new ConfirmLeaveGuildGUI(plugin, guild));
    }
    
    private void handleDeleteGuild(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação")));
            return;
        }
        plugin.getGuiManager().openGUI(player, new ConfirmDeleteGuildGUI(plugin, guild));
    }
    
    /**
     * Cria item
     */
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        return createItem(material, name, Arrays.asList(lore));
    }
}
