package com.haooz.chedule.i18n

/**
 * 「假期余额提醒」与「返校准备清单」的文案（本地定制，上游没有）。
 *
 * 为什么单独一个对象：这两块是 fork 后来加的，文案当初直接写死在
 * `HolidaySettingsScreen.kt` 里 —— English 界面下整块显示中文。
 * 同页上游那些卡片已经全部走 `HolidayStr`，这里补齐同一套口径。
 *
 * ⚠ 只放**这两块**的文案；假期/调休本体的文案仍在 [HolidayStr]。
 */
object ReturnDayStr {

    // ── 分组标题与说明 ──
    val balanceSection = L("假期余额提醒", "Holiday balance")
    val balanceSectionDesc = L(
        "• 假期期间每天一条，含假期最后一天（不占超级岛/实时动态的位置）\n" +
            "• 独立开关：不再挂在「节假日课程排除」下面 —— 关掉豁免不会连带关掉余额提醒",
        "• One notification per holiday day, including the last day (it never takes the island / " +
            "live-activity slot)\n" +
            "• Independent switch: no longer nested under holiday course exclusion — turning that " +
            "off won't turn this off",
    )
    val prepSection = L("返校准备清单", "Return prep checklist")
    val prepSectionDesc = L(
        "• 返校日到点推一条普通通知，内容自己写\n" +
            "• 独立开关：不依赖「节假日课程排除」，关掉豁免它照样会响",
        "• Sends one regular notification on the return day; you write the content\n" +
            "• Independent switch: doesn't depend on holiday course exclusion",
    )

    // ── 余额卡 ──
    val balanceTitle = L("假期余额提醒", "Holiday balance reminder")
    val balanceSummaryDisabled = L("已关闭", "Off")
    /** 每天都提醒：假期中每天 HH:mm 提醒还剩几天 */
    val balanceSummaryDaily = L(
        "假期中每天 %s 提醒还剩几天",
        "Reminds you how many days are left every day at %s",
    )
    /** 只在最后 N 天：只在假期最后 N 天的 HH:mm 提醒 */
    val balanceSummaryLastDays = L(
        "只在假期最后 %d 天的 %s 提醒",
        "Only in the last %d day(s) of the holiday, at %s",
    )
    val startFromLabel = L("从哪天开始提醒", "When to start")
    val leadDaysDaily = L("每天", "Every day")
    val leadDaysLastN = L("只在最后 %d 天", "Last %d day(s) of the holiday")
    val leadDaysDialogHint = L(
        "默认（每天）整个假期都会提醒；选「最后 N 天」则只在剩余天数少于 N 天时提醒。",
        "Default (every day) notifies throughout the holiday; picking \"last N days\" only notifies " +
            "once the remaining days drop below N.",
    )
    val leadDaysOptionN = L("最后%d天", "Last %d day(s)")
    val balanceFootnote = L(
        "• 假期期间每天都发一条，含假期最后一天（以前最后一天会漏掉）\n" +
            "• 不占超级岛/实时动态的位置，只是普通通知\n" +
            "• 最后一天的文案跟「假期末天课程排除」开关走：\n" +
            "  开着说「收拾一下准备返校」，关着只说「好好休息」",
        "• One per holiday day, including the last day (that one used to be missed)\n" +
            "• Never takes the island / live-activity slot — a regular notification only\n" +
            "• The last day's wording follows the end-of-holiday exclusion switch:\n" +
            "  on → \"pack up, you're going back\", off → just \"get some rest\"",
    )

    // ── 清单卡 ──
    val prepTitle = L("返校准备清单", "Return prep checklist")
    val prepSummaryOn = L("返校日 %s 提醒要带的东西", "On the return day at %s, reminds you what to bring")
    /** 与上面同一句 +（默认关闭）：两条分开给，别在代码里拼括号 */
    val prepSummaryOff = L(
        "返校日 %s 提醒要带的东西（默认关闭）",
        "On the return day at %s, reminds you what to bring (off by default)",
    )
    val prepFootnote = L(
        "• 触发条件：当天休息 且 次日要上课（普通周日、假期最后一天）\n" +
            "• 独立开关：不依赖「节假日课程排除」，关掉豁免它照样会响",
        "• Fires when today is a rest day and classes resume tomorrow (normal Sunday, last holiday day)\n" +
            "• Independent switch: doesn't depend on holiday course exclusion",
    )

    // ── 两条共用 ──
    val reminderTime = L("提醒时间", "Reminder time")
    val sendTestNow = L("立即发一条测试", "Send a test now")
    val sendTestNowSummary = L(
        "随时点，不影响每天只发一次的去重",
        "Tap any time; doesn't affect the once-a-day de-duplication",
    )
    val save = L("保存", "Save")
    val cancel = L("取消", "Cancel")

    // ── 时间/清单对话框 ──
    val balanceTimeDialog = L("余额提醒时间", "Balance reminder time")
    /** 滚轮上方的两列标签（与官方「开始 / 结束」同一位置、同一样式） */
    val hourLabel = L("时", "Hour")
    val minuteLabel = L("分", "Minute")
    val prepTimeDialog = L("清单提醒时间", "Checklist reminder time")
    val prepTextLabel = L("清单内容", "Checklist content")
    val prepTextHint = L(
        "返校日按上面设定的时间推送这条内容，用 / 分隔要带的东西",
        "Pushed on the return day at the time above; separate items with \"/\"",
    )
}
