package com.haooz.nexio.ohos

import com.haooz.chedule.data.platform.ScheduleChangeSink

/**
 * 课程 / 课表变更后的平台副作用（鸿蒙）。
 *
 * 与安卓 `AndroidScheduleEffects` 同职责：提醒重排、小组件刷新。
 * （本 fork 已移除手表互联，`ScheduleChangeSink` 里没有 `syncWearable` 了这个成员，
 *   鸿蒙侧也就不能再留空实现 —— 留着会编译不过。）
 */
internal object OhosScheduleEffects : ScheduleChangeSink {

    override fun rescheduleReminders() {
        // 课表变更同样要刷新卡片快照：共享层只在「重排提醒」这一个出口上挂变更事件
        OhosWidgetSnapshot.refresh()
        OhosCourseReminder.reconcile()
    }

    override fun refreshWidgets() {
        OhosWidgetSnapshot.refresh()
    }
}
