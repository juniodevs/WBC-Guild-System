package com.guild.core.utils;

import java.util.List;

import org.bukkit.entity.Player;

import com.guild.GuildPlugin;
import com.guild.models.Guild;

/**
 * 变量测试工具类 - 用于验证GUI变量替换
 */
public class VariableTestUtils {
    
    /**
     * 测试GUI变量替换
     * @param plugin 插件实例
     * @param guild 工会对象
     * @param player 玩家对象
     */
    public static void testGUIVariables(GuildPlugin plugin, Guild guild, Player player) {
        player.sendMessage("§6=== Teste de Variáveis GUI ===");
        
        // 测试基础变量
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
            
            // Verificar se há variáveis não resolvidas
            if (GUIUtils.hasUnresolvedVariables(processed)) {
                List<String> unresolved = GUIUtils.getUnresolvedVariables(processed);
                player.sendMessage("§cVariáveis não resolvidas: §f" + unresolved);
            }
            player.sendMessage("");
        }
        
        // 测试异步变量
        plugin.getGuildService().getGuildMemberCountAsync(guild.getId()).thenAccept(memberCount -> {
            String asyncTest = "Quantidade de membros: {member_count}/{guild_max_members}";
            GUIUtils.processGUIVariablesAsync(asyncTest, guild, player, plugin).thenAccept(processed -> {
                player.sendMessage("§6Teste assíncrono: §f" + asyncTest);
                player.sendMessage("§aResultado assíncrono: §f" + processed);
            });
        });
    }
    
    /**
     * 测试颜色代码
     * @param player 玩家对象
     */
    public static void testColorCodes(Player player) {
        player.sendMessage("§6=== Teste de Códigos de Cores ===");
        
        String[] colorTests = {
            "&aTexto verde",
            "&cTexto vermelho",
            "&eTexto amarelo",
            "&bTexto ciano",
            "&dTexto rosa",
            "&fTexto branco",
            "&7Texto cinza",
            "&8Texto cinza escuro",
            "&9Texto azul",
            "&0Texto preto",
            "&lTexto negrito",
            "&nTexto sublinhado",
            "&oTexto itálico",
            "&kCaracteres aleatórios",
            "&rResetar formatação"
        };
        
        for (String test : colorTests) {
            String processed = ColorUtils.colorize(test);
            player.sendMessage("§eOriginal: §f" + test);
            player.sendMessage("§aProcessado: §f" + processed);
            player.sendMessage("");
        }
    }
    
    /**
     * 测试PlaceholderUtils
     * @param guild 工会对象
     * @param player 玩家对象
     */
    public static void testPlaceholderUtils(Guild guild, Player player) {
        player.sendMessage("§6=== Teste do PlaceholderUtils ===");
        
        String testText = "Guilda: {guild_name}, Líder: {leader_name}, Nível: {guild_level}, Saldo: {guild_balance_formatted}";
        String processed = PlaceholderUtils.replaceGuildPlaceholders(testText, guild, player);
        
        player.sendMessage("§eOriginal: §f" + testText);
        player.sendMessage("§aProcessado: §f" + processed);
        
        // Verificar se há variáveis não resolvidas
        if (GUIUtils.hasUnresolvedVariables(processed)) {
            List<String> unresolved = GUIUtils.getUnresolvedVariables(processed);
            player.sendMessage("§cVariáveis não resolvidas: §f" + unresolved);
        }
    }
}
