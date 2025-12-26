package com.guild.core.database;

import com.guild.GuildPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class DatabaseManager {
    
    private final GuildPlugin plugin;
    private final Logger logger;
    private HikariDataSource dataSource;
    private DatabaseType databaseType;
    
    public DatabaseManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    public void initialize() {
        FileConfiguration config = plugin.getConfigManager().getDatabaseConfig();
        String type = config.getString("type", config.getString("database.type", "sqlite")).toLowerCase();
        
        try {
            if ("mysql".equals(type)) {
                initializeMySQL(config);
            } else {
                initializeSQLite(config);
            }
            
            createTables();
            
            logger.info("Conexão com banco de dados inicializada com sucesso: " + databaseType);
            
        } catch (Exception e) {
            logger.severe("Falha ao inicializar conexão com banco de dados: " + e.getMessage());
            throw new RuntimeException("Falha na conexão com banco de dados", e);
        }
    }
    
    private void initializeMySQL(FileConfiguration config) {
        databaseType = DatabaseType.MYSQL;
        
        HikariConfig hikariConfig = new HikariConfig();
        String host = config.getString("mysql.host", config.getString("database.mysql.host", "localhost"));
        int port = config.getInt("mysql.port", config.getInt("database.mysql.port", 3306));
        String database = config.getString("mysql.database", config.getString("database.mysql.database", "guild"));
        String params = "?useSSL=" + (config.getBoolean("mysql.use-ssl", config.getBoolean("database.mysql.use-ssl", false)) ? "true" : "false") +
                "&serverTimezone=" + config.getString("mysql.timezone", config.getString("database.mysql.timezone", "UTC")) +
                "&characterEncoding=" + config.getString("mysql.character-encoding", config.getString("database.mysql.character-encoding", "UTF-8"));
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + params);
        
        hikariConfig.setUsername(config.getString("mysql.username", config.getString("database.mysql.username", "root")));
        hikariConfig.setPassword(config.getString("mysql.password", config.getString("database.mysql.password", "")));
        hikariConfig.setMaximumPoolSize(config.getInt("mysql.pool-size", config.getInt("database.mysql.pool-size", 20)));
        hikariConfig.setMinimumIdle(config.getInt("mysql.min-idle", config.getInt("database.mysql.min-idle", 10)));
        hikariConfig.setConnectionTimeout(config.getLong("mysql.connection-timeout", config.getLong("database.mysql.connection-timeout", 60000)));
        hikariConfig.setIdleTimeout(config.getLong("mysql.idle-timeout", config.getLong("database.mysql.idle-timeout", 600000)));
        hikariConfig.setMaxLifetime(config.getLong("mysql.max-lifetime", config.getLong("database.mysql.max-lifetime", 1800000)));
        
        dataSource = new HikariDataSource(hikariConfig);
    }
    
    private void initializeSQLite(FileConfiguration config) {
        databaseType = DatabaseType.SQLITE;
        
        HikariConfig hikariConfig = new HikariConfig();
        String fileName = config.getString("sqlite.file", config.getString("database.sqlite.file", "guild.db"));
        String dbPath = plugin.getDataFolder() + "/" + fileName;
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbPath);
        int maxPool = config.getInt("connection-pool.maximum-pool-size", 2);
        if (maxPool < 1) { maxPool = 1; }
        hikariConfig.setMaximumPoolSize(maxPool);
        long connTimeout = config.getLong("connection-pool.connection-timeout", 10000);
        hikariConfig.setConnectionTimeout(connTimeout);
        long idleTimeout = config.getLong("connection-pool.idle-timeout", 600000);
        hikariConfig.setIdleTimeout(idleTimeout);
        long maxLifetime = config.getLong("connection-pool.max-lifetime", 1800000);
        hikariConfig.setMaxLifetime(maxLifetime);

        boolean walMode = config.getBoolean("sqlite.wal-mode", true);
        String synchronous = config.getString("sqlite.synchronous", "NORMAL");
        boolean foreignKeys = config.getBoolean("sqlite.foreign-keys", true);
        int cacheSize = config.getInt("sqlite.cache-size", 2000);
        int busyTimeoutMs = (int) config.getLong("sqlite.busy-timeout", 5000);
        StringBuilder initSql = new StringBuilder();
        if (walMode) {
            initSql.append("PRAGMA journal_mode=WAL;");
        }
        if (synchronous != null) {
            initSql.append("PRAGMA synchronous=").append(synchronous).append(";");
        }
        initSql.append("PRAGMA foreign_keys=").append(foreignKeys ? "ON" : "OFF").append(";");
        initSql.append("PRAGMA cache_size=").append(cacheSize).append(";");
        initSql.append("PRAGMA busy_timeout=").append(busyTimeoutMs).append(";");
        hikariConfig.setConnectionInitSql(initSql.toString());
        
        dataSource = new HikariDataSource(hikariConfig);
    }
    
    private void createTables() {
        if (databaseType == DatabaseType.SQLITE) {
            createSQLiteTables();
        } else {
            createMySQLTables();
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000);
                checkAndAddMissingColumns();
            } catch (Exception e) {
                logger.warning("Erro ao verificar colunas do banco de dados de forma assíncrona: " + e.getMessage());
            }
        });
        
        logger.info("Tabelas criadas com sucesso");
    }
    
    private void createSQLiteTables() {
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guilds (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL,
                tag TEXT UNIQUE,
                description TEXT,
                leader_uuid TEXT NOT NULL,
                leader_name TEXT NOT NULL,
                level INTEGER DEFAULT 1,
                max_members INTEGER DEFAULT 6,
                frozen INTEGER DEFAULT 0,
                banner_data TEXT,
                created_at TEXT DEFAULT (datetime('now','localtime')),
                updated_at TEXT DEFAULT (datetime('now','localtime'))
            )
        """);
        
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_members (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                role TEXT DEFAULT 'MEMBER',
                joined_at TEXT DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE,
                UNIQUE(guild_id, player_uuid)
            )
        """);
        
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_applications (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                message TEXT,
                status TEXT DEFAULT 'PENDING',
                created_at TEXT DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
        
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_invites (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                inviter_uuid TEXT NOT NULL,
                inviter_name TEXT NOT NULL,
                status TEXT DEFAULT 'PENDING',
                expires_at TEXT,
                created_at TEXT DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
        
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_relations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild1_id INTEGER NOT NULL,
                guild2_id INTEGER NOT NULL,
                guild1_name TEXT NOT NULL,
                guild2_name TEXT NOT NULL,
                relation_type TEXT NOT NULL,
                status TEXT DEFAULT 'PENDING',
                initiator_uuid TEXT NOT NULL,
                initiator_name TEXT NOT NULL,
                created_at TEXT DEFAULT (datetime('now','localtime')),
                updated_at TEXT DEFAULT (datetime('now','localtime')),
                expires_at TEXT,
                FOREIGN KEY (guild1_id) REFERENCES guilds(id) ON DELETE CASCADE,
                FOREIGN KEY (guild2_id) REFERENCES guilds(id) ON DELETE CASCADE,
                UNIQUE(guild1_id, guild2_id)
            )
        """);
        
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id INTEGER NOT NULL,
                guild_name TEXT NOT NULL,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                log_type TEXT NOT NULL,
                description TEXT NOT NULL,
                details TEXT,
                created_at TEXT DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
    }
    
    private void createMySQLTables() {
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guilds (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(50) UNIQUE NOT NULL,
                tag VARCHAR(10) UNIQUE,
                description TEXT,
                leader_uuid VARCHAR(36) NOT NULL,
                leader_name VARCHAR(16) NOT NULL,
                level INT DEFAULT 1,
                max_members INT DEFAULT 6,
                frozen BOOLEAN DEFAULT FALSE,
                banner_data TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
        """);
        
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_members (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild_id INT NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                role VARCHAR(20) DEFAULT 'MEMBER',
                joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE,
                UNIQUE KEY unique_guild_player (guild_id, player_uuid)
            )
        """);
        
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_applications (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild_id INT NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                message TEXT,
                status VARCHAR(20) DEFAULT 'PENDING',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
        
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_invites (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild_id INT NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                inviter_uuid VARCHAR(36) NOT NULL,
                inviter_name VARCHAR(16) NOT NULL,
                status VARCHAR(20) DEFAULT 'PENDING',
                expires_at TIMESTAMP NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
        
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_relations (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild1_id INT NOT NULL,
                guild2_id INT NOT NULL,
                guild1_name VARCHAR(50) NOT NULL,
                guild2_name VARCHAR(50) NOT NULL,
                relation_type VARCHAR(20) NOT NULL,
                status VARCHAR(20) DEFAULT 'PENDING',
                initiator_uuid VARCHAR(36) NOT NULL,
                initiator_name VARCHAR(16) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                expires_at TIMESTAMP NULL,
                FOREIGN KEY (guild1_id) REFERENCES guilds(id) ON DELETE CASCADE,
                FOREIGN KEY (guild2_id) REFERENCES guilds(id) ON DELETE CASCADE,
                UNIQUE KEY unique_guild_relation (guild1_id, guild2_id)
            )
        """);
        
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_logs (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild_id INT NOT NULL,
                guild_name VARCHAR(50) NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                log_type VARCHAR(50) NOT NULL,
                description TEXT NOT NULL,
                details TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
    }
    
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Conexão com banco de dados não inicializada");
        }
        return dataSource.getConnection();
    }
    
    public int executeUpdate(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            return stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.severe("Falha ao executar atualização: " + e.getMessage());
            throw new RuntimeException("Falha na operação do banco de dados", e);
        }
    }
    
    public CompletableFuture<Integer> executeUpdateAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> executeUpdate(sql, params));
    }
    
    public ResultSet executeQuery(String sql, Object... params) {
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            return stmt.executeQuery();
            
        } catch (SQLException e) {
            logger.severe("Falha ao executar consulta: " + e.getMessage());
            throw new RuntimeException("Falha na operação do banco de dados", e);
        }
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Conexão com banco de dados fechada");
        }
    }
    
    public DatabaseType getDatabaseType() {
        return databaseType;
    }
    
    private void checkAndAddMissingColumns() {
        try {
            if (databaseType == DatabaseType.SQLITE) {
                checkAndAddSQLiteColumns();
            } else {
                checkAndAddMySQLColumns();
            }
            logger.info("Verificação de colunas do banco de dados concluída");
        } catch (Exception e) {
            logger.warning("Erro ao verificar colunas do banco de dados: " + e.getMessage());
        }
    }
    
    private void checkAndAddSQLiteColumns() {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); 
            
            conn.commit(); 
        } catch (SQLException e) {
            logger.warning("Erro ao verificar colunas SQLite: " + e.getMessage());
        }
    }
    
    private void checkAndAddMySQLColumns() {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); 
            
            conn.commit(); 
        } catch (SQLException e) {
            logger.warning("Erro ao verificar colunas MySQL: " + e.getMessage());
        }
    }
    
    public enum DatabaseType {
        MYSQL, SQLITE
    }
}

