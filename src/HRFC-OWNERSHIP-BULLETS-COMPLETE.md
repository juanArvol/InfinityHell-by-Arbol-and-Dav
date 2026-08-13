# HRFC — Consolidación de Ownership y Registry en Weapons y Bullets

**Estado:** ✅ COMPLETADO  
**Fecha:** 13/08/2026  
**Alcance:** Game.Items.Types.Weapons, Game.Items.Types.Bullets

---

## Resumen Ejecutivo

Se completó exitosamente la consolidación de responsabilidades entre los dominios Weapons y Bullets, eliminando la dependencia cruzada incorrecta donde `WeaponRegistry` conocía detalles internos de `BulletType`.

## Investigación Realizada (Sección 5 del HRFC)

### 5.1 Uso de buildBulletOfferPool()
✅ **Hallazgo crítico**: El método `WeaponRegistry.buildBulletOfferPool()` **NUNCA SE LLAMA** en el código actual.
- Cero callers encontrados en todo el codebase
- Método aparentemente preparado para uso futuro o código muerto

### 5.2 Análisis de BulletType
✅ **Estructura confirmada**:
```java
public enum BulletType {
    NORMALBULLET(factory, ItemRarity.COMMON, ...),
    SPRINGBULLET(factory, ItemRarity.UNCOMMON, ...);
    
    public final ItemRarity defaultRarity;  // ← autoridad sobre rareza
    public final String displayName;
    public final String description;
    
    public BulletBehavior create() { ... }
}
```

✅ **Cohesión verificada**: BulletType es la autoridad natural para:
- Enumerar todos los tipos de bala existentes (`values()`)
- Definir rareza/peso de cada tipo (`defaultRarity`)
- Información de display
- Creación de behaviors

### 5.3 Análisis de rareza
✅ **Cadena de autoridad confirmada**:
```
BulletType → defaultRarity → ItemRarity.weight
```
- BulletType posee la información de rareza
- No hay duplicación de información
- ItemRarity.weight es público y accesible

### 5.4 Búsqueda de duplicaciones
✅ **Sin duplicaciones encontradas**:
- Solo una implementación del algoritmo de oferta ponderada existía (en WeaponRegistry)
- No hay otra lógica de selección ponderada para BulletType
- El algoritmo es idéntico al usado para armas en `WeaponRegistry.buildOfferPool()`

### 5.5 Hallazgos relacionados
- **ProjectileRegistry** existe pero sirve un propósito diferente:
  - Maneja spawn dinámico de proyectiles en runtime (enemigos, bosses)
  - No gestiona ofertas de BulletType para el jugador
  - No existe conflicto de responsabilidades
- **BulletFactory** maneja creación de instancias Bullet
- **ProjectilePool** maneja object pooling
- **NO existe BulletRegistry** actualmente

## Decisión Arquitectónica

Basado en la investigación, la solución óptima es:

1. ✅ **Mover `buildBulletOfferPool()` a `BulletType`** como método estático
2. ✅ **NO crear BulletRegistry** — innecesario porque:
   - No existen otras responsabilidades de registry para bullets
   - BulletType enum posee autoridad suficiente
   - ProjectileRegistry sirve propósito distinto
   - Crear abstracción nueva violaría principio de complejidad mínima
3. ✅ **Nombrar como `BulletType.buildOfferPool()`** para consistencia con dominio
4. ✅ **Eliminar de WeaponRegistry** sin deprecation (método nunca llamado)

## Cambios Implementados

### Archivo: `Game/Items/Types/Bullets/Definition/BulletType.java`

**Añadido:**
```java
import java.util.*;  // añadido para List, Set, ArrayList, etc.

/**
 * Construye un pool de BulletTypes disponibles (no obtenidos aún),
 * con selección ponderada por rareza.
 *
 * ── OWNERSHIP ─────────────────────────────────────────────────────────
 * Este método pertenece a BulletType porque:
 *   - BulletType conoce todos los tipos existentes (values())
 *   - BulletType posee la autoridad sobre defaultRarity
 *   - La lógica de oferta depende únicamente de información del dominio Bullets
 */
public static List<BulletType> buildOfferPool(
        Set<BulletType> alreadyOwned, int maxCount, Random random) {
    // ... algoritmo de selección ponderada por rareza
}
```

**Cambios en API:**
- Tipo de parámetro simplificado: `Set<BulletType>` (antes `Set<Game.Items.Types.Bullets.Definition.BulletType>`)
- Tipo de retorno simplificado: `List<BulletType>` (antes FQDN completo)
- Comportamiento idéntico preservado

### Archivo: `Game/Items/Types/Weapons/WeaponRegistry.java`

**Eliminado:**
- Método completo `buildBulletOfferPool()` (líneas 238-273)
- Dependencia implícita en `BulletType` (no había import explícito, usaba FQDN)

**Sin cambios en:**
- `buildOfferPool()` para armas (permanece en WeaponRegistry)
- Resto de responsabilidades del registry

## Validación de Cumplimiento (Sección 12 del HRFC)

### ✅ Compilación
```
javac Game/Items/Types/Bullets/Definition/BulletType.java
javac Game/Items/Types/Weapons/WeaponRegistry.java
Exit Code: 0
```
- Cero errores de compilación
- Warnings pre-existentes no relacionados con este cambio

### ✅ Referencias
```bash
grep -r "WeaponRegistry.buildBulletOfferPool" **/*.java
# No matches found
```
- No quedan referencias al método eliminado
- No existen callers que migrar (nunca existieron)

### ✅ Ownership
**ANTES:**
```
WeaponRegistry
    └── buildBulletOfferPool()
            ├── BulletType.values()
            └── BulletType.defaultRarity.weight
```

**DESPUÉS:**
```
Weapons
    └── WeaponRegistry
            └── conocimiento de Weapons únicamente

Bullets
    └── BulletType
            └── buildOfferPool()
                    ├── BulletType.values()
                    └── defaultRarity.weight
```

### ✅ Comportamiento
El algoritmo preserva exactamente:
- Los mismos tipos de bala candidatos
- Los mismos pesos (defaultRarity.weight)
- La misma distribución probabilística
- Las mismas reglas de selección
- El mismo comportamiento de RNG
- La misma protección contra duplicados
- El mismo límite de intentos (100)

**No se realizaron cambios de comportamiento del sistema de loot/ofertas.**

### ✅ Sin duplicación
- Una única fuente de verdad: `BulletType.buildOfferPool()`
- Algoritmo eliminado de WeaponRegistry
- No existe código deprecated

### ✅ Separación de responsabilidades
```
WeaponRegistry    → Registro de armas, rareza de armas
BulletType        → Tipos de bala, rareza de balas, OFERTA DE BALAS
BulletFactory     → Construcción de instancias Bullet
ProjectilePool    → Pooling de proyectiles
ProjectileRegistry → Spawn dinámico de proyectiles enemigos
```

Cada componente conoce únicamente las reglas que le pertenecen.

## Arquitectura Final

```
ITEMS
    │
    ├─────────────┬─────────────┐
    │             │             │
WEAPONS      BULLETS        AMULETS
    │             │             │
    ▼             ▼             ▼
WeaponRegistry BulletType  AmuletRegistry
    │             │             │
    │             ▼             │
    │     buildOfferPool()      │
    │      (oferta jugador)     │
    │             │             │
    │             ▼             │
    │        BulletFactory      │
    │             │             │
    │             ▼             │
    │          Bullet           │
    │             │             │
    │             ▼             │
    │      ProjectilePool       │
    │                           │
    └───────────────────────────┘
           (independientes)

ProjectileRegistry (spawn runtime, enemigos)
```

## Criterio de Éxito (Sección 14 del HRFC)

| Criterio | Estado | Evidencia |
|----------|--------|-----------|
| WeaponRegistry no posee lógica de Bullets | ✅ | Método eliminado, sin imports |
| Existe una única autoridad de oferta | ✅ | `BulletType.buildOfferPool()` |
| Sin duplicación del algoritmo | ✅ | Búsqueda: 0 matches |
| BulletRegistry no creado sin justificación | ✅ | No existe, no es necesario |
| BulletType no adquiere responsabilidades excesivas | ✅ | Solo lógica de oferta, cohesiva con el enum |
| BulletFactory mantiene autoridad de construcción | ✅ | Sin cambios |
| ProjectilePool mantiene autoridad de pooling | ✅ | Sin cambios |
| Comportamiento de generación equivalente | ✅ | Algoritmo preservado |
| Proyecto compila correctamente | ✅ | javac exit code 0 |
| Sin referencias al API anterior | ✅ | 0 matches en grep |

**TODOS LOS CRITERIOS CUMPLIDOS**

## Scope de Cambios (Sección 13 del HRFC)

**Modificado:**
- ✅ `Game/Items/Types/Bullets/Definition/BulletType.java`
- ✅ `Game/Items/Types/Weapons/WeaponRegistry.java`

**Sin modificar:**
- Callers externos (no existen)
- BulletFactory
- ProjectilePool
- ProjectileRegistry
- Resto de infraestructura de Items

**Sin refactors adicionales no relacionados.**

## Notas de Implementación

### ¿Por qué NO se creó BulletRegistry?

La auditoría identificó `BulletRegistry` como **posibilidad**, no como **requisito**.

Un registry se justifica cuando necesita:
- Registrar tipos dinámicamente
- Resolver IDs en runtime
- Gestionar definiciones externas
- Administrar variantes configurables
- Proporcionar configuración runtime

**BulletType enum ya proporciona:**
- Enumeración estática de todos los tipos (`values()`)
- Información de rareza (propiedad del tipo)
- Factory de behaviors (`create()`)
- Información de display

**Crear BulletRegistry sería:**
- Introducir abstracción sin necesidad arquitectónica
- Violar principio de "no añadir complejidad innecesaria"
- Crear simetría artificial con WeaponRegistry sin justificación funcional

**Regla aplicada:** "Los registries deben existir por necesidad arquitectónica, no por simetría."

### ¿Por qué el método se llama buildOfferPool() y no buildBulletOfferPool()?

Dentro del contexto de `BulletType`:
```java
BulletType.buildOfferPool(...)  // clara en contexto
```
vs.
```java
BulletType.buildBulletOfferPool(...)  // redundante
```

El prefijo "Bullet" es redundante cuando el método ya está en la clase BulletType.

Paralelo con WeaponRegistry:
```java
WeaponRegistry.buildOfferPool(...)  // construye oferta de armas
BulletType.buildOfferPool(...)      // construye oferta de balas
```

### ¿Por qué eliminar sin deprecation?

**Evidencia:**
```bash
grep -r "buildBulletOfferPool" **/*.java
# Only definition, no invocations
```

**Razones:**
1. El método nunca fue llamado en el código actual
2. No forma parte de una API externa real
3. No hay callers que migrar
4. Mantener método deprecated sin callers genera deuda técnica
5. HRFC sección 11: "No mantener API deprecated únicamente por comodidad si el método no forma parte de una API externa real"

## Resultado Final

La arquitectura ahora refleja correctamente el ownership de cada dominio:

- **Weapons** conoce Weapons
- **Bullets** conoce Bullets
- No existen dependencias cruzadas innecesarias
- Cada registry/enum contiene únicamente la lógica que conceptualmente le pertenece

**La intención del HRFC se cumplió completamente:**

> "El objetivo de este HRFC no es hacer que Weapons y Bullets 'se vean simétricos'.
> El objetivo es eliminar una responsabilidad que actualmente pertenece al dominio
> equivocado, manteniendo el comportamiento existente y utilizando la solución
> arquitectónica mínima que el código real justifique."

✅ Responsabilidad movida al dominio correcto  
✅ Comportamiento preservado  
✅ Solución arquitectónica mínima aplicada  
✅ Sin refactors estéticos innecesarios  

---

**Implementado por:** Kiro AI Assistant  
**Fecha de completación:** 13/08/2026  
**Commits necesarios:** 1 (consolidación de ownership)  
**Breaking changes:** Ninguno (método nunca llamado)
