package Game.Gameplay.World.Presets;

import Game.Engine.World.Components.MaterialComponent;

/**
 * Perfiles de material predefinidos para MaterialComponent.
 *
 * ── HRFC-015 — World Simulation Core ──────────────────────────────────────
 *
 * ── UBICACIÓN ─────────────────────────────────────────────────────────────
 * MaterialPresets vive en Game.Gameplay.World.Presets, no en el Engine.
 * Los presets son contenido reutilizable del universo de Infinity Hell —
 * no infraestructura del Engine.
 *
 * El Engine (Game.Engine.World.Components.MaterialComponent) no conoce
 * estos presets. Los Assemblers de gameplay los usan como punto de partida:
 *
 *   addComponent(MaterialPresets.organicFlesh().build());
 *
 *   addComponent(MaterialPresets.iron()
 *       .elasticity(0.9)   // variante más elástica que el estándar
 *       .build());
 *
 * ── DISEÑO: SIN CATEGORÍAS RÍGIDAS EN EL ENGINE ──────────────────────────
 * MaterialPresets NO introduce un enum ni una jerarquía de tipos en el Engine.
 * Son factories de Builder con valores numéricos predefinidos.
 *
 * El Engine solo ve un MaterialComponent con thermalConductivity=0.8,
 * electricalResistance=0.02, etc. El nombre del preset es conveniencia
 * exclusiva para los Assemblers de gameplay.
 *
 * ── VALORES ───────────────────────────────────────────────────────────────
 * Los valores son relativos al universo del juego, no unidades SI reales.
 *
 *   thermalConductivity  [0,1]    — velocidad de intercambio térmico
 *   heatCapacity         (0,∞)    — inercia térmica (resistencia al cambio)
 *   thermalDiffusivity   [0,1]    — homogeneización interna del calor
 *   electricalResistance [0,1]    — resistencia al flujo de carga (0=conductor)
 *   humidityAbsorption   [0,1]    — velocidad de absorción/liberación de humedad
 *   compressibility      [0,1]    — facilidad de cambio de volumen bajo presión
 *   elasticity           [0,1]    — energía conservada en rebotes
 *   hardness             [0,1]    — resistencia a deformación mecánica
 *   density              (0,∞)    — masa relativa por volumen
 */
public final class MaterialPresets {

    private MaterialPresets() {}

    // ── Materiales orgánicos ──────────────────────────────────────────────

    /** Carne orgánica — cuerpos de criaturas vivas y no-muertas. */
    public static MaterialComponent.Builder organicFlesh() {
        return MaterialComponent.builder()
            .thermalConductivity(0.22).heatCapacity(3500.0).thermalDiffusivity(0.15)
            .electricalResistance(0.60).humidityAbsorption(0.55)
            .compressibility(0.65).elasticity(0.20).hardness(0.25).density(1050.0);
    }

    /** Hueso — esqueletos, constructos óseos, proyectiles de hueso. */
    public static MaterialComponent.Builder bone() {
        return MaterialComponent.builder()
            .thermalConductivity(0.14).heatCapacity(1200.0).thermalDiffusivity(0.10)
            .electricalResistance(0.85).humidityAbsorption(0.10)
            .compressibility(0.20).elasticity(0.15).hardness(0.70).density(1900.0);
    }

    /** Madera — estructuras de nivel, armas de madera. */
    public static MaterialComponent.Builder wood() {
        return MaterialComponent.builder()
            .thermalConductivity(0.12).heatCapacity(1700.0).thermalDiffusivity(0.08)
            .electricalResistance(0.95).humidityAbsorption(0.60)
            .compressibility(0.30).elasticity(0.20).hardness(0.40).density(600.0);
    }

    // ── Metales ───────────────────────────────────────────────────────────

    /** Hierro / acero — armas, armaduras, estructuras metálicas. */
    public static MaterialComponent.Builder iron() {
        return MaterialComponent.builder()
            .thermalConductivity(0.80).heatCapacity(500.0).thermalDiffusivity(0.60)
            .electricalResistance(0.02).humidityAbsorption(0.02)
            .compressibility(0.05).elasticity(0.60).hardness(0.88).density(7800.0);
    }

    /** Cobre — conductores, circuitos, artefactos tecnológicos. */
    public static MaterialComponent.Builder copper() {
        return MaterialComponent.builder()
            .thermalConductivity(0.95).heatCapacity(385.0).thermalDiffusivity(0.75)
            .electricalResistance(0.005).humidityAbsorption(0.01)
            .compressibility(0.04).elasticity(0.65).hardness(0.72).density(8900.0);
    }

    /** Oro — artefactos mágicos, objetos de valor. */
    public static MaterialComponent.Builder gold() {
        return MaterialComponent.builder()
            .thermalConductivity(0.90).heatCapacity(129.0).thermalDiffusivity(0.80)
            .electricalResistance(0.008).humidityAbsorption(0.005)
            .compressibility(0.06).elasticity(0.55).hardness(0.50).density(19300.0);
    }

    // ── Piedra y tierra ───────────────────────────────────────────────────

    /** Piedra — paredes de nivel, suelos, obstáculos. */
    public static MaterialComponent.Builder stone() {
        return MaterialComponent.builder()
            .thermalConductivity(0.08).heatCapacity(840.0).thermalDiffusivity(0.05)
            .electricalResistance(0.98).humidityAbsorption(0.05)
            .compressibility(0.02).elasticity(0.10).hardness(0.92).density(2700.0);
    }

    /** Tierra / barro — suelos naturales, entornos orgánicos. */
    public static MaterialComponent.Builder earth() {
        return MaterialComponent.builder()
            .thermalConductivity(0.15).heatCapacity(800.0).thermalDiffusivity(0.07)
            .electricalResistance(0.70).humidityAbsorption(0.40)
            .compressibility(0.45).elasticity(0.05).hardness(0.20).density(1500.0);
    }

    // ── Hielo y agua ──────────────────────────────────────────────────────

    /** Hielo — superficies heladas, proyectiles de hielo, criaturas de hielo. */
    public static MaterialComponent.Builder ice() {
        return MaterialComponent.builder()
            .thermalConductivity(0.55).heatCapacity(2090.0).thermalDiffusivity(0.40)
            .electricalResistance(0.45).humidityAbsorption(0.05)
            .compressibility(0.08).elasticity(0.35).hardness(0.65).density(917.0);
    }

    /** Agua / fluido — zonas inundadas, cuerpos de agua, criaturas acuáticas. */
    public static MaterialComponent.Builder water() {
        return MaterialComponent.builder()
            .thermalConductivity(0.30).heatCapacity(4186.0).thermalDiffusivity(0.20)
            .electricalResistance(0.15).humidityAbsorption(1.00)
            .compressibility(0.95).elasticity(0.00).hardness(0.00).density(1000.0);
    }

    // ── Materiales energéticos / mágicos ──────────────────────────────────

    /**
     * Materia de alma — entidades etéreas, fantasmas, almas invocadas.
     * Casi sin interacción física. Sus propiedades especiales emergen
     * de Influencias mágicas, no del material.
     */
    public static MaterialComponent.Builder soulMatter() {
        return MaterialComponent.builder()
            .thermalConductivity(0.01).heatCapacity(10000.0).thermalDiffusivity(0.01)
            .electricalResistance(0.99).humidityAbsorption(0.00)
            .compressibility(0.90).elasticity(0.00).hardness(0.00).density(0.10);
    }

    /** Plasma / energía concentrada — rayos, explosiones, criaturas de energía. */
    public static MaterialComponent.Builder plasma() {
        return MaterialComponent.builder()
            .thermalConductivity(0.99).heatCapacity(100.0).thermalDiffusivity(0.99)
            .electricalResistance(0.001).humidityAbsorption(0.00)
            .compressibility(0.99).elasticity(0.00).hardness(0.00).density(0.01);
    }

    /** Magma / lava — zonas volcánicas, criaturas ígneas. */
    public static MaterialComponent.Builder magma() {
        return MaterialComponent.builder()
            .thermalConductivity(0.40).heatCapacity(1200.0).thermalDiffusivity(0.25)
            .electricalResistance(0.90).humidityAbsorption(0.00)
            .compressibility(0.10).elasticity(0.05).hardness(0.30).density(2700.0);
    }

    // ── Materiales estructurales / artificiales ───────────────────────────

    /** Cristal — ventanas, barreras mágicas, objetos frágiles. */
    public static MaterialComponent.Builder glass() {
        return MaterialComponent.builder()
            .thermalConductivity(0.25).heatCapacity(840.0).thermalDiffusivity(0.18)
            .electricalResistance(0.97).humidityAbsorption(0.01)
            .compressibility(0.03).elasticity(0.80).hardness(0.30).density(2500.0);
    }

    /** Goma / elastómero — amortiguadores, criaturas gelatinosas. */
    public static MaterialComponent.Builder rubber() {
        return MaterialComponent.builder()
            .thermalConductivity(0.05).heatCapacity(2000.0).thermalDiffusivity(0.03)
            .electricalResistance(0.99).humidityAbsorption(0.05)
            .compressibility(0.80).elasticity(0.95).hardness(0.15).density(1200.0);
    }
}
