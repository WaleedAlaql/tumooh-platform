package org.fadhel.tumoohplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fadhel.tumoohplatform.Api.ApiException;
import org.fadhel.tumoohplatform.dto.out.CompanyRecommendationResponse;
import org.fadhel.tumoohplatform.dto.out.ProfileResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyRecommendationService {

    private static final String COMPANIES_PATH = "data/saudi_companies.json";
    private static final int SAMPLE_SIZE = 35;
    private static final String PLACEHOLDER_LOGO = "/images/logo-placeholder.png";

    private final ProfileService profileService;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    private Map<String, String> logoByNormalizedName;

    public CompanyRecommendationResponse getCompanyRecommendationsForUser(Long userId) {
        ProfileResponse profile;
        try {
            profile = profileService.getProfileByUserId(userId);
        } catch (ApiException e) {
            throw new ApiException("Create a profile with a major before requesting company recommendations.");
        }

        String major = profile.getMajor();
        if (major == null || major.isBlank()) {
            throw new ApiException("Profile major is required for company recommendations.");
        }

        String companySample = loadCompanySampleBlock();
        String prompt = String.format("""
            You are a career advisor for graduates in Saudi Arabia.
            Student major: %s
            Skills (if any): %s

            From this sample list of Saudi companies (name and industry), pick exactly 5 that best fit the student's major \
            and explain briefly why each is a good match. Prefer tech and software roles when the major is CS-related.

            Companies sample:
            %s

            Return ONLY valid JSON (no markdown):
            {
              "recommendations": [
                {"name": "Company Name", "industry": "Industry", "reason": "One sentence why it fits the major."}
              ]
            }
            """,
                major,
                profile.getSkills() != null ? profile.getSkills() : "Not specified",
                companySample);

        try {
            String raw = geminiService.generateText(prompt);
            String clean = raw.replaceAll("```json|```", "").trim();
            int start = clean.indexOf('{');
            int end = clean.lastIndexOf('}');
            if (start >= 0 && end > start) {
                clean = clean.substring(start, end + 1);
            }
            JsonNode root = objectMapper.readTree(clean);
            JsonNode recs = root.get("recommendations");
            List<CompanyRecommendationResponse.RecommendedCompanyResponse> list = new ArrayList<>();
            if (recs != null && recs.isArray()) {
                for (JsonNode node : recs) {
                    String name = textOrEmpty(node, "name");
                    list.add(new CompanyRecommendationResponse.RecommendedCompanyResponse(
                            name,
                            textOrEmpty(node, "industry"),
                            textOrEmpty(node, "reason"),
                            resolveCompanyLogoUrl(name)));
                }
            }
            return new CompanyRecommendationResponse(major, list);
        } catch (Exception e) {
            log.error("Company recommendation failed for user {}", userId, e);
            throw new ApiException(
                    "Could not generate company recommendations. Check your Gemini API key, model, and network, then try again.");
        }
    }

    private String loadCompanySampleBlock() {
        try (InputStream in = new ClassPathResource(COMPANIES_PATH).getInputStream()) {
            JsonNode array = objectMapper.readTree(in);
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (JsonNode company : array) {
                if (count >= SAMPLE_SIZE) {
                    break;
                }
                sb.append("- ")
                        .append(textOrEmpty(company, "nameEn"))
                        .append(" (")
                        .append(textOrEmpty(company, "industryEn"))
                        .append(")\n");
                count++;
            }
            return sb.toString();
        } catch (Exception e) {
            throw new ApiException("Company catalog is unavailable.");
        }
    }

    private static String textOrEmpty(JsonNode node, String field) {
        JsonNode val = node.get(field);
        return val != null && !val.isNull() ? val.asText() : "";
    }

    private String resolveCompanyLogoUrl(String recommendedName) {
        if (recommendedName == null || recommendedName.isBlank()) {
            return PLACEHOLDER_LOGO;
        }
        ensureLogoIndex();
        String key = normalizeCompanyName(recommendedName);
        String direct = logoByNormalizedName.get(key);
        if (direct != null) {
            return direct;
        }
        for (Map.Entry<String, String> entry : logoByNormalizedName.entrySet()) {
            if (entry.getKey().contains(key) || key.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return PLACEHOLDER_LOGO;
    }

    private void ensureLogoIndex() {
        if (logoByNormalizedName != null) {
            return;
        }
        logoByNormalizedName = new HashMap<>();
        try (InputStream in = new ClassPathResource(COMPANIES_PATH).getInputStream()) {
            JsonNode array = objectMapper.readTree(in);
            for (JsonNode company : array) {
                String logo = textOrEmpty(company, "companyLogoUrl");
                if (logo.isBlank()) {
                    logo = PLACEHOLDER_LOGO;
                }
                String nameEn = textOrEmpty(company, "nameEn");
                if (!nameEn.isBlank()) {
                    logoByNormalizedName.put(normalizeCompanyName(nameEn), logo);
                }
                JsonNode aliases = company.get("aliases");
                if (aliases != null && aliases.isArray()) {
                    for (JsonNode alias : aliases) {
                        if (!alias.isNull() && !alias.asText().isBlank()) {
                            logoByNormalizedName.putIfAbsent(normalizeCompanyName(alias.asText()), logo);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logoByNormalizedName = Map.of();
        }
    }

    private static String normalizeCompanyName(String name) {
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
