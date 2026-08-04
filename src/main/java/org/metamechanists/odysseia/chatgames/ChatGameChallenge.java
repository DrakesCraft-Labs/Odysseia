package org.metamechanists.odysseia.chatgames;

/** A compact challenge always retains its accepted answer and a human explanation for timeout reveal. */
public record ChatGameChallenge(SeasonalGameMode mode, String prompt, String answer, String explanation) {
    public boolean matches(String candidate) { return normalize(answer).equals(normalize(candidate)); }

    static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
    }
}
