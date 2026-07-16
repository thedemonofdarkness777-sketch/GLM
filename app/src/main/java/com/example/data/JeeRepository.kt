package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

class JeeRepository(private val jeeDao: JeeDao) {

    // --- Flows ---
    val allProgress: Flow<List<SubConceptProgress>> = jeeDao.getAllProgressFlow()
    val allDpts: Flow<List<DailyPracticeTest>> = jeeDao.getAllDptsFlow()
    val allPyqTrackers: Flow<List<PyqTracker>> = jeeDao.getAllPyqTrackersFlow()
    val allPracticeTests: Flow<List<PracticeTest>> = jeeDao.getAllPracticeTestsFlow()
    val allTodoItems: Flow<List<TodoItem>> = jeeDao.getAllTodoItemsFlow()
    val allStudyLogs: Flow<List<StudyLog>> = jeeDao.getAllStudyLogsFlow()

    fun getProgressForSubConceptFlow(subConceptId: String): Flow<SubConceptProgress?> {
        return jeeDao.getProgressForSubConceptFlow(subConceptId)
    }

    fun getPyqTrackerFlow(subConceptId: String): Flow<PyqTracker?> {
        return jeeDao.getPyqTrackerFlow(subConceptId)
    }

    fun getDptsForSubConcept(subConceptId: String): Flow<List<DailyPracticeTest>> {
        return jeeDao.getDptsForSubConcept(subConceptId)
    }

    // --- Actions ---

    suspend fun toggleCheckbox(subConceptId: String, checkboxId: String, checked: Boolean) {
        val currentProgress = jeeDao.getProgressForSubConcept(subConceptId) ?: run {
            val concept = JeeHierarchy.getConceptForSubConcept(subConceptId)
            val chapter = JeeHierarchy.getChapterForConcept(concept.id)
            SubConceptProgress(
                subConceptId = subConceptId,
                subjectId = chapter.subjectId,
                chapterId = chapter.id,
                conceptId = concept.id
            )
        }

        val checkedList = currentProgress.checkboxes.split(",")
            .filter { it.isNotEmpty() }
            .toMutableSet()

        if (checked) {
            checkedList.add(checkboxId)
        } else {
            checkedList.remove(checkboxId)
        }

        val newCheckboxesString = checkedList.joinToString(",")
        
        // Let's count mastery based on checked items.
        // There are 9 primary checkboxes in the checklist:
        // learn_hook, learn_theory, learn_examples, build_twist, build_notes, build_mistakes, test_dpt, test_pyq, test_practice
        val checkedCount = checkedList.size
        val totalCheckboxes = 9
        val masteryPercent = ((checkedCount.toFloat() / totalCheckboxes.toFloat()) * 100f).toInt().coerceIn(0, 100)

        val updatedProgress = currentProgress.copy(
            checkboxes = newCheckboxesString,
            masteryPercentage = masteryPercent
        )
        jeeDao.insertOrUpdateProgress(updatedProgress)

        // Log concept activity if checked
        if (checked) {
            incrementConceptCompletedToday()
        }
    }

    suspend fun updateConfidenceStars(subConceptId: String, stars: Int) {
        val currentProgress = jeeDao.getProgressForSubConcept(subConceptId) ?: run {
            val concept = JeeHierarchy.getConceptForSubConcept(subConceptId)
            val chapter = JeeHierarchy.getChapterForConcept(concept.id)
            SubConceptProgress(
                subConceptId = subConceptId,
                subjectId = chapter.subjectId,
                chapterId = chapter.id,
                conceptId = concept.id
            )
        }
        val updated = currentProgress.copy(confidenceStars = stars)
        jeeDao.insertOrUpdateProgress(updated)
    }

    suspend fun updateRevisionSchedule(subConceptId: String, intervalDays: Int) {
        val currentProgress = jeeDao.getProgressForSubConcept(subConceptId) ?: run {
            val concept = JeeHierarchy.getConceptForSubConcept(subConceptId)
            val chapter = JeeHierarchy.getChapterForConcept(concept.id)
            SubConceptProgress(
                subConceptId = subConceptId,
                subjectId = chapter.subjectId,
                chapterId = chapter.id,
                conceptId = concept.id
            )
        }
        val now = System.currentTimeMillis()
        val nextDue = now + (intervalDays.toLong() * 24L * 60L * 60L * 1000L)
        val updated = currentProgress.copy(
            revisionIntervalDays = intervalDays,
            lastRevisedTimestamp = now,
            nextDueTimestamp = nextDue
        )
        jeeDao.insertOrUpdateProgress(updated)
    }

    suspend fun markRevisedNow(subConceptId: String) {
        val currentProgress = jeeDao.getProgressForSubConcept(subConceptId) ?: return
        val now = System.currentTimeMillis()
        val nextDue = now + (currentProgress.revisionIntervalDays.toLong() * 24L * 60L * 60L * 1000L)
        val updated = currentProgress.copy(
            lastRevisedTimestamp = now,
            nextDueTimestamp = nextDue
        )
        jeeDao.insertOrUpdateProgress(updated)
    }

    suspend fun toggleDoubtStatus(subConceptId: String) {
        val currentProgress = jeeDao.getProgressForSubConcept(subConceptId) ?: run {
            val concept = JeeHierarchy.getConceptForSubConcept(subConceptId)
            val chapter = JeeHierarchy.getChapterForConcept(concept.id)
            SubConceptProgress(
                subConceptId = subConceptId,
                subjectId = chapter.subjectId,
                chapterId = chapter.id,
                conceptId = concept.id
            )
        }
        val updated = currentProgress.copy(doubtStatus = !currentProgress.doubtStatus)
        jeeDao.insertOrUpdateProgress(updated)
    }

    // --- PYQs ---
    suspend fun updatePyqProgress(subConceptId: String, done: Int, total: Int, score: Int, timeMin: Int, years: List<Int>) {
        val tracker = PyqTracker(
            subConceptId = subConceptId,
            doneCount = done,
            totalCount = total,
            score = score,
            timeSpentMinutes = timeMin,
            yearsFilter = years.joinToString(",")
        )
        jeeDao.insertOrUpdatePyq(tracker)

        // Log solved questions count
        incrementPyqsSolvedToday(done)
    }

    // --- Practice Test ---
    suspend fun savePracticeTest(chapterId: String, score: Int, total: Int, timeMin: Int) {
        val test = PracticeTest(
            chapterId = chapterId,
            score = score,
            totalQuestions = total,
            timeSpentMinutes = timeMin,
            completed = true
        )
        jeeDao.insertOrUpdatePracticeTest(test)
    }

    // --- To-Do ---
    suspend fun addTodoItem(task: String, linkedSubConceptId: String, linkedSubConceptName: String, priority: String, dueDateMs: Long) {
        val todo = TodoItem(
            task = task,
            linkedSubConceptId = linkedSubConceptId,
            linkedSubConceptName = linkedSubConceptName,
            dueDateTimestamp = dueDateMs,
            priority = priority,
            completed = false
        )
        jeeDao.insertOrUpdateTodo(todo)
    }

    suspend fun toggleTodoCompleted(todoId: Int) {
        // We need to fetch the Todo first. To avoid writing custom fetch, we can just filter all
        // but since it is run in coroutine, we can get the list or do a clean update.
        // For simplicity, we can pass the whole Todo item in toggleTodoCompleted(todo: TodoItem)
    }

    suspend fun updateTodo(todo: TodoItem) {
        jeeDao.insertOrUpdateTodo(todo)
    }

    suspend fun deleteTodo(id: Int) {
        jeeDao.deleteTodoById(id)
    }

    // --- Daily Practice Test ---
    suspend fun saveDailyPracticeTest(subConceptId: String, subConceptName: String, score: Int, total: Int, timeSec: Int, accuracy: Float, questionsJson: String) {
        val dpt = DailyPracticeTest(
            subConceptId = subConceptId,
            subConceptName = subConceptName,
            score = score,
            totalQuestions = total,
            timeTakenSeconds = timeSec,
            accuracy = accuracy,
            questionsJson = questionsJson
        )
        jeeDao.insertDpt(dpt)
        incrementDptSolvedToday(accuracy)
    }

    // --- Study Logging ---
    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    suspend fun addStudySessionTime(subjectId: String, seconds: Long) {
        val today = getTodayDateString()
        val currentLog = jeeDao.getStudyLogForDate(today) ?: StudyLog(dateString = today)
        
        val updatedLog = when (subjectId.lowercase()) {
            "physics" -> currentLog.copy(
                secondsStudied = currentLog.secondsStudied + seconds,
                physicsSeconds = currentLog.physicsSeconds + seconds
            )
            "chemistry" -> currentLog.copy(
                secondsStudied = currentLog.secondsStudied + seconds,
                chemistrySeconds = currentLog.chemistrySeconds + seconds
            )
            "maths", "mathematics" -> currentLog.copy(
                secondsStudied = currentLog.secondsStudied + seconds,
                mathsSeconds = currentLog.mathsSeconds + seconds
            )
            else -> currentLog.copy(
                secondsStudied = currentLog.secondsStudied + seconds
            )
        }
        jeeDao.insertOrUpdateStudyLog(updatedLog)
    }

    private suspend fun incrementConceptCompletedToday() {
        val today = getTodayDateString()
        val log = jeeDao.getStudyLogForDate(today) ?: StudyLog(dateString = today)
        jeeDao.insertOrUpdateStudyLog(log.copy(conceptsCompletedCount = log.conceptsCompletedCount + 1))
    }

    private suspend fun incrementPyqsSolvedToday(count: Int) {
        val today = getTodayDateString()
        val log = jeeDao.getStudyLogForDate(today) ?: StudyLog(dateString = today)
        jeeDao.insertOrUpdateStudyLog(log.copy(pyqsSolvedCount = log.pyqsSolvedCount + count))
    }

    private suspend fun incrementDptSolvedToday(accuracy: Float) {
        val today = getTodayDateString()
        val log = jeeDao.getStudyLogForDate(today) ?: StudyLog(dateString = today)
        val newCount = log.dptsSolvedCount + 1
        jeeDao.insertOrUpdateStudyLog(log.copy(
            dptsSolvedCount = newCount,
            dptAccuracySum = log.dptAccuracySum + accuracy
        ))
    }

    // --- Streak & Analytics Helper ---
    fun calculateStreak(logs: List<StudyLog>): Int {
        if (logs.isEmpty()) return 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val studiedDates = logs
            .filter { it.secondsStudied > 0 }
            .map { it.dateString }
            .toSet()

        if (studiedDates.isEmpty()) return 0

        val cal = Calendar.getInstance()
        var streak = 0
        
        // Check if user studied today or yesterday. If neither, streak is 0.
        val todayStr = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)
        
        if (!studiedDates.contains(todayStr) && !studiedDates.contains(yesterdayStr)) {
            return 0
        }

        // Calculate consecutive days going backward
        cal.time = Date() // Reset to today
        while (true) {
            val checkStr = sdf.format(cal.time)
            if (studiedDates.contains(checkStr)) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                // If it's today and not studied yet, keep checking yesterday
                if (checkStr == todayStr) {
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    continue
                }
                break
            }
        }
        return streak
    }
}
