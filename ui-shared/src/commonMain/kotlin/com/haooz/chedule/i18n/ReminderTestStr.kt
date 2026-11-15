package com.haooz.chedule.i18n

/**
 * 「通知与超级岛测试」页内文案。
 *
 * ⚠ 只放**页面自身的措辞**（标题、状态词、按钮、脚注）。
 * 具体场景的文案由宿主侧的 `ReminderTestScenario` 提供 —— 本页只展示与派发，
 * 在那里再写一份就是「两处各写一套」（本项目已经踩过两次）。
 */
object ReminderTestStr {

    // ── 通道卡 ──
    val currentChannel = L("当前通道", "Current channel")
    val reread = L("重新读取", "Re-read")
    val channelExplain = L(
        "课程链路与返校提醒都按这个通道发：开了超级岛走岛，关掉走原生实时动态。" +
            "清单 / 余额 / 次日提醒是普通通知，不走岛。",
        "Course and return-to-school reminders both use this channel: with Super Island on they go " +
            "through the island, otherwise through the native live activity. Checklists, balance and " +
            "next-day reminders are regular notifications and never use the island.",
    )

    // ── 场景行 ──
    val send = L("发送", "Send")
    /** 点一条场景后的提示：已发送 + 场景名（括号里是通道） */
    val sentToast = L("已发送：%s（%s）", "Sent: %s (%s)")
    val plainNotification = L("普通通知", "Regular notification")

    // ── 空状态 ──
    val emptyHint = L(
        "当前平台没有可用的测试场景：这些场景测的是 Android 的通知通道 / 超级岛 / 实时动态。",
        "No test scenarios on this platform: these scenarios exercise Android notification " +
            "channels, Super Island and live activities.",
    )

    // ── 脚注 ──
    val footnote = L(
        "说明：\n" +
            "• 课程场景的「到点自动切换」靠刷新链对账，最多滞后一个刷新周期，属正常；\n" +
            "• 课中卡到下课点会自动收岛，那是时序走到了，不是掉线；\n" +
            "• 「课前提醒」的普通通知没有单独入口：它和岛 / 实时动态共用\n" +
            "  sendPreClassNotification 的同一条时间线，由上面的课程场景覆盖。",
        "Notes:\n" +
            "• The automatic switch at class start is reconciled by the refresh chain; a delay of up " +
            "to one refresh cycle is normal.\n" +
            "• The in-class card retracts the island at the end of class — that is the timeline " +
            "reaching its end, not a disconnect.\n" +
            "• The pre-class reminder has no separate entry: it shares the same timeline as the " +
            "island / live activity via sendPreClassNotification, covered by the course scenarios above.",
    )

    // ── 分组标题（场景表在宿主侧，标题也走这里：一处语言源）──
    val groupCourseFlow = L("课程链路（走当前通道）", "Course flow (current channel)")
    val groupReturnFlow = L("返校链路", "Return-to-school flow")
    val groupOther = L("其他通知", "Other notifications")

    // ── 通知通道卡（读系统里这几个通道的真实状态）──
    val channelCardTitle = L("通知通道", "Notification channels")
    val channelCardHint = L(
        "这些是 App 在系统里建的通知通道。停用的通道不会显示对应提醒，点一下可以去系统设置里改。",
        "These are the notification channels the app registered with the system. A disabled channel " +
            "won't show its reminders — tap to change it in system settings.",
    )
    val channelOn = L("开启", "On")
    val channelOff = L("已停用", "Disabled")
    val channelMissing = L("未创建", "Not created")
    val channelImportanceBlocked = L("被设为静音", "Silenced")
    val channelReadFailed = L("读不到系统通道状态", "Can't read channel state from the system")
    val channelOpenSettings = L("打开通知设置", "Open notification settings")

    // ── 通道名（宿主侧判定后按语言取值）──
    val channelIsland = L("超级岛", "Super Island")
    val channelLiveActivity = L("原生实时动态", "Native live activity")

    // ── 页面标题（顶栏 + 设置入口）──
    val pageTitle = L("通知与超级岛测试", "Notifications & Super Island Test")
    /** 设置页入口标题（短一点，配合图标） */
    val settingsEntry = L("通知与超级岛测试", "Notifications & Island")
    /** 搜索索引里的副标题（会整句做关键词匹配） */
    val searchSummary = L("发测试通知、查看系统通知通道状态", "Send test notifications and check channel state")
}
