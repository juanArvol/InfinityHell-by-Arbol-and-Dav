package Game.Items.Types.Ammulets;

import Game.Items.ItemDefinition;
import Game.Items.ItemRarity;

/**
 * Definición de un amuleto — mejora pasiva acumulable al estilo Hollow Knight.
 *
 * ── JERARQUÍA ────────────────────────────────────────────────────────────
 * Extiende ItemDefinition para heredar la estructura común de metadata
 * (id, displayName, description, defaultRarity).
 *
 * ── DISEÑO ───────────────────────────────────────────────────────────────
 * Los amuletos reemplazan al concepto de "modificador de arma" para las
 * propiedades de PROYECTIL que antes vivían en WeaponModifier:
 *
 *   Antes (WeaponModifier, único):  PiercingModifier, ExplosiveModifier…
 *   Ahora (Amuleto, acumulable):    "Punta Ósea", "Pluma del Eco"…
 *
 * DIFERENCIA CLAVE respecto a armas y tipos de bala:
 *   • Armas y BulletTypes → únicos por run (no repiten).
 *   • Amuletos → pueden aparecer MÚLTIPLES VECES de forma aleatoria.
 *     Cada copia suma su efecto (igual que en Hollow Knight o Binding of Isaac).
 *
 * ── EFECTOS ───────────────────────────────────────────────────────────────
 * Los amuletos modifican las estadísticas de las balas via AmuletEffect.
 * Son ADITIVOS por defecto; múltiples copias suman.
 *
 * Ejemplo:
 *   "Punta Ósea" x1: +1 perforación de enemigos
 *   "Punta Ósea" x3: +3 perforaciones de enemigos
 *
 * ── RAREZA ────────────────────────────────────────────────────────────────
 * La rareza controla la frecuencia en el pool de oferta pero NO limita
 * cuántas veces puede aparecer. Un amuleto EPIC puede caer 5 veces en una run.
 *
 * ── AÑADIR UN AMULETO ─────────────────────────────────────────────────────
 * 1. Crear un AmuletEffect que implemente el bonus.
 * 2. Registrar en AmuletRegistry con una AmuletDefinition.
 * 3. Nada más — el sistema lo recoge automáticamente.
 *
 * @see Game.Items.ItemDefinition    clase base con metadata común
 * @see Game.Items.Types.Ammulets.AmuletEffect interface del efecto
 */
public final class AmuletDefinition extends ItemDefinition {

    /** Efecto que aplica este amuleto sobre las stats de bala. */
    public final AmuletEffect effect;

    public AmuletDefinition(String id,
                            String displayName,
                            String description,
                            ItemRarity defaultRarity,
                            AmuletEffect effect) {
        super(id, displayName, description, defaultRarity);
        if (effect == null)
            throw new IllegalArgumentException("effect no puede ser null");
        this.effect = effect;
    }
}
