/** 通知 / 超级岛 / 实时动态测试页 - Screen（场景表在宿主侧，本页只展示与派发） */
package com.haooz.chedule.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haooz.chedule.data.platform.showToast
import com.haooz.chedule.i18n.ReminderTestStr
import com.haooz.chedule.ui.components.text.material.Text
import com.haooz.softglass.shape.ContinuousRoundedRectangle
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.layout.CollapsibleTopAppBarDefaults
import top.yukonga.miuix.kmp.layout.SharedScrollBehavior
import top.yukonga.miuix.kmp.layout.collapsibleTopInset
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** 超级岛通道色（与胶囊顶栏选中态同一族）。 */
private val IslandAccent = Color(0xFF5B5BD6)
/** 普通通知通道色。 */
private val NeutralAccent = Color(0xFF8A8A8E)
/** 通道正常（绿）。 */
private val ChannelOkColor = Color(0xFF3BA55D)
/** 通道降到静音档（琥珀）。 */
private val ChannelWarnColor = Color(0xFFE07A2B)
/** 通道被关掉（红）。 */
private val ChannelOffColor = Color(0xFFD7263D)

/**
 * 测试页的一个场景。
 *
 * ⚠ 文案只由场景表（宿主侧 `ReminderTestScenario`）提供，本页**不复刻**任何场景文案与发送逻辑 ——
 * 本项目已经两次踩到「两处各写一套」（页面里再写一份文案/再拼一次通知）。
 * 本页也不知道「超级岛/实时动态怎么发」：点一下只把 [id] 交回宿主。
 */
data class ReminderTestEntry(
    /** 稳定 id（= 场景枚举名）：派发时用它回查场景表，commonMain 不认识宿主侧枚举 */
    val id: String,
    val label: String,
    val detail: String,
    /** 走「超级岛 / 实时动态」通道（普通通知为 false）；只用于结果提示文案 */
    val liveContainer: Boolean = false,
)

/** 场景分组（课程链路 / 返校链路 / 其他通知）。 */
data class ReminderTestGroup(
    val title: String,
    val entries: List<ReminderTestEntry>,
)

/**
 * 一条通知通道在**系统里的真实状态**（宿主侧读 NotificationManager）。
 *
 * ⚠ 「没建出来」与「被用户停用」必须分开：全新安装时通道还不存在，
 * 报成「已停用」就是假警报（提醒体检踩过一次，这里沿用同一口径）。
 */
data class ReminderTestChannel(
    /** 通道名（取系统里那份，用户可能改过）；通道不存在时为空串 */
    val name: String,
    /** 通道尚未创建 */
    val missing: Boolean = false,
    /** 被用户彻底关掉（importance == NONE） */
    val disabled: Boolean = false,
    /** 降到了「静音」档（还能弹，但不响不震） */
    val silenced: Boolean = false,
)

/** 卡片圆角：与课表卡片同一档（20dp），视觉上一眼「同一套器件」。 */
private val CardCorner = 20.dp
/** 行内左侧色条宽度。 */
private val AccentBarWidth = 3.dp

/**
 * 「通知 / 超级岛 / 实时动态」测试页。
 *
 * 独立成页（不再塞在提醒设置页里）：场景表与分发都在宿主的 `ReminderTestScenario`，
 * 本页只负责列表、展示与把点击交回去 —— 发送一律走主路径那几支函数，不在这里拼通知文案。
 *
 * 通道（超级岛 / 原生实时动态）由主路径同一套判定决定，页面上直接显示出来，
 * 免得「点了没反应却不知道发去哪了」。
 *
 * ## 视觉（1.7.x 重做，第二版）
 * - 卡片走 miuix `Card`（与备份、导入导出、提醒设置这些**同为文档页**的页面一致）；
 * - 每个场景带一条**通道色条**（岛=靛蓝 / 普通通知=灰），扫一眼就知道发去哪；
 * - 行按压有缩放 + 触感反馈，点了到底有没有反应不再靠猜；
 * - 顶部通道卡再加一个状态点，与色条同一套语义。
 *
 * ## 为什么这里**不**用 softglass（第一版崩在这）
 * 本页挂在 `DocumentPageScaffold` 的 `content` 里，而那个 content 正被
 * `layerBackdrop(liquidGlassBackdrop)` 整块录进采样层。在 content 里画玻璃卡 =
 * 玻璃去采「正在录制自己」的那一层，softglass README §六 第 1 条写得很直白：
 * **采样层不能采样自己，包在同一层里就是自采样，直接崩或画面全黑。**
 * 全仓的用法都是「玻璃件画在录制层之外」：顶栏按钮、下拉面板、对话框、
 * 长按浮层的课程卡（采的是页面层的组合采样源）。文档页的 content 里没有任何一处玻璃卡，
 * 本页第一版是唯一一个 —— 真机点进去即崩，改回与全站一致的做法。
 *
 * @param channelLabel 当前实际生效的通道文案（宿主判定，与主路径一致）
 * @param islandActive 当前是否走「岛」通道：状态点用它判断，**不要**解析 [channelLabel] 文案
 * @param groups 场景分组；空列表 = 该平台没有可测场景（桌面/iOS）
 * @param channels 系统里这几条通知通道的真实状态（宿主读 NotificationManager）；空 = 读不到
 * @param onRefreshChannel 重新读取通道，返回新的文案（宿主顺带重读 [channels]）
 * @param onOpenChannelSettings 打开本应用的通知设置页（通道在里面）
 * @param onSend 点一条场景：宿主按 [ReminderTestEntry.id] 调主路径发送函数
 */
@Composable
fun ReminderTestScreen(
    channelLabel: String,
    /** 当前是否走「岛」通道（宿主判定）；用它决定状态点，别去解析 channelLabel 文案 */
    islandActive: Boolean = false,
    groups: List<ReminderTestGroup>,
    /** 系统里这几条通知通道的真实状态；空 = 读不到（按「读不到」展示，不编造） */
    channels: List<ReminderTestChannel> = emptyList(),
    onRefreshChannel: () -> String,
    /** 点通道行：打开本应用的通知设置页 */
    onOpenChannelSettings: () -> Unit = {},
    onSend: (ReminderTestEntry) -> Unit,
    scrollBehavior: SharedScrollBehavior? = null,
) {
    // 通道是设置项的派生值：进页面读一次，另给一个「重新读取」入口
    var channel by remember(channelLabel) { mutableStateOf(channelLabel) }

    // Scaffold 只为了拿状态栏内边距：DocumentPageScaffold 的 content 是整屏摆放的
    // （顶栏叠在内容之上），页内必须自己让出状态栏 + 折叠顶栏，否则第一张卡会被胶囊顶栏压住。
    // 与备份 / 导入导出 / 提醒设置这些文档页同一套算法。
    Scaffold(topBar = {}) { paddingValues ->
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .scrollEndHaptic(hapticFeedbackType = HapticFeedbackType.TextHandleMove)
                .collapsibleTopInset(scrollBehavior)
                .then(
                    scrollBehavior?.let { Modifier.nestedScroll(it.nestedScrollConnection) } ?: Modifier
                ),
            contentPadding = PaddingValues(
                start = 16.dp,
                // 状态栏 + 折叠态顶栏高度 + 呼吸：既不被胶囊顶栏盖住，也不让第一张卡贴边
                top = paddingValues.calculateTopPadding() +
                    CollapsibleTopAppBarDefaults.CollapsedHeight + 20.dp,
                end = 16.dp,
                bottom = 120.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (groups.isEmpty()) {
                // 该平台没有可测场景（桌面/iOS）：只给一句说明，别渲染一个空通道卡片
                item(key = "empty") { ReminderTestEmptyHint() }
            } else {
                item(key = "channel") {
                    ReminderTestChannelCard(
                        channel = channel,
                        islandActive = islandActive,
                        onRefresh = { channel = onRefreshChannel() },
                    )
                }
                item(key = "channels") {
                    ReminderTestChannelsCard(
                        channels = channels,
                        onOpenSettings = onOpenChannelSettings,
                    )
                }
                groups.forEach { group ->
                    item(key = "title_" + group.title) { ReminderTestSectionTitle(group.title) }
                    item(key = "card_" + group.title) {
                        ReminderTestScenarioCard(
                            entries = group.entries,
                            channel = channel,
                            onSend = onSend,
                        )
                    }
                }
                item(key = "footnote") { ReminderTestFootnote() }
            }
        }
    }
}

/**
 * 卡片壳：本页所有卡片共用，只负责圆角与底色。
 *
 * 用 `Card` 而不是自绘表面：文档页的视觉基线就是它，备份 / 导入导出 / 提醒设置都这样，
 * 换主题、换莫奈配色时不需要本页再跟一遍。
 */
@Composable
private fun TestCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = CardCorner,
        insideMargin = PaddingValues(0.dp),
    ) {
        content()
    }
}

@Composable
private fun ReminderTestChannelCard(
    channel: String,
    islandActive: Boolean,
    onRefresh: () -> Unit,
) {
    TestCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 状态点：走岛=靛蓝实心，普通实时动态=中空灰。
                // ★ 用宿主给的 islandActive，**不要** contains("岛") —— 那既依赖中文文案，
                //   切到 English 后又永远匹配不上（"Super Island" 里没有「岛」）。
                val islandOn = islandActive
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(ContinuousRoundedRectangle(5.dp))
                        .background(
                            if (islandOn) IslandAccent
                            else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                        )
                )
                Spacer(modifier = Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ReminderTestStr.currentChannel(),
                        style = MiuixTheme.textStyles.body1.copy(fontSize = 12.sp),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        text = channel,
                        style = MiuixTheme.textStyles.title3.copy(fontWeight = FontWeight.SemiBold),
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                PressablePill(text = ReminderTestStr.reread(), onClick = onRefresh)
            }
            Text(
                text = ReminderTestStr.channelExplain(),
                style = MiuixTheme.textStyles.body1.copy(fontSize = 12.sp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
            )
        }
    }
}

/** 小胶囊按钮：按压缩放 + 触感反馈。 */
@Composable
private fun PressablePill(
    text: String,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "pillScale",
    )
    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(ContinuousRoundedRectangle(14.dp))
            .background(MiuixTheme.colorScheme.primary.copy(alpha = if (pressed) 0.22f else 0.14f))
            .clickable(interactionSource = interaction, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.body1.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            color = MiuixTheme.colorScheme.primary,
            maxLines = 1,
        )
    }
}

@Composable
private fun ReminderTestSectionTitle(text: String) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.body1.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
    )
}

/**
 * 一组场景。
 *
 * 做成一张连续卡片、条目之间用极细分隔线（不是每行一张卡）：
 * 场景是「同一件事的几个变体」，切开反而显得零散。
 */
@Composable
private fun ReminderTestScenarioCard(
    entries: List<ReminderTestEntry>,
    channel: String,
    onSend: (ReminderTestEntry) -> Unit,
) {
    TestCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            entries.forEachIndexed { index, entry ->
                ScenarioRow(
                    entry = entry,
                    onClick = {
                        onSend(entry)
                        val where = if (entry.liveContainer) channel else ReminderTestStr.plainNotification()
                        showToast(ReminderTestStr.sentToast(entry.label, where))
                    },
                )
                if (index != entries.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 18.dp)
                            .height(0.5.dp)
                            .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    )
                }
            }
        }
    }
}

/** 单条场景：色条 + 标题 + 说明 + 「发送」，整行按压缩放。 */
@Composable
private fun ScenarioRow(
    entry: ReminderTestEntry,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "rowScale",
    )
    // 通道色：岛用靛蓝，普通通知用中性灰 —— 与顶部状态点同一套语义
    val accent = if (entry.liveContainer) IslandAccent else NeutralAccent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interaction, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                onClick()
            }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = AccentBarWidth, height = 26.dp)
                .clip(ContinuousRoundedRectangle(2.dp))
                .background(accent.copy(alpha = 0.85f))
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.label,
                style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium),
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.detail.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.detail,
                    style = MiuixTheme.textStyles.body1.copy(fontSize = 12.sp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.size(10.dp))
        // 「发送」不再是裸文字：给一颗有质感的小胶囊（高光 + 描边 + 按压回弹 + 触感）
        SendPill(
            text = ReminderTestStr.send(),
            tint = if (entry.liveContainer) accent else MiuixTheme.colorScheme.primary,
        )
    }
}

/**
 * 场景行右侧的「发送」标签。
 *
 * 为什么不用真按钮：这一整行都可点（点哪都发送），再嵌一个真按钮会出现两个点击靶，
 * 语义上也重复。所以这里只做**视觉标记**，点击仍由整行承担。
 *
 * ## 为什么是扁平而不是「伪玻璃」
 * 上一版做成了「半透明染色 + 顶部白色高光 + 描边」，想模仿玻璃 —— 真机上效果是
 * **一块能看出来的假材质**（用户原话：伪玻璃效果有点难看），而且它还是版式噪音：
 * 每一行右边都挂一颗，列表就变成了一串玻璃钉。现在回到扁平：一层淡淡的通道色底，
 * 颜色仍表示「这条发去岛还是普通通知」，但不再假装有材质。
 *
 * ⚠ 本页 content 在 DocumentPageScaffold 的采样层里，**真玻璃会自采样闪崩**（gh31 的根因）。
 * 所以这里既不做玻璃，也不做伪玻璃。
 *
 * @param tint 通道色（岛=靛蓝 / 普通通知=主题色）
 */
@Composable
private fun SendPill(text: String, tint: Color) {
    Box(
        modifier = Modifier
            .clip(ContinuousRoundedRectangle(13.dp))
            .background(tint.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.body1.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            color = tint,
            maxLines = 1,
        )
    }
}


/**
 * 「通知通道」卡：把系统里这几条通道的真实状态摊开。
 *
 * 为什么值得单独一张卡：用户点「发送」没反应时，九成是那条通道被静音/关掉了
 * （或者压根还没建出来）。原来页面只说「已发送」，没法继续往下查 —— 这里补上
 * 「系统现在到底认不认这条通道」以及去改的入口。
 *
 * 与页面其它卡一致：miuix Card + 状态点 + 细分隔线，**不用玻璃**
 * （本页内容在 DocumentPageScaffold 的采样层里，画玻璃就是自采样）。
 */
@Composable
private fun ReminderTestChannelsCard(
    channels: List<ReminderTestChannel>,
    onOpenSettings: () -> Unit,
) {
    TestCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp)) {
                Text(
                    text = ReminderTestStr.channelCardTitle(),
                    style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.SemiBold),
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ReminderTestStr.channelCardHint(),
                    style = MiuixTheme.textStyles.body1.copy(fontSize = 12.sp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (channels.isEmpty()) {
                Text(
                    text = ReminderTestStr.channelReadFailed(),
                    style = MiuixTheme.textStyles.body1.copy(fontSize = 12.sp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                )
            } else {
                channels.forEachIndexed { index, channel ->
                    ChannelRow(channel)
                    if (index != channels.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 18.dp, end = 18.dp)
                                .height(0.5.dp)
                                .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)) {
                PressablePill(text = ReminderTestStr.channelOpenSettings(), onClick = onOpenSettings)
            }
        }
    }
}

/** 一条通道：状态点（绿=正常 / 橙=静音 / 红=关掉 / 灰=没建）+ 名字 + 状态词。 */
@Composable
private fun ChannelRow(channel: ReminderTestChannel) {
    val dot = when {
        channel.missing -> MiuixTheme.colorScheme.onSurface.copy(alpha = 0.28f)
        channel.disabled -> ChannelOffColor
        channel.silenced -> ChannelWarnColor
        else -> ChannelOkColor
    }
    val label = when {
        channel.missing -> ReminderTestStr.channelMissing()
        channel.disabled -> ReminderTestStr.channelOff()
        channel.silenced -> ReminderTestStr.channelImportanceBlocked()
        else -> ReminderTestStr.channelOn()
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(ContinuousRoundedRectangle(5.dp))
                .background(dot)
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = channel.name.ifBlank { ReminderTestStr.channelMissing() },
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = label,
            style = MiuixTheme.textStyles.body1.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            color = dot,
            maxLines = 1,
        )
    }
}

/** 空状态：桌面/iOS 没有 Android 的通知通道，给一句说明而不是白屏 */
@Composable
private fun ReminderTestEmptyHint() {
    TestCard {
        Text(
            text = ReminderTestStr.emptyHint(),
            style = MiuixTheme.textStyles.body1.copy(fontSize = 12.sp),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(18.dp),
        )
    }
}

@Composable
private fun ReminderTestFootnote() {
    Text(
        text = ReminderTestStr.footnote(),
        style = MiuixTheme.textStyles.body1.copy(fontSize = 12.sp),
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}