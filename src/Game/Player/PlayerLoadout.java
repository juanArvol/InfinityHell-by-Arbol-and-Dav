package Game.Player;

import Game.Items.Types.Ammulets.AmuletType;
import Game.Items.Types.Bullets.Definition.BulletType;
import Game.Items.Types.Weapons.WeaponType.WeaponType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuración declarativa del equipamiento inicial del jugador.
 *
 * ── HRFC — Player Reengineering v2 ────────────────────────────────────────
 * ── MINI-HRFC — API Única de Construcción ─────────────────────────────────
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
 *   > "¿Por qué comienza con esto?"        ← Contexto de uso externo
 *
 * ── SEPARACIÓN LOADOUT vs RUNTIME ────────────────────────────────────────
 *
 *   PlayerLoadout   → configuración inicial (inmutable)
 *   PlayerRuntime   → estado actual de la run (mutable)
 *   PlayerInventory → almacenamiento de posesiones
 *
 * ── FLUJO ARQUITECTÓNICO ─────────────────────────────────────────────────
 *
 *   Contexto externo (GameWorldBootstrap, SaveSystem, ChallengeSystem, etc.)
 *        │
 *        │ construye
 *        ▼
 *   PlayerLoadout (configuración declarativa)
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
 * ── API DECLARATIVA ÚNICA ─────────────────────────────────────────────────
 *
 * El loadout se construye mediante una única API declarativa:
 *
 *   PlayerLoadout
 *       .initialWeapons(WeaponType.PISTOLA, WeaponType.ESCOPETA)
 *       .initialBullets(BulletType.NORMAL_BULLET, BulletType.SPRING_BULLET)
 *       .initialAmulets("bone_tip", "swift_quill")
 *       .build()
 *
 * Cada método de configuración es opcional y puede omitirse:
 *
 *   PlayerLoadout
 *       .initialWeapons(WeaponType.PISTOLA)
 *       .initialBullets(BulletType.NORMAL_BULLET)
 *       .initialAmulets()  // Sin amuletos
 *       .build()
 *
 * El Player comenzará con todo el contenido disponible para uso independiente.
 *
 * ── MINI-HRFC — CONFIGURACIÓN VACÍA ───────────────────────────────────────
 *
 * El loadout soporta nativamente configuración vacía:
 *
 *   PlayerLoadout
 *       .initialWeapons()
 *       .initialBullets()
 *       .initialAmulets()
 *       .build()
 *
 * Esto representa un estado válido donde el Player comienza sin armas,
 * balas ni amuletos. El sistema permanece estable en este estado.
 *
 * ── NO ES UN CATÁLOGO DE PRESETS ─────────────────────────────────────────
 *
 * PlayerLoadout NO contiene métodos estáticos para configuraciones concretas.
 * Los contextos externos son responsables de construir el loadout apropiado:
 *
 *   • Nueva partida       → construye loadout inicial
 *   • Continuar partida   → construye desde save
 *   • Challenge           → construye según desafío
 *   • Testing/Development → construye según necesidad
 *
 * ── INVARIANTES ───────────────────────────────────────────────────────────
 *
 *   • Todas las categorías pueden estar vacías (estado válido)
 *   • La primera arma en la lista es la activa al inicio (si hay armas)
 *   • La primera bala en la lista es la activa al inicio (si hay balas)
 *   • Los amuletos se aplican automáticamente (si hay amuletos)
 */
public final class PlayerLoadout {

    /** Tipos de arma que el jugador comienza equipados, en orden. */
    private final List<WeaponType> weapons;

    /** Tipos de bala que el jugador comienza equipados, en orden. */
    private final List<BulletType> bullets;

    /** Definiciones de amuletos que el jugador comienza equipados. */
    private final List<AmuletType> amulets;

    private PlayerLoadout(List<WeaponType> weapons, 
                          List<BulletType> bullets,
                          List<AmuletType> amulets) {
        this.weapons = Collections.unmodifiableList(new ArrayList<>(weapons));
        this.bullets = Collections.unmodifiableList(new ArrayList<>(bullets));
        this.amulets = Collections.unmodifiableList(new ArrayList<>(amulets));
    }

    // ── Acceso ────────────────────────────────────────────────────────────

    /**
     * Lista inmutable de tipos de arma del loadout, en orden.
     * El primer elemento es el arma activa al inicio (si hay armas).
     *
     * @return lista de WeaponType (puede estar vacía). Nunca null.
     */
    public List<WeaponType> getWeapons() { return weapons; }

    /**
     * Lista inmutable de tipos de bala del loadout, en orden.
     * El primer elemento es la bala activa al inicio (si hay balas).
     *
     * @return lista de BulletType (puede estar vacía). Nunca null.
     */
    public List<BulletType> getBullets() { return bullets; }

    /**
     * Lista inmutable de definiciones de amuletos del loadout.
     *
     * @return lista de ItemDefinition (puede estar vacía). Nunca null.
     */
    public List<AmuletType> getAmulets() { return amulets; }

    // ── HRFC — Consolidación y Limpieza de Legacy ────────────────────────
    // getBulletType() fue eliminado. PlayerLoadout ahora maneja listas completas.
    // Migración: usar getBullets().get(0) para la primera bala o getBullets() para todas.
    
    // ── MINI-HRFC — API ÚNICA DE CONSTRUCCIÓN ────────────────────────────
    // El método builder() fue eliminado como API pública.
    // El Builder existe únicamente como mecanismo interno de implementación.
    // La única API pública de construcción es la declarativa:
    //   PlayerLoadout.initialWeapons(...).initialBullets(...).initialAmulets(...).build()

    // ── MINI-HRFC — API Declarativa ──────────────────────────────────────

    /**
     * Crea un builder y configura las armas iniciales de forma declarativa.
     * 
     * ── API DECLARATIVA ───────────────────────────────────────────────────
     * 
     * Permite la sintaxis ergonómica buscada por el Mini-HRFC:
     * 
     *   initialWeapons(WeaponType.PISTOLA, WeaponType.ESCOPETA);
     *   initialBullets(BulletType.NORMAL_BULLET, BulletType.EXPLOSIVE);
     *   initialAmulets();
     *   return build();
     *
     * Esta capacidad pertenece directamente a PlayerLoadout, no a capas auxiliares.
     *
     * @param weapons tipos de arma iniciales (varargs, puede ser vacío)
     * @return builder configurado con las armas especificadas
     */
    public static Builder initialWeapons(WeaponType... weapons) {
        Builder builder = new Builder();
        for (WeaponType weapon : weapons) {
            builder.weapon(weapon);
        }
        return builder;
    }

    /**
     * Crea un builder y configura las balas iniciales de forma declarativa.
     * 
     * @param bullets tipos de bala iniciales (varargs, puede ser vacío)
     * @return builder configurado con las balas especificadas
     */
    public static Builder initialBullets(BulletType... bullets) {
        Builder builder = new Builder();
        for (BulletType bullet : bullets) {
            builder.bullet(bullet);
        }
        return builder;
    }

    /**
     * Crea un builder y configura los amuletos iniciales de forma declarativa.
     * 
     * ── RESOLUCIÓN DE DEFINICIONES ────────────────────────────────────────
     * 
     * Los IDs se resuelven a ItemDefinition mediante AmuletRegistry.get().
     * Si un ID no existe, se lanza IllegalArgumentException con mensaje claro.
     *
     * @param amuletIds IDs de amuletos iniciales (varargs, puede ser vacío)
     * @return builder configurado con los amuletos especificados
     * @throws IllegalArgumentException si algún ID no está registrado
     */
    public static Builder initialAmulets(AmuletType... amulets) {
        Builder builder = new Builder();
        for (AmuletType amulet : amulets) {
            builder.amulet(amulet);
        }
        return builder;
    }

    /**
     * Builder de PlayerLoadout.
     * 
     * ── MINI-HRFC — API ÚNICA ─────────────────────────────────────────────
     * 
     * Este Builder NO tiene constructor público ni método builder() público.
     * Solo es accesible a través de los métodos declarativos:
     * 
     *   PlayerLoadout.initialWeapons(...)
     *   PlayerLoadout.initialBullets(...)
     *   PlayerLoadout.initialAmulets(...)
     * 
     * Aunque la clase es pública (requerido para acceso cross-package), 
     * NO puede ser instanciada directamente desde código externo.
     * Este diseño garantiza que existe un único camino de construcción.
     */
    public static final class Builder {

        private final List<WeaponType> weapons = new ArrayList<>();
        private final List<BulletType> bullets = new ArrayList<>();
        private final List<AmuletType> amulets = new ArrayList<>();

        private Builder() {}

        /**
         * Añade un tipo de arma al loadout.
         * El primer arma añadida será la activa al inicio.
         *
         * ── API INTERNA ───────────────────────────────────────────────────
         * Este método es interno y solo se usa desde los métodos estáticos
         * de configuración declarativa.
         *
         * @param weaponType tipo de arma a equipar. No puede ser null.
         * @return this.
         */
        private Builder weapon(WeaponType weaponType) {
            if (weaponType == null)
                throw new IllegalArgumentException("weaponType no puede ser null");
            weapons.add(weaponType);
            return this;
        }

        /**
         * Añade un tipo de bala al loadout.
         * La primera bala añadida será la activa al inicio.
         *
         * ── API INTERNA ───────────────────────────────────────────────────
         * Este método es interno y solo se usa desde los métodos estáticos
         * de configuración declarativa.
         *
         * @param bulletType tipo de bala a equipar. No puede ser null.
         * @return this.
         */
        private Builder bullet(BulletType bulletType) {
            if (bulletType == null)
                throw new IllegalArgumentException("bulletType no puede ser null");
            bullets.add(bulletType);
            return this;
        }

        /**
         * Añade un amuleto al loadout.
         *
         * ── MINI-HRFC — BOOTSTRAP DECLARATIVO ────────────────────────────
         *
         * Los amuletos utilizan las abstracciones reales del sistema:
         * ItemDefinition desde AmuletRegistry, no IDs String directamente.
         *
         * ── API INTERNA ───────────────────────────────────────────────────
         * Este método es interno y solo se usa desde los métodos estáticos
         * de configuración declarativa.
         *
         * Uso:
         *   builder.amulet(AmuletRegistry.get("bone_tip"))
         *
         * @param amulet definición del amuleto a equipar. No puede ser null.
         * @return this.
         */
        private Builder amulet(AmuletType amulet) {
            if (amulet == null)
                throw new IllegalArgumentException("amulet no puede ser null");
            amulets.add(amulet);
            return this;
        }

        /**
         * Configura las balas iniciales del loadout.
         * 
         * ── API DECLARATIVA FLUIDA ────────────────────────────────────────
         * 
         * Permite configurar balas en el contexto de un builder ya existente:
         * 
         *   builder()
         *       .weapon(WeaponType.PISTOLA)
         *       .initialBullets(BulletType.NORMAL_BULLET, BulletType.EXPLOSIVE)
         *       .build();
         *
         * @param bulletTypes tipos de bala iniciales (varargs, puede ser vacío)
         * @return this.
         */
        public Builder initialBullets(BulletType... bulletTypes) {
            for (BulletType bulletType : bulletTypes) {
                bullet(bulletType);
            }
            return this;
        }

        /**
         * Configura los amuletos iniciales del loadout.
         * 
         * ── API DECLARATIVA FLUIDA ────────────────────────────────────────
         * 
         * Permite configurar amuletos en el contexto de un builder ya existente:
         * 
         *   builder()
         *       .weapon(WeaponType.PISTOLA)
         *       .initialBullets(BulletType.NORMAL_BULLET)
         *       .initialAmulets("bone_tip", "swift_quill")
         *       .build();
         *
         * @param amuletIds IDs de amuletos iniciales (varargs, puede ser vacío)
         * @return this.
         * @throws IllegalArgumentException si algún ID no está registrado
         */
        public Builder initialAmulets(AmuletType... amulets) {
            for (AmuletType amulet : amulets) {
                amulet(amulet);
            }
            return this;
        }

        // ── HRFC — Consolidación y Limpieza de Legacy ────────────────────
        // Builder.bulletType() fue eliminado. Usar Builder.bullet() en su lugar.

        /**
         * Construye el PlayerLoadout.
         *
         * ── MINI-HRFC — CONFIGURACIÓN VACÍA ───────────────────────────────
         *
         * NO valida que haya al menos un arma o una bala. La configuración
         * vacía es válida y representa que el Player comienza sin contenido.
         *
         * El sistema está diseñado para soportar inventarios vacíos de forma
         * segura en todos los niveles (runtime, combat, UI).
         *
         * ── API INTERNA ───────────────────────────────────────────────────
         *
         * Este método build() es interno. Los contextos externos no acceden
         * directamente al Builder, sino a través de la API declarativa:
         *
         *   PlayerLoadout.initialWeapons(...).initialBullets(...).build()
         */
        public PlayerLoadout build() {
            return new PlayerLoadout(weapons, bullets, amulets);
        }
    }
}
