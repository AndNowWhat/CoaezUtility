package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.Dialog;
import net.botwithus.rs3.game.Area;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.quest.Quest;
import net.botwithus.rs3.game.js5.types.QuestType;
import net.botwithus.rs3.game.js5.types.configs.ConfigManager;
import net.botwithus.rs3.script.ScriptConsole;
// Navigation imports
import net.botwithus.rs3.game.Area;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.movement.Movement;
import net.botwithus.rs3.game.movement.NavPath;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Random;

/**
 * QuestHelper class for managing and tracking quests.
 * Provides functionality to list all quests, filter by completion status,
 * and get detailed information about quest requirements.
 * Also provides main loop functionality for dialog assistance with step tracking.
 * Now includes UIScaler integration for coordinate calculation (rendering handled by GUI).
 */
public class QuestHelper implements Task {

    private final CoaezUtility script;
    private Quest selectedQuest;
    private QuestType selectedQuestType;
    private final List<Quest> allQuests;
    private final List<Quest> completedQuests;
    private final List<Quest> inProgressQuests;
    private final List<Quest> notStartedQuests;
    
    // Quest ID mapping for efficient lookup
    private final Map<Quest, Integer> questToIdMap;
    
    // UI Scaler for dialog coordinate calculation
    private final UIScaler uiScaler;
    private boolean overlayInitialized = false;
    
    // Enhanced dialog assistance fields with quest guide support
    private QuestDialogFetcher.QuestGuide currentQuestGuide;
    private List<String> fetchedDialogs; // Changed from Map to List<String> for legacy compatibility
    private boolean dialogsFetched = false;
    private boolean questGuideFetched = false;
    private String currentRecommendedOption = null;
    private int currentRecommendedOptionIndex = -1;
    private String currentRecommendation = null;
    private int recommendedOptionIndex = -1;
    private boolean isDialogAssistanceActive = false;
    private String currentDialogText = null;
    private List<String> previousDialogOptions = null;
    
    // Dialog overlay coordinates for GUI rendering
    private int dialogOverlayX = -1;
    private int dialogOverlayY = -1;
    private int dialogOverlayWidth = -1;
    private int dialogOverlayHeight = -1;
    private boolean hasValidOverlayCoordinates = false;
    
    // GUI state management fields
    private List<String> questDisplayNames = new ArrayList<>();
    private boolean showCompletedQuests = true;
    private boolean showInProgressQuests = true;
    private boolean showNotStartedQuests = true;
    private boolean showFreeToPlayQuests = true;
    private boolean showMembersQuests = true;
    private String questSearchText = "";
    private int selectedQuestIndex = 0;

    private String cachedComprehensiveInfo = null;
    private Quest lastCachedQuest = null;
    
    // Navigation fields
    private boolean isNavigatingToQuestStart = false;
    private Area questStartArea = null;
    private Coordinate questStartCoordinate = null;
    
    // Step tracking fields
    private boolean showStepTracker = false;
    private int selectedSectionIndex = 0;
    private boolean autoScrollToIncompleteSteps = true;
    
    // Additional fields for dialog assistance
    private boolean showRandomMessage = true;
    private boolean debugMode = false;
    private Set<String> completedSteps = new HashSet<>();

    // Auto-interaction for dialogs
    private boolean autoInteractWithDialogs = false;
    private long lastDialogInteractionTime = 0;
    private long nextInteractionDelay = 0;
    private static final long MIN_DIALOG_DELAY = 600; // 600ms minimum
    private static final long MAX_DIALOG_DELAY = 1800; // 1800ms maximum
    private final Random random = new Random();

    /**
     * Constructs a new QuestHelper.
     * Initializes quest lists and categorizes them by completion status.
     * @param script The main script instance.
     */
    public QuestHelper(CoaezUtility script) {
        this.script = script;
        this.allQuests = new ArrayList<>();
        this.completedQuests = new ArrayList<>();
        this.inProgressQuests = new ArrayList<>();
        this.notStartedQuests = new ArrayList<>();
        this.fetchedDialogs = new ArrayList<>();
        this.questToIdMap = new HashMap<>();
        this.uiScaler = new UIScaler();
        loadQuests();
    }

    /**
     * Loads all quests from the game and categorizes them by completion status.
     * This method is called during initialization.
     * Filters out quests that don't have meaningful data and handles duplicate quest names
     * by preferring the quest entry with more comprehensive data.
     */
    private void loadQuests() {
        ScriptConsole.println("[QuestHelper] Loading quests...");
        int filteredCount = 0;
        
        Map<String, QuestCandidate> questCandidates = new HashMap<>();
        
        for (int i = 0; i < 509; i++) {
            Optional<Quest> questOpt = Quest.byId(i);
            if (questOpt.isPresent()) {
                Quest quest = questOpt.get();
                
                if (isValidQuest(quest, i)) {
                    String questName = quest.name();
                    QuestType questType = ConfigManager.getQuestType(i);
                    int dataRichness = calculateQuestDataRichness(quest, questType, i);
                    
                    QuestCandidate candidate = new QuestCandidate(quest, questType, i, dataRichness);
                    
                    QuestCandidate existingCandidate = questCandidates.get(questName);
                    if (existingCandidate == null || candidate.dataRichness > existingCandidate.dataRichness) {
                        if (existingCandidate != null) {
                            ScriptConsole.println("[QuestHelper] Found duplicate quest '" + questName + 
                                                 "': Replacing ID " + existingCandidate.questId + 
                                                 " (richness: " + existingCandidate.dataRichness + 
                                                 ") with ID " + i + " (richness: " + dataRichness + ")");
                        }
                        questCandidates.put(questName, candidate);
                    } else {
                        ScriptConsole.println("[QuestHelper] Found duplicate quest '" + questName + 
                                             "': Keeping ID " + existingCandidate.questId + 
                                             " (richness: " + existingCandidate.dataRichness + 
                                             ") over ID " + i + " (richness: " + dataRichness + ")");
                    }
                } else {
                    filteredCount++;
                    ScriptConsole.println("[QuestHelper] Filtered out quest ID " + i + ": " + quest.name() + " (insufficient data)");
                }
            }
        }
        
        for (QuestCandidate candidate : questCandidates.values()) {
            Quest quest = candidate.quest;
            allQuests.add(quest);
            
            try {
                if (quest.isComplete()) {
                    completedQuests.add(quest);
                } else if (quest.isStarted()) {
                    inProgressQuests.add(quest);
                } else {
                    notStartedQuests.add(quest);
                }
            } catch (NullPointerException e) {
                ScriptConsole.println("[QuestHelper] Warning: Could not determine status for quest " + quest.name() + " (ID: " + candidate.questId + "), assuming not started");
                notStartedQuests.add(quest);
            }
            
            questToIdMap.put(quest, candidate.questId);
        }
        
        Comparator<Quest> byName = Comparator.comparing(Quest::name);
        Collections.sort(allQuests, byName);
        Collections.sort(completedQuests, byName);
        Collections.sort(inProgressQuests, byName);
        Collections.sort(notStartedQuests, byName);
        
        ScriptConsole.println("[QuestHelper] Loaded " + allQuests.size() + " quests (filtered out " + filteredCount + " quests with insufficient data)");
    }

    /**
     * Helper class to track quest candidates with their data richness scores.
     */
    private static class QuestCandidate {
        final Quest quest;
        final QuestType questType;
        final int questId;
        final int dataRichness;
        
        QuestCandidate(Quest quest, QuestType questType, int questId, int dataRichness) {
            this.quest = quest;
            this.questType = questType;
            this.questId = questId;
            this.dataRichness = dataRichness;
        }
    }
    
    /**
     * Calculates a richness score for quest data to help prefer more complete quest entries.
     * Higher scores indicate more comprehensive data.
     * 
     * @param quest The quest object
     * @param questType The quest type (may be null)
     * @param questId The quest ID
     * @return A richness score (higher = more data)
     */
    private int calculateQuestDataRichness(Quest quest, QuestType questType, int questId) {
        int score = 0;
        
        try {
            if (quest.getQuestPoints() > 0) score += 10;
            if (quest.getQuestPointReq() > 0) score += 5;
            if (!quest.getRequiredSkills().isEmpty()) score += 15;
            if (!quest.getRequiredQuests().isEmpty()) score += 15;
        } catch (Exception e) {
        }
        
        if (questType != null) {
            score += 20;
            
            if (questType.questPoints() > 0) score += 10;
            if (questType.questPointReq() > 0) score += 5;
            if (questType.skillRequirments() != null && questType.skillRequirments().length > 0) score += 20;
            if (questType.dependentQuests() != null && questType.dependentQuests().length > 0) score += 20;
            if (questType.startLocations() != null && questType.startLocations().length > 0) score += 25;
            if (questType.category() > 0) score += 5;
            if (questType.difficulty() > 0) score += 5;
            if (questType.questItemSprite() > 0) score += 5;
            
            Map<Integer, Object> params = questType.params();
            if (params != null && !params.isEmpty()) {
                score += 30;
                
                int meaningfulParams = 0;
                for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                    Object value = entry.getValue();
                    if (value != null && !value.toString().trim().isEmpty()) {
                        if (entry.getKey() == 1 && value.toString().equals(quest.name())) {
                            continue;
                        }
                        meaningfulParams++;
                    }
                }
                score += meaningfulParams * 3;
            }
        }
        
        return score;
    }

    /**
     * Validates if a quest has meaningful data and should be included in the quest lists.
     * Now less restrictive to allow quests that can be used for dialog assistance.
     * 
     * @param quest The quest to validate
     * @param questId The quest ID
     * @return true if the quest has meaningful data, false otherwise
     */
    private boolean isValidQuest(Quest quest, int questId) {
        if (quest == null || quest.name() == null || quest.name().trim().isEmpty()) {
            return false;
        }
        
        String questName = quest.name().trim();
        
        if (questName.equalsIgnoreCase("null") || 
            questName.equalsIgnoreCase("unknown") || 
            questName.equalsIgnoreCase("test") ||
            questName.length() < 2) {
            return false;
        }
        
        QuestType questType = ConfigManager.getQuestType(questId);
        if (questType != null) {
            Map<Integer, Object> params = questType.params();
            if (params != null && !params.isEmpty()) {
                boolean hasMeaningfulParams = false;
                for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                    Object value = entry.getValue();
                    if (value != null && !value.toString().trim().isEmpty()) {
                        if (entry.getKey() == 1 && value.toString().equals(quest.name())) {
                            continue;
                        }
                        hasMeaningfulParams = true;
                        break;
                    }
                }
                
                if (hasMeaningfulParams || hasBasicQuestTypeData(questType)) {
                    return true;
                }
            }
            
            if (hasBasicQuestTypeData(questType)) {
                return true;
            }
        }
        
        if (hasBasicQuestData(quest)) {
            return true;
        }
        
        return questName.length() >= 3 && !questName.matches(".*\\d{3,}.*"); // Exclude names with long numbers
    }
    
    /**
     * Checks if the Quest object has basic meaningful data.
     * Safely handles Quest objects that might have missing QuestType data.
     * 
     * @param quest The quest to check
     * @return true if the quest has basic data, false otherwise
     */
    private boolean hasBasicQuestData(Quest quest) {
        try {
            return quest.getQuestPoints() > 0 || 
                   quest.getQuestPointReq() > 0 || 
                   !quest.getRequiredSkills().isEmpty() || 
                   !quest.getRequiredQuests().isEmpty();
        } catch (NullPointerException e) {
            return false;
        }
    }
    
    /**
     * Checks if the QuestType has basic meaningful data beyond just the name.
     * 
     * @param questType The QuestType to check
     * @return true if the QuestType has meaningful data, false otherwise
     */
    private boolean hasBasicQuestTypeData(QuestType questType) {
        return questType.questPoints() > 0 ||
               questType.questPointReq() > 0 ||
               (questType.skillRequirments() != null && questType.skillRequirments().length > 0) ||
               (questType.dependentQuests() != null && questType.dependentQuests().length > 0) ||
               (questType.startLocations() != null && questType.startLocations().length > 0) ||
               questType.category() > 0 ||
               questType.difficulty() > 0 ||
               questType.questItemSprite() > 0;
    }

    /**
     * Gets a list of all quests.
     * @return An unmodifiable list of all quests.
     */
    public List<Quest> getAllQuests() {
        return Collections.unmodifiableList(allQuests);
    }

    /**
     * Gets a list of completed quests.
     * @return An unmodifiable list of completed quests.
     */
    public List<Quest> getCompletedQuests() {
        return Collections.unmodifiableList(completedQuests);
    }

    /**
     * Gets a list of quests in progress.
     * @return An unmodifiable list of quests in progress.
     */
    public List<Quest> getInProgressQuests() {
        return Collections.unmodifiableList(inProgressQuests);
    }

    /**
     * Gets a list of quests not started.
     * @return An unmodifiable list of quests not started.
     */
    public List<Quest> getNotStartedQuests() {
        return Collections.unmodifiableList(notStartedQuests);
    }

    /**
     * Sets the selected quest and loads its QuestType data.
     * @param quest The quest to select.
     */
    public void setSelectedQuest(Quest quest) {
        this.selectedQuest = quest;
        
        cachedComprehensiveInfo = null;
        lastCachedQuest = null;
        
        // Reset quest guide state
        currentQuestGuide = null;
        questGuideFetched = false;
        dialogsFetched = false;
        clearCurrentRecommendation();
        selectedSectionIndex = 0;
        
        if (quest != null) {
            int questId = getQuestId(quest);
            ScriptConsole.println("[QuestHelper] Selected quest: " + quest.name() + " (ID: " + questId + ")");
            
            if (questId != -1) {
                this.selectedQuestType = ConfigManager.getQuestType(questId);
                
                if (this.selectedQuestType != null) {
                    ScriptConsole.println("[QuestHelper] QuestType loaded for ID: " + questId);
                    
                    Map<Integer, Object> params = this.selectedQuestType.params();
                    if (params != null && !params.isEmpty()) {
                        ScriptConsole.println("[QuestHelper] Quest " + questId + " params:");
                        for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                            Object value = entry.getValue();
                            if (value != null) {
                                ScriptConsole.println("  Param " + entry.getKey() + ": " + value + " (" + value.getClass().getSimpleName() + ")");
                            }
                        }
                    } else {
                        ScriptConsole.println("[QuestHelper] Quest " + questId + " has no params");
                    }
                } else {
                    ScriptConsole.println("[QuestHelper] No QuestType data for ID: " + questId);
                }
            } else {
                this.selectedQuestType = null;
                ScriptConsole.println("[QuestHelper] Could not determine quest ID for: " + quest.name());
            }
        } else {
            this.selectedQuestType = null;
        }
    }

    /**
     * Gets the currently selected quest.
     * @return The selected quest, or null if none is selected.
     */
    public Quest getSelectedQuest() {
        return selectedQuest;
    }
    
    /**
     * Gets the QuestType for the currently selected quest.
     * @return The QuestType for the selected quest, or null if none is selected or QuestType couldn't be loaded.
     */
    public QuestType getSelectedQuestType() {
        return selectedQuestType;
    }

    /**
     * Gets a formatted string of quest requirements for the selected quest.
     * @return A string containing skill requirements, quest requirements, and other requirements.
     */
    public String getQuestRequirementsInfo() {
        if (selectedQuest == null) {
            return "No quest selected";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Requirements for ").append(selectedQuest.name()).append(":\n");
        
        int qpReq = selectedQuest.getQuestPointReq();
        if (qpReq > 0) {
            sb.append("- ").append(qpReq).append(" Quest Points\n");
        }
        
        List<net.botwithus.rs3.game.skills.Skills> skillReqs = selectedQuest.getRequiredSkills();
        if (!skillReqs.isEmpty()) {
            sb.append("Skill Requirements:\n");
            for (net.botwithus.rs3.game.skills.Skills skill : skillReqs) {
                sb.append("- ").append(skill.toString()).append("\n");
            }
        }
        
        List<Quest> questReqs = selectedQuest.getRequiredQuests();
        if (!questReqs.isEmpty()) {
            sb.append("Quest Requirements:\n");
            for (Quest quest : questReqs) {
                sb.append("- ").append(quest.name());
                if (quest.isComplete()) {
                    sb.append(" (Completed)");
                } else if (quest.isStarted()) {
                    sb.append(" (In Progress)");
                } else {
                    sb.append(" (Not Started)");
                }
                sb.append("\n");
            }
        }
        
        sb.append("\nDifficulty: ").append(selectedQuest.getDifficulty()).append("\n");
        sb.append("Members Only: ").append(selectedQuest.isMembers() ? "Yes" : "No").append("\n");
        sb.append("Quest Points Reward: ").append(selectedQuest.getQuestPoints()).append("\n");
        
        try {
            sb.append("Player has all requirements: ").append(selectedQuest.hasRequirements() ? "Yes" : "No").append("\n");
        } catch (Exception e) {
            sb.append("Player has all requirements: Unable to determine (quest data incomplete)\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Converts a packed location value to readable coordinates.
     * @param value The packed location value
     * @return A formatted string with x, y, z coordinates
     */
    private String convertLocationToCoordinates(int value) {
        int x = (value >> 14) & 0x3fff;
        int y = value & 0x3fff;
        int z = value >> 28;
        return String.format("(%d, %d, %d)", x, y, z);
    }

    /**
     * Gets a formatted string of quest parameters from QuestType.
     * @return A string containing quest parameters.
     */
    public String getQuestTypeInfo() {
        if (selectedQuestType == null) {
            return "No QuestType data available";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("QuestType Information for ").append(selectedQuestType.name()).append(":\n\n");
        
        sb.append("Category: ").append(selectedQuestType.category()).append("\n");
        sb.append("Difficulty: ").append(selectedQuestType.difficulty()).append("\n");
        sb.append("Members Only: ").append(selectedQuestType.membersOnly()).append("\n");
        sb.append("Quest Points: ").append(selectedQuestType.questPoints()).append("\n");
        sb.append("Quest Point Req: ").append(selectedQuestType.questPointReq()).append("\n");
        sb.append("Quest Item Sprite: ").append(selectedQuestType.questItemSprite()).append("\n");
        
        int[] startLocs = selectedQuestType.startLocations();
        if (startLocs != null && startLocs.length > 0) {
            sb.append("\nStart Locations:\n");
            for (int loc : startLocs) {
                sb.append("- ").append(convertLocationToCoordinates(loc)).append(" (Raw: ").append(loc).append(")\n");
            }
        }
        
        int[] depQuests = selectedQuestType.dependentQuests();
        if (depQuests != null && depQuests.length > 0) {
            sb.append("\nDependent Quests:\n");
            for (int questId : depQuests) {
                Optional<Quest> quest = Quest.byId(questId);
                String questName = quest.isPresent() ? quest.get().name() : "Unknown Quest";
                sb.append("- ").append(questName).append(" (ID: ").append(questId).append(")\n");
            }
        }
        
        int[][] skillReqs = selectedQuestType.skillRequirments();
        if (skillReqs != null && skillReqs.length > 0) {
            sb.append("\nSkill Requirements:\n");
            for (int[] skillReq : skillReqs) {
                if (skillReq.length >= 2) {
                    int skillId = skillReq[0];
                    int level = skillReq[1];
                    sb.append("- Skill ID ").append(skillId).append(": Level ").append(level).append("\n");
                }
            }
        }
        
        Map<Integer, Object> params = selectedQuestType.params();
        if (params != null && !params.isEmpty()) {
            sb.append("\nAdditional Parameters:\n");
            for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                sb.append("- Param ").append(entry.getKey()).append(": ");
                if (entry.getValue() != null) {
                    sb.append(entry.getValue().toString());
                } else {
                    sb.append("null");
                }
                sb.append("\n");
            }
        }
        
        return sb.toString();
    }

    /**
     * Gets a list of quest names with their IDs for display in the GUI.
     * @return A list of strings in the format "Quest Name [ID]".
     */
    public List<String> getQuestDisplayNames() {
        return Collections.unmodifiableList(questDisplayNames);
    }
    
    /**
     * Gets the ID of a quest by using the efficient questToIdMap.
     * This method uses the same logic as loadQuests to prefer quests with richer data
     * when multiple quest IDs have the same name.
     * @param quest The quest to get the ID for.
     * @return The quest ID, or -1 if not found.
     */
    private int getQuestId(Quest quest) {
        if (quest == null) {
            return -1;
        }
        
        Integer questId = questToIdMap.get(quest);
        if (questId != null) {
            return questId;
        }
        
        String questName = quest.name();
        int bestQuestId = -1;
        int bestDataRichness = -1;
        
        for (int i = 0; i < 509; i++) {
            Optional<Quest> testQuest = Quest.byId(i);
            if (testQuest.isPresent() && testQuest.get().name().equals(questName)) {
                QuestType questType = ConfigManager.getQuestType(i);
                int dataRichness = calculateQuestDataRichness(testQuest.get(), questType, i);
                
                if (dataRichness > bestDataRichness) {
                    bestQuestId = i;
                    bestDataRichness = dataRichness;
                }
            }
        }
        
        return bestQuestId;
    }
    
    /**
     * Gets a quest by its ID.
     * @param questId The quest ID.
     * @return The Quest object, or null if not found.
     */
    public Quest getQuestById(int questId) {
        Optional<Quest> questOpt = Quest.byId(questId);
        return questOpt.orElse(null);
    }
    
    /**
     * Gets a quest by its name.
     * @param questName The quest name.
     * @return The Quest object, or null if not found.
     */
    public Quest getQuestByName(String questName) {
        return allQuests.stream()
            .filter(q -> q.name().equalsIgnoreCase(questName))
            .findFirst()
            .orElse(null);
    }

    /**
     * Gets comprehensive quest information combining Quest and QuestType data.
     * Shows what data is available and gracefully handles missing information.
     * Uses caching to prevent expensive operations from running every frame.
     * @return A formatted string with all available quest information.
     */
    public String getComprehensiveQuestInfo() {
        if (selectedQuest == null) {
            return "No quest selected";
        }
        
        if (cachedComprehensiveInfo != null && selectedQuest.equals(lastCachedQuest)) {
            return cachedComprehensiveInfo;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Quest Information for ").append(selectedQuest.name()).append(":\n\n");
        
        sb.append("=== Basic Information ===\n");
        sb.append("Status: ");
        if (selectedQuest.isComplete()) {
            sb.append("Completed\n");
        } else if (selectedQuest.isStarted()) {
            sb.append("In Progress\n");
        } else {
            sb.append("Not Started\n");
        }
        
        sb.append("Quest Points Reward: ").append(selectedQuest.getQuestPoints()).append("\n");
        sb.append("Members Only: ").append(selectedQuest.isMembers() ? "Yes" : "No").append("\n");
        sb.append("Difficulty: ").append(selectedQuest.getDifficulty()).append("\n");
        
        sb.append("\n=== Requirements ===\n");
        int qpReq = selectedQuest.getQuestPointReq();
        if (qpReq > 0) {
            sb.append("Quest Points Required: ").append(qpReq).append("\n");
        }
        
        try {
            List<net.botwithus.rs3.game.skills.Skills> skillReqs = selectedQuest.getRequiredSkills();
            if (skillReqs != null && !skillReqs.isEmpty()) {
                sb.append("Skill Requirements:\n");
                for (net.botwithus.rs3.game.skills.Skills skill : skillReqs) {
                    sb.append("- ").append(skill.toString()).append("\n");
                }
            }
        } catch (Exception e) {
            sb.append("Skill Requirements: Unable to load (quest data incomplete)\n");
        }
        
        try {
            List<Quest> questReqs = selectedQuest.getRequiredQuests();
            if (questReqs != null && !questReqs.isEmpty()) {
                sb.append("Quest Requirements:\n");
                for (Quest quest : questReqs) {
                    sb.append("- ").append(quest.name());
                    if (quest.isComplete()) {
                        sb.append(" (Completed)");
                    } else if (quest.isStarted()) {
                        sb.append(" (In Progress)");
                    } else {
                        sb.append(" (Not Started)");
                    }
                    sb.append("\n");
                }
            }
        } catch (Exception e) {
            sb.append("Quest Requirements: Unable to load (quest data incomplete)\n");
        }
        
        try {
            sb.append("Player has all requirements: ").append(selectedQuest.hasRequirements() ? "Yes" : "No").append("\n");
        } catch (Exception e) {
            sb.append("Player has all requirements: Unable to determine (quest data incomplete)\n");
        }
        
        if (selectedQuestType != null) {
            sb.append("\n=== Advanced Quest Data ===\n");
            sb.append("Category: ").append(selectedQuestType.category()).append("\n");
            sb.append("Quest Item Sprite: ").append(selectedQuestType.questItemSprite()).append("\n");
            
            int[] startLocs = selectedQuestType.startLocations();
            if (startLocs != null && startLocs.length > 0) {
                sb.append("\nStart Locations:\n");
                for (int loc : startLocs) {
                    sb.append("- ").append(convertLocationToCoordinates(loc)).append(" (Raw: ").append(loc).append(")\n");
                }
            }
            
            int[] depQuests = selectedQuestType.dependentQuests();
            if (depQuests != null && depQuests.length > 0) {
                sb.append("\nDependent Quests:\n");
                for (int questId : depQuests) {
                    Optional<Quest> quest = Quest.byId(questId);
                    String questName = quest.isPresent() ? quest.get().name() : "Unknown Quest";
                    sb.append("- ").append(questName).append(" (ID: ").append(questId).append(")\n");
                }
            }
            
            int[][] skillReqsDetailed = selectedQuestType.skillRequirments();
            if (skillReqsDetailed != null && skillReqsDetailed.length > 0) {
                sb.append("\nDetailed Skill Requirements:\n");
                for (int[] skillReq : skillReqsDetailed) {
                    if (skillReq.length >= 2) {
                        int skillId = skillReq[0];
                        int level = skillReq[1];
                        sb.append("- Skill ID ").append(skillId).append(": Level ").append(level).append("\n");
                    }
                }
            }
            
            Map<Integer, Object> params = selectedQuestType.params();
            if (params != null && !params.isEmpty()) {
                sb.append("\nAdditional Parameters:\n");
                for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                    sb.append("- Param ").append(entry.getKey()).append(": ");
                    if (entry.getValue() != null) {
                        sb.append(entry.getValue().toString());
                    } else {
                        sb.append("null");
                    }
                    sb.append("\n");
                }
            }
        } else {
            sb.append("\n=== Advanced Quest Data ===\n");
            sb.append("No advanced quest data available for this quest.\n");
            sb.append("This quest may be older or have limited configuration data.\n");
            
            int questId = getQuestId(selectedQuest);
            sb.append("Quest ID: ").append(questId).append("\n");
            if (questId == -1) {
                sb.append("Issue: Could not determine quest ID\n");
            } else {
                sb.append("Issue: ConfigManager.getQuestType(").append(questId).append(") returned null\n");
            }
        }
        
        cachedComprehensiveInfo = sb.toString();
        lastCachedQuest = selectedQuest;
        
        return cachedComprehensiveInfo;
    }

    /**
     * Fetches quest dialog options from the RuneScape Wiki and prints them to console.
     * This method is for testing the dialog fetching functionality.
     */
    public void fetchAndPrintQuestDialogs() {
        if (selectedQuest == null) {
            ScriptConsole.println("[QuestHelper] No quest selected for dialog fetching.");
            return;
        }
        
        String questName = selectedQuest.name();
        ScriptConsole.println("[QuestHelper] Fetching dialog options for quest: " + questName);
        
        try {
            List<String> dialogs = 
                QuestDialogFetcher.fetchQuestDialogs(questName);
            
            if (dialogs.isEmpty()) {
                ScriptConsole.println("[QuestHelper] No dialog options found for: " + questName);
                return;
            }
            
            ScriptConsole.println("[QuestHelper] === DIALOG OPTIONS FOR: " + questName + " ===");
            
            for (String dialog : dialogs) {
                ScriptConsole.println("[QuestHelper] " + dialog);
            }
            
            ScriptConsole.println("[QuestHelper] === END DIALOG OPTIONS ===");
            
        } catch (Exception e) {
            ScriptConsole.println("[QuestHelper] Error fetching dialogs for " + questName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets quest dialog options as a formatted string.
     * @return A formatted string containing dialog options, or an error message if none found.
     */
    public String getQuestDialogOptions() {
        if (selectedQuest == null) {
            return "No quest selected";
        }
        
        try {
            List<String> dialogs = 
                QuestDialogFetcher.fetchQuestDialogs(selectedQuest.name());
            
            if (dialogs.isEmpty()) {
                return "No dialog options found for: " + selectedQuest.name();
            }
            
            return String.join("\n", dialogs);
            
        } catch (Exception e) {
            return "Error fetching dialog options: " + e.getMessage();
        }
    }

    /**
     * Fetches quest guide for the currently selected quest.
     */
    private void fetchQuestGuideForSelectedQuest() {
        if (selectedQuest == null) {
            return;
        }
        
        try {
            ScriptConsole.println("[QuestHelper] Fetching quest guide for: " + selectedQuest.name());
            currentQuestGuide = QuestDialogFetcher.fetchQuestGuide(selectedQuest.name());
            questGuideFetched = true;
            
            if (currentQuestGuide.getSections().isEmpty()) {
                ScriptConsole.println("[QuestHelper] No quest guide found for: " + selectedQuest.name());
            } else {
                ScriptConsole.println("[QuestHelper] Successfully fetched quest guide for: " + selectedQuest.name());
                ScriptConsole.println("[QuestHelper] Found " + currentQuestGuide.getSections().size() + " sections with " + currentQuestGuide.getTotalSteps() + " total steps");
            }
        } catch (Exception e) {
            ScriptConsole.println("[QuestHelper] Error fetching quest guide: " + e.getMessage());
            questGuideFetched = false;
        }
    }

    /**
     * Gets the current quest guide.
     * @return The current quest guide, or null if none loaded
     */
    public QuestDialogFetcher.QuestGuide getCurrentQuestGuide() {
        return currentQuestGuide;
    }
    
    /**
     * Gets dialog options for incomplete steps only.
     * @return List of dialog sequences for incomplete steps
     */
    public List<QuestDialogFetcher.DialogSequence> getDialogsForIncompleteSteps() {
        if (currentQuestGuide == null) {
            return new ArrayList<>();
        }
        return currentQuestGuide.getDialogsForIncompleteSteps();
    }
    
    /**
     * Marks a quest step as completed or incomplete.
     * @param sectionIndex The index of the section containing the step
     * @param stepIndex The index of the step within the section
     * @param completed Whether the step should be marked as completed
     */
    public void setStepCompleted(int sectionIndex, int stepIndex, boolean completed) {
        if (currentQuestGuide == null || sectionIndex < 0 || sectionIndex >= currentQuestGuide.getSections().size()) {
            return;
        }
        
        QuestDialogFetcher.QuestSection section = currentQuestGuide.getSections().get(sectionIndex);
        if (stepIndex < 0 || stepIndex >= section.getSteps().size()) {
            return;
        }
        
        QuestDialogFetcher.QuestStep step = section.getSteps().get(stepIndex);
        step.setCompleted(completed);
        
        ScriptConsole.println("[QuestHelper] Step " + (stepIndex + 1) + " in section '" + section.getSectionName() + "' marked as " + (completed ? "completed" : "incomplete"));
        
        // Clear cached dialog recommendations as they may have changed
        clearCurrentRecommendation();
    }
    
    /**
     * Gets the completion status of a quest step.
     * @param sectionIndex The index of the section containing the step
     * @param stepIndex The index of the step within the section
     * @return true if the step is completed, false otherwise
     */
    public boolean isStepCompleted(int sectionIndex, int stepIndex) {
        if (currentQuestGuide == null || sectionIndex < 0 || sectionIndex >= currentQuestGuide.getSections().size()) {
            return false;
        }
        
        QuestDialogFetcher.QuestSection section = currentQuestGuide.getSections().get(sectionIndex);
        if (stepIndex < 0 || stepIndex >= section.getSteps().size()) {
            return false;
        }
        
        return section.getSteps().get(stepIndex).isCompleted();
    }
    
    /**
     * Toggles the completion status of a quest step.
     * @param sectionIndex The index of the section containing the step
     * @param stepIndex The index of the step within the section
     */
    public void toggleStepCompleted(int sectionIndex, int stepIndex) {
        boolean currentStatus = isStepCompleted(sectionIndex, stepIndex);
        setStepCompleted(sectionIndex, stepIndex, !currentStatus);
    }
    
    /**
     * Marks all steps in a section as completed or incomplete.
     * @param sectionIndex The index of the section
     * @param completed Whether all steps should be marked as completed
     */
    public void setSectionCompleted(int sectionIndex, boolean completed) {
        if (currentQuestGuide == null || sectionIndex < 0 || sectionIndex >= currentQuestGuide.getSections().size()) {
            return;
        }
        
        QuestDialogFetcher.QuestSection section = currentQuestGuide.getSections().get(sectionIndex);
        for (int i = 0; i < section.getSteps().size(); i++) {
            section.getSteps().get(i).setCompleted(completed);
        }
        
        ScriptConsole.println("[QuestHelper] All steps in section '" + section.getSectionName() + "' marked as " + (completed ? "completed" : "incomplete"));
        clearCurrentRecommendation();
    }
    
    /**
     * Gets the index of the first incomplete step in the quest guide.
     * Used for auto-scrolling to the current task.
     * @return Array of [sectionIndex, stepIndex] for the first incomplete step, or null if all steps are completed
     */
    public int[] getFirstIncompleteStepIndex() {
        if (currentQuestGuide == null) {
            return null;
        }
        
        for (int sectionIndex = 0; sectionIndex < currentQuestGuide.getSections().size(); sectionIndex++) {
            QuestDialogFetcher.QuestSection section = currentQuestGuide.getSections().get(sectionIndex);
            for (int stepIndex = 0; stepIndex < section.getSteps().size(); stepIndex++) {
                if (!section.getSteps().get(stepIndex).isCompleted()) {
                    return new int[]{sectionIndex, stepIndex};
                }
            }
        }
        
        return null; // All steps completed
    }
    
    /**
     * Resets all step completion status for the current quest guide.
     */
    public void resetAllSteps() {
        if (currentQuestGuide == null) {
            return;
        }
        
        for (QuestDialogFetcher.QuestSection section : currentQuestGuide.getSections()) {
            for (QuestDialogFetcher.QuestStep step : section.getSteps()) {
                step.setCompleted(false);
            }
        }
        
        ScriptConsole.println("[QuestHelper] Reset all step completion status for: " + selectedQuest.name());
        clearCurrentRecommendation();
    }
    
    /**
     * Gets a formatted string representation of the quest progress.
     * @return A string showing completion progress
     */
    public String getQuestProgress() {
        if (currentQuestGuide == null) {
            return "No quest guide loaded";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Quest Progress for ").append(currentQuestGuide.getQuestName()).append(":\n");
        sb.append("Overall: ").append(currentQuestGuide.getCompletedSteps()).append("/").append(currentQuestGuide.getTotalSteps()).append(" steps completed\n\n");
        
        for (QuestDialogFetcher.QuestSection section : currentQuestGuide.getSections()) {
            sb.append("• ").append(section.getSectionName()).append(": ");
            sb.append(section.getCompletedSteps()).append("/").append(section.getTotalSteps()).append(" completed\n");
        }
        
        return sb.toString();
    }

    /**
     * Enhanced execute method with step tracking and dialog assistance
     */
    @Override
    public void execute() {
        if (selectedQuest == null && showRandomMessage) {
            ScriptConsole.println("[QuestHelper] Select a quest to get started!");
            showRandomMessage = false;
        }
        
        if (selectedQuest == null) return;
        
        // Initialize UI scaler if needed
        if (!overlayInitialized) {
            initializeOverlay();
        }
        
        // Fetch quest guide if needed and step tracker is enabled
        if (showStepTracker && !questGuideFetched) {
            fetchQuestGuideForSelectedQuest();
        }
        
        // Handle quest start navigation
        if (isNavigatingToQuestStart) {
            processQuestStartNavigation();
        }
        
        // Handle dialog assistance with step tracking
        if (isDialogAssistanceActive) {
            processOpenDialogs();
            
            // Calculate overlay coordinates if dialog is open and we have a recommendation
            if (Dialog.isOpen() && recommendedOptionIndex != -1) {
                calculateDialogOverlayCoordinates();
            } else {
                clearOverlayCoordinates();
            }
        }
        
        // Handle debug mode
        if (debugMode) {
            debugCurrentDialogs();
        }
    }
    
    /**
     * Initializes the UI overlay system.
     */
    private void initializeOverlay() {
        if (uiScaler.initialize()) {
            overlayInitialized = true;
            ScriptConsole.println("[QuestHelper] Dialog overlay system initialized successfully");
        } else {
            ScriptConsole.println("[QuestHelper] Failed to initialize dialog overlay system");
        }
    }
    

    /**
     * Processes open dialogs and updates recommendations.
     */
    private void processOpenDialogs() {
        if (!Dialog.isOpen()) {
            clearCurrentRecommendation();
            clearOverlayCoordinates();
            return;
        }
        
        List<String> currentOptions = Dialog.getOptions();
        if (currentOptions == null || currentOptions.isEmpty()) {
            if (autoInteractWithDialogs && canInteractWithDialog()) {
                ScriptConsole.println("[QuestHelper] Auto-continuing dialog (no options available)");
                if (Dialog.select()) {
                    lastDialogInteractionTime = System.currentTimeMillis();
                    generateNextInteractionDelay();
                    ScriptConsole.println("[QuestHelper] Successfully continued dialog");
                } else {
                    ScriptConsole.println("[QuestHelper] Failed to continue dialog");
                }
                clearCurrentRecommendation();
                clearOverlayCoordinates();
                return;
            }
            currentRecommendation = "Continue";
            clearOverlayCoordinates();
            return;
        }
        
        if (currentQuestGuide != null && !currentQuestGuide.getSections().isEmpty()) {
            processOpenDialogWithStepTracking();
        } else if (dialogsFetched) {
            processLegacyDialog();
        }
        
        if (Dialog.isOpen() && recommendedOptionIndex != -1) {
            calculateDialogOverlayCoordinates();
        } else {
            clearOverlayCoordinates();
        }
    }

    /**
     * Processes open dialogs with step tracking support and coordinate calculation.
     */
    private void processOpenDialogWithStepTracking() {
        List<String> currentOptions = Dialog.getOptions();
        if (currentOptions == null || currentOptions.isEmpty()) {
            clearCurrentRecommendation();
            return;
        }
        
        ScriptConsole.println("[QuestHelper] === DIALOG MATCHING DEBUG ===");
        ScriptConsole.println("[QuestHelper] Current dialog options available:");
        for (int i = 0; i < currentOptions.size(); i++) {
            ScriptConsole.println("[QuestHelper]   " + (i + 1) + ": '" + currentOptions.get(i) + "'");
        }
        
        int[] currentStepIndex = getFirstIncompleteStepIndex();
        if (currentStepIndex == null) {
            currentRecommendation = "All steps completed! No dialog assistance needed.";
            recommendedOptionIndex = -1;
            ScriptConsole.println("[QuestHelper] === END DIALOG MATCHING DEBUG ===");
            return;
        }
        
        ScriptConsole.println("[QuestHelper] Checking dialogs from first incomplete step: Section " + currentStepIndex[0] + ", Step " + currentStepIndex[1]);
        
        List<String> currentStepDialogs = getDialogOptionsForSpecificStep(currentStepIndex[0], currentStepIndex[1]);
        
        if (currentStepDialogs == null || currentStepDialogs.isEmpty()) {
            ScriptConsole.println("[QuestHelper] No dialog options found for current step");
        } else {
            ScriptConsole.println("[QuestHelper] Current step dialog options:");
            for (int i = 0; i < currentStepDialogs.size(); i++) {
                ScriptConsole.println("[QuestHelper]   " + (i + 1) + ": '" + currentStepDialogs.get(i) + "'");
            }
        }
        
        if (currentStepDialogs != null) {
            for (String stepDialog : currentStepDialogs) {
                int matchIndex = findMatchingOptionIndex(currentOptions, stepDialog);
                if (matchIndex != -1) {
                    currentRecommendation = "Select: " + currentOptions.get(matchIndex);
                    recommendedOptionIndex = matchIndex;
                    currentRecommendedOption = currentOptions.get(matchIndex);
                    currentRecommendedOptionIndex = matchIndex;
                    
                    ScriptConsole.println("[QuestHelper] ✓ Found match - option " + (matchIndex + 1) + ": " + currentOptions.get(matchIndex));
                    
                    // Auto-interact with dialog if enabled
                    if (autoInteractWithDialogs && canInteractWithDialog()) {
                        ScriptConsole.println("[QuestHelper] Auto-interacting with dialog option " + (matchIndex + 1));
                        if (Dialog.interact(matchIndex)) {
                            lastDialogInteractionTime = System.currentTimeMillis();
                            generateNextInteractionDelay();
                            ScriptConsole.println("[QuestHelper] Successfully interacted with dialog option " + (matchIndex + 1));
                        } else {
                            ScriptConsole.println("[QuestHelper] Failed to interact with dialog option " + (matchIndex + 1));
                        }
                        clearCurrentRecommendation();
                        clearOverlayCoordinates();
                        ScriptConsole.println("[QuestHelper] === END DIALOG MATCHING DEBUG ===");
                        return;
                    }
                    
                    ScriptConsole.println("[QuestHelper] === END DIALOG MATCHING DEBUG ===");
                    return;
                }
            }
        }
        
        currentRecommendation = "No matching dialog option for current step";
        recommendedOptionIndex = -1;
        currentRecommendedOption = null;
        currentRecommendedOptionIndex = -1;
        ScriptConsole.println("[QuestHelper] No matching dialog options found");
        ScriptConsole.println("[QuestHelper] === END DIALOG MATCHING DEBUG ===");
    }
    
    /**
     * Processes legacy dialogs without step tracking but with coordinate calculation.
     */
    private void processLegacyDialog() {
        if (fetchedDialogs == null || fetchedDialogs.isEmpty()) {
            clearCurrentRecommendation();
            return;
        }
        
        List<String> currentOptions = Dialog.getOptions();
        if (currentOptions == null || currentOptions.isEmpty()) {
            clearCurrentRecommendation();
            return;
        }
        
        for (String fetchedOption : fetchedDialogs) {
            if (fetchedOption == null) continue;
            
            int matchIndex = findMatchingOptionIndex(currentOptions, fetchedOption);
            if (matchIndex != -1) {
                currentRecommendation = "Select: " + currentOptions.get(matchIndex);
                recommendedOptionIndex = matchIndex;
                currentRecommendedOption = currentOptions.get(matchIndex);
                currentRecommendedOptionIndex = matchIndex;
                
                ScriptConsole.println("[QuestHelper] Recommending option " + (matchIndex + 1) + ": " + currentOptions.get(matchIndex));
                
                // Auto-interact with dialog if enabled
                if (autoInteractWithDialogs && canInteractWithDialog()) {
                    ScriptConsole.println("[QuestHelper] Auto-interacting with legacy dialog option " + (matchIndex + 1));
                    if (Dialog.interact(matchIndex)) {
                        lastDialogInteractionTime = System.currentTimeMillis();
                        generateNextInteractionDelay();
                        ScriptConsole.println("[QuestHelper] Successfully interacted with legacy dialog option " + (matchIndex + 1));
                    } else {
                        ScriptConsole.println("[QuestHelper] Failed to interact with legacy dialog option " + (matchIndex + 1));
                    }
                    clearCurrentRecommendation();
                    clearOverlayCoordinates();
                    return;
                }
                
                return;
            }
        }
        
        currentRecommendation = "No matching dialog option found";
        recommendedOptionIndex = -1;
        currentRecommendedOption = null;
        currentRecommendedOptionIndex = -1;
    }
    
    /**
     * Clears the current dialog recommendation and overlay coordinates.
     */
    private void clearCurrentRecommendation() {
        if (currentRecommendation != null) {
            currentRecommendation = null;
            recommendedOptionIndex = -1;
            currentRecommendedOption = null;
            currentRecommendedOptionIndex = -1;
            clearOverlayCoordinates();
        }
    }
    
    /**
     * Gets the UIScaler instance for external access.
     * @return The UIScaler instance
     */
    public UIScaler getUIScaler() {
        return uiScaler;
    }
    
    /**
     * Checks if dialog overlay is enabled.
     * @return true if dialog overlay is enabled
     */
    public boolean isShowDialogOverlay() {
        return true; // Always enabled for coordinate calculation
    }
    
    /**
     * Enables or disables dialog overlay visualization.
     * @param showDialogOverlay true to show dialog overlays, false to hide
     */
    public void setShowDialogOverlay(boolean showDialogOverlay) {
        // Method kept for compatibility but rendering is handled by GUI
        ScriptConsole.println("[QuestHelper] Dialog overlay " + (showDialogOverlay ? "enabled" : "disabled"));
    }
    
    /**
     * Updates the overlay colors for dialog highlighting.
     * @param highlightColor The fill color for the highlight
     * @param borderColor The border color for the highlight
     * @param textColor The text color for the recommendation
     */
    public void setOverlayColors(Object highlightColor, Object borderColor, Object textColor) {
        // Method kept for compatibility but colors are handled by GUI
        ScriptConsole.println("[QuestHelper] Dialog overlay colors updated");
    }
    
    /**
     * Forces a refresh of the UI scaler (useful when game window is resized).
     */
    public void refreshUIScaler() {
        if (uiScaler.refresh()) {
            ScriptConsole.println("[QuestHelper] UI scaler refreshed successfully");
        } else {
            ScriptConsole.println("[QuestHelper] Failed to refresh UI scaler");
            overlayInitialized = false;
        }
    }
    
    /**
     * Gets debug information about the current UI scaling state.
     * @return A string containing UI scaling debug information
     */
    public String getUIScalerDebugInfo() {
        if (!uiScaler.isInitialized()) {
            return "UI Scaler not initialized";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== UI Scaler Debug Info ===\n");
        
        UIScaler.InterfaceRect windowSize = uiScaler.getWindowSize();
        if (windowSize != null) {
            sb.append("Window Size: ").append(windowSize.width).append(" x ").append(windowSize.height).append("\n");
        }
        
        double[] scaleFactors = uiScaler.getScaleFactors();
        if (scaleFactors != null) {
            sb.append("Scale Factors: ").append(String.format("%.3f x %.3f", scaleFactors[0], scaleFactors[1])).append("\n");
        }
        
        sb.append("Overlay Initialized: ").append(overlayInitialized).append("\n");
        sb.append("Current Recommendation Index: ").append(recommendedOptionIndex).append("\n");
        sb.append("Has Valid Overlay Coordinates: ").append(hasValidOverlayCoordinates).append("\n");
        
        if (hasValidOverlayCoordinates) {
            sb.append("Overlay Coordinates: (").append(dialogOverlayX).append(", ").append(dialogOverlayY).append(", ")
              .append(dialogOverlayWidth).append(", ").append(dialogOverlayHeight).append(")\n");
        }
        
        return sb.toString();
    }

    /**
     * Debug method to log current dialog information.
     */
    private void debugCurrentDialogs() {
        if (Dialog.isOpen()) {
            List<String> options = Dialog.getOptions();
            if (options != null && !options.isEmpty()) {
                ScriptConsole.println("[QuestHelper] DEBUG - Current dialog options:");
                for (int i = 0; i < options.size(); i++) {
                    ScriptConsole.println("[QuestHelper]   " + (i + 1) + ": " + options.get(i));
                }
            }
        }
    }
    
    /**
     * Fetches dialogs for the selected quest (legacy method).
     */
    private void fetchDialogsForSelectedQuest() {
        if (selectedQuest == null) {
            return;
        }
        
        try {
            fetchedDialogs = QuestDialogFetcher.fetchQuestDialogs(selectedQuest.name());
            dialogsFetched = true;
            ScriptConsole.println("[QuestHelper] Fetched " + fetchedDialogs.size() + " dialog options for " + selectedQuest.name());
        } catch (Exception e) {
            ScriptConsole.println("[QuestHelper] Failed to fetch dialogs: " + e.getMessage());
            dialogsFetched = true; // Mark as fetched to avoid retry loops
        }
    }

    /**
     * Updates the quest display list based on current filter settings.
     */
    public void updateQuestDisplayList() {
        questDisplayNames.clear();
        
        Set<String> uniqueQuestNames = new HashSet<>();
        
        if (showCompletedQuests) {
            uniqueQuestNames.addAll(completedQuests.stream()
                .filter(this::matchesQuestFilters)
                .map(q -> q.name() + " [" + getQuestId(q) + "]")
                .collect(Collectors.toSet()));
        }
        
        if (showInProgressQuests) {
            uniqueQuestNames.addAll(inProgressQuests.stream()
                .filter(this::matchesQuestFilters)
                .map(q -> q.name() + " [" + getQuestId(q) + "]")
                .collect(Collectors.toSet()));
        }
        
        if (showNotStartedQuests) {
            uniqueQuestNames.addAll(notStartedQuests.stream()
                .filter(this::matchesQuestFilters)
                .map(q -> q.name() + " [" + getQuestId(q) + "]")
                .collect(Collectors.toSet()));
        }
        
        questDisplayNames.addAll(uniqueQuestNames);
        Collections.sort(questDisplayNames, (a, b) -> {
            int idA = extractQuestId(a);
            int idB = extractQuestId(b);
            return Integer.compare(idA, idB);
        });
    }
    
    /**
     * Checks if a quest matches the current filter criteria.
     * @param quest The quest to check
     * @return true if the quest matches the filters
     */
    private boolean matchesQuestFilters(Quest quest) {
        boolean matchesMembershipFilter = false;
        if (quest.isMembers() && showMembersQuests) {
            matchesMembershipFilter = true;
        } else if (!quest.isMembers() && showFreeToPlayQuests) {
            matchesMembershipFilter = true;
        }
        
        if (!matchesMembershipFilter) {
            return false;
        }
        
        if (questSearchText != null && !questSearchText.trim().isEmpty()) {
            String searchLower = questSearchText.toLowerCase().trim();
            String questNameLower = quest.name().toLowerCase();
            return questNameLower.contains(searchLower);
        }
        
        return true;
    }
    
    /**
     * Extracts the quest ID from a display name with format "Quest Name [ID]".
     * @param displayName The display name
     * @return The quest ID, or -1 if not found
     */
    private int extractQuestId(String displayName) {
        try {
            int startIndex = displayName.lastIndexOf('[');
            int endIndex = displayName.lastIndexOf(']');
            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                String idStr = displayName.substring(startIndex + 1, endIndex);
                return Integer.parseInt(idStr);
            }
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            ScriptConsole.println("[QuestHelper] Error parsing quest ID from: " + displayName);
        }
        return -1;
    }
    
    /**
     * Selects a quest by its index in the display list.
     * @param index The index in the display list
     */
    public void selectQuestByIndex(int index) {
        if (index >= 0 && index < questDisplayNames.size()) {
            selectedQuestIndex = index;
            String selectedDisplayName = questDisplayNames.get(index);
            int questId = extractQuestId(selectedDisplayName);
            
            if (questId != -1) {
                Quest quest = getQuestById(questId);
                if (quest != null) {
                    setSelectedQuest(quest);
                    enableDialogAssistance();
                    ScriptConsole.println("[QuestHelper] Quest selected: " + quest.name() + " - Dialog assistance enabled");
                }
            }
        }
    }
    
    public boolean isShowCompletedQuests() {
        return showCompletedQuests;
    }
    
    public void setShowCompletedQuests(boolean showCompletedQuests) {
        if (this.showCompletedQuests != showCompletedQuests) {
            this.showCompletedQuests = showCompletedQuests;
            updateQuestDisplayList();
        }
    }
    
    public boolean isShowInProgressQuests() {
        return showInProgressQuests;
    }
    
    public void setShowInProgressQuests(boolean showInProgressQuests) {
        if (this.showInProgressQuests != showInProgressQuests) {
            this.showInProgressQuests = showInProgressQuests;
            updateQuestDisplayList();
        }
    }
    
    public boolean isShowNotStartedQuests() {
        return showNotStartedQuests;
    }
    
    public void setShowNotStartedQuests(boolean showNotStartedQuests) {
        if (this.showNotStartedQuests != showNotStartedQuests) {
            this.showNotStartedQuests = showNotStartedQuests;
            updateQuestDisplayList();
        }
    }
    
    public boolean isShowFreeToPlayQuests() {
        return showFreeToPlayQuests;
    }
    
    public void setShowFreeToPlayQuests(boolean showFreeToPlayQuests) {
        if (this.showFreeToPlayQuests != showFreeToPlayQuests) {
            this.showFreeToPlayQuests = showFreeToPlayQuests;
            updateQuestDisplayList();
        }
    }
    
    public boolean isShowMembersQuests() {
        return showMembersQuests;
    }
    
    public void setShowMembersQuests(boolean showMembersQuests) {
        if (this.showMembersQuests != showMembersQuests) {
            this.showMembersQuests = showMembersQuests;
            updateQuestDisplayList();
        }
    }
    
    public String getQuestSearchText() {
        return questSearchText;
    }
    
    public void setQuestSearchText(String questSearchText) {
        if (!Objects.equals(this.questSearchText, questSearchText)) {
            this.questSearchText = questSearchText;
            updateQuestDisplayList();
        }
    }
    
    public int getSelectedQuestIndex() {
        return selectedQuestIndex;
    }
    
    /**
     * Initializes the quest display list. Should be called when the GUI is first opened.
     */
    public void initializeQuestDisplay() {
        if (questDisplayNames.isEmpty()) {
            updateQuestDisplayList();
        }
    }

    /**
     * Get the current dialog text for display in the overlay
     */
    public String getCurrentDialogText() {
        return currentDialogText;
    }
    
    /**
     * Check if dialog assistance is currently active.
     */
    public boolean isDialogAssistanceActive() {
        return isDialogAssistanceActive;
    }
    
    /**
     * Enable dialog assistance for the selected quest.
     */
    public void enableDialogAssistance() {
        if (selectedQuest == null) {
            ScriptConsole.println("[QuestHelper] Cannot enable dialog assistance: No quest selected.");
            return;
        }
        
        isDialogAssistanceActive = true;
        showStepTracker = true; // Enable step tracking to use checkbox completion status
        ScriptConsole.println("[QuestHelper] Dialog assistance enabled for: " + selectedQuest.name());
        
        // Fetch quest guide and dialogs
        fetchQuestGuideForSelectedQuest();
        fetchDialogsForSelectedQuest();
    }
    
    /**
     * Disable dialog assistance.
     */
    public void disableDialogAssistance() {
        isDialogAssistanceActive = false;
        showStepTracker = false; // Disable step tracking when dialog assistance is disabled
        clearCurrentRecommendation();
        ScriptConsole.println("[QuestHelper] Dialog assistance disabled.");
    }
    
    /**
     * Check if dialogs have been fetched for the selected quest.
     */
    public boolean areDialogsFetched() {
        return dialogsFetched;
    }
    
    /**
     * Get the list of fetched dialog options.
     */
    public List<String> getFetchedDialogs() {
        return new ArrayList<>(fetchedDialogs);
    }
    
    /**
     * Get the current recommended dialog option text.
     */
    public String getCurrentRecommendedOption() {
        return currentRecommendedOption;
    }
    
    /**
     * Get the index of the current recommended dialog option.
     */
    public int getCurrentRecommendedOptionIndex() {
        return currentRecommendedOptionIndex;
    }
    
    /**
     * Get dialog options for incomplete steps only.
     */
    public List<String> getDialogOptionsForIncompleteSteps() {
        List<String> incompleteDialogs = new ArrayList<>();
        
        if (currentQuestGuide == null) {
            return incompleteDialogs;
        }
        
        for (int sectionIndex = 0; sectionIndex < currentQuestGuide.getSections().size(); sectionIndex++) {
            QuestDialogFetcher.QuestSection section = currentQuestGuide.getSections().get(sectionIndex);
            
            for (int stepIndex = 0; stepIndex < section.getSteps().size(); stepIndex++) {
                if (!isStepCompleted(sectionIndex, stepIndex)) {
                    QuestDialogFetcher.QuestStep step = section.getSteps().get(stepIndex);
                    
                    if (step.hasDialogs()) {
                        for (QuestDialogFetcher.DialogSequence sequence : step.getDialogs()) {
                            for (QuestDialogFetcher.DialogOption option : sequence.getOptions()) {
                                incompleteDialogs.add(option.getOptionText());
                            }
                        }
                    }
                }
            }
        }
        
        return incompleteDialogs;
    }

    /**
     * Get dialog options for a specific step.
     * @param sectionIndex The section index
     * @param stepIndex The step index
     * @return List of dialog options for the specified step
     */
    public List<String> getDialogOptionsForSpecificStep(int sectionIndex, int stepIndex) {
        List<String> stepDialogs = new ArrayList<>();
        
        if (currentQuestGuide == null) {
            return stepDialogs;
        }
        
        if (sectionIndex < 0 || sectionIndex >= currentQuestGuide.getSections().size()) {
            return stepDialogs;
        }
        
        QuestDialogFetcher.QuestSection section = currentQuestGuide.getSections().get(sectionIndex);
        if (section == null) {
            return stepDialogs;
        }
        
        if (stepIndex < 0 || stepIndex >= section.getSteps().size()) {
            return stepDialogs;
        }
        
        QuestDialogFetcher.QuestStep step = section.getSteps().get(stepIndex);
        if (step == null || !step.hasDialogs()) {
            return stepDialogs;
        }
        
        for (QuestDialogFetcher.DialogSequence sequence : step.getDialogs()) {
            if (sequence == null || sequence.getOptions() == null) continue;
            
            for (QuestDialogFetcher.DialogOption option : sequence.getOptions()) {
                if (option != null && option.getOptionText() != null) {
                    stepDialogs.add(option.getOptionText());
                }
            }
        }
        
        return stepDialogs;
    }

    /**
     * Starts navigation to the quest start location for the currently selected quest.
     * Converts the packed location coordinates and creates a circular area for navigation.
     */
    public void startNavigationToQuestStart() {
        if (selectedQuest == null) {
            ScriptConsole.println("[QuestHelper] No quest selected for navigation.");
            return;
        }
        
        if (selectedQuestType == null) {
            ScriptConsole.println("[QuestHelper] No QuestType data available for navigation.");
            return;
        }
        
        int[] startLocations = selectedQuestType.startLocations();
        if (startLocations == null || startLocations.length == 0) {
            ScriptConsole.println("[QuestHelper] No start locations found for quest: " + selectedQuest.name());
            return;
        }
        
        int packedLocation = startLocations[0];
        questStartCoordinate = convertPackedLocationToCoordinate(packedLocation);
        
        if (questStartCoordinate == null) {
            ScriptConsole.println("[QuestHelper] Failed to convert start location for quest: " + selectedQuest.name());
            return;
        }
        
        questStartArea = new Area.Circular(questStartCoordinate, 5);
        isNavigatingToQuestStart = true;
        
        ScriptConsole.println("[QuestHelper] Starting navigation to quest start location: " + questStartCoordinate + 
                             " for quest: " + selectedQuest.name());
    }
    
    /**
     * Stops navigation to quest start location.
     */
    public void stopNavigationToQuestStart() {
        isNavigatingToQuestStart = false;
        questStartArea = null;
        questStartCoordinate = null;
        ScriptConsole.println("[QuestHelper] Navigation to quest start stopped.");
    }
    
    /**
     * Checks if currently navigating to quest start location.
     * @return true if navigating, false otherwise
     */
    public boolean isNavigatingToQuestStart() {
        return isNavigatingToQuestStart;
    }
    
    /**
     * Gets the quest start coordinate.
     * @return The quest start coordinate, or null if not set
     */
    public Coordinate getQuestStartCoordinate() {
        return questStartCoordinate;
    }
    
    /**
     * Processes navigation to quest start location.
     * Should be called from the main execute loop when navigation is active.
     */
    private void processQuestStartNavigation() {
        if (!isNavigatingToQuestStart || questStartArea == null) {
            return;
        }
        
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            return;
        }
        
        if (questStartArea.contains(player.getCoordinate())) {
            ScriptConsole.println("[QuestHelper] Arrived at quest start location for: " + selectedQuest.name());
            stopNavigationToQuestStart();
            return;
        }
        
        try {
            Coordinate targetCoordinate = questStartArea.getRandomWalkableCoordinate();
            ScriptConsole.println("[QuestHelper] Target coordinate: " + targetCoordinate);
            if (targetCoordinate != null) {
                NavPath path = NavPath.resolve(targetCoordinate);
                if (path != null) {
                    Movement.traverse(path);
                    ScriptConsole.println("[QuestHelper] Navigating to quest start location...");
                } else {
                    ScriptConsole.println("[QuestHelper] Failed to resolve path to quest start location.");
                    stopNavigationToQuestStart();
                }
            } else {
                ScriptConsole.println("[QuestHelper] Failed to get walkable coordinate in quest start area.");
                stopNavigationToQuestStart();
            }
        } catch (Exception e) {
            ScriptConsole.println("[QuestHelper] Error during navigation: " + e.getMessage());
            stopNavigationToQuestStart();
        }
    }
    
    /**
     * Converts a packed location value to a Coordinate object.
     * @param packedLocation The packed location value
     * @return A Coordinate object, or null if conversion fails
     */
    private Coordinate convertPackedLocationToCoordinate(int packedLocation) {
        try {
            int x = (packedLocation >> 14) & 0x3fff;
            int y = packedLocation & 0x3fff;
            int z = packedLocation >> 28;
            return new Coordinate(x, y, z);
        } catch (Exception e) {
            ScriptConsole.println("[QuestHelper] Error converting packed location " + packedLocation + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Debug method to verify quest loading logic for duplicate names.
     * This method can be called to check if quests with duplicate names are being handled correctly.
     */
    public void debugQuestDuplicates() {
        ScriptConsole.println("[QuestHelper] === QUEST DUPLICATE DEBUG ===");
        
        Map<String, List<Integer>> questNameToIds = new HashMap<>();
        
        for (int i = 0; i < 509; i++) {
            Optional<Quest> questOpt = Quest.byId(i);
            if (questOpt.isPresent()) {
                Quest quest = questOpt.get();
                if (quest.name() != null && !quest.name().trim().isEmpty()) {
                    questNameToIds.computeIfAbsent(quest.name(), k -> new ArrayList<>()).add(i);
                }
            }
        }
        
        for (Map.Entry<String, List<Integer>> entry : questNameToIds.entrySet()) {
            if (entry.getValue().size() > 1) {
                String questName = entry.getKey();
                List<Integer> questIds = entry.getValue();
                
                ScriptConsole.println("[QuestHelper] Duplicate quest found: " + questName);
                
                for (Integer questId : questIds) {
                    Optional<Quest> questOpt = Quest.byId(questId);
                    if (questOpt.isPresent()) {
                        Quest quest = questOpt.get();
                        QuestType questType = ConfigManager.getQuestType(questId);
                        int richness = calculateQuestDataRichness(quest, questType, questId);
                        
                        boolean isInOurList = allQuests.stream().anyMatch(q -> q.name().equals(questName) && getQuestId(q) == questId);
                        
                        ScriptConsole.println("[QuestHelper]   ID " + questId + ": richness=" + richness + 
                                             ", inOurList=" + isInOurList + 
                                             ", hasQuestType=" + (questType != null) +
                                             ", questPoints=" + (questType != null ? questType.questPoints() : "N/A"));
                    }
                }
                
                Quest selectedForName = getQuestByName(questName);
                if (selectedForName != null) {
                    int selectedId = getQuestId(selectedForName);
                    ScriptConsole.println("[QuestHelper]   -> Selected ID: " + selectedId + " for " + questName);
                }
                
                ScriptConsole.println("[QuestHelper]   ---");
            }
        }
        
        ScriptConsole.println("[QuestHelper] === END QUEST DUPLICATE DEBUG ===");
    }

    private static class MatchCandidate {
        final String optionText;
        final int dialogIndex;
        final String sectionName;
        final int sequenceIndex;
        final int optionIndex;
        
        MatchCandidate(String optionText, int dialogIndex, String sectionName, int sequenceIndex, int optionIndex) {
            this.optionText = optionText;
            this.dialogIndex = dialogIndex;
            this.sectionName = sectionName;
            this.sequenceIndex = sequenceIndex;
            this.optionIndex = optionIndex;
        }
    }
    
    /**
     * Finds the index of a fetched option text within the current dialog options.
     * Uses exact lowercase matching after stripping punctuation for accurate comparison.
     * @param currentOptions The current dialog options array
     * @param fetchedOptionText The fetched option text to match
     * @return The index of the matching option, or -1 if no match found
     */
    private int findMatchingOptionIndex(List<String> currentOptions, String fetchedOptionText) {
        if (currentOptions == null || fetchedOptionText == null) {
            return -1;
        }
        
        String fetchedNormalized = fetchedOptionText.toLowerCase().trim().replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", " ");
        
        for (int i = 0; i < currentOptions.size(); i++) {
            String currentOption = currentOptions.get(i);
            if (currentOption == null) continue;
            
            String currentNormalized = currentOption.toLowerCase().trim().replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", " ");
            
            if (currentNormalized.equals(fetchedNormalized)) {
                ScriptConsole.println("[QuestHelper]       ✓ EXACT MATCH: '" + currentOption + "' == '" + fetchedOptionText + "'");
                return i;
            }
        }
        
        ScriptConsole.println("[QuestHelper] NO MATCH found for: '" + fetchedOptionText + "'");
        return -1;
    }
    
    /**
     * Calculates dialog overlay coordinates using enhanced dynamic detection.
     * This method uses the improved UIScaler dialog detection system to avoid hardcoded interface IDs.
     */
    private void calculateDialogOverlayCoordinates() {
        if (uiScaler == null || !uiScaler.isInitialized()) {
            ScriptConsole.println("[QuestHelper] UIScaler not initialized - cannot calculate overlay coordinates");
            clearOverlayCoordinates();
            return;
        }
        
        if (recommendedOptionIndex < 0) {
            ScriptConsole.println("[QuestHelper] No recommended option index available");
            clearOverlayCoordinates();
            return;
        }
        
        ScriptConsole.println("[QuestHelper] Calculating overlay coordinates for option index: " + recommendedOptionIndex);
        
        try {
            List<String> currentOptions = Dialog.getOptions();
            int totalOptions = (currentOptions != null && !currentOptions.isEmpty()) ? currentOptions.size() : 3;
            
            int[] dialogInterfaceIds = {1188, 1186, 1184, 1189, 1191};
            
            for (int interfaceId : dialogInterfaceIds) {
                UIScaler.InterfaceRect dialogRect = uiScaler.getInterfaceRect(interfaceId, -1);
                
                if (dialogRect != null && dialogRect.x >= 0 && dialogRect.y >= 0 && dialogRect.width > 0 && dialogRect.height > 0) {
                    int optionHeight = dialogRect.height / totalOptions;
                    
                    dialogOverlayX = dialogRect.x;
                    dialogOverlayY = dialogRect.y + (recommendedOptionIndex * optionHeight);
                    dialogOverlayWidth = dialogRect.width;
                    dialogOverlayHeight = optionHeight;
                    hasValidOverlayCoordinates = true;
                    
                    ScriptConsole.println(String.format("[QuestHelper] Used simplified positioning for option %d: x=%d, y=%d, w=%d, h=%d (total options: %d)", 
                        recommendedOptionIndex + 1, dialogOverlayX, dialogOverlayY, dialogOverlayWidth, dialogOverlayHeight, totalOptions));
                    return;
                }
            }
            
            ScriptConsole.println("[QuestHelper] Could not detect any dialog interface for overlay positioning");
            clearOverlayCoordinates();
                
        } catch (Exception e) {
            ScriptConsole.println("[QuestHelper] Error calculating dialog overlay coordinates: " + e.getMessage());
            clearOverlayCoordinates();
        }
    }
    
    /**
     * Clears the overlay coordinates
     */
    private void clearOverlayCoordinates() {
        dialogOverlayX = -1;
        dialogOverlayY = -1;
        dialogOverlayWidth = -1;
        dialogOverlayHeight = -1;
        hasValidOverlayCoordinates = false;
    }
    
    /**
     * Gets the dialog overlay coordinates
     * @return int array with [x, y, width, height] or null if not valid
     */
    public int[] getDialogOverlayCoordinates() {
        if (hasValidOverlayCoordinates) {
            return new int[]{dialogOverlayX, dialogOverlayY, dialogOverlayWidth, dialogOverlayHeight};
        }
        return null;
    }
    
    /**
     * Checks if valid overlay coordinates are available
     * @return true if coordinates are valid, false otherwise
     */
    public boolean hasValidOverlayCoordinates() {
        return hasValidOverlayCoordinates;
    }
    
    /**
     * Gets the recommended option text for display
     * @return the recommended option text or null if not available
     */
    public String getRecommendedOptionText() {
        return currentRecommendedOption;
    }

    

    /**
     * Check if step tracking is enabled.
     * @return true if step tracking is enabled, false otherwise
     */
    public boolean isShowStepTracker() {
        return showStepTracker;
    }
    
    /**
     * Enable or disable step tracking.
     * @param showStepTracker true to enable step tracking, false to disable
     */
    public void setShowStepTracker(boolean showStepTracker) {
        this.showStepTracker = showStepTracker;
    }

    /**
     * Gets whether auto-interaction with dialogs is enabled
     */
    public boolean isAutoInteractWithDialogs() {
        return autoInteractWithDialogs;
    }

    /**
     * Sets whether auto-interaction with dialogs is enabled
     */
    public void setAutoInteractWithDialogs(boolean autoInteractWithDialogs) {
        this.autoInteractWithDialogs = autoInteractWithDialogs;
        if (!autoInteractWithDialogs) {
            ScriptConsole.println("[QuestHelper] Auto dialog interaction disabled");
        } else {
            generateNextInteractionDelay();
            ScriptConsole.println("[QuestHelper] Auto dialog interaction enabled");
        }
    }

    /**
     * Checks if we can interact with the dialog (respecting randomized delay)
     */
    private boolean canInteractWithDialog() {
        return System.currentTimeMillis() - lastDialogInteractionTime >= nextInteractionDelay;
    }
    
    /**
     * Generates a new random delay for the next interaction
     */
    private void generateNextInteractionDelay() {
        nextInteractionDelay = MIN_DIALOG_DELAY + random.nextInt((int)(MAX_DIALOG_DELAY - MIN_DIALOG_DELAY + 1));
        ScriptConsole.println("[QuestHelper] Next interaction delay: " + nextInteractionDelay + "ms");
    }
} 