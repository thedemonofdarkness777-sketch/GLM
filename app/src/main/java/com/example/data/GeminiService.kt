package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Checks if the API key is configured.
     */
    fun isKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * Query Gemini directly via REST.
     */
    private suspend fun queryGemini(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!isKeyConfigured()) {
            return@withContext "ERROR: API Key is not configured. Please add your GEMINI_API_KEY in the Secrets panel."
        }

        val url = "$BASE_URL?key=$apiKey"

        val jsonRequest = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            }
            put("contents", contentsArray)

            if (systemInstruction != null) {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                })
            }

            // Request creative but focused generation
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.4)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonRequest.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Unsuccessful response: $bodyString")
                    return@withContext "ERROR: Server returned code ${response.code}\n$bodyString"
                }

                if (bodyString == null) {
                    return@withContext "ERROR: Empty response body."
                }

                val jsonResponse = JSONObject(bodyString)
                val candidates = jsonResponse.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text")

                return@withContext text ?: "ERROR: No response text found."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during API call", e)
            return@withContext "ERROR: ${e.message}"
        }
    }

    /**
     * Generates a 5-question Daily Practice Test (DPT) for a concept.
     */
    suspend fun generateDailyPracticeTest(
        subject: String,
        chapter: String,
        subConcept: String
    ): List<DptQuestion> = withContext(Dispatchers.IO) {
        val systemInstruction = """
            You are an expert JEE (Joint Entrance Exam) and NCERT educator. 
            Generate highly authentic, high-quality, conceptual, and numerical Multiple Choice Questions (MCQ) for the JEE Mains level.
            Always reply with a single JSON array, and absolutely nothing else. No markdown wrappers, no backticks, no text before or after.
        """.trimIndent()

        val prompt = """
            Generate exactly 5 JEE Mains/NCERT standard multiple-choice questions for:
            Subject: $subject
            Chapter: $chapter
            Concept/Subconcept: $subConcept

            Format the response strictly as a JSON array of 5 objects, each having:
            - "question": (String) The question text, clear and rigorous.
            - "options": (Array of 4 Strings) Choices A, B, C, D. Do not prefix with A, B, C, D.
            - "correct_option_index": (Int) Index (0 to 3) representing the correct option.
            - "explanation": (String) Step-by-step conceptual or mathematical derivation explaining why this option is correct.

            Make sure questions cover core fundamentals, formula applications, and classic trick cases.
        """.trimIndent()

        try {
            var rawResponse = queryGemini(prompt, systemInstruction).trim()
            
            // Clean markdown blocks if present
            if (rawResponse.startsWith("```")) {
                val lines = rawResponse.lines()
                val cleanedLines = lines.filterNot { it.trim().startsWith("```") }
                rawResponse = cleanedLines.joinToString("\n").trim()
            }

            if (rawResponse.startsWith("ERROR:")) {
                return@withContext getMockQuestions(subConcept)
            }

            val array = JSONArray(rawResponse)
            val list = mutableListOf<DptQuestion>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val qText = obj.getString("question")
                val optsArray = obj.getJSONArray("options")
                val options = List(4) { idx -> optsArray.getString(idx) }
                val correctIndex = obj.getInt("correct_option_index")
                val explanation = obj.getString("explanation")
                list.add(DptQuestion(qText, options, correctIndex, explanation))
            }
            return@withContext list
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing JSON, falling back to offline questions", e)
            return@withContext getMockQuestions(subConcept)
        }
    }

    /**
     * Generates a comprehensive Study Guide (Learn + Build phases) for a concept.
     */
    suspend fun generateStudyGuide(
        subject: String,
        chapter: String,
        subConcept: String
    ): StudyGuide = withContext(Dispatchers.IO) {
        val prompt = """
            Provide a comprehensive, high-yield NCERT study guide for JEE aspirants on:
            Subject: $subject
            Chapter: $chapter
            Sub-Concept: $subConcept

            Always reply with a single JSON object. Do not wrap in markdown or backticks, do not include text before or after.
            The JSON object must have these exact keys:
            - "hook": (String) A fascinating real-world hook explaining WHY this concept exists and where it is applied in actual technology or nature.
            - "fundamentals": (String) Detailed conceptual theory, core equations, and step-by-step mathematical derivations.
            - "examples": (String) 2 solved beginner examples with easy-to-follow, clear calculations.
            - "twists": (String) 2 advanced twist problems, special exceptional conditions, note points, and complex scenarios.
            - "mistakes": (String) Classic trap points, common formula mistakes, or sign-convention traps where students fail.

            Include mathematical equations in standard text format (e.g., E = -dV/dr or F = q*E). Make it extremely rich, rigorous, and inspiring.
        """.trimIndent()

        val systemInstruction = "You are a senior IIT JEE Physics/Chemistry/Maths Professor. Provide premium educational content. Respond strictly with JSON."

        try {
            var rawResponse = queryGemini(prompt, systemInstruction).trim()
            
            if (rawResponse.startsWith("```")) {
                val lines = rawResponse.lines()
                val cleanedLines = lines.filterNot { it.trim().startsWith("```") }
                rawResponse = cleanedLines.joinToString("\n").trim()
            }

            if (rawResponse.startsWith("ERROR:")) {
                return@withContext getMockStudyGuide(subConcept)
            }

            val obj = JSONObject(rawResponse)
            return@withContext StudyGuide(
                hook = obj.getString("hook"),
                fundamentals = obj.getString("fundamentals"),
                examples = obj.getString("examples"),
                twists = obj.getString("twists"),
                mistakes = obj.getString("mistakes")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing Study Guide JSON", e)
            return@withContext getMockStudyGuide(subConcept)
        }
    }

    // --- FALLBACK MOCK DATA FOR OFFLINE WORK / API KEY MISSING ---

    private fun getMockQuestions(subConcept: String): List<DptQuestion> {
        return listOf(
            DptQuestion(
                question = "For the concept of '$subConcept', what is the fundamental governing principle under standard conditions?",
                options = listOf(
                    "It varies directly with the field strength squared.",
                    "It follows the inverse square law of distance.",
                    "It remains constant regardless of boundary conditions.",
                    "It is determined solely by the dielectric constant of vacuum."
                ),
                correct_option_index = 1,
                explanation = "In standard NCERT formulations of this concept, forces and potentials scale inversely with distance or distance-squared, modeling a central-field interaction."
            ),
            DptQuestion(
                question = "Which of the following common mathematical traps most frequently causes errors in solving problems for '$subConcept'?",
                options = listOf(
                    "Forgetting the unit conversion from cm to meters.",
                    "Ignoring sign conventions in work-energy balance.",
                    "Confusing dot products with cross products.",
                    "All of the above."
                ),
                correct_option_index = 3,
                explanation = "JEE Mains frequently tests elementary traps such as sign conventions, metric units, and scalar/vector products. Proper dimensional analysis is vital."
            ),
            DptQuestion(
                question = "Consider a system experiencing a perturbation in '$subConcept'. If the medium's relative permittivity is doubled, how does the associated potential energy change?",
                options = listOf(
                    "Doubled",
                    "Halved",
                    "Quadrupled",
                    "Unchanged"
                ),
                correct_option_index = 1,
                explanation = "Potential energy in a electrostatic-like medium is inversely proportional to the dielectric constant (permittivity). Doubling permittivity halves the potential energy."
            ),
            DptQuestion(
                question = "A classic limit problem in this concept involves evaluating the boundary values. When distance 'r' approaches infinity, what happens to the force field?",
                options = listOf(
                    "Approaches infinity exponentially",
                    "Approaches 1 unit",
                    "Approaches 0 asymptotically",
                    "Becomes highly oscillatory"
                ),
                correct_option_index = 2,
                explanation = "Due to the 1/r^2 dependence, as r -> infinity, the electrostatic/gravitational force decays to 0, which defines our standard reference state."
            ),
            DptQuestion(
                question = "What is the primary difference between a board exam proof and a JEE Mains approach for '$subConcept'?",
                options = listOf(
                    "JEE Mains does not test formulas, only graph shapes.",
                    "Board exams require full step derivation; JEE tests fast application and limiting-case elimination.",
                    "JEE Mains requires double integration for all problems.",
                    "There is no difference; the syllabus and testing criteria are identical."
                ),
                correct_option_index = 1,
                explanation = "Board exams award step-wise marks for complete written derivations, whereas JEE Mains prioritizes rapid mental models, edge-case checks, and quick calculation tricks."
            )
        )
    }

    private fun getMockStudyGuide(subConcept: String): StudyGuide {
        return StudyGuide(
            hook = "⚡ Real World Hook: Have you ever wondered why your smartphone touchscreen behaves so instantly and smoothly? It is governed exactly by the electric field and charge storage principles in '$subConcept'. These micro-capacitors sense the change in capacitance when your finger (a conductor) touches the glass!",
            fundamentals = "📖 Fundamentals:\n" +
                    "1. The Core Equation is given by V = W / q, representing work done per unit charge in a conservative field.\n" +
                    "2. Line integral formulation: V = -∫ E · dr, from infinity to the point of interest.\n" +
                    "3. Superposition principle applies: for multiple discrete source charges, the net potential is the scalar sum V_net = ∑ V_i.\n" +
                    "4. For continuous charge distributions, we integrate: V = ∫ dq / (4 * π * ε_0 * r). This forms the baseline for electric potential problems.",
            examples = "💡 Beginner Examples:\n" +
                    "Example 1: A point charge q = 2 μC is kept at the origin. Find potential at a distance of r = 9 cm.\n" +
                    "Solution: V = k * q / r = (9 * 10^9) * (2 * 10^-6) / (0.09) = 200,000 Volts (or 2 * 10^5 V).\n\n" +
                    "Example 2: Find work done in moving a charge of 5 nC between two points with potential difference of 10V.\n" +
                    "Solution: W = q * dV = (5 * 10^-9 C) * (10 V) = 50 nJ (nanojoules).",
            twists = "⚠️ Twist Problems & Advanced Applications:\n" +
                    "- Twist 1: Conducting vs. Non-conducting spheres. Outside they behave identically, but INSIDE a non-conducting charged sphere, the potential varies parabolically: V(r) = (k*Q/2R) * (3 - r^2/R^2). This is a frequent source of tricky questions.\n" +
                    "- Twist 2: Charges placed on connected spheres of different radii. They will share charges until their potential V is equal, not their charges! The sphere with smaller radius will have higher surface charge density (sigma ∝ 1/R).",
            mistakes = "🛑 Mistake & Trap Points:\n" +
                    "1. The Sign Error: Forgetting that a negative charge creates a negative potential! Always substitute signs literally in scalar potential calculations.\n" +
                    "2. Work Done by Field vs. External Agent: W_ext = q * dV, but W_field = -q * dV. Always read carefully whether the question asks for work done by the field or the external agent!"
        )
    }
}

data class DptQuestion(
    val question: String,
    val options: List<String>,
    val correct_option_index: Int,
    val explanation: String
)

data class StudyGuide(
    val hook: String,
    val fundamentals: String,
    val examples: String,
    val twists: String,
    val mistakes: String
)
