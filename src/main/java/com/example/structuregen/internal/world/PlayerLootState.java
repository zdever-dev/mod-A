package com.example.structuregen.internal.world;

import com.example.structuregen.StructureGenMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-world persistent state pro player-instanced loot systém (viz A-10).
 *
 * <p>Uchovává záznamy o tom který hráč si již vzal loot z kterého kontejneru.
 * Záznamy přežívají restarty serveru.
 *
 * <h2>Datová struktura</h2>
 * <p>{@code Map<UUID containerUUID, Set<UUID playerUUIDs>>} —
 * pro každý kontejner sada hráčů kteří si loot vzali (CLAIMED).
 *
 * <h2>Přístup</h2>
 * <p>Vždy přes {@link #get(ServerLevel)}.
 */
public final class PlayerLootState extends SavedData {

    public static final String DATA_NAME = "structuregen_player_loot";

    /**
     * Mapa containerUUID → set playerUUIDs kteří si vzali loot.
     * Lazy-loaded — funguje i pro offline hráče.
     */
    private final Map<UUID, Set<UUID>> claimedLoot = new HashMap<>();

    // ---- Přístup ------------------------------------------------------------

    /**
     * Vrátí existující nebo nově vytvořenou instanci pro danou dimenzi.
     *
     * @param level server level; nesmí být {@code null}
     * @return instance pro tuto dimenzi
     */
    public static PlayerLootState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            PlayerLootState::load,  // Function<CompoundTag, T>
            PlayerLootState::new,   // Supplier<T>
            DATA_NAME               // název souboru
        );
    }

    /** Konstruktor pro novou prázdnou instanci. Voláno Forge jako Supplier. */
    private PlayerLootState() {}

    // ---- Public API ---------------------------------------------------------

    /**
     * Označí kontejner jako CLAIMED pro daného hráče.
     *
     * @param containerUUID UUID kontejneru (uloženo v BlockEntity NBT)
     * @param playerUUID    UUID hráče
     */
    public void markClaimed(UUID containerUUID, UUID playerUUID) {
        claimedLoot.computeIfAbsent(containerUUID, k -> new HashSet<>()).add(playerUUID);
        setDirty();
    }

    /**
     * Vrátí zda hráč již loot z kontejneru vzal.
     *
     * @param containerUUID UUID kontejneru
     * @param playerUUID    UUID hráče
     * @return {@code true} pokud je loot CLAIMED
     */
    public boolean isClaimed(UUID containerUUID, UUID playerUUID) {
        Set<UUID> players = claimedLoot.get(containerUUID);
        return players != null && players.contains(playerUUID);
    }

    /**
     * Smaže všechny CLAIMED záznamy pro kontejnery ze zadané množiny UUID.
     * Voláno při {@code /sgdebug reset} a {@code /sgdebug gc}.
     *
     * @param containerUUIDs sada UUID kontejnerů k vyčištění
     */
    public void cleanupForStructure(Set<UUID> containerUUIDs) {
        int removed = 0;
        for (UUID uuid : containerUUIDs) {
            if (claimedLoot.remove(uuid) != null) removed++;
        }
        if (removed > 0) {
            setDirty();
            StructureGenMod.LOGGER.debug(
                "PlayerLootState: cleaned up {} container CLAIMED records.", removed
            );
        }
    }

    /**
     * Vrátí immutable pohled na všechny CLAIMED záznamy.
     * Používáno pro orphaned UUID cleanup v {@code /sgdebug gc}.
     *
     * @return unmodifiable mapa; nikdy {@code null}
     */
    public Map<UUID, Set<UUID>> getAllClaimedEntries() {
        return Collections.unmodifiableMap(claimedLoot);
    }

    // ---- NBT serializace ----------------------------------------------------

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag claimedTag = new CompoundTag();
        for (Map.Entry<UUID, Set<UUID>> entry : claimedLoot.entrySet()) {
            ListTag playerList = new ListTag();
            for (UUID playerUUID : entry.getValue()) {
                playerList.add(StringTag.valueOf(playerUUID.toString()));
            }
            claimedTag.put(entry.getKey().toString(), playerList);
        }
        tag.put("claimed", claimedTag);
        return tag;
    }

    /**
     * Načte state z NBT.
     * Voláno Forge jako Function&lt;CompoundTag, PlayerLootState&gt;.
     *
     * @param tag zdrojový tag
     * @return nová instance s načteným state
     */
    private static PlayerLootState load(CompoundTag tag) {
        PlayerLootState state = new PlayerLootState();
        if (tag.contains("claimed", Tag.TAG_COMPOUND)) {
            CompoundTag claimedTag = tag.getCompound("claimed");
            for (String containerStr : claimedTag.getAllKeys()) {
                UUID containerUUID = UUID.fromString(containerStr);
                Set<UUID> players = new HashSet<>();
                ListTag playerList = claimedTag.getList(containerStr, Tag.TAG_STRING);
                for (int i = 0; i < playerList.size(); i++) {
                    players.add(UUID.fromString(playerList.getString(i)));
                }
                state.claimedLoot.put(containerUUID, players);
            }
        }
        StructureGenMod.LOGGER.debug(
            "PlayerLootState loaded: {} container entries.", state.claimedLoot.size()
        );
        return state;
    }
}