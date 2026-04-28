package com.example.structuregen.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Kompletní definice jedné procedurálně generované struktury.
 *
 * <p>Vývojář třetí strany vytvoří instanci přes {@link Builder} a zaregistruje
 * ji přes {@link StructureGeneratorAPI#register(StructureDefinition)} v
 * {@code FMLCommonSetupEvent}. Po registraci je instance immutable.
 *
 * <h2>Povinná pole</h2>
 * <ul>
 *   <li>{@link #id} — unikátní {@link ResourceLocation}</li>
 *   <li>{@link #templates} — alespoň jeden {@link RoomTemplate}</li>
 *   <li>{@link #rules} — {@link GenerationRules}</li>
 *   <li>{@link #uniqueRule} — {@link UniqueRule}</li>
 *   <li>{@link #sealBlock} — block použitý pro seal operaci</li>
 * </ul>
 *
 * <h2>Volitelná pole</h2>
 * <ul>
 *   <li>{@link #populator} — {@link SpawnPopulator} pro post-generation spawn</li>
 *   <li>{@link #processors} — custom {@link PostProcessor} seznam</li>
 *   <li>{@link #terminators} — terminator templates per connection type tag</li>
 *   <li>{@link #playerInstancedLoot} — per-player loot generování</li>
 * </ul>
 */
public final class StructureDefinition {

    // ---- Fields --------------------------------------------------------------

    /**
     * Unikátní identifikátor struktury.
     * Formát: {@code modid:structure_name}, např. {@code scp_entities:foundation_complex}.
     * Duplikáty způsobí {@link IllegalStateException} při registraci (viz A-3-4).
     */
    private final ResourceLocation id;

    /**
     * Seznam všech {@link RoomTemplate} instancí dostupných pro tuto strukturu.
     * Assembly engine vybírá z tohoto seznamu na základě ConnectionPoint
     * kompatibility a seed-based váhování.
     *
     * <p>První template v seznamu je považován za ROOT template — umísťován
     * jako první na origin pozici.
     */
    private final List<RoomTemplate> templates;

    /** Pravidla pro worldgen placement a assembly chování. */
    private final GenerationRules rules;

    /** Pravidla unikátnosti — kolik instancí smí existovat per dimenze. */
    private final UniqueRule uniqueRule;

    /**
     * Volitelný callback volaný po dokončení assembly pro spawn entit
     * nebo jiné post-placement akce.
     * {@code null} = žádný populator.
     */
    @Nullable
    private final SpawnPopulator populator;

    /**
     * Seznam custom post-procesorů spouštěných po built-in procesorech.
     * Prázdný seznam = pouze built-in procesory.
     */
    private final List<PostProcessor> processors;

    /**
     * Mapa terminator templates per {@link ConnectionPoint#getTypeTag()}.
     * Každý type tag musí mít přiřazený terminator — validováno při
     * registraci (viz A-3-4).
     */
    private final Map<String, RoomTemplate> terminators;

    /**
     * Block použitý pro "seal" operaci jako ultimate fallback pokud ani
     * terminator neprojde collision check (viz A-5-9).
     *
     * <p>Výchozí doporučená hodnota: {@link Blocks#STONE}.
     */
    private final Block sealBlock;

    /**
     * Pokud {@code true}, loot v kontejnerech je generován per-hráč
     * (Player-Instanced Loot systém, viz A-10).
     * Pokud {@code false}, loot je generován jednou sdíleně.
     *
     * <p>Výchozí hodnota: {@code false}.
     */
    private final boolean playerInstancedLoot;

    // ---- Constructor (private) -----------------------------------------------

    private StructureDefinition(Builder b) {
        this.id                  = b.id;
        this.templates           = Collections.unmodifiableList(new ArrayList<>(b.templates));
        this.rules               = b.rules;
        this.uniqueRule          = b.uniqueRule;
        this.populator           = b.populator;
        this.processors          = Collections.unmodifiableList(new ArrayList<>(b.processors));
        this.terminators         = Collections.unmodifiableMap(new HashMap<>(b.terminators));
        this.sealBlock           = b.sealBlock;
        this.playerInstancedLoot = b.playerInstancedLoot;
    }

    // ---- Static factory ------------------------------------------------------

    /** @return nový builder; nikdy {@code null} */
    public static Builder builder() {
        return new Builder();
    }

    // ---- Accessors -----------------------------------------------------------

    public ResourceLocation getId()                            { return id; }
    public List<RoomTemplate> getTemplates()                   { return templates; }
    public GenerationRules getRules()                          { return rules; }
    public UniqueRule getUniqueRule()                          { return uniqueRule; }
    @Nullable public SpawnPopulator getPopulator()             { return populator; }
    public List<PostProcessor> getProcessors()                 { return processors; }
    public Map<String, RoomTemplate> getTerminators()          { return terminators; }
    public Block getSealBlock()                                { return sealBlock; }
    public boolean isPlayerInstancedLoot()                     { return playerInstancedLoot; }

    @Override
    public String toString() {
        return "StructureDefinition{id=" + id + ", templates=" + templates.size() + "}";
    }

    // =========================================================================
    // Builder
    // =========================================================================

    public static final class Builder {

        private ResourceLocation id;
        private final List<RoomTemplate> templates   = new ArrayList<>();
        private GenerationRules rules;
        private UniqueRule uniqueRule                = UniqueRule.unlimited();
        @Nullable
        private SpawnPopulator populator             = null;
        private final List<PostProcessor> processors = new ArrayList<>();
        private final Map<String, RoomTemplate> terminators = new HashMap<>();
        private Block sealBlock                      = Blocks.STONE;
        private boolean playerInstancedLoot          = false;

        private Builder() {}

        /**
         * @param id unikátní ID struktury; nesmí být {@code null}
         * @return {@code this}
         */
        public Builder id(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id must not be null");
            return this;
        }

        /**
         * Přidá jeden nebo více {@link RoomTemplate} instancí.
         * První přidaný template = ROOT.
         *
         * @param templates nesmí být {@code null}; žádný prvek nesmí být {@code null}
         * @return {@code this}
         */
        public Builder addTemplates(RoomTemplate... templates) {
            for (RoomTemplate t : templates) {
                this.templates.add(Objects.requireNonNull(t, "template must not be null"));
            }
            return this;
        }

        /**
         * @param rules nesmí být {@code null}
         * @return {@code this}
         */
        public Builder rules(GenerationRules rules) {
            this.rules = Objects.requireNonNull(rules, "rules must not be null");
            return this;
        }

        /**
         * @param uniqueRule nesmí být {@code null}
         * @return {@code this}
         */
        public Builder uniqueRule(UniqueRule uniqueRule) {
            this.uniqueRule = Objects.requireNonNull(uniqueRule, "uniqueRule must not be null");
            return this;
        }

        /**
         * @param populator může být {@code null} (= žádný populator)
         * @return {@code this}
         */
        public Builder populator(@Nullable SpawnPopulator populator) {
            this.populator = populator;
            return this;
        }

        /**
         * Přidá custom {@link PostProcessor}.
         * Custom procesory by měly mít prioritu ≥ 100.
         *
         * @param processor nesmí být {@code null}
         * @return {@code this}
         */
        public Builder addPostProcessor(PostProcessor processor) {
            this.processors.add(Objects.requireNonNull(processor));
            return this;
        }

        /**
         * Registruje terminator template pro daný connection type tag.
         * Každý type tag použitý v jakémkoli template musí mít terminator —
         * validace při registraci (viz A-3-4).
         *
         * @param typeTag    connection point type tag; nesmí být {@code null}
         * @param terminator template s min BoundingBox 1×1×1; nesmí být {@code null}
         * @return {@code this}
         */
        public Builder addTerminator(String typeTag, RoomTemplate terminator) {
            this.terminators.put(
                Objects.requireNonNull(typeTag,    "typeTag must not be null"),
                Objects.requireNonNull(terminator, "terminator must not be null")
            );
            return this;
        }

        /**
         * @param block block pro seal operaci; nesmí být {@code null};
         *              výchozí {@link Blocks#STONE}
         * @return {@code this}
         */
        public Builder sealBlock(Block block) {
            this.sealBlock = Objects.requireNonNull(block, "sealBlock must not be null");
            return this;
        }

        /**
         * @param playerInstanced {@code true} = per-player loot; výchozí {@code false}
         * @return {@code this}
         */
        public Builder playerInstancedLoot(boolean playerInstanced) {
            this.playerInstancedLoot = playerInstanced;
            return this;
        }

        /**
         * Sestaví a zvaliduje {@link StructureDefinition}.
         *
         * @return nová immutable instance
         * @throws IllegalStateException pokud chybí povinné pole nebo je definice nevalidní
         */
        public StructureDefinition build() {
            if (id == null)
                throw new IllegalStateException("StructureDefinition.Builder: id is required");
            if (templates.isEmpty())
                throw new IllegalStateException("StructureDefinition.Builder[" + id + "]: at least one template is required");
            if (rules == null)
                throw new IllegalStateException("StructureDefinition.Builder[" + id + "]: rules is required");
            if (sealBlock == null)
                throw new IllegalStateException("StructureDefinition.Builder[" + id + "]: sealBlock must not be null");

            return new StructureDefinition(this);
        }
    }
}