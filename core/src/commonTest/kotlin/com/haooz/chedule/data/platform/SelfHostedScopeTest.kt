package com.haooz.chedule.data.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * 主动拦截的范围：**只有免费 AI**。
 *
 * 为什么要钉：那个服务端（nexioschedule.icu）是上游作者自建的，按 App 版本放行、
 * 还要一个我们拿不到的访问密钥，实测一律 403。拦它是为了给用户一句人话；
 * 但**多拦一个都是事故** —— 天气（小米 / Open-Meteo / 中国天气网）、
 * 节假日（apihubs / unpkg）、BYOK 第三方地址都必须照常工作。
 *
 * 这些 URL 全部取自代码里的真实常量，不是编出来的样例。
 */
class SelfHostedScopeTest {

    private val base = ApiEndpoints.BASE

    @Test
    fun freeAiIsBlocked() {
        // 免费 AI 的所有入口（含登录态之外的公开路径）
        listOf(
            "$base/api/ai",
            "$base/api/ai/",
            "$base/api/ai/agent",
            "$base/api/ai/capabilities",
            "$base/api/ai/public-key",
            "$base/api/ai/edu-scripts",
            "$base/api/ai?task=shrink_classrooms",
        ).forEach { url ->
            assertEquals(true, isBlockedSelfHostedCall(url), "免费 AI 应被拦截：$url")
        }
    }

    @Test
    fun weatherIsNeverBlocked() {
        // 三家天气源的真实域名（WeatherRepository 的 FUSION 就是并发打这三个）
        listOf(
            "https://weatherapi.market.xiaomi.com/wtr-v3/weather/all",
            "https://api.open-meteo.com/v1/forecast?latitude=1&longitude=2",
            "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=1",
            "https://d1.weather.com.cn/sk_2d/101010100.html",
            "https://www.weather.com.cn/",
        ).forEach { url ->
            assertFalse(isBlockedSelfHostedCall(url), "天气不能被拦：$url")
        }
    }

    @Test
    fun holidayIsNeverBlocked() {
        // HolidayManager 的两个真实数据源
        listOf(
            "https://api.apihubs.cn/holiday/get?year=2026&size=500",
            "https://unpkg.com/holiday-calendar@1/data/CN/2026.json",
        ).forEach { url ->
            assertFalse(isBlockedSelfHostedCall(url), "节假日不能被拦：$url")
        }
    }

    @Test
    fun otherSelfHostedEndpointsAreLeftAlone() {
        // 这些接口虽然也 403，但保持原样：它们失败时有兜底，拦了只会多出一堆无用报错
        listOf(
            "$base/api/notice",
            "$base/api/stats/report",
            "$base/api/appreciations",
            "$base/api/appreciations/top",
            "$base/api/feedback",
            "$base/api/share",
        ).forEach { url ->
            assertFalse(isBlockedSelfHostedCall(url), "本次只关免费 AI，不该拦：$url")
        }
    }

    @Test
    fun byokAndThirdPartyAreNeverBlocked() {
        // BYOK 自配必须畅通，这是关掉免费 AI 之后唯一可用的 AI 路径
        listOf(
            "https://api.deepseek.com/chat/completions",
            "https://api.xiaomimimo.com/v1/chat/completions",
            "https://open.bigmodel.cn/api/paas/v4/chat/completions",
            "https://api.moonshot.cn/v1/chat/completions",
            "https://api.openai.com/v1/chat/completions",
            // 相近域名的伪装不能被误判成自家
            "https://nexioschedule.icu.evil.com/api/ai/agent",
        ).forEach { url ->
            assertFalse(isBlockedSelfHostedCall(url), "BYOK / 第三方不能被拦：$url")
        }
    }
}
