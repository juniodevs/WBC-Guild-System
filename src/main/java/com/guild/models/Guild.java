package com.guild.models;

import java.time.LocalDateTime;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;

/**
 * Modelo de dados da guilda
 */
public class Guild {
    
    private int id;
    private String name;
    private String tag;
    private String description;
    private UUID leaderUuid;
    private String leaderName;
    private int level;
    private int maxMembers;
    private boolean frozen;
    private ItemStack banner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public Guild() {}
    
    public Guild(String name, String tag, String description, UUID leaderUuid, String leaderName) {
        this.name = name;
        this.tag = tag;
        this.description = description;
        this.leaderUuid = leaderUuid;
        this.leaderName = leaderName;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getTag() {
        return tag;
    }
    
    public void setTag(String tag) {
        this.tag = tag;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public UUID getLeaderUuid() {
        return leaderUuid;
    }
    
    public void setLeaderUuid(UUID leaderUuid) {
        this.leaderUuid = leaderUuid;
    }
    
    public String getLeaderName() {
        return leaderName;
    }
    
    public void setLeaderName(String leaderName) {
        this.leaderName = leaderName;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    


    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public int getMaxMembers() {
        return maxMembers;
    }
    
    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }
    
    public boolean isFrozen() {
        return frozen;
    }
    
    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }
    
    public ItemStack getBanner() {
        return banner;
    }
    
    public void setBanner(ItemStack banner) {
        this.banner = banner;
    }
    

    @Override
    public String toString() {
        return "Guild{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", tag='" + tag + '\'' +
                ", description='" + description + '\'' +
                ", leaderUuid=" + leaderUuid +
                ", leaderName='" + leaderName + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
