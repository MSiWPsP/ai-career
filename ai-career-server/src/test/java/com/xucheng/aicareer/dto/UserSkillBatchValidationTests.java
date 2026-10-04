package com.xucheng.aicareer.dto;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class UserSkillBatchValidationTests {
    @Test
    void emptyReplacementIsValidButMissingCollectionIsRejected() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var request = new UserSkillBatchDTO();
            assertThat(validator.validate(request)).isNotEmpty();
            request.setSkills(List.of());
            assertThat(validator.validate(request)).isEmpty();
        }
    }
}
