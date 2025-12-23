package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * GUI de Promoção de Membros
 */
public class PromoteMemberGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    private int currentPage = 0;
    private List<GuildMember> members;
    
    public PromoteMemberGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
        // Obtém lista de membros na inicialização
        this.members = List.of();
        loadMembers();
    }
    
    private void loadMembers() {
        plugin.getGuildService().getGuildMembersAsync(guild.getId()).thenAccept(memberList -> {
            this.members = memberList.stream()
                .filter(member -> !member.getPlayerUuid().equals(guild.getLeaderUuid()))
                .filter(member -> !member.getRole().equals(GuildMember.Role.OFFICER)) // Mostra apenas membros que podem ser promovidos
                .collect(java.util.stream.Collectors.toList());
        });
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6Promover Membro - Pág " + (currentPage + 1) + "");
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // Preenche a borda
        fillBorder(inventory);
        
        // Mostra lista de membros
        displayMembers(inventory);
        
        // Adiciona botões de navegação
        setupNavigationButtons(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot >= 9 && slot < 45) {
            // Área de cabeças dos membros
            int memberIndex = slot - 9 + (currentPage * 36);
            if (memberIndex < members.size()) {
                GuildMember member = members.get(memberIndex);
                handlePromoteMember(player, member);
            }
        } else if (slot == 45) {
            // Página anterior
            if (currentPage > 0) {
                currentPage--;
                plugin.getGuiManager().refreshGUI(player);
            }
        } else if (slot == 53) {
            // Próxima página
            int maxPage = (members.size() - 1) / 36;
            if (currentPage < maxPage) {
                currentPage++;
                plugin.getGuiManager().refreshGUI(player);
            }
        } else if (slot == 49) {
            // Voltar
            plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild));
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
     * Mostra lista de membros
     */
    private void displayMembers(Inventory inventory) {
        int startIndex = currentPage * 36;
        int endIndex = Math.min(startIndex + 36, members.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            GuildMember member = members.get(i);
            int slot = 9 + (i - startIndex);
            
            ItemStack memberHead = createMemberHead(member);
            inventory.setItem(slot, memberHead);
        }
    }
    
    /**
     * Configura botões de navegação
     */
    private void setupNavigationButtons(Inventory inventory) {
        // Botão de página anterior
        if (currentPage > 0) {
            ItemStack prevPage = createItem(
                Material.ARROW,
                ColorUtils.colorize("&ePágina Anterior"),
                ColorUtils.colorize("&7Clique para ver a página anterior")
            );
            inventory.setItem(45, prevPage);
        }
        
        // Botão de próxima página
        int maxPage = (members.size() - 1) / 36;
        if (currentPage < maxPage) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize("&ePróxima Página"),
                ColorUtils.colorize("&7Clique para ver a próxima página")
            );
            inventory.setItem(53, nextPage);
        }
        
        // Botão de voltar
        ItemStack back = createItem(
            Material.BARRIER,
            ColorUtils.colorize("&cVoltar"),
            ColorUtils.colorize("&7Voltar para configurações da guilda")
        );
        inventory.setItem(49, back);
    }
    
    /**
     * Cria cabeça do membro
     */
    private ItemStack createMemberHead(GuildMember member) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&6" + member.getPlayerName()));
            meta.setLore(Arrays.asList(
                ColorUtils.colorize("&7Cargo Atual: &e" + member.getRole().getDisplayName()),
                ColorUtils.colorize("&7Entrou em: &e" + member.getJoinedAt()),
                ColorUtils.colorize("&6Clique para promover a Oficial")
            ));
            head.setItemMeta(meta);
        }
        
        return head;
    }
    
    /**
     * Processa promoção de membro
     */
    private void handlePromoteMember(Player promoter, GuildMember member) {
        // Verifica permissão
        if (!promoter.hasPermission("guild.promote")) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&cPermissão insuficiente");
            promoter.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // Promove membro
        plugin.getGuildService().updateMemberRoleAsync(member.getPlayerUuid(), GuildMember.Role.OFFICER, promoter.getUniqueId()).thenAccept(success -> {
            if (success) {
                String promoterMessage = plugin.getConfigManager().getMessagesConfig().getString("promote.success", "&a{player} promovido a Oficial!")
                    .replace("{player}", member.getPlayerName());
                promoter.sendMessage(ColorUtils.colorize(promoterMessage));
                
                // Notifica o jogador promovido
                Player promotedPlayer = plugin.getServer().getPlayer(member.getPlayerUuid());
                if (promotedPlayer != null) {
                    String promotedMessage = plugin.getConfigManager().getMessagesConfig().getString("promote.promoted", "&aVocê foi promovido a Oficial da guilda {guild}!")
                        .replace("{guild}", guild.getName());
                    promotedPlayer.sendMessage(ColorUtils.colorize(promotedMessage));
                }
                
                // Atualiza GUI
                plugin.getGuiManager().openGUI(promoter, new PromoteMemberGUI(plugin, guild));
            } else {
                String message = plugin.getConfigManager().getMessagesConfig().getString("promote.failed", "&cFalha ao promover membro!");
                promoter.sendMessage(ColorUtils.colorize(message));
            }
        });
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
