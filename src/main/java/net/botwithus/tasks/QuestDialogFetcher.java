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
        
        /**
         * Gets clean step text without any dialog information for GUI display.
         * This removes all dialog-related content and formatting.
         */
        public String getCleanStepText() {
            if (stepText == null) return "";
            
            String cleaned = stepText;
            
            // Remove all dialog-related patterns more aggressively
            cleaned = cleaned
                    // Remove any text that mentions "Dialog:" or "dialog:"
                    .replaceAll("(?i)dialog[^\\n]*", "")
                    // Remove numbered option patterns like "1. Option text" or "? ? ? ?"
                    .replaceAll("\\d+\\.\\s*[^\\n]*", "")
                    .replaceAll("[?\\d\\s]{3,}", "")
                    // Remove patterns like "Choose option X" or "Select option Y"
                    .replaceAll("(?i)(choose|select|pick)\\s+(option|choice)\\s*\\d*[^\\n]*", "")
                    // Remove any remaining dialog instruction patterns
                    .replaceAll("(?i)(say|tell|ask|answer|respond)[^\\n]*", "")
                    // Remove option number patterns at start of lines
                    .replaceAll("(?m)^\\s*\\d+[.:)]\\s*", "")
                    // Remove question mark sequences
                    .replaceAll("\\?+\\s*", "")
                    // Clean up whitespace and empty lines
                    .replaceAll("\\s+", " ")
                    .replaceAll("(\\s*[.,;:]\\s*)+", ". ")
                    .trim();
            
            // If the cleaned text is too short or empty, return the original step text
            if (cleaned.length() < 5) {
                cleaned = stepText.replaceAll("(?i)dialog[^\\n]*", "").trim();
            }
            
            // Ensure proper capitalization and punctuation
            if (!cleaned.isEmpty()) {
                cleaned = Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
                if (!cleaned.matches(".*[.!?]\\s*$")) {
                    cleaned += ".";
                }
            }
            
            return cleaned;
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
            
            cleaned = improveTextReadability(cleaned);
            
            return cleaned;
        }
        
        /**
         * Improves text readability by adding proper punctuation, spacing, and formatting.
         */
        private String improveTextReadability(String text) {
            if (text == null || text.trim().isEmpty()) {
                return text;
            }

            text = text
                      .replaceAll("Dialog:\\s*Step\\s*dialog:[^\\n]*", "")
                      .replaceAll("Dialog:\\s*[^\\n]*", "")
                      .replaceAll("(?m)^\\s*Dialog:.*$", "")
                      .replaceAll("[\\d?\\s]{5,}", "")
                      .replaceAll("[?\\s]{3,}", "")
                      .replaceAll("\\bStep\\s+dialog\\b[^\\n]*", "");

            text = text.replaceAll("\\b(go|walk|run|move|travel|head|proceed)\\s+(to|towards|into|through|up|down|north|south|east|west)", 
                                 "$1 to")
                      .replaceAll("\\b(click|select|choose|pick)\\s+(on|the)\\s+", "click on ")
                      .replaceAll("\\b(use|equip|wield)\\s+(the|your)\\s+", "use ")
                      .replaceAll("\\b(open|close|examine|search)\\s+(the)\\s+", "$1 the ");
            
            text = text.replaceAll("\\b(enter|exit|leave)\\s+(the)\\s+", "$1 the ")
                      .replaceAll("\\b(climb|ascend|descend)\\s+(the|up|down)\\s+", "$1 ")
                      .replaceAll("\\b(bank|teleport|transport)\\s+(to|at)\\s+", "$1 to ");
            
            text = text.replaceAll("\\b(take|get|obtain|collect|gather)\\s+(the|a|an)\\s+", "take ")
                      .replaceAll("\\b(drop|destroy|discard)\\s+(the|your)\\s+", "drop ")
                      .replaceAll("\\b(equip|wear|wield)\\s+(the|your)\\s+", "equip ");
            
            text = text.replaceAll("\\b(kill|defeat|fight|attack)\\s+(the|a|an)\\s+", "kill ")
                      .replaceAll("\\b(mine|fish|cut|cook|craft|smith)\\s+(the|a|an)\\s+", "$1 ")
                      .replaceAll("\\b(level|levels?)\\s+(\\d+)\\s+(\\w+)", "level $2 $3");
            
            text = text.replaceAll("([.!?])([A-Z])", "$1 $2")
                      .replaceAll("([,;:])([A-Za-z])", "$1 $2");
            
            text = text.replaceAll("\\((\\d+),\\s*(\\d+)\\)", "($1, $2)")
                      .replaceAll("\\((\\d+),\\s*(\\d+),\\s*(\\d+)\\)", "($1, $2, $3)");
            
            text = text.replaceAll("\\b(then)\\s+", "then ")
                      .replaceAll("\\b(and)\\s+", "and ")
                      .replaceAll("\\b(or)\\s+", "or ")
                      .replaceAll("\\b(but)\\s+", "but ")
                      .replaceAll("\\b(if)\\s+", "if ")
                      .replaceAll("\\b(when)\\s+", "when ")
                      .replaceAll("\\b(after)\\s+", "after ")
                      .replaceAll("\\b(before)\\s+", "before ");
            
            text = text.replaceAll("\\b(npc|NPC)\\b", "NPC")
                      .replaceAll("\\b(hp|HP)\\b", "HP")
                      .replaceAll("\\b(xp|XP|exp|EXP)\\b", "XP")
                      .replaceAll("\\b(gp|GP)\\b", "GP")
                      .replaceAll("\\b(poh|POH)\\b", "POH")
                      .replaceAll("\\b(ge|GE)\\b", "Grand Exchange")
                      .replaceAll("\\b(tele|teleport)\\b", "teleport");
            
            text = text.replaceAll("\\b(\\d+)\\s*x\\s*(\\w+)", "$1 $2")
                      .replaceAll("\\b(\\d+)\\s+(coins?|gp|GP)\\b", "$1 GP");
            
            text = text.replaceAll("\\s*-\\s*", " - ")
                      .replaceAll("\\s*→\\s*", " → ")
                      .replaceAll("\\s*/\\s*", "/");

            text = text.replaceAll("\\s+", " ")
                      .replaceAll("(\\s*\\.\\s*)+", ". ")
                      .replaceAll("^[\\s.,]+", "")
                      .trim();
            
            if (text.trim().isEmpty()) {
                return "";
            }
            
            if (!text.matches(".*[.!?]\\s*$") && text.length() > 0) {
                text = text.trim() + ".";
            }
            
            if (text.length() > 0 && Character.isLowerCase(text.charAt(0))) {
                text = Character.toUpperCase(text.charAt(0)) + text.substring(1);
            }
            
            return text;
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
        Pattern sectionPattern = Pattern.compile(
            "<h2><span class=\"mw-headline\"[^>]*id=\"([^\"]+)\"[^>]*>([^<]+)</span>.*?</h2>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );

        ScriptConsole.println("[QuestDialogFetcher] Starting to parse quest guide...");

        Matcher sectionMatcher = sectionPattern.matcher(htmlContent);

        while (sectionMatcher.find()) {
            String sectionId = sectionMatcher.group(1);
            String sectionName = cleanSectionName(sectionMatcher.group(2));
            int sectionEnd = sectionMatcher.end();

            String sectionContent;
            Matcher nextSectionMatcher = sectionPattern.matcher(htmlContent);
            nextSectionMatcher.region(sectionEnd, htmlContent.length());
            if (nextSectionMatcher.find()) {
                sectionContent = htmlContent.substring(sectionEnd, nextSectionMatcher.start());
            } else {
                sectionContent = htmlContent.substring(sectionEnd);
            }

            ScriptConsole.println("[QuestDialogFetcher] Found section: " + sectionName + " (ID: " + sectionId + ")");
            QuestSection section = new QuestSection(sectionName);

            int searchIdx = 0;
            String checklistMarker = "<div class=\"lighttable checklist";
            while (searchIdx < sectionContent.length()) {
                int divIdx = sectionContent.indexOf(checklistMarker, searchIdx);
                if (divIdx == -1) break;

                int divOpenEnd = sectionContent.indexOf('>', divIdx);
                if (divOpenEnd == -1) break;

                String divFragment = sectionContent.substring(divOpenEnd + 1);
                String checklistContent = extractOuterUlContent(divFragment);
                if (checklistContent != null && !checklistContent.isEmpty()) {
                    parseStepsFromChecklist(checklistContent, section);
                }

                int closingDiv = sectionContent.indexOf("</div>", divOpenEnd);
                searchIdx = closingDiv != -1 ? closingDiv + 6 : divOpenEnd + 1;
            }

            if (section.getTotalSteps() > 0) {
                guide.addSection(section);
            }
        }
    }
    
    /**
     * Extracts the top-level <ul>...</ul> block from the provided HTML fragment and returns its inner HTML.
     * Uses tag depth tracking so nested lists do not prematurely terminate extraction.
     */
    private static String extractOuterUlContent(String html) {
        if (html == null) return "";
        int ulStart = html.indexOf("<ul");
        if (ulStart == -1) return "";

        int openTagEnd = html.indexOf('>', ulStart);
        if (openTagEnd == -1) return "";

        int depth = 1;
        int index = openTagEnd + 1;
        Pattern tagPattern = Pattern.compile("<ul[^>]*>|</ul>", Pattern.CASE_INSENSITIVE);
        Matcher tagMatcher = tagPattern.matcher(html);
        tagMatcher.region(index, html.length());

        while (tagMatcher.find()) {
            String tag = tagMatcher.group().toLowerCase();
            if (tag.startsWith("<ul")) {
                depth++;
            } else { 
                depth--;
                if (depth == 0) {
                    return html.substring(openTagEnd + 1, tagMatcher.start());
                }
            }
        }
        return "";
    }
    
    /**
     * Parses steps from checklist content using improved regex parsing.
     */
    private static void parseStepsFromChecklist(String checklistContent, QuestSection section) {
        ScriptConsole.println("[QuestDialogFetcher] Parsing checklist for section: " + section.getSectionName());
        ScriptConsole.println("[QuestDialogFetcher] Original content length: " + checklistContent.length());
        
        String cleanedContent = removeNestedUlBlocks(checklistContent);
        ScriptConsole.println("[QuestDialogFetcher] Cleaned content length: " + cleanedContent.length());
        
        Pattern liPattern = Pattern.compile("<li>(.*?)</li>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = liPattern.matcher(cleanedContent);
        
        int stepCount = 0;
        while (matcher.find()) {
            stepCount++;
            String stepContent = matcher.group(1);
            
            if (!stepContent.trim().isEmpty()) {
                String cleanStepText = extractCleanStepText(stepContent);
                
                QuestStep step = new QuestStep(cleanStepText);
                String originalStepHtml = extractOriginalStepHtml(checklistContent, stepContent, stepCount);
                parseDialogsInStep(originalStepHtml, step);
                
                section.addStep(step);
                
                String stepPreview = step.getStepText().length() > 100 ? 
                    step.getStepText().substring(0, 100) + "..." : 
                    step.getStepText();
                ScriptConsole.println("[QuestDialogFetcher] Added step " + stepCount + ": " + stepPreview + 
                    (step.hasDialogs() ? " [Has Dialogs]" : ""));
            }
        }
        
        ScriptConsole.println("[QuestDialogFetcher] Successfully parsed " + stepCount + " steps");
    }
    
    /**
     * Extracts clean step text by removing dialog elements and other unwanted content.
     */
    private static String extractCleanStepText(String stepContent) {
        if (stepContent == null || stepContent.trim().isEmpty()) {
            return "";
        }
        
        String cleanText = stepContent;
        
        cleanText = cleanText.replaceAll("<span class=\"chat-options\"[^>]*>.*?</span>", "");
        cleanText = cleanText.replaceAll("<div[^>]*data-tooltip-for=\"[^\"]+\"[^>]*>.*?</div>", "");
        
        cleanText = cleanText.replaceAll("<[^>]+>", "");
        
        cleanText = cleanText.replace("&quot;", "\"")
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
                            .replaceAll("&#([0-9]+);", "");
        
        cleanText = cleanText.replaceAll("\\s+", " ").trim();
        
        return cleanText;
    }
    
    /**
     * Removes nested <ul>...</ul> blocks while preserving the main structure.
     */
    private static String removeNestedUlBlocks(String content) {
        if (content == null || !content.contains("<ul")) {
            return content;
        }

        String result = content;
        Pattern nestedUlPattern = Pattern.compile("<ul[^>]*>.*?</ul>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        while (nestedUlPattern.matcher(result).find()) {
            result = nestedUlPattern.matcher(result).replaceAll("");
        }

        return result;
    }
    
    /**
     * Extracts the original HTML for a specific step to preserve dialog parsing.
     */
    private static String extractOriginalStepHtml(String originalContent, String stepText, int stepNumber) {
        return stepText;
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
        
        String cleaned = text.replace("&quot;", "\"")
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
        
        cleaned = applyBasicTextFormatting(cleaned);
        
        return cleaned;
    }
    
    /**
     * Applies basic text formatting improvements for better readability.
     */
    private static String applyBasicTextFormatting(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        text = text.replaceAll("([.!?])([A-Z])", "$1 $2")
                  .replaceAll("([,;:])([A-Za-z])", "$1 $2");
        
        text = text.replaceAll("\\((\\d+),\\s*(\\d+)\\)", "($1, $2)")
                  .replaceAll("\\((\\d+),\\s*(\\d+),\\s*(\\d+)\\)", "($1, $2, $3)");
        
        text = text.replaceAll("\\b(npc|NPC)\\b", "NPC")
                  .replaceAll("\\b(hp|HP)\\b", "HP")
                  .replaceAll("\\b(xp|XP|exp|EXP)\\b", "XP")
                  .replaceAll("\\b(gp|GP)\\b", "GP")
                  .replaceAll("\\b(poh|POH)\\b", "POH")
                  .replaceAll("\\b(ge|GE)\\b", "Grand Exchange");
        
        text = text.replaceAll("\\b(\\d+)\\s*x\\s*(\\w+)", "$1 $2")
                  .replaceAll("\\b(\\d+)\\s+(coins?|gp|GP)\\b", "$1 GP");
        
        text = text.replaceAll("\\s*-\\s*", " - ")
                  .replaceAll("\\s*→\\s*", " → ")
                  .replaceAll("\\s*/\\s*", "/");
        
        text = text.replaceAll("\\b(yes|no|ok|okay)\\b", "YES")
                  .replaceAll("\\b(continue)\\b", "Continue")
                  .replaceAll("\\b(skip)\\b", "Skip")
                  .replaceAll("\\b(next)\\b", "Next")
                  .replaceAll("\\b(back)\\b", "Back");
        
        text = text.replaceAll("\\s+", " ").trim();
        

        if (text.length() > 0 && Character.isLowerCase(text.charAt(0))) {
            text = Character.toUpperCase(text.charAt(0)) + text.substring(1);
        }
        
        return text;
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
     * Debug method to test parsing of a specific section.
     */
    public static void debugDescentSection(String questName) {
        ScriptConsole.println("[QuestDialogFetcher] Debug testing Descent section for: " + questName);
        QuestGuide guide = fetchQuestGuide(questName);
        
        for (QuestSection section : guide.getSections()) {
            if (section.getSectionName().toLowerCase().contains("descent")) {
                ScriptConsole.println("[DEBUG] Found Descent section with " + section.getTotalSteps() + " steps:");
                for (int i = 0; i < section.getSteps().size(); i++) {
                    QuestStep step = section.getSteps().get(i);
                    ScriptConsole.println("[DEBUG] Step " + (i+1) + ": " + step.getStepText());
                    if (step.hasDialogs()) {
                        ScriptConsole.println("[DEBUG]   Has " + step.getDialogs().size() + " dialog sequences");
                    }
                }
                break;
            }
        }
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

    /**
     * Parses dialog options within a specific step.
     */
    private static void parseDialogsInStep(String stepContent, QuestStep step) {
        Pattern chatOptionsPattern = Pattern.compile(
            "<span class=\"chat-options\">.*?data-tooltip-name=\"([^\"]+)\".*?</span>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        
        Pattern tooltipPattern = Pattern.compile(
            "<div[^>]*data-tooltip-for=\"([^\"]+)\"[^>]*>.*?<table><tbody>(.*?)</tbody></table>.*?</div>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        
        Pattern tableRowPattern = Pattern.compile(
            "<tr><td><b>([^<]+)</b></td><td>([^<]+)</td></tr>",
            Pattern.CASE_INSENSITIVE
        );
        
        Pattern sequenceDisplayPattern = Pattern.compile(
            "<span class=\"chat-options-underline\"[^>]*>([^<]+)</span>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher chatMatcher = chatOptionsPattern.matcher(stepContent);
        while (chatMatcher.find()) {
            String tooltipName = chatMatcher.group(1);
            String chatOptionsContent = chatMatcher.group(0);
            
            List<String> sequenceParts = new ArrayList<>();
            Matcher seqMatcher = sequenceDisplayPattern.matcher(chatOptionsContent);
            while (seqMatcher.find()) {
                String part = seqMatcher.group(1).trim();
                part = cleanOptionText(part);
                if (!part.isEmpty() && !part.equals("?")) {
                    sequenceParts.add(part);
                }
            }
            
            if (sequenceParts.isEmpty()) {
                continue;
            }
            
            String sequenceDisplay = String.join(" → ", sequenceParts);
            
            if (sequenceDisplay.trim().isEmpty() || sequenceDisplay.matches("^[?\\s→]+$")) {
                continue;
            }
            
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
                        
                        if (optionNum.equals("?") || optionNum.isEmpty() || 
                            optionText.equals("?") || optionText.isEmpty()) {
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
     * Cleans up section name by removing HTML entities and extra whitespace.
     */
    private static String cleanSectionName(String text) {
        if (text == null) return "";
        
        String cleaned = text.replace("&quot;", "\"")
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
        
        cleaned = applyBasicTextFormatting(cleaned);
        
        return cleaned;
    }
} 