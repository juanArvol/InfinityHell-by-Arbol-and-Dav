# HRFC-025 — Eliminación de la Deuda Histórica CoreProperties / CoreRelations

## Introducción

Este documento especifica los requisitos para la eliminación completa de los conceptos históricos `CoreProperties` y `CoreRelations` del World Simulation Core, sustituyéndolos por una organización exclusivamente orientada a dominios físicos.

Esta corrección no introduce funcionalidades nuevas, no modifica ninguna ecuación, no altera el comportamiento del Solver y no cambia ninguna API pública del motor de simulación. Su propósito es eliminar la última deuda arquitectónica del diseño original, donde parte del sistema permanecía organizada según criterios históricos ("Core") en lugar de criterios de dominio físico.

### Contexto arquitectónico

El análisis del codebase confirma la siguiente situación previa a HRFC-025:

**`CoreProperties.java`** contiene 16 descriptores de 4 dominios físicos distintos mezclados en un único catálogo:
- Dominio térmico: `TEMPERATURE`, `THERMAL_CONDUCTIVITY`, `HEAT_CAPACITY`, `THERMAL_DIFFUSIVITY`, `MELTING_POINT`, `BOILING_POINT`
- Dominio eléctrico: `CHARGE`, `ELECTRICAL_CONDUCTIVITY`
- Dominio fluídico: `HUMIDITY`, `HUMIDITY_ABSORPTION`, `VISCOSITY`
- Dominio mecánico: `PRESSURE`, `COMPRESSIBILITY`, `ELASTICITY`, `HARDNESS`, `DENSITY`

**`CoreRelations.java`** contiene 12 relaciones de 4 dominios físicos distintos mezcladas en un único catálogo:
- Dominio térmico: `THERMAL_CONDUCTION`, `THERMAL_AMBIENT_DISSIPATION`, `THERMAL_EXCESS_CORRECTION`, `VOLUMETRIC_EXPANSION`, `RADIATION_THERMAL_CONVERSION`
- Dominio eléctrico: `ELECTRICAL_TRANSFER`, `ELECTRICAL_DISSIPATION`, `ELECTRICAL_EXCESS_CORRECTION`, `JOULE_HEATING`
- Dominio fluídico: `FLUID_DIFFUSION`, `FLUID_AMBIENT_DISSIPATION`, `FLUID_SATURATION_RELEASE`

**Consumidores actuales de `CoreProperties`** (16 archivos):
- 10 evaluadores: `BernoulliEvaluator`, `FickEvaluator`, `FourierEvaluator`, `HookeEvaluator`, `JouleEvaluator`, `OhmEvaluator`, `PascalEvaluator`, `RadiationThermalEvaluator`, `ArchimedesEvaluator`, `StokesEvaluator`
- 4 catálogos de relaciones: `CoreRelations`, `ElectromagneticRelations`, `MaterialStateRelations`, `RadiationRelations`
- Componente: `MaterialComponent`
- Gameplay externo: `WorldFieldPresets`

**Consumidores actuales de `CoreRelations`** (1 archivo):
- `WorldSimulation.java` — en el método `withDefaults()`

---

## Requirements

### REQ-000: Criterio de organización arquitectónica

**Como** arquitecto del sistema,
**quiero** que toda organización del código se realice exclusivamente por cohesión semántica de dominio físico,
**para que** la arquitectura refleje el modelo físico del Engine y no pueda degradarse bajo criterios de conveniencia o distribución uniforme.

Este requisito es el principio rector de todos los demás. Cualquier decisión de organización en este HRFC y en los futuros deberá responder únicamente a la pregunta:

> ¿Este elemento pertenece al mismo fenómeno físico que los demás elementos de este módulo?

Nunca a:
> ¿Este archivo ya tiene muchos elementos? ¿Podemos moverlo aquí para equilibrar?

#### Criterios de aceptación

- [ ] Cada catálogo `*Properties` agrupa exclusivamente propiedades que pertenecen al mismo fenómeno físico, independientemente de cuántas sean.
- [ ] Cada catálogo `*Relations` agrupa exclusivamente relaciones que modelan el mismo fenómeno físico, independientemente de cuántas sean.
- [ ] La distribución de elementos entre catálogos no está justificada por cantidad ni por equilibrio de tamaño, sino por pertenencia al dominio físico.
- [ ] Un dominio con dos propiedades y un dominio con veinte propiedades permanecen en catálogos separados si representan fenómenos físicos distintos.
- [ ] Ningún elemento ha sido movido a un catálogo de dominio diferente al suyo por razones de distribución uniforme, legibilidad de tamaño o conveniencia de agrupación.
- [ ] El Javadoc de cada catálogo declara explícitamente el fenómeno físico que modela, de forma que cualquier contribuidor futuro pueda determinar sin ambigüedad si un nuevo elemento pertenece o no a ese catálogo.

---

### REQ-001: Creación de ThermalProperties

**Como** arquitecto del sistema,
**quiero** que todas las propiedades del dominio térmico estén encapsuladas en un único catálogo `ThermalProperties`,
**para que** el código exprese el dominio físico que modela, no la historia del proyecto.

#### Criterios de aceptación

- [ ] Existe la clase `Game.Engine.World.Physics.ThermalProperties` en el paquete `Game.Engine.World.Physics`.
- [ ] `ThermalProperties` contiene exactamente los siguientes descriptores, con los mismos valores de construcción (id, defaultValue, min, max, bounded, description) que tenían en `CoreProperties`:
  - `TEMPERATURE`
  - `THERMAL_CONDUCTIVITY`
  - `HEAT_CAPACITY`
  - `THERMAL_DIFFUSIVITY`
  - `MELTING_POINT`
  - `BOILING_POINT`
- [ ] `ThermalProperties` tiene constructor privado y es `final`.
- [ ] `ThermalProperties` reside en el paquete `Game.Engine.World.Physics`, garantizando acceso al constructor package-private de `PropertyDescriptor`.
- [ ] Ningún id de descriptor de `ThermalProperties` colisiona con ningún id en otro catálogo del paquete.

---

### REQ-002: Creación de ElectricalProperties

**Como** arquitecto del sistema,
**quiero** que todas las propiedades del dominio eléctrico estén encapsuladas en un único catálogo `ElectricalProperties`,
**para que** el dominio eléctrico tenga su propio módulo autocontenido.

#### Criterios de aceptación

- [ ] Existe la clase `Game.Engine.World.Physics.ElectricalProperties` en el paquete `Game.Engine.World.Physics`.
- [ ] `ElectricalProperties` contiene exactamente los siguientes descriptores, con los mismos valores de construcción que tenían en `CoreProperties`:
  - `CHARGE`
  - `ELECTRICAL_CONDUCTIVITY`
- [ ] `ElectricalProperties` tiene constructor privado y es `final`.
- [ ] `ElectricalProperties` reside en el paquete `Game.Engine.World.Physics`.

---

### REQ-003: Creación de FluidProperties

**Como** arquitecto del sistema,
**quiero** que todas las propiedades del dominio fluídico estén encapsuladas en un único catálogo `FluidProperties`,
**para que** el dominio fluídico tenga su propio módulo autocontenido.

#### Criterios de aceptación

- [ ] Existe la clase `Game.Engine.World.Physics.FluidProperties` en el paquete `Game.Engine.World.Physics`.
- [ ] `FluidProperties` contiene exactamente los siguientes descriptores, con los mismos valores de construcción que tenían en `CoreProperties`:
  - `HUMIDITY`
  - `HUMIDITY_ABSORPTION`
  - `VISCOSITY`
- [ ] `FluidProperties` tiene constructor privado y es `final`.
- [ ] `FluidProperties` reside en el paquete `Game.Engine.World.Physics`.

---

### REQ-004: Creación de MechanicalProperties

**Como** arquitecto del sistema,
**quiero** que todas las propiedades del dominio mecánico estén encapsuladas en un único catálogo `MechanicalProperties`,
**para que** el dominio mecánico tenga su propio módulo autocontenido.

#### Criterios de aceptación

- [ ] Existe la clase `Game.Engine.World.Physics.MechanicalProperties` en el paquete `Game.Engine.World.Physics`.
- [ ] `MechanicalProperties` contiene exactamente los siguientes descriptores, con los mismos valores de construcción que tenían en `CoreProperties`:
  - `PRESSURE`
  - `COMPRESSIBILITY`
  - `ELASTICITY`
  - `HARDNESS`
  - `DENSITY`
- [ ] `MechanicalProperties` tiene constructor privado y es `final`.
- [ ] `MechanicalProperties` reside en el paquete `Game.Engine.World.Physics`.

---

### REQ-005: Creación de ThermalRelations

**Como** arquitecto del sistema,
**quiero** que todas las relaciones del dominio térmico estén encapsuladas en `ThermalRelations`,
**para que** el catálogo de relaciones térmicas sea simétrico al catálogo `ThermalProperties`.

#### Criterios de aceptación

- [ ] Existe la clase `Game.Engine.World.Solver.ThermalRelations` en el paquete `Game.Engine.World.Solver`.
- [ ] `ThermalRelations` contiene exactamente las siguientes relaciones, con los mismos valores de construcción (name, relationType, participating, constraints, priority) que tenían en `CoreRelations`:
  - `THERMAL_CONDUCTION`
  - `THERMAL_AMBIENT_DISSIPATION`
  - `THERMAL_EXCESS_CORRECTION`
  - `VOLUMETRIC_EXPANSION`
  - `RADIATION_THERMAL_CONVERSION`
- [ ] `ThermalRelations` expone un método estático `all()` que retorna un array con todas sus relaciones.
- [ ] `ThermalRelations` tiene constructor privado y es `final`.
- [ ] Ninguna ecuación, prioridad ni restricción de ninguna relación es alterada respecto a su definición en `CoreRelations`.
- [ ] `ThermalRelations` referencia `ThermalProperties` y `MechanicalProperties` donde corresponda (nunca `CoreProperties`).

---

### REQ-006: Creación de ElectricalRelations

**Como** arquitecto del sistema,
**quiero** que todas las relaciones del dominio eléctrico estén encapsuladas en `ElectricalRelations`,
**para que** el catálogo de relaciones eléctricas sea simétrico al catálogo `ElectricalProperties`.

#### Criterios de aceptación

- [ ] Existe la clase `Game.Engine.World.Solver.ElectricalRelations` en el paquete `Game.Engine.World.Solver`.
- [ ] `ElectricalRelations` contiene exactamente las siguientes relaciones, con los mismos valores de construcción que tenían en `CoreRelations`:
  - `ELECTRICAL_TRANSFER`
  - `ELECTRICAL_DISSIPATION`
  - `ELECTRICAL_EXCESS_CORRECTION`
  - `JOULE_HEATING`
- [ ] `ElectricalRelations` expone un método estático `all()` que retorna un array con todas sus relaciones.
- [ ] `ElectricalRelations` tiene constructor privado y es `final`.
- [ ] Ninguna ecuación, prioridad ni restricción es alterada.
- [ ] `ElectricalRelations` referencia `ElectricalProperties`, `ThermalProperties` y `MechanicalProperties` donde corresponda (nunca `CoreProperties`).

---

### REQ-007: Creación de FluidRelations

**Como** arquitecto del sistema,
**quiero** que todas las relaciones del dominio fluídico estén encapsuladas en `FluidRelations`,
**para que** el catálogo de relaciones fluídicas sea simétrico al catálogo `FluidProperties`.

#### Criterios de aceptación

- [ ] Existe la clase `Game.Engine.World.Solver.FluidRelations` en el paquete `Game.Engine.World.Solver`.
- [ ] `FluidRelations` contiene exactamente las siguientes relaciones, con los mismos valores de construcción que tenían en `CoreRelations`:
  - `FLUID_DIFFUSION`
  - `FLUID_AMBIENT_DISSIPATION`
  - `FLUID_SATURATION_RELEASE`
- [ ] `FluidRelations` expone un método estático `all()` que retorna un array con todas sus relaciones.
- [ ] `FluidRelations` tiene constructor privado y es `final`.
- [ ] Ninguna ecuación, prioridad ni restricción es alterada.
- [ ] `FluidRelations` referencia `FluidProperties` donde corresponda (nunca `CoreProperties`).

---

### REQ-008: Eliminación de CoreProperties

**Como** arquitecto del sistema,
**quiero** que `CoreProperties` sea eliminado completamente del codebase,
**para que** el concepto "Core" desaparezca de la arquitectura del dominio físico.

#### Criterios de aceptación

- [ ] El archivo `Physics/CoreProperties.java` no existe en el proyecto.
- [ ] No existe ninguna clase con el nombre `CoreProperties` bajo `Game.Engine.World.Physics`.
- [ ] `CoreProperties` no ha sido reemplazado por una clase vacía, un wrapper, una fachada ni un delegador.
- [ ] Ningún archivo del proyecto contiene el import `Game.Engine.World.Physics.CoreProperties`.
- [ ] Ningún archivo del proyecto contiene la referencia `CoreProperties.` en código fuente activo.

---

### REQ-009: Eliminación de CoreRelations

**Como** arquitecto del sistema,
**quiero** que `CoreRelations` sea eliminado completamente del codebase,
**para que** el concepto "Core" desaparezca de la arquitectura del dominio físico.

#### Criterios de aceptación

- [ ] El archivo `Solver/CoreRelations.java` no existe en el proyecto.
- [ ] No existe ninguna clase con el nombre `CoreRelations` bajo `Game.Engine.World.Solver`.
- [ ] `CoreRelations` no ha sido reemplazado por una clase vacía, un wrapper, una fachada ni un delegador.
- [ ] Ningún archivo del proyecto contiene el import `Game.Engine.World.Solver.CoreRelations`.
- [ ] Ningún archivo del proyecto contiene la referencia `CoreRelations.` en código fuente activo.

---

### REQ-010: Actualización de los evaluadores del Solver

**Como** desarrollador del Solver,
**quiero** que los evaluadores referencien los catálogos de dominio correspondientes,
**para que** cada evaluador dependa únicamente del catálogo de su fenómeno.

#### Criterios de aceptación

- [ ] `FourierEvaluator` importa `ThermalProperties` (no `CoreProperties`).
- [ ] `OhmEvaluator` importa `ElectricalProperties` (no `CoreProperties`).
- [ ] `BernoulliEvaluator` importa `FluidProperties` (no `CoreProperties`).
- [ ] `FickEvaluator` importa `FluidProperties` (no `CoreProperties`).
- [ ] `PascalEvaluator` importa `ThermalProperties` y `MechanicalProperties` (no `CoreProperties`).
- [ ] `HookeEvaluator` importa `MechanicalProperties` (no `CoreProperties`).
- [ ] `JouleEvaluator` importa `ThermalProperties` (no `CoreProperties`).
- [ ] `RadiationThermalEvaluator` importa `ThermalProperties` (no `CoreProperties`).
- [ ] `ArchimedesEvaluator` importa `FluidProperties` (no `CoreProperties`).
- [ ] `StokesEvaluator` importa `FluidProperties` (no `CoreProperties`).
- [ ] Ninguno de los evaluadores anteriores contiene el import `Game.Engine.World.Physics.CoreProperties`.
- [ ] El comportamiento numérico de cada evaluador es idéntico al anterior al HRFC-025.

---

### REQ-011: Actualización de catálogos de relaciones ya existentes

**Como** desarrollador del Solver,
**quiero** que `ElectromagneticRelations`, `MaterialStateRelations` y `RadiationRelations` referencien los nuevos catálogos de dominio,
**para que** ningún catálogo de relaciones dependa de `CoreProperties`.

#### Criterios de aceptación

- [ ] `ElectromagneticRelations` referencia `ThermalProperties` y `ElectricalProperties` donde corresponda (no `CoreProperties`).
- [ ] `MaterialStateRelations` referencia `ThermalProperties`, `FluidProperties` y `ElectricalProperties` donde corresponda (no `CoreProperties`).
- [ ] `RadiationRelations` referencia `ThermalProperties` donde corresponda (no `CoreProperties`).
- [ ] Ninguno de estos archivos contiene el import `Game.Engine.World.Physics.CoreProperties`.
- [ ] Ninguna relación de estos archivos tiene su nombre, prioridad, restricciones o propiedades participantes alteradas.

---

### REQ-012: Actualización de MaterialComponent

**Como** desarrollador del sistema de componentes,
**quiero** que `MaterialComponent` referencie los nuevos catálogos de dominio,
**para que** el componente de material no dependa del concepto histórico "Core".

#### Criterios de aceptación

- [ ] `MaterialComponent.registerInto()` usa `ThermalProperties`, `ElectricalProperties`, `FluidProperties` y `MechanicalProperties` en lugar de `CoreProperties`.
- [ ] El método `registerInto()` registra exactamente las mismas propiedades (mismo `PropertyDescriptor` por identidad de id) que antes del HRFC-025.
- [ ] `MaterialComponent` no contiene el import `Game.Engine.World.Physics.CoreProperties`.

---

### REQ-013: Actualización de WorldSimulation

**Como** desarrollador del orquestador del mundo,
**quiero** que `WorldSimulation.withDefaults()` use los nuevos catálogos de dominio,
**para que** la composición del mundo no dependa de `CoreRelations`.

#### Criterios de aceptación

- [ ] `WorldSimulation.withDefaults()` registra todas las relaciones de `ThermalRelations.all()`, `ElectricalRelations.all()` y `FluidRelations.all()`.
- [ ] El conjunto total de relaciones registradas es idéntico al que producía `CoreRelations.all()` (mismas 12 relaciones).
- [ ] `WorldSimulation` no contiene el import `Game.Engine.World.Solver.CoreRelations`.
- [ ] El orden de registro no altera las prioridades de evaluación, ya que cada `PhysicalRelation` declara su propia prioridad.

---

### REQ-014: Actualización de WorldFieldPresets

**Como** desarrollador del módulo de Gameplay,
**quiero** que `WorldFieldPresets` referencie los nuevos catálogos de dominio,
**para que** ningún módulo del proyecto dependa del concepto histórico "Core".

#### Criterios de aceptación

- [ ] `WorldFieldPresets` importa los catálogos de dominio correspondientes a los descriptores que usa (no `CoreProperties`).
- [ ] `WorldFieldPresets` no contiene el import `Game.Engine.World.Physics.CoreProperties`.
- [ ] El comportamiento visible de `WorldFieldPresets` es idéntico al anterior.

---

### REQ-015: Invarianza de comportamiento del Solver

**Como** desarrollador del motor de simulación,
**quiero** que el comportamiento numérico del Solver sea idéntico antes y después del HRFC-025,
**para que** esta sea una refactorización pura sin efectos funcionales.

#### Criterios de aceptación

- [ ] Las prioridades de todas las relaciones son idénticas a las que tenían en `CoreRelations`.
- [ ] Las restricciones (`RelationConstraint`) de todas las relaciones son idénticas.
- [ ] Las propiedades participantes de todas las relaciones son idénticas en semántica (mismo string `id` en el `PropertyDescriptor`).
- [ ] Los valores defaultValue, min y max de todos los descriptores son idénticos a los que tenían en `CoreProperties`.
- [ ] Los archivos restringidos (`PhysicsSolver`, `PhysicsCoordinator`, `WorkingState`, `PhysicalStateFrame`, `StateRelationEvaluator`, `EvaluatorRegistry`) no han sido modificados salvo actualización de imports.

---

### REQ-016: Actualización del Javadoc de PropertyDescriptor

**Como** desarrollador que mantiene el sistema,
**quiero** que `PropertyDescriptor` actualice su lista de catálogos autorizados,
**para que** la documentación refleje exactamente la nueva organización.

#### Criterios de aceptación

- [ ] El Javadoc de `PropertyDescriptor` lista `ThermalProperties`, `ElectricalProperties`, `FluidProperties` y `MechanicalProperties` como catálogos autorizados.
- [ ] El Javadoc de `PropertyDescriptor` no menciona `CoreProperties` como catálogo autorizado.
- [ ] Únicamente se modifica el bloque de comentarios; el código de `PropertyDescriptor` permanece intacto.

---

## Restricciones absolutas

Los siguientes archivos **no deben ser modificados** salvo para actualizar imports cuando sea estrictamente necesario:

- `PhysicsSolver.java`
- `PhysicsCoordinator.java`
- `WorkingState.java`
- `PhysicalStateFrame.java`
- `StateRelationEvaluator.java`
- `EvaluatorRegistry.java`

No deben alterarse bajo ninguna circunstancia:
- Prioridades de evaluación
- Ecuaciones de los evaluadores
- Restricciones de las relaciones
- Comportamiento numérico del Solver
- Orden de evaluación

---

## Resumen de artefactos

### Archivos nuevos (7)

| Archivo | Paquete |
|---|---|
| `ThermalProperties.java` | `Game.Engine.World.Physics` |
| `ElectricalProperties.java` | `Game.Engine.World.Physics` |
| `FluidProperties.java` | `Game.Engine.World.Physics` |
| `MechanicalProperties.java` | `Game.Engine.World.Physics` |
| `ThermalRelations.java` | `Game.Engine.World.Solver` |
| `ElectricalRelations.java` | `Game.Engine.World.Solver` |
| `FluidRelations.java` | `Game.Engine.World.Solver` |

### Archivos a eliminar (2)

| Archivo |
|---|
| `CoreProperties.java` |
| `CoreRelations.java` |

### Archivos a actualizar (17)

| Archivo | Cambio requerido |
|---|---|
| `FourierEvaluator.java` | `CoreProperties` → `ThermalProperties` |
| `OhmEvaluator.java` | `CoreProperties` → `ElectricalProperties` |
| `BernoulliEvaluator.java` | `CoreProperties` → `FluidProperties` |
| `FickEvaluator.java` | `CoreProperties` → `FluidProperties` |
| `PascalEvaluator.java` | `CoreProperties` → `ThermalProperties` + `MechanicalProperties` |
| `HookeEvaluator.java` | `CoreProperties` → `MechanicalProperties` |
| `JouleEvaluator.java` | `CoreProperties` → `ThermalProperties` |
| `RadiationThermalEvaluator.java` | `CoreProperties` → `ThermalProperties` |
| `ArchimedesEvaluator.java` | `CoreProperties` → `FluidProperties` |
| `StokesEvaluator.java` | `CoreProperties` → `FluidProperties` |
| `ElectromagneticRelations.java` | `CoreProperties` → catálogos de dominio |
| `MaterialStateRelations.java` | `CoreProperties` → catálogos de dominio |
| `RadiationRelations.java` | `CoreProperties` → `ThermalProperties` |
| `MaterialComponent.java` | `CoreProperties` → catálogos de dominio |
| `WorldSimulation.java` | `CoreRelations` → `ThermalRelations` + `ElectricalRelations` + `FluidRelations` |
| `WorldFieldPresets.java` | `CoreProperties` → catálogos de dominio |
| `PropertyDescriptor.java` | Actualizar Javadoc únicamente |
