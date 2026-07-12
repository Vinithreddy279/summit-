package com.example.data

import java.io.Serializable
import kotlin.math.*

data class GPSPoint(
    val lat: Double,
    val lng: Double,
    val elevation: Double = 0.0,
    val timeMs: Long = 0,
    val speedMps: Double = 0.0,
    val hasElevation: Boolean = true
) : Serializable {
    val latlng: Pair<Double, Double> get() = Pair(lat, lng)
}

object JsonHelper {
    fun pointsToJson(points: List<GPSPoint>): String {
        return buildString {
            append("[")
            points.forEachIndexed { idx, p ->
                append("""{"lat":${p.lat},"lng":${p.lng},"ele":${p.elevation},"t":${p.timeMs},"s":${p.speedMps},"hasEle":${p.hasElevation}}""")
                if (idx < points.size - 1) append(",")
            }
            append("]")
        }
    }

    fun jsonToPoints(json: String): List<GPSPoint> {
        if (json.isEmpty() || json == "[]") return emptyList()
        val list = mutableListOf<GPSPoint>()
        try {
            // Match fields lat, lng, ele, t, s, and optional hasEle
            val pattern = """\{"lat":\s*([0-9\.\-]+)\s*,\s*"lng":\s*([0-9\.\-]+)\s*,\s*"ele":\s*([0-9\.\-]+)\s*,\s*"t":\s*([0-9\-]+)\s*,\s*"s":\s*([0-9\.\-]+)\s*(?:,\s*"hasEle":\s*(true|false))?\s*\}""".toRegex()
            val matches = pattern.findAll(json)
            for (match in matches) {
                val lat = match.groupValues[1].toDoubleOrNull() ?: 0.0
                val lng = match.groupValues[2].toDoubleOrNull() ?: 0.0
                val ele = match.groupValues[3].toDoubleOrNull() ?: 0.0
                val t = match.groupValues[4].toLongOrNull() ?: 0L
                val s = match.groupValues[5].toDoubleOrNull() ?: 0.0
                val hasEle = if (match.groupValues.size >= 7 && match.groupValues[6].isNotEmpty()) {
                    match.groupValues[6] == "true"
                } else {
                    true
                }
                list.add(GPSPoint(lat = lat, lng = lng, elevation = ele, timeMs = t, speedMps = s, hasElevation = hasEle))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun polylineToJson(points: List<Pair<Double, Double>>): String {
        return buildString {
            append("[")
            points.forEachIndexed { idx, p ->
                append("""[${p.first},${p.second}]""")
                if (idx < points.size - 1) append(",")
            }
            append("]")
        }
    }

    fun jsonToPolyline(json: String): List<Pair<Double, Double>> {
        if (json.isEmpty() || json == "[]") return emptyList()
        val list = mutableListOf<Pair<Double, Double>>()
        try {
            val pattern = """\[\s*([0-9\.\-]+)\s*,\s*([0-9\.\-]+)\s*\]""".toRegex()
            val matches = pattern.findAll(json)
            for (match in matches) {
                val lat = match.groupValues[1].toDoubleOrNull() ?: 0.0
                val lng = match.groupValues[2].toDoubleOrNull() ?: 0.0
                list.add(Pair(lat, lng))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}

object SegmentMatcher {
    private const val EARTH_RADIUS_M = 6371000.0

    // Tuning constants matching the prototype
    private const val ENDPOINT_TOLERANCE_M = 25.0
    private const val CORRIDOR_TOLERANCE_M = 20.0
    private const val MIN_CORRIDOR_COVERAGE = 0.85
    private const val MAX_FRECHET_RATIO = 0.15
    private const val MIN_CONFIDENCE = 0.6

    val MAX_PLAUSIBLE_SPEED_MPS = mapOf(
        "run" to 8.0,    // ~3:20/km
        "ride" to 25.0,  // ~90 km/h
        "hike" to 4.0,
        "walk" to 3.0,
        "swim" to 3.0
    )

    fun haversineM(p1: Pair<Double, Double>, p2: Pair<Double, Double>): Double {
        val lat1 = Math.toRadians(p1.first)
        val lng1 = Math.toRadians(p1.second)
        val lat2 = Math.toRadians(p2.first)
        val lng2 = Math.toRadians(p2.second)
        val dlat = lat2 - lat1
        val dlng = lng2 - lng1
        val a = sin(dlat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dlng / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(a))
    }

    fun pointToSegmentDistanceM(
        p: Pair<Double, Double>,
        a: Pair<Double, Double>,
        b: Pair<Double, Double>
    ): Double {
        val lat0 = Math.toRadians((a.first + b.first) / 2.0)
        
        fun toXy(pt: Pair<Double, Double>): Pair<Double, Double> {
            val x = Math.toRadians(pt.second) * cos(lat0) * EARTH_RADIUS_M
            val y = Math.toRadians(pt.first) * EARTH_RADIUS_M
            return Pair(x, y)
        }

        val (px, py) = toXy(p)
        val (ax, ay) = toXy(a)
        val (bx, by) = toXy(b)

        val dx = bx - ax
        val dy = by - ay
        if (dx == 0.0 && dy == 0.0) {
            return hypot(px - ax, py - ay)
        }

        var t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)
        t = maxOf(0.0, minOf(1.0, t))
        val projX = ax + t * dx
        val projY = ay + t * dy
        return hypot(px - projX, py - projY)
    }

    fun pointToPolylineDistanceM(p: Pair<Double, Double>, polyline: List<Pair<Double, Double>>): Double {
        if (polyline.size < 2) return Double.MAX_VALUE
        var minDist = Double.MAX_VALUE
        for (i in 0 until polyline.size - 1) {
            val d = pointToSegmentDistanceM(p, polyline[i], polyline[i + 1])
            if (d < minDist) minDist = d
        }
        return minDist
    }

    fun polylineLengthM(polyline: List<Pair<Double, Double>>): Double {
        if (polyline.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until polyline.size - 1) {
            total += haversineM(polyline[i], polyline[i + 1])
        }
        return total
    }

    /**
     * Iterative, bottom-up stack-safe Discrete Fréchet Distance in meters.
     */
    fun discreteFrechetDistance(p: List<Pair<Double, Double>>, q: List<Pair<Double, Double>>): Double {
        val n = p.size
        val m = q.size
        if (n == 0 || m == 0) return 0.0
        val dp = Array(n) { DoubleArray(m) }

        dp[0][0] = haversineM(p[0], q[0])

        for (i in 1 until n) {
            dp[i][0] = maxOf(dp[i - 1][0], haversineM(p[i], q[0]))
        }
        for (j in 1 until m) {
            dp[0][j] = maxOf(dp[0][j - 1], haversineM(p[0], q[j]))
        }
        for (i in 1 until n) {
            for (j in 1 until m) {
                val dist = haversineM(p[i], q[j])
                dp[i][j] = maxOf(
                    minOf(dp[i - 1][j], dp[i - 1][j - 1], dp[i][j - 1]),
                    dist
                )
            }
        }
        return dp[n - 1][m - 1]
    }

    private fun getBbox(polyline: List<Pair<Double, Double>>): DoubleArray {
        var minLat = Double.MAX_VALUE
        var minLng = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var maxLng = -Double.MAX_VALUE
        for (pt in polyline) {
            if (pt.first < minLat) minLat = pt.first
            if (pt.second < minLng) minLng = pt.second
            if (pt.first > maxLat) maxLat = pt.first
            if (pt.second > maxLng) maxLng = pt.second
        }
        return doubleArrayOf(minLat, minLng, maxLat, maxLng)
    }

    private fun bboxOverlaps(a: DoubleArray, b: DoubleArray, padDeg: Double = 0.001): Boolean {
        return !(a[2] + padDeg < b[0] || b[2] + padDeg < a[0] ||
                a[3] + padDeg < b[1] || b[3] + padDeg < a[1])
    }

    fun candidateFilter(trace: List<GPSPoint>, segments: List<Segment>): List<Segment> {
        if (trace.isEmpty() || segments.isEmpty()) return emptyList()
        val traceLatLngs = trace.map { it.latlng }
        val traceBbox = getBbox(traceLatLngs)
        return segments.filter { seg ->
            val segLatLngs = JsonHelper.jsonToPolyline(seg.polylinePointsJson)
            if (segLatLngs.isEmpty()) false
            else {
                val segBbox = getBbox(segLatLngs)
                bboxOverlaps(traceBbox, segBbox)
            }
        }
    }

    private fun findEndpointHits(trace: List<GPSPoint>, target: Pair<Double, Double>, toleranceM: Double): List<Int> {
        return trace.indices.filter { i ->
            haversineM(trace[i].latlng, target) <= toleranceM
        }
    }

    private fun scoreCandidate(subTrace: List<GPSPoint>, segment: Segment, segmentPolyline: List<Pair<Double, Double>>): DoubleArray {
        if (subTrace.size < 2) return doubleArrayOf(0.0, 0.0, 1.0)

        val subPoints = subTrace.map { it.latlng }

        // Corridor coverage: fraction of trace points within tolerance of segment
        var withinCount = 0
        for (pt in subPoints) {
            if (pointToPolylineDistanceM(pt, segmentPolyline) <= CORRIDOR_TOLERANCE_M) {
                withinCount++
            }
        }
        val coverage = withinCount.toDouble() / subPoints.size

        // Discrete Frechet distance
        val frechetM = discreteFrechetDistance(subPoints, segmentPolyline)
        val frechetRatio = frechetM / maxOf(segment.lengthM, 1.0)

        val coverageScore = coverage
        val shapeScore = maxOf(0.0, 1.0 - frechetRatio / MAX_FRECHET_RATIO)
        val confidence = 0.7 * coverageScore + 0.3 * shapeScore

        return doubleArrayOf(confidence, coverage, frechetRatio)
    }

    fun matchActivityToSegment(trace: List<GPSPoint>, segment: Segment): List<SegmentEffort> {
        val segPolyline = JsonHelper.jsonToPolyline(segment.polylinePointsJson)
        if (segPolyline.size < 2) return emptyList()

        val startHits = findEndpointHits(trace, Pair(segment.startLat, segment.startLng), ENDPOINT_TOLERANCE_M)
        val endHits = findEndpointHits(trace, Pair(segment.endLat, segment.endLng), ENDPOINT_TOLERANCE_M)

        if (startHits.isEmpty() || endHits.isEmpty()) return emptyList()

        val efforts = mutableListOf<SegmentEffort>()
        var usedUpTo = -1

        for (startIdx in startHits) {
            if (startIdx <= usedUpTo) continue
            val candidateEnds = endHits.filter { it > startIdx }
            if (candidateEnds.isEmpty()) continue
            val endIdx = candidateEnds.minOrNull() ?: continue

            val subTrace = trace.subList(startIdx, endIdx + 1)
            val scoreResult = scoreCandidate(subTrace, segment, segPolyline)
            val confidence = scoreResult[0]

            if (confidence < MIN_CONFIDENCE) continue

            val elapsedS = (trace[endIdx].timeMs - trace[startIdx].timeMs) / 1000.0
            if (elapsedS <= 0.0) continue

            val avgSpeedMps = segment.lengthM / elapsedS
            val ceiling = MAX_PLAUSIBLE_SPEED_MPS[segment.sportType] ?: 30.0
            val flagged = avgSpeedMps > ceiling
            val flagReason = if (flagged) {
                String.format("Average speed %.1f m/s exceeds %s ceiling of %.1f m/s", avgSpeedMps, segment.sportType, ceiling)
            } else null

            efforts.add(SegmentEffort(
                activityId = 0L, // To be updated upon activity insertion
                segmentId = segment.id,
                segmentName = segment.name,
                startSeq = startIdx,
                endSeq = endIdx,
                startTime = trace[startIdx].timeMs,
                elapsedTimeSeconds = elapsedS,
                avgSpeedMps = avgSpeedMps,
                matchConfidence = confidence,
                isPr = false, // Configured later by comparing with existing user records
                flaggedAnomalous = flagged,
                flagReason = flagReason
            ))
            usedUpTo = endIdx
        }

        return efforts
    }

    fun matchActivity(trace: List<GPSPoint>, segments: List<Segment>): List<SegmentEffort> {
        val candidates = candidateFilter(trace, segments)
        val results = mutableListOf<SegmentEffort>()
        for (seg in candidates) {
            results.addAll(matchActivityToSegment(trace, seg))
        }
        return results
    }
}
