package com.example.colorsensor.utils

import com.google.firebase.firestore.FirebaseFirestore

object PaintFinder{
    private val db = FirebaseFirestore.getInstance()

    data class PaintColor(val name: String, val r: Int, val g: Int, val b: Int)

    // Finding closest paint match
    fun findClosestPaint(
        targetColor: PaintColor,
        // Adjustable range, modify as fit
        range: Int = 30,
        onResult: (PaintColor?) -> Unit,
        onError: (Exception) -> Unit
    ){
        // Querying the database, filtering out paints outside of the range
        db.collection("paints")
            .whereGreaterThanOrEqualTo("r", targetColor.r - range)
            .whereLessThanOrEqualTo("r", targetColor.r + range)
            .whereGreaterThanOrEqualTo("g", targetColor.g - range)
            .whereLessThanOrEqualTo("g", targetColor.g + range)
            .whereGreaterThanOrEqualTo("b", targetColor.b - range)
            .whereLessThanOrEqualTo("b", targetColor.b + range)
            .get()
            .addOnSuccessListener { documents ->
                val colors = mutableListOf<PaintColor>()

                for(document in documents){
                    val name = document.getString("name") ?:"Unknown"
                    val r = document.getLong("r")?.toInt() ?: 0
                    val g = document.getLong("g")?.toInt() ?: 0
                    val b = document.getLong("b")?.toInt() ?: 0
                    colors.add(PaintColor(name, r, g, b))
                }

                // Finding closest color using Euclidean distance
                val closestColor = colors.minByOrNull{ color ->
                    val dr = color.r - targetColor.r
                    val dg = color.g - targetColor.g
                    val db = color.b - targetColor.b
                    // Square Euclidean distance
                    dr * dr + dg * dg + db * db
                }

                onResult(closestColor)
            }
            // Throwing exception if caught
            .addOnFailureListener{exception ->
                onError(exception)
            }
    }
}