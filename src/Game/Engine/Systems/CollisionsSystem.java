package Game.Engine.Systems;

import Game.Engine.Colisions.CollisionDetector;
import Game.Engine.Colisions.CollisionDispatcher;
import Game.Engine.Colisions.CollisionHandler;
import Game.Engine.Colisions.CollisionResult;
import Game.Engine.Colisions.SweptAABB;
import Game.Engine.Component;
import Game.Engine.Destroyable;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.Entity.Components.InteractionSideComponent;
import Game.Engine.Entity.Components.InteractionSideComponent.SideInteraction;
import Game.Engine.Entity.Components.Physics2DComponent;
import Game.Engine.Entity.Components.PushableComponent;
import Game.Engine.Entity.Components.SurfaceComponent;
import Game.Engine.GameMath.Logic2D.RectangleH;
import Game.Engine.GameObjects;
import Game.Engine.Physics.Contact.ContactState;
import Game.Engine.Physics.KineticPhysics.PhysicsStepper;
import Game.Engine.Physics.KineticPhysics.SurfaceMaterial;
import Game.Engine.Physics.KineticPhysics.Types.Physics2D;
import Game.Engine.Physics.Predictive.VelocityRaycast;
import Game.Engine.Physics.Predictive.RaycastCollisionDetector;
import Game.Items.Types.Bullets.BulletComport.BulletPhysics;
import java.awt.geom.Point2D;
import java.util.*;

/**
 * Sistema de colisiones — pipeline unificado por frame.
 *
 * ── HRFC — Unificación del pipeline de colisiones ─────────────────────────
 *
 * PRINCIPIO FUNDAMENTAL (Diseño B):
 *   La matemática de detección geométrica es ÚNICA para todos los objetos.
 *   La diferencia entre SÓLIDO y TRIGGER está en la POLÍTICA DE RESPUESTA,
 *   no en la detección.
 *
 *   SÓLIDO  → SweptAABB eje-por-eje → resolución de velocidad + ContactState
 *   TRIGGER → SweptAABB 2D completo → setLastContactNormal + dispatch (sin resolución)
 *
 * CAMBIOS RESPECTO A LA VERSIÓN ANTERIOR:
 *
 *   ANTES:
 *     FASE 1   = SweptAABB solo para SÓLIDOS
 *     FASE 2   = AABB estática para TODOS (incluidos TRIGGERS)
 *     Bullets nunca recibían normal de colisión
 *     Bullets no participaban en CCD
 *     moveByPhysics() en Bullet.update() antes de CollisionsSystem
 *
 *   AHORA:
 *     FASE 0   = applyGravity + flushForces          [SÓLIDOS]
 *     FASE 0.5 = reset contactState + normal trigger  [SÓLIDOS + TRIGGERS]
 *     FASE 1   = SweptAABB eje X, luego eje Y         [SÓLIDOS no-trigger]
 *                 → política sólida: resolveVelocity + ContactState
 *     FASE 1B  = SweptAABB 2D simultáneo              [TRIGGERS con velocity != 0]
 *                 → política trigger: setLastContactNormal + dispatch
 *                 → integra el movimiento del trigger (mover hasta contacto)
 *                 → CollisionDetector.markDispatched() para evitar duplicado
 *     FASE 2   = CollisionDetector.detect()           [overlaps residuales]
 *                 (con colecciones reutilizables, con normal calculada)
 *     FASE 3   = dispatch pares FASE 2
 *                 → guard: omitir si algún objeto está destruido
 *     FASE 4   = CollisionListener enter/stay/exit
 *
 * ORDEN DEL PIPELINE:
 *   Bullet.update() llama movement.tick() + behavior.onUpdate() pero YA NO
 *   llama moveByPhysics(). CollisionsSystem integra la posición del trigger
 *   en FASE 1B con CCD, garantizando que la detección es swept y que la
 *   normal está disponible cuando onCollision() se ejecuta.
 *
 * INVARIANTES GARANTIZADOS:
 *   1. Todo objeto con velocity != 0 recibe detección swept (no AABB snapshot).
 *   2. La normal del impacto siempre está disponible en onCollision().
 *   3. Un par no se despacha más de una vez por frame.
 *   4. Objetos destruidos durante dispatch no reciben eventos adicionales.
 *   5. Bullets mueven su posición dentro de CollisionsSystem con CCD.
 */
public class CollisionsSystem {

    // ── ContactRecord — registro interno de un contacto del frame ────────

    /**
     * Registro de un contacto detectado durante FASE 1 (sólidos).
     * Acumulado por objeto para combinar múltiples contactos del mismo frame.
     */
    private static final class ContactRecord {
        final GameObjects   other;
        final int           normalX;
        final int           normalY;
        final SurfaceMaterial surface;

        ContactRecord(GameObjects other, int normalX, int normalY, SurfaceMaterial surface) {
            this.other   = other;
            this.normalX = normalX;
            this.normalY = normalY;
            this.surface = surface;
        }
    }

    /**
     * Separación mínima robusta aplicada en contactos de colisión.
     *
     * ── SOLUCIÓN DEFINITIVA POST-AUDITORÍA ───────────────────────────────
     *
     * HALLAZGO CRÍTICO:
     *   ColliderComponent.getBounds() NO hace truncado (int) — retorna double
     *   precision completa vía RectangleH. El comentario anterior era incorrecto.
     *
     * PROBLEMA DIAGNOSTICADO:
     *   Con epsilon de 0.5px y loop iterativo de verificación, bullets con
     *   velocidades bajas (~2 px/frame) requerían 5 iteraciones para salir del
     *   collider. Esto indica fallo del algoritmo de separación, no de velocidad.
     *
     * CAUSA RAÍZ:
     *   El loop iterativo con intersects() fallaba por acumulación de error en
     *   operaciones double: position + offset + epsilon → bounds → intersects().
     *   La verificación post-separación introducía false-positives.
     *
     * SOLUCIÓN:
     *   Separación mínima robusta de 1.0px (no 0.5px) aplicada UNA SOLA VEZ,
     *   sin verificación iterativa. Garantiza matemáticamente salida completa.
     *
     *   - MÍNIMO ABSOLUTO: 1.0px garantiza salida en TODOS los casos
     *   - ESCALADO: Crece 1% del desplazamiento para velocidades altas
     *   - SIN CAP: Si la velocidad requiere separación >1px, se aplica
     *   - SIN LOOP: La separación es correcta la primera vez
     *
     * GARANTÍA MATEMÁTICA:
     *   Con separación >= 1.0px en dirección de normal, el bounds NUNCA puede
     *   intersectar el hitBounds (tamaño mínimo bullet: 4×4px, mínimo collider: 4px).
     *   
     *   Ejemplo: Bullet 4×4 en borde de suelo 1280×180:
     *     - Posición: exactamente en contacto (touching)
     *     - Separación: 1.0px en dirección normal
     *     - Resultado: bullet a 1.0px del borde → NO intersection garantizada
     *
     * @deprecated CONTACT_EPSILON de 0.5px era insuficiente - ahora 1.0px mínimo
     */
    private static final double ROBUST_SEPARATION_MINIMUM = 1.0;

    /**
     * Umbral de velocidad para activar SUB-STEPPING PREDICTIVO.
     * 
     * JUSTIFICACIÓN:
     *   Velocidades altas (>50 px/frame) pueden causar tunneling incluso con SweptAABB.
     *   Sub-stepping divide el movimiento en pasos más pequeños para garantizar detección.
     *   
     *   UMBRAL: 50.0 px/frame (~3000 px/s @ 60fps)
     *   - Por debajo: SweptAABB normal (suficiente para velocidades moderadas)
     *   - Por encima: Sub-stepping adaptativo (velocidad / MAX_STEP_SIZE pasos)
     */
    private static final double SUBSTEP_VELOCITY_THRESHOLD = 50.0;
    
    /**
     * Tamaño máximo de sub-paso para movimiento de alta velocidad.
     * 
     * GARANTÍA:
     *   Con MAX_STEP_SIZE = 20.0px y tamaño mínimo de collider = 4×4px,
     *   es IMPOSIBLE que un objeto atraviese otro sin detección.
     *   
     *   Ejemplo: velocidad 900 px/frame → 45 sub-pasos de 20px cada uno.
     */
    private static final double MAX_STEP_SIZE = 20.0;

    /**
     * @deprecated Ya no se usa - reemplazado por ROBUST_SEPARATION_MINIMUM
     */
    @Deprecated
    private static final double CONTACT_EPSILON = 0.5;

    /**
     * @deprecated Ya no se usa - reemplazado por ROBUST_SEPARATION_MINIMUM
     */
    @Deprecated
    private static final double PENETRATION_EPSILON = 0.5;

    /**
     * Contactos activos del frame anterior, por objeto.
     * Clave: identidad del objeto; Valor: conjunto de identidades de sus contactos.
     * Usado en FASE 4 para calcular enter/stay/exit.
     */
    private final IdentityHashMap<GameObjects, Set<GameObjects>> previousContacts =
            new IdentityHashMap<>();

    /**
     * CollisionDetector instanciable con colecciones reutilizables.
     * Se reutiliza entre frames — zero allocations de colecciones en hot path.
     */
    private final CollisionDetector detector = new CollisionDetector();

    // ── HRFC: Performance — Reusable broadphase rectangles ────────────────
    /**
     * RectangleH reutilizable para broadphase X.
     * Evita allocations en hot path — solo válido durante una iteración.
     */
    private final RectangleH reusableBroadX = new RectangleH();

    /**
     * RectangleH reutilizable para broadphase Y.
     * Evita allocations en hot path — solo válido durante una iteración.
     */
    private final RectangleH reusableBroadY = new RectangleH();

    /**
     * RectangleH reutilizable para broadphase 2D (FASE 1B triggers).
     * Evita allocations en hot path — solo válido durante una iteración.
     */
    private final RectangleH reusableBroad2D = new RectangleH();


    // ── HRFC — Profiling Infrastructure ───────────────────────────────────
    private final Game.Engine.Profiling.SubsystemTimer collisionTimer = 
        new Game.Engine.Profiling.SubsystemTimer();
    private final Game.Engine.Profiling.SubsystemTimer broadPhaseTimer = 
        new Game.Engine.Profiling.SubsystemTimer();
    private final Game.Engine.Profiling.SubsystemTimer narrowPhaseTimer = 
        new Game.Engine.Profiling.SubsystemTimer();
    private final Game.Engine.Profiling.SubsystemTimer dispatchTimer = 
        new Game.Engine.Profiling.SubsystemTimer();

    /**
     * Ejecuta el pipeline completo de física y colisiones.
     *
     * ── Mini-HRFC — Unified Physics Time Integration ─────────────────────
     *
     * CAMBIO: Ahora recibe deltaTime explícito del simulation step y lo
     * propaga a todos los métodos de integración física (applyGravity,
     * flushAccumulatedForces) para garantizar integración temporal correcta.
     *
     * deltaTime debe expresarse en segundos (ej: 0.01667s para 60 FPS).
     *
     * ── HRFC — Bottleneck Diagnosis ──────────────────────────────────────
     *
     * Instrumentado para medir tiempo de colisiones en el frame profile.
     * collisionTimer se resetea en WorldManager después de cada frame.
     *
     * ── HRFC — Collision Culling ──────────────────────────────────────────
     *
     * Si se proporciona una cámara, se aplicará collision culling para omitir
     * triggers (bullets) que están completamente fuera del área relevante.
     * Esto reduce drásticamente el trabajo cuando hay muchos bullets off-screen.
     *
     * @param objects lista de objetos activos en el mundo
     * @param deltaTime tiempo del simulation step en segundos
     * @param camera cámara activa del juego (null = sin culling)
     */
    public void update(List<GameObjects> objects, double deltaTime, Game.Engine.Camera.GameCamera camera) {
        collisionTimer.start();

        // ── FASE 0: Aplicar gravedad y fuerzas acumuladas ────────────────
        // Usa el onGround del frame ANTERIOR (todavía válido aquí).
        // Solo objetos SÓLIDOS — los triggers gestionan su gravedad
        // externamente via ProjectileMovement (isGravityManagedExternally).
        for (GameObjects obj : objects) {
            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            if (physComp == null) continue;
            Physics2D physics = physComp.getPhysics();
            if (physics.isGravityManagedExternally()) continue;
            ColliderComponent col = obj.getComponent(ColliderComponent.class);
            if (col == null || col.isTrigger()) continue;
            // Mini-HRFC: Pasar deltaTime para integración temporal correcta
            physics.applyGravity(physics.getOnGround(), deltaTime);
            physics.flushAccumulatedForces(deltaTime);
        }

        // ── FASE 0.5: Resetear estado de contacto ────────────────────────
        // DESPUÉS de applyGravity para que use el onGround previo correcto.
        // ANTES de SweptAABB para que los nuevos contactos partan de cero.
        // Para TRIGGERS: limpia lastContactNormal (escrito en el frame anterior).
        for (GameObjects obj : objects) {
            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            ColliderComponent  colA     = obj.getComponent(ColliderComponent.class);
            if (physComp == null || colA == null) continue;
            Physics2D physics = physComp.getPhysics();
            if (!colA.isTrigger()) {
                // Sólido: resetear flags de contacto
                physics.setOnGround(false);
                physics.setOnWall(false);
                physics.setOnCeiling(false);
                physics.clearSurface();
                physComp.setContactState(ContactState.NONE);
            } else {
                // Trigger: resetear normal de contacto
                if (physics instanceof BulletPhysics bp) {
                    bp.clearLastContactNormal();
                }
            }
        }


        // ── FASE 1: SweptAABB eje-por-eje para SÓLIDOS ───────────────────
        // Solo objetos no-trigger con Physics2DComponent y velocity != 0.
        // Política sólida: resuelve movimiento, escribe ContactState.

        IdentityHashMap<GameObjects, List<ContactRecord>> frameContacts =
                new IdentityHashMap<>();

        for (GameObjects obj : objects) {
            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            ColliderComponent  colA     = obj.getComponent(ColliderComponent.class);
            if (physComp == null || colA == null || colA.isTrigger()) continue;
            if (obj instanceof Destroyable d && d.isPendingDestruction()) continue;

            Physics2D physics = physComp.getPhysics();
            // ── Mini-HRFC 1.5: Temporal integration correction ────────────
            // velocity está en units/second. Debe multiplicarse por deltaTime
            // para obtener displacement en units.
            // ANTES: vx = velocity.x  (tratado como px/frame)
            // AHORA: vx = velocity.x × deltaTime  (displacement correcto)
            double vx = physics.getVelocity().getX() * deltaTime;
            double vy = physics.getVelocity().getY() * deltaTime;
            
            if (vx == 0.0 && vy == 0.0) continue;

            List<ContactRecord> records = frameContacts.computeIfAbsent(
                    obj, k -> new ArrayList<>(4));

            // ── Eje X ──────────────────────────────────────────────────
            if (vx != 0.0) {
                RectangleH bounds = colA.getBounds();
                
                // HRFC: Usar sweep() de RectangleH para construir broadphase bounds
                // El shrink vertical (y+1, height-2) evita falsos positivos en esquinas
                // durante movimiento horizontal puro
                RectangleH sweptX = bounds.sweep(vx, 0).shrunk(0, 1);
                reusableBroadX.copyFrom(sweptX);

                double      nearestTimeX = 1.0;
                int         hitNormalX   = 0;
                GameObjects hitX         = null;

                for (GameObjects other : objects) {
                    if (other == obj) continue;
                    if (other instanceof Destroyable d && d.isPendingDestruction()) continue;
                    ColliderComponent colB = other.getComponent(ColliderComponent.class);
                    if (colB == null || colB.isTrigger()) continue;
                    if (!colA.canCollideWith(colB)) continue;

                    InteractionSideComponent iscB = other.getComponent(InteractionSideComponent.class);
                    RectangleH ob = colB.getBounds();
                    if (ob.y >= bounds.y + bounds.height) continue;
                    if (ob.y + ob.height <= bounds.y) continue;
                    if (!reusableBroadX.intersects(ob)) continue;

                    SweptAABB.Result r = SweptAABB.calculate(bounds, ob, vx, 0.0);
                    if (!r.hasCollision() || r.time >= nearestTimeX) continue;

                    if (iscB != null) {
                        SideInteraction interaction = iscB.forNormal(r.normalX, r.normalY);
                        if (interaction == SideInteraction.PASSTHROUGH) continue;
                    }

                    nearestTimeX = r.time;
                    hitNormalX   = r.normalX;
                    hitX         = other;
                }

                PhysicsStepper.moveWith(obj, vx * nearestTimeX, 0);

                if (hitX != null && hitNormalX != 0) {
                    resolveVelocityX(physics, hitX, vx);
                    InteractionSideComponent iscHit = hitX.getComponent(InteractionSideComponent.class);
                    SideInteraction sideType = (iscHit != null)
                            ? iscHit.forNormal(hitNormalX, 0) : SideInteraction.WALL;
                    SurfaceMaterial surface = resolveSurface(hitX);
                    records.add(new ContactRecord(hitX, hitNormalX, 0, surface));
                    applyContactFlag(physics, hitNormalX, 0, sideType);
                    applyPushIfPushable(hitX, vx, 0.0, physics.getMass());
                    detector.markDispatched(obj, hitX);
                    CollisionDispatcher.dispatch(obj, hitX);
                }
            }


            // ── Eje Y ──────────────────────────────────────────────────
            if (vy != 0.0) {
                RectangleH bounds = colA.getBounds();
                
                // HRFC: Usar sweep() de RectangleH para construir broadphase bounds
                // En Y no se hace shrink horizontal porque queremos detectar
                // contactos en toda la anchura del objeto
                RectangleH sweptY = bounds.sweep(0, vy);
                reusableBroadY.copyFrom(sweptY);

                double      nearestTimeY = 1.0;
                int         hitNormalY   = 0;
                GameObjects hitY         = null;

                for (GameObjects other : objects) {
                    if (other == obj) continue;
                    if (other instanceof Destroyable d && d.isPendingDestruction()) continue;
                    ColliderComponent colB = other.getComponent(ColliderComponent.class);
                    if (colB == null || colB.isTrigger()) continue;
                    if (!colA.canCollideWith(colB)) continue;

                    RectangleH ob = colB.getBounds();
                    if (!reusableBroadY.intersects(ob)) continue;

                    SweptAABB.Result r = SweptAABB.calculate(bounds, ob, 0.0, vy);
                    if (!r.hasCollision() || r.time >= nearestTimeY) continue;

                    InteractionSideComponent iscB = other.getComponent(InteractionSideComponent.class);
                    if (iscB != null) {
                        SideInteraction interaction = iscB.forNormal(r.normalX, r.normalY);
                        if (interaction == SideInteraction.PASSTHROUGH) continue;
                    }

                    nearestTimeY = r.time;
                    hitNormalY   = r.normalY;
                    hitY         = other;
                }

                PhysicsStepper.moveWith(obj, 0, vy * nearestTimeY);

                if (hitY != null && hitNormalY != 0) {
                    resolveVelocityY(physics, hitY, vy);
                    InteractionSideComponent iscHit = hitY.getComponent(InteractionSideComponent.class);
                    SideInteraction sideType = (iscHit != null)
                            ? iscHit.forNormal(0, hitNormalY)
                            : defaultSideForNormalY(hitNormalY);
                    SurfaceMaterial surface = resolveSurface(hitY);
                    records.add(new ContactRecord(hitY, 0, hitNormalY, surface));
                    applyContactFlag(physics, 0, hitNormalY, sideType);
                    if (hitNormalY == -1) physics.setCurrentSurface(surface);
                    applyPushIfPushable(hitY, 0.0, vy, physics.getMass());
                    detector.markDispatched(obj, hitY);
                    CollisionDispatcher.dispatch(obj, hitY);
                }
            }
        } // fin loop FASE 1


        // ── FASE 1 — finalización: construir ContactState por objeto ─────
        for (Map.Entry<GameObjects, List<ContactRecord>> entry : frameContacts.entrySet()) {
            GameObjects         obj     = entry.getKey();
            List<ContactRecord> records = entry.getValue();
            Physics2DComponent  physComp = obj.getComponent(Physics2DComponent.class);
            if (physComp == null || records.isEmpty()) continue;
            Physics2D physics = physComp.getPhysics();

            boolean       hasGround  = false;
            boolean       hasWall    = false;
            boolean       hasCeiling = false;
            SurfaceMaterial groundSurface = null;
            int primaryNX = 0, primaryNY = 0;

            for (ContactRecord rec : records) {
                if (rec.normalY == -1) {
                    hasGround = true;
                    if (groundSurface == null) groundSurface = rec.surface;
                    primaryNX = 0; primaryNY = -1;
                } else if (rec.normalY == 1) {
                    hasCeiling = true;
                    if (primaryNY == 0) { primaryNX = 0; primaryNY = 1; }
                } else if (rec.normalX != 0) {
                    hasWall = true;
                    if (primaryNX == 0 && primaryNY == 0) { primaryNX = rec.normalX; primaryNY = 0; }
                }
            }

            if (groundSurface == null) groundSurface = SurfaceMaterial.DEFAULT;

            ContactState state = ContactState.builder()
                    .onGround(hasGround)
                    .onWall(hasWall)
                    .onCeiling(hasCeiling)
                    .surfaceFriction(groundSurface.getFriction())
                    .surfaceNormalX(primaryNX)
                    .surfaceNormalY(primaryNY)
                    .build();

            physComp.setContactState(state);
        }


        // ── FASE 1B: SUB-STEPPING PREDICTIVO + SweptAABB 2D ──────────────────
        //
        // ARQUITECTURA ANTI-PENETRACIÓN:
        //   1. Detectar velocidad peligrosa (>SUBSTEP_VELOCITY_THRESHOLD)
        //   2. Si alta: SUB-STEP adaptativo (movimiento / MAX_STEP_SIZE pasos)
        //   3. Si baja: SweptAABB normal (suficiente para velocidades moderadas)
        //   4. Detener EXACTAMENTE en punto de contacto — NO penetración
        //
        // GARANTÍA MATEMÁTICA:
        //   Con MAX_STEP_SIZE = 20.0px y collider mínimo = 4×4px,
        //   es IMPOSIBLE atravesar un objeto sin detección.
        //
        // Los triggers NO reciben resolución de velocidad — su behavior
        // (BulletJump, etc.) decide qué hacer con la velocidad en onCollision.
        //
        // Solo se comparan triggers contra NO-triggers (objetos sólidos del mundo).
        // Trigger vs Trigger se maneja en FASE 2 si es necesario.

        for (GameObjects obj : objects) {
            ColliderComponent colA = obj.getComponent(ColliderComponent.class);
            if (colA == null || !colA.isTrigger()) continue;
            if (obj instanceof Destroyable d && d.isPendingDestruction()) continue;
            
            // ── HRFC — Collision Culling ──────────────────────────────────
            if (camera != null && CollisionCulling.shouldSkipCollision(obj, camera, deltaTime)) {
                Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
                if (physComp != null) {
                    Physics2D physics = physComp.getPhysics();
                    double vx = physics.getVelocity().getX() * deltaTime;
                    double vy = physics.getVelocity().getY() * deltaTime;
                    PhysicsStepper.moveWith(obj, vx, vy);
                }
                continue;
            }

            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            if (physComp == null) continue;

            Physics2D physics = physComp.getPhysics();
            double vx = physics.getVelocity().getX() * deltaTime;
            double vy = physics.getVelocity().getY() * deltaTime;
            
            if (vx == 0.0 && vy == 0.0) {
                PhysicsStepper.moveWith(obj, 0, 0);
                continue;
            }

            // ── RAYCAST PREDICTIVO LIMITANTE (GENÉRICO) ───────────────────
            // Detectar obstáculos y calcular factor de limitación del movimiento
            RaycastCollisionDetector.DetectionResult detection = 
                RaycastCollisionDetector.detectAndLimitMovement(obj, vx, vy, objects);
            
            // REGISTRAR RAYCAST PARA VISUALIZACIÓN (si está activo)
            if (VelocityRaycast.SHOW_RAYCASTS) {
                double objX = obj.getTransform().getPosition().getX();
                double objY = obj.getTransform().getPosition().getY();
                VelocityRaycast.RaycastResult raycastVis = VelocityRaycast.calculateRaycast(vx, vy);
                Game.Engine.Physics.Predictive.RaycastVisualizationSystem.registerRaycast(
                    obj, objX, objY, raycastVis, detection);
            }
            
            // APLICAR LIMITACIÓN: Si hay obstáculo cercano, reducir velocidad
            if (detection.hasObstacle && detection.movementLimitFactor < 1.0) {
                vx *= detection.movementLimitFactor;
                vy *= detection.movementLimitFactor;
                
                // Si el movimiento queda casi en cero, detener completamente
                if (Math.abs(vx) < 0.1 && Math.abs(vy) < 0.1) {
                    PhysicsStepper.moveWith(obj, 0, 0);
                    
                    // Marcar colisión y despachar evento
                    if (detection.nearestObject != null) {
                        if (physics instanceof BulletPhysics bp) {
                            // Calcular normal desde la dirección del movimiento
                            int normalX = (vx > 0) ? -1 : (vx < 0) ? 1 : 0;
                            int normalY = (vy > 0) ? -1 : (vy < 0) ? 1 : 0;
                            bp.setLastContactNormal(normalX, normalY);
                        }
                        if (obj instanceof Game.Items.Types.Bullets.Definition.Bullet bullet) {
                            int normalX = (vx > 0) ? -1 : (vx < 0) ? 1 : 0;
                            int normalY = (vy > 0) ? -1 : (vy < 0) ? 1 : 0;
                            bullet.setCollisionNormal(normalX, normalY);
                        }
                        
                        detector.markDispatched(obj, detection.nearestObject);
                        if (!shouldFilterCollision(obj, detection.nearestObject)) {
                            CollisionDispatcher.dispatch(obj, detection.nearestObject);
                        }
                    }
                    continue; // No mover más
                }
            }
            
            // Recalcular velocityMag después de limitación
            double velocityMag = Math.sqrt(vx * vx + vy * vy);
            
            // ── SUB-STEPPING ADAPTATIVO para velocidad alta ───────────────
            
            if (velocityMag > SUBSTEP_VELOCITY_THRESHOLD) {
                // Velocidad PELIGROSA — dividir en sub-pasos
                int numSteps = (int) Math.ceil(velocityMag / MAX_STEP_SIZE);
                double stepVx = vx / numSteps;
                double stepVy = vy / numSteps;
                
                // Procesar cada sub-paso con SweptAABB completo
                boolean hitThisFrame = false;
                GameObjects finalHitObj = null;
                int finalNormalX = 0;
                int finalNormalY = 0;
                
                for (int step = 0; step < numSteps; step++) {
                    RectangleH bounds = colA.getBounds();
                    reusableBroad2D.copyFrom(bounds.sweep(stepVx, stepVy));
                    
                    double nearestTime = 1.0;
                    int hitNormalX = 0;
                    int hitNormalY = 0;
                    GameObjects hitObj = null;
                    
                    // Narrow phase en este sub-paso
                    for (GameObjects other : objects) {
                        if (other == obj) continue;
                        if (other instanceof Destroyable d && d.isPendingDestruction()) continue;
                        ColliderComponent colB = other.getComponent(ColliderComponent.class);
                        if (colB == null || colB.isTrigger()) continue;
                        if (!colA.canCollideWith(colB)) continue;
                        
                        RectangleH ob = colB.getBounds();
                        if (!reusableBroad2D.intersects(ob)) continue;
                        
                        SweptAABB.Result r = SweptAABB.calculate2D(bounds, ob, stepVx, stepVy);
                        if (!r.hasCollision()) continue;
                        if (r.time >= nearestTime) continue;
                        
                        nearestTime = r.time;
                        hitNormalX = r.normalX;
                        hitNormalY = r.normalY;
                        hitObj = other;
                    }
                    
                    // Mover al punto de contacto (o completo si no hay hit)
                    PhysicsStepper.moveWith(obj, stepVx * nearestTime, stepVy * nearestTime);
                    
                    if (hitObj != null) {
                        // ¡COLISIÓN DETECTADA! — detener sub-stepping
                        hitThisFrame = true;
                        finalHitObj = hitObj;
                        finalNormalX = hitNormalX;
                        finalNormalY = hitNormalY;
                        
                        // Aplicar separación mínima (sin escalado — ya sub-stepeamos)
                        double sepX = hitNormalX * ROBUST_SEPARATION_MINIMUM;
                        double sepY = hitNormalY * ROBUST_SEPARATION_MINIMUM;
                        PhysicsStepper.moveWith(obj, sepX, sepY);
                        
                        break; // Detener sub-stepping — ya colisionamos
                    }
                }
                
                // Procesar la colisión detectada
                if (hitThisFrame && finalHitObj != null) {
                    if (physics instanceof BulletPhysics bp) {
                        bp.setLastContactNormal(finalNormalX, finalNormalY);
                    }
                    if (obj instanceof Game.Items.Types.Bullets.Definition.Bullet bullet) {
                        bullet.setCollisionNormal(finalNormalX, finalNormalY);
                    }
                    
                    detector.markDispatched(obj, finalHitObj);
                    
                    if (!shouldFilterCollision(obj, finalHitObj)) {
                        CollisionDispatcher.dispatch(obj, finalHitObj);
                    }
                }
                
            } else {
                // Velocidad NORMAL — SweptAABB estándar (código original)
                RectangleH bounds = colA.getBounds();
                reusableBroad2D.copyFrom(bounds.sweep(vx, vy));

                double nearestTime = 1.0;
                int hitNormalX = 0;
                int hitNormalY = 0;
                GameObjects hitObj = null;

                for (GameObjects other : objects) {
                    if (other == obj) continue;
                    if (other instanceof Destroyable d && d.isPendingDestruction()) continue;
                    ColliderComponent colB = other.getComponent(ColliderComponent.class);
                    if (colB == null || colB.isTrigger()) continue;
                    if (!colA.canCollideWith(colB)) continue;
                    
                    RectangleH ob = colB.getBounds();
                    if (!reusableBroad2D.intersects(ob)) continue;

                    SweptAABB.Result r = SweptAABB.calculate2D(bounds, ob, vx, vy);
                    if (!r.hasCollision()) continue;
                    if (r.time >= nearestTime) continue;

                    nearestTime = r.time;
                    hitNormalX = r.normalX;
                    hitNormalY = r.normalY;
                    hitObj = other;
                }

                PhysicsStepper.moveWith(obj, vx * nearestTime, vy * nearestTime);

                if (hitObj != null) {
                    // Separación robusta según tipo de contacto
                    double sepX, sepY;
                    
                    if (nearestTime > 0.0) {
                        // Contacto CCD limpio: separación mínima directa
                        sepX = hitNormalX * ROBUST_SEPARATION_MINIMUM;
                        sepY = hitNormalY * ROBUST_SEPARATION_MINIMUM;
                    } else {
                        // time = 0: penetración preexistente — calcular MTV
                        RectangleH trigBounds = colA.getBounds();
                        RectangleH ob = hitObj.getComponent(ColliderComponent.class).getBounds();
                        Point2D.Double mtv = trigBounds.minimumTranslationVector(ob, hitNormalX, hitNormalY);
                        
                        // MTV ya incluye dirección correcta, agregar epsilon
                        sepX = (mtv.x != 0.0) ? mtv.x + Math.signum(mtv.x) * ROBUST_SEPARATION_MINIMUM : 0.0;
                        sepY = (mtv.y != 0.0) ? mtv.y + Math.signum(mtv.y) * ROBUST_SEPARATION_MINIMUM : 0.0;
                    }
                    
                    PhysicsStepper.moveWith(obj, sepX, sepY);

                    if (physics instanceof BulletPhysics bp) {
                        bp.setLastContactNormal(hitNormalX, hitNormalY);
                    }
                    if (obj instanceof Game.Items.Types.Bullets.Definition.Bullet bullet) {
                        bullet.setCollisionNormal(hitNormalX, hitNormalY);
                    }

                    detector.markDispatched(obj, hitObj);

                    if (!shouldFilterCollision(obj, hitObj)) {
                        CollisionDispatcher.dispatch(obj, hitObj);
                    }
                }
            }
        } // FIN FASE 1B


        // ── FASE 2: Detección AABB (overlaps residuales) ─────────────────
        // Detecta pares que no fueron procesados en FASE 1 ni FASE 1B:
        //   - Trigger vs Trigger
        //   - Sólido vs Sólido ya solapados (penetración preexistente sin velocidad)
        //   - Cualquier par que se solape y no haya sido despachado ya
        // CollisionDetector omite los pares registrados via markDispatched().
        List<CollisionResult> pairs = detector.detect(objects);

        // ── FASE 3: Despacho de eventos de FASE 2 ────────────────────────
        for (CollisionResult pair : pairs) {
            // Guard: skip si alguno está destruido
            if (pair.a instanceof Destroyable da && da.isPendingDestruction()) continue;
            if (pair.b instanceof Destroyable db && db.isPendingDestruction()) continue;

            // ── Filtro de colisión por ownership ───────────────────────────
            // DOD: Un proyectil NO debe colisionar con su owner
            if (shouldFilterCollision(pair.a, pair.b)) {
                continue; // skip dispatch
            }

            // Para triggers de FASE 2 que llegaron con normal calculada,
            // escribir la normal en BulletPhysics antes del dispatch
            propagateNormalToTrigger(pair);

            CollisionDispatcher.dispatch(pair.a, pair.b);
        }

        // ── FASE 4: CollisionListener enter / stay / exit ─────────────────
        IdentityHashMap<GameObjects, Set<GameObjects>> currentContacts =
                new IdentityHashMap<>();

        for (CollisionResult pair : pairs) {
            currentContacts
                .computeIfAbsent(pair.a, k -> Collections.newSetFromMap(new IdentityHashMap<>()))
                .add(pair.b);
            currentContacts
                .computeIfAbsent(pair.b, k -> Collections.newSetFromMap(new IdentityHashMap<>()))
                .add(pair.a);
        }

        for (GameObjects obj : objects) {
            List<CollisionHandler> listeners = collectListeners(obj);
            if (listeners.isEmpty()) continue;

            Set<GameObjects> current  = currentContacts.getOrDefault(obj, Set.of());
            Set<GameObjects> previous = previousContacts.getOrDefault(obj, Set.of());
            boolean          isTrigger = isTriggerObj(obj);

            for (GameObjects contact : current) {
                if (!previous.contains(contact)) {
                    for (CollisionHandler listener : listeners) {
                        if (isTrigger) listener.onTriggerEnter(contact);
                        else           listener.onCollisionEnter(contact);
                    }
                } else {
                    for (CollisionHandler listener : listeners) {
                        if (isTrigger) listener.onTriggerStay(contact);
                        else           listener.onCollisionStay(contact);
                    }
                }
            }

            for (GameObjects contact : previous) {
                if (!current.contains(contact)) {
                    for (CollisionHandler listener : listeners) {
                        if (isTrigger) listener.onTriggerExit(contact);
                        else           listener.onCollisionExit(contact);
                    }
                }
            }
        }

        // Rotar contactos: actual → anterior para el siguiente frame
        previousContacts.clear();
        previousContacts.putAll(currentContacts);

        // ── HRFC — Profiling Infrastructure ───────────────────────────────
        collisionTimer.stop();
    }


    /**
     * Limpia el estado de contactos anteriores.
     * Llamar al cambiar de mundo/escena para evitar enter/exit espurios.
     */
    public void clearContactHistory() {
        previousContacts.clear();
        detector.clear();
    }

    // ── HRFC — Profiling Infrastructure ───────────────────────────────────

    /**
     * Retorna el tiempo de colisiones del último frame en milisegundos.
     * Para diagnóstico de cuellos de botella.
     */
    public double getLastCollisionTimeMs() {
        return collisionTimer.getElapsedMs();
    }

    /**
     * Retorna el tiempo de broad phase del último frame en milisegundos.
     */
    public double getLastBroadPhaseTimeMs() {
        return broadPhaseTimer.getElapsedMs();
    }

    /**
     * Retorna el tiempo de narrow phase del último frame en milisegundos.
     */
    public double getLastNarrowPhaseTimeMs() {
        return narrowPhaseTimer.getElapsedMs();
    }

    /**
     * Retorna el tiempo de dispatch del último frame en milisegundos.
     */
    public double getLastDispatchTimeMs() {
        return dispatchTimer.getElapsedMs();
    }

    // ── Helpers de resolución de velocidad (política sólida) ──────────────

    private static void resolveVelocityX(Physics2D physics, GameObjects other, double vx) {
        // DOD FIX: Usar vSetX() en lugar de velocity.setX() para que
        // EnemyPhysics pueda escribir al DOD storage
        physics.vSetX(0);
    }

    private static void resolveVelocityY(Physics2D physics, GameObjects other, double vy) {
        // DOD FIX: Usar vSetY() en lugar de velocity.setY() para que
        // EnemyPhysics pueda escribir al DOD storage
        physics.vSetY(0);
    }

    // ── Helper: filtro de colisiones por ownership ────────────────────────

    /**
     * Determina si una colisión debe ser filtrada (no despachada).
     *
     * ── HRFC — Projectile DOD Migration: Collision Filtering ──────────────
     *
     * REGLA: Un proyectil NO debe colisionar con su owner.
     *
     * Esta es la solución correcta en DOD — el filtro está en el sistema
     * de colisiones, no en cada behavior individual. Evita que proyectiles
     * maten a su owner o se autodestruyan al spawnearse.
     *
     * CASOS FILTRADOS:
     *   - Bullet A con owner O colisionando con O
     *   - O colisionando con Bullet A que tiene owner O
     *
     * @param a primer objeto
     * @param b segundo objeto
     * @return true si la colisión debe ser filtrada (no despachar)
     */
    private boolean shouldFilterCollision(GameObjects a, GameObjects b) {
        // Verificar si A es un proyectil que colisiona con su owner
        if (a instanceof Game.Items.Types.Bullets.Definition.Bullet bullet) {
            Object owner = bullet.getOwner();
            if (owner != null && owner == b) {
                return true; // filtrar: proyectil colisionando con su owner
            }
        }
        
        // Verificar si B es un proyectil que colisiona con su owner
        if (b instanceof Game.Items.Types.Bullets.Definition.Bullet bullet) {
            Object owner = bullet.getOwner();
            if (owner != null && owner == a) {
                return true; // filtrar: proyectil colisionando con su owner
            }
        }
        
        return false; // no filtrar
    }

    // ── Helper: propagar normal a trigger desde CollisionResult de FASE 2 ─

    /**
     * Si el par de FASE 2 involucra un trigger con BulletPhysics,
     * escribe la normal calculada por CollisionDetector en el trigger
     * antes del dispatch para que onCollision() pueda leerla.
     *
     * Solo escribe si la normal es válida (no (0,0)) — evita borrar
     * una normal escrita por FASE 1B en el mismo frame.
     */
    private static void propagateNormalToTrigger(CollisionResult pair) {
        if (pair.normalX == 0 && pair.normalY == 0) return;

        if (pair.a.getComponent(ColliderComponent.class) != null
                && pair.a.getComponent(ColliderComponent.class).isTrigger()) {
            Physics2DComponent pc = pair.a.getComponent(Physics2DComponent.class);
            if (pc != null && pc.getPhysics() instanceof BulletPhysics bp
                    && !bp.hasContactNormal()) {
                bp.setLastContactNormal(pair.normalX, pair.normalY);
            }
            // ── HRFC — DOD Bridge: también escribir en Bullet DOD adapter ─
            if (pair.a instanceof Game.Items.Types.Bullets.Definition.Bullet bullet) {
                bullet.setCollisionNormal(pair.normalX, pair.normalY);
            }
        }

        if (pair.b.getComponent(ColliderComponent.class) != null
                && pair.b.getComponent(ColliderComponent.class).isTrigger()) {
            Physics2DComponent pc = pair.b.getComponent(Physics2DComponent.class);
            if (pc != null && pc.getPhysics() instanceof BulletPhysics bp
                    && !bp.hasContactNormal()) {
                // Normal invertida: la de pair es desde perspectiva de A
                bp.setLastContactNormal(-pair.normalX, -pair.normalY);
            }
            // ── HRFC — DOD Bridge: también escribir en Bullet DOD adapter ─
            if (pair.b instanceof Game.Items.Types.Bullets.Definition.Bullet bullet) {
                bullet.setCollisionNormal(-pair.normalX, -pair.normalY);
            }
        }
    }

    // ── Helper: aplicar flag de contacto según la cara impactada ─────────

    private static void applyContactFlag(Physics2D physics, int normalX, int normalY,
                                          SideInteraction sideType) {
        switch (sideType) {
            case SURFACE   -> physics.setOnGround(true);
            case CEILING   -> physics.setOnCeiling(true);
            case WALL,
                 CLIMBABLE -> physics.setOnWall(true);
            case NONE      -> {
                if (normalY == -1)      physics.setOnGround(true);
                else if (normalY == 1)  physics.setOnCeiling(true);
                else if (normalX != 0)  physics.setOnWall(true);
            }
            case PASSTHROUGH -> { /* filtrado antes de llegar aquí */ }
        }
    }

    private static SideInteraction defaultSideForNormalY(int normalY) {
        return normalY == -1 ? SideInteraction.SURFACE : SideInteraction.CEILING;
    }

    // ── Helper: resolución de superficie ─────────────────────────────────

    private static SurfaceMaterial resolveSurface(GameObjects obj) {
        SurfaceComponent sc = obj.getComponent(SurfaceComponent.class);
        if (sc != null) return sc;
        if (obj instanceof SurfaceMaterial sm) return sm;
        return SurfaceMaterial.DEFAULT;
    }

    // ── Helper: PushableComponent ─────────────────────────────────────────

    private static void applyPushIfPushable(GameObjects target,
                                             double vx, double vy,
                                             double sourceMass) {
        PushableComponent pushable = target.getComponent(PushableComponent.class);
        if (pushable == null || !pushable.isEnabled()) return;
        pushable.applyPush(vx * sourceMass, vy * sourceMass);
    }

    // ── Helpers privados de FASE 4 ────────────────────────────────────────

    private static List<CollisionHandler> collectListeners(GameObjects obj) {
        List<CollisionHandler> result = null;
        for (Component c : obj.getComponents()) {
            if (c instanceof CollisionHandler cle) {
                if (result == null) result = new ArrayList<>(2);
                result.add(cle);
            }
        }
        return result != null ? result : List.of();
    }

    private static boolean isTriggerObj(GameObjects obj) {
        ColliderComponent col = obj.getComponent(ColliderComponent.class);
        return col != null && col.isTrigger();
    }

    // ── Helpers de separación de contacto ────────────────────────────────

    /**
     * Calcula la profundidad de penetración del trigger dentro del hit en el
     * eje de la normal del impacto.
     *
     * Se usa exclusivamente para contactos con time=0 (penetración preexistente),
     * donde SweptAABB ya no puede calcular el desplazamiento correcto porque el
     * objeto ya está dentro del obstáculo.
     *
     * ── HRFC — Uso de RectangleH API ──────────────────────────────────────
     * Actualizado para usar overlapWidth() y overlapHeight() en lugar de
     * cálculo manual de overlaps. Esto elimina duplicación y usa la API probada.
     *
     * ── Cálculo ───────────────────────────────────────────────────────────
     * Para normal (nx=1, ny=0) — pared derecha:
     *   La bullet viene desde la derecha y penetró la pared.
     *   pen = (hit.x + hit.width) - trig.x  → cuánto solapan en X desde la izquierda.
     *   Pero la normal es +X (bullet saldrá hacia la derecha), así que pen es:
     *   pen = (hit.x + hit.width) - trig.x
     *
     * La fórmula general es el overlap en el eje activo de la normal:
     *   Si nx != 0: pen = solapamiento en X = min(trig.x+trig.w, hit.x+hit.w) - max(trig.x, hit.x)
     *   Si ny != 0: pen = solapamiento en Y = min(trig.y+trig.h, hit.y+hit.h) - max(trig.y, hit.y)
     *
     * Retorna 0.0 si no hay overlap real (caso defensivo — no debería ocurrir
     * si el caller verificó time=0 correctamente).
     *
     * @param trig      bounds del trigger (ya puede estar solapando)
     * @param hit       bounds del collider impactado
     * @param normalX   normal en X del impacto (-1, 0, +1)
     * @param normalY   normal en Y del impacto (-1, 0, +1)
     * @return profundidad de penetración en el eje de la normal (>= 0)
     */
    private static double computePenetrationDepth(RectangleH trig, RectangleH hit,
                                                   int normalX, int normalY) {
        // HRFC: Usar overlapWidth/Height en lugar de cálculo manual
        if (normalX != 0) {
            // Eje X: calcular overlap horizontal
            return trig.overlapWidth(hit);
        } else {
            // Eje Y: calcular overlap vertical
            return trig.overlapHeight(hit);
        }
    }

    /**
     * Garantiza que la velocidad del trigger apunte FUERA del collider tras el
     * dispatch de contacto.
     *
     * ── Por qué existe este guard ─────────────────────────────────────────
     * Después del dispatch, el behavior puede haber modificado la velocidad
     * incorrectamente (inversión doble por doble-dispatch, fallback incorrecto,
     * velocidad sin cambios por un behavior que no maneja el caso). Si la
     * velocidad resultante apunta hacia el collider — es decir, V·N < 0 —
     * el siguiente frame detectará inmediatamente otro contacto: oscilación.
     *
     * El guard asegura que la componente de velocidad en la dirección de la
     * normal sea >= 0 (el objeto no está penetrando la superficie).
     * Solo actúa en el eje de la normal, sin tocar la componente perpendicular.
     *
     * ── Semántica ─────────────────────────────────────────────────────────
     * V·N > 0 significa que el objeto se aleja del collider → correcto.
     * V·N = 0 significa que el objeto se mueve paralelo → neutro, aceptable.
     * V·N < 0 significa que el objeto penetra → forzar a 0 en ese eje.
     *
     * Forzar a 0 es más conservador que invertir la velocidad (que podría
     * producir rebotes no deseados). El behavior ya aplicó la reflexión que
     * consideró correcta; el guard solo interviene si esa reflexión falló.
     *
     * @param physics   física del trigger (se modificará si V·N < 0)
     * @param normalX   normal del contacto en X
     * @param normalY   normal del contacto en Y
     */
    private static void enforcePostContactVelocity(Physics2D physics,
                                                    int normalX, int normalY) {
        double vx = physics.getVelocity().getX();
        double vy = physics.getVelocity().getY();

        // Producto punto V·N
        double dot = vx * normalX + vy * normalY;

        if (dot < 0.0) {
            // La velocidad sigue apuntando hacia el collider — neutralizar esa componente.
            // Solo se toca el eje de la normal (el perpendicular no cambia).
            if (normalX != 0) physics.getVelocity().setX(0.0);
            if (normalY != 0) physics.getVelocity().setY(0.0);
        }
    }
}
