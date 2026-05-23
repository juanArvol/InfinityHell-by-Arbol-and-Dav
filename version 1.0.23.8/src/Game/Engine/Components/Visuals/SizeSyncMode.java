package Game.Engine.Components.Visuals;

public enum SizeSyncMode {
    NONE,               // Nada automático
    HITBOX_TO_SPRITE,   // Hitbox copia tamaño del sprite
    SPRITE_TO_HITBOX,   // Sprite copia tamaño de hitbox
    BIDIRECTIONAL       // Siempre sincronizados dinámicamente
}