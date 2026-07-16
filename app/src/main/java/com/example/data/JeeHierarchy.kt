package com.example.data

data class Subject(
    val id: String,
    val name: String,
    val icon: String // "physics", "chemistry", "maths"
)

data class Chapter(
    val id: String,
    val subjectId: String,
    val name: String,
    val order: Int
)

data class Concept(
    val id: String,
    val chapterId: String,
    val name: String
)

data class SubConcept(
    val id: String,
    val conceptId: String,
    val name: String
)

object JeeHierarchy {
    val subjects = listOf(
        Subject("physics", "Physics", "⚡"),
        Subject("chemistry", "Chemistry", "🧪"),
        Subject("maths", "Mathematics", "📐")
    )

    val chapters = listOf(
        // --- PHYSICS ---
        Chapter("phy_ch1", "physics", "Electric Charges & Fields", 1),
        Chapter("phy_ch2", "physics", "Electrostatic Potential & Capacitance", 2),
        Chapter("phy_ch3", "physics", "Current Electricity", 3),
        Chapter("phy_ch4", "physics", "Moving Charges & Magnetism", 4),
        Chapter("phy_ch5", "physics", "Magnetism & Matter", 5),
        Chapter("phy_ch6", "physics", "Electromagnetic Induction", 6),
        Chapter("phy_ch7", "physics", "Alternating Current", 7),
        Chapter("phy_ch8", "physics", "Electromagnetic Waves", 8),
        Chapter("phy_ch9", "physics", "Ray Optics & Optical Instruments", 9),
        Chapter("phy_ch10", "physics", "Wave Optics", 10),
        Chapter("phy_ch11", "physics", "Dual Nature of Radiation & Matter", 11),
        Chapter("phy_ch12", "physics", "Atoms", 12),
        Chapter("phy_ch13", "physics", "Nuclei", 13),
        Chapter("phy_ch14", "physics", "Semiconductor Electronics", 14),

        // --- CHEMISTRY ---
        Chapter("chem_ch1", "chemistry", "Solutions", 1),
        Chapter("chem_ch2", "chemistry", "Electrochemistry", 2),
        Chapter("chem_ch3", "chemistry", "Chemical Kinetics", 3),
        Chapter("chem_ch4", "chemistry", "d- and f-Block Elements", 4),
        Chapter("chem_ch5", "chemistry", "Coordination Compounds", 5),
        Chapter("chem_ch6", "chemistry", "Haloalkanes & Haloarenes", 6),
        Chapter("chem_ch7", "chemistry", "Alcohols, Phenols & Ethers", 7),
        Chapter("chem_ch8", "chemistry", "Aldehydes, Ketones & Carboxylic Acids", 8),
        Chapter("chem_ch9", "chemistry", "Amines", 9),
        Chapter("chem_ch10", "chemistry", "Biomolecules", 10),

        // --- MATHS ---
        Chapter("math_ch1", "maths", "Relations & Functions", 1),
        Chapter("math_ch2", "maths", "Inverse Trigonometric Functions", 2),
        Chapter("math_ch3", "maths", "Matrices", 3),
        Chapter("math_ch4", "maths", "Determinants", 4),
        Chapter("math_ch5", "maths", "Continuity & Differentiability", 5),
        Chapter("math_ch6", "maths", "Application of Derivatives", 6),
        Chapter("math_ch7", "maths", "Integrals", 7),
        Chapter("math_ch8", "maths", "Application of Integrals", 8),
        Chapter("math_ch9", "maths", "Differential Equations", 9),
        Chapter("math_ch10", "maths", "Vector Algebra", 10),
        Chapter("math_ch11", "maths", "Three Dimensional Geometry", 11),
        Chapter("math_ch12", "maths", "Linear Programming", 12),
        Chapter("math_ch13", "maths", "Probability", 13)
    )

    // Detailed concepts & subconcepts for some key chapters to give high-fidelity deep dives
    private val detailedConcepts = mapOf(
        // Electrostatic Potential & Capacitance
        "phy_ch2" to listOf(
            Concept("phy_ch2_c1", "phy_ch2", "Electric Potential"),
            Concept("phy_ch2_c2", "phy_ch2", "Capacitors & Capacitance")
        ),
        // Current Electricity
        "phy_ch3" to listOf(
            Concept("phy_ch3_c1", "phy_ch3", "Ohm's Law & Resistance"),
            Concept("phy_ch3_c2", "phy_ch3", "Kirchhoff's Laws")
        ),
        // Solutions
        "chem_ch1" to listOf(
            Concept("chem_ch1_c1", "chem_ch1", "Concentration Terms"),
            Concept("chem_ch1_c2", "chem_ch1", "Colligative Properties")
        ),
        // Matrices
        "math_ch3" to listOf(
            Concept("math_ch3_c1", "math_ch3", "Matrix Operations"),
            Concept("math_ch3_c2", "math_ch3", "Determinants & Inverse")
        )
    )

    private val detailedSubConcepts = mapOf(
        "phy_ch2_c1" to listOf(
            SubConcept("sub_phy_ch2_c1_s1", "phy_ch2_c1", "Work Done to Move a Charge"),
            SubConcept("sub_phy_ch2_c1_s2", "phy_ch2_c1", "Potential and Potential Difference"),
            SubConcept("sub_phy_ch2_c1_s3", "phy_ch2_c1", "Equipotential Surfaces"),
            SubConcept("sub_phy_ch2_c1_s4", "phy_ch2_c1", "Potential Energy of Charge System")
        ),
        "phy_ch2_c2" to listOf(
            SubConcept("sub_phy_ch2_c2_s1", "phy_ch2_c2", "Capacitors and Capacitance"),
            SubConcept("sub_phy_ch2_c2_s2", "phy_ch2_c2", "Parallel Plate Capacitor with Dielectric"),
            SubConcept("sub_phy_ch2_c2_s3", "phy_ch2_c2", "Combination of Capacitors"),
            SubConcept("sub_phy_ch2_c2_s4", "phy_ch2_c2", "Energy Stored in Capacitor")
        ),
        "phy_ch3_c1" to listOf(
            SubConcept("sub_phy_ch3_c1_s1", "phy_ch3_c1", "Electric Current and Drift Velocity"),
            SubConcept("sub_phy_ch3_c1_s2", "phy_ch3_c1", "Ohm's Law and Resistivity"),
            SubConcept("sub_phy_ch3_c1_s3", "phy_ch3_c1", "Temperature Dependence of Resistance")
        ),
        "phy_ch3_c2" to listOf(
            SubConcept("sub_phy_ch3_c2_s1", "phy_ch3_c2", "Kirchhoff's Junction and Loop Rules"),
            SubConcept("sub_phy_ch3_c2_s2", "phy_ch3_c2", "Wheatstone Bridge and Meter Bridge"),
            SubConcept("sub_phy_ch3_c2_s3", "phy_ch3_c2", "Potentiometer Principles")
        ),
        "chem_ch1_c1" to listOf(
            SubConcept("sub_chem_ch1_c1_s1", "chem_ch1_c1", "Molarity, Molality, and Mole Fraction"),
            SubConcept("sub_chem_ch1_c1_s2", "chem_ch1_c1", "Henry's Law of Gas Solubility"),
            SubConcept("sub_chem_ch1_c1_s3", "chem_ch1_c1", "Raoult's Law of Vapor Pressure")
        ),
        "chem_ch1_c2" to listOf(
            SubConcept("sub_chem_ch1_c2_s1", "chem_ch1_c2", "Relative Lowering of Vapor Pressure"),
            SubConcept("sub_chem_ch1_c2_s2", "chem_ch1_c2", "Elevation of Boiling Point"),
            SubConcept("sub_chem_ch1_c2_s3", "chem_ch1_c2", "Depression of Freezing Point"),
            SubConcept("sub_chem_ch1_c2_s4", "chem_ch1_c2", "Osmotic Pressure and van 't Hoff Factor")
        ),
        "math_ch3_c1" to listOf(
            SubConcept("sub_math_ch3_c1_s1", "math_ch3_c1", "Types of Matrices & Equality"),
            SubConcept("sub_math_ch3_c1_s2", "math_ch3_c1", "Matrix Addition & Scalar Multiplication"),
            SubConcept("sub_math_ch3_c1_s3", "math_ch3_c1", "Matrix Multiplication and its Properties")
        ),
        "math_ch3_c2" to listOf(
            SubConcept("sub_math_ch3_c2_s1", "math_ch3_c2", "Transpose, Symmetric & Skew Symmetric"),
            SubConcept("sub_math_ch3_c2_s2", "math_ch3_c2", "Adjoint and Inverse of Matrix"),
            SubConcept("sub_math_ch3_c2_s3", "math_ch3_c2", "System of Linear Equations using Matrices")
        )
    )

    fun getConceptsForChapter(chapterId: String): List<Concept> {
        return detailedConcepts[chapterId] ?: listOf(
            Concept("${chapterId}_c1", chapterId, "Fundamental Theory & Principles"),
            Concept("${chapterId}_c2", chapterId, "Solved Problems & Applications")
        )
    }

    fun getSubConceptsForConcept(conceptId: String): List<SubConcept> {
        return detailedSubConcepts[conceptId] ?: run {
            // Generate standard generic NCERT sub-concepts
            val parts = conceptId.split("_")
            val chapterId = parts.subList(0, 2).joinToString("_")
            val chIndex = chapters.indexOfFirst { it.id == chapterId }
            val chName = if (chIndex >= 0) chapters[chIndex].name else "Syllabus"
            
            if (conceptId.endsWith("_c1")) {
                listOf(
                    SubConcept("${conceptId}_s1", conceptId, "Introduction & Hook of $chName"),
                    SubConcept("${conceptId}_s2", conceptId, "Core Derivations & Formulas"),
                    SubConcept("${conceptId}_s3", conceptId, "Standard Model Problems")
                )
            } else {
                listOf(
                    SubConcept("${conceptId}_s1", conceptId, "Advanced Twist Problems"),
                    SubConcept("${conceptId}_s2", conceptId, "NCERT Textbook Exercises"),
                    SubConcept("${conceptId}_s3", conceptId, "Board & JEE PYQ Patterns")
                )
            }
        }
    }

    fun getSubjectForChapter(chapterId: String): Subject {
        val chapter = chapters.find { it.id == chapterId }
        return subjects.find { it.id == chapter?.subjectId } ?: subjects[0]
    }

    fun getChapterForConcept(conceptId: String): Chapter {
        val concept = chapters.flatMap { getConceptsForChapter(it.id) }.find { it.id == conceptId }
        return chapters.find { it.id == concept?.chapterId } ?: chapters[0]
    }

    fun getConceptForSubConcept(subConceptId: String): Concept {
        val subConcept = chapters
            .flatMap { getConceptsForChapter(it.id) }
            .flatMap { getSubConceptsForConcept(it.id) }
            .find { it.id == subConceptId }
        
        return chapters
            .flatMap { getConceptsForChapter(it.id) }
            .find { it.id == subConcept?.conceptId }
            ?: Concept("unknown", "unknown", "Unknown Concept")
    }
}
