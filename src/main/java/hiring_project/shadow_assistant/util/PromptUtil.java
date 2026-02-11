package hiring_project.shadow_assistant.util;

public class PromptUtil {

    public static String getEliminatorPrompt(String jd, String resume) {
        return """
            You are a strict technical recruiter. Compare the following Resume against the Job Description.
            
            JOB DESCRIPTION:
            %s
            
            RESUME:
            %s
            
            CRITICAL SCORING RUBRIC:
            - If Match Score is 0-60: Verdict MUST be "REJECT"
            - If Match Score is 61-85: Verdict MUST be "MODERATE MATCH"
            - If Match Score is 86-100: Verdict MUST be "STRONG MATCH"
            
            Output ONLY a JSON object with these fields:
            - matchScore (integer 0-100)
            - summary (string, max 50 words)
            - missingSkills (list of strings, technical skills found in JD but missing in Resume)
            - verdict (string: Based strictly on the score range above)
            """.formatted(jd, resume);
    }

//    public static String getDepthDetectorPrompt(String question, String transcript) {
//        return """
//            You are a Technical Interview Auditor. Analyze the candidate's answer for depth and authenticity.
//
//            QUESTION ASKED: "%s"
//            CANDIDATE TRANSCRIPT: "%s"
//
//            SCORING CRITERIA:
//            1. Technical Score (0-100):
//               - High score if they mention trade-offs, edge cases, or specific experiences.
//               - Low score if they just give a dictionary definition or generic benefits (e.g., "it is scalable").
//            2. Communication Score (0-100): Clarity and flow.
//            3. Generic/Fake Detection:
//               - Set 'isGeneric' to TRUE if the answer sounds like a memorized ChatGPT output with no personal insight.
//
//            Output ONLY a JSON object matching this structure:
//            - technicalScore (int)
//            - communicationScore (int)
//            - isGeneric (boolean)
//            - analysis (string, max 30 words)
//            """.formatted(question, transcript);
//    }

    /**
     * Analyze a single interview answer for depth, quality, and technical accuracy
     *
     * @param question The interview question that was asked
     * @param answer The candidate's response (transcribed from speech)
     * @return Prompt string for AI to analyze the answer
     */
    public static String getDepthDetectorPrompt(String question, String answer) {
        return String.format("""
            You are an expert technical interviewer analyzing a candidate's response.
            
            QUESTION ASKED:
            %s
            
            CANDIDATE'S ANSWER:
            %s
            
            Analyze this answer across three dimensions:
            
            1. TECHNICAL SCORE (0-10):
               - How accurate and deep is their technical knowledge?
               - Do they demonstrate understanding of concepts?
               - Are there any technical errors or misconceptions?
               - 0 = completely wrong, 5 = surface level, 10 = expert level
            
            2. COMMUNICATION SCORE (0-10):
               - How clearly did they explain their answer?
               - Did they provide specific examples or evidence?
               - Is the answer well-structured and coherent?
               - 0 = incoherent, 5 = adequate, 10 = exceptionally clear
            
            3. IS GENERIC (boolean):
               - true if the answer is vague, templated, or lacks specifics
               - true if they're clearly copying common interview answers
               - false if the answer shows genuine understanding and experience
            
            4. ANALYSIS (string):
               - 2-3 sentences explaining your scores
               - Highlight both strengths and weaknesses
               - Be constructive but honest
            
            Return ONLY a valid JSON object in this exact format:
            {
              "technicalScore": 7,
              "communicationScore": 8,
              "isGeneric": false,
              "analysis": "The candidate demonstrates solid understanding of the concept with specific examples from their experience. However, they could have elaborated more on edge cases."
            }
            
            Do NOT include any markdown, code blocks, or additional text.
            """, question, answer);
    }


    // ... existing prompts ...

    public static String getQuestionGeneratorPrompt(String jd, String resume) {
        return """
            You are a Technical Interviewer. Generate 4 technical questions based on the Job Description and Resume.
            
            JOB DESCRIPTION:
            %s
            
            RESUME:
            %s
            
            RULES:
            1. Identify skills required in the JD but missing or weak in the Resume. Ask about these first.
            2. If the Resume claims a high-level skill (e.g., "System Design"), ask a scenario-based question to verify it.
            3. Do NOT ask generic HR questions like "Tell me about yourself." strictly technical.
            
            Output ONLY a JSON object:
            {
                "questions": ["Question 1...", "Question 2...", "Question 3..."],
                "focusArea": "The main technical themes on which these questions are based upon"
            }
            Do NOT include any markdown, code blocks, or additional text.
            """.formatted(jd, resume);
    }

    public static String getRecruiterSummaryPrompt(String interviewLog) {
        return String.format("""
            You are a senior technical recruiter reviewing a completed AI pre-interview.
            
            Below is the complete interview transcript with AI analysis for each answer:
            
            %s
            
            Based on this interview, provide:
            
            1. AVERAGE SCORE (0-100):
               - Holistic assessment of overall performance
               - Consider both technical depth and communication
               - Factor in consistency across all answers
               - Consider the number of generic/weak answers
           
            2. INTERVIEW SUMMARY (string):
               - Professional summary for the hiring manager
               - 3-4 sentences covering:
                 * Overall technical competency
                 * Communication skills
                 * Notable strengths
                 * Areas of concern (if any)
               - Be objective and specific
               - Avoid generic platitudes
            
            3. AI RECOMMENDATION (string):
               Must be EXACTLY one of:
               - "Highly Recommended" - Strong candidate, definitely interview
               - "Worth Interviewing" - Decent candidate, worth considering
               - "Not Recommended" - Weak performance, likely not a fit
            
            Scoring Guidelines:
            - 80-100: Highly Recommended (strong across the board)
            - 60-79: Worth Interviewing (solid with some weaknesses)
            - 0-59: Not Recommended (significant gaps or concerns)
            
            Consider red flags:
            - Multiple generic answers (suggests lack of real experience)
            - Low technical scores (fundamental knowledge gaps)
            - Poor communication (can't explain their work)
            
            Return ONLY a valid JSON object in this exact format:
            {
              "averageScore": 75,
              "interviewSummary": "Candidate demonstrates strong technical knowledge in backend development with clear communication. Provided specific examples from previous projects. Some answers lacked depth on system design topics, but overall shows promise for the role.",
              "aiRecommendation": "Worth Interviewing"
            }
            
            Do NOT include any markdown, code blocks, or additional text.
            """, interviewLog);
    }

    // ... existing prompts

    public static String getBiasAnalysisPrompt(String transcript) {
        return """
            You are a Diversity & Inclusion Officer auditing a job interview transcript.
            
            TRANSCRIPT:
            %s
            
            Analyze the RECRUITER'S questions and behavior for:
            1. UNCONSCIOUS BIAS: Are there assumptions based on gender, age, race, or background?
            2. ILLEGAL QUESTIONS: Did they ask about marital status, religion, kids, etc.?
            3. PROFESSIONALISM: Was the tone respectful?
            4. RELEVANCE: Did the questions stick to the Job Description skills?
            
            Output ONLY a JSON object:
            {
                "biasDetected": boolean,
                "biasScore": integer (0-100, where 0 is neutral/good, 100 is highly biased),
                "flaggedPhrases": ["phrase 1", "phrase 2"],
                "constructiveFeedback": "Recruiter interrupted candidate 3 times...",
                "legalRisk": "Low" | "Medium" | "High"
            }
            Do NOT include markdown.
            """.formatted(transcript);
    }

    // ... existing code ...

    // ⚡ REPLACED: Bias Prompt -> Insights Prompt
    public static String getInterviewInsightsPrompt(String transcript) {
        return """
            You are an expert Technical Recruiter Assistant. 
            Analyze the following interview transcript between a Recruiter and a Candidate.
            
            TRANSCRIPT:
            %s
            
            Your goal is to generate structured interview notes to save the recruiter time.
            
            Output ONLY a valid JSON object with this exact structure:
            {
                "executiveSummary": "A professional 3-4 sentence summary of the candidate's performance and fit.",
                "keyStrengths": ["List 3-5 specific technical or behavioral strengths demonstrated"],
                "areasForImprovement": ["List 2-3 areas where the candidate struggled or lacked depth"],
                "topicsDiscussed": ["List of technical topics covered, e.g., 'Spring Boot', 'System Design'"],
                "suggestedRating": integer (0-100, overall assessment score)
            }
            
            Do NOT include markdown, code blocks, or explanations. Return only the JSON.
            """.formatted(transcript);
    }

    public static String getSkillAnalysisPrompt(String resumeText) {
        return """
            You are a Career Coach AI. Analyze the following resume text and provide a structured skill analysis.
            
            RESUME TEXT:
            %s
            
            OUTPUT FORMAT (Strict JSON, no markdown):
            {
              "skills": [
                {"name": "Java", "category": "Backend", "proficiency": 85},
                {"name": "React", "category": "Frontend", "proficiency": 70}
              ],
              "strengths": [
                {"title": "System Design", "description": "Strong evidence of architecting scalable microservices."}
              ],
              "growthAreas": [
                {"title": "Cloud Native", "description": "Limited experience with Kubernetes/Docker orchestration found."}
              ],
              "careerMatches": [
                {"role": "Senior Backend Engineer", "match": 80},
                {"role": "Full Stack Developer", "match": 75}
                {"role": {Job Role}, "match": {match score}}
                {"role": {Job Role}, "match": {match score}}
                {"role": {Job Role}, "match": {match score}}
              ],
              "radarChartData": [
                {"category": "Technical", "value": 85},
                {"category": "Communication", "value": 70},
                {"category": "Leadership", "value": 60},
                {"category": "Problem Solving", "value": 80},
                {"category": "System Design", "value": 75}
              ]
            }
            ** The output format is just for your reference, you don't have to copy it as it is. **
            
            Analyze the candidate's actual experience to determine proficiency (0-100).
            For 'radarChartData', estimate values for these 5 categories based on project complexity and roles.
            """.formatted(resumeText);
    }
}