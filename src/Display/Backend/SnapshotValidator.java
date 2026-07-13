package Display.Backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Valida si un {@link DisplaySnapshot} representa un estado utilizable
 * para las distintas fases del ciclo de vida del Display Engine.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * FILOSOFÍA
 *
 * La gate de readiness NO se abre únicamente porque exista un snapshot.
 * Se abre únicamente cuando ese snapshot representa un estado confirmado
 * por AWT que cumple las condiciones mínimas requeridas para cada fase.
 *
 * SnapshotValidator es el único lugar donde se codifican esas condiciones.
 * El Pipeline consume el resultado; nunca reescribe las reglas localmente.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * DOS NIVELES DE VALIDACIÓN
 *
 * isUsable(snapshot)
 *   Condiciones mínimas para que el canvas pueda usarse como superficie
 *   de render: el peer existe, el canvas es visible, la ventana es visible
 *   y las dimensiones son válidas. Este nivel se verifica ANTES de
 *   construir la BufferStrategy. Si falla, no tiene sentido intentar build.
 *
 * isRenderReady(snapshot)
 *   Condiciones completas para reanudar el render: todo lo de isUsable()
 *   más la presencia de una BS sana y una GraphicsConfiguration válida.
 *   Este nivel se verifica DESPUÉS de construir la surface. La gate de
 *   readiness se abre solo si este nivel pasa.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * RESULTADO TIPADO: ValidationResult
 *
 * Ambos métodos retornan un ValidationResult que indica:
 *   - si la validación pasó (passed)
 *   - la lista de razones de fallo en texto legible (reasons)
 *
 * Esto permite logging preciso sin necesidad de booleanos separados
 * por condición o excepciones de control de flujo.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREAD SAFETY
 *
 * Sin estado interno. Todos los métodos son estáticos y puros.
 * Seguro llamar desde cualquier thread.
 */
public final class SnapshotValidator {

    private SnapshotValidator() {}

    // ── Validación de usabilidad básica ───────────────────────────────────────

    /**
     * Verifica las condiciones mínimas para que el canvas pueda usarse
     * como destino de render y para que createBufferStrategy() tenga sentido.
     *
     * Condiciones verificadas:
     *   1. canvas.isDisplayable()  — el peer nativo existe.
     *   2. canvas.isVisible()      — el canvas es visible en el árbol Swing.
     *   3. frame.isVisible()       — la ventana está en pantalla.
     *   4. canvasWidth > 0         — dimensión horizontal válida.
     *   5. canvasHeight > 0        — dimensión vertical válida.
     *
     * No verifica la BufferStrategy: esta validación ocurre antes de build().
     *
     * @param snapshot snapshot a validar; nunca null.
     * @return resultado con passed=true si todas las condiciones se cumplen.
     */
    public static ValidationResult isUsable(DisplaySnapshot snapshot) {
        List<String> failures = new ArrayList<>();

        if (!snapshot.canvasDisplayable()) {
            failures.add("canvas not displayable (peer not yet created)");
        }
        if (!snapshot.canvasVisible()) {
            failures.add("canvas not visible in component tree");
        }
        if (!snapshot.windowVisible()) {
            failures.add("window not visible on screen");
        }
        if (snapshot.canvasWidth() <= 0) {
            failures.add("canvas width <= 0 (" + snapshot.canvasWidth() + ")");
        }
        if (snapshot.canvasHeight() <= 0) {
            failures.add("canvas height <= 0 (" + snapshot.canvasHeight() + ")");
        }

        return failures.isEmpty()
            ? ValidationResult.passed()
            : ValidationResult.failed(failures);
    }

    // ── Validación de readiness completa ──────────────────────────────────────

    /**
     * Verifica las condiciones completas para reanudar el render.
     *
     * Extiende isUsable() con:
     *   6. graphicsConfig != null  — hay un device gráfico activo.
     *   7. bufferStrategyPresent   — la BS fue creada correctamente.
     *   8. !bufferStrategyContentsLost — la BS tiene contenido válido.
     *
     * La gate de readiness se abre solo cuando este nivel pasa.
     * Si la BS está presente pero contentsLost, no se abre la gate:
     * la surface debe reconstruirse antes de reanudar.
     *
     * @param snapshot snapshot a validar tras construir la surface; nunca null.
     * @return resultado con passed=true si todas las condiciones se cumplen.
     */
    public static ValidationResult isRenderReady(DisplaySnapshot snapshot) {
        // Primero verificar las condiciones básicas de usabilidad.
        ValidationResult base = isUsable(snapshot);
        List<String> failures = new ArrayList<>(base.reasons);

        if (snapshot.graphicsConfig() == null) {
            failures.add("graphicsConfig is null (canvas not bound to a device)");
        }
        if (!snapshot.bufferStrategyPresent()) {
            failures.add("no BufferStrategy present (build may have failed)");
        }
        if (snapshot.bufferStrategyContentsLost()) {
            failures.add("BufferStrategy contentsLost (needs rebuild before render)");
        }

        return failures.isEmpty()
            ? ValidationResult.passed()
            : ValidationResult.failed(failures);
    }

    // ── ValidationResult ──────────────────────────────────────────────────────

    /**
     * Resultado tipado de una validación de snapshot.
     *
     * Si {@code passed} es true, {@code reasons} está vacío.
     * Si {@code passed} es false, {@code reasons} contiene la descripción
     * textual de cada condición fallida para logging y diagnóstico.
     */
    public static final class ValidationResult {

        /** True si la validación pasó; todas las condiciones se cumplieron. */
        public final boolean passed;

        /**
         * Razones de fallo. Vacío si passed == true.
         * Lista inmutable.
         */
        public final List<String> reasons;

        private ValidationResult(boolean passed, List<String> reasons) {
            this.passed  = passed;
            this.reasons = Collections.unmodifiableList(reasons);
        }

        static ValidationResult passed() {
            return new ValidationResult(true, List.of());
        }

        static ValidationResult failed(List<String> reasons) {
            return new ValidationResult(false, new ArrayList<>(reasons));
        }

        /** True si la validación falló. Conveniencia para if (!result.passed). */
        public boolean failed() {
            return !passed;
        }

        /**
         * Descripción legible de las razones de fallo, separadas por "; ".
         * Retorna la cadena vacía si la validación pasó.
         */
        public String summary() {
            return String.join("; ", reasons);
        }

        @Override
        public String toString() {
            return passed ? "ValidationResult[PASSED]"
                          : "ValidationResult[FAILED: " + summary() + "]";
        }
    }
}
