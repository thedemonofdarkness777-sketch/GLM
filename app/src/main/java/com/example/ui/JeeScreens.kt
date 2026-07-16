package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Screen Identifiers for state-based navigation
enum class Screen {
    Courses,
    Todo,
    Insights
}

@Composable
fun JeeTrackerApp(viewModel: JeeViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.Courses) }
    var activeSubConceptIdForDetail by remember { mutableStateOf<String?>(null) }

    val allProgress by viewModel.allProgress.collectAsStateWithLifecycle()
    val allDpts by viewModel.allDpts.collectAsStateWithLifecycle()
    val allPyqs by viewModel.allPyqs.collectAsStateWithLifecycle()
    val allPracticeTests by viewModel.allPracticeTests.collectAsStateWithLifecycle()
    val allTodos by viewModel.allTodos.collectAsStateWithLifecycle()
    val allStudyLogs by viewModel.allStudyLogs.collectAsStateWithLifecycle()

    val currentSessionSec by viewModel.currentSessionSeconds.collectAsStateWithLifecycle()
    val activeDptState by viewModel.activeDptState.collectAsStateWithLifecycle()

    // Adaptive design check: screen width grouping
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 600.dp

        Scaffold(
            bottomBar = {
                if (activeDptState is DptUiState.Empty) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == Screen.Courses && activeSubConceptIdForDetail == null,
                            onClick = {
                                currentScreen = Screen.Courses
                                activeSubConceptIdForDetail = null
                            },
                            icon = { Icon(Icons.Default.MenuBook, contentDescription = "Courses") },
                            label = { Text("Courses") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF201A19),
                                selectedTextColor = Color(0xFF201A19),
                                indicatorColor = Color(0xFFFFDBCB),
                                unselectedIconColor = Color(0xFF85736E),
                                unselectedTextColor = Color(0xFF85736E)
                            ),
                            modifier = Modifier.testTag("nav_courses")
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.Todo,
                            onClick = {
                                currentScreen = Screen.Todo
                                activeSubConceptIdForDetail = null
                            },
                            icon = { Icon(Icons.Default.PlaylistAddCheck, contentDescription = "To-Do Zone") },
                            label = { Text("To-Do Zone") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF201A19),
                                selectedTextColor = Color(0xFF201A19),
                                indicatorColor = Color(0xFFFFDBCB),
                                unselectedIconColor = Color(0xFF85736E),
                                unselectedTextColor = Color(0xFF85736E)
                            ),
                            modifier = Modifier.testTag("nav_todo")
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.Insights,
                            onClick = {
                                currentScreen = Screen.Insights
                                activeSubConceptIdForDetail = null
                            },
                            icon = { Icon(Icons.Default.BarChart, contentDescription = "Insights") },
                            label = { Text("Insights") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF201A19),
                                selectedTextColor = Color(0xFF201A19),
                                indicatorColor = Color(0xFFFFDBCB),
                                unselectedIconColor = Color(0xFF85736E),
                                unselectedTextColor = Color(0xFF85736E)
                            ),
                            modifier = Modifier.testTag("nav_insights")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Main screen transitions
                when {
                    activeSubConceptIdForDetail != null -> {
                        ConceptDetailScreen(
                            subConceptId = activeSubConceptIdForDetail!!,
                            viewModel = viewModel,
                            allProgress = allProgress,
                            allDpts = allDpts,
                            allPyqs = allPyqs,
                            allPracticeTests = allPracticeTests,
                            onBack = { activeSubConceptIdForDetail = null }
                        )
                    }
                    currentScreen == Screen.Courses -> {
                        CoursesScreen(
                            viewModel = viewModel,
                            allProgress = allProgress,
                            isWideScreen = isWideScreen,
                            currentSessionSec = currentSessionSec,
                            onSubConceptSelect = { subId -> activeSubConceptIdForDetail = subId },
                            onNavigateToTodo = { currentScreen = Screen.Todo }
                        )
                    }
                    currentScreen == Screen.Todo -> {
                        TodoZoneScreen(
                            viewModel = viewModel,
                            allTodos = allTodos,
                            onLinkSelect = { subId -> activeSubConceptIdForDetail = subId }
                        )
                    }
                    currentScreen == Screen.Insights -> {
                        InsightsScreen(
                            viewModel = viewModel,
                            allProgress = allProgress,
                            allStudyLogs = allStudyLogs,
                            allDpts = allDpts
                        )
                    }
                }

                // Immersive Active Dpt Screen Overlay
                if (activeDptState !is DptUiState.Empty) {
                    ActiveDptScreen(
                        state = activeDptState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

// ==========================================
// A) COURSES SCREEN
// ==========================================

@Composable
fun CoursesScreen(
    viewModel: JeeViewModel,
    allProgress: List<SubConceptProgress>,
    isWideScreen: Boolean,
    currentSessionSec: Long,
    onSubConceptSelect: (String) -> Unit,
    onNavigateToTodo: () -> Unit
) {
    var selectedSubjectId by remember { mutableStateOf("physics") }
    var expandedChapterId by remember { mutableStateOf<String?>(null) }

    val filteredChapters = remember(selectedSubjectId) {
        JeeHierarchy.chapters.filter { it.subjectId == selectedSubjectId }
    }

    // Dynamic session timer display
    val sessionActive = currentSessionSec > 0

    // Dynamic stats computations for the header/cards
    val allStudyLogs by viewModel.allStudyLogs.collectAsStateWithLifecycle()
    val streak = remember(allStudyLogs) { viewModel.calculateStreak(allStudyLogs) }

    val subjectProgress = remember(allProgress) {
        JeeHierarchy.subjects.associate { subject ->
            val subjectChapters = JeeHierarchy.chapters.filter { it.subjectId == subject.id }
            val chapterConcepts = subjectChapters.flatMap { JeeHierarchy.getConceptsForChapter(it.id) }
            val subConcepts = chapterConcepts.flatMap { JeeHierarchy.getSubConceptsForConcept(it.id) }
            val progressMap = allProgress.associateBy { it.subConceptId }
            val avg = if (subConcepts.isNotEmpty()) {
                subConcepts.sumOf { progressMap[it.id]?.masteryPercentage ?: 0 } / subConcepts.size
            } else 0
            subject.id to avg
        }
    }

    val lastStudiedSubConcept = remember(allProgress) {
        val inProgress = allProgress.filter { it.masteryPercentage in 1..99 }
        if (inProgress.isNotEmpty()) {
            val sorted = inProgress.sortedByDescending { it.confidenceStars }
            val subId = sorted.first().subConceptId
            JeeHierarchy.chapters
                .flatMap { JeeHierarchy.getConceptsForChapter(it.id) }
                .flatMap { JeeHierarchy.getSubConceptsForConcept(it.id) }
                .find { it.id == subId }
        } else {
            JeeHierarchy.chapters
                .flatMap { JeeHierarchy.getConceptsForChapter(it.id) }
                .flatMap { JeeHierarchy.getSubConceptsForConcept(it.id) }
                .find { it.id == "sub_phy_ch2_c1_s1" }
        }
    }

    val lastStudiedProgress = remember(allProgress, lastStudiedSubConcept) {
        allProgress.find { it.subConceptId == lastStudiedSubConcept?.id }?.masteryPercentage ?: 75
    }

    val lastStudiedConcept = remember(lastStudiedSubConcept) {
        if (lastStudiedSubConcept != null) {
            JeeHierarchy.getConceptForSubConcept(lastStudiedSubConcept.id)
        } else null
    }

    val lastStudiedChapter = remember(lastStudiedConcept) {
        if (lastStudiedConcept != null) {
            JeeHierarchy.chapters.find { it.id == lastStudiedConcept.chapterId }
        } else null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Natural Tones Header Section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "JEE TRACKER • 2025",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF85736E),
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Welcome back,\nAditya",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFF201A19),
                        lineHeight = 32.sp
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Streak Badge
                    Surface(
                        color = Color(0xFFF4DDCC),
                        shape = RoundedCornerShape(50),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🔥", fontSize = 14.sp)
                            Text(
                                text = "$streak Day Streak",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF554335)
                            )
                        }
                    }

                    // Avatar Circle
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8DEF8))
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF21005D),
                            style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        )
                    }
                }
            }
        }

        // 2. Subject Selection Tabs (Physics, Chemistry, Mathematics)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                JeeHierarchy.subjects.forEach { subject ->
                    val isSelected = selectedSubjectId == subject.id
                    val progress = subjectProgress[subject.id] ?: 0

                    val (boxBg, activeRingColor, trackRingColor) = when (subject.id) {
                        "physics" -> Triple(Color(0xFFE6E0E9), Color(0xFF6750A4), Color(0xFFD1C4E9))
                        "chemistry" -> Triple(Color(0xFFD7E3FF), Color(0xFF0056D2), Color(0xFFC2D5FF))
                        else -> Triple(Color(0xFFF2E7D3), Color(0xFF705D00), Color(0xFFE5D9C2)) // Maths
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedSubjectId = subject.id
                                expandedChapterId = null
                                if (sessionActive) {
                                    viewModel.startStudySession(subject.id)
                                }
                            }
                            .testTag("subject_card_${subject.id}"),
                        colors = CardDefaults.cardColors(containerColor = boxBg),
                        border = if (isSelected) BorderStroke(2.dp, Color(0xFF201A19)) else null,
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier.size(44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawArc(
                                        color = trackRingColor,
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = 3.dp.toPx())
                                    )
                                    drawArc(
                                        color = activeRingColor,
                                        startAngle = -90f,
                                        sweepAngle = (progress.toFloat() / 100f) * 360f,
                                        useCenter = false,
                                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                                Text(
                                    text = "$progress%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF201A19)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = subject.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF201A19)
                            )
                        }
                    }
                }
            }
        }

        // 3. Continue Learning Card
        item {
            val lastSub = lastStudiedSubConcept
            val lastProg = lastStudiedProgress
            val lastChapName = lastStudiedChapter?.name ?: "Electrostatic Potential & Capacitance"
            val lastSubName = lastSub?.name ?: "Work Done to Move a Charge"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2928))
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(80.dp),
                        tint = Color.White.copy(alpha = 0.05f)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = if (sessionActive) "ACTIVE STUDY SESSION" else "RESUME STUDYING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC7C7CC),
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = lastChapName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "Sub-concept: $lastSubName",
                            fontSize = 13.sp,
                            color = Color(0xFFA09D9C),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .background(Color(0xFF4B4543), CircleShape)
                                    .clip(CircleShape)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = (lastProg.toFloat() / 100f).coerceIn(0f, 1f))
                                        .background(Color(0xFFD4E157), CircleShape)
                                )
                            }
                            Text(
                                text = "$lastProg%",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (sessionActive) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Card(
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(48.dp)
                                        .clickable {
                                            if (lastSub != null) {
                                                onSubConceptSelect(lastSub.id)
                                            }
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD4E157)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Open Checklist",
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2D2928),
                                                fontSize = 13.sp
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowForward,
                                                contentDescription = null,
                                                tint = Color(0xFF2D2928),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clickable { viewModel.stopStudySession() },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4B4543)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Pause,
                                                contentDescription = "Pause",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Pause",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "Session duration: ${formatTime(currentSessionSec)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD4E157),
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        } else {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clickable {
                                        if (lastSub != null) {
                                            onSubConceptSelect(lastSub.id)
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFD4E157)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Open Checklist",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2D2928),
                                            fontSize = 14.sp
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = null,
                                            tint = Color(0xFF2D2928),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { viewModel.startStudySession(selectedSubjectId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f), contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("btn_start_session")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Study Session Timer", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 4. Weekly Insights Mini Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF2EDE8))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DPT Performance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF524441)
                        )
                        Text(
                            text = "PAST 7 DAYS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF85736E),
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val heights = listOf(0.5f, 0.75f, 1.0f, 0.66f, 0.82f, 0.6f, 0.4f)
                        heights.forEachIndexed { idx, frac ->
                            val isActive = idx == 4
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .fillMaxHeight(frac)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(if (isActive) Color(0xFFD4E157) else Color(0xFFF7ECE7)),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                if (isActive) {
                                    Text(
                                        text = "82%",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF201A19),
                                        modifier = Modifier.offset(y = (-16).dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. To-Do Preview Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFDBCB)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Next PYQ Goal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF341100)
                        )
                        Column {
                            Text(
                                text = "20 Questions",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF341100)
                            )
                            Text(
                                text = "2023 Shift 1",
                                fontSize = 12.sp,
                                color = Color(0xFF341100).copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .width(76.dp)
                        .height(100.dp)
                        .clickable { onNavigateToTodo() },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF2EDE8)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("➕", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "New Task",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF85736E),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "NCERT Chapters & Progress",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Chapters List
        items(filteredChapters) { chapter ->
            val isExpanded = expandedChapterId == chapter.id

            // Calculate chapter mastery statistics
            val chapterConcepts = remember(chapter.id) { JeeHierarchy.getConceptsForChapter(chapter.id) }
            val chapterSubConcepts = remember(chapterConcepts) {
                chapterConcepts.flatMap { JeeHierarchy.getSubConceptsForConcept(it.id) }
            }
            val totalSubConcepts = chapterSubConcepts.size
            val progressList = allProgress.filter { it.chapterId == chapter.id }
            val avgMastery = if (progressList.isNotEmpty()) {
                val sum = progressList.sumOf { it.masteryPercentage }
                sum / totalSubConcepts
            } else 0

            val masteryLabel = when {
                avgMastery == 0 -> "Not Started"
                avgMastery < 40 -> "In Progress"
                avgMastery < 80 -> "Proficient"
                else -> "Mastered"
            }

            val masteryColor = when {
                avgMastery == 0 -> Color.Gray
                avgMastery < 40 -> Color(0xFFFFA726)
                avgMastery < 80 -> Color(0xFF29B6F6)
                else -> Color(0xFF66BB6A)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chapter_card_${chapter.id}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedChapterId = if (isExpanded) null else chapter.id }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Progress ring on left
                        Box(
                            modifier = Modifier.size(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = Color.LightGray.copy(alpha = 0.3f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = masteryColor,
                                    startAngle = -90f,
                                    sweepAngle = (avgMastery.toFloat() / 100f) * 360f,
                                    useCenter = false,
                                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = "$avgMastery%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = masteryColor
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Chapter Title & Info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Chapter ${chapter.order}: ${chapter.name}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = masteryColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = masteryLabel,
                                        color = masteryColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$totalSubConcepts Concept Topics",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { expandedChapterId = if (isExpanded) null else chapter.id }) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle"
                            )
                        }
                    }

                    // Expanded Concepts and Subconcepts list
                    if (isExpanded) {
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                                .padding(bottom = 8.dp)
                        ) {
                            chapterConcepts.forEach { concept ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary,
                                                    CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = concept.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Sub-concepts inside concept
                                    val subConcepts = JeeHierarchy.getSubConceptsForConcept(concept.id)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 14.dp, top = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        subConcepts.forEach { subConcept ->
                                            val progress = allProgress.find { it.subConceptId == subConcept.id }
                                            val mastery = progress?.masteryPercentage ?: 0
                                            val doubt = progress?.doubtStatus ?: false

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { onSubConceptSelect(subConcept.id) }
                                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = if (mastery >= 80) Color(0xFF66BB6A) else if (mastery > 0) Color(0xFFFFA726) else Color.LightGray.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = subConcept.name,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (doubt) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            color = Color.Red.copy(alpha = 0.1f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                "Doubt",
                                                                color = Color.Red,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (mastery > 0) {
                                                        Text(
                                                            "$mastery% Mastered",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = Icons.Default.ChevronRight,
                                                        contentDescription = "Go",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// ==========================================
// B) CONCEPT DETAIL PAGE (CHECKLIST CARD)
// ==========================================

@Composable
fun ConceptDetailScreen(
    subConceptId: String,
    viewModel: JeeViewModel,
    allProgress: List<SubConceptProgress>,
    allDpts: List<DailyPracticeTest>,
    allPyqs: List<PyqTracker>,
    allPracticeTests: List<PracticeTest>,
    onBack: () -> Unit
) {
    val subConcept = remember(subConceptId) {
        JeeHierarchy.chapters
            .flatMap { JeeHierarchy.getConceptsForChapter(it.id) }
            .flatMap { JeeHierarchy.getSubConceptsForConcept(it.id) }
            .find { it.id == subConceptId }!!
    }

    val concept = remember(subConceptId) { JeeHierarchy.getConceptForSubConcept(subConceptId) }
    val chapter = remember(subConceptId) { JeeHierarchy.getChapterForConcept(concept.id) }
    val subject = remember(subConceptId) { JeeHierarchy.getSubjectForChapter(chapter.id) }

    val progress = allProgress.find { it.subConceptId == subConceptId }
    val mastery = progress?.masteryPercentage ?: 0
    val checkedBoxes = remember(progress) {
        progress?.checkboxes?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Phase 1: Learn", "Phase 2: Build", "Phase 3: Test", "Phase 4: Retain")

    // For PYQ tracker inputs
    val pyq = allPyqs.find { it.subConceptId == subConceptId }
    var pyqDoneInput by remember(pyq) { mutableStateOf(pyq?.doneCount?.toString() ?: "0") }
    var pyqTotalInput by remember(pyq) { mutableStateOf(pyq?.totalCount?.toString() ?: "15") }
    var pyqScoreInput by remember(pyq) { mutableStateOf(pyq?.score?.toString() ?: "0") }
    var pyqTimeInput by remember(pyq) { mutableStateOf(pyq?.timeSpentMinutes?.toString() ?: "0") }

    val yearsFilter = remember(pyq) {
        pyq?.yearsFilter?.split(",")?.filter { it.isNotEmpty() }?.mapNotNull { it.toIntOrNull() }?.toSet()
            ?: setOf(2019, 2020, 2021, 2022, 2023, 2024, 2025)
    }

    val studyGuideState by viewModel.studyGuideState.collectAsStateWithLifecycle()

    // Automatically trigger study session timer on enter
    LaunchedEffect(subConceptId) {
        viewModel.startStudySession(subject.id)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Immersive Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subConcept.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${subject.name} • Chapter ${chapter.order}: ${chapter.name}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Circular progress ring showing Mastery % on top bar
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.2f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawArc(
                        color = Color(0xFF66BB6A),
                        startAngle = -90f,
                        sweepAngle = (mastery.toFloat() / 100f) * 360f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "$mastery%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF66BB6A)
                )
            }
        }

        // Tab bar switcher
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 11.sp, maxLines = 1) }
                )
            }
        }

        // Checklist body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // PHASE 1: LEARN
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Checkboxes Checklist Card
                        item {
                            PhaseChecklistHeader(
                                phaseTitle = "Phase 1: Acquire Fundamentals",
                                checkboxes = listOf(
                                    "learn_hook" to "Hook & Why - Real-world application hook",
                                    "learn_theory" to "How it works fundamentally - Detailed derivation & theory",
                                    "learn_examples" to "Beginner Examples - Step-by-step solved questions"
                                ),
                                checkedBoxes = checkedBoxes,
                                onToggle = { key, checked -> viewModel.toggleCheckbox(subConceptId, key, checked) }
                            )
                        }

                        // Detailed study notes via AI or template
                        item {
                            StudyContentSection(
                                studyGuideState = studyGuideState,
                                subConceptId = subConceptId,
                                subConceptName = subConcept.name,
                                subjectName = subject.name,
                                chapterName = chapter.name,
                                viewModel = viewModel
                            )
                        }
                    }
                }
                1 -> {
                    // PHASE 2: BUILD
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            PhaseChecklistHeader(
                                phaseTitle = "Phase 2: Build Technical Rigor",
                                checkboxes = listOf(
                                    "build_twist" to "Twist Problems - Special exceptions + application note points",
                                    "build_notes" to "Short Notes & Formulas - Quick references sheet",
                                    "build_mistakes" to "Mistake / Trap Points - Sign conventions & concept traps"
                                ),
                                checkedBoxes = checkedBoxes,
                                onToggle = { key, checked -> viewModel.toggleCheckbox(subConceptId, key, checked) }
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("💡 Quick Build Guidance", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "For this topic, make sure to compile a personalized short formula notebook. Most errors in JEE result from algebraic simplifications or forgetting the boundary parameters.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { selectedTab = 0 }, // Go back to learn guide
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Open Study Guide with Twist Scenarios", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // PHASE 3: TEST (Score & Time Tracking)
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            PhaseChecklistHeader(
                                phaseTitle = "Phase 3: Rigorous Testing",
                                checkboxes = listOf(
                                    "test_dpt" to "DPT - Daily Practice Test with AI",
                                    "test_pyq" to "PYQs - High score in past years questions",
                                    "test_practice" to "Practice Test - Chapter-level complete mock"
                                ),
                                checkedBoxes = checkedBoxes,
                                onToggle = { key, checked -> viewModel.toggleCheckbox(subConceptId, key, checked) }
                            )
                        }

                        // A) DPT Generator Box
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🤖 AI DPT Generator", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "5 Questions",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Click to generate a unique 5-question Daily Practice Test tailored to this concept using the Gemini AI model. Tracks your score, accuracy, and total time.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.launchDpt(subConceptId) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("btn_generate_dpt")
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Generate Daily Practice Test", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // B) PYQs Year Filter and Logger
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("📅 PYQ Tracker (JEE Mains 2019 - 2025)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Interactive years select toggles
                                    Text("Year Filters:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(2019, 2020, 2021, 2022, 2023, 2024, 2025).forEach { year ->
                                            val isSelected = yearsFilter.contains(year)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    val newList = yearsFilter.toMutableSet()
                                                    if (isSelected) newList.remove(year) else newList.add(year)
                                                    viewModel.updatePyqProgress(
                                                        subConceptId,
                                                        pyqDoneInput.toIntOrNull() ?: 0,
                                                        pyqTotalInput.toIntOrNull() ?: 15,
                                                        pyqScoreInput.toIntOrNull() ?: 0,
                                                        pyqTimeInput.toIntOrNull() ?: 0,
                                                        newList.toList()
                                                    )
                                                },
                                                label = { Text(year.toString(), fontSize = 11.sp) }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Score logger fields
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = pyqDoneInput,
                                            onValueChange = { pyqDoneInput = it },
                                            label = { Text("Solved", fontSize = 10.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = pyqTotalInput,
                                            onValueChange = { pyqTotalInput = it },
                                            label = { Text("Total Target", fontSize = 10.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = pyqScoreInput,
                                            onValueChange = { pyqScoreInput = it },
                                            label = { Text("Avg Score %", fontSize = 10.sp) },
                                            modifier = Modifier.weight(1.2f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = pyqTimeInput,
                                            onValueChange = { pyqTimeInput = it },
                                            label = { Text("Time (min)", fontSize = 10.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            viewModel.updatePyqProgress(
                                                subConceptId,
                                                pyqDoneInput.toIntOrNull() ?: 0,
                                                pyqTotalInput.toIntOrNull() ?: 15,
                                                pyqScoreInput.toIntOrNull() ?: 0,
                                                pyqTimeInput.toIntOrNull() ?: 0,
                                                yearsFilter.toList()
                                            )
                                            // Toggle PYQ checkbox as done automatically!
                                            viewModel.toggleCheckbox(subConceptId, "test_pyq", true)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Save PYQ Progress", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // C) Practice Mock Test Score Tracker
                        item {
                            val mockTest = allPracticeTests.find { it.chapterId == chapter.id }
                            var mockScore by remember(mockTest) { mutableStateOf(mockTest?.score?.toString() ?: "0") }
                            var mockTotal by remember(mockTest) { mutableStateOf(mockTest?.totalQuestions?.toString() ?: "10") }
                            var mockTime by remember(mockTest) { mutableStateOf(mockTest?.timeSpentMinutes?.toString() ?: "0") }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("📝 Chapter Mock Test Logger", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Log scores of any external or text-book mocks of this chapter.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = mockScore,
                                            onValueChange = { mockScore = it },
                                            label = { Text("Correct Qs") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = mockTotal,
                                            onValueChange = { mockTotal = it },
                                            label = { Text("Total Qs") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = mockTime,
                                            onValueChange = { mockTime = it },
                                            label = { Text("Time (min)") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            viewModel.savePracticeTest(
                                                chapter.id,
                                                mockScore.toIntOrNull() ?: 0,
                                                mockTotal.toIntOrNull() ?: 10,
                                                mockTime.toIntOrNull() ?: 0
                                            )
                                            // Toggle mock checkbox
                                            viewModel.toggleCheckbox(subConceptId, "test_practice", true)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Log Chapter Mock Results", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // PHASE 4: RETAIN
                    val dueStatus = progress?.nextDueTimestamp ?: 0L
                    val formattedLastDue = if (progress?.lastRevisedTimestamp != null && progress.lastRevisedTimestamp > 0L) {
                        SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date(progress.lastRevisedTimestamp))
                    } else "Never"

                    val formattedNextDue = if (dueStatus > 0L) {
                        SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date(dueStatus))
                    } else "Not scheduled yet"

                    var isScheduleDropdownExpanded by remember { mutableStateOf(false) }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Header
                        item {
                            Text(
                                "Phase 4: Retention, Spaced Revision, and Doubt Resolution",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Confidence Rating Stars
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("⭐ Concept Confidence Level", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Rate how confident you feel to solve JEE Mains questions", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        (1..5).forEach { stars ->
                                            val isSelected = stars <= (progress?.confidenceStars ?: 0)
                                            IconButton(
                                                onClick = { viewModel.updateConfidenceStars(subConceptId, stars) },
                                                modifier = Modifier.testTag("star_$stars")
                                            ) {
                                                Icon(
                                                    imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                                                    contentDescription = "$stars Stars",
                                                    tint = if (isSelected) Color(0xFFFFB300) else Color.LightGray,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Custom Revision Scheduler
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("🕒 Custom Spaced Revision Scheduler", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Last Revised: $formattedLastDue", fontSize = 12.sp)
                                            Text("Next Due: $formattedNextDue", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (dueStatus > 0 && dueStatus <= System.currentTimeMillis()) Color.Red else MaterialTheme.colorScheme.onSurface)
                                            Text("Current Interval: ${progress?.revisionIntervalDays ?: 3} days", fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = { viewModel.markRevisedNow(subConceptId) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                        ) {
                                            Text("Mark Revised", fontSize = 11.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Scheduler Option Selection
                                    Text("Set Spaced Interval:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(3, 7, 14, 30).forEach { days ->
                                            val isSelected = progress?.revisionIntervalDays == days
                                            ElevatedFilterChip(
                                                selected = isSelected,
                                                onClick = { viewModel.updateRevisionSchedule(subConceptId, days) },
                                                label = { Text("$days Days") }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Doubt status Toggle Box
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("❓ Doubt Flag Status", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = "Flag this topic to consult teachers or study again.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    val hasDoubt = progress?.doubtStatus ?: false
                                    Switch(
                                        checked = hasDoubt,
                                        onCheckedChange = { viewModel.toggleDoubtStatus(subConceptId) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Red,
                                            checkedTrackColor = Color.Red.copy(alpha = 0.3f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Subcomponents helper
@Composable
fun PhaseChecklistHeader(
    phaseTitle: String,
    checkboxes: List<Pair<String, String>>,
    checkedBoxes: Set<String>,
    onToggle: (String, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = phaseTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            checkboxes.forEach { (key, desc) ->
                val isChecked = checkedBoxes.contains(key)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(key, !isChecked) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onToggle(key, it) },
                        modifier = Modifier.testTag("chk_$key")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

// AI Study Content generation view
@Composable
fun StudyContentSection(
    studyGuideState: StudyGuideUiState,
    subConceptId: String,
    subConceptName: String,
    subjectName: String,
    chapterName: String,
    viewModel: JeeViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📚 NCERT Study Guide", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                
                IconButton(onClick = { viewModel.fetchStudyGuide(subConceptId) }) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Regenerate with AI",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (studyGuideState) {
                is StudyGuideUiState.Empty -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Get detailed NCERT study notes, equations, derivations, beginner examples, twists, and mistake traps.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.fetchStudyGuide(subConceptId) },
                            modifier = Modifier.testTag("btn_deep_dive_ai")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate AI Deep Dive notes")
                        }
                    }
                }
                is StudyGuideUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Gemini is assembling NCERT guides & derivations...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                is StudyGuideUiState.Success -> {
                    val guide = studyGuideState.guide
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Hook Card
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = guide.hook,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        // Fundamentals
                        Text("🏫 Core Fundamentals & Derivation", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(guide.fundamentals, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Examples
                        Text("📐 Beginner Solved Examples", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(guide.examples, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Twists
                        Text("🔥 Advanced Twist Scenarios", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(guide.twists, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Traps
                        Surface(
                            color = Color.Red.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = "Trap", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Common Sign Convention / Mistake Traps", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Red)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(guide.mistakes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
                is StudyGuideUiState.Error -> {
                    Text("Error: ${studyGuideState.message}", color = Color.Red, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.fetchStudyGuide(subConceptId) }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

// ==========================================
// C) INSIGHTS SCREEN
// ==========================================

@Composable
fun InsightsScreen(
    viewModel: JeeViewModel,
    allProgress: List<SubConceptProgress>,
    allStudyLogs: List<StudyLog>,
    allDpts: List<DailyPracticeTest>
) {
    var selectedRange by remember { mutableStateOf(0) } // 0 = Daily, 1 = Weekly, 2 = Monthly
    val ranges = listOf("Daily", "Weekly", "Monthly")

    // Dynamic stats computation
    val streak = remember(allStudyLogs) { viewModel.calculateStreak(allStudyLogs) }
    
    val totalSecondsStudied = remember(allStudyLogs) { allStudyLogs.sumOf { it.secondsStudied } }
    val totalMinutes = totalSecondsStudied / 60

    val solvedPyqs = remember(allStudyLogs) { allStudyLogs.sumOf { it.pyqsSolvedCount } }
    val solvedDptsCount = allDpts.size
    
    val masteredCount = remember(allProgress) { allProgress.filter { it.masteryPercentage >= 80 }.size }

    val dptAccuracy = remember(allDpts) {
        if (allDpts.isNotEmpty()) {
            allDpts.sumOf { it.accuracy.toDouble() } / allDpts.size
        } else 0.0
    }

    // Pie chart / distribution stats by subject
    val totalPhy = allStudyLogs.sumOf { it.physicsSeconds }
    val totalChem = allStudyLogs.sumOf { it.chemistrySeconds }
    val totalMath = allStudyLogs.sumOf { it.mathsSeconds }
    val sumSec = (totalPhy + totalChem + totalMath).toFloat()

    val physicsPercent = if (sumSec > 0) (totalPhy.toFloat() / sumSec) else 0f
    val chemistryPercent = if (sumSec > 0) (totalChem.toFloat() / sumSec) else 0f
    val mathsPercent = if (sumSec > 0) (totalMath.toFloat() / sumSec) else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📈 Study Insights", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                
                // Switcher
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    ranges.forEachIndexed { idx, title ->
                        Box(
                            modifier = Modifier
                                .clickable { selectedRange = idx }
                                .background(if (selectedRange == idx) Color(0xFF2D2928) else Color.Transparent)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedRange == idx) Color.White else Color(0xFF85736E)
                            )
                        }
                    }
                }
            }
        }

        // Streak & Activity Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF4DDCC)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Current Study Streak",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF554335).copy(alpha = 0.7f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$streak Days 🔥",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF554335)
                            )
                        }
                        Text(
                            text = "Keep studying daily to cement concepts in memory!",
                            fontSize = 11.sp,
                            color = Color(0xFF554335).copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = "Streak",
                            tint = Color(0xFF554335),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        // Quick Stats Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total hours card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF2EDE8))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF85736E))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Studied Time", fontSize = 11.sp, color = Color(0xFF85736E))
                        Text(
                            text = if (totalMinutes < 60) "$totalMinutes min" else "${totalMinutes / 60}h ${totalMinutes % 60}m",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF201A19)
                        )
                    }
                }

                // Mastery Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF2EDE8))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF705D00))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Mastered topics", fontSize = 11.sp, color = Color(0xFF85736E))
                        Text("$masteredCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF201A19))
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // PYQs solved
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF2EDE8))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF0056D2))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("PYQs solved", fontSize = 11.sp, color = Color(0xFF85736E))
                        Text("$solvedPyqs Qs", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF201A19))
                    }
                }

                // DPT Accuracy
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF2EDE8))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF6750A4))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("DPT Accuracy", fontSize = 11.sp, color = Color(0xFF85736E))
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f%%", dptAccuracy),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF201A19)
                        )
                    }
                }
            }
        }

        // Study Time Breakdown by subject
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF2EDE8))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📐 Subject Study Time Distribution", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF201A19))
                    Spacer(modifier = Modifier.height(12.dp))

                    if (sumSec == 0f) {
                        Text("No session records recorded yet. Start study sessions in Courses to see your logs!", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        // Horizontal stacked bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            if (physicsPercent > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(physicsPercent)
                                        .background(Color(0xFF6750A4))
                                )
                            }
                            if (chemistryPercent > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(chemistryPercent)
                                        .background(Color(0xFF0056D2))
                                )
                            }
                            if (mathsPercent > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(mathsPercent)
                                        .background(Color(0xFF705D00))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Legends with values
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LegendRow("Physics", Color(0xFF6750A4), String.format(Locale.getDefault(), "%.1f%%", physicsPercent * 100))
                            LegendRow("Chemistry", Color(0xFF0056D2), String.format(Locale.getDefault(), "%.1f%%", chemistryPercent * 100))
                            LegendRow("Mathematics", Color(0xFF705D00), String.format(Locale.getDefault(), "%.1f%%", mathsPercent * 100))
                        }
                    }
                }
            }
        }

        // DPT Accuracy Trend Graph (Canvas drawn line chart)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📈 Daily Practice Test Accuracy Trend", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (allDpts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Solve AI DPT mock tests to render your accuracy trends!", fontSize = 12.sp, color = Color.Gray)
                        }
                    } else {
                        // Draw a custom canvas-based line chart of the last 10 DPT scores
                        val points = remember(allDpts) {
                            allDpts.take(10).reversed().map { it.accuracy }
                        }

                        val primaryColor = MaterialTheme.colorScheme.primary

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            val w = size.width
                            val h = size.height
                            val spacing = w / (points.size - 1).coerceAtLeast(1)

                            // Draw baseline grid lines
                            val gridPaintColor = Color.LightGray.copy(alpha = 0.3f)
                            for (i in 0..4) {
                                val yGrid = h * (i / 4f)
                                drawLine(
                                    color = gridPaintColor,
                                    start = Offset(0f, yGrid),
                                    end = Offset(w, yGrid),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            // Build path
                            val path = Path()
                            points.forEachIndexed { idx, acc ->
                                // Accuracy is 0 to 100, maps to h..0 (0 accuracy is at bottom h, 100 accuracy is at top 0)
                                val x = idx * spacing
                                val y = h - (acc / 100f) * h

                                if (idx == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }

                                // Draw circular node points
                                drawCircle(
                                    color = primaryColor,
                                    center = Offset(x, y),
                                    radius = 4.dp.toPx()
                                )
                            }

                            // Draw path line
                            drawPath(
                                path = path,
                                color = primaryColor,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "<- Chronological progress logs (Last ${points.size} DPTs) ->",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun LegendRow(label: String, color: Color, percentage: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "$label: $percentage", fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// Helper: formats seconds to HH:MM:SS or MM:SS
fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }
}

// ==========================================
// D) TO-DO ZONE
// ==========================================

@Composable
fun TodoZoneScreen(
    viewModel: JeeViewModel,
    allTodos: List<TodoItem>,
    onLinkSelect: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var taskText by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("Medium") }
    var selectedLinkedSubConceptId by remember { mutableStateOf("") }

    val priorities = listOf("Low", "Medium", "High")

    // Filtered to-dos
    val activeTodos = remember(allTodos) { allTodos.filter { !it.completed } }
    val completedTodos = remember(allTodos) { allTodos.filter { it.completed } }

    val allSubConcepts = remember {
        JeeHierarchy.chapters
            .flatMap { JeeHierarchy.getConceptsForChapter(it.id) }
            .flatMap { JeeHierarchy.getSubConceptsForConcept(it.id) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFFD4E157),
                contentColor = Color(0xFF2D2928),
                modifier = Modifier.testTag("btn_add_todo_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("📋 JEE To-Do Zone", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "Link study tasks directly to NCERT concepts for systematic revisions.",
                    fontSize = 11.sp,
                    color = Color(0xFF85736E)
                )
            }

            // Recommended AI Tasks for Weak Areas!
            item {
                val progressList by viewModel.allProgress.collectAsStateWithLifecycle()
                val weakConcepts = remember(progressList) { viewModel.getWeakSubConcepts(progressList) }

                if (weakConcepts.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFDBCB)),
                        border = BorderStroke(1.dp, Color(0xFF341100).copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF341100), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI Weak Area Recommendations", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF341100))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "We identified these topics as your weak areas. Add tasks to revise them!",
                                fontSize = 11.sp,
                                color = Color(0xFF341100).copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            weakConcepts.take(2).forEach { sub ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("• ${sub.name}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF341100), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    TextButton(
                                        onClick = {
                                            viewModel.addTodoItem(
                                                task = "Revise ${sub.name} + do 15 PYQs",
                                                linkedSubConceptId = sub.id,
                                                priority = "High",
                                                dueDateMs = System.currentTimeMillis()
                                              )
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF341100))
                                    ) {
                                        Text("+ Add task", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ACTIVE TASKS SECTION
            item {
                Text(
                    text = "Active Tasks (${activeTodos.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2928)
                )
            }

            if (activeTodos.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active tasks! Click + to add a customized JEE task.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                items(activeTodos) { todo ->
                    TodoRowItem(
                        todo = todo,
                        onToggle = { viewModel.toggleTodoCompleted(todo) },
                        onDelete = { viewModel.deleteTodo(todo.id) },
                        onLinkClicked = { onLinkSelect(todo.linkedSubConceptId) }
                    )
                }
            }

            // COMPLETED TASKS SECTION
            if (completedTodos.isNotEmpty()) {
                item {
                    Text(
                        text = "Completed Tasks (${completedTodos.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }

                items(completedTodos) { todo ->
                    TodoRowItem(
                        todo = todo,
                        onToggle = { viewModel.toggleTodoCompleted(todo) },
                        onDelete = { viewModel.deleteTodo(todo.id) },
                        onLinkClicked = { onLinkSelect(todo.linkedSubConceptId) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Add To-Do Dialog
    if (showAddDialog) {
        var dropdownExpanded by remember { mutableStateOf(false) }
        var dropdownText by remember { mutableStateOf("Tap to link concept (optional)") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("📝 Add JEE Study Task", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                    OutlinedTextField(
                        value = taskText,
                        onValueChange = { taskText = it },
                        label = { Text("Task Description") },
                        placeholder = { Text("e.g., Revise Equipotential Surfaces + do 20 PYQs") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("todo_input_desc"),
                        singleLine = true
                    )

                    // Linked concept selection dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(dropdownText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.height(250.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    selectedLinkedSubConceptId = ""
                                    dropdownText = "No Link"
                                    dropdownExpanded = false
                                }
                            )
                            allSubConcepts.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text(sub.name) },
                                    onClick = {
                                        selectedLinkedSubConceptId = sub.id
                                        dropdownText = sub.name
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Priority Row
                    Column {
                        Text("Task Priority:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            priorities.forEach { p ->
                                val isSel = selectedPriority == p
                                ElevatedFilterChip(
                                    selected = isSel,
                                    onClick = { selectedPriority = p },
                                    label = { Text(p) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (taskText.isNotEmpty()) {
                                    viewModel.addTodoItem(
                                        task = taskText,
                                        linkedSubConceptId = selectedLinkedSubConceptId,
                                        priority = selectedPriority,
                                        dueDateMs = System.currentTimeMillis()
                                    )
                                    taskText = ""
                                    selectedLinkedSubConceptId = ""
                                    showAddDialog = false
                                }
                            },
                            modifier = Modifier.testTag("todo_btn_submit")
                        ) {
                            Text("Create Task")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TodoRowItem(
    todo: TodoItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onLinkClicked: () -> Unit
) {
    val priorityColor = when (todo.priority) {
        "High" -> Color.Red
        "Medium" -> Color(0xFFFFA726)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (todo.completed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todo.completed,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("chk_todo_${todo.id}")
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.task,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (todo.completed) Color.Gray else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = priorityColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            todo.priority,
                            color = priorityColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    if (todo.linkedSubConceptId.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.clickable { onLinkClicked() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, size = 10.dp, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = todo.linkedSubConceptName,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Helper size extension
@Composable
fun Icon(imageVector: ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(size)
    )
}

// ==========================================
// E) ACTIVE DPT IMMERSIVE EXAM OVERLAY
// ==========================================

@Composable
fun ActiveDptScreen(
    state: DptUiState,
    viewModel: JeeViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (state) {
            is DptUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "AI is selecting tailored questions based on NCERT syllabus...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
            is DptUiState.Running -> {
                val questions = state.questions
                val currentIndex = state.currentQuestionIndex
                val currentQ = questions.getOrNull(currentIndex)
                val isSubmitted = state.examSubmitted
                val timerSeconds by viewModel.activeDptTimerSeconds.collectAsStateWithLifecycle()

                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Bar Info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.exitDpt() }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit")
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Daily Practice Test (DPT)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(state.subConceptName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Display countdown or mock score
                        if (isSubmitted) {
                            Surface(
                                color = Color(0xFF66BB6A).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "${state.scoreResult} / ${questions.size} Correct",
                                    color = Color(0xFF66BB6A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d:%02d", timerSeconds / 60, timerSeconds % 60),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Main questions view
                    if (currentQ != null) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Progress bar
                            item {
                                LinearProgressIndicator(
                                    progress = (currentIndex + 1).toFloat() / questions.size,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                                Text(
                                    text = "Question ${currentIndex + 1} of ${questions.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // Question Title Card
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        text = currentQ.question,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(16.dp),
                                        lineHeight = 22.sp
                                    )
                                }
                            }

                            // Options Checklist/Cards
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    currentQ.options.forEachIndexed { optIndex, option ->
                                        val isSelected = state.selectedAnswers[currentIndex] == optIndex
                                        val isCorrectIndex = currentQ.correct_option_index == optIndex
                                        
                                        val cardColor = when {
                                            isSubmitted && isCorrectIndex -> Color(0xFFE8F5E9) // correct green
                                            isSubmitted && isSelected && !isCorrectIndex -> Color(0xFFFFEBEE) // wrong red
                                            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                            else -> MaterialTheme.colorScheme.surface
                                        }

                                        val borderColor = when {
                                            isSubmitted && isCorrectIndex -> Color(0xFF4CAF50)
                                            isSubmitted && isSelected && !isCorrectIndex -> Color.Red
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.outlineVariant
                                        }

                                        val textAccentColor = when {
                                            isSubmitted && isCorrectIndex -> Color(0xFF2E7D32)
                                            isSubmitted && isSelected && !isCorrectIndex -> Color.Red
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = !isSubmitted) {
                                                    viewModel.submitDptAnswer(currentIndex, optIndex)
                                                }
                                                .testTag("dpt_option_${currentIndex}_$optIndex"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = cardColor),
                                            border = BorderStroke(1.5.dp, borderColor)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Option letter icon
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .background(borderColor.copy(alpha = 0.15f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = ('A' + optIndex).toString(),
                                                        color = textAccentColor,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(12.dp))

                                                Text(
                                                    text = option,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Dynamic answer explanation if submitted
                            if (isSubmitted) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Step-by-Step AI Explanation", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = currentQ.explanation,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom bar navigation controls
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.navigateDptQuestion(currentIndex - 1) },
                                    enabled = currentIndex > 0,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                                ) {
                                    Text("Previous")
                                }

                                Button(
                                    onClick = { viewModel.navigateDptQuestion(currentIndex + 1) },
                                    enabled = currentIndex < questions.size - 1,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                                ) {
                                    Text("Next")
                                }
                            }

                            if (isSubmitted) {
                                Button(
                                    onClick = { viewModel.exitDpt() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                    modifier = Modifier.testTag("btn_exit_dpt_result")
                                ) {
                                    Text("Finish & Return", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.submitDptExam() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.testTag("btn_submit_dpt")
                                ) {
                                    Text("Submit Test", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            is DptUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Error generation failed: ${state.message}", color = Color.Red, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.exitDpt() }) {
                        Text("Exit")
                    }
                }
            }
            else -> {}
        }
    }
}
