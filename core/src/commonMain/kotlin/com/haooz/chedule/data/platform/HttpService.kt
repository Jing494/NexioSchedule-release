package com.haooz.chedule.data.platform

/**
 * 跨平台 HTTP 门面。
 *
 * 这里只定义接口，各平台给实现（Android → OkHttp，JVM → HttpURLConnection，iOS → NSURLSession）。
 * 注意：引入 Ktor 会把 kotlinx-coroutines 从 1.9.0 顶到 1.10+，影响提醒、同步与小组件，
 * 不要顺手替换。
 *
 * 语义：网络异常照常抛出（调用方普遍自带 try/catch）；响应体一次性读入内存。
 */
interface HttpService {

    /**
     * GET，返回状态码与响应体。
     *
     * @param maxBytes 响应体上限，`<= 0` 表示不限制。超出时**不抛异常**，而是返回
     *   [HttpResult.truncated] = true，由调用方决定怎么处理（迁移前 `ScriptRepository`
     *   就是「超限即视为下载失败」）。
     *   ⚠ 平台实现必须**边读边计数、超限即中止**，不能读完再检查大小（那样 OOM 已经发生）。
     */
    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        maxBytes: Long = -1,
    ): HttpResult

    /** POST，body 为 UTF-8 文本。 */
    suspend fun post(
        url: String,
        body: String,
        contentType: String = "application/json",
        headers: Map<String, String> = emptyMap(),
    ): HttpResult

    /**
     * 任意方法。WebDAV 备份用到 `PROPFIND` / `MKCOL`。
     * @param body 为 null 表示无请求体
     */
    suspend fun request(
        method: String,
        url: String,
        body: String? = null,
        contentType: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): HttpResult

    /**
     * 流式 POST：响应体**边到边回调**，每收到完整一行就交给 [onLine]。
     *
     * 只给 AI 的 SSE 用（`/api/ai/agent/stream`）：思考内容只有在流里才有中间态，
     * 一次性读完再切行等于又退回「想完才显示」。
     *
     * @param onLine 在 IO 线程上调用；回调里更新 StateFlow 是安全的，但不要做耗时操作，
     *   会拖住读取（服务的 keep-alive 会因此超时）。
     *
     * 默认实现退化为「一次性读完再按行切」：iOS 侧 NSURLSession 目前只有一次性回调，
     * 拿不到真流式（表现为思考整段跳出来），但协议与调用方完全一致。
     */
    suspend fun streamPost(
        url: String,
        body: String,
        contentType: String = "application/json",
        headers: Map<String, String> = emptyMap(),
        onLine: suspend (String) -> Unit,
    ) {
        val result = post(url, body, contentType, headers)
        result.text.lineSequence().forEach { line ->
            if (line.isNotBlank()) onLine(line)
        }
    }
}

/**
 * 一次性读空的响应。
 *
 * @param truncated 是否因超出 `maxBytes` 而被截断。为 true 时 [bytes] 是不完整内容，
 *   调用方应视为失败，不要拿去解析。
 */
class HttpResult(
    val code: Int,
    val bytes: ByteArray,
    val truncated: Boolean = false,
) {
    val isSuccessful: Boolean get() = code in 200..299

    /** 响应体按 UTF-8 解码。 */
    val text: String get() = bytes.decodeToString()

    override fun toString(): String =
        "HttpResult(code=$code, bytes=${bytes.size}${if (truncated) ", truncated" else ""})"
}

/**
 * 在读取过程中最多读 [maxBytes]：超限立即停手，返回（已读字节, 是否被截断）。
 *
 * 抽到 commonMain 是因为各平台实现需要同一套语义。用「按块收集再拼接」而不是
 * `ByteArrayOutputStream`（JVM 专有）。超限时保留到上限为止的字节并标记截断。
 *
 * @param read 每次调用读一段到给定缓冲区，返回读到的字节数，-1 表示流结束。
 */
internal fun readAtMost(maxBytes: Long, read: (ByteArray) -> Int): Pair<ByteArray, Boolean> {
    if (maxBytes <= 0) return ByteArray(0) to false
    val buffer = ByteArray(8192)
    val chunks = ArrayList<ByteArray>()
    var total = 0L
    var truncated = false
    while (true) {
        val n = read(buffer)
        if (n == -1) break
        total += n
        if (total > maxBytes) {
            // 只保留到上限为止，多出的部分丢弃
            val keep = (n - (total - maxBytes)).toInt()
            if (keep > 0) chunks.add(buffer.copyOf(keep))
            truncated = true
            break
        }
        chunks.add(buffer.copyOf(n))
    }
    val size = chunks.sumOf { it.size }
    val out = ByteArray(size)
    var pos = 0
    for (chunk in chunks) {
        chunk.copyInto(out, pos)
        pos += chunk.size
    }
    return out to truncated
}

/**
 * 超时配置（秒）。
 *
 * 迁移前每个调用点各自配超时，实测有 4 种组合：5/5、10/30、10/10+15、12/20+30。
 * 这里保留同样的粒度，不擅自统一。
 */
data class HttpTimeouts(
    val connectSeconds: Long = 10,
    val readSeconds: Long = 10,
    /** <=0 表示不设置（对应 OkHttp 不调 callTimeout）。 */
    val callSeconds: Long = 0,
)

/** 平台实现（Android→OkHttp / JVM→UrlConnection / iOS→NSURLSession）。外部请用 [createHttpService]。 */
expect fun createPlatformHttpService(timeouts: HttpTimeouts): HttpService

/** 访问密钥请求头；App 开源，密钥可被逆向，只挡随手写脚本的，兜底靠服务端额度与预算。 */
private const val ACCESS_KEY_HEADER = "X-Nexio-Key"

/** 只看前缀会把 `nexioschedule.icu.evil.com` 也认成自家域名，必须要求主机后紧跟 `/`。 */
private fun isOwnServer(url: String): Boolean {
    if (!url.startsWith(ApiEndpoints.BASE)) return false
    val rest = url.substring(ApiEndpoints.BASE.length)
    return rest.isEmpty() || rest.startsWith("/") || rest.startsWith("?") || rest.startsWith("#")
}

/** 只给自家服务端加密钥头：BYOK / WebDAV 等第三方地址共用这个 HttpService，不能漏出去。 */
internal fun withAccessKey(url: String, headers: Map<String, String>): Map<String, String> {
    if (!isOwnServer(url)) return headers
    val key = NexioAccessKey.VALUE
    if (key.isEmpty() || headers.containsKey(ACCESS_KEY_HEADER)) return headers
    return headers + (ACCESS_KEY_HEADER to key)
}

/**
 * 本方案**只关闭「免费 AI」**这一类对上游自建服务端的调用，其余一律照旧。
 *
 * 为什么只关 AI：那个服务端是上游作者自己部署的、**不在本仓库里**；它按 App 版本放行，
 * 并要求请求头带一个由服务端下发的访问密钥（`NEXIO_ACCESS_KEY`）。该密钥只存在于上游的
 * 发布构建里（`core/src/accessKeySecret/`，已 gitignore），开源仓与我们的 CI 都拿不到 ——
 * 实测所有自家接口一律 `403 {"error":"这个版本已经停止服务…"}`。
 *
 * 免费 AI 是唯一「用户会一直卡在失败上、且没有替代路径」的那类：不拦它，界面会停在
 * 「正在读取额度…」或一句看不懂的 403。拦下来就能直接告诉用户去配自己的服务商（BYOK）。
 *
 * ⚠ **不在拦截范围**（都不碰）：
 *   · 天气：小米 / Open-Meteo / 中国天气网（第三方域名，本来就不经过这里）
 *   · 节假日：`api.apihubs.cn` 与 `unpkg.com/holiday-calendar`（第三方）
 *   · 公告 / 统计上报 / 赞赏榜 / 反馈 / 分享口令（拦了只会多出一堆无用报错，且它们
 *     失败时本来就有兜底；保持原样，将来拿到密钥即可恢复）
 *   · BYOK 第三方地址、WebDAV、Gitee 脚本仓库、检查更新用的 GitHub/Gitee API
 */
private const val FREE_AI_CALLS_ENABLED = false

/** 被主动拦截时返回给调用方的固定话术（UI 直接展示，不出现“403”这种看不懂的数字）。 */
const val FREE_AI_DISABLED_MESSAGE =
    "本方案未启用 Nexio 免费 AI，请到「AI 功能」里配置自己的模型服务商"

/** 免费 AI 的入口路径：`/api/ai`、`/api/ai/`、`/api/ai/agent`、`/api/ai/public-key`… */
private fun isFreeAiEndpoint(url: String): Boolean {
    if (!isOwnServer(url)) return false
    val rest = url.substring(ApiEndpoints.BASE.length).substringBefore('?').substringBefore('#')
    // /api/ai 本身，或 /api/ai/ 前缀下的任意子路径（agent / capabilities / public-key / edu-scripts）
    return rest == "/api/ai" || rest.startsWith("/api/ai/")
}

/** 这条 URL 会不会被主动拦截（免费 AI + 开关关闭）。 */
internal fun isBlockedSelfHostedCall(url: String): Boolean =
    !FREE_AI_CALLS_ENABLED && isFreeAiEndpoint(url)

/**
 * 「免费 AI（走 Nexio 自建服务端）」这一档当前不可用。
 *
 * 调用方用它决定**要不要走免费 AI 分支**：为 true 时应当直接返回
 * [FREE_AI_DISABLED_MESSAGE]，而不是发一次注定被拦的请求。
 */
fun isFreeAiDisabled(): Boolean = !FREE_AI_CALLS_ENABLED

/** 透明代理：四个方法都只做「补请求头」，其余原样转发。 */
private class AccessKeyHttpService(private val delegate: HttpService) : HttpService {

    /**
     * 拦截落点：返回 503 + 固定话术，**不发任何网络请求**。
     *
     * 用 503 而不是 403/200：调用方普遍按 `isSuccessful` 判失败，503 天然落进失败分支；
     * 同时把话术塞进响应体，UI 那些「把服务端 error 原样带出来」的路径能直接显示人话。
     */
    private fun blocked(): HttpResult = HttpResult(
        code = 503,
        bytes = """{"error":"$SELF_HOSTED_DISABLED_MESSAGE"}""".encodeToByteArray(),
    )

    override suspend fun get(
        url: String,
        headers: Map<String, String>,
        maxBytes: Long,
    ): HttpResult = if (isBlockedSelfHostedCall(url)) {
        blocked()
    } else {
        delegate.get(url, withAccessKey(url, headers), maxBytes)
    }

    override suspend fun post(
        url: String,
        body: String,
        contentType: String,
        headers: Map<String, String>,
    ): HttpResult = if (isBlockedSelfHostedCall(url)) {
        blocked()
    } else {
        delegate.post(url, body, contentType, withAccessKey(url, headers))
    }

    override suspend fun request(
        method: String,
        url: String,
        body: String?,
        contentType: String?,
        headers: Map<String, String>,
    ): HttpResult = if (isBlockedSelfHostedCall(url)) {
        blocked()
    } else {
        delegate.request(method, url, body, contentType, withAccessKey(url, headers))
    }

    /** 显式转发：用默认实现会把 SSE 变成「读完再切行」。 */
    override suspend fun streamPost(
        url: String,
        body: String,
        contentType: String,
        headers: Map<String, String>,
        onLine: suspend (String) -> Unit,
    ) {
        // 拦截时**一行都不回调**：调用方靠"流里没有 event"收尾，不会卡住
        if (isBlockedSelfHostedCall(url)) return
        delegate.streamPost(url, body, contentType, withAccessKey(url, headers), onLine)
    }
}

/** 全 App 统一的 HTTP 入口：平台实现外包一层访问密钥，调用方无需关心。 */
fun createHttpService(timeouts: HttpTimeouts): HttpService =
    AccessKeyHttpService(createPlatformHttpService(timeouts))
