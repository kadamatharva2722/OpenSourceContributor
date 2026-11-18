package com.osc.backend;

import com.osc.backend.aiusage.repository.AiUsageRepository;
import com.osc.backend.contribution.repository.ContributionRepository;
import com.osc.backend.savedissue.repository.SavedIssueRepository;
import com.osc.backend.user.repository.UserRepository;
import com.osc.backend.userpreference.repository.UserPreferenceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class BackendApplicationTests {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserPreferenceRepository userPreferenceRepository;

    @MockBean
    private SavedIssueRepository savedIssueRepository;

    @MockBean
    private ContributionRepository contributionRepository;

    @MockBean
    private AiUsageRepository aiUsageRepository;

    @Test
    @DisplayName("Context loads successfully verifying component wiring and security configuration")
    void contextLoads() {
    }

}
