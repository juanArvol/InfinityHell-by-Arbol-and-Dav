package Game.Items.Types.Weapons.WeaponType.FireMode;

public class FireModeResult {

    private final boolean shouldShoot;
    private final double damageMultiplier;
    private final double speedMultiplier;

    public FireModeResult(
        boolean shouldShoot,
        double damageMultiplier,
        double speedMultiplier
    ) {
        this.shouldShoot = shouldShoot;
        this.damageMultiplier = damageMultiplier;
        this.speedMultiplier = speedMultiplier;
    }

    public boolean shouldShoot() { return shouldShoot; }
    public double getDamageMultiplier() { return damageMultiplier; }
    public double getSpeedMultiplier() { return speedMultiplier; }
} 