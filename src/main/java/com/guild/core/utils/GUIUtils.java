package com.guild.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bukkit.entity.Player;

import com.guild.GuildPlugin;
import com.guild.models.Guild;
import com.guild.models.GuildMember;

/**
 * Utilitários de GUI - Processamento unificado de substituição de variáveis e códigos de cores em GUIs
 */
public class GUIUtils {
    
    /**
     * Processa substituição de variáveis na configuração da GUI
     * @param text Texto original
     * @param guild Objeto da guilda
     * @param player Objeto do jogador
     * @return Texto substituído
     */
    public static String processGUIVariables(String text, Guild guild, Player player) {
        if (text == null) {
            return "";
        }
        
        // Usa PlaceholderUtils para processar variáveis básicas
        String result = PlaceholderUtils.replaceGuildPlaceholders(text, guild, player);
        
        // Garante que os códigos de cores sejam aplicados corretamente
        return ColorUtils.colorize(result);
    }
    
    /**
     * Processa assincronamente a substituição de variáveis na configuração da GUI (incluindo dados dinâmicos)
     * @param text Texto original
     * @param guild Objeto da guilda
     * @param player Objeto do jogador
     * @param plugin Instância do plugin
     * @return CompletableFuture do texto substituído
     */
    public static CompletableFuture<String> processGUIVariablesAsync(String text, Guild guild, Player player, GuildPlugin plugin) {
        if (text == null) {
            return CompletableFuture.completedFuture("");
        }
        
        // Processa variáveis estáticas primeiro
        String result = processGUIVariables(text, guild, player);
        
        // Obtém dados dinâmicos assincronamente
        return plugin.getGuildService().getGuildMemberCountAsync(guild.getId()).thenApply(memberCount -> {
            return result
                .replace("{member_count}", String.valueOf(memberCount))
                .replace("{online_member_count}", String.valueOf(memberCount)); // Usando contagem total de membros temporariamente
        });
    }
    
    /**
     * Processa lista de lore de itens na configuração da GUI
     * @param loreList Lista de lore original
     * @param guild Objeto da guilda
     * @param player Objeto do jogador
     * @return Lista de lore processada
     */
    public static List<String> processGUILore(List<String> loreList, Guild guild, Player player) {
        List<String> processedLore = new ArrayList<>();
        
        if (loreList != null) {
            for (String line : loreList) {
                processedLore.add(processGUIVariables(line, guild, player));
            }
        }
        
        return processedLore;
    }
    
    /**
     * Processa assincronamente a lista de lore de itens na configuração da GUI (incluindo dados dinâmicos)
     * @param loreList Lista de lore original
     * @param guild Objeto da guilda
     * @param player Objeto do jogador
     * @param plugin Instância do plugin
     * @return CompletableFuture da lista de lore processada
     */
    public static CompletableFuture<List<String>> processGUILoreAsync(List<String> loreList, Guild guild, Player player, GuildPlugin plugin) {
        if (loreList == null || loreList.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        List<CompletableFuture<String>> futures = new ArrayList<>();
        
        for (String line : loreList) {
            futures.add(processGUIVariablesAsync(line, guild, player, plugin));
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<String> processedLore = new ArrayList<>();
                for (CompletableFuture<String> future : futures) {
                    try {
                        processedLore.add(future.get());
                    } catch (Exception e) {
                        plugin.getLogger().warning("Erro ao processar descrição da GUI: " + e.getMessage());
                        processedLore.add("&cErro");
                    }
                }
                return processedLore;
            });
    }
    
    /**
     * Processa variáveis de GUI relacionadas a membros
     * @param text Texto original
     * @param member Objeto do membro
     * @param guild Objeto da guilda
     * @return Texto substituído
     */
    public static String processMemberGUIVariables(String text, GuildMember member, Guild guild) {
        if (text == null) {
            return "";
        }
        
        return PlaceholderUtils.replaceMemberPlaceholders(text, member, guild);
    }
    
    /**
     * Verifica se as variáveis foram substituídas corretamente
     * @param text Texto a ser verificado
     * @return Se contém variáveis não substituídas
     */
    public static boolean hasUnresolvedVariables(String text) {
        if (text == null) {
            return false;
        }
        
        // Verifica se contém placeholders de variáveis não substituídos
        return text.contains("{") && text.contains("}");
    }
    
    /**
     * Obtém lista de variáveis não substituídas
     * @param text Texto a ser verificado
     * @return Lista de variáveis não substituídas
     */
    public static List<String> getUnresolvedVariables(String text) {
        List<String> unresolved = new ArrayList<>();
        
        if (text == null) {
            return unresolved;
        }
        
        // Detecção simples de variáveis (pode ser expandida para regex mais complexo)
        String[] parts = text.split("\\{");
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            int endIndex = part.indexOf("}");
            if (endIndex > 0) {
                String variable = part.substring(0, endIndex);
                unresolved.add("{" + variable + "}");
            }
        }
        
        return unresolved;
    }
}
