package com.example.structuregen.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server → client packet that synchronises the visual state of a per-player
 * loot container (see Pillar 7 — Player-Instanced Loot, step A-10).
 *
 * <h2>Payload</h2>
 * <ul>
 *   <li>{@link #containerUUID} — UUID of the loot container block-entity.</li>
 *   <li>{@link #playerUUID}   — UUID of the player whose view is being updated.</li>
 *   <li>{@link #hasLoot}      — {@code true} if the container still holds
 *       unclaimed loot for this player; {@code false} means visually empty.</li>
 * </ul>
 *
 * <h2>Client handler</h2>
 * <p>The client handler (to be implemented in A-10-5) should update a
 * client-side cache keyed by {@code containerUUID → playerUUID → hasLoot} so
 * that the container block renderer can show the correct visual state without
 * querying the server on every render frame.
 *
 * <p><strong>Stub notice:</strong> {@code encode}, {@code decode} and
 * {@code handle} bodies are placeholders. Full implementation will be added in
 * step A-10-5.
 */
public final class ContainerStatePacket {

    /** UUID of the loot container block-entity being updated. */
    private final UUID containerUUID;

    /** UUID of the player for whom the visual state is being set. */
    private final UUID playerUUID;

    /**
     * {@code true}  → the container has unclaimed loot for this player.<br>
     * {@code false} → the container appears empty for this player.
     */
    private final boolean hasLoot;

    // ---- Constructors --------------------------------------------------------

    /**
     * Creates a new packet with explicit field values.
     *
     * @param containerUUID UUID of the loot container; never {@code null}
     * @param playerUUID    UUID of the target player; never {@code null}
     * @param hasLoot       {@code true} if the player has unclaimed loot here
     */
    public ContainerStatePacket(UUID containerUUID, UUID playerUUID, boolean hasLoot) {
        this.containerUUID = containerUUID;
        this.playerUUID    = playerUUID;
        this.hasLoot       = hasLoot;
    }

    // ---- Encoding / decoding (stub) -----------------------------------------

    /**
     * Serialises this packet into the given buffer.
     *
     * <p><em>Stub — full implementation in A-10-5.</em>
     *
     * @param buf target buffer; not {@code null}
     */
    public void encode(FriendlyByteBuf buf) {
        // TODO (A-10-5): buf.writeUUID(containerUUID); buf.writeUUID(playerUUID); buf.writeBoolean(hasLoot);
    }

    /**
     * Deserialises a {@link ContainerStatePacket} from the given buffer.
     *
     * <p><em>Stub — full implementation in A-10-5.</em>
     *
     * @param buf source buffer; not {@code null}
     * @return a placeholder packet instance
     */
    public static ContainerStatePacket decode(FriendlyByteBuf buf) {
        // TODO (A-10-5): return new ContainerStatePacket(buf.readUUID(), buf.readUUID(), buf.readBoolean());
        return new ContainerStatePacket(new UUID(0, 0), new UUID(0, 0), false);
    }

    /**
     * Handles this packet on the receiving end (client side).
     *
     * <p><em>Stub — full implementation in A-10-5.</em>
     *
     * @param ctx network event context supplied by Forge
     */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        // TODO (A-10-5): enqueue work on client thread, update visual state cache.
    }

    // ---- Accessors -----------------------------------------------------------

    /** @return UUID of the loot container */
    public UUID getContainerUUID() { return containerUUID; }

    /** @return UUID of the target player */
    public UUID getPlayerUUID() { return playerUUID; }

    /** @return {@code true} if the container has unclaimed loot for this player */
    public boolean hasLoot() { return hasLoot; }
}
