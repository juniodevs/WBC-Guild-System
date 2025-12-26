package com.guild.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.guild.GuildPlugin;
import com.guild.core.database.DatabaseManager;
import com.guild.core.time.TimeProvider;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildApplication;
import com.guild.models.GuildInvitation;
import com.guild.models.GuildLog;
import com.guild.models.GuildMember;
import com.guild.models.GuildRelation;

public class GuildService {
    
    private final GuildPlugin plugin;
    private final DatabaseManager databaseManager;
    private final Logger logger;
    
    public GuildService(GuildPlugin plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.logger = plugin.getLogger();
    }
    
    // Ferramenta de tempo: use uniformemente a string de hora local do sistema operacional (yyyy-MM-dd HH:mm:ss)
    private String nowString() { return TimeProvider.nowString(); }
    private String plusMinutesString(int minutes) { return TimeProvider.plusMinutesString(minutes); }
    private String plusDaysString(int days) { return TimeProvider.plusDaysString(days); }
    
    /**
     * Criar guilda (Assíncrono)
     */
    public CompletableFuture<Boolean> createGuildAsync(String name, String tag, String description, UUID leaderUuid, String leaderName) {
        return getGuildByNameAsync(name).thenCompose(existingGuildByName -> {
            if (existingGuildByName != null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return getGuildByTagAsync(tag).thenCompose(existingGuildByTag -> {
                if (existingGuildByTag != null) {
                    return CompletableFuture.completedFuture(false);
                }
                
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        String sql = "INSERT INTO guilds (name, tag, description, leader_uuid, leader_name, level, max_members, frozen, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 1, 6, 0, ?, ?)";
                        
                        try (Connection conn = databaseManager.getConnection();
                             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                            
                            stmt.setString(1, name);
                            stmt.setString(2, tag);
                            stmt.setString(3, description);
                            stmt.setString(4, leaderUuid.toString());
                            stmt.setString(5, leaderName);
                            
                            stmt.setString(6, nowString());
                            stmt.setString(7, nowString());
                            int affectedRows = stmt.executeUpdate();
                            if (affectedRows > 0) {
                                try (ResultSet rs = stmt.getGeneratedKeys()) {
                                    if (rs.next()) {
                                        int guildId = rs.getInt(1);
                                        logger.info("Guilda criada com sucesso: " + name + " (ID: " + guildId + ")");
                                        return guildId;
                                    }
                                }
                            }
                        }
                    } catch (SQLException e) {
                        logger.severe("Erro ao criar guilda: " + e.getMessage());
                    }
                    return -1;
                }).thenCompose(guildId -> {
                    if ((Integer) guildId > 0) {
                        // Adicionar o líder como membro da guilda (evitar consultas repetidas)
                        return addGuildMemberDirectAsync((Integer) guildId, leaderUuid, leaderName, GuildMember.Role.LEADER)
                            .thenCompose(success -> {
                                if (success) {
                                    // Registrar log de criação da guilda
                                    return logGuildActionAsync((Integer) guildId, name, leaderUuid.toString(), leaderName,
                                        GuildLog.LogType.GUILD_CREATED, "Criar Guilda", "Nome da Guilda: " + name + ", Tag: " + tag)
                                        .thenApply(logSuccess -> success);
                                }
                                return CompletableFuture.completedFuture(success);
                            });
                    }
                    return CompletableFuture.completedFuture(false);
                });
            });
        });
    }
    
    /**
     * Criar guilda (Wrapper Síncrono)
     */
    public boolean createGuild(String name, String tag, String description, UUID leaderUuid, String leaderName) {
        try {
            return createGuildAsync(name, tag, description, leaderUuid, leaderName).get();
        } catch (Exception e) {
            logger.severe("Exceção ao criar guilda: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Deletar guilda (Assíncrono)
     */
    public CompletableFuture<Boolean> deleteGuildAsync(int guildId, UUID requesterUuid) {
        return getGuildByIdAsync(guildId).thenCompose(guild -> {
            if (guild == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return getGuildMemberAsync(requesterUuid).thenCompose(member -> {
                // Verificar permissões
                if (member == null || member.getGuildId() != guildId || member.getRole() != GuildMember.Role.LEADER) {
                    return CompletableFuture.completedFuture(false);
                }
                
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        // Deletar todos os membros da guilda
                        String deleteMembersSql = "DELETE FROM guild_members WHERE guild_id = ?";
                        try (Connection conn = databaseManager.getConnection();
                             PreparedStatement stmt = conn.prepareStatement(deleteMembersSql)) {
                            stmt.setInt(1, guildId);
                            stmt.executeUpdate();
                        }
                        
                        // Deletar guilda
                        String deleteGuildSql = "DELETE FROM guilds WHERE id = ?";
                        try (Connection conn = databaseManager.getConnection();
                             PreparedStatement stmt = conn.prepareStatement(deleteGuildSql)) {
                            stmt.setInt(1, guildId);
                            int affectedRows = stmt.executeUpdate();
                            if (affectedRows > 0) {
                                logger.info("Guilda deletada com sucesso: " + guild.getName() + " (ID: " + guildId + ")");
                                
                                // Registrar log de dissolução da guilda
                                logGuildActionAsync(guildId, guild.getName(), guild.getLeaderUuid().toString(), guild.getLeaderName(),
                                    GuildLog.LogType.GUILD_DISSOLVED, "Dissolução da Guilda", "Guilda dissolvida");
                                
                                return true;
                            }
                        }
                    } catch (SQLException e) {
                        logger.severe("Erro ao deletar guilda: " + e.getMessage());
                    }
                    return false;
                });
            });
        });
    }
    
    /**
     * Deletar guilda (Wrapper Síncrono)
     */
    public boolean deleteGuild(int guildId, UUID requesterUuid) {
        try {
            return deleteGuildAsync(guildId, requesterUuid).get();
        } catch (Exception e) {
            logger.severe("Exceção ao deletar guilda: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Atualizar informações da guilda (Assíncrono)
     */
    public CompletableFuture<Boolean> updateGuildAsync(int guildId, String name, String tag, String description, UUID requesterUuid) {
        return getGuildByIdAsync(guildId).thenCompose(guild -> {
            if (guild == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return getGuildMemberAsync(requesterUuid).thenCompose(member -> {
                // Verificar permissões
                if (member == null || member.getGuildId() != guildId || 
                    (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
                    return CompletableFuture.completedFuture(false);
                }
                
                // Verificar se o nome e a tag conflitam com outras guildas
                CompletableFuture<Boolean> nameCheck = CompletableFuture.completedFuture(true);
                if (name != null && !name.equals(guild.getName())) {
                    nameCheck = getGuildByNameAsync(name).thenApply(existingGuild -> existingGuild == null);
                }
                
                CompletableFuture<Boolean> tagCheck = CompletableFuture.completedFuture(true);
                if (tag != null && !tag.equals(guild.getTag())) {
                    tagCheck = getGuildByTagAsync(tag).thenApply(existingGuild -> existingGuild == null);
                }
                
                return nameCheck.thenCombine(tagCheck, (nameValid, tagValid) -> nameValid && tagValid)
                    .thenCompose(valid -> {
                        if (!valid) {
                            return CompletableFuture.completedFuture(false);
                        }
                        
                        return CompletableFuture.supplyAsync(() -> {
                            try {
                                String sql = "UPDATE guilds SET name = COALESCE(?, name), tag = COALESCE(?, tag), description = COALESCE(?, description), updated_at = ? WHERE id = ?";
                                
                                try (Connection conn = databaseManager.getConnection();
                                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                                    
                                    stmt.setString(1, name);
                                    stmt.setString(2, tag);
                                    stmt.setString(3, description);
                                    stmt.setString(4, nowString());
                                    stmt.setInt(5, guildId);
                                    
                                    int affectedRows = stmt.executeUpdate();
                                    if (affectedRows > 0) {
                                        logger.info("Informações da guilda atualizadas com sucesso: " + guild.getName() + " (ID: " + guildId + ")");
                                        return true;
                                    }
                                }
                            } catch (SQLException e) {
                                logger.severe("Erro ao atualizar informações da guilda: " + e.getMessage());
                            }
                            return false;
                        });
                    });
            });
        });
    }
    
    /**
     * Atualizar informações da guilda (Wrapper Síncrono)
     */
    public boolean updateGuild(int guildId, String name, String tag, String description, UUID requesterUuid) {
        try {
            return updateGuildAsync(guildId, name, tag, description, requesterUuid).get();
        } catch (Exception e) {
            logger.severe("Exceção ao atualizar informações da guilda: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Adicionar membro da guilda (Assíncrono)
     */
    public CompletableFuture<Boolean> addGuildMemberAsync(int guildId, UUID playerUuid, String playerName, GuildMember.Role role) {
        return getPlayerGuildAsync(playerUuid).thenCompose(existingGuild -> {
            if (existingGuild != null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return CompletableFuture.supplyAsync(() -> {
                try {
                
                String sql = "INSERT INTO guild_members (guild_id, player_uuid, player_name, role, joined_at) VALUES (?, ?, ?, ?, ?)";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, guildId);
                    stmt.setString(2, playerUuid.toString());
                    stmt.setString(3, playerName);
                    stmt.setString(4, role.name());
                    stmt.setString(5, nowString());
                    
                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows > 0) {
                        logger.info("Jogador " + playerName + " entrou na guilda (ID: " + guildId + ")");
                        // Atualizar cache de permissões interno
                        try { plugin.getPermissionManager().updatePlayerPermissions(playerUuid); } catch (Exception ignored) {}
                        
                        // Registrar log de entrada de membro
                        getGuildByIdAsync(guildId).thenAccept(guild -> {
                            if (guild != null) {
                                logGuildActionAsync(guildId, guild.getName(), playerUuid.toString(), playerName,
                                    GuildLog.LogType.MEMBER_JOINED, "Membro Entrou", "Jogador: " + playerName + ", Cargo: " + role.getDisplayName());
                            }
                        });
                        
                        return true;
                    }
                }
            } catch (SQLException e) {
                logger.severe("Erro ao adicionar membro da guilda: " + e.getMessage());
            }
            return false;
        });
        });
    }
    
    /**
     * Adicionar membro da guilda (Wrapper Síncrono)
     */
    public boolean addGuildMember(int guildId, UUID playerUuid, String playerName, GuildMember.Role role) {
        try {
            return addGuildMemberAsync(guildId, playerUuid, playerName, role).get();
        } catch (Exception e) {
            logger.severe("Exceção ao adicionar membro da guilda: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Remover membro da guilda (Assíncrono)
     */
    public CompletableFuture<Boolean> removeGuildMemberAsync(UUID playerUuid, UUID requesterUuid) {
        return getGuildMemberAsync(playerUuid).thenCompose(member -> {
            if (member == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return getGuildMemberAsync(requesterUuid).thenCompose(requester -> {
                // Verificar permissões
                if (requester == null || requester.getGuildId() != member.getGuildId()) {
                    return CompletableFuture.completedFuture(false);
                }
                
                // O líder não pode ser expulso, a menos que saia por conta própria
                if (member.getRole() == GuildMember.Role.LEADER && !playerUuid.equals(requesterUuid)) {
                    return CompletableFuture.completedFuture(false);
                }
                
                // Apenas o líder e oficiais podem expulsar membros
                if (!playerUuid.equals(requesterUuid) && 
                    requester.getRole() != GuildMember.Role.LEADER && 
                    requester.getRole() != GuildMember.Role.OFFICER) {
                    return CompletableFuture.completedFuture(false);
                }
                
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        String sql = "DELETE FROM guild_members WHERE player_uuid = ?";
                        
                        try (Connection conn = databaseManager.getConnection();
                             PreparedStatement stmt = conn.prepareStatement(sql)) {
                            
                            stmt.setString(1, playerUuid.toString());
                            
                            int affectedRows = stmt.executeUpdate();
                            if (affectedRows > 0) {
                                logger.info("Jogador " + member.getPlayerName() + " saiu da guilda (ID: " + member.getGuildId() + ")");
                                // Atualizar cache de permissões interno
                                try { plugin.getPermissionManager().updatePlayerPermissions(playerUuid); } catch (Exception ignored) {}
                                
                                // Registrar log de saída de membro
                                getGuildByIdAsync(member.getGuildId()).thenAccept(guild -> {
                                    if (guild != null) {
                                        GuildLog.LogType logType = playerUuid.equals(requesterUuid) ? 
                                            GuildLog.LogType.MEMBER_LEFT : GuildLog.LogType.MEMBER_KICKED;
                                        String description = playerUuid.equals(requesterUuid) ? "Membro saiu" : "Membro expulso";
                                        String details = "Jogador: " + member.getPlayerName() + 
                                            (playerUuid.equals(requesterUuid) ? "" : ", Operador: " + requester.getPlayerName());
                                        
                                        logGuildActionAsync(member.getGuildId(), guild.getName(), 
                                            requesterUuid.toString(), requester.getPlayerName(),
                                            logType, description, details);
                                    }
                                });
                                
                                return true;
                            }
                        }
                    } catch (SQLException e) {
                        logger.severe("Erro ao remover membro da guilda: " + e.getMessage());
                    }
                    return false;
                });
            });
        });
    }
    
    /**
     * Remover membro da guilda (Wrapper Síncrono)
     */
    public boolean removeGuildMember(UUID playerUuid, UUID requesterUuid) {
        try {
            return removeGuildMemberAsync(playerUuid, requesterUuid).get();
        } catch (Exception e) {
            logger.severe("Exceção ao remover membro da guilda: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Atualizar cargo de membro (Assíncrono)
     */
    public CompletableFuture<Boolean> updateMemberRoleAsync(UUID playerUuid, GuildMember.Role newRole, UUID requesterUuid) {
        return getGuildMemberAsync(playerUuid).thenCompose(member -> {
            if (member == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return getGuildMemberAsync(requesterUuid).thenCompose(requester -> {
                // Verificar permissões - Apenas o líder pode alterar cargos
                if (requester == null || requester.getGuildId() != member.getGuildId() || 
                    requester.getRole() != GuildMember.Role.LEADER) {
                    return CompletableFuture.completedFuture(false);
                }
                
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        String sql = "UPDATE guild_members SET role = ? WHERE player_uuid = ?";
                        
                        try (Connection conn = databaseManager.getConnection();
                             PreparedStatement stmt = conn.prepareStatement(sql)) {
                            
                            stmt.setString(1, newRole.name());
                            stmt.setString(2, playerUuid.toString());
                            
                            int affectedRows = stmt.executeUpdate();
                            if (affectedRows > 0) {
                                logger.info("Jogador " + member.getPlayerName() + " cargo atualizado para: " + newRole.name());
                                // Atualizar cache de permissões interno
                                try { plugin.getPermissionManager().updatePlayerPermissions(playerUuid); } catch (Exception ignored) {}
                                
                                // Registrar log de alteração de cargo
                                getGuildByIdAsync(member.getGuildId()).thenAccept(guild -> {
                                    if (guild != null) {
                                        GuildLog.LogType logType = newRole == GuildMember.Role.LEADER ? 
                                            GuildLog.LogType.LEADER_TRANSFERRED : 
                                            (newRole == GuildMember.Role.OFFICER ? GuildLog.LogType.MEMBER_PROMOTED : GuildLog.LogType.MEMBER_DEMOTED);
                                        String description = newRole == GuildMember.Role.LEADER ? "Transferência de Líder" : 
                                            (newRole == GuildMember.Role.OFFICER ? "Promoção de Membro" : "Rebaixamento de Membro");
                                        String details = "Jogador: " + member.getPlayerName() + ", Novo Cargo: " + newRole.getDisplayName() + 
                                            ", Operador: " + requester.getPlayerName();
                                        
                                        logGuildActionAsync(member.getGuildId(), guild.getName(), 
                                            requesterUuid.toString(), requester.getPlayerName(),
                                            logType, description, details);
                                    }
                                });
                                
                                return true;
                            }
                        }
                    } catch (SQLException e) {
                        logger.severe("Erro ao atualizar cargo do membro: " + e.getMessage());
                    }
                    return false;
                });
            });
        });
    }
    
    /**
     * Atualizar cargo de membro (Wrapper Síncrono)
     */
    public boolean updateMemberRole(UUID playerUuid, GuildMember.Role newRole, UUID requesterUuid) {
        try {
            return updateMemberRoleAsync(playerUuid, newRole, requesterUuid).get();
        } catch (Exception e) {
            logger.severe("Exceção ao atualizar cargo do membro: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Obter guilda do jogador (Assíncrono)
     */
    public CompletableFuture<Guild> getPlayerGuildAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT g.* FROM guilds g " +
                            "INNER JOIN guild_members gm ON g.id = gm.guild_id " +
                            "WHERE gm.player_uuid = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setString(1, playerUuid.toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return createGuildFromResultSet(rs);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Erro ao obter guilda do jogador: " + e.getMessage());
            }
            return null;
        });
    }
    
    /**
     * Obter guilda do jogador (Wrapper Síncrono)
     */
    public Guild getPlayerGuild(UUID playerUuid) {
        try {
            return getPlayerGuildAsync(playerUuid).get();
        } catch (Exception e) {
            logger.severe("Exceção ao obter guilda do jogador: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Obter membro da guilda (Assíncrono)
     */
    public CompletableFuture<GuildMember> getGuildMemberAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT * FROM guild_members WHERE player_uuid = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setString(1, playerUuid.toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return createGuildMemberFromResultSet(rs);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Erro ao obter membro da guilda: " + e.getMessage());
            }
            return null;
        });
    }
    
    /**
     * Obter membro da guilda (Wrapper Síncrono)
     */
    public GuildMember getGuildMember(UUID playerUuid) {
        try {
            return getGuildMemberAsync(playerUuid).get();
        } catch (Exception e) {
            logger.severe("Exceção ao obter membro da guilda: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Obter contagem de membros da guilda (Assíncrono)
     */
    public CompletableFuture<Integer> getGuildMemberCountAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT COUNT(*) FROM guild_members WHERE guild_id = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, guildId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getInt(1);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Erro ao obter contagem de membros da guilda: " + e.getMessage());
            }
            return 0;
        });
    }
    
    /**
     * Obter contagem de membros da guilda (Wrapper Síncrono)
     */
    public int getGuildMemberCount(int guildId) {
        try {
            return getGuildMemberCountAsync(guildId).get();
        } catch (Exception e) {
            logger.severe("Exceção ao obter contagem de membros da guilda: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Obter todos os membros da guilda (Assíncrono)
     */
    public CompletableFuture<List<GuildMember>> getGuildMembersAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> {
            List<GuildMember> members = new ArrayList<>();
            try {
                String sql = "SELECT * FROM guild_members WHERE guild_id = ? ORDER BY role ASC, joined_at ASC";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, guildId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            members.add(createGuildMemberFromResultSet(rs));
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Erro ao obter lista de membros da guilda: " + e.getMessage());
            }
            return members;
        });
    }
    
    /**
     * Obter todos os membros da guilda (Wrapper Síncrono)
     */
    public List<GuildMember> getGuildMembers(int guildId) {
        try {
            return getGuildMembersAsync(guildId).get();
        } catch (Exception e) {
            logger.severe("Exceção ao obter lista de membros da guilda: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Obter guilda por ID (Assíncrono)
     */
    public CompletableFuture<Guild> getGuildByIdAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT * FROM guilds WHERE id = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, guildId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return createGuildFromResultSet(rs);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Erro ao obter guilda por ID: " + e.getMessage());
            }
            return null;
        });
    }
    
    /**
     * Obter guilda por ID (Wrapper Síncrono)
     */
    public Guild getGuildById(int guildId) {
        try {
            return getGuildByIdAsync(guildId).get();
        } catch (Exception e) {
            logger.severe("Exceção ao obter guilda por ID: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Obter guilda por nome (Assíncrono)
     */
    public CompletableFuture<Guild> getGuildByNameAsync(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT * FROM guilds WHERE name = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setString(1, name);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return createGuildFromResultSet(rs);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Erro ao obter guilda por nome: " + e.getMessage());
            }
            return null;
        });
    }
    
    /**
     * Obter guilda por nome (Wrapper Síncrono)
     */
    public Guild getGuildByName(String name) {
        try {
            return getGuildByNameAsync(name).get();
        } catch (Exception e) {
            logger.severe("Exceção ao obter guilda por nome: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Obter guilda por tag (Assíncrono)
     */
    public CompletableFuture<Guild> getGuildByTagAsync(String tag) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT * FROM guilds WHERE tag = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setString(1, tag);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return createGuildFromResultSet(rs);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Erro ao obter guilda por tag: " + e.getMessage());
            }
            return null;
        });
    }
    
    /**
     * Obter guilda por tag (Wrapper Síncrono)
     */
    public Guild getGuildByTag(String tag) {
        try {
            return getGuildByTagAsync(tag).get();
        } catch (Exception e) {
            logger.severe("Exceção ao obter guilda por tag: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Obter todas as guildas (Assíncrono)
     */
    public CompletableFuture<List<Guild>> getAllGuildsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            List<Guild> guilds = new ArrayList<>();
            try {
                String sql = "SELECT * FROM guilds ORDER BY created_at DESC";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    
                    while (rs.next()) {
                        guilds.add(createGuildFromResultSet(rs));
                    }
                }
            } catch (SQLException e) {
                logger.severe("Erro ao obter todas as guildas: " + e.getMessage());
            }
            return guilds;
        });
    }
    
    /**
     * Obter todas as guildas (Wrapper Síncrono)
     */
    public List<Guild> getAllGuilds() {
        try {
            return getAllGuildsAsync().get();
        } catch (Exception e) {
            logger.severe("Exceção ao obter todas as guildas: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Verificar se é líder da guilda
     */
    public boolean isGuildLeader(UUID playerUuid) {
        GuildMember member = getGuildMember(playerUuid);
        return member != null && member.getRole() == GuildMember.Role.LEADER;
    }
    
    /**
     * Verificar se é líder da guilda especificada
     */
    public boolean isGuildLeader(UUID playerUuid, int guildId) {
        GuildMember member = getGuildMember(playerUuid);
        return member != null && member.getGuildId() == guildId && member.getRole() == GuildMember.Role.LEADER;
    }
    
    /**
     * Verificar se é oficial da guilda
     */
    public boolean isGuildOfficer(UUID playerUuid) {
        GuildMember member = getGuildMember(playerUuid);
        return member != null && member.getRole() == GuildMember.Role.OFFICER;
    }
    
    /**
     * Verificar se tem permissões de guilda
     */
    public boolean hasGuildPermission(UUID playerUuid) {
        GuildMember member = getGuildMember(playerUuid);
        return member != null && (member.getRole() == GuildMember.Role.LEADER || member.getRole() == GuildMember.Role.OFFICER);
    }
    
    /**
     * Criar objeto Guild a partir do ResultSet
     */
    private Guild createGuildFromResultSet(ResultSet rs) throws SQLException {
        Guild guild = new Guild();
        guild.setId(rs.getInt("id"));
        guild.setName(rs.getString("name"));
        guild.setTag(rs.getString("tag"));
        guild.setDescription(rs.getString("description"));
        guild.setLeaderUuid(UUID.fromString(rs.getString("leader_uuid")));
        guild.setLeaderName(rs.getString("leader_name"));
        
        guild.setCreatedAt(parseTimestamp(rs, "created_at"));
        guild.setUpdatedAt(parseTimestamp(rs, "updated_at"));
        
        try {
            guild.setLevel(rs.getInt("level"));
        } catch (SQLException e) {
            guild.setLevel(1);
        }
        
        try {
            guild.setMaxMembers(rs.getInt("max_members"));
        } catch (SQLException e) {
            guild.setMaxMembers(6);
        }
        
        try {
            guild.setFrozen(rs.getBoolean("frozen"));
        } catch (SQLException e) {
            guild.setFrozen(false);
        }
        
        try {
            String bannerData = rs.getString("banner_data");
            if (bannerData != null && !bannerData.isEmpty()) {
                guild.setBanner(com.guild.core.utils.BannerSerializer.deserialize(bannerData));
            }
        } catch (SQLException e) {
            // Coluna banner_data pode não existir em bancos antigos
        }
        
        return guild;
    }
    
    /**
     * Criar objeto GuildMember a partir do ResultSet
     */
    private GuildMember createGuildMemberFromResultSet(ResultSet rs) throws SQLException {
        GuildMember member = new GuildMember();
        member.setId(rs.getInt("id"));
        member.setGuildId(rs.getInt("guild_id"));
        member.setPlayerUuid(UUID.fromString(rs.getString("player_uuid")));
        member.setPlayerName(rs.getString("player_name"));
        member.setRole(GuildMember.Role.valueOf(rs.getString("role")));
        member.setJoinedAt(parseTimestamp(rs, "joined_at"));
        return member;
    }
    
    /**
     * Analisar timestamp
     */
    private java.time.LocalDateTime parseTimestamp(ResultSet rs, String columnName) throws SQLException {
        // Analisar preferencialmente como string no formato unificado, para evitar desvios de conversão de fuso horário pelo driver
        String s = rs.getString(columnName);
        if (s != null && !s.isEmpty()) {
            try {
                return LocalDateTime.parse(s, com.guild.core.time.TimeProvider.FULL_FORMATTER);
            } catch (Exception ignore) {
                try {
                    return LocalDateTime.parse(s.replace(" ", "T"));
                } catch (Exception ex) {
                    logger.warning("Não foi possível analisar o timestamp: " + s);
                }
            }
        }
        // Fallback: usar timestamp do driver
        try {
            Timestamp ts = rs.getTimestamp(columnName);
            if (ts != null) return ts.toLocalDateTime();
        } catch (SQLException ignore) {}
        return LocalDateTime.now();
    }

    
    
    /**
     * Enviar aplicação (Assíncrono)
     */
    public CompletableFuture<Boolean> submitApplicationAsync(int guildId, UUID playerUuid, String playerName, String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Verificar se já existe uma aplicação pendente
                if (hasPendingApplication(playerUuid, guildId)) {
                    return false;
                }
                
                String sql = "INSERT INTO guild_applications (guild_id, player_uuid, player_name, message, status, created_at) VALUES (?, ?, ?, ?, ?, ?)";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, guildId);
                    stmt.setString(2, playerUuid.toString());
                    stmt.setString(3, playerName);
                    stmt.setString(4, message);
                    stmt.setString(5, GuildApplication.ApplicationStatus.PENDING.name());
                    stmt.setString(6, nowString());
                    
                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows > 0) {
                        logger.info("Jogador " + playerName + " enviou aplicação para guilda (ID: " + guildId + ")");
                        
                        // Registrar log de envio de aplicação
                        getGuildByIdAsync(guildId).thenAccept(guild -> {
                            if (guild != null) {
                                logGuildActionAsync(guildId, guild.getName(), playerUuid.toString(), playerName,
                                    GuildLog.LogType.APPLICATION_SUBMITTED, "Aplicação Enviada", "Mensagem: " + message);
                            }
                        });
                        
                        return true;
                    }
                }
            } catch (SQLException e) {
                logger.severe("Erro ao enviar aplicação: " + e.getMessage());
            }
            return false;
        });
    }
    
    /**
     * Enviar aplicação (Wrapper Síncrono)
     */
    public boolean submitApplication(int guildId, UUID playerUuid, String playerName, String message) {
        try {
            return submitApplicationAsync(guildId, playerUuid, playerName, message).get();
        } catch (Exception e) {
            logger.severe("Exceção ao enviar aplicação: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Processar aplicação (Assíncrono)
     */
    public CompletableFuture<Boolean> processApplicationAsync(int applicationId, GuildApplication.ApplicationStatus status, UUID processorUuid) {
        return getApplicationByIdAsync(applicationId).thenCompose(application -> {
            if (application == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return getGuildMemberAsync(processorUuid).thenCompose(processor -> {
                // Verificar permissões do processador
                if (processor == null || processor.getGuildId() != application.getGuildId() || 
                    (processor.getRole() != GuildMember.Role.LEADER && processor.getRole() != GuildMember.Role.OFFICER)) {
                    return CompletableFuture.completedFuture(false);
                }
                
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        String sql = "UPDATE guild_applications SET status = ? WHERE id = ?";
                        
                        try (Connection conn = databaseManager.getConnection();
                             PreparedStatement stmt = conn.prepareStatement(sql)) {
                            
                            stmt.setString(1, status.name());
                            stmt.setInt(2, applicationId);
                            
                            int affectedRows = stmt.executeUpdate();
                            if (affectedRows > 0) {
                                logger.info("Aplicação processada: " + application.getPlayerName() + " -> " + status.name());
                                
                                // Registrar log de processamento de aplicação
                                getGuildByIdAsync(application.getGuildId()).thenAccept(guild -> {
                                    if (guild != null) {
                                        GuildLog.LogType logType = status == GuildApplication.ApplicationStatus.APPROVED ? 
                                            GuildLog.LogType.APPLICATION_ACCEPTED : GuildLog.LogType.APPLICATION_REJECTED;
                                        String description = status == GuildApplication.ApplicationStatus.APPROVED ? "Aplicação Aceita" : "Aplicação Recusada";
                                        String details = "Candidato: " + application.getPlayerName() + ", Processador: " + processor.getPlayerName();
                                        
                                        logGuildActionAsync(application.getGuildId(), guild.getName(), 
                                            processorUuid.toString(), processor.getPlayerName(),
                                            logType, description, details);
                                    }
                                });
                                
                                return true;
                            }
                        }
                    } catch (SQLException e) {
                        logger.severe("Erro ao processar aplicação: " + e.getMessage());
                    }
                    return false;
                }).thenCompose(success -> {
                    if (success && status == GuildApplication.ApplicationStatus.APPROVED) {
                        // Se a aplicação for aprovada, adicionar membro automaticamente
                        return addGuildMemberAsync(application.getGuildId(), application.getPlayerUuid(), 
                                                  application.getPlayerName(), GuildMember.Role.MEMBER);
                    }
                    return CompletableFuture.completedFuture(success);
                });
            });
        });
    }
    
    /**
     * Processar aplicação (Wrapper Síncrono)
     */
    public boolean processApplication(int applicationId, GuildApplication.ApplicationStatus status, UUID processorUuid) {
        try {
            return processApplicationAsync(applicationId, status, processorUuid).get();
        } catch (Exception e) {
            logger.severe("Exceção ao processar aplicação: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verificar se há aplicações pendentes (Assíncrono)
     */
    public CompletableFuture<Boolean> hasPendingApplicationAsync(UUID playerUuid, int guildId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT COUNT(*) FROM guild_applications WHERE player_uuid = ? AND guild_id = ? AND status = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setString(1, playerUuid.toString());
                    stmt.setInt(2, guildId);
                    stmt.setString(3, GuildApplication.ApplicationStatus.PENDING.name());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getInt(1) > 0;
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Erro ao verificar aplicações pendentes: " + e.getMessage());
            }
            return false;
        });
    }
    
    /**
     * Verificar se há aplicações pendentes (Wrapper Síncrono)
     */
    public boolean hasPendingApplication(UUID playerUuid, int guildId) {
        try {
            return hasPendingApplicationAsync(playerUuid, guildId).get();
        } catch (Exception e) {
            logger.severe("Exceção ao verificar aplicações pendentes: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Obter lista de aplicações da guilda (Assíncrono)
     */
    public CompletableFuture<List<GuildApplication>> getGuildApplicationsAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> {
            List<GuildApplication> applications = new ArrayList<>();
            try {
                String sql = "SELECT * FROM guild_applications WHERE guild_id = ? ORDER BY created_at DESC";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, guildId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            applications.add(createGuildApplicationFromResultSet(rs));
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Erro ao obter lista de aplicações da guilda: " + e.getMessage());
            }
            return applications;
        });
    }
    
    /**
     * Obter lista de aplicações da guilda (Wrapper Síncrono)
     */
    public List<GuildApplication> getGuildApplications(int guildId) {
        try {
            return getGuildApplicationsAsync(guildId).get();
        } catch (Exception e) {
            logger.severe("Exceção ao obter lista de aplicações da guilda: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Obter lista de aplicações do jogador (Assíncrono)
     */
    public CompletableFuture<List<GuildApplication>> getPlayerApplicationsAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<GuildApplication> applications = new ArrayList<>();
            try {
                String sql = "SELECT * FROM guild_applications WHERE player_uuid = ? ORDER BY created_at DESC";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setString(1, playerUuid.toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            applications.add(createGuildApplicationFromResultSet(rs));
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Erro ao obter lista de aplicações do jogador: " + e.getMessage());
            }
            return applications;
        });
    }
    
    /**
     * Obter lista de aplicações do jogador (Wrapper Síncrono)
     */
    public List<GuildApplication> getPlayerApplications(UUID playerUuid) {
        try {
            return getPlayerApplicationsAsync(playerUuid).get();
        } catch (Exception e) {
            logger.severe("Exceção ao obter lista de aplicações do jogador: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Obter aplicação por ID (Assíncrono)
     */
    public CompletableFuture<GuildApplication> getApplicationByIdAsync(int applicationId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT * FROM guild_applications WHERE id = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, applicationId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return createGuildApplicationFromResultSet(rs);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Erro ao obter aplicação por ID: " + e.getMessage());
            }
            return null;
        });
    }
    
    /**
     * Obter aplicação por ID (Wrapper Síncrono)
     */
    public GuildApplication getApplicationById(int applicationId) {
        try {
            return getApplicationByIdAsync(applicationId).get();
        } catch (Exception e) {
            logger.severe("Exceção ao obter aplicação por ID: " + e.getMessage());
            return null;
        }
    }
    
        /**
     * Criar objeto GuildApplication a partir do ResultSet
     */
    private GuildApplication createGuildApplicationFromResultSet(ResultSet rs) throws SQLException {
        GuildApplication application = new GuildApplication();
        application.setId(rs.getInt("id"));
        application.setGuildId(rs.getInt("guild_id"));
        application.setPlayerUuid(UUID.fromString(rs.getString("player_uuid")));
        application.setPlayerName(rs.getString("player_name"));
        application.setMessage(rs.getString("message"));
                 application.setStatus(GuildApplication.ApplicationStatus.valueOf(rs.getString("status")));
         application.setCreatedAt(parseTimestamp(rs, "created_at"));
         
         return application;
     }
     

     // ==================== Sistema de Convites ====================
     
     /**
      * Enviar convite (Assíncrono)
      */
     public CompletableFuture<Boolean> sendInvitationAsync(int guildId, UUID inviterUuid, String inviterName, UUID targetUuid, String targetName) {
         return getPlayerGuildAsync(targetUuid).thenCompose(existingGuild -> {
             if (existingGuild != null) {
                 return CompletableFuture.completedFuture(false);
             }
             
             return getPendingInvitationAsync(targetUuid, guildId).thenCompose(existingInvitation -> {
                 if (existingInvitation != null) {
                     return CompletableFuture.completedFuture(false);
                 }
                 
                 return CompletableFuture.supplyAsync(() -> {
                     try {
                         String sql = "INSERT INTO guild_invites (guild_id, player_uuid, player_name, inviter_uuid, inviter_name, status, expires_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                         
                         try (Connection conn = databaseManager.getConnection();
                              PreparedStatement stmt = conn.prepareStatement(sql)) {
                         
                             stmt.setInt(1, guildId);
                             stmt.setString(2, targetUuid.toString());
                             stmt.setString(3, targetName);
                             stmt.setString(4, inviterUuid.toString());
                             stmt.setString(5, inviterName);
                             stmt.setString(6, "PENDING");
                             stmt.setString(7, plusMinutesString(30));
                             stmt.setString(8, nowString());
                         
                             int affectedRows = stmt.executeUpdate();
                             if (affectedRows > 0) {
                                 logger.info("Convite enviado com sucesso: " + inviterName + " -> " + targetName + " (ID da Guilda: " + guildId + ")");
                                 return true;
                             }
                         }
                     } catch (SQLException e) {
                         logger.severe("Erro ao enviar convite: " + e.getMessage());
                     }
                     return false;
                 });
             });
         });
     }
     
     /**
      * Enviar convite (Wrapper Síncrono)
      */
     public boolean sendInvitation(int guildId, UUID inviterUuid, String inviterName, UUID targetUuid, String targetName) {
         try {
             return sendInvitationAsync(guildId, inviterUuid, inviterName, targetUuid, targetName).get();
         } catch (Exception e) {
             logger.severe("Exceção ao enviar convite: " + e.getMessage());
             return false;
         }
     }
     
     /**
      * Processar convite (Assíncrono)
      */
     public CompletableFuture<Boolean> processInvitationAsync(UUID targetUuid, UUID inviterUuid, boolean accept) {
         return getPendingInvitationAsync(targetUuid, inviterUuid).thenCompose(invitation -> {
             if (invitation == null) {
                 return CompletableFuture.completedFuture(false);
             }
             
             return CompletableFuture.supplyAsync(() -> {
                 try {
                     String status = accept ? "ACCEPTED" : "DECLINED";
                     String sql = "UPDATE guild_invites SET status = ? WHERE player_uuid = ? AND inviter_uuid = ? AND status = 'PENDING'";
                     
                     try (Connection conn = databaseManager.getConnection();
                          PreparedStatement stmt = conn.prepareStatement(sql)) {
                         
                         stmt.setString(1, status);
                         stmt.setString(2, targetUuid.toString());
                         stmt.setString(3, inviterUuid.toString());
                         
                         int affectedRows = stmt.executeUpdate();
                         if (affectedRows > 0) {
                             logger.info("Convite processado com sucesso: " + targetUuid + " -> " + status);
                             return true;
                         }
                         return false;
                     }
                 } catch (SQLException e) {
                     logger.severe("Erro ao processar convite: " + e.getMessage());
                 }
                 return false;
             }).thenCompose(success -> {
                 if (success && accept) {
                     // Se o convite for aceito, adicionar jogador à guilda
                     return addGuildMemberAsync(invitation.getGuildId(), targetUuid, invitation.getTargetName(), GuildMember.Role.MEMBER);
                 }
                 return CompletableFuture.completedFuture(success);
             });
         });
     }
     
     /**
      * Processar convite (Wrapper Síncrono)
      */
     public boolean processInvitation(UUID targetUuid, UUID inviterUuid, boolean accept) {
         try {
             return processInvitationAsync(targetUuid, inviterUuid, accept).get();
         } catch (Exception e) {
             logger.severe("Exceção ao processar convite: " + e.getMessage());
             return false;
         }
     }
     
     /**
      * Obter convite pendente (Assíncrono)
      */
     public CompletableFuture<GuildInvitation> getPendingInvitationAsync(UUID targetUuid, UUID inviterUuid) {
         return CompletableFuture.supplyAsync(() -> {
             try {
                 String sql = "SELECT * FROM guild_invites WHERE player_uuid = ? AND inviter_uuid = ? AND status = 'PENDING' AND expires_at > ? ORDER BY created_at DESC LIMIT 1";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                 
                     stmt.setString(1, targetUuid.toString());
                     stmt.setString(2, inviterUuid.toString());
                     stmt.setString(3, nowString());
                 
                     try (ResultSet rs = stmt.executeQuery()) {
                         if (rs.next()) {
                             return createGuildInvitationFromResultSet(rs);
                         }
                     }
                 }
             } catch (SQLException e) {
                 logger.severe("Erro ao obter convites pendentes: " + e.getMessage());
             }
             return null;
         });
     }
     
     /**
      * Obter convite pendente (Wrapper Síncrono)
      */
     public GuildInvitation getPendingInvitation(UUID targetUuid, UUID inviterUuid) {
         try {
             return getPendingInvitationAsync(targetUuid, inviterUuid).get();
         } catch (Exception e) {
             logger.severe("Exceção ao obter convite: " + e.getMessage());
             return null;
         }
     }
     
     /**
      * Obter convite pendente do jogador (Assíncrono)
      */
     public CompletableFuture<GuildInvitation> getPendingInvitationAsync(UUID targetUuid, int guildId) {
         return CompletableFuture.supplyAsync(() -> {
             try {
                 String sql = "SELECT * FROM guild_invites WHERE player_uuid = ? AND guild_id = ? AND status = 'PENDING' AND expires_at > ? ORDER BY created_at DESC LIMIT 1";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                 
                     stmt.setString(1, targetUuid.toString());
                     stmt.setInt(2, guildId);
                     stmt.setString(3, nowString());
                 
                     try (ResultSet rs = stmt.executeQuery()) {
                         if (rs.next()) {
                             return createGuildInvitationFromResultSet(rs);
                         }
                     }
                 }
             } catch (SQLException e) {
                 logger.severe("Erro ao obter convite: " + e.getMessage());
             }
             return null;
         });
     }
     
     /**
      * Obter convite pendente do jogador (Wrapper Síncrono)
      */
     public GuildInvitation getPendingInvitation(UUID targetUuid, int guildId) {
         try {
             return getPendingInvitationAsync(targetUuid, guildId).get();
         } catch (Exception e) {
             logger.severe("Exceção ao obter convite: " + e.getMessage());
             return null;
         }
     }
     
     /**
      * Criar objeto GuildInvitation a partir do ResultSet
      */
     private GuildInvitation createGuildInvitationFromResultSet(ResultSet rs) throws SQLException {
         GuildInvitation invitation = new GuildInvitation();
         invitation.setId(rs.getInt("id"));
         invitation.setGuildId(rs.getInt("guild_id"));
         invitation.setTargetUuid(UUID.fromString(rs.getString("player_uuid")));
         invitation.setTargetName(rs.getString("player_name"));
         invitation.setInviterUuid(UUID.fromString(rs.getString("inviter_uuid")));
         invitation.setInviterName(rs.getString("inviter_name"));
         invitation.setStatus(GuildInvitation.InvitationStatus.valueOf(rs.getString("status")));
         invitation.setInvitedAt(parseTimestamp(rs, "created_at"));
         invitation.setExpiresAt(parseTimestamp(rs, "expires_at"));
         return invitation;
     }
     
     /**
      * Obter aplicações pendentes (Assíncrono)
      */
     public CompletableFuture<List<GuildApplication>> getPendingApplicationsAsync(int guildId) {
         return CompletableFuture.supplyAsync(() -> {
             List<GuildApplication> applications = new ArrayList<>();
             try {
                 String sql = "SELECT * FROM guild_applications WHERE guild_id = ? AND status = 'PENDING' ORDER BY created_at DESC";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                     
                     stmt.setInt(1, guildId);
                     
                     try (ResultSet rs = stmt.executeQuery()) {
                         while (rs.next()) {
                             applications.add(createGuildApplicationFromResultSet(rs));
                         }
                     }
                 }
             } catch (SQLException e) {
                 logger.severe("Erro ao obter solicitações pendentes: " + e.getMessage());
             }
             return applications;
         });
     }
     
     /**
      * Obter histórico de aplicações (Assíncrono)
      */
     public CompletableFuture<List<GuildApplication>> getApplicationHistoryAsync(int guildId) {
         return CompletableFuture.supplyAsync(() -> {
             List<GuildApplication> applications = new ArrayList<>();
             try {
                 String sql = "SELECT * FROM guild_applications WHERE guild_id = ? AND status != 'PENDING' ORDER BY created_at DESC";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                     
                     stmt.setInt(1, guildId);
                     
                     try (ResultSet rs = stmt.executeQuery()) {
                         while (rs.next()) {
                             applications.add(createGuildApplicationFromResultSet(rs));
                         }
                     }
                 }
             } catch (SQLException e) {
                 logger.severe("Erro ao obter histórico de solicitações: " + e.getMessage());
             }
             return applications;
         });
     }
     
     /**
      * Obter membro da guilda (Assíncrono) - Método sobrecarregado, aceita parâmetro guildId
      */
     public CompletableFuture<GuildMember> getGuildMemberAsync(int guildId, UUID playerUuid) {
         return CompletableFuture.supplyAsync(() -> {
             try {
                 String sql = "SELECT * FROM guild_members WHERE guild_id = ? AND player_uuid = ?";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                     
                     stmt.setInt(1, guildId);
                     stmt.setString(2, playerUuid.toString());
                     
                     try (ResultSet rs = stmt.executeQuery()) {
                         if (rs.next()) {
                             return createGuildMemberFromResultSet(rs);
                         }
                     }
                 }
             } catch (SQLException e) {
                 logger.severe("Erro ao obter membro da guilda: " + e.getMessage());
             }
             return null;
         });
     }
     
     /**
      * Atualizar descrição da guilda (Assíncrono)
      */
     public CompletableFuture<Boolean> updateGuildDescriptionAsync(int guildId, String description) {
         return CompletableFuture.supplyAsync(() -> {
             try {
                 String sql = "UPDATE guilds SET description = ? WHERE id = ?";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                     
                     stmt.setString(1, description);
                     stmt.setInt(2, guildId);
                     
                     int rowsAffected = stmt.executeUpdate();
                     return rowsAffected > 0;
                 }
             } catch (SQLException e) {
                 logger.severe("Erro ao atualizar descrição da guilda: " + e.getMessage());
                 return false;
             }
         });
     }
     
     /**
      * Atualizar banner da guilda (Assíncrono)
      */
     public CompletableFuture<Boolean> updateGuildBannerAsync(int guildId, org.bukkit.inventory.ItemStack banner) {
         return CompletableFuture.supplyAsync(() -> {
             try {
                 String bannerData = com.guild.core.utils.BannerSerializer.serialize(banner);
                 String sql = "UPDATE guilds SET banner_data = ? WHERE id = ?";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                     
                     stmt.setString(1, bannerData);
                     stmt.setInt(2, guildId);
                     
                     int rowsAffected = stmt.executeUpdate();
                     return rowsAffected > 0;
                 }
             } catch (SQLException e) {
                 logger.severe("Erro ao atualizar banner da guilda: " + e.getMessage());
                 return false;
             }
         });
     }
     
     // ==================== Sistema de Relações de Guilda ====================
     
     /**
      * Criar relação de guilda (Assíncrono)
      */
     public CompletableFuture<Boolean> createGuildRelationAsync(int guild1Id, int guild2Id, String guild1Name, String guild2Name,
                                                              GuildRelation.RelationType type, UUID initiatorUuid, String initiatorName) {
         return CompletableFuture.supplyAsync(() -> {
             try {
                 String sql = "INSERT INTO guild_relations (guild1_id, guild2_id, guild1_name, guild2_name, relation_type, initiator_uuid, initiator_name, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                 
                     stmt.setInt(1, guild1Id);
                     stmt.setInt(2, guild2Id);
                     stmt.setString(3, guild1Name);
                     stmt.setString(4, guild2Name);
                     stmt.setString(5, type.name());
                     stmt.setString(6, initiatorUuid.toString());
                     stmt.setString(7, initiatorName);
                     stmt.setString(8, plusDaysString(7));
                     
                     int rowsAffected = stmt.executeUpdate();
                     return rowsAffected > 0;
                 }
             } catch (SQLException e) {
                 logger.severe("Erro ao criar relação de guilda: " + e.getMessage());
                 return false;
             }
         });
     }
     
     /**
      * Atualizar status da relação de guilda (Assíncrono)
      */
     public CompletableFuture<Boolean> updateGuildRelationStatusAsync(int relationId, GuildRelation.RelationStatus status) {
         return CompletableFuture.supplyAsync(() -> {
             try {
                 String sql = "UPDATE guild_relations SET status = ?, updated_at = ? WHERE id = ?";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                 
                     stmt.setString(1, status.name());
                     stmt.setString(2, nowString());
                     stmt.setInt(3, relationId);
                 
                     int rowsAffected = stmt.executeUpdate();
                     return rowsAffected > 0;
                 }
             } catch (SQLException e) {
                 logger.severe("Erro ao atualizar status da relação de guilda: " + e.getMessage());
                 return false;
             }
         });
     }
     
     /**
      * Obter relação de guilda (Assíncrono)
      */
     public CompletableFuture<GuildRelation> getGuildRelationAsync(int guild1Id, int guild2Id) {
         return CompletableFuture.supplyAsync(() -> {
             try {
                 String sql = "SELECT * FROM guild_relations WHERE (guild1_id = ? AND guild2_id = ?) OR (guild1_id = ? AND guild2_id = ?)";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                     
                     stmt.setInt(1, guild1Id);
                     stmt.setInt(2, guild2Id);
                     stmt.setInt(3, guild2Id);
                     stmt.setInt(4, guild1Id);
                     
                     try (ResultSet rs = stmt.executeQuery()) {
                         if (rs.next()) {
                             return createGuildRelationFromResultSet(rs);
                         }
                     }
                 }
             } catch (SQLException e) {
                 logger.severe("Erro ao obter relação de guilda: " + e.getMessage());
             }
             return null;
         });
     }
     
     /**
      * Obter todas as relações da guilda (Assíncrono)
      */
     public CompletableFuture<List<GuildRelation>> getGuildRelationsAsync(int guildId) {
         return CompletableFuture.supplyAsync(() -> {
             List<GuildRelation> relations = new ArrayList<>();
             try {
                 String sql = "SELECT * FROM guild_relations WHERE guild1_id = ? OR guild2_id = ? ORDER BY created_at DESC";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                     
                     stmt.setInt(1, guildId);
                     stmt.setInt(2, guildId);
                     
                     try (ResultSet rs = stmt.executeQuery()) {
                         while (rs.next()) {
                             relations.add(createGuildRelationFromResultSet(rs));
                         }
                     }
                 }
             } catch (SQLException e) {
                 logger.severe("Erro ao obter lista de relações de guilda: " + e.getMessage());
             }
             return relations;
         });
     }
     
     /**
      * Deletar relação de guilda (Assíncrono)
      */
     public CompletableFuture<Boolean> deleteGuildRelationAsync(int relationId) {
         return CompletableFuture.supplyAsync(() -> {
             try {
                 String sql = "DELETE FROM guild_relations WHERE id = ?";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                     
                     stmt.setInt(1, relationId);
                     
                     int rowsAffected = stmt.executeUpdate();
                     return rowsAffected > 0;
                 }
             } catch (SQLException e) {
                 logger.severe("Erro ao excluir relação de guilda: " + e.getMessage());
                 return false;
             }
         });
     }
     

     // ==================== Métodos Auxiliares ====================
     
     private GuildRelation createGuildRelationFromResultSet(ResultSet rs) throws SQLException {
         GuildRelation relation = new GuildRelation();
         relation.setId(rs.getInt("id"));
         relation.setGuild1Id(rs.getInt("guild1_id"));
         relation.setGuild2Id(rs.getInt("guild2_id"));
         relation.setGuild1Name(rs.getString("guild1_name"));
         relation.setGuild2Name(rs.getString("guild2_name"));
         relation.setType(GuildRelation.RelationType.valueOf(rs.getString("relation_type")));
         relation.setStatus(GuildRelation.RelationStatus.valueOf(rs.getString("status")));
         relation.setInitiatorUuid(UUID.fromString(rs.getString("initiator_uuid")));
         relation.setInitiatorName(rs.getString("initiator_name"));
         relation.setCreatedAt(parseTimestamp(rs, "created_at"));
         relation.setUpdatedAt(parseTimestamp(rs, "updated_at"));
         
         String expiresAt = rs.getString("expires_at");
         if (expiresAt != null) {
             relation.setExpiresAt(parseTimestamp(rs, "expires_at"));
         }
         
         return relation;
     }
     


    /**
     * Atualizar nível da guilda (Assíncrono)
     */
    public CompletableFuture<Boolean> updateGuildLevelAsync(int guildId, int level) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "UPDATE guilds SET level = ? WHERE id = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, level);
                    stmt.setInt(2, guildId);
                    
                    int affectedRows = stmt.executeUpdate();
                    return affectedRows > 0;
                }
            } catch (SQLException e) {
                logger.severe("Erro ao atualizar nível da guilda: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Atualizar número máximo de membros da guilda (Assíncrono)
     */
    public CompletableFuture<Boolean> updateGuildMaxMembersAsync(int guildId, int maxMembers) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "UPDATE guilds SET max_members = ? WHERE id = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, maxMembers);
                    stmt.setInt(2, guildId);
                    
                    int affectedRows = stmt.executeUpdate();
                    return affectedRows > 0;
                }
            } catch (SQLException e) {
                logger.severe("Erro ao atualizar número máximo de membros da guilda: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Atualizar status de congelamento da guilda (Assíncrono)
     */
    public CompletableFuture<Boolean> updateGuildFrozenStatusAsync(int guildId, boolean frozen) {
        return getGuildByIdAsync(guildId).thenCompose(guild -> {
            if (guild == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return CompletableFuture.supplyAsync(() -> {
                try {
                    String sql = "UPDATE guilds SET frozen = ? WHERE id = ?";
                    
                    try (Connection conn = databaseManager.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(sql)) {
                        
                        stmt.setBoolean(1, frozen);
                        stmt.setInt(2, guildId);
                        
                        int affectedRows = stmt.executeUpdate();
                        if (affectedRows > 0) {
                            // Registrar log de alteração de status de congelamento
                            GuildLog.LogType logType = frozen ? GuildLog.LogType.GUILD_FROZEN : GuildLog.LogType.GUILD_UNFROZEN;
                            String description = frozen ? "Guilda congelada" : "Guilda descongelada";
                            
                            logGuildActionAsync(guildId, guild.getName(), "SYSTEM", "Sistema",
                                logType, description, "Ação: " + (frozen ? "Congelar" : "Descongelar"));
                            
                            return true;
                        }
                    }
                } catch (SQLException e) {
                    logger.severe("Erro ao atualizar status de congelamento da guilda: " + e.getMessage());
                }
                return false;
            });
        });
    }

    /**
     * Inserir membro da guilda diretamente (sem verificação de guilda existente).
     * Usado apenas para inserir o líder após a criação da guilda, para evitar contenção de conexão extra.
     */
    private CompletableFuture<Boolean> addGuildMemberDirectAsync(int guildId, UUID playerUuid, String playerName, GuildMember.Role role) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "INSERT INTO guild_members (guild_id, player_uuid, player_name, role, joined_at) VALUES (?, ?, ?, ?, ?)";
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, guildId);
                    stmt.setString(2, playerUuid.toString());
                    stmt.setString(3, playerName);
                    stmt.setString(4, role.name());
                    stmt.setString(5, nowString());
                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows > 0) {
                        try { plugin.getPermissionManager().updatePlayerPermissions(playerUuid); } catch (Exception ignored) {}
                        return true;
                    }
                }
            } catch (SQLException e) {
                logger.severe("Erro ao adicionar membro da guilda diretamente: " + e.getMessage());
            }
            return false;
        });
    }


    
    /**
     * Notificar membros da guilda sobre atualização bem-sucedida
     */
    private void notifyGuildMembersOfUpgrade(int guildId, int newLevel, int newMaxMembers) {
        getGuildMembersAsync(guildId).thenAccept(members -> {
            String message = plugin.getConfigManager().getMessagesConfig().getString("economy.level-up", "&aGuilda atualizada com sucesso! Nível atual: {level}")
                .replace("{level}", String.valueOf(newLevel))
                .replace("{max_members}", String.valueOf(newMaxMembers));
            
            // Enviar mensagem na thread principal
            CompatibleScheduler.runTask(plugin, () -> {
                for (GuildMember member : members) {
                    Player player = Bukkit.getPlayer(member.getPlayerUuid());
                    if (player != null && player.isOnline()) {
                        player.sendMessage(com.guild.core.utils.ColorUtils.colorize(message));
                    }
                }
            });
        }).exceptionally(throwable -> {
            logger.warning("Erro ao notificar membros da guilda sobre a atualização: " + throwable.getMessage());
            return null;
        });
    }
    
    // ==================== Sistema de Logs da Guilda ====================
    
    /**
     * Registrar ação da guilda (Assíncrono)
     */
    public CompletableFuture<Boolean> logGuildActionAsync(int guildId, String guildName, String playerUuid, 
                                                        String playerName, GuildLog.LogType logType, 
                                                        String description, String details) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "INSERT INTO guild_logs (guild_id, guild_name, player_uuid, player_name, log_type, description, details, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                 
                     stmt.setInt(1, guildId);
                     stmt.setString(2, guildName);
                     stmt.setString(3, playerUuid);
                     stmt.setString(4, playerName);
                     stmt.setString(5, logType.name());
                     stmt.setString(6, description);
                     stmt.setString(7, details);
                     stmt.setString(8, nowString());
                 
                     int affectedRows = stmt.executeUpdate();
                     return affectedRows > 0;
                 }
            } catch (SQLException e) {
                logger.severe("Erro ao registrar log da guilda: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Registrar ação da guilda (Wrapper Síncrono)
     */
    public boolean logGuildAction(int guildId, String guildName, String playerUuid, String playerName, 
                                GuildLog.LogType logType, String description, String details) {
        try {
            return logGuildActionAsync(guildId, guildName, playerUuid, playerName, logType, description, details).get();
        } catch (Exception e) {
            logger.severe("Exceção ao registrar log da guilda: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Obter lista de logs da guilda (Assíncrono)
     */
    public CompletableFuture<List<GuildLog>> getGuildLogsAsync(int guildId, int limit, int offset) {
        return CompletableFuture.supplyAsync(() -> {
            List<GuildLog> logs = new ArrayList<>();
            try {
                String sql = "SELECT * FROM guild_logs WHERE guild_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, guildId);
                    stmt.setInt(2, limit);
                    stmt.setInt(3, offset);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            GuildLog log = createGuildLogFromResultSet(rs);
                            logs.add(log);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Erro ao obter logs da guilda: " + e.getMessage());
            }
            return logs;
        });
    }
    
    /**
     * Obter lista de logs da guilda (Wrapper Síncrono)
     */
    public List<GuildLog> getGuildLogs(int guildId, int limit, int offset) {
        try {
            return getGuildLogsAsync(guildId, limit, offset).get();
        } catch (Exception e) {
            logger.severe("Exceção ao obter logs da guilda: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Obter contagem total de logs da guilda (Assíncrono)
     */
    public CompletableFuture<Integer> getGuildLogsCountAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT COUNT(*) FROM guild_logs WHERE guild_id = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, guildId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getInt(1);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Erro ao obter contagem total de logs da guilda: " + e.getMessage());
            }
            return 0;
        });
    }
    
    /**
     * Obter contagem total de logs da guilda (Wrapper Síncrono)
     */
    public int getGuildLogsCount(int guildId) {
        try {
            return getGuildLogsCountAsync(guildId).get();
        } catch (Exception e) {
            logger.severe("Exceção ao obter contagem total de logs da guilda: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Criar objeto GuildLog a partir do ResultSet
     */
    private GuildLog createGuildLogFromResultSet(ResultSet rs) throws SQLException {
        GuildLog log = new GuildLog();
        log.setId(rs.getInt("id"));
        log.setGuildId(rs.getInt("guild_id"));
        log.setGuildName(rs.getString("guild_name"));
        log.setPlayerUuid(rs.getString("player_uuid"));
        log.setPlayerName(rs.getString("player_name"));
        log.setLogType(GuildLog.LogType.valueOf(rs.getString("log_type")));
        log.setDescription(rs.getString("description"));
        log.setDetails(rs.getString("details"));
        
        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null) {
            try {
                if (plugin.getDatabaseManager().getDatabaseType() == DatabaseManager.DatabaseType.SQLITE) {
                    log.setCreatedAt(LocalDateTime.parse(createdAtStr, com.guild.core.time.TimeProvider.FULL_FORMATTER));
                } else {
                    log.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                }
            } catch (Exception e) {
                logger.warning("Erro ao analisar data de criação do log: " + e.getMessage());
            }
        }
        
        return log;
    }
    
    /**
     * Limpar logs antigos (Assíncrono)
     */
    public CompletableFuture<Integer> cleanOldLogsAsync(int daysToKeep) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Usar parâmetro de tempo limite unificado (string) para evitar diferenças de fuso horário no banco de dados
                String sql = "DELETE FROM guild_logs WHERE created_at < ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    // Calcular string de limite: hora atual do sistema menos daysToKeep dias, formato yyyy-MM-dd HH:mm:ss
                    String threshold = com.guild.core.time.TimeProvider.nowLocalDateTime().minusDays(daysToKeep)
                        .format(com.guild.core.time.TimeProvider.FULL_FORMATTER);
                    stmt.setString(1, threshold);
                    int affectedRows = stmt.executeUpdate();
                    logger.info("Limpos " + affectedRows + " registros de log antigos");
                    return affectedRows;
                }
            } catch (SQLException e) {
                logger.severe("Erro ao limpar logs antigos: " + e.getMessage());
                return 0;
            }
        });
    }
    
    /**
     * Limpar logs antigos (Wrapper Síncrono)
     */
    public int cleanOldLogs(int daysToKeep) {
        try {
            return cleanOldLogsAsync(daysToKeep).get();
        } catch (Exception e) {
            logger.severe("Exceção ao limpar logs antigos: " + e.getMessage());
            return 0;
        }
    }
}
