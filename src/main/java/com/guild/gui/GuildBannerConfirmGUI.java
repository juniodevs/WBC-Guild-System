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
import com.guild.models.Guild;
import com.guild.models.GuildLog;
import com.guild.models.GuildMember;

/**
 * GUI de confirmação para definir o banner da guilda
 */
public class GuildBannerConfirmGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    private final ItemStack banner;
    
    public GuildBannerConfirmGUI(GuildPlugin plugin, Guild guild, ItemStack banner) {
        this.plugin = plugin;
        this.guild = guild;
        this.banner = banner;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6Confirmar Estandarte");
    }
    
    @Override
    public int getSize() {
        return 27;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // Preencher bordas
        fillBorder(inventory);
        
        // Exibir preview do banner
        displayBannerPreview(inventory);
        
        // Adicionar botões de confirmação e cancelamento
        setupButtons(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 11: // Confirmar
                handleConfirm(player);
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
     * Exibir preview do banner
     */
    private void displayBannerPreview(Inventory inventory) {
        ItemStack preview = banner.clone();
        ItemMeta meta = preview.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&6Preview do Estandarte"));
            preview.setItemMeta(meta);
        }
        
        inventory.setItem(13, preview);
    }
    
    /**
     * Configurar botões
     */
    private void setupButtons(Inventory inventory) {
        // Botão de confirmar
        ItemStack confirm = createItem(
            Material.LIME_WOOL,
            ColorUtils.colorize("&aConfirmar"),
            ColorUtils.colorize("&7Definir este estandarte para a guilda")
        );
        inventory.setItem(11, confirm);
        
        // Botão de cancelar
        ItemStack cancel = createItem(
            Material.RED_WOOL,
            ColorUtils.colorize("&cCancelar"),
            ColorUtils.colorize("&7Voltar sem salvar alterações")
        );
        inventory.setItem(15, cancel);
    }
    
    /**
     * Processar confirmação
     */
    private void handleConfirm(Player player) {
        // Verificar se o jogador é líder
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            player.sendMessage(ColorUtils.colorize(
                plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", 
                "&cApenas o líder da guilda pode realizar esta operação")));
            player.closeInventory();
            return;
        }
        
        // Atualizar banner da guilda
        plugin.getGuildService().updateGuildBannerAsync(guild.getId(), banner).thenAccept(success -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (success) {
                    // Atualizar o objeto guild
                    guild.setBanner(banner);
                    
                    // Registrar log
                    plugin.getGuildService().logGuildActionAsync(
                        guild.getId(), 
                        guild.getName(), 
                        player.getUniqueId().toString(), 
                        player.getName(),
                        GuildLog.LogType.GUILD_RENAMED, 
                        "Estandarte Alterado", 
                        "O estandarte da guilda foi alterado"
                    );
                    
                    player.sendMessage(ColorUtils.colorize("&aEstandarte da guilda alterado com sucesso!"));
                    plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, 1));
                } else {
                    player.sendMessage(ColorUtils.colorize("&cErro ao alterar o estandarte da guilda."));
                    player.closeInventory();
                }
            });
        });
    }
    
    /**
     * Processar cancelamento
     */
    private void handleCancel(Player player) {
        plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, 1));
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
    
    @Override
    public void onClose(Player player) {
        // Processamento ao fechar
    }
    
    @Override
    public void refresh(Player player) {
        if (player.isOnline()) {
            setupInventory(player.getOpenInventory().getTopInventory());
        }
    }
}
