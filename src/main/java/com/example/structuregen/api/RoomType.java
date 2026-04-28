package com.example.structuregen.api;

/**
 * Kategorický typ místnosti používaný assembly enginem pro validaci
 * {@code requiredRoomTypes} a dynamický weight boost.
 *
 * <p>Hodnoty jsou seřazeny od vstupu do komplexu směrem dovnitř.
 * {@link #CUSTOM} slouží jako escape hatch pro třetí strany které potřebují
 * typ který zde není definován — sémantiku si definují samy přes metadata.
 *
 * <p>Každá hodnota nese {@link #displayName} pro GUI/debug výpisy a příznak
 * {@link #isTerminable} který říká zda smí být tento typ použit jako
 * terminální (slepá ulička). Assembly engine odmítne zapečetit
 * {@code isTerminable == false} místnost pokud ještě zbývají nevyřešené
 * ConnectionPointy — místo toho zkusí backtracking.
 */
public enum RoomType {

    /**
     * Vstupní hala komplexu. Každá struktura by měla mít právě jednu.
     * Typicky první místnost umístěná assemblerem (root template).
     */
    ENTRANCE("Entrance", false),

    /**
     * Bezpečnostní kontrolní bod — přechod mezi zónami s různou clearance úrovní.
     * Obvykle obsahuje {@code KeycardReaderBlock} a {@code BlastDoorBlock}.
     */
    CHECKPOINT("Checkpoint", false),

    /**
     * Spojovací chodba mezi ostatními místnostmi.
     * Nejčastěji umísťovaný typ — slouží jako "padding" mezi důležitými místnostmi.
     */
    CORRIDOR("Corridor", false),

    /**
     * Containment komora pro SCP objekt. Strukturálně nejpevnější místnost,
     * typicky obklopena checkpointy a chodbami.
     */
    CONTAINMENT("Containment", false),

    /**
     * Kontrolní centrum. Obsahuje panely, interkom a alarm controller.
     * Obvykle jedno na strukturu.
     */
    CONTROL("Control Room", false),

    /**
     * Technická místnost — kabeláž, generátory, potrubí.
     * Může se napojovat na jakoukoli jinou místnost.
     */
    MAINTENANCE("Maintenance", true),

    /**
     * Víceúčelová místnost bez specifické sémantiky — sklady, kanceláře, apod.
     */
    UTILITY("Utility", true),

    /**
     * Slepá ulička. Jedná se o terminální místnost — assembly engine ji smí
     * umístit na libovolný volný ConnectionPoint jako uzávěr.
     * Vždy {@code isTerminable == true}.
     */
    DEAD_END("Dead End", true),

    /**
     * Uživatelsky definovaný typ. Sémantiku určuje vývojář třetí strany
     * prostřednictvím {@link RoomTemplate#getMetadata()}.
     * Assembly engine s tímto typem neprovádí žádnou speciální logiku.
     */
    CUSTOM("Custom", true);

    // ---- Fields --------------------------------------------------------------

    /**
     * Čitelný název zobrazovaný v GUI, debug příkazech a logu.
     * Nikdy {@code null}.
     */
    private final String displayName;

    /**
     * {@code true} pokud smí assembly engine tento typ použít jako terminální
     * uzávěr ConnectionPointu (tj. umístit sem "dead end" nebo seal operaci
     * bez dalšího backtrackingu).
     *
     * <p>{@code false} znamená že engine musí dál hledat kompatibilní
     * pokračování — slepá ulička zde není akceptovatelná.
     */
    private final boolean isTerminable;

    // ---- Constructor ---------------------------------------------------------

    RoomType(String displayName, boolean isTerminable) {
        this.displayName   = displayName;
        this.isTerminable  = isTerminable;
    }

    // ---- Accessors -----------------------------------------------------------

    /**
     * Vrátí čitelný název tohoto typu místnosti.
     *
     * @return display name; nikdy {@code null}
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Vrátí zda smí assembly engine tento typ použít jako terminální uzávěr.
     *
     * @return {@code true} pokud je typ terminovatelný
     */
    public boolean isTerminable() {
        return isTerminable;
    }
}