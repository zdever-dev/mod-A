package com.example.structuregen.network;

import com.example.structuregen.StructureGenMod;
import com.example.structuregen.network.packet.BoundingBoxDebugPacket;
import com.example.structuregen.network.packet.ContainerStatePacket;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Central registration point for the Structure Generator network channel.
 *
 * <h2>Channel contract</h2>
 * <ul>
 *   <li>The channel uses a simple string protocol version. Client and server
 *       must carry the same version string or the connection is rejected.</li>
 *   <li>Packet IDs are assigned sequentially starting at {@code 0}. Never
 *       re-use or re-order IDs — doing so is a protocol break that requires
 *       a {@code PROTOCOL_VERSION} bump.</li>
 * </ul>
 *
 * <h2>Registered packets</h2>
 * <ol>
 *   <li>ID {@code 0} — {@link ContainerStatePacket}: server → client,
 *       synchronises per-player loot container visual state.</li>
 *   <li>ID {@code 1} — {@link BoundingBoxDebugPacket}: server → client,
 *       transmits bounding-box data for the dev-mode wireframe renderer.</li>
 * </ol>
 *
 * <h2>Initialisation order</h2>
 * <p>{@link #init()} is called from the mod constructor — before
 * {@code FMLCommonSetupEvent} — because Forge requires channel registration
 * to occur during the mod constructor phase.
 */
public final class ModNetworking {

    // ---- Protocol version ----------------------------------------------------

    /**
     * Protocol version string for this channel.
     *
     * <p>Bump this string whenever a packet's binary format changes in a
     * backwards-incompatible way. The version is checked on both sides of a
     * client-server connection; a mismatch will prevent the connection from
     * being established.
     *
     * <p>Format convention: {@code "<mod_major>.<packet_schema_revision>"}
     * e.g. {@code "1.0"}.
     */
    public static final String PROTOCOL_VERSION = "1.0";

    // ---- Channel -------------------------------------------------------------

    /**
     * The mod's {@link SimpleChannel}.
     *
     * <p>All packets sent by Structure Generator travel through this channel.
     * The reference is set once during {@link #init()} and is effectively
     * final thereafter — safe to read from any thread.
     */
    public static SimpleChannel CHANNEL;

    // ---- Packet ID counter ---------------------------------------------------

    /** Next available packet discriminator ID. Increment via {@link #nextId()}. */
    private static int packetId = 0;

    // ---- Init ----------------------------------------------------------------

    /**
     * Registers the network channel and all packet message types.
     *
     * <p>Called once from {@link StructureGenMod#StructureGenMod()} during mod
     * construction. Must not be called a second time.
     */
    public static void init() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
            // Channel resource location — unique identifier
            new net.minecraft.resources.ResourceLocation(StructureGenMod.MOD_ID, "main"),

            // Supplier of the current running version string
            () -> PROTOCOL_VERSION,

            // Client-side version acceptor:
            //   accept the expected version OR the "absent" marker so that
            //   vanilla clients that do not have this mod installed are not
            //   rejected outright (server-only deployments).
            clientVersion -> clientVersion.equals(PROTOCOL_VERSION)
                || clientVersion.equals(NetworkRegistry.ABSENT),

            // Server-side version acceptor: same logic
            serverVersion -> serverVersion.equals(PROTOCOL_VERSION)
                || serverVersion.equals(NetworkRegistry.ABSENT)
        );

        // ---- Register packets (order = packet ID assignment) -----------------
        // ID 0 — ContainerStatePacket (server → client)
        CHANNEL.registerMessage(
            nextId(),
            ContainerStatePacket.class,
            ContainerStatePacket::encode,
            ContainerStatePacket::decode,
            ContainerStatePacket::handle
        );

        // ID 1 — BoundingBoxDebugPacket (server → client)
        CHANNEL.registerMessage(
            nextId(),
            BoundingBoxDebugPacket.class,
            BoundingBoxDebugPacket::encode,
            BoundingBoxDebugPacket::decode,
            BoundingBoxDebugPacket::handle
        );

        StructureGenMod.LOGGER.debug(
            "ModNetworking: channel registered with protocol version '{}'.",
            PROTOCOL_VERSION
        );
    }

    // ---- Helpers -------------------------------------------------------------

    /**
     * Returns the next sequential packet discriminator ID and advances the
     * internal counter.
     *
     * @return unique packet ID
     */
    private static int nextId() {
        return packetId++;
    }

    /** Utility class — no instantiation. */
    private ModNetworking() {}
}
