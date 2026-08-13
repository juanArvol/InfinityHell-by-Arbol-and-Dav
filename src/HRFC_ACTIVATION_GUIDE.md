# Guía de Activación — MetheorBullet

**HRFC:** Consolidación y Limpieza de Legacy  
**Estado:** Implementación completa, registro pendiente

---

## MetheorBullet — Activación

MetheorBullet fue migrado completamente al sistema actual como `MetheorBulletBehavior`.

### Estado Actual

✅ **Implementado:** `Game/Items/Types/Bullets/BulletComport/BulletClass/MetheorBullet.java`  
❌ **No registrado:** Requiere activación manual en BulletType

---

## Cómo Activar

### 1. Registrar en BulletType

**Archivo:** `Game/Items/Types/Bullets/Definition/BulletType.java`

**Ubicación:** Después de `SPRINGBULLET`, en la sección de efectos elementales

**Código a descomentar:**

```java
/**
 * Meteoro del Vacío — proyectil de alta masa con explosión devastadora.
 * Escala daño con velocidad de caída.
 */
VOIDMETEOR   (MetheorBullet::new, ItemRarity.RARE,
              "Meteoro del Vacío",
              "Proyectil de alta masa que genera explosiones devastadoras."),
```

### 2. Verificar Imports

Asegúrate de que el import esté presente:

```java
import Game.Items.Types.Bullets.BulletComport.BulletClass.MetheorBullet;
```

### 3. Probar

```java
// En código de test o loadout:
PlayerLoadout.builder()
    .weapon(WeaponType.PISTOLA)
    .bullet(BulletType.VOIDMETEOR)  // <- Ahora disponible
    .build()
```

---

## Comportamiento Esperado

### Características

- **Gravedad:** Sí (GravityMovement)
- **Daño directo:** 10
- **Daño explosión base:** 35
- **Escalado:** |velocityY| × 2.3
- **Radio:** 250 + (explosionPower × 1.5)
- **Lifetime:** 300 ticks (5 segundos)

### Al Disparar

1. Proyectil sale con trayectoria parabólica (gravedad)
2. Al impactar enemigo o terreno → explosión
3. Daño en área escalado con velocidad de caída
4. Empuje radial a entities cercanas

### Comparación con Legacy

| Aspecto | Legacy | Actual |
|---------|--------|--------|
| Gravedad | `hasGravity()` flag | GravityMovement composable |
| Colisión | Manual dispatch | Polimorfismo BulletBehavior |
| Explosión | Mutación directa | ProjectileContext.applyAreaEffect() |
| Dependencias | Player, EnimyNormal, Ambiente | CollisionProfile genérico |

---

## Troubleshooting

### "El proyectil no explota"

**Causa:** `bullet.getContext()` retorna null

**Solución:** Verificar que ProjectilePool tenga context inyectado:
```java
registry.setProjectileContext(new WorldProjectileContext(worldManager));
```

### "Compilación falla con 'ProjectileContext no tiene applyAreaEffect'"

**Causa:** ProjectileContext aún no implementa el método

**Solución temporal:** Modificar explode() para no usar applyAreaEffect:
```java
private void explode(Bullet bullet) {
    // TODO: Implementar cuando ProjectileContext.applyAreaEffect() exista
    System.out.println("[MetheorBullet] Explosion at " + bullet.getPosition());
}
```

---

## Próximos Pasos

1. ✅ MetheorBullet implementado
2. ⏳ Añadir a BulletType (manual)
3. ⏳ Verificar ProjectileContext.applyAreaEffect()
4. ⏳ Crear asset "void_meteor"
5. ⏳ Playtest y ajustar balance

---

**Documento completo:** Ver `HRFC_CONSOLIDATION_REPORT.md`
