package com.flathike.app

import com.flathike.app.ai.GemmaPromptBuilder
import com.flathike.app.ai.GemmaTrailIntelligenceEngine
import com.flathike.app.calculator.TrackAnalyzer
import com.flathike.app.data.DemoTracks
import com.flathike.app.model.HikingPreset
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GemmaAiIntegrationTest {

    @Test
    fun testGemmaPromptBuilderContent() {
        val elbrusTrack = DemoTracks.allTracks.first()
        val result = TrackAnalyzer.analyze(elbrusTrack.points, elbrusTrack.name)
        val prompt = GemmaPromptBuilder.buildTrailAnalysisPrompt(result, HikingPreset.SWISS_SAC, 10.0)

        assertTrue(prompt.contains("Эльбрус"))
        assertTrue(prompt.contains("WGS-84 ортодромия"))
        assertTrue(prompt.contains("Швейцарский SAC"))
        assertTrue(prompt.contains("k = 10.00"))
        assertTrue(prompt.contains("Функция Тоблера"))
        assertTrue(prompt.contains("Правило Найсмита"))
        assertTrue(prompt.contains("Геодезическая редукция"))
    }

    @Test
    fun testGemmaTrailIntelligenceReport() {
        val elbrusTrack = DemoTracks.allTracks.first()
        val result = TrackAnalyzer.analyze(elbrusTrack.points, elbrusTrack.name)
        val report = GemmaTrailIntelligenceEngine.generateTrailReport(result, HikingPreset.SWISS_SAC, 10.0)

        assertNotNull(report)
        assertTrue(report.contains("Gemma Trail AI"))
        assertTrue(report.contains("Оценка сложности"))
        assertTrue(report.contains("По прямой на уровне моря"))
        assertTrue(report.contains("Коэффициент подъема"))
        assertTrue(report.contains("Рекомендации по экипировке"))
        assertTrue(report.contains("Вода:"))
        assertTrue(report.contains("уклон") || report.contains("Уклон"))
    }

    @Test
    fun testGemmaQuestionsAnswering() {
        val elbrusTrack = DemoTracks.allTracks.first()
        val result = TrackAnalyzer.analyze(elbrusTrack.points, elbrusTrack.name)

        // Question about coefficients
        val ansCoeff = GemmaTrailIntelligenceEngine.answerQuestion(
            "Зачем нужны коэффициенты при подъеме?",
            result,
            HikingPreset.SWISS_SAC,
            10.0
        )
        assertTrue(ansCoeff.contains("Швейцарский SAC") || ansCoeff.contains("Naismith"))
        assertTrue(ansCoeff.contains("КПД") || ansCoeff.contains("тяжести"))

        // Question about straight line at sea level
        val ansSeaLevel = GemmaTrailIntelligenceEngine.answerQuestion(
            "Что значит расстояние по прямой на уровне моря?",
            result,
            HikingPreset.SWISS_SAC,
            10.0
        )
        assertTrue(ansSeaLevel.contains("WGS-84") || ansSeaLevel.contains("уровне моря"))
        assertTrue(ansSeaLevel.contains("ортодромическая") || ansSeaLevel.contains("дуги"))

        // Question about water
        val ansWater = GemmaTrailIntelligenceEngine.answerQuestion(
            "Сколько воды взять?",
            result,
            HikingPreset.SWISS_SAC,
            10.0
        )
        assertTrue(ansWater.contains("воды") || ansWater.contains("л"))

        // Question about equipment
        val ansGear = GemmaTrailIntelligenceEngine.answerQuestion(
            "Какое снаряжение нужно?",
            result,
            HikingPreset.SWISS_SAC,
            10.0
        )
        assertTrue(ansGear.contains("ботинки") || ansGear.contains("палки") || ansGear.contains("экипировке"))

        // Question about loop route
        val loopTrack = DemoTracks.allTracks.first { it.name.contains("Мыс Айя") }
        val loopResult = TrackAnalyzer.analyze(loopTrack.points, loopTrack.name)
        val ansLoop = GemmaTrailIntelligenceEngine.answerQuestion(
            "Это кольцевой маршрут?",
            loopResult,
            HikingPreset.SWISS_SAC,
            10.0
        )
        assertTrue(ansLoop.contains("кольцевым"))
        assertTrue(ansLoop.contains("удаление"))

        // Question about time and pace
        val ansTime = GemmaTrailIntelligenceEngine.answerQuestion(
            "Сколько времени займет поход?",
            result,
            HikingPreset.SWISS_SAC,
            10.0
        )
        assertTrue(ansTime.contains("Тоблера") || ansTime.contains("Найсмита") || ansTime.contains("мин"))

        // Question about slope and steepness
        val ansSlope = GemmaTrailIntelligenceEngine.answerQuestion(
            "Какой максимальный уклон и рельеф?",
            result,
            HikingPreset.SWISS_SAC,
            10.0
        )
        assertTrue(ansSlope.contains("уклон") || ansSlope.contains("склон"))
    }
}
