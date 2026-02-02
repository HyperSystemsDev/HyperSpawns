# HyperSpawns Troubleshooting Guide

This guide helps diagnose and resolve common issues with HyperSpawns.

## Table of Contents

1. [Zone Not Blocking Spawns](#zone-not-blocking-spawns)
2. [Mobs Still Appearing in Zone](#mobs-still-appearing-in-zone)
3. [Filter Not Working](#filter-not-working)
4. [Console Errors](#console-errors)
5. [Performance Issues](#performance-issues)
6. [Debug Mode](#debug-mode)
7. [Known Limitations](#known-limitations)

---

## Zone Not Blocking Spawns

This is the most common issue. Follow this checklist:

### 1. Check World Name Mismatch

**Symptom:** Zone appears to be configured correctly but spawns still occur.

**Diagnosis:**
```
/hyperspawns zone info <zone_name>
```

Look at the "World:" field. This must **exactly match** your server's world name.

**Common causes:**
- Zone created with world name "default" but server uses "world"
- Zone created in wrong world
- Case sensitivity issues

**Solution:**
You'll need to recreate the zone in the correct world, or manually edit `zones.json`.

---

### 2. Check Zone Mode

**Symptom:** Zone exists but doesn't block spawns.

**Diagnosis:**
```
/hyperspawns zone info <zone_name>
```

Look at the "Mode:" field.

**Only BLOCK and DENY modes suppress spawns:**
- `BLOCK` - Blocks all spawns
- `DENY` - Blocks spawns matching filter

**These modes don't block spawns:**
- `ALLOW` - Only blocks spawns NOT matching filter
- `MODIFY` - Adjusts rates, doesn't block
- `REPLACE` - Replaces mobs, doesn't block

**Solution:**
```
/hyperspawns zone mode <zone_name> BLOCK
```

---

### 3. Check If Zone Is Enabled

**Symptom:** Zone configured correctly but not working.

**Diagnosis:**
```
/hyperspawns zone info <zone_name>
```

Look at "Enabled:" field.

**Solution:**
```
/hyperspawns zone enable <zone_name>
```

---

### 4. Check Zone Boundary

**Symptom:** Spawns blocked in some areas but not others.

**Diagnosis:**
```
/hyperspawns zone info <zone_name>
```

Check the "Boundary:" description. Verify coordinates are correct.

**Common issues:**
- Coordinates swapped (pos1 and pos2 in wrong positions)
- Y range too limited (mobs spawn above/below zone)
- Sphere radius too small

**Solution:**
```
/hyperspawns pos1 <correct_x> <correct_y> <correct_z>
/hyperspawns pos2 <correct_x> <correct_y> <correct_z>
/hyperspawns zone redefine <zone_name>
```

---

### 5. Reload After Changes

**Symptom:** Changes made but not taking effect.

**Solution:**
```
/hyperspawns reload
```

This re-applies all zone suppressions to the world.

---

### 6. Check Console for Errors

Look for HyperSpawns messages in the server console:

```
[HyperSpawns] SpawnSuppressionController NOT AVAILABLE for world 'xxx'
```

This means the spawning plugin isn't loaded or isn't compatible.

---

## Mobs Still Appearing in Zone

Even with a working zone, you may see mobs in the area:

### Existing Mobs

**Issue:** Mobs that existed before the zone was created are not affected.

**Solution:** HyperSpawns only prevents new spawns. Existing mobs must be killed manually or will despawn naturally.

### Mobs Wandering In

**Issue:** Mobs spawn outside the zone and walk into it.

**Solution:** Expand your zone boundaries to include surrounding areas, or use a mob removal system.

### Spawner Blocks

**Issue:** Mob spawners may not be affected by zone suppression.

**Note:** This depends on how Hytale handles spawner blocks vs. natural spawning.

### Chunk Boundary Issues

**Issue:** Zone appears to not cover an area you expected.

**Explanation:** Zones are applied per-chunk. If your zone boundary crosses a chunk, both chunks are affected, but the boundary check is per-block within the chunk.

**Debug:** Enable debug mode and check console output when spawns occur.

---

## Filter Not Working

### Check Mode Compatibility

Filters only apply to certain modes:
- `BLOCK` - **Ignores filters** (blocks everything)
- `ALLOW` - Uses filter as whitelist
- `DENY` - Uses filter as blacklist
- `MODIFY` - Uses filter to select affected mobs
- `REPLACE` - Uses filter to select affected mobs

If you're using BLOCK mode, filters have no effect.

### Check Filter Syntax

View current filters:
```
/hyperspawns zone info <zone_name>
```

Filters should appear like:
```
Filter: ZoneFilter{groups=[hostile], roles=[zombie]}
```

### Filter Logic

Multiple filters use AND logic - all conditions must match:

```
group=hostile AND time=night AND maxlight=7
```

If a mob doesn't match ALL criteria, it won't be affected.

### Role Names

NPC role names must match exactly what the server uses. Check your server's NPC definitions for correct names.

---

## Console Errors

### "SpawnSuppressionController NOT AVAILABLE"

**Full message:**
```
[HyperSpawns] SpawnSuppressionController NOT AVAILABLE for world 'xxx'. Spawn suppression will NOT work!
```

**Cause:** The SpawningPlugin or its suppression system isn't loaded.

**Solutions:**
1. Ensure SpawningPlugin is installed and enabled
2. Check load order - HyperSpawns may be loading before SpawningPlugin
3. Verify Hytale server version compatibility

### "Failed to load configuration"

**Cause:** Syntax error in config.json

**Solution:**
1. Check config.json for valid JSON syntax
2. Delete config.json and let it regenerate
3. Check file permissions

### "Failed to load zones"

**Cause:** Syntax error or corruption in zones.json

**Solution:**
1. Backup zones.json
2. Validate JSON syntax
3. Check for truncated file
4. If needed, delete and recreate zones

---

## Performance Issues

### High Memory Usage

**Diagnosis:**
```
/hyperspawns stats
```

**Causes:**
- Too many zones (200+)
- Very large zones covering many chunks
- Many overlapping zones

**Solutions:**
1. Consolidate overlapping zones
2. Use fewer, larger zones instead of many small ones
3. Disable unused zones
4. Increase auto-save interval

### Lag When Creating Zones

**Cause:** Applying suppression to many chunks takes time.

**Solutions:**
1. Create zones during low-activity periods
2. Break very large zones into smaller pieces created over time

---

## Debug Mode

Debug mode provides detailed console output for diagnosing issues.

### Enable Debug Mode

```
/hyperspawns debug on
```

### Debug Output Includes

- Zone suppression applications
- World name matching
- Chunk processing details
- Filter compilation results
- Suppression entry counts

### Example Debug Output

```
[HyperSpawns] ChunkSuppressionIntegrator.applyToWorld() called for world 'world'
[HyperSpawns] SpawnSuppressionController found for world 'world'
[HyperSpawns] Checking 5 zones for world 'world'
[HyperSpawns] Skipping zone 'nether_zone': wrong world (zone='nether', current='world')
[HyperSpawns] Applied zone 'spawn_protection' suppression to 64 chunks
[HyperSpawns] Applied 4 zones affecting 256 chunks in world 'world'
```

### Disable Debug Mode

```
/hyperspawns debug off
```

Or set in config.json:
```json
{
  "debugMode": false
}
```

---

## Known Limitations

### 1. Existing Mobs Not Removed

HyperSpawns prevents new spawns but does not remove existing mobs. Consider using a companion mob-clearing system.

### 2. World Name Must Match Exactly

Zone world names are string matches. "World" != "world" != "WORLD".

### 3. No Cross-World Zones

A zone only affects one world. Create separate zones for each world.

### 4. Chunk-Based Suppression

Suppression data is stored per-chunk. Very small zones may affect more area than expected due to chunk boundaries.

### 5. No Runtime Filter Changes for BLOCK Mode

BLOCK mode ignores filters by design. Switch to DENY mode if you need filtering.

### 6. Spawner Compatibility

Mob spawner blocks may behave differently than natural spawning depending on Hytale's implementation.

---

## Getting Help

If you've tried these solutions and still have issues:

1. **Enable debug mode** and capture console output
2. **Run `/hyperspawns stats`** and note the output
3. **Check your zone** with `/hyperspawns zone info <name>`
4. **Join our Discord:** https://discord.gg/SNPjyfkYPc

When reporting issues, include:
- Server version
- HyperSpawns version
- Console errors (if any)
- Debug mode output
- Steps to reproduce

---

## See Also

- [Admin Guide](ADMIN-GUIDE.md) - Complete administration guide
- [Commands Reference](COMMANDS.md) - Command documentation
- [Configuration](CONFIGURATION.md) - Configuration options
