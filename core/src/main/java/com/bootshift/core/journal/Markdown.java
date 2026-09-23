package com.bootshift.core.journal;

import java.util.List;

/**
 * Markdown assembly helpers for generated documents.
 *
 * <p>Small, but not trivial: almost everything rendered here is derived from a repository the
 * harness does not control. Module names, file paths, compiler diagnostics and command lines all
 * reach these documents, and any of them can contain a pipe, a backtick or a newline. An unescaped
 * pipe in a table cell silently shifts every column to its right, which in an audit document means a
 * hash appearing under "status".
 */
public final class Markdown {

    /** Rendered in place of a value the run never produced. */
    public static final String ABSENT = "—";

    /** What a section says when the stage it belongs to has nothing of that kind. */
    public static final String NOT_APPLICABLE = "Not applicable for this stage.";

    private Markdown() {
    }

    /** Escapes a value for use inside a table cell. */
    public static String cell(Object value) {
        if (value == null) {
            return ABSENT;
        }
        String text = String.valueOf(value);
        if (text.isBlank()) {
            return ABSENT;
        }
        return text.replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\r\n", " ")
                .replace("\n", " ")
                .replace("\r", " ")
                .trim();
    }

    /**
     * Wraps a value in code ticks, escaping only what a code span cannot survive.
     *
     * <p>Notably <em>not</em> the backslash. Markdown does not process escapes inside a code span, so
     * escaping one there is not a no-op - it renders literally, and every Windows path in a generated
     * document comes out as {@code C:\\Users\\...}. The pipe still has to go, because a table row is
     * split before its cells are parsed as Markdown at all.
     */
    public static String code(Object value) {
        if (value == null) {
            return ABSENT;
        }
        String text = String.valueOf(value);
        if (text.isBlank()) {
            return ABSENT;
        }
        String inner = text.replace("|", "\\|")
                .replace("\r\n", " ")
                .replace("\n", " ")
                .replace("\r", " ")
                .trim();
        // A value containing a backtick is fenced with a longer run, per CommonMark.
        String fence = inner.contains("``") ? "```" : inner.contains("`") ? "``" : "`";
        String padding = inner.startsWith("`") || inner.endsWith("`") ? " " : "";
        return fence + padding + inner + padding + fence;
    }

    /** Plain body text with the characters that would start a block construct neutralised. */
    public static String text(Object value) {
        if (value == null) {
            return ABSENT;
        }
        String raw = String.valueOf(value).trim();
        if (raw.isEmpty()) {
            return ABSENT;
        }
        return raw.replace("\r\n", "\n");
    }

    /**
     * Escapes a label for a Mermaid node.
     *
     * <p>Mermaid has its own grammar layered on top of Markdown and breaks on characters Markdown is
     * perfectly happy with - quotes, brackets, semicolons and the arrow sequences it uses for edges.
     * A diagram that fails to parse renders as an error box in place of the content.
     */
    public static String mermaid(Object value) {
        if (value == null) {
            return "unknown";
        }
        String text = String.valueOf(value);
        // Order matters, and getting it wrong is not obvious. The edge arrow has to go before the
        // angle brackets it is made of, or it survives as a fragment. HTML entities are deliberately
        // not used: &gt; ends in a semicolon, which is itself a Mermaid statement separator, so
        // escaping with entities would reintroduce the exact character the previous step removed.
        String cleaned = text.replace("-->", " to ")
                .replace("---", " to ")
                .replace("\"", "'")
                .replace("[", "(")
                .replace("]", ")")
                .replace("{", "(")
                .replace("}", ")")
                .replace("<", "(")
                .replace(">", ")")
                .replace(";", ",")
                .replace("|", "/")
                .replace("#", " ")
                .replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isEmpty() ? "unknown" : cleaned;
    }

    /** Renders a table, or the empty-state sentence when there are no rows. */
    public static String table(StringBuilder sb, List<String> headers, List<List<String>> rows,
                               String emptyState) {
        if (rows.isEmpty()) {
            sb.append(emptyState).append("\n\n");
            return sb.toString();
        }
        sb.append("| ").append(String.join(" | ", headers)).append(" |\n");
        sb.append("|").append(" --- |".repeat(headers.size())).append("\n");
        for (List<String> row : rows) {
            sb.append("| ").append(String.join(" | ", row)).append(" |\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    /** Formats a duration for a human, not for a parser. */
    public static String duration(long millis) {
        if (millis < 1000) {
            return millis + " ms";
        }
        if (millis < 60_000) {
            return String.format("%.1f s", millis / 1000.0);
        }
        long minutes = millis / 60_000;
        long seconds = (millis % 60_000) / 1000;
        return minutes + "m " + seconds + "s";
    }

    /** Shortens a hash for display while keeping enough to be recognisable. */
    public static String shortHash(String hash) {
        if (hash == null || hash.isBlank()) {
            return ABSENT;
        }
        return hash.length() <= 16 ? hash : hash.substring(0, 16) + "…";
    }

    /** A bullet list, or the empty-state sentence. */
    public static void bullets(StringBuilder sb, List<String> items, String emptyState) {
        if (items.isEmpty()) {
            sb.append(emptyState).append("\n\n");
            return;
        }
        items.forEach(item -> sb.append("- ").append(text(item)).append("\n"));
        sb.append("\n");
    }
}
