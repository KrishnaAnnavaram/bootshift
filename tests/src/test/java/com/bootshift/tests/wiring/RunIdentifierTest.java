package com.bootshift.tests.wiring;

import com.bootshift.cli.CommonOptions;
import com.bootshift.core.domain.ExitCode;
import com.bootshift.core.domain.HarnessException;
import com.bootshift.core.util.Ids;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The run identifier the CLI accepts has to be one the artifact schemas accept.
 *
 * <p>A {@code --run-id} of the operator's own choosing used to pass the CLI, survive bootstrap -
 * which snapshots the entire repository into a fresh workspace - and then fail Agent 01 with
 * {@code $.run_id: does not match the regex pattern}. The work was already done and the message
 * named neither the option nor the fix.
 */
class RunIdentifierTest {

    @Test
    @DisplayName("an allocated run id is accepted")
    void allocatedRunIdIsUsable() {
        assertThatCode(() -> CommonOptions.requireUsableRunId(Ids.runId()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("no run id at all is accepted: the harness allocates one")
    void absentRunIdIsUsable() {
        assertThatCode(() -> CommonOptions.requireUsableRunId(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a readable but unusable run id is refused by the CLI, not by stage 01")
    void unusableRunIdIsRefusedUpFront() {
        for (String unusable : new String[]{"RUN-FX-1", "my-run", "RUN-", "RUN-0123456789",
                // Crockford base32 excludes I, L, O and U so they cannot be confused with 1 and 0.
                "RUN-0123456789ABCDEFGHIJKLMNOP"}) {
            assertThatThrownBy(() -> CommonOptions.requireUsableRunId(unusable))
                    .as("run id %s should have been refused", unusable)
                    .isInstanceOf(HarnessException.class)
                    .hasMessageContaining("ULID");
        }
    }

    @Test
    @DisplayName("the refusal is a structured refusal, not a crash")
    void refusalIsStructured() {
        HarnessException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                HarnessException.class, () -> CommonOptions.requireUsableRunId("RUN-FX-1"));
        assertThat(thrown.exitCode()).isEqualTo(ExitCode.STRUCTURED_REFUSAL);
    }
}
