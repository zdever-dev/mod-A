package com.example.structuregen.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server → client packet that transmits a set of axis-aligned bounding boxes
 * (one per active structure) to the client for debug wireframe rendering.
 *
 * <h2>Payload</h2>
 * <ul>
 *   <li>{@link #boxes}  — list of {@link AABB} regions to highlight.</li>
 *   <li>{@link #colors} — parallel list of packed ARGB integers; index {@code i}
 *       is the colour for {@code boxes.get(i)}.</li>
 * </ul>
 *
 * <h2>Client handler</h2>
 * <p>The client handler (to be implemented in A-13-1) stores received boxes in
 * {@code BoundingBoxRenderer.ACTIVE_BOXES} with a 60-second expiry timestamp.
 * Boxes older than 60 s are automatically purged on the next render frame.
 *
 * <h2>Invariant</h2>
 * <p>{@code boxes.size() == colors.size()} must hold at all times. This is
 * enforced in the constructor and in {@link #decode}.
 *
 * <p><strong>Stub notice:</strong> {@code encode}, {@code decode} and
 * {@code handle} bodies are placeholders. Full implementation will be added in
 * step A-13-1.
 */
public final class BoundingBoxDebugPacket {

    /** Bounding boxes to render as wireframes on the client. */
    private final List<AABB> boxes;

    /**
     * Packed ARGB colour per box ({@code 0xAARRGGBB}).
     * Parallel to {@link #boxes}: {@code colors.get(i)} is the colour for
     * {@code boxes.get(i)}.
     */
    private final List<Integer> colors;

    // ---- Constructors --------------------------------------------------------

    /**
     * Creates a new packet.
     *
     * @param boxes  non-null list of bounding boxes; must not be {@code null}
     * @param colors parallel list of packed ARGB colours; must be the same
     *               size as {@code boxes}
     * @throws IllegalArgumentException if {@code boxes.size() != colors.size()}
     */
    public BoundingBoxDebugPacket(List<AABB> boxes, List<Integer> colors) {
        if (boxes.size() != colors.size()) {
            throw new IllegalArgumentException(
                "BoundingBoxDebugPacket: boxes and colors lists must be the same size. "
                + "boxes=" + boxes.size() + " colors=" + colors.size()
            );
        }
        // Defensive copies — packet is immutable after construction
        this.boxes  = Collections.unmodifiableList(new ArrayList<>(boxes));
        this.colors = Collections.unmodifiableList(new ArrayList<>(colors));
    }

    // ---- Encoding / decoding (stub) -----------------------------------------

    /**
     * Serialises this packet into the given buffer.
     *
     * <p><em>Stub — full implementation in A-13-1.</em>
     *
     * @param buf target buffer; not {@code null}
     */
    public void encode(FriendlyByteBuf buf) {
        // TODO (A-13-1):
        // buf.writeVarInt(boxes.size());
        // for (int i = 0; i < boxes.size(); i++) {
        //     AABB b = boxes.get(i);
        //     buf.writeDouble(b.minX); buf.writeDouble(b.minY); buf.writeDouble(b.minZ);
        //     buf.writeDouble(b.maxX); buf.writeDouble(b.maxY); buf.writeDouble(b.maxZ);
        //     buf.writeInt(colors.get(i));
        // }
    }

    /**
     * Deserialises a {@link BoundingBoxDebugPacket} from the given buffer.
     *
     * <p><em>Stub — full implementation in A-13-1.</em>
     *
     * @param buf source buffer; not {@code null}
     * @return an empty placeholder packet instance
     */
    public static BoundingBoxDebugPacket decode(FriendlyByteBuf buf) {
        // TODO (A-13-1): read count, then read boxes + colors in parallel.
        return new BoundingBoxDebugPacket(Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Handles this packet on the client side.
     *
     * <p><em>Stub — full implementation in A-13-1.</em>
     *
     * @param ctx network event context supplied by Forge
     */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        // TODO (A-13-1): enqueue work on client thread, populate BoundingBoxRenderer.ACTIVE_BOXES.
    }

    // ---- Accessors -----------------------------------------------------------

    /** @return unmodifiable list of bounding boxes included in this packet */
    public List<AABB> getBoxes() { return boxes; }

    /** @return unmodifiable list of packed ARGB colours, parallel to {@link #getBoxes()} */
    public List<Integer> getColors() { return colors; }
}
