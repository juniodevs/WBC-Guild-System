package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * GUI de Gerenciamento de Membros
 */
public class MemberManagementGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    private int currentPage = 0;
    private static final int MEMBERS_PER_PAGE = 28; // 4 linhas 7 colunas, excluindo borda
    
    public MemberManagementGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.title", "&6Gerenciamento de Membros"));
    }
    
    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("member-management.size", 54);
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // Preenche a borda
        fillBorder(inventory);
        
        // Adiciona botões de função
        setupFunctionButtons(inventory);
        
        // Carrega lista de membros
        loadMembers(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // Verifica se é botão de função
        if (isFunctionButton(slot)) {
            handleFunctionButton(player, slot);
            return;
        }
        
        // Verifica se é botão de paginação
        if (isPaginationButton(slot)) {
            handlePaginationButton(player, slot);
            return;
        }
        
        // Verifica se é botão de membro
        if (isMemberSlot(slot)) {
            handleMemberClick(player, slot, clickedItem, clickType);
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
     * Configura botões de função
     */
    private void setupFunctionButtons(Inventory inventory) {
        // Botão de convidar membro
        ItemStack inviteMember = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.invite-member.name", "&aConvidar Membro")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.invite-member.lore.1", "&7Convidar novos membros"))
        );
        inventory.setItem(45, inviteMember);
        
        // Botão de expulsar membro
        ItemStack kickMember = createItem(
            Material.REDSTONE_BLOCK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.kick-member.name", "&cExpulsar Membro")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.kick-member.lore.1", "&7Expulsar membro da guilda"))
        );
        inventory.setItem(47, kickMember);
        
        // Botão de promover membro
        ItemStack promoteMember = createItem(
            Material.GOLD_INGOT,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.promote-member.name", "&6Promover Membro")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.promote-member.lore.1", "&7Promover cargo de membro"))
        );
        inventory.setItem(49, promoteMember);
        
        // Botão de rebaixar membro
        ItemStack demoteMember = createItem(
            Material.IRON_INGOT,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.demote-member.name", "&7Rebaixar Membro")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.demote-member.lore.1", "&7Rebaixar cargo de membro"))
        );
        inventory.setItem(51, demoteMember);
        
        // Botão de voltar
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.back.name", "&7Voltar")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.back.lore.1", "&7Voltar ao menu principal"))
        );
        inventory.setItem(53, back);
    }
    
    /**
     * Carrega lista de membros
     */
    private void loadMembers(Inventory inventory) {
        plugin.getGuildService().getGuildMembersAsync(guild.getId()).thenAccept(members -> {
            if (members == null || members.isEmpty()) {
                // Mostra informação de sem membros
                ItemStack noMembers = createItem(
                    Material.BARRIER,
                    ColorUtils.colorize("&cSem membros"),
                    ColorUtils.colorize("&7A guilda ainda não tem membros")
                );
                inventory.setItem(22, noMembers);
                return;
            }
            
            // Calcula paginação
            int totalPages = (members.size() - 1) / MEMBERS_PER_PAGE;
            if (currentPage > totalPages) {
                currentPage = totalPages;
            }
            
            // Configura botões de paginação
            setupPaginationButtons(inventory, totalPages);
            
            // Mostra membros da página atual
            int startIndex = currentPage * MEMBERS_PER_PAGE;
            int endIndex = Math.min(startIndex + MEMBERS_PER_PAGE, members.size());
            
            int slotIndex = 10; // Começa da linha 2, coluna 2
            for (int i = startIndex; i < endIndex; i++) {
                GuildMember member = members.get(i);
                if (slotIndex >= 44) break; // Evita exceder área de exibição
                
                ItemStack memberItem = createMemberItem(member);
                inventory.setItem(slotIndex, memberItem);
                
                slotIndex++;
                if (slotIndex % 9 == 8) { // Pula borda
                    slotIndex += 2;
                }
            }
        });
    }
    
    /**
     * Configura botões de paginação
     */
    private void setupPaginationButtons(Inventory inventory, int totalPages) {
        // Botão de página anterior
        if (currentPage > 0) {
            ItemStack previousPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.previous-page.name", "&cPágina Anterior")),
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.previous-page.lore.1", "&7Ver página anterior"))
            );
            inventory.setItem(18, previousPage);
        }
        
        // Botão de próxima página
        if (currentPage < totalPages) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.next-page.name", "&aPróxima Página")),
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.next-page.lore.1", "&7Ver próxima página"))
            );
            inventory.setItem(26, nextPage);
        }
    }
    
    /**
     * Cria item de membro
     */
    private ItemStack createMemberItem(GuildMember member) {
        Material material;
        String name;
        List<String> lore = new ArrayList<>();
        
        switch (member.getRole()) {
            case LEADER:
                material = Material.GOLDEN_HELMET;
                name = PlaceholderUtils.replaceMemberPlaceholders("&c{member_name}", member, guild);
                lore.add(PlaceholderUtils.replaceMemberPlaceholders("&7Cargo: &c{member_role}", member, guild));
                break;
            case OFFICER:
                material = Material.GOLDEN_HELMET;
                name = PlaceholderUtils.replaceMemberPlaceholders("&6{member_name}", member, guild);
                lore.add(PlaceholderUtils.replaceMemberPlaceholders("&7Cargo: &6{member_role}", member, guild));
                break;
            default:
                material = Material.PLAYER_HEAD;
                name = PlaceholderUtils.replaceMemberPlaceholders("&f{member_name}", member, guild);
                lore.add(PlaceholderUtils.replaceMemberPlaceholders("&7Cargo: &f{member_role}", member, guild));
                break;
        }
        
        lore.add(PlaceholderUtils.replaceMemberPlaceholders("&7Entrou em: {member_join_time}", member, guild));
        lore.add(PlaceholderUtils.replaceMemberPlaceholders("&7Permissões: " + getRolePermissions(member.getRole()), member, guild));
        lore.add("");
        lore.add(ColorUtils.colorize("&aBotão Esquerdo: Ver Detalhes"));
        
        if (member.getRole() != GuildMember.Role.LEADER) {
            lore.add(ColorUtils.colorize("&cBotão Direito: Expulsar Membro"));
            lore.add(ColorUtils.colorize("&6Botão do Meio: Promover/Rebaixar"));
        }
        
        return createItem(material, name, lore.toArray(new String[0]));
    }
    
    /**
     * Obtém descrição de permissões do cargo
     */
    private String getRolePermissions(GuildMember.Role role) {
        switch (role) {
            case LEADER:
                return "Todas as permissões";
            case OFFICER:
                return "Convidar, Expulsar";
            default:
                return "Permissões básicas";
        }
    }
    
    /**
     * Verifica se é botão de função
     */
    private boolean isFunctionButton(int slot) {
        return slot == 45 || slot == 47 || slot == 49 || slot == 51 || slot == 53;
    }
    
    /**
     * Verifica se é botão de paginação
     */
    private boolean isPaginationButton(int slot) {
        return slot == 18 || slot == 26;
    }
    
    /**
     * Verifica se é slot de membro
     */
    private boolean isMemberSlot(int slot) {
        return slot >= 10 && slot <= 44 && slot % 9 != 0 && slot % 9 != 8;
    }
    
    /**
     * Processa clique em botão de função
     */
    private void handleFunctionButton(Player player, int slot) {
        switch (slot) {
            case 45: // Convidar membro
                handleInviteMember(player);
                break;
            case 47: // Expulsar membro
                handleKickMember(player);
                break;
            case 49: // Promover membro
                handlePromoteMember(player);
                break;
            case 51: // Rebaixar membro
                handleDemoteMember(player);
                break;
            case 53: // Voltar
                plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
                break;
        }
    }
    
    /**
     * Processa clique em botão de paginação
     */
    private void handlePaginationButton(Player player, int slot) {
        if (slot == 18) { // Página anterior
            if (currentPage > 0) {
                currentPage--;
                refreshInventory(player);
            }
        } else if (slot == 26) { // Próxima página
            currentPage++;
            refreshInventory(player);
        }
    }
    
    /**
     * Processa clique em membro
     */
    private void handleMemberClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // Obtém membro clicado
        int memberIndex = (currentPage * MEMBERS_PER_PAGE) + (slot - 10);
        if (memberIndex % 9 == 0 || memberIndex % 9 == 8) return; // Pula borda
        
        plugin.getGuildService().getGuildMembersAsync(guild.getId()).thenAccept(members -> {
            if (members != null && memberIndex < members.size()) {
                GuildMember member = members.get(memberIndex);
                
                if (clickType == ClickType.LEFT) {
                    // Ver detalhes do membro
                    showMemberDetails(player, member);
                } else if (clickType == ClickType.RIGHT) {
                    // Expulsar membro
                    handleKickMemberDirect(player, member);
                } else if (clickType == ClickType.MIDDLE) {
                    // Promover/Rebaixar membro
                    handlePromoteDemoteMember(player, member);
                }
            }
        });
    }
    
    /**
     * Mostra detalhes do membro
     */
    private void showMemberDetails(Player player, GuildMember member) {
        // Abre GUI de detalhes do membro
        plugin.getGuiManager().openGUI(player, new MemberDetailsGUI(plugin, guild, member, player));
    }
    
    /**
     * Expulsar membro diretamente
     */
    private void handleKickMemberDirect(Player player, GuildMember member) {
        // Verifica permissão
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(executor -> {
            if (executor == null || !executor.getRole().canKick()) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&cPermissão insuficiente");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // Não pode expulsar o líder
            if (member.getRole() == GuildMember.Role.LEADER) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.cannot-kick-leader", "&cNão é possível expulsar o líder da guilda");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // Confirmar expulsão
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.confirm-kick", "&cTem certeza que deseja expulsar {member}? Digite &f/guild kick {member} confirm &cpara confirmar")
                .replace("{member}", member.getPlayerName());
            player.sendMessage(ColorUtils.colorize(message));
        });
    }
    
    /**
     * Promover/Rebaixar membro
     */
    private void handlePromoteDemoteMember(Player player, GuildMember member) {
        // Verifica permissão
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(executor -> {
            if (executor == null || executor.getRole() != GuildMember.Role.LEADER) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta ação");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // Não pode modificar o líder
            if (member.getRole() == GuildMember.Role.LEADER) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.cannot-modify-leader", "&cNão é possível modificar o cargo do líder da guilda");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            if (member.getRole() == GuildMember.Role.OFFICER) {
                // Rebaixar para membro comum
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.confirm-demote", "&cTem certeza que deseja rebaixar {member}? Digite &f/guild demote {member} confirm &cpara confirmar")
                    .replace("{member}", member.getPlayerName());
                player.sendMessage(ColorUtils.colorize(message));
            } else {
                // Promover para oficial
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.confirm-promote", "&aTem certeza que deseja promover {member} a Oficial? Digite &f/guild promote {member} confirm &apara confirmar")
                    .replace("{member}", member.getPlayerName());
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    /**
     * Processa convite de membro
     */
    private void handleInviteMember(Player player) {
        // Verifica permissão
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
            if (member == null || !member.getRole().canInvite()) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&cPermissão insuficiente");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // Abre GUI de convite de membro
            InviteMemberGUI inviteMemberGUI = new InviteMemberGUI(plugin, guild);
            plugin.getGuiManager().openGUI(player, inviteMemberGUI);
        });
    }
    
    /**
     * Processa expulsão de membro
     */
    private void handleKickMember(Player player) {
        // Verifica permissão
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
            if (member == null || !member.getRole().canKick()) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&cPermissão insuficiente");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // Abre GUI de expulsão de membro
            KickMemberGUI kickMemberGUI = new KickMemberGUI(plugin, guild);
            plugin.getGuiManager().openGUI(player, kickMemberGUI);
        });
    }
    
    /**
     * Processa promoção de membro
     */
    private void handlePromoteMember(Player player) {
        // Verifica permissão
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
            if (member == null || member.getRole() != GuildMember.Role.LEADER) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta ação");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // Abre GUI de promoção de membro
            PromoteMemberGUI promoteMemberGUI = new PromoteMemberGUI(plugin, guild);
            plugin.getGuiManager().openGUI(player, promoteMemberGUI);
        });
    }
    
    /**
     * Processa rebaixamento de membro
     */
    private void handleDemoteMember(Player player) {
        // Verifica permissão
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
            if (member == null || member.getRole() != GuildMember.Role.LEADER) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta ação");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // Abre GUI de rebaixamento de membro
            DemoteMemberGUI demoteMemberGUI = new DemoteMemberGUI(plugin, guild);
            plugin.getGuiManager().openGUI(player, demoteMemberGUI);
        });
    }
    
    /**
     * Atualiza inventário
     */
    private void refreshInventory(Player player) {
        plugin.getGuiManager().refreshGUI(player);
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
