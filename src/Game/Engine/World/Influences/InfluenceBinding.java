package Game.Engine.World.Influences;

import Game.Engine.GameObjects;

/**
 * Asociación entre una Influence y su objeto destino.
 *
 * ── HRFC-015 — World Simulation Core ──────────────────────────────────────
 *
 * ── POR QUÉ EXISTE ────────────────────────────────────────────────────────
 * InfluenceSystem necesita saber sobre quién actúa cada Influence para poder:
 *   1. Llamar influence.apply(target) cada frame.
 *   2. Llamar influence.onExpire(target) cuando la influencia termina.
 *   3. Eliminar el binding cuando tick() retorna false.
 *
 * InfluenceBinding agrupa estos dos datos en un objeto simple e inmutable.
 * InfluenceSystem solo opera sobre InfluenceBinding[] — no conoce tipos
 * concretos de Influence ni de GameObjects.
 *
 * ── CONSTRUCCIÓN ──────────────────────────────────────────────────────────
 *
 *   // Influencia sobre un target específico:
 *   InfluenceBinding binding = InfluenceBinding.of(myInfluence, target);
 *   influenceSystem.add(binding);
 *
 *   // Atajo de conveniencia (sin construir InfluenceBinding explícitamente):
 *   influenceSystem.add(myInfluence, target);
 */
public final class InfluenceBinding {

    private final Influence   influence;
    private final GameObjects target;

    private InfluenceBinding(Influence influence, GameObjects target) {
        this.influence = influence;
        this.target    = target;
    }

    /**
     * Crea un binding entre una influencia y su objeto destino.
     *
     * @param influence influencia a aplicar. No puede ser null.
     * @param target    objeto destino. No puede ser null.
     * @throws IllegalArgumentException si alguno es null.
     */
    public static InfluenceBinding of(Influence influence, GameObjects target) {
        if (influence == null) throw new IllegalArgumentException("influence no puede ser null");
        if (target    == null) throw new IllegalArgumentException("target no puede ser null");
        return new InfluenceBinding(influence, target);
    }

    /** La influencia asociada. Nunca null. */
    public Influence   getInfluence() { return influence; }

    /** El objeto destino de la influencia. Nunca null. */
    public GameObjects getTarget()    { return target; }
}
