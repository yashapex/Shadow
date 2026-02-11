package hiring_project.shadow_assistant.dto.ai;

import lombok.Data;
import java.util.List;

@Data
public class SkillAnalysisResponse {
    private List<SkillProficiency> skills;
    private List<Strength> strengths;
    private List<GrowthArea> growthAreas;
    private List<CareerMatch> careerMatches;
    private List<RadarChartData> radarChartData;

    @Data
    public static class SkillProficiency {
        private String name;
        private String category;
        private int proficiency; // 0-100
    }

    @Data
    public static class Strength {
        private String title;
        private String description;
    }

    @Data
    public static class GrowthArea {
        private String title;
        private String description;
    }

    @Data
    public static class CareerMatch {
        private String role;
        private int match; // 0-100
    }

    @Data
    public static class RadarChartData {
        private String category;
        private int value; // 0-100
    }
}