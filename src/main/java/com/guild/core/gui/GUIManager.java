package com.guild.core.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.guild.GuildPlugin;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.gui.GuildNameInputGUI;

public class GUIManager implements Listener {
    
    private final GuildPlugin plugin;
    private final Logger logger;
    private final Map<UUID, GUI> openGuis = new HashMap<>();
    private final Map<UUID, Function<String, Boolean>> inputModes = new HashMap<>();
    private final Map<UUID, Long> lastClickTime = new HashMap<>(); 
    
    public GUIManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    public void initialize() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        logger.info("Gerenciador de GUI inicializado");
    }
    
    public void openGUI(Player player, GUI gui) {
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, () -> openGUI(player, gui));
            return;
        }
        
        try {
            closeGUI(player);
            
            Inventory inventory = Bukkit.createInventory(null, gui.getSize(), gui.getTitle());
            
            gui.setupInventory(inventory);
            
            player.openInventory(inventory);
            
            openGuis.put(player.getUniqueId(), gui);
            
            logger.info("Jogador " + player.getName() + " abriu GUI: " + gui.getClass().getSimpleName());
        } catch (Exception e) {
            logger.severe("Erro ao abrir GUI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void closeGUI(Player player) {
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, () -> closeGUI(player));
            return;
        }
        
        try {
            GUI gui = openGuis.remove(player.getUniqueId());
            if (gui != null) {
                if (player.getOpenInventory() != null && player.getOpenInventory().getTopInventory() != null) {
                    player.closeInventory();
                }
                
                logger.info("Jogador " + player.getName() + " fechou GUI: " + gui.getClass().getSimpleName());
            }
        } catch (Exception e) {
            logger.severe("Erro ao fechar GUI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public GUI getOpenGUI(Player player) {
        return openGuis.get(player.getUniqueId());
    }
    
    public boolean hasOpenGUI(Player player) {
        return openGuis.containsKey(player.getUniqueId());
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        GUI gui = openGuis.get(player.getUniqueId());
        if (gui == null) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        Long lastClick = lastClickTime.get(player.getUniqueId());
        if (lastClick != null && currentTime - lastClick < 200) { 
            event.setCancelled(true);
            return;
        }
        lastClickTime.put(player.getUniqueId(), currentTime);
        
        try {
            event.setCancelled(true);
            
            int slot = event.getRawSlot();
            ItemStack clickedItem = event.getCurrentItem();
            
            logger.info("Jogador " + player.getName() + " clicou na GUI: " + gui.getClass().getSimpleName() + " Slot: " + slot);
            
            gui.onClick(player, slot, clickedItem, event.getClick());
        } catch (Exception e) {
            logger.severe("Erro ao processar clique na GUI: " + e.getMessage());
            e.printStackTrace();
            closeGUI(player);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        
        try {
            GUI gui = openGuis.remove(player.getUniqueId());
            if (gui != null) {
                if (inputModes.containsKey(player.getUniqueId())) {
                    clearInputMode(player);
                }
                
                gui.onClose(player);
                logger.info("Jogador " + player.getName() + " fechou GUI: " + gui.getClass().getSimpleName());
            }
        } catch (Exception e) {
            logger.severe("Erro ao processar fechamento de GUI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void refreshGUI(Player player) {
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, () -> refreshGUI(player));
            return;
        }
        
        try {
            GUI gui = openGuis.get(player.getUniqueId());
            if (gui != null) {
                closeGUI(player);
                
                openGUI(player, gui);
                
                logger.info("GUI do jogador " + player.getName() + " foi atualizada: " + gui.getClass().getSimpleName());
            }
        } catch (Exception e) {
            logger.severe("Erro ao atualizar GUI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void closeAllGUIs() {
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, this::closeAllGUIs);
            return;
        }
        
        try {
            for (UUID playerUuid : openGuis.keySet()) {
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    closeGUI(player);
                }
            }
            openGuis.clear();
            logger.info("Todas as GUIs foram fechadas");
        } catch (Exception e) {
            logger.severe("Erro ao fechar todas as GUIs: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public int getOpenGUICount() {
        return openGuis.size();
    }
    
    public void setInputMode(Player player, Function<String, Boolean> inputHandler) {
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, () -> setInputMode(player, inputHandler));
            return;
        }
        
        try {
            inputModes.put(player.getUniqueId(), inputHandler);
            logger.info("Jogador " + player.getName() + " entrou em modo de entrada");
        } catch (Exception e) {
            logger.severe("Erro ao definir modo de entrada: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void setInputMode(Player player, String mode, GUI gui) {
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, () -> setInputMode(player, mode, gui));
            return;
        }
        
        try {
            if ("guild_name_input".equals(mode) && gui instanceof GuildNameInputGUI) {
                GuildNameInputGUI nameInputGUI = (GuildNameInputGUI) gui;
                inputModes.put(player.getUniqueId(), input -> {
                    if ("Cancelar".equals(input.trim())) {
                        nameInputGUI.handleCancel(player);
                        return true;
                    }
                    nameInputGUI.handleInputComplete(player, input);
                    return true;
                });
                logger.info("Jogador " + player.getName() + " entrou no modo de entrada de nome da guilda");
            } else {
                logger.warning("Modo de entrada desconhecido: " + mode);
            }
        } catch (Exception e) {
            logger.severe("Erro ao definir modo de entrada: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void clearInputMode(Player player) {
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, () -> clearInputMode(player));
            return;
        }
        
        try {
            inputModes.remove(player.getUniqueId());
            logger.info("Jogador " + player.getName() + " saiu do modo de entrada");
        } catch (Exception e) {
            logger.severe("Erro ao limpar modo de entrada: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean isInInputMode(Player player) {
        return inputModes.containsKey(player.getUniqueId());
    }
    
    public boolean handleInput(Player player, String input) {
        try {
            Function<String, Boolean> handler = inputModes.get(player.getUniqueId());
            if (handler != null) {
                boolean result = handler.apply(input);
                if (result) {
                    inputModes.remove(player.getUniqueId());
                }
                return result;
            }
            return false;
        } catch (Exception e) {
            logger.severe("Erro ao processar entrada do jogador: " + e.getMessage());
            e.printStackTrace();
            clearInputMode(player);
            return false;
        }
    }
}
