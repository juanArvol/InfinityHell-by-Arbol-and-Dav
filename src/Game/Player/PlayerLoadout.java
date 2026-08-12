package Game.Player;

import Game.Items.Types.Bullets.Definition.BulletType;
import Game.Items.Types.Weapons.WeaponType.WeaponType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuración declarativa del equipamiento inicial del jugador.
 *
 * ── HRFC — Player Reengineering v2 ────────────────────────────────────────
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * PlayerLoadout responde exclusivamente:
 *
 *   > "¿Con qué comienza el Player?"
 *
 * NO responde:
 *
 *   > "¿Qué posee actualmente el Player?" ← PlayerRuntime/PlayerInventory
 *
 * ── SEPARACIÓN LOADOUT vs RUNTIME ────────────────────────────────────────
 *
 *   PlayerLoadout   → configuración inicial (inmutable)
 *   PlayerRuntime   → estado actual de la run (mutable)
 *   PlayerInventory → almacenamiento de posesiones
 *
 * ── FLUJO ARQUITECTÓNICO ─────────────────────────────────────────────────
 *
 *   PlayerLoadout
 *        │
 *        │ describe
 *        ▼
 *   PlayerAssembler
 *        │
 *        │ materializa
 *        ▼
 *   PlayerRuntime / PlayerInventory
 *        │
 *        ▼
 *   Player
 *
 * Una vez completado el ensamblado, PlayerLoadout deja de tener relevancia.
 *
 * ── ARMAS Y BALAS INDEPENDIENTES ─────────────────────────────────────────
 *
 * El loadout puede declarar múltiples armas y múltiples tipos de bala:
 *
 *   PlayerLoadout.builder()
 *       .weapon(WeaponType.PISTOLA)
 *       .weapon(WeaponType.ESCOPETA)
 *       .bullet(BulletType.NORMALBULLET)
 *       .bullet(BulletType.BULLETJUMP)
 *       .build()
 *
 * El Player comenzará con todas las armas y todas las balas disponibles
 * para selección independiente.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 *
 * Diferentes perfiles de inicio pueden definirse sin tocar PlayerCombat:
 *
 *   PlayerLoadout.defaultLoadout()      // principiante
 *   PlayerLoadout.shotgunStart()        // challenge
 *   PlayerLoadout.fromSaveFile(data)    // continuación
 *
 * ── INVARIANTES ───────────────────────────────────────────────────────────
 *
 *   • Al menos un arma es requerida (validado en build())
 *   • Al menos una bala es requerida (validado en build())
 *   • La primera arma en la lista es la activa al inicio
 *   • La primera bala en la lista es la activa al inicio
 */
public final class PlayerLoadout {

    /** Tipos de arma que el jugador comienza equipados, en orden. */
    private final List<WeaponType> weapons;

    /** Tipos de bala que el jugador comienza equipados, en orden. */
    private final List<BulletType> bullets;

    private PlayerLoadout(List<WeaponType> weapons, List<BulletType> bullets) {
        this.weapons = Collections.unmodifiableList(new ArrayList<>(weapons));
        this.bullets = Collections.unmodifiableList(new ArrayList<>(bullets));
    }

    // ── Factory — loadouts predefinidos ───────────────────────────────────

    /**
     * Loadout por defecto: pistola + bala normal.
     * Es el equipamiento estándar al inicio de una run nueva.
     *
     * @return loadout por defecto.
     */
    public static PlayerLoadout defaultLoadout() {
        return builder()
            .weapon(WeaponType.PISTOLA)
            .bullet(BulletType.NORMALBULLET)
            .build();
    }

    /**
     * Loadout avanzado: pistola + escopeta + múltiples balas.
     * Ejemplo de configuración con más opciones.
     *
     * @return loadout avanzado.
     */
    public static PlayerLoadout advancedLoadout() {
        return builder()
            .weapon(WeaponType.PISTOLA)
            .weapon(WeaponType.ESCOPETA)
            .bullet(BulletType.NORMALBULLET)
            .bullet(BulletType.SPRINGBULLET)
            .build();
    }

    // ── Acceso ────────────────────────────────────────────────────────────

    /**
     * Lista inmutable de tipos de arma del loadout, en orden.
     * El primer elemento es el arma activa al inicio.
     *
     * @return lista no vacía de WeaponType. Nunca null.
     */
    public List<WeaponType> getWeapons() { return weapons; }

    /**
     * Lista inmutable de tipos de bala del loadout, en orden.
     * El primer elemento es la bala activa al inicio.
     *
     * @return lista no vacía de BulletType. Nunca null.
     */
    public List<BulletType> getBullets() { return bullets; }

    /**
     * Tipo de bala base (primera en la lista) — método de compatibilidad.
     * 
     * @deprecated Usar getBullets().get(0) o getBullets() para acceso completo
     */
    @Deprecated
    public BulletType getBulletType() { 
        return bullets.isEmpty() ? BulletType.NORMALBULLET : bullets.get(0); 
    }

    // ── Builder ───────────────────────────────────────────────────────────

    /** Punto de entrada del builder. */
    public static Builder builder() { return new Builder(); }

    /** Builder de PlayerLoadout. */
    public static final class Builder {

        private final List<WeaponType> weapons = new ArrayList<>();
        private final List<BulletType> bullets = new ArrayList<>();

        private Builder() {}

        /**
         * Añade un tipo de arma al loadout.
         * El primer arma añadida será la activa al inicio.
         *
         * @param weaponType tipo de arma a equipar. No puede ser null.
         * @return this.
         */
        public Builder weapon(WeaponType weaponType) {
            if (weaponType == null)
                throw new IllegalArgumentException("weaponType no puede ser null");
            weapons.add(weaponType);
            return this;
        }

        /**
         * Añade un tipo de bala al loadout.
         * La primera bala añadida será la activa al inicio.
         *
         * @param bulletType tipo de bala a equipar. No puede ser null.
         * @return this.
         */
        public Builder bullet(BulletType bulletType) {
            if (bulletType == null)
                throw new IllegalArgumentException("bulletType no puede ser null");
            bullets.add(bulletType);
            return this;
        }

        /**
         * Establece el tipo de bala base del loadout — método de compatibilidad.
         * Equivale a bullet(bulletType) si no se han añadido balas aún,
         * o reemplaza la primera bala si ya hay alguna.
         *
         * @deprecated Usar bullet(bulletType) para mayor claridad
         */
        @Deprecated
        public Builder bulletType(BulletType bulletType) {
            if (bulletType == null)
                throw new IllegalArgumentException("bulletType no puede ser null");
            if (bullets.isEmpty()) {
                bullets.add(bulletType);
            } else {
                bullets.set(0, bulletType);
            }
            return this;
        }

        /**
         * Construye el PlayerLoadout.
         *
         * @throws IllegalStateException si no se añadió ningún arma o ninguna bala.
         */
        public PlayerLoadout build() {
            if (weapons.isEmpty())
                throw new IllegalStateException(
                    "PlayerLoadout: debe declararse al menos un arma");
            if (bullets.isEmpty())
                throw new IllegalStateException(
                    "PlayerLoadout: debe declararse al menos una bala");
            return new PlayerLoadout(weapons, bullets);
        }
    }
}
