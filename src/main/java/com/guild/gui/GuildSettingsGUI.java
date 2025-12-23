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
 * 工会设置GUI
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
        // 填充边框
        fillBorder(inventory);
        
        // 添加设置按钮
        setupSettingsButtons(inventory);
        
        // 添加功能按钮
        setupFunctionButtons(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 11: // Modificar nome
                handleChangeName(player);
                break;
            case 12: // Modificar descrição
                handleChangeDescription(player);
                break;
            case 13: // Modificar tag
                handleChangeTag(player);
                break;
            case 14: // Configurar permissões
                handlePermissions(player);
                break;
            case 19: // Convidar membro
                handleInviteMember(player);
                break;
            case 21: // Expulsar membro
                handleKickMember(player);
                break;
            case 23: // Promover membro
                handlePromoteMember(player);
                break;
            case 25: // Rebaixar membro
                handleDemoteMember(player);
                break;
            case 30: // Gerenciar solicitações
                handleApplications(player);
                break;
            case 31: // Gerenciar relações
                handleRelations(player);
                break;
            case 32: // Logs da guilda
                handleGuildLogs(player);
                break;
            case 34: // Sair da guilda
                handleLeaveGuild(player);
                break;
            case 28: // Excluir guilda
                handleDeleteGuild(player);
                break;
            case 49: // Voltar
                plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
                break;
        }
    }
    
    /**
     * 填充边框
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
     * 设置设置按钮
     */
    private void setupSettingsButtons(Inventory inventory) {
        // 修改名称按钮
        ItemStack changeName = createItem(
            Material.NAME_TAG,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-name.name", "&eAlterar Nome")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-name.lore.1", "&7Alterar nome da guilda"))
        );
        inventory.setItem(11, changeName);
        
        // 修改描述按钮
        ItemStack changeDescription = createItem(
            Material.BOOK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-description.name", "&eAlterar Descrição")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-description.lore.1", "&7Alterar descrição da guilda"))
        );
        inventory.setItem(12, changeDescription);
        
        // 修改标签按钮
        ItemStack changeTag = createItem(
            Material.OAK_SIGN,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-tag.name", "&eAlterar Tag")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-tag.lore.1", "&7Alterar tag da guilda"))
        );
        inventory.setItem(13, changeTag);
        
        // 权限设置按钮
        ItemStack permissions = createItem(
            Material.SHIELD,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.permissions.name", "&eConfigurar Permissões")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.permissions.lore.1", "&7Gerenciar permissões de membros"))
        );
        inventory.setItem(14, permissions);
    }
    
    /**
     * 设置功能按钮
     */
    private void setupFunctionButtons(Inventory inventory) {
        // 邀请成员按钮
        ItemStack inviteMember = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.invite-member", "&aConvidar Membro")),
            ColorUtils.colorize("&7Convidar novos membros para a guilda")
        );
        inventory.setItem(19, inviteMember);
        
        // 踢出成员按钮
        ItemStack kickMember = createItem(
            Material.REDSTONE,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.kick-member", "&cExpulsar Membro")),
            ColorUtils.colorize("&7Expulsar membro da guilda")
        );
        inventory.setItem(21, kickMember);
        
        // 提升成员按钮
        ItemStack promoteMember = createItem(
            Material.GOLD_INGOT,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.promote-member", "&6Promover Membro")),
            ColorUtils.colorize("&7Promover cargo do membro")
        );
        inventory.setItem(23, promoteMember);
        
        // 降级成员按钮
        ItemStack demoteMember = createItem(
            Material.IRON_INGOT,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.demote-member", "&7Rebaixar Membro")),
            ColorUtils.colorize("&7Rebaixar cargo do membro")
        );
        inventory.setItem(25, demoteMember);
        
        // 处理申请按钮
        ItemStack applications = createItem(
            Material.PAPER,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.application-management", "&eGerenciar Solicitações")),
            ColorUtils.colorize("&7Processar solicitações de entrada")
        );
        inventory.setItem(30, applications);
        
        // 工会关系管理按钮
        ItemStack relations = createItem(
            Material.RED_WOOL,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-relations-management.name", "&eGerenciar Relações")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-relations-management.lore.1", "&7Gerenciar relações da guilda")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-relations-management.lore.2", "&7Aliados, Inimigos, etc."))
        );
        inventory.setItem(31, relations);
        
        // 工会日志按钮
        ItemStack guildLogs = createItem(
            Material.BOOK,
            ColorUtils.colorize("&6Logs da Guilda"),
            ColorUtils.colorize("&7Ver histórico de operações da guilda"),
            ColorUtils.colorize("&7Registra todas as operações importantes")
        );
        inventory.setItem(32, guildLogs);
        
        // 离开工会按钮
        ItemStack leaveGuild = createItem(
            Material.BARRIER,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.leave-guild", "&cSair da Guilda")),
            ColorUtils.colorize("&7Sair da guilda atual")
        );
        inventory.setItem(34, leaveGuild);
        
        // 删除工会按钮
        ItemStack deleteGuild = createItem(
            Material.TNT,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.delete-guild", "&4Excluir Guilda")),
            ColorUtils.colorize("&7Excluir a guilda inteira"),
            ColorUtils.colorize("&cEsta operação é irreversível!")
        );
        inventory.setItem(28, deleteGuild);
        
        // 返回按钮
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.back.name", "&7Voltar")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.back.lore.1", "&7Voltar ao menu principal"))
        );
        inventory.setItem(49, back);
    }
    

    
    /**
     * 处理修改名称
     */
    private void handleChangeName(Player player) {
        // 检查权限（只有会长可以修改名称）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开名称输入GUI
        plugin.getGuiManager().openGUI(player, new GuildNameInputGUI(plugin, guild, player));
    }
    
    /**
     * 处理修改描述
     */
    private void handleChangeDescription(Player player) {
        // 检查权限（只有会长可以修改描述）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开描述输入GUI
        plugin.getGuiManager().openGUI(player, new GuildDescriptionInputGUI(plugin, guild));
    }
    
    /**
     * 处理修改标签
     */
    private void handleChangeTag(Player player) {
        // 检查权限（只有会长可以修改标签）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开标签输入GUI
        plugin.getGuiManager().openGUI(player, new GuildTagInputGUI(plugin, guild));
    }
    
    /**
     * 处理权限设置
     */
    private void handlePermissions(Player player) {
        // 检查权限（只有会长可以管理权限）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开权限设置GUI
        plugin.getGuiManager().openGUI(player, new GuildPermissionsGUI(plugin, guild));
    }
    
    /**
     * 处理邀请成员
     */
    private void handleInviteMember(Player player) {
        // 检查权限（官员或会长可以邀请成员）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.officer-or-higher", "&cRequer cargo de Oficial ou superior");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开邀请成员GUI
        plugin.getGuiManager().openGUI(player, new InviteMemberGUI(plugin, guild));
    }
    
    /**
     * 处理踢出成员
     */
    private void handleKickMember(Player player) {
        // 检查权限（官员或会长可以踢出成员）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.officer-or-higher", "&cRequer cargo de Oficial ou superior");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开踢出成员GUI
        plugin.getGuiManager().openGUI(player, new KickMemberGUI(plugin, guild));
    }
    
    /**
     * 处理提升成员
     */
    private void handlePromoteMember(Player player) {
        // 检查权限（只有会长可以提升成员）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开提升成员GUI
        plugin.getGuiManager().openGUI(player, new PromoteMemberGUI(plugin, guild));
    }
    
    /**
     * 处理降级成员
     */
    private void handleDemoteMember(Player player) {
        // 检查权限（只有会长可以降级成员）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开降级成员GUI
        plugin.getGuiManager().openGUI(player, new DemoteMemberGUI(plugin, guild));
    }
    
    /**
     * 处理申请管理
     */
    private void handleApplications(Player player) {
        // 检查权限（官员或会长可以处理申请）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.officer-or-higher", "&cRequer cargo de Oficial ou superior");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开申请管理GUI
        plugin.getGuiManager().openGUI(player, new ApplicationManagementGUI(plugin, guild));
    }
    
    /**
     * 处理工会关系管理
     */
    private void handleRelations(Player player) {
        // 检查权限（只有会长可以管理关系）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("relation.only-leader", "&cApenas o líder da guilda pode gerenciar relações!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开工会关系管理GUI
        plugin.getGuiManager().openGUI(player, new GuildRelationsGUI(plugin, guild, player));
    }
    
    /**
     * 处理工会日志查看
     */
    private void handleGuildLogs(Player player) {
        // 检查权限（工会成员可以查看日志）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&cSem permissão");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开工会日志GUI
        plugin.getGuiManager().openGUI(player, new GuildLogsGUI(plugin, guild, player));
    }
    
    /**
     * 处理离开工会
     */
    private void handleLeaveGuild(Player player) {
        // 打开确认离开GUI
        plugin.getGuiManager().openGUI(player, new ConfirmLeaveGuildGUI(plugin, guild));
    }
    
    /**
     * 处理删除工会
     */
    private void handleDeleteGuild(Player player) {
        // 检查权限（只有会长可以删除工会）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开确认删除GUI
        plugin.getGuiManager().openGUI(player, new ConfirmDeleteGuildGUI(plugin, guild));
    }
    

    
    /**
     * 创建物品
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
