package Game.Engine.Components;

import Game.Engine.Component;
import Game.Engine.GameObjects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Componente de efectos de estado — gestiona efectos activos sobre cualquier entidad.
 *
 * ── REFACTOR: MOVIDO Y DESACOPLADO DE Enemy ──────────────────────────────
 *
 * PROBLEMA ORIGINAL:
 *   StatusEffectComponent vivía en Game.Enemys.Components y su interface
 *   StatusEffect.tick() recibía Enemy como parámetro hardcodeado:
 *
 *     boolean tick(Enemy enemy);
 *
 *   Esto lo hacía inutilizable para Player, NPCs u otras entidades. Si el
 *   Player fuera a recibir efectos de estado (congelación del entorno,
 *   veneno de trampa), habría que duplicar la clase o crear un StatusEffect
 *   paralelo. Además, ataba Engine.Components a Game.Enemys, creando una
 *   dependencia cíclica: Engine conoce Enemys.
 *
 * SOLUCIÓN:
 *   - Mover a Game.Engine.Components.Gameplay (capa compartida de gameplay).
 *   - Cambiar la firma de tick() de tick(Enemy) a tick(GameObjects), que es
 *     la base común de todas las entidades. Los efectos concretos hacen
 *     cast interno cuando necesiten el tipo específico.
 *   - Ahora cualquier entidad (Player, Enemy, NPC, objeto destruible) puede
 *     recibir efectos de estado simplemente añadiendo este componente.
 *
 * BENEFICIO:
 *   - StatusEffect es independiente del tipo de entidad.
 *   - Sin dependencias cruzadas entre módulos (Engine no conoce Enemys).
 *   - Extensible: cualquier código puede implementar StatusEffect sin
 *     modificar esta clase.
 *
 * ── USO ──────────────────────────────────────────────────────────────────
 *
 *   // En constructor de Enemy, Player, etc.:
 *   addComponent(new StatusEffectComponent());
 *
 *   // Aplicar efecto:
 *   StatusEffectComponent fx = entity.getComponent(StatusEffectComponent.class);
 *   if (fx != null) fx.add(new PoisonEffect(3, 20, 120));
 *
 * ── STACKING ─────────────────────────────────────────────────────────────
 *
 * Por defecto, efectos del mismo tipo se acumulan. Si un efecto no debe
 * stackearse, su tick() puede verificar internamente o llamar
 * removeAll(effectId) antes de añadirse.
 */
public class StatusEffectComponent extends Component {

    /** Interfaz que implementa cada efecto concreto. */
    public interface StatusEffect {
        /**
         * Procesa un tick del efecto sobre la entidad.
         *
         * CAMBIO VS. ORIGINAL: el parámetro es GameObjects (base común) en lugar
         * de Enemy. Los efectos concretos hacen instanceof/cast si necesitan
         * acceder a métodos específicos del tipo.
         *
         * @return true si el efecto sigue activo, false si expiró.
         */
        boolean tick(GameObjects entity);

        /** ID del tipo de efecto. Usado para consultas ("¿está envenenado?"). */
        String effectId();
    }

    private final List<StatusEffect> effects = new ArrayList<>();

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    @Override
    public void update() {
        if (effects.isEmpty() || gameObject == null) return;

        Iterator<StatusEffect> it = effects.iterator();
        while (it.hasNext()) {
            boolean active = it.next().tick(gameObject);
            if (!active) it.remove();
        }
    }

    // ── API ───────────────────────────────────────────────────────────────

    public void add(StatusEffect effect) {
        effects.add(effect);
    }

    public void removeAll(String effectId) {
        effects.removeIf(e -> e.effectId().equals(effectId));
    }

    public boolean hasEffect(String effectId) {
        return effects.stream().anyMatch(e -> e.effectId().equals(effectId));
    }

    public int activeCount() { return effects.size(); }

    public void clearAll() { effects.clear(); }
}
