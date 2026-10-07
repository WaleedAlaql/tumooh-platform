package org.fadhel.tumoohplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyRecommendationResponse {
    private String major;
    private List<RecommendedCompanyResponse> recommendations;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedCompanyResponse {
        private String name;
        private String industry;
        private String reason;
        private String companyLogoUrl;
    }
}
