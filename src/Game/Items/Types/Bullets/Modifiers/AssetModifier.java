package Game.Items.Types.Bullets.Modifiers;

import Game.Items.Types.Bullets.ProjectileBlueprint;
import Game.Items.Types.Bullets.ProjectileModifier;

/**
 * Modifier que establece el assetKey de un proyectil.
 *
 * Permite que cualquier sistema externo cambie el sprite de un proyectil
 * sin modificar BulletFactory ni el BulletBehavior original.
 *
 * La resolución real (assetKey → BufferedImage) la hace BulletAssetResolver
 * dentro de BulletFactory.build(). Este modifier solo declara la intención.
 *
 * Uso:
 *   // Proyectil con sprite específico:
 *   ProjectileModifier cometaSprite = new AssetModifier("bullet.cometa");
 *   blueprint = cometaSprite.apply(blueprint);
 *
 *   // Inline:
 *   blueprint = blueprint.withAssetKey("bullet.cometa");
 */
public final class AssetModifier implements ProjectileModifier {

    private final String assetKey;

    /**
     * @param assetKey clave del sprite en AssetRegistry (ej: "bullet.cometa").
     *                 null = sprite por defecto "bullet.bala".
     */
    public AssetModifier(String assetKey) {
        this.assetKey = assetKey;
    }

    @Override
    public ProjectileBlueprint apply(ProjectileBlueprint blueprint) {
        return blueprint.withAssetKey(assetKey);
    }
}
