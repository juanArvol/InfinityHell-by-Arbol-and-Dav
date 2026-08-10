package Game.Player;

import Game.Items.Types.Bullets.Definition.BulletType;
import Game.Items.Types.Weapons.WeaponType.WeaponType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuración declarativa del equipamiento inicial del jugador.
 *
 * ── HRFC — Player Reengineering ───────────────────────────────────────────
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * PlayerLoadout declara QUÉ armas y QUÉ tipo de bala comienza equipados
 * el jugador al inicio de la run. No construye nada — solo describe.
 *
 * La construcción real (new ModifiedWeapon, new WeaponPistola) ocurre en
 * PlayerAssembler, que lee el loadout y produce las instancias concretas.
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   PlayerLoadout   → QUÉ armas y balas comienza equipadas (configuración)
 *   PlayerAssembler → construye las instancias concretas (construcción)
 *   PlayerCombat    → ciclo de disparo runtime (ejecución)
 *
 * Antes, estas tres responsabilidades estaban mezcladas en el constructor
 * de Player:
 *
 *   combat.addWeapon(new ModifiedWeapon(new WeaponPistola(), BulletType.NORMALBULLET, ...))
 *
 * Ahora:
 *
 *   PlayerLoadout.defaultLoadout()  →  PlayerAssembler.assemble()  →  Player
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 *
 * Diferentes perfiles de inicio (tutorial, challenge, custom run) pueden
 * definirse creando diferentes instancias de PlayerLoadout sin tocar ni
 * PlayerCombat ni Player:
 *
 *   PlayerLoadout.defaultLoadout()
 *   PlayerLoadout.shotgunStart()
 *   PlayerLoadout.fromSaveFile(saveData)
 *
 * ── INVARIANTES ───────────────────────────────────────────────────────────
 *
 *   - Al menos una entrada de arma es requerida (validado en build()).
 *   - La primera entrada en la lista es el arma activa al inicio.
 *   - bulletType es el tipo de bala base equipado al inicio de la run.
 *
 * ── EJEMPLO DE USO ────────────────────────────────────────────────────────
 *
 *   // Loadout por defecto — pistola + bala normal:
 *   PlayerLoadout loadout = PlayerLoadout.defaultLoadout();
 *
 *   // Loadout custom:
 *   PlayerLoadout loadout = PlayerLoadout.builder()
 *       .weapon(WeaponType.PISTOLA)
 *       .weapon(WeaponType.ESCOPETA)
 *       .bulletType(BulletType.NORMALBULLET)
 *       .build();
 */
public final class PlayerLoadout {

    /** Tipos de arma que el jugador comienza equipados, en orden. */
    private final List<WeaponType> weapons;

    /** Tipo de bala base equipado al inicio de la run. */
    private final BulletType bulletType;

    private PlayerLoadout(List<WeaponType> weapons, BulletType bulletType) {
        this.weapons    = Collections.unmodifiableList(new ArrayList<>(weapons));
        this.bulletType = bulletType;
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
            .bulletType(BulletType.NORMALBULLET)
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
     * Tipo de bala base equipado al inicio de la run.
     *
     * @return BulletType. Nunca null.
     */
    public BulletType getBulletType() { return bulletType; }

    // ── Builder ───────────────────────────────────────────────────────────

    /** Punto de entrada del builder. */
    public static Builder builder() { return new Builder(); }

    /** Builder de PlayerLoadout. */
    public static final class Builder {

        private final List<WeaponType> weapons    = new ArrayList<>();
        private BulletType             bulletType = BulletType.NORMALBULLET;

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
         * Establece el tipo de bala base del loadout.
         *
         * @param bulletType tipo de bala. No puede ser null.
         * @return this.
         */
        public Builder bulletType(BulletType bulletType) {
            if (bulletType == null)
                throw new IllegalArgumentException("bulletType no puede ser null");
            this.bulletType = bulletType;
            return this;
        }

        /**
         * Construye el PlayerLoadout.
         *
         * @throws IllegalStateException si no se añadió ningún arma.
         */
        public PlayerLoadout build() {
            if (weapons.isEmpty())
                throw new IllegalStateException(
                    "PlayerLoadout: debe declararse al menos un arma");
            return new PlayerLoadout(weapons, bulletType);
        }
    }
}
