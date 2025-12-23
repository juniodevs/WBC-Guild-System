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
import java.util.concurrent.CompletableFuture;

/**
 * GUI de Entrada de Tag da Guilda
 */
public class GuildTagInputGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    private String currentTag;
    
    public GuildTagInputGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
        this.currentTag = guild.getTag() != null ? guild.getTag() : "";
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6Modificar Tag da Guilda");
    }
    
    @Override
    public int getSize() {
        return 27;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // Preenche a borda
        fillBorder(inventory);
        
        // Mostra tag atual
        displayCurrentTag(inventory);
        
        // Adiciona botões de ação
        setupButtons(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 11: // Inserir tag
                handleInputTag(player);
                break;
            case 15: // Confirmar
                handleConfirm(player);
                break;
            case 13: // Cancelar
                handleCancel(player);
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
            inventory.setItem(i + 18, border);
        }
        for (int i = 9; i < 18; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }
    
    /**
     * Mostra tag atual
     */
    private void displayCurrentTag(Inventory inventory) {
        ItemStack currentTagItem = createItem(
            Material.OAK_SIGN,
            ColorUtils.colorize("&eTag Atual"),
            ColorUtils.colorize("&7" + (currentTag.isEmpty() ? "Sem Tag" : "[" + currentTag + "]"))
        );
        inventory.setItem(11, currentTagItem);
    }
    
    /**
     * Configura botões
     */
    private void setupButtons(Inventory inventory) {
        // Botão de confirmar
        ItemStack confirm = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize("&aConfirmar Modificação"),
            ColorUtils.colorize("&7Confirmar modificação da tag da guilda")
        );
        inventory.setItem(15, confirm);
        
        // Botão de cancelar
        ItemStack cancel = createItem(
            Material.REDSTONE_BLOCK,
            ColorUtils.colorize("&cCancelar"),
            ColorUtils.colorize("&7Cancelar modificação")
        );
        inventory.setItem(13, cancel);
    }
    
    /**
     * Processa entrada de tag
     */
    private void handleInputTag(Player player) {
        // Fecha GUI
        player.closeInventory();
        
        // Envia mensagem solicitando entrada
        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.input-tag", "&aPor favor, digite a nova tag da guilda no chat (máximo 10 caracteres):");
        player.sendMessage(ColorUtils.colorize(message));
        
        // Define jogador em modo de entrada
        plugin.getGuiManager().setInputMode(player, input -> {
            if (input.length() > 10) {
                String errorMessage = plugin.getConfigManager().getMessagesConfig().getString("gui.tag-too-long", "&cTag muito longa, máximo 10 caracteres!");
                player.sendMessage(ColorUtils.colorize(errorMessage));
                return false;
            }
            
            // Atualiza tag
            currentTag = input;
            
            // Salva no banco de dados
            plugin.getGuildService().updateGuildAsync(guild.getId(), guild.getName(), input, guild.getDescription(), player.getUniqueId()).thenAccept(success -> {
                if (success) {
                    String successMessage = plugin.getConfigManager().getMessagesConfig().getString("gui.tag-updated", "&aTag da guilda atualizada!");
                    player.sendMessage(ColorUtils.colorize(successMessage));
                    
                    // Atualiza GUI com segurança
                    plugin.getGuiManager().refreshGUI(player);
                } else {
                    String errorMessage = plugin.getConfigManager().getMessagesConfig().getString("gui.tag-update-failed", "&cFalha ao atualizar tag da guilda!");
                    player.sendMessage(ColorUtils.colorize(errorMessage));
                }
            });
            
            return true;
        });
    }
    
    /**
     * Processa confirmação
     */
    private void handleConfirm(Player player) {
        // Retorna para GUI de configurações da guilda
        plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild));
    }
    
    /**
     * Processa cancelamento
     */
    private void handleCancel(Player player) {
        // Retorna para GUI de configurações da guilda
        plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild));
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
