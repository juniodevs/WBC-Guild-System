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
 * GUI de Rebaixamento de Membro
 */
public class DemoteMemberGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    private int currentPage = 0;
    private List<GuildMember> members;
    
    public DemoteMemberGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
        // Inicializar lista de membros
        this.members = List.of();
        loadMembers();
    }
    
    private void loadMembers() {
        plugin.getGuildService().getGuildMembersAsync(guild.getId()).thenAccept(memberList -> {
            this.members = memberList.stream()
                .filter(member -> !member.getPlayerUuid().equals(guild.getLeaderUuid()))
                .filter(member -> member.getRole().equals(GuildMember.Role.OFFICER)) // Mostrar apenas oficiais
                .collect(java.util.stream.Collectors.toList());
        });
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6Rebaixar Membro - Pág " + (currentPage + 1) + "");
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // Preencher borda
        fillBorder(inventory);
        
        // Exibir lista de membros
        displayMembers(inventory);
        
        // Adicionar botões de navegação
        setupNavigationButtons(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot >= 9 && slot < 45) {
            // Área de cabeças dos membros
            int memberIndex = slot - 9 + (currentPage * 36);
            if (memberIndex < members.size()) {
                GuildMember member = members.get(memberIndex);
                handleDemoteMember(player, member);
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
     * Exibir lista de membros
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
     * Configurar botões de navegação
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
            ColorUtils.colorize("&7Voltar para Configurações da Guilda")
        );
        inventory.setItem(49, back);
    }
    
    /**
     * Criar cabeça do membro
     */
    private ItemStack createMemberHead(GuildMember member) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&7" + member.getPlayerName()));
            meta.setLore(Arrays.asList(
                ColorUtils.colorize("&7Cargo Atual: &e" + member.getRole().getDisplayName()),
                ColorUtils.colorize("&7Entrou em: &e" + member.getJoinedAt()),
                ColorUtils.colorize("&7Clique para rebaixar para Membro")
            ));
            head.setItemMeta(meta);
        }
        
        return head;
    }
    
    /**
     * Tratar rebaixamento de membro
     */
    private void handleDemoteMember(Player demoter, GuildMember member) {
        // Verificar permissão
        if (!demoter.hasPermission("guild.demote")) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&cSem permissão");
            demoter.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // Rebaixar membro
        plugin.getGuildService().updateMemberRoleAsync(member.getPlayerUuid(), GuildMember.Role.MEMBER, demoter.getUniqueId()).thenAccept(success -> {
            if (success) {
                String demoterMessage = plugin.getConfigManager().getMessagesConfig().getString("demote.success", "&a{player} rebaixado para Membro!")
                    .replace("{player}", member.getPlayerName());
                demoter.sendMessage(ColorUtils.colorize(demoterMessage));
                
                // Notificar jogador rebaixado
                Player demotedPlayer = plugin.getServer().getPlayer(member.getPlayerUuid());
                if (demotedPlayer != null) {
                    String demotedMessage = plugin.getConfigManager().getMessagesConfig().getString("demote.demoted", "&cVocê foi rebaixado para Membro na guilda {guild}!")
                        .replace("{guild}", guild.getName());
                    demotedPlayer.sendMessage(ColorUtils.colorize(demotedMessage));
                }
                
                // Atualizar GUI
                plugin.getGuiManager().openGUI(demoter, new DemoteMemberGUI(plugin, guild));
            } else {
                String message = plugin.getConfigManager().getMessagesConfig().getString("demote.failed", "&cFalha ao rebaixar membro!");
                demoter.sendMessage(ColorUtils.colorize(message));
            }
        });
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
