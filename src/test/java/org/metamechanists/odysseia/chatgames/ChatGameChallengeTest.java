package org.metamechanists.odysseia.chatgames;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChatGameChallengeTest {
    @Test
    void acceptsCaseAndWhitespaceWithoutAcceptingWrongAnswers() {
        ChatGameChallenge challenge = new ChatGameChallenge(SeasonalGameMode.HERMES_REFLEX, "prompt", "RAYO DORADO", "answer");
        assertTrue(challenge.matches("  rayo   dorado "));
        assertFalse(challenge.matches("RAYO PLATEADO"));
    }
}
