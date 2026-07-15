/**
 * Cámara como entidad del gameplay.
 *
 * ── Propósito de este package ────────────────────────────────────────────
 *
 * Este package es el hogar declarado de toda clase de cámara que participe
 * en el dominio del juego como actor con comportamiento propio. No contiene
 * código de infraestructura de render ni de control de vista del Engine.
 *
 * ── Qué pertenece aquí ───────────────────────────────────────────────────
 *
 * Ejemplos de clases que irán en este package cuando se implementen:
 *
 *   CinematicCamera      — secuencia de posición/zoom/rotación para intro de boss.
 *   BossCameraSequence   — lógica de cámara controlada por el script de un boss.
 *   CameraZone           — zona del mapa que fuerza un zoom o posición específica.
 *   CameraShakeEvent     — evento que desencadena un shake en la GameCamera.
 *   CameraLockTrigger    — trigger que bloquea la cámara en una región del mundo.
 *
 * Estas clases leen o modifican {@link Game.Engine.Camera.GameCamera} a través
 * de la API pública de GameCamera o de un CameraController. No reimplementan
 * la cámara: la dirigen.
 *
 * ── Qué NO pertenece aquí ────────────────────────────────────────────────
 *
 *   Game.Engine.Camera   — infraestructura del Engine: GameCamera (estado de
 *                          la vista), CameraController (interfaz de comportamiento),
 *                          FollowCameraController (seguimiento suave del player).
 *                          Estas clases no tienen conocimiento del gameplay.
 *
 *   Game.Engine.RenderEngine   — RenderCamera (snapshot de posición para el pipeline
 *                          de render). No es una cámara; es un value object del
 *                          sistema de render.
 *
 * ── Separación de conceptos ───────────────────────────────────────────────
 *
 *   Engine.Camera  → "cómo se mueve y calcula la vista"  (infraestructura)
 *   Gameplay.Camera→ "qué le pasa a la cámara en el juego" (dominio)
 *   Render         → "qué necesita el pipeline para dibujar" (presentación)
 */
package Game.Gameplay.Camera;
