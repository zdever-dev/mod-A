package com.example.structuregen.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Popisuje místo na {@link RoomTemplate} kde se může připojit jiná místnost
 * nebo chodba.
 *
 * <p>Assembly engine prochází frontu volných {@code ConnectionPoint} objektů
 * a pro každý hledá kompatibilní kandidátní {@link RoomTemplate} přes
 * porovnání {@link #compatibilityTag} hodnot (viz A-5-4).
 *
 * <h2>Příklad použití</h2>
 * <pre>{@code
 * ConnectionPoint north = ConnectionPoint.builder()
 *     .direction(Direction.NORTH)
 *     .typeTag("corridor")
 *     .compatibilityTag("corridor")
 *     .relativeOffset(new BlockPos(0, 0, 0))
 *     .build();
 * }</pre>
 *
 * <h2>Rovnost</h2>
 * <p>Dva {@code ConnectionPoint} objekty jsou si rovny pokud mají stejný
 * {@link #direction} a {@link #relativeOffset}. {@link #typeTag} a
 * {@link #compatibilityTag} nejsou součástí equality — záměrně, aby bylo
 * možné porovnávat connection pointy bez znalosti tagů.
 */
public final class ConnectionPoint {

    // ---- Fields --------------------------------------------------------------

    /**
     * Světová strana ze které se připojuje sousední místnost.
     * Nikdy {@code null}.
     */
    private final Direction direction;

    /**
     * Typ tohoto connection pointu — popisuje "druh otvoru" (např.
     * {@code "corridor"}, {@code "containment_door"}, {@code "maintenance_hatch"}).
     *
     * <p>Terminátory jsou registrovány per {@code typeTag} v
     * {@code StructureDefinition} — každý {@code typeTag} musí mít
     * přiřazený terminator (validace při registraci, viz A-3-4).
     */
    private final String typeTag;

    /**
     * Tag kompatibility — assembly engine spojuje connection pointy jejichž
     * {@code compatibilityTag} se shoduje s dostupnými connection pointy
     * kandidátního {@link RoomTemplate}.
     *
     * <p>Může se shodovat s {@link #typeTag} nebo být odlišný — záleží na
     * designu struktury. Např. {@code typeTag = "wide_door"} ale
     * {@code compatibilityTag = "door"} by se napojilo na jakýkoli template
     * s connection pointem tagu {@code "door"}.
     */
    private final String compatibilityTag;

    /**
     * Pozice tohoto connection pointu relativní vůči origin pozici
     * nadřazeného {@link RoomTemplate}.
     *
     * <p>Používána assemblerem při výpočtu absolutní pozice pro napojení
     * sousední místnosti. Nikdy {@code null}.
     */
    private final BlockPos relativeOffset;

    // ---- Constructor (private — použij Builder) ------------------------------

    private ConnectionPoint(Builder builder) {
        this.direction        = builder.direction;
        this.typeTag          = builder.typeTag;
        this.compatibilityTag = builder.compatibilityTag;
        this.relativeOffset   = builder.relativeOffset;
    }

    // ---- Static factory ------------------------------------------------------

    /**
     * Vytvoří nový {@link Builder} pro konstrukci {@code ConnectionPoint}.
     *
     * @return nový builder; nikdy {@code null}
     */
    public static Builder builder() {
        return new Builder();
    }

    // ---- Accessors -----------------------------------------------------------

    /**
     * @return světová strana připojení; nikdy {@code null}
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * @return typ tohoto connection pointu; nikdy {@code null}
     */
    public String getTypeTag() {
        return typeTag;
    }

    /**
     * @return kompatibilitní tag pro párovací logiku enginu; nikdy {@code null}
     */
    public String getCompatibilityTag() {
        return compatibilityTag;
    }

    /**
     * @return offset relativní vůči origin pozici nadřazeného template; nikdy {@code null}
     */
    public BlockPos getRelativeOffset() {
        return relativeOffset;
    }

    // ---- equals / hashCode ---------------------------------------------------

    /**
     * Rovnost je založena výhradně na {@link #direction} + {@link #relativeOffset}.
     * Tags nejsou součástí porovnání (viz class Javadoc).
     */
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof ConnectionPoint other)) return false;
        return direction == other.direction
            && relativeOffset.equals(other.relativeOffset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(direction, relativeOffset);
    }

    @Override
    public String toString() {
        return "ConnectionPoint{dir=" + direction
            + ", type=" + typeTag
            + ", compat=" + compatibilityTag
            + ", offset=" + relativeOffset + "}";
    }

    // =========================================================================
    // Builder
    // =========================================================================

    /**
     * Fluent builder pro {@link ConnectionPoint}.
     *
     * <p>Všechna pole jsou povinná — {@link #build()} vyhodí
     * {@link IllegalStateException} pokud některé chybí.
     */
    public static final class Builder {

        private Direction direction;
        private String typeTag;
        private String compatibilityTag;
        private BlockPos relativeOffset;

        private Builder() {}

        /**
         * Nastaví světovou stranu připojení.
         *
         * @param direction směr; nesmí být {@code null}
         * @return {@code this} pro řetězení
         */
        public Builder direction(Direction direction) {
            this.direction = Objects.requireNonNull(direction, "direction must not be null");
            return this;
        }

        /**
         * Nastaví typ connection pointu.
         *
         * @param typeTag neprázdný string; nesmí být {@code null}
         * @return {@code this} pro řetězení
         */
        public Builder typeTag(String typeTag) {
            this.typeTag = Objects.requireNonNull(typeTag, "typeTag must not be null");
            return this;
        }

        /**
         * Nastaví kompatibilitní tag.
         *
         * @param compatibilityTag neprázdný string; nesmí být {@code null}
         * @return {@code this} pro řetězení
         */
        public Builder compatibilityTag(String compatibilityTag) {
            this.compatibilityTag = Objects.requireNonNull(compatibilityTag, "compatibilityTag must not be null");
            return this;
        }

        /**
         * Nastaví relativní offset od origin pozice template.
         *
         * @param relativeOffset pozice; nesmí být {@code null}
         * @return {@code this} pro řetězení
         */
        public Builder relativeOffset(BlockPos relativeOffset) {
            this.relativeOffset = Objects.requireNonNull(relativeOffset, "relativeOffset must not be null");
            return this;
        }

        /**
         * Sestaví {@link ConnectionPoint}.
         *
         * @return nová immutable instance
         * @throws IllegalStateException pokud některé povinné pole nebylo nastaveno
         */
        public ConnectionPoint build() {
            if (direction == null)        throw new IllegalStateException("ConnectionPoint.Builder: direction is required");
            if (typeTag == null)          throw new IllegalStateException("ConnectionPoint.Builder: typeTag is required");
            if (compatibilityTag == null) throw new IllegalStateException("ConnectionPoint.Builder: compatibilityTag is required");
            if (relativeOffset == null)   throw new IllegalStateException("ConnectionPoint.Builder: relativeOffset is required");
            return new ConnectionPoint(this);
        }
    }
}