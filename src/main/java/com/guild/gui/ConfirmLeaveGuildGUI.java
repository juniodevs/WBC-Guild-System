package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * GUI de confirmação de saída da guilda
 */
public class ConfirmLeaveGuildGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    
    public ConfirmLeaveGuildGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize("&cConfirmar Saída da Guilda");
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
            case 11: // Confirmar saída
                handleConfirmLeave(player);
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
            ColorUtils.colorize("&cConfirmar Saída da Guilda"),
            ColorUtils.colorize("&7Guilda: &e" + guild.getName()),
            ColorUtils.colorize("&7Tem certeza que deseja sair desta guilda?"),
            ColorUtils.colorize("&cEsta operação é irreversível!")
        );
        inventory.setItem(13, info);
    }
    
    /**
     * Configurar botões
     */
    private void setupButtons(Inventory inventory) {
        // Botão de confirmar saída
        ItemStack confirm = createItem(
            Material.REDSTONE_BLOCK,
            ColorUtils.colorize("&cConfirmar Saída"),
            ColorUtils.colorize("&7Clique para confirmar a saída")
        );
        inventory.setItem(11, confirm);
        
        // Botão de cancelar
        ItemStack cancel = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize("&aCancelar"),
            ColorUtils.colorize("&7Cancelar saída da guilda")
        );
        inventory.setItem(15, cancel);
    }
    
    /**
     * Tratar confirmação de saída
     */
    private void handleConfirmLeave(Player player) {
        // Verificar se é o líder
        if (player.getUniqueId().equals(guild.getLeaderUuid())) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("leave.leader-cannot-leave", "&cO líder da guilda não pode sair! Transfira a liderança ou exclua a guilda primeiro.");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // Sair da guilda
        plugin.getGuildService().removeGuildMemberAsync(player.getUniqueId(), player.getUniqueId()).thenAccept(success -> {
            if (success) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("leave.success", "&aVocê saiu da guilda &e{guild}&a com sucesso!")
                    .replace("{guild}", guild.getName());
                player.sendMessage(ColorUtils.colorize(message));
                
                // Fechar GUI
                player.closeInventory();
            } else {
                String message = plugin.getConfigManager().getMessagesConfig().getString("leave.failed", "&cFalha ao sair da guilda!");
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
