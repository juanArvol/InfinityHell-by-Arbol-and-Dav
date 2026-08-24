package Game.Items.Types.Ammulets;

import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;
import java.util.List;

/**
 * Aplicador de efectos de amuletos — servicio de aplicación.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * SEPARACIÓN DE RESPONSABILIDADES:
 *   AmuletType              → Contenedor de instancias estáticas
 *   AmuletEffect            → Definición de un efecto individual
 *   AmuletEffectApplicator  → Lógica de aplicación de efectos (ESTA CLASE)
 *
 * DECISIÓN:
 *   La lógica de "aplicar todos los amuletos" no pertenece en AmuletType
 *   (que solo debe contener instancias), ni en ObjectTypeFactory (que es
 *   genérico para todos los item types).
 *
 *   Por eso existe esta clase — centraliza la lógica de aplicación de
 *   efectos de amuletos sobre stats y behavior.
 *
 * USO:
 *   BulletBehavior result = AmuletEffectApplicator.applyAll(
 *       playerAmulets, weaponStats, baseBehavior
 *   );
 */
public final class AmuletEffectApplicator {

    // Constructor privado — clase de utilidad estática
    private AmuletEffectApplicator() {}

    /**
     * Aplica todos los amuletos poseídos sobre las estadísticas y comportamiento.
     *
     * ORDEN DE APLICACIÓN:
     *   Los efectos se aplican en orden de la lista. Esto permite que
     *   amuletos posteriores modifiquen los resultados de amuletos anteriores.
     *
     * MUTABILIDAD:
     *   - stats es mutable y se modifica in-place
     *   - behavior es inmutable, cada wrapper retorna una nueva instancia
     *
     * @param ownedAmulets tipos de amuletos que posee el jugador (puede repetirse)
     * @param stats        estadísticas mutables a modificar
     * @param behavior     comportamiento base a envolver
     * @return behavior con todos los efectos de amuleto aplicados
     */
    public static BulletBehavior applyAll(
            List<AmuletType> ownedAmulets,
            WeaponStats stats,
            BulletBehavior behavior) {

        for (AmuletType type : ownedAmulets) {
            AmuletEffect effect = type.createEffect();
            effect.applyToStats(stats);
            behavior = effect.wrapBehavior(behavior);
        }
        return behavior;
    }
}
