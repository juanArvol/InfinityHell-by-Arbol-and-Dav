package Game.Items.Types.Weapons.WeaponType.FireMode;

import Game.Items.Types.Weapons.WeaponType.WeaponComport;

/**
 * Interfaz para modos de disparo de armas.
 *
 * ── HRFC — Unified DeltaTime Migration & Temporal Model Completion ────────
 *
 * MIGRACIÓN TEMPORAL:
 *   El método update() ahora recibe deltaTime para permitir que los FireModes
 *   gestionen estado temporal basado en tiempo real en lugar de frames.
 *
 *   Implementaciones:
 *   - AutoMode, SemiAutoMode: update() vacío (sin estado temporal)
 *   - ChargeMode: acumula tiempo de carga con deltaTime
 *   - BurstMode (futuro): cuenta tiempo entre disparos de ráfaga
 */
public interface iFireMode {

    /**
     * Procesa input y ejecuta la lógica del FireMode, potencialmente mutando su estado.
     * 
     * ── OPERACIÓN DE EJECUCIÓN ────────────────────────────────────────────
     * 
     * Esta operación:
     * - Procesa el input del usuario (held, pressed)
     * - Puede mutar el estado interno del FireMode (timers, cargas, etc.)
     * - Avanza la máquina de estados del modo de disparo
     * - Determina si se debe disparar y con qué multiplicadores
     * 
     * USO: Disparo real donde el input debe procesarse y el estado debe actualizarse.
     * 
     * @param held true si el botón está siendo mantenido
     * @param pressed true si el botón fue presionado este frame (edge-trigger)
     * @param weapon contexto del arma que contiene este FireMode
     * @return FireModeResult con decisión de disparo y multiplicadores
     */
    FireModeResult handleInput(
        boolean held,
        boolean pressed,
        WeaponComport weapon
    );

    /**
     * Consulta los parámetros de resolución actuales sin procesar input ni mutar estado.
     * 
     * ── OPERACIÓN DE CONSULTA ─────────────────────────────────────────────
     * 
     * Esta operación:
     * - Es idempotente: llamarla repetidamente produce el mismo resultado
     * - No muta el estado interno del FireMode
     * - No procesa input ni avanza timers
     * - Retorna los multiplicadores que corresponden al estado actual
     * 
     * USO: ProjectilePreview para obtener multiplicadores sin ejecutar disparo.
     * 
     * DIFERENCIA CON handleInput():
     * - handleInput(): procesa input → muta estado → retorna resultado
     * - queryResolution(): lee estado actual → retorna multiplicadores
     * 
     * @param held true si el botón está siendo mantenido (para modos como ChargeMode)
     * @param weapon contexto del arma que contiene este FireMode
     * @return FireModeResolution con multiplicadores actuales (sin decisión de disparo)
     */
    FireModeResolution queryResolution(
        boolean held,
        WeaponComport weapon
    );

    /**
     * Actualiza el estado interno del FireMode con el tiempo transcurrido.
     *
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * Recibe deltaTime del simulation step para gestionar estado temporal
     * de forma independiente del framerate.
     *
     * Implementaciones:
     * - AutoMode, SemiAutoMode: sin estado temporal, método vacío
     * - ChargeMode: acumula tiempo de carga
     * - BurstMode: cuenta tiempo entre disparos en ráfaga
     *
     * @param deltaTime tiempo real del simulation step en segundos
     */
    void update(double deltaTime);
} 