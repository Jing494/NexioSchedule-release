package com.haooz.chedule.i18n

/**
 * 「提醒体检」页内文案。
 *
 * 为什么单独一个对象：这套检查项散落在 [com.haooz.chedule.ui.activities] 的
 * CourseReminderScreen 里，原先全是硬编码中文 —— English 界面下整块显示中文。
 * 这里集中收口，两边都从这里取。
 */
object ReminderHealthStr {

    // ── 卡片标题与状态 ──
    val title = L("提醒体检", "Reminder Health")
    val allPassed = L("全部通过", "All checks passed")

    /** 有 N 项待处理 */
    val pendingN = L("%d 项待处理", "%d need attention")
    val expand = L("展开", "Expand")
    val collapse = L("收起", "Collapse")
    val tapToExpand = L(
        "点这一行展开，能看到是哪几项、并直接跳去设置",
        "Tap to see which checks need attention and jump straight to the setting",
    )
    val recheck = L("重新检查", "Check again")
    val goToSettings = L("去设置", "Open setting")

    // ── ① 通知权限 ──
    val notifPermission = L("通知权限", "Notification permission")
    val notifAllowed = L("已允许", "Allowed")
    val notifBlocked = L("被关闭了，所有提醒都发不出来", "Turned off — no reminder can be delivered")

    // ── ② 后台不受限 ──
    val batteryUnrestricted = L("后台不受限", "Unrestricted in background")
    val batteryWhitelisted = L("已加入电池优化白名单", "Already whitelisted for battery optimization")
    val batteryNotWhitelisted = L(
        "未加入白名单，锁屏久了提醒可能被系统压下",
        "Not whitelisted — reminders may be suppressed after the screen has been off for a while",
    )

    // ── ③ 精确闹钟 ──
    val exactAlarm = L("精确闹钟", "Exact alarms")
    val exactAvailable = L("可用，提醒能对准分钟", "Available — reminders fire to the minute")
    val exactUnavailable = L("不可用，提醒会漂移甚至不发", "Unavailable — reminders drift or never fire")

    // ── ④ 后台自启动 ──
    val autoStart = L("后台自启动", "Auto-start in background")
    val autoStartRestricted = L(
        "系统已限制后台运行，锁屏久了提醒可能被压住",
        "Background running is restricted — reminders may be suppressed",
    )
    val autoStartOk = L(
        "系统未限制后台；厂商的自启动开关需在应用信息里手动确认",
        "Not restricted by the system; vendor auto-start switches must be confirmed in App info",
    )

    // ── ⑤ 提醒通道 ──
    val notifChannels = L("提醒通道", "Notification channels")
    val channelsBlockedN = L(
        "%d 个通道被停用，对应提醒不会显示",
        "%d channel(s) disabled — those reminders won't show",
    )
    val channelsMissing = L(
        "尚未创建（发出第一条提醒后会自动建好）",
        "Not created yet (they are set up automatically after the first reminder)",
    )
    val channelsAllOn = L(
        "课前提醒 / 假期与返校 / 实时动态 都开着",
        "Pre-class, holiday & return-to-school, and live activity are all on",
    )

    // ── ⑥ 系统勿扰 ──
    val systemDnd = L("系统勿扰", "System Do Not Disturb")
    val dndOn = L(
        "系统当前处于勿扰/静音：普通提醒会被静音（实时动态通道已申请绕过勿扰）",
        "Do Not Disturb is on: regular reminders are silenced (the live activity channel requests an exemption)",
    )
    val dndOff = L("未开启", "Off")

    // ── ⑦ 假期课程排除（说明项） ──
    val holidayExclusion = L("假期课程排除", "Holiday course exclusion")

    /** 说明项正文：前缀 + 节次范围 + 后缀（拼接顺序随语言变化，所以整句给两条模板） */
    val holidayExclusionBody = L(
        "假期课程排除：%s 不展示也不提醒（正常）",
        "Holiday course exclusion: %s not shown and no reminders (expected)",
    )
    val holidayBeforeRange = L("前一天 %d~%d 节", "day before, periods %d–%d")
    val holidayEndRange = L("最后一天 %d~%d 节", "last day, periods %d–%d")
}
