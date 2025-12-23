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
import com.guild.models.Guild;
import com.guild.models.GuildMember;

/**
 * GUI de Configurações da Guilda
 */
public class GuildSettingsGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    
    public GuildSettingsGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.title", "&6Configurações da Guilda - {guild_name}")
            .replace("{guild_name}", guild.getName() != null ? guild.getName() : "Guilda Desconhecida"));
    }
    
    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("guild-settings.size", 54);
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // Preenche a borda
        fillBorder(inventory);
        
        // Adiciona botões de configuração
        setupSettingsButtons(inventory);
        
        // Mostra informações de configuração atuais
        displayCurrentSettings(inventory);
        
        // Adiciona botões de função
        setupFunctionButtons(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 10: // Alterar nome
                handleChangeName(player);
                break;
            case 11: // Alterar descrição
                handleChangeDescription(player);
                break;
            case 12: // Alterar tag
                handleChangeTag(player);
                break;
            case 16: // Configurações de permissão
                handlePermissions(player);
                break;
            case 20: // Convidar membro
                handleInviteMember(player);
                break;
            case 22: // Expulsar membro
                handleKickMember(player);
                break;
            case 24: // Promover membro
                handlePromoteMember(player);
                break;
            case 26: // Rebaixar membro
                handleDemoteMember(player);
                break;
            case 30: // Processar solicitações
                handleApplications(player);
                break;
            case 31: // Gerenciar relações da guilda
                handleRelations(player);
                break;
            case 32: // Logs da guilda
                handleGuildLogs(player);
                break;
            case 34: // Sair da guilda
                handleLeaveGuild(player);
                break;
            case 36: // Excluir guilda
                handleDeleteGuild(player);
                break;
            case 49: // Voltar
                plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
                break;
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
     * Configura botões de configuração
     */
    private void setupSettingsButtons(Inventory inventory) {
        // Botão de alterar nome
        ItemStack changeName = createItem(
            Material.NAME_TAG,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-name.name", "&eAlterar Nome")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-name.lore.1", "&7Alterar nome da guilda"))
        );
        inventory.setItem(11, changeName);
        
        // Botão de alterar descrição
        ItemStack changeDescription = createItem(
            Material.BOOK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-description.name", "&eAlterar Descrição")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-description.lore.1", "&7Alterar descrição da guilda"))
        );
        inventory.setItem(12, changeDescription);
        
        // Botão de alterar tag
        ItemStack changeTag = createItem(
            Material.OAK_SIGN,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-tag.name", "&eAlterar Tag")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-tag.lore.1", "&7Alterar tag da guilda"))
        );
        inventory.setItem(13, changeTag);
        
        // Botão de configurações de permissão
        ItemStack permissions = createItem(
            Material.SHIELD,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.permissions.name", "&eConfigurar Permissões")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.permissions.lore.1", "&7Gerenciar permissões de membros"))
        );
        inventory.setItem(14, permissions);
    }
    
    /**
     * Configura botões de função
     */
    private void setupFunctionButtons(Inventory inventory) {
        // Botão de convidar membro
        ItemStack inviteMember = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.invite-member", "&aConvidar Membro")),
            ColorUtils.colorize("&7Convidar novos membros para a guilda")
        );
        inventory.setItem(19, inviteMember);
        
        // Botão de expulsar membro
        ItemStack kickMember = createItem(
            Material.REDSTONE,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.kick-member", "&cExpulsar Membro")),
            ColorUtils.colorize("&7Expulsar membro da guilda")
        );
        inventory.setItem(21, kickMember);
        
        // Botão de promover membro
        ItemStack promoteMember = createItem(
            Material.GOLD_INGOT,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.promote-member", "&6Promover Membro")),
            ColorUtils.colorize("&7Promover cargo do membro")
        );
        inventory.setItem(23, promoteMember);
        
        // Botão de rebaixar membro
        ItemStack demoteMember = createItem(
            Material.IRON_INGOT,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.demote-member", "&7Rebaixar Membro")),
            ColorUtils.colorize("&7Rebaixar cargo do membro")
        );
        inventory.setItem(25, demoteMember);
        
        // Botão de processar solicitações
        ItemStack applications = createItem(
            Material.PAPER,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.application-management", "&eGerenciar Solicitações")),
            ColorUtils.colorize("&7Processar solicitações de entrada")
        );
        inventory.setItem(30, applications);
        
        // Botão de gerenciar relações da guilda
        ItemStack relations = createItem(
            Material.RED_WOOL,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-relations-management.name", "&eGerenciar Relações")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-relations-management.lore.1", "&7Gerenciar relações da guilda")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-relations-management.lore.2", "&7Aliados, Inimigos, etc."))
        );
        inventory.setItem(31, relations);
        
        // Botão de logs da guilda
        ItemStack guildLogs = createItem(
            Material.BOOK,
            ColorUtils.colorize("&6Logs da Guilda"),
            ColorUtils.colorize("&7Ver histórico de operações da guilda"),
            ColorUtils.colorize("&7Registra todas as operações importantes")
        );
        inventory.setItem(32, guildLogs);
        
        // Botão de sair da guilda
        ItemStack leaveGuild = createItem(
            Material.BARRIER,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.leave-guild", "&cSair da Guilda")),
            ColorUtils.colorize("&7Sair da guilda atual")
        );
        inventory.setItem(34, leaveGuild);
        
        // Botão de excluir guilda
        ItemStack deleteGuild = createItem(
            Material.TNT,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.delete-guild", "&4Excluir Guilda")),
            ColorUtils.colorize("&7Excluir a guilda inteira"),
            ColorUtils.colorize("&cEsta operação é irreversível!")
        );
        inventory.setItem(28, deleteGuild);
        
        // Botão de voltar
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.back.name", "&7Voltar")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.back.lore.1", "&7Voltar ao menu principal"))
        );
        inventory.setItem(49, back);
    }
    
    /**
     * Mostra informações de configuração atuais
     */
    private void displayCurrentSettings(Inventory inventory) {
        // Nome atual
        ItemStack currentName = createItem(
            Material.NAME_TAG,
            ColorUtils.colorize("&eNome Atual"),
            ColorUtils.colorize("&7" + (guild.getName() != null ? guild.getName() : "Sem Nome"))
        );
        inventory.setItem(10, currentName);
        
        // Descrição atual
        ItemStack currentDescription = createItem(
            Material.BOOK,
            ColorUtils.colorize("&eDescrição Atual"),
            ColorUtils.colorize("&7" + (guild.getDescription() != null ? guild.getDescription() : "Sem Descrição"))
        );
        inventory.setItem(11, currentDescription);
        
        // Tag atual
        ItemStack currentTag = createItem(
            Material.OAK_SIGN,
            ColorUtils.colorize("&eTag Atual"),
            ColorUtils.colorize("&7" + (guild.getTag() != null ? "[" + guild.getTag() + "]" : "Sem Tag"))
        );
        inventory.setItem(13, currentTag);
        
        // Configurações de permissão atuais
        ItemStack currentPermissions = createItem(
            Material.SHIELD,
            ColorUtils.colorize("&eConfigurações de Permissão Atuais"),
            ColorUtils.colorize("&7Líder: Todas as permissões"),
            ColorUtils.colorize("&7Oficial: Convidar, Expulsar"),
            ColorUtils.colorize("&7Membro: Permissões básicas")
        );
        inventory.setItem(17, currentPermissions);
    }
    
    /**
     * Processa alteração de nome
     */
    private void handleChangeName(Player player) {
        // Verifica permissão (apenas o líder pode alterar o nome)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // Abre GUI de entrada de nome
        plugin.getGuiManager().openGUI(player, new GuildNameInputGUI(plugin, guild, player));
    }
    
    /**
     * Processa alteração de descrição
     */
    private void handleChangeDescription(Player player) {
        // Verifica permissão (apenas o líder pode alterar a descrição)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // Abre GUI de entrada de descrição
        plugin.getGuiManager().openGUI(player, new GuildDescriptionInputGUI(plugin, guild));
    }
    
    /**
     * Processa alteração de tag
     */
    private void handleChangeTag(Player player) {
        // Verifica permissão (apenas o líder pode alterar a tag)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // Abre GUI de entrada de tag
        plugin.getGuiManager().openGUI(player, new GuildTagInputGUI(plugin, guild));
    }
    
    /**
     * Processa configurações de permissão
     */
    private void handlePermissions(Player player) {
        // Verifica permissão (apenas o líder pode gerenciar permissões)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // Abre GUI de configurações de permissão
        plugin.getGuiManager().openGUI(player, new GuildPermissionsGUI(plugin, guild));
    }
    
    /**
     * Processa convite de membro
     */
    private void handleInviteMember(Player player) {
        // Verifica permissão (oficial ou líder pode convidar membros)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.officer-or-higher", "&cRequer cargo de Oficial ou superior");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // Abre GUI de convite de membro
        plugin.getGuiManager().openGUI(player, new InviteMemberGUI(plugin, guild));
    }
    
    /**
     * Processa expulsão de membro
     */
    private void handleKickMember(Player player) {
        // Verifica permissão (oficial ou líder pode expulsar membros)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.officer-or-higher", "&cRequer cargo de Oficial ou superior");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // Abre GUI de expulsão de membro
        plugin.getGuiManager().openGUI(player, new KickMemberGUI(plugin, guild));
    }
    
    /**
     * Processa promoção de membro
     */
    private void handlePromoteMember(Player player) {
        // Verifica permissão (apenas o líder pode promover membros)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // Abre GUI de promoção de membro
        plugin.getGuiManager().openGUI(player, new PromoteMemberGUI(plugin, guild));
    }
    
    /**
     * Processa rebaixamento de membro
     */
    private void handleDemoteMember(Player player) {
        // Verifica permissão (apenas o líder pode rebaixar membros)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // Abre GUI de rebaixamento de membro
        plugin.getGuiManager().openGUI(player, new DemoteMemberGUI(plugin, guild));
    }
    
    /**
     * Processa gerenciamento de solicitações
     */
    private void handleApplications(Player player) {
        // Verifica permissão (oficial ou líder pode processar solicitações)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.officer-or-higher", "&cRequer cargo de Oficial ou superior");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // Abre GUI de gerenciamento de solicitações
        plugin.getGuiManager().openGUI(player, new ApplicationManagementGUI(plugin, guild));
    }
    
    /**
     * Processa gerenciamento de relações da guilda
     */
    private void handleRelations(Player player) {
        // Verifica permissão (apenas o líder pode gerenciar relações)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("relation.only-leader", "&cApenas o líder da guilda pode gerenciar relações!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // Abre GUI de gerenciamento de relações da guilda
        plugin.getGuiManager().openGUI(player, new GuildRelationsGUI(plugin, guild, player));
    }
    
    /**
     * Processa visualização de logs da guilda
     */
    private void handleGuildLogs(Player player) {
        // Verifica permissão (membros da guilda podem ver logs)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&cSem permissão");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // Abre GUI de logs da guilda
        plugin.getGuiManager().openGUI(player, new GuildLogsGUI(plugin, guild, player));
    }
    
    /**
     * Processa saída da guilda
     */
    private void handleLeaveGuild(Player player) {
        // Abre GUI de confirmação de saída
        plugin.getGuiManager().openGUI(player, new ConfirmLeaveGuildGUI(plugin, guild));
    }
    
    /**
     * Processa exclusão da guilda
     */
    private void handleDeleteGuild(Player player) {
        // Verifica permissão (apenas o líder pode excluir a guilda)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // Abre GUI de confirmação de exclusão
        plugin.getGuiManager().openGUI(player, new ConfirmDeleteGuildGUI(plugin, guild));
    }
    

    
    /**
     * Cria item
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
