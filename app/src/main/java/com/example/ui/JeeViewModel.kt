package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class JeeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: JeeRepository

    init {
        val database = JeeDatabase.getDatabase(application)
        repository = JeeRepository(database.jeeDao())
    }

    // --- Core Database Flows ---
    val allProgress: StateFlow<List<SubConceptProgress>> = repository.allProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDpts: StateFlow<List<DailyPracticeTest>> = repository.allDpts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPyqs: StateFlow<List<PyqTracker>> = repository.allPyqTrackers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPracticeTests: StateFlow<List<PracticeTest>> = repository.allPracticeTests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTodos: StateFlow<List<TodoItem>> = repository.allTodoItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStudyLogs: StateFlow<List<StudyLog>> = repository.allStudyLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Study Session Timer ---
    private var activeSessionSubjectId: String? = null
    private var sessionJob: Job? = null
    private val _currentSessionSeconds = MutableStateFlow(0L)
    val currentSessionSeconds: StateFlow<Long> = _currentSessionSeconds.asStateFlow()

    fun startStudySession(subjectId: String) {
        if (activeSessionSubjectId == subjectId) return
        stopStudySession()
        activeSessionSubjectId = subjectId
        _currentSessionSeconds.value = 0L
        sessionJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(1000L)
                _currentSessionSeconds.value += 1L
                // Log progress to DB every 10 seconds to keep it persisted
                if (_currentSessionSeconds.value % 10L == 0L) {
                    repository.addStudySessionTime(subjectId, 10L)
                }
            }
        }
    }

    fun stopStudySession() {
        sessionJob?.cancel()
        sessionJob = null
        val seconds = _currentSessionSeconds.value
        val subject = activeSessionSubjectId
        if (subject != null && seconds > 0) {
            viewModelScope.launch(Dispatchers.IO) {
                val remainingSeconds = seconds % 10L
                if (remainingSeconds > 0) {
                    repository.addStudySessionTime(subject, remainingSeconds)
                }
            }
        }
        activeSessionSubjectId = null
        _currentSessionSeconds.value = 0L
    }

    override fun onCleared() {
        super.onCleared()
        stopStudySession()
    }

    // --- Action Methods ---

    fun toggleCheckbox(subConceptId: String, checkboxId: String, checked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleCheckbox(subConceptId, checkboxId, checked)
        }
    }

    fun updateConfidenceStars(subConceptId: String, stars: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateConfidenceStars(subConceptId, stars)
        }
    }

    fun updateRevisionSchedule(subConceptId: String, intervalDays: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateRevisionSchedule(subConceptId, intervalDays)
        }
    }

    fun markRevisedNow(subConceptId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markRevisedNow(subConceptId)
        }
    }

    fun toggleDoubtStatus(subConceptId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleDoubtStatus(subConceptId)
        }
    }

    fun updatePyqProgress(subConceptId: String, done: Int, total: Int, score: Int, timeMin: Int, years: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePyqProgress(subConceptId, done, total, score, timeMin, years)
        }
    }

    fun savePracticeTest(chapterId: String, score: Int, total: Int, timeMin: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.savePracticeTest(chapterId, score, total, timeMin)
        }
    }

    fun addTodoItem(task: String, linkedSubConceptId: String, priority: String, dueDateMs: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val subName = if (linkedSubConceptId.isNotEmpty()) {
                val concept = JeeHierarchy.getConceptForSubConcept(linkedSubConceptId)
                val sub = JeeHierarchy.getSubConceptsForConcept(concept.id).find { it.id == linkedSubConceptId }
                sub?.name ?: ""
            } else ""
            repository.addTodoItem(task, linkedSubConceptId, subName, priority, dueDateMs)
        }
    }

    fun toggleTodoCompleted(todo: TodoItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTodo(todo.copy(completed = !todo.completed))
        }
    }

    fun deleteTodo(todoId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTodo(todoId)
        }
    }

    // --- Study Guide AI Generator State ---
    private val _studyGuideState = MutableStateFlow<StudyGuideUiState>(StudyGuideUiState.Empty)
    val studyGuideState: StateFlow<StudyGuideUiState> = _studyGuideState.asStateFlow()

    fun fetchStudyGuide(subConceptId: String) {
        _studyGuideState.value = StudyGuideUiState.Loading
        viewModelScope.launch {
            val subConcept = JeeHierarchy.chapters
                .map { JeeHierarchy.getConceptsForChapter(it.id) }.flatten()
                .map { JeeHierarchy.getSubConceptsForConcept(it.id) }.flatten()
                .find { it.id == subConceptId }

            if (subConcept == null) {
                _studyGuideState.value = StudyGuideUiState.Error("Invalid Sub-concept ID")
                return@launch
            }

            val concept = JeeHierarchy.getConceptForSubConcept(subConceptId)
            val chapter = JeeHierarchy.getChapterForConcept(concept.id)
            val subject = JeeHierarchy.getSubjectForChapter(chapter.id)

            val guide = GeminiService.generateStudyGuide(subject.name, chapter.name, subConcept.name)
            _studyGuideState.value = StudyGuideUiState.Success(guide)
        }
    }

    fun clearStudyGuideState() {
        _studyGuideState.value = StudyGuideUiState.Empty
    }

    // --- DPT Active Exam Run State ---
    private val _activeDptState = MutableStateFlow<DptUiState>(DptUiState.Empty)
    val activeDptState: StateFlow<DptUiState> = _activeDptState.asStateFlow()

    private var activeDptTimerJob: Job? = null
    private val _activeDptTimerSeconds = MutableStateFlow(0)
    val activeDptTimerSeconds: StateFlow<Int> = _activeDptTimerSeconds.asStateFlow()

    fun launchDpt(subConceptId: String) {
        _activeDptState.value = DptUiState.Loading
        _activeDptTimerSeconds.value = 0
        activeDptTimerJob?.cancel()

        viewModelScope.launch {
            val subConcept = JeeHierarchy.chapters
                .map { JeeHierarchy.getConceptsForChapter(it.id) }.flatten()
                .map { JeeHierarchy.getSubConceptsForConcept(it.id) }.flatten()
                .find { it.id == subConceptId }

            if (subConcept == null) {
                _activeDptState.value = DptUiState.Error("Invalid Sub-concept")
                return@launch
            }

            val concept = JeeHierarchy.getConceptForSubConcept(subConceptId)
            val chapter = JeeHierarchy.getChapterForConcept(concept.id)
            val subject = JeeHierarchy.getSubjectForChapter(chapter.id)

            val questions = GeminiService.generateDailyPracticeTest(subject.name, chapter.name, subConcept.name)
            
            _activeDptState.value = DptUiState.Running(
                subConceptId = subConceptId,
                subConceptName = subConcept.name,
                questions = questions,
                currentQuestionIndex = 0,
                selectedAnswers = Map(questions.size) { -1 },
                examSubmitted = false,
                isWeakAreaGenerated = false
            )

            // Start test duration timer
            activeDptTimerJob = viewModelScope.launch {
                while (true) {
                    delay(1000L)
                    _activeDptTimerSeconds.value += 1
                }
            }
        }
    }

    fun submitDptAnswer(questionIndex: Int, selectedOptionIndex: Int) {
        val currentState = _activeDptState.value
        if (currentState is DptUiState.Running && !currentState.examSubmitted) {
            val updatedAnswers = currentState.selectedAnswers.toMutableMap()
            updatedAnswers[questionIndex] = selectedOptionIndex
            _activeDptState.value = currentState.copy(selectedAnswers = updatedAnswers)
        }
    }

    fun navigateDptQuestion(index: Int) {
        val currentState = _activeDptState.value
        if (currentState is DptUiState.Running) {
            _activeDptState.value = currentState.copy(currentQuestionIndex = index)
        }
    }

    fun submitDptExam() {
        val currentState = _activeDptState.value
        if (currentState is DptUiState.Running && !currentState.examSubmitted) {
            activeDptTimerJob?.cancel()
            
            var correctCount = 0
            val totalQuestions = currentState.questions.size
            
            currentState.questions.forEachIndexed { index, q ->
                val selected = currentState.selectedAnswers[index]
                if (selected == q.correct_option_index) {
                    correctCount++
                }
            }

            val accuracy = if (totalQuestions > 0) (correctCount.toFloat() / totalQuestions.toFloat()) * 100f else 0f
            val timeTakenSec = _activeDptTimerSeconds.value

            // Format JSON questions for persistence review
            val questionsArray = JSONArray()
            currentState.questions.forEachIndexed { index, q ->
                val qObj = JSONObject().apply {
                    put("question", q.question)
                    put("options", JSONArray(q.options))
                    put("correct_option_index", q.correct_option_index)
                    put("selected_option_index", currentState.selectedAnswers[index] ?: -1)
                    put("explanation", q.explanation)
                }
                questionsArray.put(qObj)
            }

            viewModelScope.launch(Dispatchers.IO) {
                repository.saveDailyPracticeTest(
                    subConceptId = currentState.subConceptId,
                    subConceptName = currentState.subConceptName,
                    score = correctCount,
                    total = totalQuestions,
                    timeSec = timeTakenSec,
                    accuracy = accuracy,
                    questionsJson = questionsArray.toString()
                )
                
                // Toggle DPT checkbox as completed in progress automatically!
                repository.toggleCheckbox(currentState.subConceptId, "test_dpt", true)
            }

            _activeDptState.value = currentState.copy(
                examSubmitted = true,
                scoreResult = correctCount,
                accuracyResult = accuracy
            )
        }
    }

    fun exitDpt() {
        activeDptTimerJob?.cancel()
        _activeDptState.value = DptUiState.Empty
    }

    // --- Dynamic Weak Concept Recommendations ---
    fun getWeakSubConcepts(progressList: List<SubConceptProgress>): List<SubConcept> {
        val weakIds = progressList
            .filter { it.confidenceStars in 1..2 || it.masteryPercentage in 1..40 || it.doubtStatus }
            .map { it.subConceptId }

        val allSubConcepts = JeeHierarchy.chapters
            .map { JeeHierarchy.getConceptsForChapter(it.id) }.flatten()
            .map { JeeHierarchy.getSubConceptsForConcept(it.id) }.flatten()

        val weakList = allSubConcepts.filter { weakIds.contains(it.id) }

        if (weakList.isNotEmpty()) {
            return weakList
        }

        // Fallback: If no tracked weak areas yet, recommend high-yield JEE topics
        return allSubConcepts.filter { it.id.startsWith("sub_phy_ch2_c1") || it.id.startsWith("sub_chem_ch1_c1") || it.id.startsWith("sub_math_ch3_c1") }.take(3)
    }

    // --- Streak calculation wrapper ---
    fun calculateStreak(logs: List<StudyLog>): Int {
        return repository.calculateStreak(logs)
    }

    // --- Helper for Navigation Map creation ---
    private fun Map(size: Int, init: (Int) -> Int): Map<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        for (i in 0 until size) {
            map[i] = init(i)
        }
        return map
    }
}

// --- UI STATE SEALED CLASSES ---

sealed interface StudyGuideUiState {
    object Empty : StudyGuideUiState
    object Loading : StudyGuideUiState
    data class Success(val guide: StudyGuide) : StudyGuideUiState
    data class Error(val message: String) : StudyGuideUiState
}

sealed interface DptUiState {
    object Empty : DptUiState
    object Loading : DptUiState
    data class Running(
        val subConceptId: String,
        val subConceptName: String,
        val questions: List<DptQuestion>,
        val currentQuestionIndex: Int,
        val selectedAnswers: Map<Int, Int>, // Question index -> Chosen option index
        val examSubmitted: Boolean,
        val isWeakAreaGenerated: Boolean = false,
        val scoreResult: Int = 0,
        val accuracyResult: Float = 0f
    ) : DptUiState
    data class Error(val message: String) : DptUiState
}
