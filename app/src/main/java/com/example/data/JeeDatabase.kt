package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- 1. ENTITY DEFINITIONS ---

@Entity(tableName = "sub_concept_progress")
data class SubConceptProgress(
    @PrimaryKey val subConceptId: String,
    val subjectId: String,
    val chapterId: String,
    val conceptId: String,
    val checkboxes: String = "", // Comma-separated checked box IDs (e.g., "learn_hook,learn_theory,build_twist")
    val confidenceStars: Int = 0, // 0 to 5 stars
    val revisionIntervalDays: Int = 3, // Custom scheduler (e.g., 3, 7, 14 days)
    val lastRevisedTimestamp: Long = 0L,
    val nextDueTimestamp: Long = 0L,
    val doubtStatus: Boolean = false, // true = open doubt, false = resolved/none
    val masteryPercentage: Int = 0 // Auto-calculated mastery % based on progress
)

@Entity(tableName = "daily_practice_tests")
data class DailyPracticeTest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subConceptId: String,
    val subConceptName: String,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val score: Int,
    val totalQuestions: Int,
    val timeTakenSeconds: Int,
    val accuracy: Float, // percentage (e.g., 80.0f)
    val questionsJson: String // Stringified JSON array of generated MCQs, options, answers, and explanations
)

@Entity(tableName = "pyq_trackers")
data class PyqTracker(
    @PrimaryKey val subConceptId: String,
    val doneCount: Int = 0,
    val totalCount: Int = 15,
    val score: Int = 0,
    val timeSpentMinutes: Int = 0,
    val yearsFilter: String = "2019,2020,2021,2022,2023,2024,2025" // Selected years filter
)

@Entity(tableName = "practice_tests")
data class PracticeTest(
    @PrimaryKey val chapterId: String,
    val score: Int = 0,
    val totalQuestions: Int = 10,
    val timeSpentMinutes: Int = 0,
    val completed: Boolean = false
)

@Entity(tableName = "todo_items")
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val task: String,
    val linkedSubConceptId: String = "",
    val linkedSubConceptName: String = "",
    val dueDateTimestamp: Long = System.currentTimeMillis(),
    val priority: String = "Medium", // "Low", "Medium", "High"
    val completed: Boolean = false
)

@Entity(tableName = "study_logs")
data class StudyLog(
    @PrimaryKey val dateString: String, // "YYYY-MM-DD"
    val secondsStudied: Long = 0,
    val conceptsCompletedCount: Int = 0,
    val pyqsSolvedCount: Int = 0,
    val dptsSolvedCount: Int = 0,
    val dptAccuracySum: Float = 0f,
    val physicsSeconds: Long = 0,
    val chemistrySeconds: Long = 0,
    val mathsSeconds: Long = 0
)

// --- 2. DAO (DATA ACCESS OBJECT) ---

@Dao
interface JeeDao {
    // Sub-concept progress
    @Query("SELECT * FROM sub_concept_progress")
    fun getAllProgressFlow(): Flow<List<SubConceptProgress>>

    @Query("SELECT * FROM sub_concept_progress WHERE subConceptId = :subConceptId")
    suspend fun getProgressForSubConcept(subConceptId: String): SubConceptProgress?

    @Query("SELECT * FROM sub_concept_progress WHERE subConceptId = :subConceptId")
    fun getProgressForSubConceptFlow(subConceptId: String): Flow<SubConceptProgress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(progress: SubConceptProgress)

    // Daily practice tests
    @Query("SELECT * FROM daily_practice_tests ORDER BY dateTimestamp DESC")
    fun getAllDptsFlow(): Flow<List<DailyPracticeTest>>

    @Query("SELECT * FROM daily_practice_tests WHERE subConceptId = :subConceptId ORDER BY dateTimestamp DESC")
    fun getDptsForSubConcept(subConceptId: String): Flow<List<DailyPracticeTest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDpt(dpt: DailyPracticeTest)

    // PYQ Trackers
    @Query("SELECT * FROM pyq_trackers")
    fun getAllPyqTrackersFlow(): Flow<List<PyqTracker>>

    @Query("SELECT * FROM pyq_trackers WHERE subConceptId = :subConceptId")
    suspend fun getPyqTracker(subConceptId: String): PyqTracker?

    @Query("SELECT * FROM pyq_trackers WHERE subConceptId = :subConceptId")
    fun getPyqTrackerFlow(subConceptId: String): Flow<PyqTracker?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePyq(pyq: PyqTracker)

    // Chapter practice tests
    @Query("SELECT * FROM practice_tests")
    fun getAllPracticeTestsFlow(): Flow<List<PracticeTest>>

    @Query("SELECT * FROM practice_tests WHERE chapterId = :chapterId")
    suspend fun getPracticeTest(chapterId: String): PracticeTest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePracticeTest(test: PracticeTest)

    // Custom To-Dos
    @Query("SELECT * FROM todo_items ORDER BY dueDateTimestamp ASC")
    fun getAllTodoItemsFlow(): Flow<List<TodoItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTodo(todo: TodoItem)

    @Query("DELETE FROM todo_items WHERE id = :id")
    suspend fun deleteTodoById(id: Int)

    // Study logs for activity insights
    @Query("SELECT * FROM study_logs ORDER BY dateString DESC")
    fun getAllStudyLogsFlow(): Flow<List<StudyLog>>

    @Query("SELECT * FROM study_logs WHERE dateString = :dateString")
    suspend fun getStudyLogForDate(dateString: String): StudyLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStudyLog(log: StudyLog)
}

// --- 3. DATABASE INSTANCE ---

@Database(
    entities = [
        SubConceptProgress::class,
        DailyPracticeTest::class,
        PyqTracker::class,
        PracticeTest::class,
        TodoItem::class,
        StudyLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class JeeDatabase : RoomDatabase() {
    abstract fun jeeDao(): JeeDao

    companion object {
        @Volatile
        private var INSTANCE: JeeDatabase? = null

        fun getDatabase(context: android.content.Context): JeeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JeeDatabase::class.java,
                    "jee_tracker_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
