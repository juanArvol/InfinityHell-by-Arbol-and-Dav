package Game.Enemys.Components;

import Game.Engine.Component;
import Game.Enemys.Enemy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Componente de efectos de estado — gestiona efectos activos sobre el enemigo.
 *
 * ── POR QUÉ EXISTE ───────────────────────────────────────────────────────
 * PoisonModifier (sistema de armas) necesita aplicar un efecto DoT al Enemy.
 * Sin este componente, Enemy no tiene forma de acumular efectos en el tiempo.
 *
 * ── DISEÑO ───────────────────────────────────────────────────────────────
 * Cada efecto implementa StatusEffect (ver inner interface).
 * StatusEffectComponent.update() los procesa en orden y elimina los expirados.
 *
 * NO hardcodea tipos de efecto. Cualquier código puede implementar StatusEffect
 * y llamar component.add(effect) — extensible indefinidamente.
 *
 * ── USO ──────────────────────────────────────────────────────────────────
 *   // En Enemy (se añade en el constructor):
 *   addComponent(new StatusEffectComponent());
 *
 *   // Desde PoisonModifier:
 *   StatusEffectComponent fx = enemy.getComponent(StatusEffectComponent.class);
 *   if (fx != null) fx.add(new PoisonModifier.PoisonEffect(3, 20, 120));
 *   // O usando el shortcut de Enemy:
 *   enemy.addEffect(new PoisonModifier.PoisonEffect(3, 20, 120));
 *
 * ── STACKING ─────────────────────────────────────────────────────────────
 * Por defecto, efectos del mismo tipo se ACUMULAN (múltiples instancias activas).
 * Si un efecto no debe stackearse (congelación), su tick() puede verificar
 * internamente o se puede implementar un sistema de tags (futuro).
 */
public class StatusEffectComponent extends Component {

    /** Interfaz que implementa cada efecto concreto. */
    public interface StatusEffect {
        /**
         * Procesa un tick del efecto sobre el enemigo.
         * @return true si el efecto sigue activo, false si expiró.
         */
        boolean tick(Enemy enemy);

        /** ID del tipo de efecto. Usado para consultas ("¿está envenenado?"). */
        String effectId();
    }

    private final List<StatusEffect> effects = new ArrayList<>();

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    @Override
    public void update() {
        if (effects.isEmpty() || gameObject == null) return;
        if (!(gameObject instanceof Enemy enemy)) return;

        Iterator<StatusEffect> it = effects.iterator();
        while (it.hasNext()) {
            boolean active = it.next().tick(enemy);
            if (!active) it.remove();
        }
    }

    // ── API ───────────────────────────────────────────────────────────────

    /** Añade un efecto activo. */
    public void add(StatusEffect effect) {
        effects.add(effect);
    }

    /** Elimina todos los efectos del tipo dado. */
    public void removeAll(String effectId) {
        effects.removeIf(e -> e.effectId().equals(effectId));
    }

    /** True si hay al menos un efecto activo de ese tipo. */
    public boolean hasEffect(String effectId) {
        return effects.stream().anyMatch(e -> e.effectId().equals(effectId));
    }

    public int activeCount() { return effects.size(); }

    public void clearAll() { effects.clear(); }
}
