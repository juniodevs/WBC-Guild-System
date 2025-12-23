package com.guild.core.utils;

import java.util.List;

import org.bukkit.entity.Player;

import com.guild.GuildPlugin;
import com.guild.models.Guild;

/**
 * Utilitários de teste de variáveis - Usado para verificar a substituição de variáveis GUI
 */
public class VariableTestUtils {
    
    /**
     * Testa a substituição de variáveis GUI
     * @param plugin Instância do plugin
     * @param guild Objeto da guilda
     * @param player Objeto do jogador
     */
    public static void testGUIVariables(GuildPlugin plugin, Guild guild, Player player) {
        player.sendMessage("§6=== Teste de Variáveis GUI ===");
        
        // Testa variáveis básicas
        String[] testTexts = {
            "Nome da Guilda: {guild_name}",
            "Tag da Guilda: {guild_tag}",
            "Descrição da Guilda: {guild_description}",
            "ID da Guilda: {guild_id}",
            "Líder: {leader_name}",
            "Nível da Guilda: {guild_level}",
            "Saldo da Guilda: {guild_balance_formatted}",
            "Máx. Membros: {guild_max_members}",
            "Status da Guilda: {guild_frozen}",
            "Criado em: {guild_created_date}",
            "Membros: {member_count}/{guild_max_members}",
            "Requisito de Upgrade: {guild_next_level_requirement}",
            "Progresso de Upgrade: {guild_level_progress}"
        };
        
        for (String testText : testTexts) {
            String processed = GUIUtils.processGUIVariables(testText, guild, player);
            player.sendMessage("§eOriginal: §f" + testText);
            player.sendMessage("§aProcessado: §f" + processed);
            
            // Verifica se há variáveis não resolvidas
            if (GUIUtils.hasUnresolvedVariables(processed)) {
                List<String> unresolved = GUIUtils.getUnresolvedVariables(processed);
                player.sendMessage("§cVariáveis não resolvidas: §f" + unresolved);
            }
            player.sendMessage("");
        }
        
        // Testa variáveis assíncronas
        plugin.getGuildService().getGuildMemberCountAsync(guild.getId()).thenAccept(memberCount -> {
            String asyncTest = "Quantidade de Membros: {member_count}/{guild_max_members}";
            GUIUtils.processGUIVariablesAsync(asyncTest, guild, player, plugin).thenAccept(processed -> {
                player.sendMessage("§6Teste Assíncrono: §f" + asyncTest);
                player.sendMessage("§aResultado Assíncrono: §f" + processed);
            });
        });
    }
    
    /**
     * Testa códigos de cores
     * @param player Objeto do jogador
     */
    public static void testColorCodes(Player player) {
        player.sendMessage("§6=== Teste de Códigos de Cores ===");
        
        String[] colorTests = {
            "&aTexto Verde",
            "&cTexto Vermelho",
            "&eTexto Amarelo",
            "&bTexto Ciano",
            "&dTexto Rosa",
            "&fTexto Branco",
            "&7Texto Cinza",
            "&8Texto Cinza Escuro",
            "&9Texto Azul",
            "&0Texto Preto",
            "&lTexto Negrito",
            "&nTexto Sublinhado",
            "&oTexto Itálico",
            "&kCaractere Aleatório",
            "&rResetar Formato"
        };
        
        for (String test : colorTests) {
            String processed = ColorUtils.colorize(test);
            player.sendMessage("§eOriginal: §f" + test);
            player.sendMessage("§aProcessado: §f" + processed);
            player.sendMessage("");
        }
    }
    
    /**
     * Testa PlaceholderUtils
     * @param guild Objeto da guilda
     * @param player Objeto do jogador
     */
    public static void testPlaceholderUtils(Guild guild, Player player) {
        player.sendMessage("§6=== Teste de PlaceholderUtils ===");
        
        String testText = "Guilda: {guild_name}, Líder: {leader_name}, Nível: {guild_level}, Saldo: {guild_balance_formatted}";
        String processed = PlaceholderUtils.replaceGuildPlaceholders(testText, guild, player);
        
        player.sendMessage("§eOriginal: §f" + testText);
        player.sendMessage("§aProcessado: §f" + processed);
        
        // Verifica se há variáveis não resolvidas
        if (GUIUtils.hasUnresolvedVariables(processed)) {
            List<String> unresolved = GUIUtils.getUnresolvedVariables(processed);
            player.sendMessage("§cVariáveis não resolvidas: §f" + unresolved);
        }
    }
}
