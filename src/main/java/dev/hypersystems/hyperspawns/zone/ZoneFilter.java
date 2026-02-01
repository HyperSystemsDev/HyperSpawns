package dev.hypersystems.hyperspawns.zone;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Filter criteria for determining which NPCs/mobs are affected by a spawn zone.
 * All conditions that are set must be met for a spawn to match.
 * If a filter field is null or empty, that criteria is not checked.
 */
public final class ZoneFilter {
    
    private final Set<String> npcGroups;
    private final Set<String> npcRoles;
    private final Integer minLightLevel;
    private final Integer maxLightLevel;
    private final Integer minYLevel;
    private final Integer maxYLevel;
    private final Set<String> timeOfDay;
    private final Set<Integer> moonPhases;
    
    // Compiled indices for fast lookup during spawn checks
    private transient IntSet compiledRoleIndices;
    private transient boolean compiled;
    
    private ZoneFilter(Builder builder) {
        this.npcGroups = builder.npcGroups.isEmpty() ? Collections.emptySet() : Set.copyOf(builder.npcGroups);
        this.npcRoles = builder.npcRoles.isEmpty() ? Collections.emptySet() : Set.copyOf(builder.npcRoles);
        this.minLightLevel = builder.minLightLevel;
        this.maxLightLevel = builder.maxLightLevel;
        this.minYLevel = builder.minYLevel;
        this.maxYLevel = builder.maxYLevel;
        this.timeOfDay = builder.timeOfDay.isEmpty() ? Collections.emptySet() : Set.copyOf(builder.timeOfDay);
        this.moonPhases = builder.moonPhases.isEmpty() ? Collections.emptySet() : Set.copyOf(builder.moonPhases);
        this.compiled = false;
    }
    
    /**
     * Create a new filter builder.
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Create an empty filter that matches everything.
     */
    @NotNull
    public static ZoneFilter empty() {
        return builder().build();
    }
    
    /**
     * Create a copy of this filter with a builder for modifications.
     */
    @NotNull
    public Builder toBuilder() {
        return new Builder()
                .npcGroups(new HashSet<>(npcGroups))
                .npcRoles(new HashSet<>(npcRoles))
                .minLightLevel(minLightLevel)
                .maxLightLevel(maxLightLevel)
                .minYLevel(minYLevel)
                .maxYLevel(maxYLevel)
                .timeOfDay(new HashSet<>(timeOfDay))
                .moonPhases(new HashSet<>(moonPhases));
    }
    
    /**
     * Check if this filter is empty (matches everything).
     */
    public boolean isEmpty() {
        return npcGroups.isEmpty() &&
               npcRoles.isEmpty() &&
               minLightLevel == null &&
               maxLightLevel == null &&
               minYLevel == null &&
               maxYLevel == null &&
               timeOfDay.isEmpty() &&
               moonPhases.isEmpty();
    }
    
    /**
     * Check if this filter has any NPC-based criteria.
     */
    public boolean hasNpcCriteria() {
        return !npcGroups.isEmpty() || !npcRoles.isEmpty();
    }
    
    /**
     * Check if this filter has any environmental criteria.
     */
    public boolean hasEnvironmentalCriteria() {
        return minLightLevel != null || maxLightLevel != null ||
               minYLevel != null || maxYLevel != null ||
               !timeOfDay.isEmpty() || !moonPhases.isEmpty();
    }
    
    /**
     * Get the NPC groups this filter applies to.
     */
    @NotNull
    public Set<String> getNpcGroups() {
        return npcGroups;
    }
    
    /**
     * Get the specific NPC roles this filter applies to.
     */
    @NotNull
    public Set<String> getNpcRoles() {
        return npcRoles;
    }
    
    /**
     * Get the minimum light level required, or null if not specified.
     */
    @Nullable
    public Integer getMinLightLevel() {
        return minLightLevel;
    }
    
    /**
     * Get the maximum light level allowed, or null if not specified.
     */
    @Nullable
    public Integer getMaxLightLevel() {
        return maxLightLevel;
    }
    
    /**
     * Get the minimum Y level required, or null if not specified.
     */
    @Nullable
    public Integer getMinYLevel() {
        return minYLevel;
    }
    
    /**
     * Get the maximum Y level allowed, or null if not specified.
     */
    @Nullable
    public Integer getMaxYLevel() {
        return maxYLevel;
    }
    
    /**
     * Get the times of day this filter applies to.
     * Valid values: "day", "night", "dawn", "dusk"
     */
    @NotNull
    public Set<String> getTimeOfDay() {
        return timeOfDay;
    }
    
    /**
     * Get the moon phases this filter applies to (0-7).
     */
    @NotNull
    public Set<Integer> getMoonPhases() {
        return moonPhases;
    }
    
    /**
     * Get compiled role indices for fast spawn checking.
     * Must call compile() first.
     */
    @Nullable
    public IntSet getCompiledRoleIndices() {
        return compiledRoleIndices;
    }
    
    /**
     * Check if this filter has been compiled.
     */
    public boolean isCompiled() {
        return compiled;
    }
    
    /**
     * Compile the filter for fast runtime lookups.
     * Call this after loading zones or modifying filters.
     * 
     * @param roleResolver Function to resolve role names to indices
     */
    public void compile(@NotNull RoleResolver roleResolver) {
        if (npcRoles.isEmpty() && npcGroups.isEmpty()) {
            // No NPC criteria - matches all roles
            compiledRoleIndices = null;
        } else {
            IntOpenHashSet indices = new IntOpenHashSet();
            
            // Add direct role indices
            for (String role : npcRoles) {
                int index = roleResolver.getRoleIndex(role);
                if (index >= 0) {
                    indices.add(index);
                }
            }
            
            // Add roles from groups
            for (String group : npcGroups) {
                IntSet groupRoles = roleResolver.getRolesInGroup(group);
                if (groupRoles != null) {
                    indices.addAll(groupRoles);
                }
            }
            
            compiledRoleIndices = indices.isEmpty() ? IntSets.EMPTY_SET : IntSets.unmodifiable(indices);
        }
        compiled = true;
    }
    
    /**
     * Check if this filter matches a specific role index.
     * Must be compiled first.
     */
    public boolean matchesRole(int roleIndex) {
        if (!compiled) {
            throw new IllegalStateException("Filter must be compiled before matching");
        }
        // null means matches all roles
        return compiledRoleIndices == null || compiledRoleIndices.contains(roleIndex);
    }
    
    /**
     * Check if the environmental criteria match.
     * 
     * @param lightLevel Current light level (0-15)
     * @param yLevel Current Y coordinate
     * @param currentTime Current time of day ("day", "night", etc.)
     * @param moonPhase Current moon phase (0-7)
     * @return true if all environmental criteria match
     */
    public boolean matchesEnvironment(int lightLevel, int yLevel, @Nullable String currentTime, int moonPhase) {
        if (minLightLevel != null && lightLevel < minLightLevel) return false;
        if (maxLightLevel != null && lightLevel > maxLightLevel) return false;
        if (minYLevel != null && yLevel < minYLevel) return false;
        if (maxYLevel != null && yLevel > maxYLevel) return false;
        if (!timeOfDay.isEmpty() && (currentTime == null || !timeOfDay.contains(currentTime.toLowerCase()))) return false;
        if (!moonPhases.isEmpty() && !moonPhases.contains(moonPhase)) return false;
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ZoneFilter{");
        boolean first = true;
        
        if (!npcGroups.isEmpty()) {
            sb.append("groups=").append(npcGroups);
            first = false;
        }
        if (!npcRoles.isEmpty()) {
            if (!first) sb.append(", ");
            sb.append("roles=").append(npcRoles);
            first = false;
        }
        if (minLightLevel != null || maxLightLevel != null) {
            if (!first) sb.append(", ");
            sb.append("light=[").append(minLightLevel != null ? minLightLevel : "*")
              .append("-").append(maxLightLevel != null ? maxLightLevel : "*").append("]");
            first = false;
        }
        if (minYLevel != null || maxYLevel != null) {
            if (!first) sb.append(", ");
            sb.append("y=[").append(minYLevel != null ? minYLevel : "*")
              .append("-").append(maxYLevel != null ? maxYLevel : "*").append("]");
            first = false;
        }
        if (!timeOfDay.isEmpty()) {
            if (!first) sb.append(", ");
            sb.append("time=").append(timeOfDay);
            first = false;
        }
        if (!moonPhases.isEmpty()) {
            if (!first) sb.append(", ");
            sb.append("moon=").append(moonPhases);
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Functional interface for resolving role names to indices.
     */
    @FunctionalInterface
    public interface RoleResolver {
        /**
         * Get the index for a role name.
         * @return The role index, or -1 if not found
         */
        int getRoleIndex(@NotNull String roleName);
        
        /**
         * Get all role indices in a group.
         * @return The set of role indices, or null if group not found
         */
        @Nullable
        default IntSet getRolesInGroup(@NotNull String groupName) {
            return null;
        }
    }
    
    /**
     * Builder for ZoneFilter.
     */
    public static final class Builder {
        private Set<String> npcGroups = new HashSet<>();
        private Set<String> npcRoles = new HashSet<>();
        private Integer minLightLevel;
        private Integer maxLightLevel;
        private Integer minYLevel;
        private Integer maxYLevel;
        private Set<String> timeOfDay = new HashSet<>();
        private Set<Integer> moonPhases = new HashSet<>();
        
        private Builder() {}
        
        public Builder addNpcGroup(@NotNull String group) {
            npcGroups.add(group);
            return this;
        }
        
        public Builder removeNpcGroup(@NotNull String group) {
            npcGroups.remove(group);
            return this;
        }
        
        public Builder npcGroups(@NotNull Set<String> groups) {
            this.npcGroups = groups;
            return this;
        }
        
        public Builder addNpcRole(@NotNull String role) {
            npcRoles.add(role);
            return this;
        }
        
        public Builder removeNpcRole(@NotNull String role) {
            npcRoles.remove(role);
            return this;
        }
        
        public Builder npcRoles(@NotNull Set<String> roles) {
            this.npcRoles = roles;
            return this;
        }
        
        public Builder minLightLevel(@Nullable Integer level) {
            this.minLightLevel = level;
            return this;
        }
        
        public Builder maxLightLevel(@Nullable Integer level) {
            this.maxLightLevel = level;
            return this;
        }
        
        public Builder lightLevelRange(@Nullable Integer min, @Nullable Integer max) {
            this.minLightLevel = min;
            this.maxLightLevel = max;
            return this;
        }
        
        public Builder minYLevel(@Nullable Integer level) {
            this.minYLevel = level;
            return this;
        }
        
        public Builder maxYLevel(@Nullable Integer level) {
            this.maxYLevel = level;
            return this;
        }
        
        public Builder yLevelRange(@Nullable Integer min, @Nullable Integer max) {
            this.minYLevel = min;
            this.maxYLevel = max;
            return this;
        }
        
        public Builder addTimeOfDay(@NotNull String time) {
            timeOfDay.add(time.toLowerCase());
            return this;
        }
        
        public Builder removeTimeOfDay(@NotNull String time) {
            timeOfDay.remove(time.toLowerCase());
            return this;
        }
        
        public Builder timeOfDay(@NotNull Set<String> times) {
            this.timeOfDay = times;
            return this;
        }
        
        public Builder addMoonPhase(int phase) {
            if (phase >= 0 && phase <= 7) {
                moonPhases.add(phase);
            }
            return this;
        }
        
        public Builder removeMoonPhase(int phase) {
            moonPhases.remove(phase);
            return this;
        }
        
        public Builder moonPhases(@NotNull Set<Integer> phases) {
            this.moonPhases = phases;
            return this;
        }
        
        @NotNull
        public ZoneFilter build() {
            return new ZoneFilter(this);
        }
    }
}
