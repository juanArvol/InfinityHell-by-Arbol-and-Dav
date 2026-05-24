package Game.Engine.Components.Visuals;

/**
 * Modo de sincronización de tamaño entre SpriteRenderer y ColliderComponent.
 *
 * Se configura UNA VEZ en el constructor del objeto, sin efectos secundarios
 * ocultos. No hay dependencias circulares: SpriteRenderer aplica el sync
 * en su método start() (cuando ya tiene acceso al gameObject y sus componentes).
 *
 * ── Modos ──────────────────────────────────────────────────────────────────
 *
 *  NONE
 *    Independencia total. El sprite se dibuja a su tamaño natural.
 *    El collider tiene el tamaño que se definió explícitamente.
 *    Usá esto cuando querés control manual de ambos.
 *    Ejemplo: jugador con hitbox más chica que el sprite.
 *
 *  COLLIDER_TO_SPRITE
 *    El collider adopta el tamaño del sprite al iniciar.
 *    Útil para bloques del mundo: el BlockWorld tiene un sprite de 64x64
 *    y quiere que la hitbox sea exactamente ese rectángulo.
 *    El sprite NO cambia.
 *
 *  SPRITE_TO_COLLIDER
 *    El sprite se escala visualmente para coincidir con el collider.
 *    Útil cuando el gameplay define el tamaño (ej: hitbox de 15x24 para el jugador)
 *    y querés que el sprite se vea de ese tamaño exacto en pantalla.
 *    El collider NO cambia.
 *
 *  SPRITE_TO_COLLIDER_WITH_OFFSET
 *    Como SPRITE_TO_COLLIDER, pero además centra el sprite sobre el collider.
 *    Si el collider tiene un offsetX/Y, el sprite se desplaza para alinearse.
 *    Útil cuando el collider tiene offset y el sprite debe seguirlo.
 */
public enum SizeSyncMode {
    NONE,
    COLLIDER_TO_SPRITE,
    SPRITE_TO_COLLIDER,
    SPRITE_TO_COLLIDER_WITH_OFFSET
}
