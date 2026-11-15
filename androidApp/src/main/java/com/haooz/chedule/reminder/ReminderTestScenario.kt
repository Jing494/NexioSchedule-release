package com.haooz.chedule.reminder

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.haooz.chedule.data.ReturnDayReminder
import com.haooz.chedule.data.repo.CourseRepository
import com.haooz.chedule.i18n.L
import com.haooz.chedule.i18n.L10n
import com.haooz.chedule.i18n.ReminderTestStr
import com.haooz.chedule.ui.screens.ReminderTestChannel
import com.haooz.chedule.ui.screens.ReminderTestEntry
import com.haooz.chedule.ui.screens.ReminderTestGroup
import java.time.LocalDate

/** 测试要直接落到哪个状态（用于单独验证某一条分支） */
enum class ReminderTestPhase { COUNTDOWN, STARTED, IN_CLASS }

/** 场景属于哪条链路 —— 决定发送时调哪支函数 */
private enum class ReminderTestKind {
    /** 课前倒计时 / 已上课 / 课中：按当前通道（岛或实时动态）发 */
    COURSE,

    /** 返校（当日）→ 日子词应为「今晚返校」 */
    RETURN_TODAY,

    /** 返校（次日）→ 日子词应为「明天返校」 */
    RETURN_TOMORROW,

    /** 返校准备清单（普通通知） */
    PLAIN_PREP,

    /** 假期余额（普通通知） */
    PLAIN_BALANCE,

    /** 次日课程提醒（普通通知） */
    PLAIN_NEXT_DAY,
}

/**
 * 「通知 / 超级岛 / 实时动态」测试场景表。
 *
 * 存在的理由：原来全项目只有一个测试按钮、只有一种时序（课前 70 秒 + 课中 2 分钟），
 * 覆盖不到「到点自动切课中」「课中距下课倒计时」「长课不中途收岛」「返校岛」这些真实分支 ——
 * 于是"上课倒计时正常、下课倒计时没了"这类问题只能靠碰。
 *
 * 本文件只是**场景表 + 分发**，不复制任何发送逻辑：真正发送仍然走主路径那几支函数，
 * 通道判定也与主路径完全一致（岛开着且设备支持 → 走岛，否则走原生实时动态），
 * 避免"测的那套"和"跑的那套"分叉。
 *
 * ⚠ 这里**不写任何"当天只发一次"的去重状态**：那会顶掉当天的正式提醒
 *   （点一下测试按钮，正式提醒就被当成"今天发过了"）。
 */
enum class ReminderTestScenario(
    private val label: L10n,
    private val detail: L10n,
    private val kind: ReminderTestKind,
    /** 仅 COURSE 用：距"上课"还有多久 / 课时长度 */
    private val startDelayMs: Long = 0L,
    private val durationMs: Long = 0L,
    private val phase: ReminderTestPhase = ReminderTestPhase.COUNTDOWN,
    /** 走超级岛 / 实时动态通道（普通通知为 false）；决定结果提示里写哪条通道 */
    private val liveContainer: Boolean = false,
) {
    // ---------- 课程链路（走当前通道）----------
    FULL_TIMELINE(
        label = L("课前倒计时 → 到点切课中", "Pre-class countdown → switch to in-class at start"),
        detail = L("70 秒倒计时，到点自动切课中卡（课时 3 分钟）", "70-second countdown, then switches to the in-class card at start time (3-minute class)"),
        kind = ReminderTestKind.COURSE,
        startDelayMs = 70_000L,
        durationMs = 180_000L,
        phase = ReminderTestPhase.COUNTDOWN,
        liveContainer = true,
    ),
    STARTED(
        label = L("已上课（静态）", "Class started (static)"),
        detail = L("强制发静态「已上课」，约 15 秒后自动收起", "Force a static “Class started” card; it retracts after about 15 seconds"),
        kind = ReminderTestKind.COURSE,
        durationMs = 60_000L,
        phase = ReminderTestPhase.STARTED,
        liveContainer = true,
    ),
    IN_CLASS(
        label = L("课中 · 距下课倒计时", "In class · countdown to the end"),
        detail = L("直接进课中卡（课时 3 分钟），B 区按你的设置显示倒计时或静态文案", "Jump straight to the in-class card (3-minute class); the right side follows your setting (countdown or static text)"),
        kind = ReminderTestKind.COURSE,
        durationMs = 180_000L,
        phase = ReminderTestPhase.IN_CLASS,
        liveContainer = true,
    ),
    IN_CLASS_LONG(
        label = L("课中 · 长课（20 分钟）", "In class · long class (20 min)"),
        detail = L("验证长时间不会中途收岛，长课时也不该在 60 分钟处自己消失", "Checks the island is not retracted mid-class; a long class must not vanish at the 60-minute mark"),
        kind = ReminderTestKind.COURSE,
        durationMs = 1_200_000L,
        phase = ReminderTestPhase.IN_CLASS,
        liveContainer = true,
    ),

    // ---------- 返校链路 ----------
    RETURN_TODAY(
        label = L("返校 · 当日（今晚返校）", "Return · today (tonight)"),
        detail = L("走当前通道发返校提醒；目标日是今天，日子词应为「今晚返校」", "Sends through the current channel; with today as the target the day word should read “tonight”"),
        kind = ReminderTestKind.RETURN_TODAY,
        liveContainer = true,
    ),
    RETURN_TOMORROW(
        label = L("返校 · 次日（明天返校）", "Return · tomorrow"),
        detail = L("走当前通道发返校提醒；目标日是明天，日子词应为「明天返校」", "Sends through the current channel; with tomorrow as the target the day word should read “tomorrow”"),
        kind = ReminderTestKind.RETURN_TOMORROW,
        liveContainer = true,
    ),
    PLAIN_PREP(
        label = L("返校准备清单（普通通知）", "Return prep checklist (regular notification)"),
        detail = L("与「立即发一条测试」同一条实现，不写当天去重状态", "Same implementation as “send one now”; writes no daily de-dupe state"),
        kind = ReminderTestKind.PLAIN_PREP,
    ),
    PLAIN_BALANCE(
        label = L("假期余额（普通通知）", "Holiday balance (regular notification)"),
        detail = L("文案跟随「节假日首末课程排除」开关；不在假期内会给演示文案", "Wording follows the holiday end-of-holiday course exclusion switch; outside a holiday it shows demo copy"),
        kind = ReminderTestKind.PLAIN_BALANCE,
    ),

    // ---------- 其他普通通知 ----------
    PLAIN_NEXT_DAY(
        label = L("次日课程提醒（普通通知）", "Next-day classes (regular notification)"),
        detail = L("汇总明天节数 + 第一节；文案与正式提醒共用一份实现", "Summarizes tomorrow's periods and first class; shares the real reminder implementation"),
        kind = ReminderTestKind.PLAIN_NEXT_DAY,
    );

    /** 场景名（按当前语言取值；枚举里存的是 L10n，不是已求值的字符串） */
    val labelText: String get() = label()

    /** 场景说明（同上） */
    val detailText: String get() = detail()

    companion object {
        /**
         * 页面「通知通道」卡的数据：这几条通道在系统里的**真实**状态。
         *
         * 为什么要读系统而不是只显示「开着/关了」：通道可能被用户单独静音、也可能还没建出来
         * （全新安装、或本机从没发过那条通知）。两种情况在系统设置里长得不一样，
         * 报成同一句话就是误导 —— 与「提醒体检」第 ⑤ 项同一条口径。
         *
         * 读不到系统服务时返回空列表（页面按「读不到」处理，不编造状态）。
         */
        fun notificationChannels(context: Context): List<ReminderTestChannel> {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return emptyList()
            val ids = listOf(
                CourseReminderHelper.CHANNEL_REMINDER_ID,
                CourseReminderHelper.CHANNEL_HOLIDAY_ID,
                CourseReminderHelper.CHANNEL_LIVE_ID,
            )
            return ids.map { id ->
                val channel = runCatching { manager.getNotificationChannel(id) }.getOrNull()
                ReminderTestChannel(
                    // 通道名以系统里那份为准（用户可能改过）；没建出来时留空，页面显示「未创建」
                    name = channel?.name?.toString().orEmpty(),
                    missing = channel == null,
                    // 静音 = 用户在系统里关掉了这条通道（importance == NONE）
                    disabled = channel != null &&
                        channel.importance == NotificationManager.IMPORTANCE_NONE,
                    // 只降到「静音」档：还能弹，但不响不震 —— 测试时最容易误判成「发了没反应」
                    silenced = channel != null &&
                        channel.importance == NotificationManager.IMPORTANCE_LOW,
                )
            }
        }

        /**
         * 打开本应用的通知设置页（通道都在里面）。
         *
         * 用 ACTION_APP_NOTIFICATION_SETTINGS + EXTRA_APP_PACKAGE：新版系统上旧的
         * "app_package/app_uid" extras 写法已被忽略，会静默落到应用详情，用户还得再点一次。
         */
        fun openNotificationSettings(context: Context) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
        }

        /** 当前实际会走哪条通道 —— 与主路径同一套判定 */
        fun islandActive(context: Context): Boolean =
            CourseRepository().getIslandNotification() &&
                IslandNotificationHelper.isIslandSupported(context)

        /**
         * 当前实际生效的通道文案（按 App 语言取值）。
         *
         * ⚠ 不再带厂商前缀「小米」：一是 English 界面下会中英混排，
         * 二是这页在非 MIUI 设备上也应该读得通。
         */
        fun channelLabel(context: Context): String =
            if (islandActive(context)) ReminderTestStr.channelIsland() else ReminderTestStr.channelLiveActivity()

        /** 当前是否真的走「岛」通道 —— 页面据此显示状态点，别去 contains("岛") 猜文案。 */
        fun isIslandActive(context: Context): Boolean = islandActive(context)

        /** 页面用的分组视图：文案全部来自本表，页面只负责展示 */
        fun groups(): List<ReminderTestGroup> {
            fun List<ReminderTestScenario>.toEntries(): List<ReminderTestEntry> = map {
                ReminderTestEntry(
                    id = it.name,
                    label = it.labelText,
                    detail = it.detailText,
                    liveContainer = it.liveContainer,
                )
            }
            val all = ReminderTestScenario.entries
            return listOf(
                ReminderTestGroup(
                    title = ReminderTestStr.groupCourseFlow(),
                    entries = all.filter { it.kind == ReminderTestKind.COURSE }.toEntries(),
                ),
                ReminderTestGroup(
                    title = ReminderTestStr.groupReturnFlow(),
                    entries = all.filter {
                        it.kind == ReminderTestKind.RETURN_TODAY ||
                            it.kind == ReminderTestKind.RETURN_TOMORROW ||
                            it.kind == ReminderTestKind.PLAIN_PREP ||
                            it.kind == ReminderTestKind.PLAIN_BALANCE
                    }.toEntries(),
                ),
                ReminderTestGroup(
                    title = ReminderTestStr.groupOther(),
                    entries = all.filter { it.kind == ReminderTestKind.PLAIN_NEXT_DAY }.toEntries(),
                ),
            )
        }

        /**
         * 点一条场景：只调主路径发送函数（岛 / 实时动态 / 普通通知），
         * 不在本表里拼文案，也不写任何去重状态。
         *
         * @param id 场景枚举名（页面 `ReminderTestEntry.id`）
         */
        fun send(context: Context, id: String) {
            val scenario = ReminderTestScenario.entries.firstOrNull { it.name == id } ?: return
            when (scenario.kind) {
                ReminderTestKind.COURSE -> if (islandActive(context)) {
                    IslandNotificationHelper.sendTestIslandNotification(
                        context = context,
                        startDelayMs = scenario.startDelayMs,
                        durationMs = scenario.durationMs,
                        phase = scenario.phase,
                    )
                } else {
                    CourseReminderHelper.sendTestLiveNotification(
                        context = context,
                        startDelayMs = scenario.startDelayMs,
                        durationMs = scenario.durationMs,
                        phase = scenario.phase,
                    )
                }

                ReminderTestKind.RETURN_TODAY -> sendReturnDay(context, today = true)
                ReminderTestKind.RETURN_TOMORROW -> sendReturnDay(context, today = false)
                ReminderTestKind.PLAIN_PREP ->
                    CourseReminderHelper.sendReturnDayReminderNow(context, prep = true)
                ReminderTestKind.PLAIN_BALANCE ->
                    CourseReminderHelper.sendReturnDayReminderNow(context, prep = false)
                ReminderTestKind.PLAIN_NEXT_DAY ->
                    CourseReminderHelper.sendNextDayReminderNow(context)
            }
        }

        /**
         * 返校提醒测试：固定用「第11节 18:30」这个晚自习样例 ——
         * 只有晚间课才能稳定验证「今晚返校」那条日子词（16:00 口径）；
         * 假期标签取真实 span，取不到就留空（两条分支都能验到）。
         */
        private fun sendReturnDay(context: Context, today: Boolean) {
            val targetDate = if (today) LocalDate.now() else LocalDate.now().plusDays(1)
            val span = ReturnDayReminder.currentHolidaySpan(context, targetDate)
            val holidayLabel = span?.name?.takeIf { it.isNotBlank() }?.let { "${it}最后一天" }
            if (islandActive(context)) {
                IslandNotificationHelper.sendReturnDayIslandNotification(
                    context = context,
                    courseName = "大学英语Ⅱ",
                    section = "第11节",
                    startTime = "18:30",
                    targetDate = targetDate,
                    holidayLabel = holidayLabel,
                    progressPercent = span?.progressPercent,
                )
            } else {
                CourseReminderHelper.showReturnDayLiveNotification(
                    context = context,
                    courseName = "大学英语Ⅱ",
                    section = "第11节",
                    startTime = "18:30",
                    targetDate = targetDate,
                    holidayLabel = holidayLabel,
                    progressPercent = span?.progressPercent,
                )
            }
        }
    }
}
