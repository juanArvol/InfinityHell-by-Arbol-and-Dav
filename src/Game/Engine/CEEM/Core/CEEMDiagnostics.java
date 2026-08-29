package Game.Engine.CEEM.Core;

/**
 * Internal diagnostics and error reporting for CEEM.
 * 
 * This class provides a centralized point for CEEM to report operational
 * issues without introducing external logging dependencies.
 * 
 * ARCHITECTURAL PRINCIPLE:
 * CEEM should not fail catastrophically when a single module has issues.
 * Instead, it isolates failures, reports them diagnostically, and continues
 * coordination for healthy modules.
 * 
 * DESIGN RATIONALE:
 * - Simple: No external dependencies (SLF4J, Log4j, etc.)
 * - Replaceable: Can be redirected to engine's logging system if it exists
 * - Non-blocking: Never throws exceptions during diagnostic reporting
 * - Structured: Different severity levels for different situations
 * 
 * VISIBILITY:
 * Package-visible within CEEM subsystem for internal diagnostics.
 * Not part of CEEM's public API.
 * 
 * FUTURE EVOLUTION:
 * If the engine adopts a centralized logging framework, this class can
 * be adapted to forward messages through that system instead of stderr.
 */
public final class CEEMDiagnostics {
    
    /**
     * Diagnostic severity levels.
     */
    enum Severity {
        /** Informational message about CEEM operation */
        INFO,
        /** Potential issue that doesn't prevent operation */
        WARNING,
        /** Error in module behavior, CEEM continues */
        ERROR,
        /** Critical contract violation, investigation required */
        CRITICAL
    }
    
    // Prevent instantiation
    private CEEMDiagnostics() {}
    
    /**
     * Reports an informational diagnostic message.
     * 
     * @param message diagnostic message
     */
    public static void info(String message) {
        report(Severity.INFO, message, null);
    }
    
    /**
     * Reports a warning diagnostic message.
     * 
     * @param message diagnostic message
     */
    public static void warning(String message) {
        report(Severity.WARNING, message, null);
    }
    
    /**
     * Reports an error diagnostic message.
     * 
     * @param message diagnostic message
     */
    public static void error(String message) {
        report(Severity.ERROR, message, null);
    }
    
    /**
     * Reports an error diagnostic message with exception context.
     * 
     * @param message diagnostic message
     * @param exception exception that triggered the diagnostic
     */
    public static void error(String message, Exception exception) {
        report(Severity.ERROR, message, exception);
    }
    
    /**
     * Reports a critical diagnostic message (contract violation).
     * 
     * @param message diagnostic message
     */
    public static void critical(String message) {
        report(Severity.CRITICAL, message, null);
    }
    
    /**
     * Core diagnostic reporting implementation.
     * 
     * Currently writes to System.err with structured formatting.
     * Can be redirected to engine logging system in the future.
     */
    private static void report(Severity severity, String message, Exception exception) {
        try {
            StringBuilder sb = new StringBuilder();
            
            // Format: [CEEM|SEVERITY] message
            sb.append("[CEEM|").append(severity).append("] ");
            sb.append(message);
            
            if (exception != null) {
                sb.append(" | Exception: ").append(exception.getClass().getSimpleName());
                sb.append(": ").append(exception.getMessage());
            }
            
            // Write to stderr (can be redirected to engine logger later)
            System.err.println(sb.toString());
            
            // For critical issues, also print stack trace if exception present
            if (severity == Severity.CRITICAL && exception != null) {
                exception.printStackTrace(System.err);
            }
            
        } catch (Exception e) {
            // Never let diagnostic reporting throw exceptions
            // Fallback to minimal stderr output
            System.err.println("[CEEM|DIAGNOSTIC-FAILURE] Failed to report diagnostic: " + e.getMessage());
        }
    }
}
