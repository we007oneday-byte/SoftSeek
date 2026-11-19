# SOFTSEEK（SK）需求分析与开发任务书

| 项目 | 内容 |
|---|---|
| 评审对象 | 《DeepSeek 第三方安卓客户端 完整AI开发执行文档 V1.0》 |
| 项目代号 | SOFTSEEK / SK |
| 文档版本 | V1.2（决策补全） |
| 编制日期 | 2026-11-19 |
| 变更记录 | V1.1：D1 保留 SOFTSEEK（接受相似度风险）；D2 首版 Vosk small-cn 42MB + 系统 TTS；D3 自动清理默认关闭 + 手动触发。V1.2：D7 包名按 GitHub owner 定为 `io.github.we007oneday_byte.softseek` |
| 技术基线核对时间 | 2026-11-19（DeepSeek API、Android 17 SDK、Vosk、sherpa-onnx） |
| 评审结论 | **有条件通过；6 个硬缺口已给出修正方案；D1/D2/D3 已确认，可进入阶段 0** |
| 适用读者 | 开发执行人、代码评审人、项目负责人 |

> 本文不是对原需求文档的复述，而是把原文档转成“能开工、能验收、能排期”的工程基线。原文档中的功能设计整体成立，问题集中在**接口契约、依赖坐标、数据模型一致性、系统权限与工具链**五类，均已给出可直接执行的修正方案。

---

## 0. 执行摘要

### 0.1 一句话结论

需求完整度约 90%，产品方向、页面结构、记忆池概念、通话链路都可以直接落地；但原文档的技术基线停留在 2024 年，若“照文档直接写代码”，会在**模型名、接口路径、Token 预算、语音库、附件模型、通话权限**六处出现“编译能过、运行不可用”的问题。建议先做 1～2 天的阶段 0（工具链 + 接口冒烟 + 空壳编译），再进入正式开发。

### 0.2 六个硬缺口

| 编号 | 缺口 | 原文档写法 | 2026-11 核实结果 | 不修正的后果 |
|---|---|---|---|---|
| G1 | 模型名过期 | 默认 `deepseek-chat` | 官方 Chat API 仅接受 `deepseek-flash`、`deepseek-v4-pro` | 首次发消息即 400 invalid model |
| G2 | 接口路径过期 | `v1/chat/completions`、`v1/user/balance` | 官方为 `/chat/completions`、`/user/balance` | 404 或鉴权失败 |
| G3 | Token 预算失配 | 默认 `max_tokens=2048`、上下文 8000 | 官方默认非思考 8K / 思考 64K，最大 384K；上下文 1M | 思考回复被截断，长回答不完整 |
| G4 | 多图无法落库 | 单字段 `attachPath` | 验收要求“多图发送” | 第二条图片覆盖第一条，导出丢图 |
| G5 | 语音链路不可按原样交付 | `org.vosk` + 150MB 高精度 + Piper 1.2.0 | 坐标应为 `com.alphacephei`；中文模型仅 42MB/1.3GB；Piper 无官方 Android AAR | 依赖拉不下来、模型下不到、TTS 无法集成 |
| G6 | 通话权限/贴脸熄屏缺失 | 权限清单无 `MODIFY_AUDIO_SETTINGS`；要求贴脸熄屏 | 通话模式必需该权限；第三方应用无公开“关屏”API | 免提/听筒切换失效；贴脸熄屏只能降级实现 |

### 0.3 工作量与关键路径

- 总工作量：**约 81 人日**（含 P5-06 可选 TTS；若首版不做则 79.5 人日），含 20% 缓冲约 **97 人日**；按每周 5 天计约 **17～19 周**。
- 关键路径：`P0 工具链 → P1 工程骨架 → P2 数据/记忆池 → P3 API/聊天 → P5 通话 → P6 迁移/适配 → P7 发布`。
- 可并行项：P4 设置/多模态可在 P3 后半段并行；P6 的 16KB/edge-to-edge 验证可提前到 P2 之后。
- 最大不确定性：P5 通话（回声消除、打断、贴脸熄屏）与 P6 的 Vosk 16KB 兼容，建议各留 2 天技术验证。

### 0.4 建议的首周动作

1. 决策已回填：D1 保留 SOFTSEEK（接受相似度风险）、D2 首版 Vosk small-cn 42MB + 系统 TTS、D3 自动清理默认关闭；D4～D6 按推荐默认执行；D7 包名按 GitHub owner 定为 `io.github.we007oneday_byte.softseek`。
2. 安装 JDK 21 + Android SDK 36 + Android Studio，修正 `JAVA_HOME`（当前指向 JDK 25）。
3. 用真实 API Key 跑通 `/models`、`/user/balance`、`/chat/completions`（流式 + 非流式 + 图片）四组冒烟，样本存入 `docs/api-samples/`。
4. 生成空壳工程并跑通 `assembleDebug`，再进入 P1。

---

## 1. 需求解构

### 1.1 七条禁令的工程化落地

| 禁令 | 工程约束 | 可验证方式 |
|---|---|---|
| 禁止 WebView | 不引入 `androidx.webkit`、不写 `WebView`；Markdown 用 Compose 原生渲染 | CI 全文检索 `WebView`/`webkit`；依赖树审查 |
| 禁止服务端 | 无自建后端模块、无中转域名；仅直连用户配置的 provider | 代码审查 + 抓包确认请求域名 |
| 禁止收费 | 无 Billing、广告、会员、付费解锁 SDK | 依赖清单审查；Manifest 无 Billing 权限 |
| 禁止数据上传 | 无统计/埋点/崩溃上报 SDK；聊天/配置只落本地 | 依赖审查；隐私测试（断网可用） |
| 禁止侵权设计 | 色值、图标、文案、动效差异化；显著“非官方”声明 | 视觉评审 + 法务确认 |
| 禁止滥用权限 | 仅声明必要权限；存储走 SAF；不申请读写存储 | Manifest 白名单 + `lint` 检查 |
| 禁止 Java | 全 Kotlin；无 `.java` 源文件 | `glob **/*.java` 为空；ktlint/detekt |

**补充红线**：不申请 `MANAGE_EXTERNAL_STORAGE`、`READ_EXTERNAL_STORAGE`、`WRITE_EXTERNAL_STORAGE`；不集成广告 ID；不做账号体系。

### 1.2 功能矩阵与优先级

优先级定义：**P0 = 首版必须可用；P1 = 首版应有，允许延后一个小版本；P2 = 后续迭代**。

| 模块 | 功能点 | 优先级 | 依赖/备注 |
|---|---|---|---|
| 基础聊天 | 流式打字机输出 | P0 | SSE 解析器 |
| 基础聊天 | Markdown 渲染 | P0 | 需先做渲染库 spike |
| 基础聊天 | 代码块高亮 | P0 | 渲染库可能需自实现高亮 |
| 基础聊天 | 重新生成 | P0 | 需定义“替换/分支”语义 |
| 基础聊天 | 消息编辑重发 | P1 | 需截断后续消息 |
| 基础聊天 | 删除单条 | P1 | 保持上下文合法 |
| 基础聊天 | 复制/分享文本 | P1 | ACTION_SEND |
| 深度思考 | 四档：none/low/high/max | P0 | 与官方 `reasoning_effort` 对齐 |
| 深度思考 | 思考过程折叠 | P0 | 流式更新 |
| 深度思考 | Token 消耗明细 | P0 | 解析末块 `usage` |
| 多模态 | 拍照/相册选图 | P0 | 系统相机/PhotoPicker，无需 CAMERA 权限 |
| 多模态 | 自动压缩 | P0 | 长边 1568、质量 85、WebP/JPEG |
| 多模态 | 多轮图文对话 | P0 | content 数组 |
| 多模态 | 多图发送 | P1 | 需附件子表 |
| 多模态 | 图片本地缓存/预览 | P0 | 私有目录 + Coil |
| 记忆池 | UUID 池块/堆点 | P0 | Room |
| 记忆池 | 清理（默认关闭，可手动触发） | P1 | 见 9-D3；设置页提供“立即清理”入口 |
| 记忆池 | 回收站/恢复 | P1 | 需 `deletedAt` |
| 记忆池 | 置顶/重命名 | P1 | 列表排序 |
| 记忆池 | 标签二次编组 | P2 | 首版可只存不用 |
| 通话 | 离线 ASR | P0 | Vosk small-cn 42MB |
| 通话 | 本地 TTS | P0 | 系统 TTS 默认 |
| 通话 | 可打断 | P0 | AEC + VAD + 清空队列 |
| 通话 | 通话音频模式 | P0 | 需 `MODIFY_AUDIO_SETTINGS` |
| 通话 | 自定义背景 | P1 | SAF 选图 |
| 通话 | 自定义音色/语速 | P1 | 系统 TTS 音色列表 |
| 通话 | 贴脸熄屏 | P2 | 无公开 API，需降级 |
| 多模型 | DeepSeek 全模型 | P0 | `/models` 动态发现 |
| 多模型 | 多服务商兼容 | P1 | 能力矩阵抽象 |
| 多模型 | 对话内切换模型/档位 | P0 | 不改池块 ID |
| 配置 | 深浅/跟随系统 | P0 | 主题系统 |
| 配置 | 参数预设 | P1 | 导出/导入 |
| 配置 | 角色提示词模板 | P1 | 对话级 system prompt |
| 配置 | 记忆池规则自定义 | P1 | 配置表 |
| 数据迁移 | 导出 JSON/Markdown/TXT | P0 | SAF 直写 |
| 数据迁移 | 导入官方导出文件 | P0（需样本） | 无样本则降为 P1 |
| 数据迁移 | 批量/全量导出 | P1 | 多选 |
| 系统适配 | Android 11～17 | P0 | 16KB、edge-to-edge 重点 |
| 系统适配 | 权限动态申请 | P0 | 通知/录音 |
| 系统适配 | 前台服务保活 | P0 | 通话专用 |

### 1.3 用户关键路径

1. **首次启动**：阅读非官方声明与开源协议 → 跳转 DeepSeek 开放平台 → 粘贴 API Key → 校验（余额接口）→ 进入主界面。
2. **首次聊天**：新建池块 → 选模型/档位 → 输入文字或图片 → 流式输出 → 思考过程折叠 → Token 明细。
3. **日常使用**：抽屉切换会话 → 重命名/置顶 → 编辑重发/重新生成 → 导出单条会话。
4. **通话**：聊天页点通话 → 权限申请 → 自动降档为 low → 语音对话 → 打断 → 挂断归档。
5. **迁移**：设置页导出全量 ZIP/JSON → 新设备导入 → 校验消息数、图片、Token。

### 1.4 非功能需求

| 维度 | 目标 | 验证方式 |
|---|---|---|
| 冷启动 | ≤ 2s（中端机） | Macrobenchmark `StartupTimingMetric` |
| 列表性能 | 1000 条消息 ≥ 55fps | Macrobenchmark + JankStats |
| 通话耗电 | 连续 1 小时 ≤ 10% | Battery Historian / 真机 |
| 包体 | 基础 APK ≤ 35MB（不含语音模型） | APK Analyzer |
| 兼容 | Android 11～17；16KB 页设备可运行 | 模拟器矩阵 + 真机 |
| 隐私 | 断网可查看历史；无遥测 | 抓包 + 依赖审查 |
| 可访问性 | 触控目标 ≥ 48dp；内容描述完整 | Accessibility Scanner |
| 可维护性 | 单文件 ≤ 200 行；核心逻辑有单测 | detekt + 覆盖率 |

---

## 2. 契约校准（勘误表）

### 2.1 DeepSeek API 契约（2026-11 官方口径）

| # | 原文档位置/写法 | 核实结果 | 影响 | 处置 |
|---|---|---|---|---|
| A1 | §6.2.1 Base URL `https://api.deepseek.com` + `v1/chat/completions` | 官方端点为 `https://api.deepseek.com/chat/completions`；`/v1` 前缀不是 2026 文档写法 | 可能 404 | Base URL 不带 `/v1`，Retrofit 路径写 `chat/completions`；保留冒烟验证 |
| A2 | §6.2.1/§6.2.2 模型 `deepseek-chat` | 官方 Chat API 仅接受 `deepseek-flash`（文本+图片，1M 上下文，最大输出 393216）与 `deepseek-v4-pro`（纯文本） | 400 invalid model | 默认 `deepseek-flash`；模型列表改为 `/models` 动态获取；旧数据做别名映射 |
| A3 | §6.2.5 `GET v1/user/balance` | 官方为 `GET https://api.deepseek.com/user/balance`，返回 `is_available` + `balance_infos[]`（currency/total_balance/granted_balance/topped_up_balance） | 404/解析失败 | 改路径；DTO 按官方字段建模；不要假定只有 CNY |
| A4 | §6.2.2 档位表 none/low/high/max | 正确；`medium`/`xhigh` 会映射为 `high`；默认 `high`；`thinking:{type}` 优先于 `reasoning_effort` | 低风险 | 枚举锁定 4 值；只发 `reasoning_effort` 或 `thinking` 之一，禁止同时发冲突值 |
| A5 | §6.2.2 “仅推理模型支持，普通模型忽略” | 两个模型都带 `effort` 元数据；应以 `/models` 的 `supported_levels` 为准 | UI 档位可能灰错 | 不硬编码；能力缓存 + 默认兜底 |
| A6 | §6.1.1 `maxTokens=2048` | 官方默认：非思考 8K、思考 64K、`max` 档 128K；`max_tokens` 上限 393216 | 回复被截断 | 默认聊天 8K、思考 32K；上限读模型元数据；UI 显示费用预警 |
| A7 | §5.2.3 `maxContextTokens=8000` | 模型上下文 1,048,576；8000 作为“输入预算”可接受，但与 64K 输出相加仍远小于上限 | 语义混淆 | 改名为 `maxInputTokens`，默认 32K；校验 `输入 + max_tokens ≤ context_window` |
| A8 | §6.2.3 SSE 解析只取 content/reasoning | 官方在最后一个内容块携带 `usage`（含 prompt/completion/total/reasoning tokens、缓存命中） | Token 明细为空 | 每个 chunk 都尝试解析 `usage`；结束时落库 |
| A9 | §6.2.3 只处理 `data:` 行，遇 `[DONE]` 直接 break | 还需处理空行/注释、非 200 的 JSON error、`finish_reason` 的 `length/content_filter/tool_calls/insufficient_system_resource/aborted`、IO 异常 | 断流后 UI 卡死/错误提示不准 | 独立 SSE 状态机；区分“正常停止/长度截断/资源不足/用户中断/网络失败” |
| A10 | §6.2.2 响应中 `reasoning_content` | 该字段仅在“对话前缀续写（prefix=true）”时作为输入回传；普通历史不得回传 | 400 或上下文污染 | DB 保存思考内容；拼装上下文时剥离；导出时保留 |
| A11 | §6.2.4 多模态模型与图片 | `deepseek-flash` 支持 `text`+`image`；JPEG/PNG/GIF/WebP；base64 data URL/URL/Files API；单图最多 384 tokens；请求体 ≤48MiB；URL 图片 ≤32MiB | 图片过大/格式不支持 | 长边 1568、质量 85；`detail=auto`；单条最多 8 图；发送前校验体积 |
| A12 | §6.2.2 思考模式 `temperature=0.7` | 官方明确“思考模式下 temperature 不生效” | 用户调了没效果 | 档位 ≠ none 时禁用温度滑块并提示 |
| A13 | §6.2.1 “兼容 OpenAI Chat Completions 标准” | 协议兼容，但模型名、思考参数、余额接口均为 DeepSeek 扩展 | 直接透传会失败 | provider adapter 做字段映射，不做“一套 DTO 打天下” |
| A14 | §6.2.3 流式请求超时 | 流式读取不能用普通 read timeout | 长回答被 OkHttp 超时切断 | 流式 `readTimeout=0`；非流式 60s；连接 15s |

**修正后的最小请求基线**

```http
POST https://api.deepseek.com/chat/completions
Authorization: Bearer <API_KEY>
Content-Type: application/json

{
  "model": "deepseek-flash",
  "messages": [
    {"role": "system", "content": "你是……"},
    {"role": "user", "content": "你好"}
  ],
  "reasoning_effort": "high",
  "max_tokens": 32768,
  "stream": true,
  "stream_options": {"include_usage": true}
}
```

**流式必须解析的字段**

| 字段 | 用途 |
|---|---|
| `choices[0].delta.content` | 正文增量 |
| `choices[0].delta.reasoning_content` | 思考增量 |
| `choices[0].finish_reason` | 结束原因（stop/length/content_filter/insufficient_system_resource/aborted） |
| `usage.prompt_tokens` / `completion_tokens` / `total_tokens` | Token 明细 |
| `usage.prompt_cache_hit_tokens` / `prompt_cache_miss_tokens` | 缓存命中（可选展示） |
| `usage.completion_tokens_details.reasoning_tokens` | 思考 token（若返回） |
| `data: [DONE]` | 流结束 |

### 2.2 依赖与平台契约

| # | 原文档位置/写法 | 核实结果 | 影响 | 处置 |
|---|---|---|---|---|
| B1 | §2.2 `org.vosk:vosk-android:0.3.45` | 官方坐标 `com.alphacephei:vosk-android`；Maven Central 最新 0.3.47（2023-03 构建） | 依赖解析失败 | 改为 `com.alphacephei:vosk-android:0.3.47`；16KB 单独验证 |
| B2 | §7.2.1 中文模型 40MB/150MB | 官方仅 `vosk-model-small-cn-0.22`（42MB）与 `vosk-model-cn-0.22`（1.3GB） | “高精度 150MB”无法交付 | 基础版用 42MB；高精度改 sherpa-onnx/SenseVoice，或砍掉该档 |
| B3 | §2.2/§7.3 Piper TTS 1.2.0 | 无官方 Android AAR；需自编译 JNI；sherpa-onnx（Apache-2.0）官方支持 Android TTS | 集成成本高、16KB 风险 | 默认系统 TTS；进阶改 sherpa-onnx；首版不做 Piper |
| B4 | §2.1 `compose-markdown 0.4.0` 与 §3.2 `compose-richtext 0.20.0` | 两套 Markdown 方案冲突；richtext 无内置代码高亮；需验证 Kotlin 2.0 兼容 | 返工/高亮缺失 | 只保留一套；阶段 3 做 1 天 spike，失败则自研轻量渲染 |
| B5 | §2.1 `accompanist-swiperefresh 0.34.0` | 已废弃；Material3 1.3 提供 `PullToRefreshBox` | 使用废弃 API | 移除 swiperefresh，改用官方组件 |
| B6 | §3.2 Hilt 2.50 + kapt + Kotlin 2.0.20 | Dagger 2.50 早于 Kotlin 2.0，KAPT/KSP 元数据存在风险 | 编译失败/增量慢 | Hilt 2.51+（建议 2.52）+ KSP；Room 2.6.1 同样走 KSP |
| B7 | §3.2 Retrofit + kotlinx.serialization | 缺 converter 依赖 | 运行时无法反序列化 | 加 `retrofit2-kotlinx-serialization-converter`；或改用 Moshi |
| B8 | §3.2 Kotlin 2.0.20 + Compose | Kotlin 2.0 起必须显式应用 Compose Compiler 插件 | 编译报错 | 加 `org.jetbrains.kotlin.plugin.compose` |
| B9 | §2.3 `compileSdk=37` | Android 17 为预览版（Cinnamon Bun Preview），需 AGP 8.9.0-rc01+、Studio Meerkat 2024.3.1+ | 稳定工具链不认识 37 | 发布用 `compileSdk=36`；37 仅做兼容验证流水线 |
| B10 | §3.2 `androidx.security:security-crypto:1.1.0` | 1.1.0 长期 alpha；维护状态需复核 | 依赖停更风险 | 直接用 Android Keystore + AES/GCM 自实现；生物识别用 `androidx.biometric` |
| B11 | §2.1 Material3 1.2.0 + BOM 2024.10.01 | BOM 2024.10.01 实际映射 material3 1.3.0 | 版本冲突 | 以 BOM 为准，不单独锁 1.2.0 |
| B12 | §9.3 权限清单 | 缺 `MODIFY_AUDIO_SETTINGS`（`setMode`/`setSpeakerphoneOn` 必需）；蓝牙耳机还需 `BLUETOOTH_CONNECT`（31+） | 免提/听筒切换静默失败 | 补权限；蓝牙按需声明，不用则不申请 |
| B13 | §7.6.2 接近传感器贴脸熄屏 | 第三方应用无公开“关屏”API；`PROXIMITY_SCREEN_OFF_WORKAROUND` 属隐藏/系统能力，反射受限制 | 功能不可用 | 降级：切听筒 + 距离传感器调暗 + 超时熄屏；真机验证 |
| B14 | §4.3.1 快捷面板“文件上传” | Chat API 的 `file` 块仅用于图片（Files API）；PDF/Word 不能直接传 | 功能承诺无法兑现 | 首版只做图片；纯文本文件读取为文本注入；PDF 解析列为 P2 |
| B15 | §4.3.1 “拍照” | 若用系统相机 contract，无需 CAMERA 权限；自建相机则需 CAMERA | 权限清单与实现不一致 | 首版用 `ActivityResultContracts.TakePicture`，不申请 CAMERA |
| B16 | §3.2 无 WorkManager | 自动清理/回收站清理需要可靠后台调度 | 冷启动触发不够 | 加 `androidx.work:work-runtime-ktx` + Hilt 集成 |
| B17 | §9.5 “长对话生成用前台服务” | Android 14+ 前台服务类型严格；聊天流式不属于 microphone/dataSync 合理场景 | 上架/系统限制风险 | 仅通话使用 `microphone` 类型 FGS；聊天不做 FGS，退后台 5 分钟断开 |
| B18 | §3.2 无 Paging | 千条消息一次性加载会卡 | 性能不达标 | 首版手动分页（最近 50 条 + 上拉加载）；或 Paging 3 |

### 2.3 数据模型一致性修订

| 表/字段 | 修订 | 原因 |
|---|---|---|
| `memory_pool_blocks` | 增加 `deletedAt: Long?` | 回收站 7 天需要删除时间，不能用 `createdAt` 推算 |
| `memory_pool_blocks` | 增加 `lastMessageAt: Long?` | 列表排序/预览更准确，避免 `updatedAt` 被设置修改污染 |
| `memory_heap_nodes` | 增加 `reasoningTokens: Int` | Token 明细需要区分思考 token |
| `memory_heap_nodes` | 增加 `modelName: String?` | 同一会话切换模型后，导出/复盘需要每条消息的模型快照 |
| `memory_heap_nodes` | 增加 `errorMessage: String?` | 失败消息可重试/展示原因 |
| 新增 `message_attachments` | `nodeId`、`type`、`localPath`、`mime`、`width`、`height`、`sizeBytes`、`sortOrder` | 支持多图/未来文件；替代单字段 `attachPath` |
| `memory_pool_config` | `maxContextTokens` → `maxInputTokens`；增加 `deletedRetentionDays`、`tokenEstimateRatio`、`callAutoDowngrade`、`imageDetail`；`enableAutoCleanup` 默认 `false`（D3），开启时阈值默认 2 条、保护期 24h | 语义清晰、可配置；清理需显式开启 |
| 索引 | `(poolId, timestamp)`、`(isDeleted, updatedAt)`、`(isPinned, updatedAt)` | 列表/清理/恢复查询性能 |
| `tags` | 由逗号分隔改为 JSON 数组（TypeConverter） | 标签含逗号会解析错 |
| 数据库 | `exportSchema = true`，schema 目录纳入版本控制 | 升级/迁移可审查 |

**附件表建议**

```kotlin
@Entity(
    tableName = "message_attachments",
    foreignKeys = [ForeignKey(
        entity = MemoryHeapNodeEntity::class,
        parentColumns = ["nodeId"],
        childColumns = ["nodeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("nodeId")]
)
data class MessageAttachmentEntity(
    @PrimaryKey val attachmentId: String,
    val nodeId: String,
    val type: String,          // image / file
    val localPath: String,
    val mime: String,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0,
    val sortOrder: Int = 0
)
```

### 2.4 多服务商能力抽象

原文档的 `AiChatService` 接口方向正确，但应把“能力”显式化，避免 UI 对每家硬编码。

| 能力字段 | 用途 |
|---|---|
| `supportsVision` | 是否显示图片按钮/是否允许发图 |
| `effortLevels` | 档位选择器的可选值（DeepSeek 为 none/low/high/max） |
| `defaultEffort` | 默认档位 |
| `contextWindow` / `maxOutputTokens` | Token 预算与费用预警 |
| `supportsBalance` | 是否显示余额卡片（DeepSeek 有，其他家可能没有） |
| `supportsModelList` | 是否走 `/models` 动态发现 |
| `authStyle` | Bearer / api-key header 等 |
| `streamUsage` | 是否在流末返回 usage |

Provider 实现只负责“请求构造 + 响应映射 + 错误码翻译”，UI 只读能力字段。

### 2.5 语音链路选型评估

| 方案 | 组成 | 优点 | 缺点/风险 | 建议 |
|---|---|---|---|---|
| A：原文档 | Vosk 0.3.47 + 系统 TTS + 可选 Piper | 与文档一致；Vosk 离线成熟 | 坐标要改；中文仅 42MB/1.3GB；Piper 无 Android 官方库；Vosk 2023 年构建，16KB 未验证 | 不采用（D2 已确认） |
| B：首版稳妥 | Vosk small-cn 42MB + 系统 TTS | 包体小、零配置、可快速验收；避开 Piper | 识别准确率一般；长句/口音体验有限 | **已确认：首版基线（D2）** |
| C：统一引擎 | sherpa-onnx（ASR+TTS，Apache-2.0） | 一套 native 库；中文模型更丰富；TTS 音质更好；维护活跃 | 模型 200MB+；集成/调参成本；需 16KB 验证 | 已确认：P1 可选，不阻塞首版 |

> D2 已确认：首版只集成 Vosk small-cn 42MB + 系统 TTS；sherpa-onnx 作为 P1 可选升级，首版验收不包含 P5-06。

**通话链路必须补的三个技术点**

1. **回声消除（AEC）**：TTS 播放会被麦克风再次识别，造成“AI 自己打断自己”。使用 `AudioSource.VOICE_COMMUNICATION` + `AcousticEchoCanceler`/`NoiseSuppressor`（设备支持时），不支持则降级为半双工（播报时暂停 ASR）。
2. **音频焦点**：来电/其他媒体抢焦点时暂停 ASR/TTS，恢复后重建 AudioRecord。
3. **打断优先级**：VAD 检测到人声 → 立即 `tts.stop()` + 清空句子队列 + 取消当前流式请求（保留已生成文本）。

### 2.6 合规与品牌评审

| 项目 | 现状 | 风险 | 建议 |
|---|---|---|---|
| 应用名 SOFTSEEK | 与 DeepSeek 仅首字母差异，尾部同为 SEEK | 中高：已由项目负责人确认接受（D1） | 保留名称；所有物料统一加“非官方 DeepSeek 客户端”副标；图标/色值继续差异化；上架前做一次法务复核 |
| 包名 `com.custom.aiclient` | 过于通用，易冲突 | 低（已定） | **定为 `io.github.we007oneday_byte.softseek`**（GitHub owner `we007oneday-byte`，连字符按 Java 规则换成下划线）；如不想暴露账号可退回 `com.softseek.app` |
| 主色 `#5B6CFF` | 与 DeepSeek 官方蓝 `#4D6BFE` 的 RGB 差仅 (14,1,1) | 高：未达文档自述“偏移 15%” | 改为 `#6C5CE7`/`#7A5AF8` 等偏紫主色，并校验深色对比度 |
| 图标 | 蓝紫渐变 SK 胶囊 | 中：品牌调性与官方接近 | 保留 SK 图形，降低渐变饱和度；启动图标用简化版，文字 LOGO 只用于 GitHub/关于页 |
| 官方商标 | 文档/关于页出现 “DeepSeek” | 合理使用范围需控制 | 仅作“兼容/直连”事实描述，不使用官方 Logo、不模仿官方登录页 |
| 隐私声明 | 未成文 | 中 | 明确：Key 本地加密、聊天本地存储、图片会发送给用户所选 provider、无遥测 |
| 开源协议 | MIT | 低 | 生成第三方许可证清单；Vosk/sherpa 均为 Apache-2.0，需保留 NOTICE |

---

## 3. 风险登记册

| ID | 风险 | 概率 | 影响 | 等级 | 触发信号 | 对策 | 归属 |
|---|---|---|---|---|---|---|---|
| R1 | 模型/接口口径过期 | 已发生 | 首条消息失败 | 高 | 400/404 | 阶段 0 冒烟；`/models` 动态发现 | P0/P3 |
| R2 | `max_tokens` 过小导致截断 | 高 | 回答不完整 | 高 | `finish_reason=length` | 默认 8K/32K；UI 费用提示 | P3 |
| R3 | SSE 边界处理不全 | 中高 | 卡死/重复/丢字 | 高 | 断流、`aborted` | 状态机 + 真流样本测试 | P3 |
| R4 | 多图 schema 不兼容 | 已发生 | 数据覆盖/导出丢图 | 高 | 第二条图覆盖 | 附件子表 + 迁移 | P2 |
| R5 | Vosk 16KB 对齐 | 中高 | Android 16 设备闪退/兼容弹窗 | 高 | 16KB 模拟器崩溃 | 实测；必要时换 sherpa 或自编译 | P0/P6 |
| R6 | 中文识别准确率不足 | 中 | 通话可用性差 | 中高 | 用户重复说 | small 模型先验收；P1 上高精度 | P5 |
| R7 | Piper 不采用（D2） | 已规避 | 无 | 低 | — | 首版系统 TTS；P1 可选 sherpa | P5 |
| R8 | 通话回声自打断 | 高 | 对话不可用 | 高 | AI 复读用户话 | AEC + 半双工降级 | P5 |
| R9 | 贴脸熄屏不可实现 | 中高 | 体验缺失 | 中 | 隐藏 API 被拒 | 降级为调暗/超时熄屏 | P5 |
| R10 | 清理误删短会话 | 低（默认关闭） | 用户数据“消失” | 中 | 用户手动开启且阈值过低 | 默认关闭；手动清理前预览确认；回收站 7 天 | P2 |
| R11 | 1M 上下文/64K 输出费用失控 | 中 | 用户账单飙升 | 中高 | 单次费用过高 | 输入预算 + 输出上限 + 预警 | P3/P4 |
| R12 | Markdown 库不兼容 Kotlin 2.0 | 中 | 返工 | 中 | 编译/渲染异常 | 1 天 spike；备选自研 | P3 |
| R13 | 导入官方格式未知 | 中 | 迁移功能延期 | 中 | 无样本 | 先要样本；adapter 化 | P6 |
| R14 | 本机无 SDK/Studio/Git，JDK 25 | 已发生 | 无法编译 | 高 | `java -version` 25 | 装 JDK 21 + SDK 36 + Git | P0 |
| R15 | compileSdk 37 预览不稳定 | 中 | CI 失败 | 中 | AGP 不识别 37 | 36 发布，37 独立验证 | P0/P6 |

---

## 4. 架构决策（ADR）

| ADR | 决策 | 理由 | 代价 |
|---|---|---|---|
| ADR-01 | 单 `:app` 模块 + 包分层，首版不做多模块 | 降低初期构建/依赖复杂度；包结构已按 Clean Architecture 分层 | 后期拆模块需迁移 |
| ADR-02 | MVVM + UseCase + Repository，Hilt 注入 | 与文档一致；可测试 | 样板代码多 |
| ADR-03 | Retrofit + OkHttp + kotlinx.serialization | 类型安全、与 Kotlin 一致 | 需 converter 依赖 |
| ADR-04 | SSE 自研解析器（OkHttp `ResponseBody.source()`） | 需要处理 usage/中断/错误等 DeepSeek 细节；通用 EventSource 库不覆盖 | 需单测覆盖 |
| ADR-05 | Room 单库，`exportSchema=true` | 记忆池关系强；迁移可审查 | 需维护 schema |
| ADR-06 | 文件一律应用私有目录；导入导出走 SAF | 满足“不申请存储权限” | 用户换机需手动导出 |
| ADR-07 | API Key：Keystore AES/GCM + 可选 BiometricPrompt | 不依赖长期 alpha 的 security-crypto | 自实现加解密/IV 管理 |
| ADR-08 | 语音：`AsrEngine`/`TtsEngine` 接口 + 可替换实现 | 便于 Vosk→sherpa 替换 | 多一层抽象 |
| ADR-09 | 通话状态机独立于 ChatViewModel | 通话有 ASR/TTS/打断/音频焦点多状态 | 需清晰状态图 |
| ADR-10 | 导出格式版本化（`format_version`）+ ZIP 携带图片 | 可重导入、可迁移 | 解析器需兼容旧版 |

**通话状态机（建议）**

```text
Idle → Preparing(权限/引擎/音频) → Listening
Listening → Recognizing(partial) → Thinking(请求中)
Thinking → Speaking(TTS 队列)
Speaking → Interrupted(VAD 命中) → Listening
任意状态 → Ending(挂断/错误) → Idle
```

---

## 5. 开发任务分解（WBS）

### 5.0 估算口径与完成定义（DoD）

- 人日口径：熟悉 Compose 的 1 名开发者；不含模型下载等待、商店审核、法务。
- 每个任务的 DoD：**代码 + 至少 1 条测试/验证记录 + 文档更新 + 评审通过**。
- 阶段 DoD：阶段验收项全部有证据（截图/日志/测试报告），且不引入新的 P0 缺陷。

### 5.1 阶段 0：工具链与契约基线（4.5 人日）

| ID | 任务 | 关键产出 | 依赖 | 人日 | 验收 |
|---|---|---|---|---|---|
| P0-01 | 安装 JDK 21，修正 `JAVA_HOME` | `java -version` = 21 | 无 | 0.5 | 当前 JDK 25 不再作为 Gradle JDK |
| P0-02 | 安装 Android SDK 36 / Build-Tools / Platform-Tools / 模拟器 | `ANDROID_HOME` 可用 | P0-01 | 1.0 | `sdkmanager --list` 正常 |
| P0-03 | 安装 Git，初始化仓库与基础文件 | `.gitignore`、`LICENSE`(MIT)、`.editorconfig`、README | 无 | 0.5 | 首次提交完成 |
| P0-04 | Gradle 基线：AGP 8.9+、Gradle 8.11+、Kotlin 2.0.20、Compose 插件、KSP、version catalog | `libs.versions.toml`、空壳工程 | P0-02 | 1.0 | `assembleDebug` 通过 |
| P0-05 | DeepSeek 接口冒烟（models/balance/chat/stream/vision） | `docs/api-samples/*.json`、问题记录 | 用户 API Key | 1.0 | 5 组请求均有可解析响应 |
| P0-06 | 设备矩阵：API 30/33/36 模拟器 + 16KB 镜像 + 真机 | `adb devices` 清单 | P0-02 | 0.5 | 空壳 APK 可安装 |

### 5.2 阶段 1：工程基础（6 人日）

| ID | 任务 | 关键产出 | 依赖 | 人日 | 验收 |
|---|---|---|---|---|---|
| P1-01 | 包结构与基础类 | `io.github.we007oneday_byte.softseek` 包树、`ChatApp`、`MainActivity`（包名发布前冻结） | P0-04 | 0.5 | 目录符合 §3.1 |
| P1-02 | Hilt 初始化 + Application | `@HiltAndroidApp`、DI 模块 | P1-01 | 0.5 | 注入可用 |
| P1-03 | 主题系统 | `Color.kt`/`Type.kt`/`Theme.kt`，三模式 | P1-01 | 1.0 | 深浅/跟随系统切换正常 |
| P1-04 | 导航框架 | `AppNavGraph`、路由常量、返回栈 | P1-03 | 0.5 | 5 个页面可达 |
| P1-05 | 通用组件 | `SettingItem` 三类型、`CommonButton`、`MarkdownText` 占位 | P1-03 | 1.0 | Preview 可见 |
| P1-06 | 聊天壳 + 侧滑抽屉 | TopAppBar、抽屉、新建按钮、设置入口 | P1-04 | 1.0 | 开合/跳转正常 |
| P1-07 | 启动页 + 自适应图标 | SplashScreen、mipmap、monochrome | P1-03 | 0.5 | Android 13 主题图标正常 |
| P1-08 | 字符串/尺寸资源化 | `strings.xml`、`dimens.xml`、无硬编码 | P1-03 | 0.5 | 切换字体比例不截断 |
| P1-09 | 质量基线 | ktlint/detekt、lint 配置、CI 脚本 | P1-01 | 0.5 | 0 阻断问题 |

### 5.3 阶段 2：数据库与记忆池（9 人日）

| ID | 任务 | 关键产出 | 依赖 | 人日 | 验收 |
|---|---|---|---|---|---|
| P2-01 | 实体/索引/外键（按 §2.3 修订） | Entity 4 张表 | P1-02 | 1.0 | schema 导出成功 |
| P2-02 | DAO | 块/节点/附件/配置 DAO | P2-01 | 1.0 | 增删改查单测 |
| P2-03 | 数据库与类型转换器 | `AppDatabase`、tags/枚举转换 | P2-02 | 0.5 | 冷启动建库 |
| P2-04 | 仓储接口/实现 | `ConversationRepository`、`MemoryPoolRepository` | P2-02 | 1.0 | 领域层不依赖 Room |
| P2-05 | 池块生命周期 | 新建/重命名/置顶/软删/恢复 | P2-04 | 1.0 | 状态流转正确 |
| P2-06 | 清理引擎（默认关闭）+ 手动触发 | 规则过滤（阈值默认 2 条、保护期 24h、置顶排除）；提供 `cleanupNow(preview)` 接口 | P2-05 | 1.0 | 默认不自动删；手动调用可预览/取消 |
| P2-07 | 回收站清理 | 7 天到期永久删除 Worker | P2-06 | 0.5 | `deletedAt` 生效 |
| P2-08 | 上下文拼装 | 系统提示、倒序截断、剥离 reasoning、图片 token | P2-04 | 1.5 | 截断不越界 |
| P2-09 | Token 估算与校准 | 字符/图片估算 + usage 反馈修正 | P2-08 | 0.5 | 误差 ≤ 20% |
| P2-10 | 数据层测试 | Room in-memory + 迁移测试 | P2-03 | 1.0 | 覆盖率 ≥ 70% |

### 5.4 阶段 3：API 接入与基础聊天（15 人日）

| ID | 任务 | 关键产出 | 依赖 | 人日 | 验收 |
|---|---|---|---|---|---|
| P3-01 | Provider 抽象 + 能力模型 | `AiProvider`、`AiChatService`、capability | P2-04 | 1.0 | DeepSeek 实现可注入 |
| P3-02 | DeepSeek DTO | 请求/响应/chunk/error/balance/model | P0-05 | 1.0 | 官方样本反序列化通过 |
| P3-03 | 网络层 | Retrofit/OkHttp、Auth、超时、代理、BaseURL 校验 | P3-02 | 1.0 | 401/402/429/5xx 映射 |
| P3-04 | SSE 解析器 | `Flow<ChatStreamEvent>` + finish/usage/中断 | P3-03 | 1.5 | 回放样本无丢字 |
| P3-05 | 发送/重发/重新生成/停止 | `ChatRepository` + 缓冲落库 | P3-04 | 1.5 | 中断后 partial 保留 |
| P3-06 | ChatViewModel 状态机 | `ChatUiState` 密封类、事件 | P3-05 | 1.0 | 无业务逻辑在 UI |
| P3-07 | 消息列表 | LazyColumn、自动滚动、上拉历史 | P3-06 | 1.0 | 1000 条滑动可用 |
| P3-08 | 气泡组件 | User/Ai/ThinkingBlock | P3-06 | 1.0 | 折叠/展开/流式更新 |
| P3-09 | 输入栏 | 多行、发送态、IME insets | P3-07 | 0.5 | 键盘不遮挡 |
| P3-10 | 消息操作菜单 | 复制/重新生成/编辑/删除/分享 | P3-07 | 1.0 | 操作后数据一致 |
| P3-11 | 会话抽屉 | 列表、置顶、重命名、删除、导出入口 | P3-07 | 1.0 | 排序/切换正确 |
| P3-12 | 标题生成 | 本地截断 + 可选 AI 标题 | P3-11 | 0.5 | 不额外阻塞发送 |
| P3-13 | Markdown + 高亮 | 渲染器 spike、代码块、流式节流 | P3-07 | 1.5 | 30fps 以上不卡顿 |
| P3-14 | Token/错误展示 | 明细、重试、费用提示 | P3-04 | 0.5 | 与 usage 一致 |
| P3-15 | 聊天测试 | 解析器/VM/UI 测试 | P3-14 | 1.0 | 关键路径覆盖 |

### 5.5 阶段 4：多模态、模型与设置（14 人日）

| ID | 任务 | 关键产出 | 依赖 | 人日 | 验收 |
|---|---|---|---|---|---|
| P4-01 | 图片选择 | PhotoPicker + SAF 兜底、多选 | P3-07 | 0.5 | Android 11～16 可用 |
| P4-02 | 压缩与缓存 | 长边 1568、质量 85、EXIF、缓存上限 | P4-01 | 1.0 | 大图不 OOM |
| P4-03 | 附件落库/渲染/预览 | 附件表、Coil、全屏预览 | P2-01 | 1.0 | 多图顺序正确 |
| P4-04 | 视觉请求构造 | content 数组、detail、体积校验 | P3-02 | 1.0 | 与官方样本一致 |
| P4-05 | 模型/档位选择 | `/models` 缓存、能力灰显 | P3-01 | 1.0 | 思考模式禁温度 |
| P4-06 | 参数预设 | 保存/应用/导出 | P4-05 | 1.0 | 重启保留 |
| P4-07 | DataStore 设置体系 | 8 组设置、默认值（`enableAutoCleanup=false`）、迁移 | P1-02 | 1.5 | 即时生效；重启保留 |
| P4-08 | API Key 管理 | Keystore AES/GCM、多 Key、备注、切换、生物识别 | P1-02 | 2.0 | 明文不落盘 |
| P4-09 | Key 校验 | 余额（DeepSeek）/模型列表（其他） | P4-08 | 0.5 | 错误可读 |
| P4-10 | 余额/用量 | 余额卡片、日/月统计、预警阈值 | P3-02 | 1.0 | 与账单口径一致 |
| P4-11 | 设置页 8 组 | API/模型/聊天/通话/记忆/外观/数据/关于；记忆组含清理开关（默认关）、“立即清理”预览/确认 | P4-07 | 1.5 | 全部项可配置；手动清理可取消 |
| P4-12 | 高级网络设置 | BaseURL/代理/超时 | P3-03 | 0.5 | 非 HTTPS 拒绝 |
| P4-13 | 主题细化 | 字号、气泡圆角、代码主题 | P1-03 | 0.5 | 深浅均正常 |
| P4-14 | 设置测试 | 配置读写/Key 加解密测试 | P4-11 | 1.0 | 覆盖率达标 |

### 5.6 阶段 5：通话（15.5 人日，含 P5-06 可选 1.5）

| ID | 任务 | 关键产出 | 依赖 | 人日 | 验收 |
|---|---|---|---|---|---|
| P5-01 | 权限与 Manifest | 录音/通知/FGS/`MODIFY_AUDIO_SETTINGS` | P1-02 | 0.5 | 低版本不申请高版本权限 |
| P5-02 | ASR 引擎接口 + Vosk 模型管理 | 仅 small-cn 42MB：下载/断点/校验/解压；1.3GB 大模型不作首版选项 | P5-01 | 1.5 | 42MB 模型可用；下载失败可恢复 |
| P5-03 | Vosk 流式识别 | partial/final、VAD、重启 | P5-02 | 1.5 | 端点到句 |
| P5-04 | AudioRecord/AEC | 16k mono、VOICE_COMMUNICATION、AEC/NS | P5-03 | 1.0 | 无明显回声 |
| P5-05 | TTS 接口 + 系统 TTS | 初始化、语速/音调/音色 | P5-01 | 1.0 | 中文可播报 |
| P5-06 | P1 可选：sherpa-onnx TTS | 接入/模型下载；首版不启用，不阻塞验收 | P5-05 | 1.5 | 可开关；未安装时回退系统 TTS |
| P5-07 | 分句与打断 | 句子队列、flush、取消请求 | P5-05 | 1.0 | 打断 <300ms |
| P5-08 | CallViewModel 状态机 | Dialing/Listening/Thinking/Speaking/Ended | P5-04 | 1.5 | 状态无死锁 |
| P5-09 | 音频模式/焦点/免提/静音 | `AudioManager` 封装 | P5-08 | 1.0 | 挂断恢复 |
| P5-10 | 前台服务/通知/WakeLock | `microphone` 类型、常驻通知 | P5-09 | 1.0 | 后台不被杀 |
| P5-11 | 接近传感器策略 | 调暗/超时熄屏降级方案 | P5-09 | 0.5 | 真机验证 |
| P5-12 | 通话页 UI | 背景、字幕、四按钮、键盘输入 | P5-08 | 1.5 | 沉浸式/可读 |
| P5-13 | 降档/恢复/归档 | 进入 low、挂断还原、写记忆池 | P5-08 | 0.5 | 参数隔离 |
| P5-14 | 通话真机测试 | 回声/延迟/打断/1h 耗电；不启用 P5-06 时系统 TTS 全链路 | P5-12 | 1.0 | 报告归档 |

> P5-06 为 P1 可选：首版不做时扣减 1.5 人日，总工作量降至 79.5 人日；阶段 5 验收以 P5-01～P5-05、P5-07～P5-14 为准。

### 5.7 阶段 6：导入导出与全版本适配（10 人日）

| ID | 任务 | 关键产出 | 依赖 | 人日 | 验收 |
|---|---|---|---|---|---|
| P6-01 | 导出 JSON（版本化 + 图片 ZIP） | `format_version`、附件打包 | P2-01 | 1.0 | 可重导入 |
| P6-02 | 导出 Markdown/TXT | 排版、代码块、图片引用 | P6-01 | 0.5 | 阅读正常 |
| P6-03 | SAF 直写 | `CREATE_DOCUMENT`、进度/失败恢复 | P6-01 | 1.0 | 无存储权限 |
| P6-04 | 导入解析 | 校验、ID 重映射、事务、统计 | P6-01 | 1.5 | 大文件不 OOM |
| P6-05 | 官方格式 adapter | 样本 → 映射 → 测试 | 样本 | 1.0 | 样本全部导入 |
| P6-06 | 批量/全量导出 | 多选、全量 ZIP | P6-03 | 0.5 | 进度可取消 |
| P6-07 | 版本适配清单 | 11～17 行为差异 checklist | P5-14 | 1.0 | 逐项有结论 |
| P6-08 | 16KB 对齐验证 | `check_elf_alignment.sh`、模拟器 | P6-07 | 1.0 | 无 4KB 依赖 |
| P6-09 | edge-to-edge/predictive back | Insets、返回手势 | P6-07 | 0.5 | 各版本不遮挡 |
| P6-10 | 性能优化 | baseline profile、列表 key、渲染缓存 | P3-13 | 1.5 | 启动/帧率达标 |
| P6-11 | 可访问性/国际化 | contentDescription、字符串 | P4-11 | 0.5 | Scanner 无阻断 |

### 5.8 阶段 7：测试与发布（7 人日）

| ID | 任务 | 关键产出 | 依赖 | 人日 | 验收 |
|---|---|---|---|---|---|
| P7-01 | 自动化测试补齐 | 单测/仪器/UI 测试 | P6-11 | 1.0 | 核心模块 ≥70% |
| P7-02 | 手工矩阵执行 | §12.1/12.2 清单 | P6-11 | 1.0 | 缺陷归档 |
| P7-03 | 性能/耗电/内存 | Macrobenchmark、LeakCanary | P6-10 | 1.0 | 达标报告 |
| P7-04 | Lint/依赖/许可证审计 | 无废弃 API、无 GPL | P7-01 | 1.0 | 报告通过 |
| P7-05 | R8/签名/打包 | release APK/AAB、mapping | P7-04 | 1.0 | 可安装 |
| P7-06 | GitHub 发布 | README、截图、Release Notes、宣传图 | P7-05 | 0.5 | Release 可下载 |
| P7-07 | 隐私/关于页定稿 | 隐私声明、第三方声明、开源协议 | P4-11 | 0.5 | 文案齐全 |
| P7-08 | 合规复审 | 名称/图标/色值/文案 | P7-07 | 0.5 | 法务/自检通过 |
| P7-09 | RC 回归 | 全量回归、打 tag | P7-08 | 0.5 | 无 P0/P1 缺陷 |

### 5.9 工作量汇总

| 阶段 | 人日 | 占比 | 主要风险 |
|---|---|---|---|
| P0 工具链/契约 | 4.5 | 5.6% | 环境、Key 可用性 |
| P1 工程基础 | 6.0 | 7.4% | 低 |
| P2 数据/记忆池 | 9.0 | 11.1% | 清理规则、附件模型 |
| P3 API/聊天 | 15.0 | 18.5% | SSE、Markdown 库 |
| P4 多模态/设置 | 14.0 | 17.3% | Key 加密、配置一致性 |
| P5 通话 | 15.5 | 19.1% | 回声、16KB、贴脸熄屏 |
| P6 迁移/适配 | 10.0 | 12.3% | 官方格式、性能 |
| P7 测试/发布 | 7.0 | 8.6% | 回归、合规 |
| **合计** | **81.0** | 100% | 含 20% 缓冲约 **97 人日** |

---

## 6. 里程碑

| 里程碑 | 累计人日 | 日历周（全职） | 交付物 | 通过标准 |
|---|---|---|---|---|
| M0 基线可用 | 4.5 | W1 | 空壳 APK、接口样本、设备清单 | `assembleDebug` + 5 组冒烟通过 |
| M1 工程骨架 | 10.5 | W2～W3 | 主题/导航/抽屉/组件 | 阶段 1 验收项全过 |
| M2 数据底座 | 19.5 | W4 | Room/记忆池/清理/上下文 | 阶段 2 验收项全过 |
| M3 可聊天 | 34.5 | W5～W7 | 流式/Markdown/思考/会话 | 阶段 3 验收项全过 |
| M4 完整体验 | 48.5 | W8～W10 | 多模态/模型/设置/Key | 阶段 4 验收项全过 |
| M5 通话可用 | 64.0 | W11～W13 | ASR/TTS/打断/通话页 | 阶段 5 验收项全过 |
| M6 可迁移/可适配 | 74.0 | W14～W15 | 导入导出/11～17/性能 | 阶段 6 验收项全过 |
| M7 可发布 | 81.0 | W16～W17 | RC、APK/AAB、GitHub | 阶段 7 验收项全过 |

> 日历周按 5 天/周、1 人全职估算；若只能业余投入，按实际可用小时折算，不要按周直接承诺。

---

## 7. 验收映射

| 原文档验收阶段 | 覆盖任务 | 证据形式 |
|---|---|---|
| §11.2 阶段 1 | P1-01～P1-09 | 编译日志、主题截图、导航录屏 |
| §11.2 阶段 2 | P2-01～P2-10 | schema 文件、单测报告、清理规则用例 |
| §11.2 阶段 3 | P3-01～P3-15 | 真流样本回放、聊天录屏、Token 截图 |
| §11.2 阶段 4 | P4-01～P4-14 | 多图会话、设置项清单、Key 加密检查 |
| §11.2 阶段 5 | P5-01～P5-14 | 通话录屏、回声/打断测试记录、耗电报告 |
| §11.2 阶段 6 | P6-01～P6-11 | 导入导出样本、版本矩阵、16KB 报告 |
| §12.1 功能清单 | P7-02 | 逐项勾选表 + 缺陷链接 |
| §12.2 兼容矩阵 | P0-06/P6-07/P7-02 | 机型/版本表格 |
| §12.3 性能指标 | P6-10/P7-03 | Macrobenchmark/Battery 报告 |

---

## 8. 本机环境现状与准备

### 8.1 现状（2026-11-19 实测）

| 项 | 现状 | 结论 |
|---|---|---|
| 操作系统 | Windows，PowerShell 7 | 可用 |
| JDK | `JAVA_HOME` 指向 `C:\Program Files\Java\jdk-25`；另有 `C:\Users\34598\.jdks\ms-21.0.8` | **需切到 JDK 21**（AGP 对 25 兼容性差） |
| Android SDK | 未安装（无 `ANDROID_HOME`，无 `C:\Users\34598\AppData\Local\Android\Sdk`） | **阻塞** |
| Android Studio | 未安装 | 建议安装（SDK Manager/模拟器最省事） |
| Git | 未安装（`git` 不在 PATH） | **阻塞**版本管理/GitHub |
| Gradle | 有 `~/.gradle/wrapper/dists` 缓存，无全局 `gradle` | 用 wrapper 或 Studio 自带 |
| 磁盘 | C 盘剩 42GB；D 盘剩 337GB | SDK 建议装 D 盘 |
| 项目目录 | `D:\DeepSeekAdK` 仅有 `APPICONS` 4 张图 | 无工程骨架 |

### 8.2 准备步骤（建议顺序）

```powershell
# 1) 本次会话临时使用 JDK 21（持久化请用“系统环境变量”手动设置）
$env:JAVA_HOME = 'C:\Users\34598\.jdks\ms-21.0.8'
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
java -version   # 应显示 21.x

# 2) 安装 Android Studio（含 SDK Manager / 模拟器）
#    下载：https://developer.android.com/studio
#    首次启动后安装：SDK Platform 36、Build-Tools 36.x、Platform-Tools、Emulator
#    Android 17 预览：SDK Manager → "Android Cinnamon Bun Preview" + Build-Tools 37.x

# 3) 安装 Git
#    https://git-scm.com/download/win

# 4) 建议 SDK 目录放到 D 盘，并在系统环境变量设置：
#    ANDROID_HOME = D:\Android\Sdk
#    Path 追加 %ANDROID_HOME%\platform-tools;%ANDROID_HOME%\cmdline-tools\latest\bin

# 5) 验证
adb devices
sdkmanager --list
```

> 注意：不要现在就把 `JAVA_HOME` 永久改成 21 之前先确认其他项目是否依赖 25；建议用 Android Studio 的 Gradle JDK 设置或项目级 `org.gradle.java.home`，避免影响全局。

### 8.3 环境风险

- 首次 Gradle 同步需要下载 AGP/Kotlin/Compose 依赖，网络不稳会长时间卡住；建议配置国内镜像或代理后再同步。
- Android 17 预览 SDK 与稳定 AGP 可能不兼容；CI 中把 37 作为“允许失败”的兼容任务，不阻塞发布。
- 16KB 模拟器镜像体积大、启动慢；建议只在 P6 阶段集中验证，不要占用日常开发机资源。

---

## 9. 决策记录与残余事项

| ID | 决策 | 选项 | 结论 | 不决策/不执行后果 |
|---|---|---|---|---|
| D1 | 应用名 | A 保留 SOFTSEEK + 显著非官方声明；B 改名；C 保留但加副标 | **已确认 A+C：保留 SOFTSEEK，接受相似度风险；对外统一加“非官方 DeepSeek 客户端”副标；图标/色值继续差异化** | 商标混淆风险由项目负责人承担；上架前法务复核 |
| D2 | 语音方案 | A 原文档 Vosk+Piper；B Vosk small + 系统 TTS；C sherpa-onnx 统一 | **已确认 B 首版 + C 作为 P1 可选**；首版不集成 Piper、不使用 1.3GB 大模型 | 首版语音能力以短句/近场为主；高精度延后 |
| D3 | 清理规则 | A 保持“<10 条 + 24h”；B 仅清 0～2 条空/中断会话；C 默认关闭 | **已确认 C：默认关闭；设置页提供开关；开启后阈值默认 2 条、保护期 24h、置顶排除；无论开关状态均提供“立即清理”并先预览/确认；回收站 7 天** | 用户需手动维护存储；设置页需做引导 |
| D4 | 首发服务商 | A 仅 DeepSeek；B +豆包；C 全部 | **未单独确认，按推荐 A + 自定义 OpenAI 兼容入口执行**；P1 再加专门适配 | 豆包/千问深度适配延后 |
| D5 | compileSdk 策略 | A 36 发布 + 37 验证；B 直接 37 | **未单独确认，按推荐 A 执行** | 37 预览问题不阻塞发布 |
| D6 | 多图/文件范围 | A 单图；B 多图（≤8）；C 多图+PDF | **未单独确认，按推荐 B 执行**；纯文本注入，PDF 放 P2 | schema 按多图设计，PDF 延后 |
| D7 | 包名 | `com.custom.aiclient`；`com.softseek.app`；`io.github.we007oneday_byte.softseek` | **已定 `io.github.we007oneday_byte.softseek`**（owner `we007oneday-byte`）；发布前冻结；如不想暴露账号可退回 `com.softseek.app` | 发布后改包名会导致升级链断裂 |

> 说明：D1/D2/D3 为本轮明确确认；D4～D6 按推荐默认执行，如需调整请在进入对应阶段前提出；D7 已按 GitHub owner 确定，发布前冻结。

> GitHub：owner `we007oneday-byte`，建议仓库名 `SoftSeek`（公开、MIT）；仓库创建需先完成 GitHub 授权，安排在 P0-03 或 P7-06 执行。

---

## 10. 下一步

1. **决策已回填**：D1/D2/D3 已确认；D4～D6 按推荐默认执行；D7 包名定为 `io.github.we007oneday_byte.softseek`。
2. **执行 P0-01～P0-04**：装 JDK 21/SDK 36/Git，生成空壳工程并跑通 `assembleDebug`。
3. **执行 P0-05**：用一个真实 API Key 跑 5 组冒烟；Key 只放本机临时环境变量，不写代码、不进日志、不提交仓库。
4. **进入 P1**：主题 → 导航 → 抽屉 → 通用组件，按阶段 1 验收。
5. 环境就绪后，我可以直接在 `D:\DeepSeekAdK` 生成阶段 1 的工程骨架（Gradle 配置、包结构、主题、导航、空壳页面）。

---

## 附录 A：依赖基线（修正版）

| 类别 | 依赖 | 版本基线 | 说明 |
|---|---|---|---|
| 构建 | AGP | 8.9.0+ | 支持 compileSdk 37 预览的最低要求；发布用 36 |
| 构建 | Gradle | 8.11.x+ | 与 AGP 矩阵对齐 |
| 语言 | Kotlin | 2.0.20 | 文档基线；可评估升级 |
| 语言 | Compose Compiler 插件 | 与 Kotlin 同版本 | **必须显式应用** |
| 注解处理 | KSP | 与 Kotlin 2.0.20 对应 | 替代 kapt |
| UI | Compose BOM | 2024.10.01 | 映射 material3 1.3.0 |
| UI | Material3 | 由 BOM 管理 | 不单独锁 1.2.0 |
| UI | Navigation Compose | 2.7.7（建议 2.8+） | 2.8+ 对 predictive back 更友好 |
| UI | Coil Compose | 2.6.0 | 图片加载/缓存 |
| UI | compose-richtext | 0.20.0 | 需 spike；备选自研 |
| UI | accompanist-permissions | 0.34.0 | 仅保留权限；移除 swiperefresh |
| 网络 | Retrofit | 2.11.0 | |
| 网络 | OkHttp | 4.12.0 | SSE/代理/超时 |
| 网络 | kotlinx-serialization-json | 1.6.3 | |
| 网络 | retrofit2-kotlinx-serialization-converter | 1.0.0 | **原文档缺失** |
| 数据库 | Room | 2.6.1 | KSP |
| 存储 | DataStore Preferences | 1.1.1 | 设置项 |
| DI | Hilt | 2.52（最低 2.51） | 2.50 对 Kotlin 2.0 风险 |
| DI | hilt-navigation-compose | 1.2.0 | |
| 后台 | WorkManager | 2.9.x | 清理/回收站 |
| 安全 | Android Keystore | 系统 API | AES/GCM 自实现 |
| 生物识别 | androidx.biometric | 1.1.0 | 可选 |
| 启动 | core-splashscreen | 1.0.1 | Android 12+ 统一 |
| ASR | com.alphacephei:vosk-android | 0.3.47 | 42MB 中文小模型 |
| TTS | 系统 TextToSpeech | 系统 | 默认引擎 |
| TTS（可选） | sherpa-onnx | 1.12.x/2.x | Apache-2.0，替代 Piper |
| 测试 | JUnit4 / Turbine / MockK / Robolectric | 最新稳定 | 解析器/VM/Room |

> 依赖版本以阶段 0 的 `dependencyResolutionManagement` 实际解析结果为准；上表是“可开工的下限”，不是永久锁定。

## 附录 B：官方核对记录（2026-11-19）

| 主题 | 结论 | 来源 |
|---|---|---|
| 模型列表 | 仅 `deepseek-flash`（文本+图片，1M ctx，393216 max output，effort low/high/max）与 `deepseek-v4-pro`（纯文本） | https://api-docs.deepseek.com/zh-cn/api/list-models/ |
| Chat 端点 | `POST https://api.deepseek.com/chat/completions` | https://api-docs.deepseek.com/zh-cn/api/create-chat-completion/ |
| 思考参数 | `reasoning_effort`：none/low/high/max；`thinking` 对象优先；默认 high | 同上 |
| 流式 usage | 最后一个内容块携带 usage；`data: [DONE]` 结束 | 同上 |
| 视觉 | `deepseek-flash` 支持 JPEG/PNG/GIF/WebP；base64/URL/Files API；单图最多 384 tokens | https://api-docs.deepseek.com/zh-cn/guides/vision/ |
| 余额 | `GET https://api.deepseek.com/user/balance`；`is_available` + `balance_infos[]` | https://api-docs.deepseek.com/zh-cn/api/get-user-balance/ |
| Android 17 | 预览版（Cinnamon Bun）；需 AGP 8.9.0-rc01+、Studio Meerkat 2024.3.1+ | https://developer.android.com/about/versions/17/setup-sdk |
| 16KB | targetSdk 35+ 的 64 位应用需支持 16KB；2027-02-01 起不合规更新无法发布 | https://developer.android.com/guide/practices/page-sizes |
| Vosk 坐标 | `com.alphacephei:vosk-android`，最新 0.3.47 | https://search.maven.org/artifact/com.alphacephei/vosk-android |
| Vosk 中文模型 | small-cn 42MB；big-cn 1.3GB | https://alphacephei.com/vosk/models |
| sherpa-onnx | Apache-2.0，Android ASR/TTS/VAD 支持 | https://k2-fsa.github.io/sherpa/onnx/tts/apk-cn.html |

---

## 附录 C：任务状态表（可直接当看板）

> 初始状态均为“待开始”。建议导入飞书多维表格/GitHub Projects 后按 ID 跟踪。

| ID | 阶段 | 任务 | 状态 | 负责人 | 备注 |
|---|---|---|---|---|---|
| P0-01 | P0 | JDK 21 | 待开始 | | |
| P0-02 | P0 | Android SDK 36 | 待开始 | | |
| P0-03 | P0 | Git/仓库 | 待开始 | | |
| P0-04 | P0 | Gradle 基线 | 待开始 | | |
| P0-05 | P0 | API 冒烟 | 待开始 | | 需 Key |
| P0-06 | P0 | 设备矩阵 | 待开始 | | |
| P1-01 | P1 | 包结构（io.github.we007oneday_byte.softseek） | 待开始 | | D7 |
| P1-02 | P1 | Hilt | 待开始 | | |
| P1-03 | P1 | 主题 | 待开始 | | |
| P1-04 | P1 | 导航 | 待开始 | | |
| P1-05 | P1 | 通用组件 | 待开始 | | |
| P1-06 | P1 | 聊天壳/抽屉 | 待开始 | | |
| P1-07 | P1 | 启动页/图标 | 待开始 | | |
| P1-08 | P1 | 资源化 | 待开始 | | |
| P1-09 | P1 | 质量基线 | 待开始 | | |
| P2-01 | P2 | 实体/索引 | 待开始 | | |
| P2-02 | P2 | DAO | 待开始 | | |
| P2-03 | P2 | 数据库 | 待开始 | | |
| P2-04 | P2 | 仓储 | 待开始 | | |
| P2-05 | P2 | 池块生命周期 | 待开始 | | |
| P2-06 | P2 | 清理引擎（默认关+手动） | 待开始 | | D3 |
| P2-07 | P2 | 回收站清理 | 待开始 | | |
| P2-08 | P2 | 上下文拼装 | 待开始 | | |
| P2-09 | P2 | Token 估算 | 待开始 | | |
| P2-10 | P2 | 数据层测试 | 待开始 | | |
| P3-01 | P3 | Provider 抽象 | 待开始 | | |
| P3-02 | P3 | DTO | 待开始 | | |
| P3-03 | P3 | 网络层 | 待开始 | | |
| P3-04 | P3 | SSE | 待开始 | | |
| P3-05 | P3 | 发送/重发/停止 | 待开始 | | |
| P3-06 | P3 | ChatViewModel | 待开始 | | |
| P3-07 | P3 | 消息列表 | 待开始 | | |
| P3-08 | P3 | 气泡 | 待开始 | | |
| P3-09 | P3 | 输入栏 | 待开始 | | |
| P3-10 | P3 | 消息操作 | 待开始 | | |
| P3-11 | P3 | 会话抽屉 | 待开始 | | |
| P3-12 | P3 | 标题生成 | 待开始 | | |
| P3-13 | P3 | Markdown/高亮 | 待开始 | | spike |
| P3-14 | P3 | Token/错误 | 待开始 | | |
| P3-15 | P3 | 聊天测试 | 待开始 | | |
| P4-01 | P4 | 图片选择 | 待开始 | | |
| P4-02 | P4 | 压缩/缓存 | 待开始 | | |
| P4-03 | P4 | 附件落库/预览 | 待开始 | | |
| P4-04 | P4 | 视觉请求 | 待开始 | | |
| P4-05 | P4 | 模型/档位 | 待开始 | | |
| P4-06 | P4 | 参数预设 | 待开始 | | |
| P4-07 | P4 | DataStore | 待开始 | | |
| P4-08 | P4 | Key 管理 | 待开始 | | |
| P4-09 | P4 | Key 校验 | 待开始 | | |
| P4-10 | P4 | 余额/用量 | 待开始 | | |
| P4-11 | P4 | 设置页 | 待开始 | | |
| P4-12 | P4 | 高级网络 | 待开始 | | |
| P4-13 | P4 | 主题细化 | 待开始 | | |
| P4-14 | P4 | 设置测试 | 待开始 | | |
| P5-01 | P5 | 权限/Manifest | 待开始 | | |
| P5-02 | P5 | ASR/模型管理 | 待开始 | | |
| P5-03 | P5 | Vosk 流式 | 待开始 | | |
| P5-04 | P5 | AudioRecord/AEC | 待开始 | | |
| P5-05 | P5 | TTS/系统 | 待开始 | | |
| P5-06 | P5 | P1 可选：sherpa TTS | 待开始 | | 首版不做 |
| P5-07 | P5 | 分句/打断 | 待开始 | | |
| P5-08 | P5 | CallViewModel | 待开始 | | |
| P5-09 | P5 | 音频模式 | 待开始 | | |
| P5-10 | P5 | 前台服务 | 待开始 | | |
| P5-11 | P5 | 接近传感器 | 待开始 | | 降级 |
| P5-12 | P5 | 通话页 | 待开始 | | |
| P5-13 | P5 | 降档/归档 | 待开始 | | |
| P5-14 | P5 | 真机测试 | 待开始 | | |
| P6-01 | P6 | 导出 JSON | 待开始 | | |
| P6-02 | P6 | 导出 MD/TXT | 待开始 | | |
| P6-03 | P6 | SAF 直写 | 待开始 | | |
| P6-04 | P6 | 导入解析 | 待开始 | | |
| P6-05 | P6 | 官方 adapter | 待开始 | | 需样本 |
| P6-06 | P6 | 批量导出 | 待开始 | | |
| P6-07 | P6 | 版本适配 | 待开始 | | |
| P6-08 | P6 | 16KB 验证 | 待开始 | | |
| P6-09 | P6 | edge-to-edge | 待开始 | | |
| P6-10 | P6 | 性能优化 | 待开始 | | |
| P6-11 | P6 | 可访问性/i18n | 待开始 | | |
| P7-01 | P7 | 自动化测试 | 待开始 | | |
| P7-02 | P7 | 手工矩阵 | 待开始 | | |
| P7-03 | P7 | 性能/耗电 | 待开始 | | |
| P7-04 | P7 | Lint/许可证 | 待开始 | | |
| P7-05 | P7 | R8/签名 | 待开始 | | |
| P7-06 | P7 | GitHub 发布 | 待开始 | | |
| P7-07 | P7 | 隐私/关于 | 待开始 | | |
| P7-08 | P7 | 合规复审 | 待开始 | | |
| P7-09 | P7 | RC 回归 | 待开始 | | |

---

**评审人签名：** ________　　**开发执行人签名：** ________　　**日期：** 2026-11-19
