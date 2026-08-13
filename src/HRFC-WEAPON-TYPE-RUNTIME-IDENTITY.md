# HRFC — Weapon Type Runtime Identity

**Estado:** ✅ COMPLETADO  
**Prioridad:** Alta  
**Alcance:** Player + Weapons

---

## Resumen de Cambios

Este HRFC restaura la identidad de `WeaponType` en las instancias runtime de `ModifiedWeapon`, eliminando la funcionalidad incompleta que impedía:

- `WeaponInventory.hasWeapon(WeaponType)` — no podía determinar si el inventario contenía un arma específica
- `PlayerRuntime.selectWeapon(WeaponType)` — no podía seleccionar el arma correcta por tipo

### Causa Raíz

`ModifiedWeapon` representaba una instancia runtime de un arma modificada, pero **no conservaba explícitamente** su identidad declarativa: `WeaponType`. Esto provocaba que los métodos mencionados tuvieran TODOs equivalentes a:

```java
// TODO: Comparar por tipo cuando ModifiedWeapon exponga WeaponType
```

---

## Implementación

### 1. ModifiedWeapon — Añadir identidad inmutable

**Archivo:** `src/Game/Items/Types/Weapons/ModifiedWeapon.java`

**Cambios realizados:**

1. **Añadido campo inmutable:**
   ```java
   private final WeaponType weaponType;
   ```

2. **Actualizado constructor completo:**
   ```java
   public ModifiedWeapon(WeaponType weaponType,
                         WeaponComport comport,
                         AmuletInventory amulets,
                         ProjectilePool pool,
                         Object owner,
                         GameEventBus eventBus)
   ```
   - `weaponType` es el **primer parámetro** (obligatorio)
   - Añadida validación: `if (weaponType == null) throw new IllegalArgumentException(...)`

3. **Actualizados constructores sobrecargados:**
   - Todos los constructores ahora requieren `WeaponType` como primer parámetro
   - Constructores delegados pasan el `weaponType` al constructor completo

4. **Añadido método de consulta:**
   ```java
   public WeaponType getWeaponType() {
       return weaponType;
   }
   ```
   - Documentación explica que la identidad permanece inmutable
   - Permite comparación directa sin reflexión, IDs String o registries

---

### 2. WeaponInventory — Implementar hasWeapon(WeaponType)

**Archivo:** `src/Game/Items/Types/Weapons/WeaponInventory.java`

**Cambios realizados:**

**Antes:**
```java
public boolean hasWeapon(WeaponType weaponType) {
    // TODO: Implementar cuando ModifiedWeapon exponga WeaponType
    return false;
}
```

**Después:**
```java
public boolean hasWeapon(WeaponType weaponType) {
    if (weaponType == null) return false;
    
    for (ModifiedWeapon weapon : weapons) {
        if (weapon.getWeaponType() == weaponType) {
            return true;
        }
    }
    return false;
}
```

- Usa identidad tipada de `ModifiedWeapon.getWeaponType()`
- No usa reflexión, IDs String ni comparación de clases
- Retorna `true` si encuentra al menos una coincidencia

---

### 3. PlayerRuntime — Corregir selectWeapon(WeaponType)

**Archivo:** `src/Game/Player/PlayerRuntime.java`

**Cambios realizados:**

**Antes:**
```java
public boolean selectWeapon(WeaponType weaponType) {
    for (int i = 0; i < inventory.getWeaponCount(); i++) {
        ModifiedWeapon weapon = inventory.weapons().getWeapon(i);
        if (weapon != null) {
            // TODO: Comparar por tipo cuando ModifiedWeapon exponga WeaponType
            currentWeaponIndex = i;
            return true;
        }
    }
    return false;
}
```

**Después:**
```java
public boolean selectWeapon(WeaponType weaponType) {
    if (weaponType == null) return false;

    for (int i = 0; i < inventory.getWeaponCount(); i++) {
        ModifiedWeapon weapon = inventory.weapons().getWeapon(i);
        if (weapon != null && weapon.getWeaponType() == weaponType) {
            currentWeaponIndex = i;
            return true;
        }
    }
    return false;
}
```

- Busca el arma que coincide con el `WeaponType` solicitado
- Solo selecciona si encuentra una coincidencia exacta
- Retorna `false` si no se posee el tipo solicitado

---

### 4. PlayerAssembler — Conservar WeaponType en creación

**Archivo:** `src/Game/Player/PlayerAssembler.java`

**Cambios realizados:**

**Antes:**
```java
for (WeaponType weaponType : loadout.getWeapons()) {
    WeaponComport comport = weaponType.createComport();
    ModifiedWeapon weapon = new ModifiedWeapon(
        comport,
        amulets,
        player,
        eventBus
    );
    playerInventory.addWeapon(weapon);
}
```

**Después:**
```java
for (WeaponType weaponType : loadout.getWeapons()) {
    WeaponComport comport = weaponType.createComport();
    ModifiedWeapon weapon = new ModifiedWeapon(
        weaponType,  // ← Identidad declarativa conservada
        comport,
        amulets,
        player,
        eventBus
    );
    playerInventory.addWeapon(weapon);
}
```

- `weaponType` se pasa explícitamente al constructor
- Garantiza que toda arma creada en el flujo normal tiene identidad

---

## Validación de Criterios de Aceptación

✅ **ModifiedWeapon conserva su WeaponType de forma inmutable**
- Campo `private final WeaponType weaponType`
- Sin setter público

✅ **Existe una consulta tipada para obtenerlo**
- `public WeaponType getWeaponType()`

✅ **WeaponInventory.hasWeapon(WeaponType) funciona correctamente**
- Implementado usando `weapon.getWeaponType() == weaponType`

✅ **PlayerRuntime.selectWeapon(WeaponType) selecciona el arma solicitada**
- Busca coincidencia exacta de `WeaponType`
- No selecciona arbitrariamente la primera arma

✅ **Todas las rutas normales de creación proporcionan WeaponType**
- PlayerAssembler es la única ruta de creación
- Pasa `weaponType` explícitamente

✅ **No se utilizan Strings, reflexión o comparación de clases**
- Comparación directa usando `==` en enums

✅ **Los TODOs relacionados desaparecieron**
- Eliminados de `WeaponInventory.hasWeapon()`
- Eliminados de `PlayerRuntime.selectWeapon()`

✅ **Las modificaciones del arma no alteran su identidad**
- Campo es `final`, no hay setter

✅ **El proyecto compila sin warnings relacionados**
- `get_diagnostics` retornó: "No diagnostics found"

✅ **No se introducen cambios fuera del alcance**
- No se modificó `WeaponRegistry`
- No se modificó `WeaponComport`
- No se modificó `ProjectileResolver`
- No se introdujeron nuevos registries

---

## Casos de Validación

### Caso 1 — Inventario contiene el tipo
```java
Inventory: PISTOLA, ESCOPETA
hasWeapon(PISTOLA)   → true ✅
hasWeapon(ESCOPETA)  → true ✅
```

### Caso 2 — Inventario no contiene el tipo
```java
Inventory: PISTOLA
hasWeapon(ESCOPETA)  → false ✅
```

### Caso 3 — Selección correcta
```java
Inventory: PISTOLA, ESCOPETA, RIFLE
selectWeapon(ESCOPETA)  → ESCOPETA seleccionada ✅
```
No selecciona la primera arma simplemente porque existe.

### Caso 4 — Tipo inexistente
```java
Inventory: PISTOLA
selectWeapon(ESCOPETA)  → false, no cambia selección ✅
```

### Caso 5 — Modificaciones
```java
ModifiedWeapon weapon = new ModifiedWeapon(WeaponType.PISTOLA, ...);
// Aplicar modificaciones de daño, velocidad, fire mode...
weapon.getWeaponType()  → WeaponType.PISTOLA ✅
```
Las modificaciones no cambian la identidad del arma.

---

## Arquitectura Final

```
WeaponType
    │
    │ define
    ▼
ModifiedWeapon
    │
    ├── weaponType (immutable)
    ├── comport
    ├── modifications
    └── runtime state
```

**Flujo de consulta:**

```
PlayerRuntime
    ↓
WeaponInventory
    ↓
ModifiedWeapon
    ↓
WeaponType
```

---

## Archivos Modificados

1. `src/Game/Items/Types/Weapons/ModifiedWeapon.java`
2. `src/Game/Items/Types/Weapons/WeaponInventory.java`
3. `src/Game/Player/PlayerRuntime.java`
4. `src/Game/Player/PlayerAssembler.java`

**Total:** 4 archivos  
**Líneas modificadas:** ~80 líneas  
**TODOs eliminados:** 2

---

## Compatibilidad

- ✅ Sin cambios en `WeaponRegistry` (sistema de loot/tiendas separado)
- ✅ Sin cambios en `WeaponComport` (comportamiento del arma)
- ✅ Sin cambios en `ProjectileResolver` (resolución de proyectiles)
- ✅ Sin cambios en `PlayerCombat` (ejecución de combate)
- ✅ `PlayerCombat` solo usa `ModifiedWeapon`, no las crea

---

## Conclusión

La implementación restaura exitosamente la identidad runtime de `WeaponType`, permitiendo que el sistema de inventario y selección de armas funcione correctamente sin depender de inferencias indirectas, reflexión o IDs String.

**La intención del HRFC no era crear un nuevo sistema de armas, sino restaurar una pieza de identidad que el modelo runtime ya necesitaba y que se perdió durante la evolución incremental del sistema.**

---

**Fecha de implementación:** 2026-08-13  
**Estado:** ✅ COMPLETADO
