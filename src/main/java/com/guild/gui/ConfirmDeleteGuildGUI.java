package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * GUI de confirmação de exclusão de guilda
 */
public class ConfirmDeleteGuildGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    
    public ConfirmDeleteGuildGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize("&4Confirmar Exclusão da Guilda");
    }
    
    @Override
    public int getSize() {
        return 27;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // Preencher bordas
        fillBorder(inventory);
        
        // Exibir informações de confirmação
        displayConfirmInfo(inventory);
        
        // Adicionar botões de confirmação e cancelamento
        setupButtons(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 11: // Confirmar exclusão
                handleConfirmDelete(player);
                break;
            case 15: // Cancelar
                handleCancel(player);
                break;
        }
    }
    
    /**
     * Preencher bordas
     */
    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 18, border);
        }
        for (int i = 9; i < 18; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }
    
    /**
     * Exibir informações de confirmação
     */
    private void displayConfirmInfo(Inventory inventory) {
        ItemStack info = createItem(
            Material.BOOK,
            ColorUtils.colorize("&4Confirmar Exclusão da Guilda"),
            ColorUtils.colorize("&7Guilda: &e" + guild.getName()),
            ColorUtils.colorize("&7Tem certeza que deseja excluir esta guilda?"),
            ColorUtils.colorize("&cEsta operação excluirá permanentemente a guilda!"),
            ColorUtils.colorize("&cTodos os membros serão removidos!"),
            ColorUtils.colorize("&cEsta operação é irreversível!")
        );
        inventory.setItem(13, info);
    }
    
    /**
     * Configurar botões
     */
    private void setupButtons(Inventory inventory) {
        // Botão de confirmar exclusão
        ItemStack confirm = createItem(
            Material.TNT,
            ColorUtils.colorize("&4Confirmar Exclusão"),
            ColorUtils.colorize("&7Clique para confirmar a exclusão"),
            ColorUtils.colorize("&cEsta operação é irreversível!")
        );
        inventory.setItem(11, confirm);
        
        // Botão de cancelar
        ItemStack cancel = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize("&aCancelar"),
            ColorUtils.colorize("&7Cancelar exclusão da guilda")
        );
        inventory.setItem(15, cancel);
    }
    
    /**
     * Tratar confirmação de exclusão
     */
    private void handleConfirmDelete(Player player) {
        // Verificar permissões (apenas o líder da guilda atual pode excluir)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getGuildId() != guild.getId() || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta operação");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // Excluir guilda
        plugin.getGuildService().deleteGuildAsync(guild.getId(), player.getUniqueId()).thenAccept(success -> {
            if (success) {
                String template = plugin.getConfigManager().getMessagesConfig().getString("delete.success", "&aGuilda &e{guild} &afoi excluída!");
                // Voltar para a thread principal para operações de interface
                CompatibleScheduler.runTask(plugin, () -> {
                    String rendered = ColorUtils.replaceWithColorIsolation(template, "{guild}", guild.getName());
                    player.sendMessage(rendered);
                    // Usar GUIManager para garantir fechamento e abertura seguros na thread principal
                    plugin.getGuiManager().closeGUI(player);
                    plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
                });
            } else {
                String message = plugin.getConfigManager().getMessagesConfig().getString("delete.failed", "&cFalha ao excluir guilda!");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    /**
     * Tratar cancelamento
     */
    private void handleCancel(Player player) {
        // Voltar para GUI de configurações da guilda
        plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild));
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
