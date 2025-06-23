package net.botwithus.tasks;

import net.botwithus.rs3.script.ScriptConsole;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QuestDialogFetcher class for retrieving quest steps and dialog options from RuneScape Wiki.
 * Parses quick guide pages to extract structured quest steps with associated dialog choices.
 */
public class QuestDialogFetcher {
    
    private static final String WIKI_BASE_URL = "https://runescape.wiki/w/";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    
    /**
     * Represents a dialog choice with its option number and text.
     */
    public static class DialogOption {
        private final String optionNumber;
        private final String optionText;
        
        public DialogOption(String optionNumber, String optionText) {
            this.optionNumber = optionNumber;
            this.optionText = optionText;
        }
        
        public String getOptionNumber() {
            return optionNumber;
        }
        
        public String getOptionText() {
            return optionText;
        }
        
        @Override
        public String toString() {
            return optionNumber + ": " + optionText;
        }
    }
    
    /**
     * Represents a sequence of dialog options for a particular conversation.
     */
    public static class DialogSequence {
        private final List<DialogOption> options;
        private final String context;
        
        public DialogSequence(String context) {
            this.context = context;
            this.options = new ArrayList<>();
        }
        
        public void addOption(DialogOption option) {
            options.add(option);
        }
        
        public List<DialogOption> getOptions() {
            return new ArrayList<>(options);
        }
        
        public String getContext() {
            return context;
        }
        
        public String getSequenceString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < options.size(); i++) {
                if (i > 0) sb.append(" → ");
                sb.append(options.get(i).getOptionNumber());
            }
            return sb.toString();
        }
        
        @Override
        public String toString() {
            return "Context: " + context + " | Sequence: " + getSequenceString();
        }
    }
    
    /**
     * Represents a single quest step with its text and associated dialog options.
     */
    public static class QuestStep {
        private final String stepText;
        private final List<DialogSequence> dialogs;
        private boolean completed;
        
        public QuestStep(String stepText) {
            this.stepText = cleanStepText(stepText);
            this.dialogs = new ArrayList<>();
            this.completed = false;
        }
        
        public void addDialog(DialogSequence dialog) {
            dialogs.add(dialog);
        }
        
        public String getStepText() {
            return stepText;
        }
        
        public List<DialogSequence> getDialogs() {
            return new ArrayList<>(dialogs);
        }
        
        public boolean isCompleted() {
            return completed;
        }
        
        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
        
        public boolean hasDialogs() {
            return !dialogs.isEmpty();
        }
        
        private String cleanStepText(String text) {
            if (text == null) return "";
            
            // Remove HTML tags and entities
            String cleaned = text.replaceAll("<[^>]+>", "")
                               .replace("&quot;", "\"")
                               .replace("&amp;", "&")
                               .replace("&lt;", "<")
                               .replace("&gt;", ">")
                               .replace("&#39;", "'")
                               .replace("&nbsp;", " ")
                               .replace("&#x2713;", "✓")
                               .replace("&#8226;", "•")
                               .replace("&#8230;", "...")
                               .replace("&#x27;", "'")
                               .replace("&#x3A;", ":")
                               .replace("&#91;", "[")
                               .replace("&#93;", "]")
                               .replace("&lsquo;", "'")
                               .replace("&rsquo;", "'")
                               .replace("&ldquo;", "\"")
                               .replace("&rdquo;", "\"")
                               .replace("&hellip;", "...")
                               .replace("&mdash;", "—")
                               .replace("&ndash;", "–")
                               .replaceAll("&#x([0-9A-Fa-f]+);", "")
                               .replaceAll("&#([0-9]+);", "")
                               .replaceAll("\\s+", " ")
                               .trim();
            
            return cleaned;
        }
        
        @Override
        public String toString() {
            return stepText + (hasDialogs() ? " [Has Dialogs]" : "");
        }
    }
    
    /**
     * Represents a quest section containing multiple steps.
     */
    public static class QuestSection {
        private final String sectionName;
        private final List<QuestStep> steps;
        
        public QuestSection(String sectionName) {
            this.sectionName = sectionName;
            this.steps = new ArrayList<>();
        }
        
        public void addStep(QuestStep step) {
            steps.add(step);
        }
        
        public String getSectionName() {
            return sectionName;
        }
        
        public List<QuestStep> getSteps() {
            return new ArrayList<>(steps);
        }
        
        public int getTotalSteps() {
            return steps.size();
        }
        
        public int getCompletedSteps() {
            return (int) steps.stream().filter(QuestStep::isCompleted).count();
        }
        
        @Override
        public String toString() {
            return sectionName + " (" + getCompletedSteps() + "/" + getTotalSteps() + " completed)";
        }
    }
    
    /**
     * Represents the complete quest guide with all sections and steps.
     */
    public static class QuestGuide {
        private final String questName;
        private final List<QuestSection> sections;
        
        public QuestGuide(String questName) {
            this.questName = questName;
            this.sections = new ArrayList<>();
        }
        
        public void addSection(QuestSection section) {
            sections.add(section);
        }
        
        public String getQuestName() {
            return questName;
        }
        
        public List<QuestSection> getSections() {
            return new ArrayList<>(sections);
        }
        
        public List<QuestStep> getAllSteps() {
            List<QuestStep> allSteps = new ArrayList<>();
            for (QuestSection section : sections) {
                allSteps.addAll(section.getSteps());
            }
            return allSteps;
        }
        
        public List<DialogSequence> getDialogsForIncompleteSteps() {
            List<DialogSequence> dialogs = new ArrayList<>();
            for (QuestStep step : getAllSteps()) {
                if (!step.isCompleted()) {
                    dialogs.addAll(step.getDialogs());
                }
            }
            return dialogs;
        }
        
        public int getTotalSteps() {
            return getAllSteps().size();
        }
        
        public int getCompletedSteps() {
            return (int) getAllSteps().stream().filter(QuestStep::isCompleted).count();
        }
        
        @Override
        public String toString() {
            return questName + " (" + getCompletedSteps() + "/" + getTotalSteps() + " steps completed)";
        }
    }

    /**
     * Fetches quest guide with structured steps and dialogs from the RuneScape Wiki.
     * 
     * @param questName The name of the quest (will be converted to wiki URL format)
     * @return QuestGuide containing structured quest data with steps and dialogs
     */
    public static QuestGuide fetchQuestGuide(String questName) {
        QuestGuide guide = new QuestGuide(questName);
        
        try {
            String wikiUrl = buildWikiUrl(questName);
            ScriptConsole.println("[QuestDialogFetcher] Fetching quest guide from: " + wikiUrl);
            
            String htmlContent = fetchHtmlContent(wikiUrl);
            if (htmlContent == null) {
                ScriptConsole.println("[QuestDialogFetcher] Failed to fetch content for: " + questName);
                return guide;
            }
            
            parseQuestGuide(htmlContent, guide);
            ScriptConsole.println("[QuestDialogFetcher] Parsed " + guide.getSections().size() + " sections with " + guide.getTotalSteps() + " total steps for: " + questName);
            
        } catch (Exception e) {
            ScriptConsole.println("[QuestDialogFetcher] Error fetching quest guide for " + questName + ": " + e.getMessage());
        }
        
        return guide;
    }
    
    /**
     * Parses the HTML content to extract quest sections and steps.
     */
    private static void parseQuestGuide(String htmlContent, QuestGuide guide) {
        // Pattern to match quest sections (h2 headers)
        Pattern sectionPattern = Pattern.compile(
            "<h2><span class=\"mw-headline\"[^>]*id=\"([^\"]+)\"[^>]*>([^<]+)</span>.*?</h2>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        
        // Pattern to match checklist content after sections
        Pattern checklistPattern = Pattern.compile(
            "<div class=\"lighttable checklist\"[^>]*>.*?<ul>(.*?)</ul>.*?</div>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        
        // Pattern to match individual list items (steps)
        Pattern stepPattern = Pattern.compile(
            "<li>(.*?)</li>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        
        ScriptConsole.println("[QuestDialogFetcher] Starting to parse quest guide...");
        
        Matcher sectionMatcher = sectionPattern.matcher(htmlContent);
        int lastSectionEnd = 0;
        
        while (sectionMatcher.find()) {
            String sectionId = sectionMatcher.group(1);
            String sectionName = cleanOptionText(sectionMatcher.group(2));
            int sectionStart = sectionMatcher.start();
            int sectionEnd = sectionMatcher.end();
            
            ScriptConsole.println("[QuestDialogFetcher] Found section: " + sectionName + " (ID: " + sectionId + ")");
            
            // Find the next section or end of content
            String sectionContent;
            Matcher nextSectionMatcher = sectionPattern.matcher(htmlContent);
            nextSectionMatcher.region(sectionEnd, htmlContent.length());
            
            if (nextSectionMatcher.find()) {
                sectionContent = htmlContent.substring(sectionEnd, nextSectionMatcher.start());
            } else {
                sectionContent = htmlContent.substring(sectionEnd);
            }
            
            QuestSection section = new QuestSection(sectionName);
            
            // Find checklist in this section
            Matcher checklistMatcher = checklistPattern.matcher(sectionContent);
            if (checklistMatcher.find()) {
                String checklistContent = checklistMatcher.group(1);
                ScriptConsole.println("[QuestDialogFetcher] Found checklist for section: " + sectionName);
                
                // Parse individual steps
                Matcher stepMatcher = stepPattern.matcher(checklistContent);
                while (stepMatcher.find()) {
                    String stepContent = stepMatcher.group(1);
                    QuestStep step = new QuestStep(stepContent);
                    
                    // Parse dialogs within this step
                    parseDialogsInStep(stepContent, step);
                    
                    section.addStep(step);
                    ScriptConsole.println("[QuestDialogFetcher] Added step: " + step.getStepText().substring(0, Math.min(50, step.getStepText().length())) + "..." + (step.hasDialogs() ? " [Has Dialogs]" : ""));
                }
            }
            
            if (section.getTotalSteps() > 0) {
                guide.addSection(section);
            }
            
            lastSectionEnd = sectionEnd;
        }
    }
    
    /**
     * Parses dialog options within a specific step.
     */
    private static void parseDialogsInStep(String stepContent, QuestStep step) {
        // Pattern for chat options with tooltip
        Pattern chatOptionsPattern = Pattern.compile(
            "<span class=\"chat-options\">.*?data-tooltip-name=\"([^\"]+)\".*?</span>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        
        // Pattern for tooltip content
        Pattern tooltipPattern = Pattern.compile(
            "<div[^>]*data-tooltip-for=\"([^\"]+)\"[^>]*>.*?<table><tbody>(.*?)</tbody></table>.*?</div>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        
        // Pattern for table rows in tooltip
        Pattern tableRowPattern = Pattern.compile(
            "<tr><td><b>([^<]+)</b></td><td>([^<]+)</td></tr>",
            Pattern.CASE_INSENSITIVE
        );
        
        // Pattern for sequence display
        Pattern sequenceDisplayPattern = Pattern.compile(
            "<span class=\"chat-options-underline\"[^>]*>([^<]+)</span>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher chatMatcher = chatOptionsPattern.matcher(stepContent);
        while (chatMatcher.find()) {
            String tooltipName = chatMatcher.group(1);
            String chatOptionsContent = chatMatcher.group(0);
            
            // Extract sequence display
            List<String> sequenceParts = new ArrayList<>();
            Matcher seqMatcher = sequenceDisplayPattern.matcher(chatOptionsContent);
            while (seqMatcher.find()) {
                String part = seqMatcher.group(1).trim();
                part = cleanOptionText(part);
                if (!part.isEmpty()) {
                    sequenceParts.add(part);
                }
            }
            
            String sequenceDisplay = String.join(" → ", sequenceParts);
            
            // Find matching tooltip in the full step content
            Matcher tooltipMatcher = tooltipPattern.matcher(stepContent);
            while (tooltipMatcher.find()) {
                String divTooltipName = tooltipMatcher.group(1);
                
                if (divTooltipName.equals(tooltipName)) {
                    String tableContent = tooltipMatcher.group(2);
                    
                    DialogSequence dialogSeq = new DialogSequence("Step dialog: " + sequenceDisplay);
                    
                    Matcher rowMatcher = tableRowPattern.matcher(tableContent);
                    while (rowMatcher.find()) {
                        String optionNum = rowMatcher.group(1).trim();
                        String optionText = rowMatcher.group(2).trim();
                        
                        optionNum = cleanOptionText(optionNum);
                        optionText = cleanOptionText(optionText);
                        
                        if (optionNum.equals("?") || optionNum.isEmpty()) {
                            continue;
                        }
                        
                        dialogSeq.addOption(new DialogOption(optionNum, optionText));
                    }
                    
                    if (!dialogSeq.getOptions().isEmpty()) {
                        step.addDialog(dialogSeq);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Builds the wiki URL for a quest's quick guide.
     */
    private static String buildWikiUrl(String questName) {
        String formattedName = questName.replace(" ", "_")
                                       .replace("'", "%27")
                                       .replace(":", "%3A");
        return WIKI_BASE_URL + formattedName + "/Quick_guide";
    }
    
    /**
     * Fetches HTML content from the given URL.
     */
    private static String fetchHtmlContent(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                ScriptConsole.println("[QuestDialogFetcher] HTTP " + responseCode + " for URL: " + urlString);
                return null;
            }
            
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            
            return content.toString();
            
        } catch (Exception e) {
            ScriptConsole.println("[QuestDialogFetcher] Error fetching HTML: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Cleans up option text by removing HTML entities and extra whitespace.
     */
    private static String cleanOptionText(String text) {
        if (text == null) return "";
        
        return text.replace("&quot;", "\"")
                  .replace("&amp;", "&")
                  .replace("&lt;", "<")
                  .replace("&gt;", ">")
                  .replace("&#39;", "'")
                  .replace("&nbsp;", " ")
                  .replace("&#x2713;", "✓")
                  .replace("&#8226;", "•")
                  .replace("&#8230;", "...")
                  .replace("&#x27;", "'")
                  .replace("&#x3A;", ":")
                  .replace("&#91;", "[")
                  .replace("&#93;", "]")
                  .replace("&lsquo;", "'")
                  .replace("&rsquo;", "'")
                  .replace("&ldquo;", "\"")
                  .replace("&rdquo;", "\"")
                  .replace("&hellip;", "...")
                  .replace("&mdash;", "—")
                  .replace("&ndash;", "–")
                  .replaceAll("&#x([0-9A-Fa-f]+);", "")
                  .replaceAll("&#([0-9]+);", "")
                  .replaceAll("\\s+", " ")
                  .trim();
    }
    
    /**
     * Formats quest guide information for display.
     */
    public static String formatQuestGuide(QuestGuide guide) {
        if (guide.getSections().isEmpty()) {
            return "No quest guide found for: " + guide.getQuestName();
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Quest Guide for ").append(guide.getQuestName()).append(":\n");
        sb.append("Progress: ").append(guide.getCompletedSteps()).append("/").append(guide.getTotalSteps()).append(" steps completed\n\n");
        
        for (QuestSection section : guide.getSections()) {
            sb.append("=== ").append(section.getSectionName()).append(" ===\n");
            sb.append("Section Progress: ").append(section.getCompletedSteps()).append("/").append(section.getTotalSteps()).append(" steps completed\n");
            
            for (int i = 0; i < section.getSteps().size(); i++) {
                QuestStep step = section.getSteps().get(i);
                sb.append((i + 1)).append(". ");
                sb.append(step.isCompleted() ? "[✓] " : "[ ] ");
                sb.append(step.getStepText()).append("\n");
                
                if (step.hasDialogs()) {
                    sb.append("   Dialog Options:\n");
                    for (DialogSequence dialog : step.getDialogs()) {
                        sb.append("   - ").append(dialog.getContext()).append("\n");
                        for (DialogOption option : dialog.getOptions()) {
                            sb.append("     ").append(option.toString()).append("\n");
                        }
                    }
                }
                sb.append("\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Test method to demonstrate new functionality.
     */
    public static void testFetchGuide(String questName) {
        ScriptConsole.println("[QuestDialogFetcher] Testing quest guide fetch for: " + questName);
        QuestGuide guide = fetchQuestGuide(questName);
        String formatted = formatQuestGuide(guide);
        ScriptConsole.println(formatted);
    }

    /**
     * Legacy method: Fetches quest dialog options as a simple list of strings.
     * This method extracts dialog options from the quest guide for backward compatibility.
     * 
     * @param questName The name of the quest
     * @return List of dialog option strings
     */
    public static List<String> fetchQuestDialogs(String questName) {
        List<String> dialogOptions = new ArrayList<>();
        
        try {
            QuestGuide guide = fetchQuestGuide(questName);
            
            if (guide != null && !guide.getSections().isEmpty()) {
                for (QuestSection section : guide.getSections()) {
                    for (QuestStep step : section.getSteps()) {
                        for (DialogSequence sequence : step.getDialogs()) {
                            for (DialogOption option : sequence.getOptions()) {
                                if (option.getOptionText() != null && !option.getOptionText().trim().isEmpty()) {
                                    dialogOptions.add(option.getOptionText());
                                }
                            }
                        }
                    }
                }
            }
            
            ScriptConsole.println("[QuestDialogFetcher] Extracted " + dialogOptions.size() + " dialog options from quest guide for: " + questName);
            
        } catch (Exception e) {
            ScriptConsole.println("[QuestDialogFetcher] Error fetching legacy dialog options for " + questName + ": " + e.getMessage());
        }
        
        return dialogOptions;
    }
} 