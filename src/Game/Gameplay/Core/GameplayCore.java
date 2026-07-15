package Game.Gameplay.Core;

/**
 * Punto de referencia del Gameplay Core de Infinity Hell.
 *
 * ── PROPÓSITO DE ESTA CLASE ───────────────────────────────────────────────
 * Esta clase no contiene lógica. Es el índice de navegación del módulo
 * Game.Gameplay.Core y el lugar donde se documenta el contrato de uso de
 * todos sus subsistemas.
 *
 * ── ESTRUCTURA DEL MÓDULO ─────────────────────────────────────────────────
 *
 *   Game.Gameplay.Core
 *   │
 *   ├── Tags/
 *   │   ├── GameplayTag           → interfaz: identidad de un tag
 *   │   ├── GameplayTagNode       → nodo jerárquico con padre
 *   │   ├── GameplayTags          → catálogo de tags del núcleo
 *   │   └── TagComponent          → componente: colección de tags en una entidad
 *   │
 *   ├── Properties/
 *   │   ├── PropertyKey<T>        → clave tipada de una propiedad
 *   │   ├── PropertyKeys          → catálogo de propiedades del núcleo
 *   │   ├── PropertyMap           → almacén de valores base
 *   │   └── PropertyComponent     → componente: propiedades de una entidad
 *   │
 *   ├── Capabilities/
 *   │   ├── GameplayCapability    → interfaz marcadora de capacidad
 *   │   ├── CoreCapabilities      → catálogo de capacidades del núcleo
 *   │   └── CapabilityComponent   → componente: capacidades de una entidad
 *   │
 *   ├── Events/
 *   │   ├── GameplayEvent         → interfaz: evento interceptable
 *   │   ├── AbstractGameplayEvent → base con cancel() implementado
 *   │   ├── GameplayEventChannel  → canal: dispara y permite interceptación
 *   │   └── CoreGameplayEvents    → catálogo de eventos del núcleo
 *   │
 *   ├── Modifiers/
 *   │   ├── PropertyModifier      → operación add/mul/override + causalidad (CFCC-002)
 *   │   ├── ModifierContainer     → colección de modificadores activos
 *   │   └── ModifierComponent     → componente: modificadores de una entidad
 *   │
 *   ├── Resolution/
 *   │   ├── PropertyResolver      → pipeline unificado CFCC-001+002+002A
 *   │   └── ResolutionContext     → agregador entidad+mapa+contenedor
 *   │
 *   └── Causality/
 *       │
 *       │  ── CFCC-002: Descripción de modificaciones ──
 *       ├── ModifierSource        → ¿de dónde proviene el modificador? (interfaz jerárquica)
 *       ├── ModifierScope         → ¿sobre qué tipo de objetivo actúa? (interfaz jerárquica)
 *       ├── ModifierPredicate     → ¿cuándo aplica? (condición lógica composable, @FunctionalInterface)
 *       ├── ModifierContext       → contexto rico de resolución (inmutable + Builder)
 *       ├── ModifierInfluence     → transforma un modificador antes del pipeline (@FunctionalInterface)
 *       ├── ModifierChain         → trayectoria causal ligera (árbol de strings, debug rápido)
 *       │
 *       │  ── CFCC-002A: Infraestructura de causalidad completa ──
 *       ├── CausalNode            → nodo causal de alta fidelidad:
 *       │                           grafo dirigido multi-padre, PropertyModifier,
 *       │                           PropertyKey, valueBefore/After, ModifierContext,
 *       │                           timestamp, detección de ciclos, fullCausalHistory()
 *       └── InfluenceRegistry     → registro de influencias EXTERNAS con prioridad:
 *                                   cualquier sistema registra una ModifierInfluence
 *                                   que actúa sobre los modificadores de cualquier entidad
 *                                   sin acoplamiento directo
 *
 * ── VOCABULARIO DE DOMINIO ────────────────────────────────────────────────
 *
 *   Tag              → "¿qué ES la entidad?"
 *   Property         → "¿qué MIDE la entidad?"
 *   Capability       → "¿qué PUEDE la entidad?"
 *   Event            → "¿qué OCURRE con la entidad?"
 *   Modifier         → "¿cómo se ALTERA una propiedad?"
 *   Resolution       → "¿cuál es el valor FINAL?"
 *   Source           → "¿de dónde viene la modificación?"
 *   Scope            → "¿a qué tipo de objetivo aplica?"
 *   Predicate        → "¿cuándo aplica la modificación?"
 *   Context          → "¿qué información está disponible al modificar?"
 *   Influence        → "¿cómo puede ser transformado el modificador?"
 *   Chain            → "trayectoria causal ligera (árbol de nombres)"
 *   CausalNode       → "hecho causal de alta fidelidad (grafo dirigido)"
 *   InfluenceRegistry → "registro de influencias externas ordenadas por prioridad"
 *
 * ── PIPELINE DE RESOLUCIÓN COMPLETO ──────────────────────────────────────
 *
 *   Valor Base
 *       ↓
 *   [CFCC-002A] InfluenceRegistry   — influencias EXTERNAS: amplifican, reducen, cancelan
 *       ↓
 *   [CFCC-002]  Predicate           — condición de activación del modificador
 *       ↓
 *   [CFCC-002]  Influence propia    — transformación declarada por el modificador
 *       ↓
 *   [CFCC-001]  ADDITIVE            — base + Σ(aditivos)
 *       ↓
 *   [CFCC-001]  MULTIPLICATIVE      — resultado × Π(multiplicativos)
 *       ↓
 *   [CFCC-001]  OVERRIDE            — sustituye si existe override
 *       ↓
 *   [CFCC-001]  CLAMP               — clamp al rango [min, max]
 *       ↓
 *   [CFCC-002A] CausalNode          — registro del hecho (solo si hay collector)
 *       ↓
 *   Valor Final
 *
 * ── RELACIÓN ENTRE ModifierChain Y CausalNode ────────────────────────────
 *
 *   ModifierChain  → árbol de strings identificadores.
 *                    Ligero, sin overhead de objetos. Útil para debug rápido
 *                    y para sistemas que solo necesitan saber la trayectoria
 *                    conceptual ("Spell → Projectile → Explosion").
 *
 *   CausalNode     → grafo dirigido de hechos concretos.
 *                    Almacena: el PropertyModifier exacto, la propiedad,
 *                    los valores antes/después, el ModifierContext del momento,
 *                    el timestamp, y enlaces a múltiples padres/hijos.
 *                    Permite convergencia causal (un efecto con múltiples causas).
 *                    Permite responder: "¿qué modificó este daño?"
 *                                      "¿qué cadena produjo este evento?"
 *
 *   Ambos coexisten. ModifierChain sigue siendo el campo en PropertyModifier
 *   para trayectorias ligeras. CausalNode es la capa de diagnóstico y
 *   reconstrucción completa del estado.
 *
 * ── COMPATIBILIDAD ────────────────────────────────────────────────────────
 * Todo el código de CFCC-001 y CFCC-002 sigue compilando y funcionando sin
 * cambios. Los nuevos parámetros de PropertyResolver (registry, causalLog)
 * son opcionales: pasarlos null es idéntico a no usarlos.
 *
 * ── EJEMPLO CFCC-001 ─────────────────────────────────────────────────────
 *
 *   // Sistema de veneno que daña a entidades orgánicas
 *   void applyPoison(GameObjects target, GameObjects source) {
 *       TagComponent tags = target.getComponent(TagComponent.class);
 *       if (tags == null || !tags.hasTagOrAncestor(GameplayTags.ORGANIC)) return;
 *       CapabilityComponent caps = target.getComponent(CapabilityComponent.class);
 *       if (caps == null || !caps.has(CoreCapabilities.CAN_RECEIVE_MODIFIERS)) return;
 *
 *       ResolutionContext ctx = ResolutionContext.of(source);
 *       double poisonDamage = ctx.resolvePositive(PropertyKeys.DAMAGE) * 0.15;
 *
 *       CoreGameplayEvents.OnDamage event = new CoreGameplayEvents.OnDamage(
 *           source, target, poisonDamage);
 *       combatChannel.fire(event);
 *
 *       if (!event.isCancelled()) {
 *           target.damage((int) event.getDamage());
 *           tags.add(GameplayTags.STATUS_POISONED);
 *       }
 *   }
 *
 * ── EJEMPLO CFCC-002A: InfluenceRegistry + CausalNode ────────────────────
 *
 *   // Amplificar todos los modificadores del jugador durante un buff de hechizo
 *   InfluenceRegistry playerRegistry = new InfluenceRegistry();
 *   playerRegistry.register(100, "fire_amplifier",
 *       (mod, ctx) -> {
 *           // Solo amplifican los modificadores aditivos de fuego
 *           if (mod.getSource() == null) return mod;
 *           if (!mod.getSource().isOrDescendantOf(SpellSources.FIRE)) return mod;
 *           if (mod.getPhase() != PropertyModifier.Phase.ADDITIVE) return mod;
 *           return PropertyModifier.additive(
 *               mod.getKey(),
 *               mod.getValue() * 1.4,       // +40%
 *               mod.getSourceId() + "+fire_amp"
 *           ).withCausalityFrom(mod);
 *       }
 *   );
 *
 *   // Resolver con trazabilidad completa
 *   List<CausalNode> causalLog = new ArrayList<>();
 *   ModifierContext ctx = ModifierContext.builder()
 *       .source(attacker).target(defender)
 *       .property(PropertyKeys.DAMAGE)
 *       .baseValue(base).currentValue(base)
 *       .tags(defender.getComponent(TagComponent.class))
 *       .timestamp(currentFrame)
 *       .build();
 *
 *   double finalDamage = PropertyResolver.resolveWithCausalLog(
 *       defenderProps.getMap(), PropertyKeys.DAMAGE,
 *       ModifierComponent.containerOf(defender),
 *       ctx, playerRegistry, causalLog
 *   );
 *
 *   // causalLog contiene un CausalNode por cada modificador que actuó.
 *   // Vincular al grafo global:
 *   for (CausalNode node : causalLog) {
 *       spellCausalNode.addChild(node);   // spellCausalNode es el origen conocido
 *   }
 *
 * ── PRINCIPIOS DE USO ────────────────────────────────────────────────────
 *
 * 1. NO usar instanceof para decidir si una interacción aplica.
 *    Usar Tags y Capabilities.
 *
 * 2. NO modificar propiedades directamente en campos de las clases.
 *    Usar PropertyModifier + PropertyResolver.
 *
 * 3. NO disparar efectos sin GameplayEventChannel.
 *
 * 4. Extender sin modificar: nuevos Tags, Properties, Capabilities,
 *    Events, Sources, Scopes en nuevos catálogos.
 *
 * 5. Usar ModifierPredicate para condicionalidad. No añadir if en sistemas.
 *
 * 6. Usar InfluenceRegistry para influencias externas entre sistemas.
 *    No modificar los PropertyModifier de otras entidades directamente.
 *
 * 7. Usar CausalNode cuando se necesita trazabilidad completa del frame.
 *    Usar ModifierChain para trayectorias ligeras en el game loop.
 */
public final class GameplayCore {
    private GameplayCore() {}
}
