# SOFTSEEK（SK）需求分析与开发任务书

| 项目 | 内容 |
|---|---|
| 评审对象 | 《DeepSeek 第三方安卓客户端 完整AI开发执行文档 V1.0》 |
| 项目代号 | SOFTSEEK / SK |
| 文档版本 | V1.6（取消 Gitee 双源，改为 GitHub 单源发布） |
| 编制日期 | 2026-11-19（V1.6 更新 2026-11-20） |
| 变更记录 | V1.1：D1 保留 SOFTSEEK（接受相似度风险）；D2 首版 Vosk small-cn 42MB + 系统 TTS；D3 自动清理默认关闭 + 手动触发。V1.2：D7 包名按 GitHub owner 定为 `io.github.we007oneday_byte.softseek`。V1.3：新增 §5.9 版本升级与软件发布计划（应用内 24h 更新检查、双源、Vite/GitHub Pages 官网、地理路由下载、Release 命名规范）。V1.4：补充 CI 构建 APK + SHA-256 + GPG 清单签名；客户端内置公钥/证书指纹，静默验签与哈希校验，失败拦截；密钥不硬编码。V1.5：新增 §11 ZL-01 增量任务（黑白 UI/UX、品牌与动效、模型/Provider 配置、AI 通话独立化、离线模型与 TTS 多后端），标记为下一 P0 大任务。V1.6：**取消 Gitee 双源、Gitee Token 与地域分流，P7 改为 GitHub 单源发布**；Gitee/CDN/OSS 作为流量增长后的独立增量，不进入本期验收 |
| 技术基线核对时间 | 2026-11-19（DeepSeek API、Android 17 SDK、Vosk、sherpa-onnx） |
| 评审结论 | **有条件通过；6 个硬缺口已给出修正方案；D1/D2/D3 已确认；V1.4 已补全升级、发布、供应链签名与客户端验签计划。当前 P1～P4 已完成，P5 基础已完成并推送，P5-14 真机验收收尾；下一 P0 为 ZL-01，见 §11** |
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

- 总工作量：**约 91.0 人日**（含 P5-06 可选 TTS 与 P7 发布增量 10.0 人日；若首版不做 P5-06 则 89.5 人日），含 20% 缓冲约 **109.2 人日**；按每周 5 天计约 **22～23 周**。
- 关键路径：`P0 工具链 → P1 工程骨架 → P2 数据/记忆池 → P3 API/聊天 → P5 通话 → P6 迁移/适配 → P7 发布（含 24h 升级检查、GitHub 单源发布、官网、供应链签名与客户端验签）`。
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
| 发布/升级 | 应用内 24h 升级检查与提醒 | P0 | WorkManager + GitHub Pages 静态清单；见 §5.9 |
| 发布/升级 | CI 构建 APK、SHA-256、GPG 清单签名 | P0 | GitHub Actions；正式版必须签名；见 §5.9.9 |
| 发布/升级 | 客户端静默验签/哈希/APK 证书校验，失败拦截 | P0 | 内置更新公钥与证书指纹；见 §5.9.9 |
| 发布/升级 | GitHub Release 单源发布 | P0 | GitHub Actions；见 §5.9.5；CDN/OSS 后置 |
| 发布/分发 | GitHub Pages + Vite 官网 | P0 | 介绍/功能/API/指南/下载；见 §5.9.6 |
| 发布/分发 | 下载中心与历史版本（GitHub 单源） | P0 | 失败给 GitHub 手动下载入口；不按国家码分流 |

> ZL-01 增量需求（黑白 UI/UX、品牌图标、设置菜单化、Provider/模型配置、AI 通话独立化、离线模型与 TTS 多后端）单列于 §11，不重复计入本矩阵。

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
| R16 | GitHub API / Pages 国内访问不稳 | 高 | 官网历史版本与更新检查失败 | 高 | 请求超时、403/429 | 构建期生成静态索引；24h 缓存；官网提供 GitHub 手动下载入口；流量增长后再评估 CDN/OSS | P7 |
| R17 | GitHub 单源故障/账号风险 | 中高 | Release 下载中断、更新检查失败 | 高 | 404/5xx、限流 | 保留签名清单与 GitHub 手动入口；发布记录归档；CDN/OSS 作为后续独立任务，不进入本期验收 | P7 |
| R18 | GitHub 在中国大陆访问不稳定 | 高 | 官网/清单/资产加载失败，用户无法下载 | 中 | 网络策略、DNS 污染 | 官网保留“复制直链”和校验说明；客户端保留上次已签名清单；不引入未签名镜像；CDN/OSS 另立任务 | P7 |
| R19 | GPG/APK 签名私钥泄露或轮换失败 | 高 | 伪造更新、供应链攻击 | 高 | 私钥误提交、丢失、过期 | 私钥仅 GitHub Secrets；GPG 使用专用子密钥；双钥重叠与撤销流程；APK keystore 分环境保存 | P7 |
| R20 | 客户端验签误杀或证书 API 兼容问题 | 中 | 正常更新被拦截 | 中 | Key ID 不匹配、Android 版本差异 | 受信公钥环、灰度发布、失败时给官网手动下载兜底、可诊断但不泄露敏感信息 | P7 |

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
| ADR-11 | 更新检查读静态 `updates/{channel}.json`，不直接依赖 GitHub Release API | 避免移动端限流/被墙；CI 生成、GitHub Pages 单托管 | 清单需原子发布、缓存一致性 |
| ADR-12 | 官网不做地理路由，P7 采用 GitHub 单源发布 | 取消 Gitee 双源；失败给 GitHub 手动入口，降低实现复杂度 | 大陆访问不稳；CDN/OSS 后续独立评估 |
| ADR-13 | 下载直链 Release 资产，不经 Pages 代理 APK | 不消耗 Pages 流量、无跳转落地页 | 依赖 GitHub 资产可用性；未来以 `urls[]` 扩展 CDN/OSS |
| ADR-14 | APK 用 Android keystore 签名，更新清单用 GPG detached signature | 两类签名职责分离；公钥可公开固定 | 需维护两套密钥与轮换 |
| ADR-15 | 客户端先验清单签名、SHA-256、APK 证书指纹，再弹安装；不静默安装 | 满足失败拦截；符合 Android 安装模型 | 首次需用户允许未知来源 |

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

> P5-06 为 P1 可选：首版不做时扣减 1.5 人日，总工作量降至 89.5 人日；阶段 5 验收以 P5-01～P5-05、P5-07～P5-14 为准。

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

### 5.8 阶段 7：测试与发布（17.0 人日，含 §5.9 发布增量）

| ID | 任务 | 关键产出 | 依赖 | 人日 | 验收 |
|---|---|---|---|---|---|
| P7-01 | 自动化测试补齐 | 单测/仪器/UI 测试 | P6-11 | 1.0 | 核心模块 ≥70% |
| P7-02 | 手工矩阵执行 | §12.1/12.2 清单 | P6-11 | 1.0 | 缺陷归档 |
| P7-03 | 性能/耗电/内存 | Macrobenchmark、LeakCanary | P6-10 | 1.0 | 达标报告 |
| P7-04 | Lint/依赖/许可证审计 | 无废弃 API、无 GPL | P7-01 | 1.0 | 报告通过 |
| P7-05 | R8/签名/打包 | release APK/AAB、mapping | P7-04 | 1.0 | 可安装 |
| P7-06 | GitHub Release 基线 | README、截图、Release Notes、宣传图、正式版/测试版 Release | P7-05 | 0.5 | Release 可下载；资产命名合规 |
| P7-07 | 隐私/关于页定稿 | 隐私声明、第三方声明、开源协议 | P4-11 | 0.5 | 文案齐全 |
| P7-08 | 合规复审 | 名称/图标/色值/文案 | P7-07 | 0.5 | 法务/自检通过 |
| P7-09 | RC 回归 | 全量回归、打 tag | P7-08 | 0.5 | 无 P0/P1 缺陷 |

> 本节 P7-01～P7-09 合计 7.0 人日；P7-10～P7-17 见 §5.9.10，合计 10.0 人日；阶段 7 总计 17.0 人日。

### 5.9 版本升级与软件发布计划（P7 增量，10.0 人日）

本节是 P7 的发布增量：解决“用户如何知道有新版本、官网如何展示与分发、下载是否可信”三个问题。**GitHub Releases 是唯一发布事实源，GitHub Pages 是官网与更新清单托管面**；APK 用 Android keystore 签名，更新清单用 GPG 分离签名，客户端验签失败即拦截。本期不做 Gitee 双源，也不做 CDN/OSS 镜像；等流量增长后另立任务。

#### 5.9.1 目标、渠道与边界

| 项 | 规则 |
|---|---|
| 升级提醒 | 应用内每 **24 小时**检查一次；发现更高版本后在应用内提示，不静默安装 |
| 完整性校验 | 清单 GPG 验签 → 资产 SHA-256 → APK 证书指纹，任一步失败即拦截 |
| 更新通道 | `stable`（正式版，默认）与 `beta`（测试版，需用户在设置中主动开启） |
| 正式源 | GitHub Releases；发布资产、更新清单、官网由 GitHub 托管 |
| 镜像源 | **本期无**；CDN/OSS 不进入 P7 验收，流量增长后另开任务 |
| 官网 | GitHub Pages + Vite 静态站；不做国家码/时区分流，统一 GitHub 下载 |
| 单源风险 | GitHub 国内访问不稳作为已知风险接受；官网保留手动下载入口，后续再评估 CDN/OSS |
| 不做 | 不做应用内代理下载服务器、不做静默安装、不绕过 Android 签名/未知来源校验、不承诺应用商店上架 |

#### 5.9.2 版本号、发布性质与包命名

**版本号**采用 SemVer：`MAJOR.MINOR.PATCH`；预发布追加 `-beta.N`。Android `versionCode` 必须单调递增，不能回退。建议 `versionCode = MAJOR*1_000_000 + MINOR*10_000 + PATCH*100 + 通道位`，正式版通道位 `99`，测试版取 `1..98`（beta 序号），确保同版本正式版始终高于 beta，且后续补丁版继续递增。该公式约束 `MINOR`/`PATCH` 不超过 99；超出或需要热修时改用 CI 维护的单调递增序号。

**发布性质**只允许两种：

- `正式版`：`stable` 通道，GitHub Release 的 `prerelease=false`，官网“最新正式版”只取此类。
- `测试版`：`beta` 通道，`prerelease=true`，只在用户开启测试通道后提示。

**软件包命名格式（强制）**：

```text
{软件名}-{发布性质}-{版本号}.apk
```

示例：

```text
SOFTSEEK-正式版-v1.2.3.apk
SOFTSEEK-测试版-v1.3.0-beta.1.apk
```

约束：

- 软件名固定大写 `SOFTSEEK`；发布性质固定中文 `正式版` / `测试版`；版本号带 `v` 前缀且只出现一次。
- 仅使用半角连字符 `-`，不得有空格、下划线、括号、斜杠、冒号；扩展名固定 `.apk`。
- Release tag 与版本号一致：`v1.2.3`、`v1.3.0-beta.1`；Release 标题为 `SOFTSEEK 正式版 v1.2.3` / `SOFTSEEK 测试版 v1.3.0-beta.1`。
- 每个 APK 必须同时生成 `.sha256` 校验文件；GitHub Release 资产文件名与 SHA256 必须与清单一致。
- `mapping.txt`、签名材料、密钥、token 一律不进公开 Release；AAB 不作为官网直接下载项。
- CI 在发布前校验：APK 的 `versionName`/`versionCode`、文件名、tag、清单字段四者一致；不一致直接失败。

#### 5.9.3 更新清单（update manifest）

更新检查不直接让手机端打 GitHub Release API，而是读取 CI 生成的静态清单，避免移动端限流、API 解析差异与 GitHub API 不可达。清单随官网一起部署在 GitHub Pages：

```text
https://we007oneday-byte.github.io/SoftSeek/updates/stable.json
https://we007oneday-byte.github.io/SoftSeek/updates/beta.json
```

本期不引入 CDN/OSS；`assets[].urls[]` 只填 GitHub 直链，未来若加镜像再追加，不改变清单验签与哈希校验逻辑。

清单最小字段：

| 字段 | 说明 |
|---|---|
| `schemaVersion` | 固定 `1`，客户端遇到未知版本拒绝解析 |
| `channel` | `stable` / `beta` |
| `versionName` / `versionCode` | 必须与 APK 一致；客户端以 `versionCode` 比较 |
| `tag` / `publishedAt` | Release tag 与 UTC 发布时间 |
| `minSdk` | 最低 Android API；低于此值不提示 |
| `minSupportedVersionCode` | 低于该版本的客户端标记为“必须更新”，不提供“跳过” |
| `signatureStatus` | `signed` / `unsigned`；`stable` 必须为 `signed` |
| `signingKeyId` | GPG 公钥指纹/Key ID；客户端只接受受信 Key ID |
| `signatureUrl` | 与清单同源的 detached `.asc` URL；对清单原始字节验签 |
| `releaseNotes` | 简短更新说明，官网与提醒弹窗共用 |
| `assets[]` | `name`、`size`、`sha256`、`urls[]`（`source`、`url`）；本期 `source=github`，未来可追加 CDN/OSS |

发布顺序必须是：**先上传 Release 资产 → 再提交清单 → 再部署 Pages**。清单更新采用“写临时文件 + 原子替换 + 提交”，避免用户读到半成品。

**签名约定**：

- 清单写完后，用 GPG 对**文件原始字节**做 detached armor 签名：`stable.json` → `stable.json.asc`；客户端不得把 JSON 重新序列化后再验签。
- 公钥导出为 `updates/softseek-update-signing-key.asc`，与清单同源托管；公钥可以公开，私钥只能存在于 GitHub Actions Secrets。
- `stable` 清单必须 `signatureStatus=signed`；CI 没有 GPG 私钥时，正式版发布任务直接失败，不允许发未签名正式版。
- `beta` 允许在 GPG 尚未配置时发 `signatureStatus=unsigned`，但客户端只在用户主动开启 beta 并确认风险后展示，不参与默认自动提醒。
- 轮换时旧、新两把公钥同时内置，`signingKeyId` 决定使用哪把；旧钥撤销后，客户端拒绝旧签名并提示手动更新。

#### 5.9.4 应用内 24 小时升级检查

**调度**：使用 WorkManager 唯一周期任务 `update-check`，周期 `24h`，弹性窗口 `1h`，约束 `NetworkType.CONNECTED`；`ExistingPeriodicWorkPolicy.UPDATE`，避免重复入队。应用冷启动时若 `lastCheckAt` 超过 24 小时，立即补一次前台检查。Android 的 Doze/厂商调度可能延后，验收口径是“24 小时周期 + 系统允许时尽快执行”，不是精确到秒。

**数据与状态**（DataStore）：

- `lastCheckAt`、`lastCheckSource`、`etagStable`、`etagBeta`
- `pendingVersionCode`、`pendingVersionName`、`pendingChannel`
- `skippedVersionCode`、`snoozedUntil`
- `updateChannel`（默认 `stable`）

**源选择**：

1. 本期只使用 GitHub Pages 清单，不按国家码/时区切换源，也不引入 CDN/OSS。
2. 清单请求 5 秒超时或非 2xx 时，保留 `pending` 状态，不打扰用户，下一次再试；官网提供 GitHub 手动下载入口。
3. 手动检查忽略 24h 间隔，但仍走同一套校验。
4. 带 `If-None-Match` 请求；304 时只更新 `lastCheckAt`。

**版本比较与提醒**：

- 仅同通道比较；`manifest.versionCode > BuildConfig.VERSION_CODE` 才提示。
- 发现新版本后写入 `pending*`；用户下次进入应用前台时显示应用内弹窗。
- 按钮：`立即更新` / `稍后` / `跳过此版本`；低于 `minSupportedVersionCode` 时只保留 `立即更新`。
- `稍后` 24 小时内不再弹同一版本；`跳过此版本` 直到出现更高 `versionCode` 才再提示。
- 弹窗展示版本号、更新说明、大小、SHA256 前 12 位、来源（GitHub）与签名状态；下载跳转官网下载页或 GitHub Release 直链，不在应用内静默下载安装。
- 下载完成后如走应用内下载器，必须校验 SHA256；安装前由系统校验 APK 签名，签名不一致拒绝。

**测试**：版本比较、同版本不提示、跨通道不提示、24h 抑制、跳过后新版本恢复提示、清单超时/非 2xx 保留 pending、304、清单损坏/字段缺失、`minSupportedVersionCode` 强制更新。

**验签与安装校验**：客户端只接受 `signed` 的 stable 清单；下载 APK 后先校验清单 GPG 签名，再校验 SHA-256，最后校验 APK 证书指纹，全部通过才弹安装。任一步失败即删除缓存并拦截，不提供“仍然安装”入口。具体流程见 §5.9.9。

#### 5.9.5 发布源策略：GitHub 单源（Gitee 取消，CDN/OSS 后置）

**决策（2026-11-20）**：P7 只做 GitHub 单源发布，不再配置 Gitee 仓库、Gitee Token、`release-sync` workflow，也不做国家码下载分流。理由：Gitee 单附件 100MB、仓库附件总量 1GB，无法承载后续大模型包；双源同步增加维护面和失败面；先把软件做完，流量增长后再评估 CDN/OSS。

**发布范围**：

1. GitHub Release：APK、`.sha256`、`updates/{channel}.json`、`.asc`、更新公钥。
2. GitHub Pages：官网与 `updates/*.json` 静态清单。
3. 不镜像 Issues、PR、Actions 日志、Secrets。

**Workflow**：

- `.github/workflows/release.yml`：构建、签名、生成清单、GPG 签名、创建/更新 Release、上传资产。
- `.github/workflows/pages.yml`：部署官网与清单。
- 不创建 `release-sync.yml`。

**Secrets（只放 GitHub Environment Secrets）**：

- `ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`：release APK 签名。
- `GPG_PRIVATE_KEY`、`GPG_PASSPHRASE`、`GPG_KEY_ID`：更新清单签名；可选用专用子密钥。
- 不配置 `GITEE_TOKEN`、`GITEE_OWNER`、`GITEE_REPO`、`GITEE_SSH_PRIVATE_KEY`。

Token、keystore、GPG 私钥不得写入代码、日志、Pages、清单或 APK；Workflow 日志必须掩码，临时 keyring 在 job 结束后删除。

**平台约束**：

- GitHub Release 单资产上限足够当前 APK；未来大模型包如果超过 GitHub 限制，再按“分卷 + 清单 sha256”或 CDN/OSS 处理，不在本期做。
- GitHub 在中国大陆访问可能不稳；官网提供手动下载入口，客户端保留上次已签名清单，不因单源失败弹未校验更新。
- 未来加 CDN/OSS 时，只扩展 `assets[].urls[]`，不改变“签名清单 → SHA-256 → APK 证书”的信任链；CDN/OSS 不允许签发清单。


#### 5.9.6 官网：GitHub Pages + Vite

**站点与工程**：

- 线上地址：`https://we007oneday-byte.github.io/SoftSeek/`（项目 Pages）。
- 源码目录：`website/`；技术栈固定为 **Vite + React + TypeScript**，静态构建；`vite.config.ts` 的 `base` 设为 `/SoftSeek/`。
- 初始化：`npm create vite@latest website -- --template react-ts`。
- 构建：Node 20，`npm ci && npm run build`，产物 `website/dist/`。
- 部署：`.github/workflows/pages.yml`，使用 `actions/upload-pages-artifact` + `actions/deploy-pages`；Pages Source 设为“GitHub Actions”，权限 `pages: write`、`id-token: write`，并发组 `pages`。
- 触发：`push` 到 `main` 且路径含 `website/**`、`release: published`、`workflow_dispatch`。
- 本地开发：`cd website && npm ci && npm run dev`；不在 Android 工程内引入 Node 依赖。

```text
website/
├── index.html
├── vite.config.ts          # base: '/SoftSeek/'
├── package.json
├── src/
│   ├── main.tsx
│   ├── pages/              # 介绍 / 功能 / API / 指南 / 下载 / 历史 / FAQ
│   ├── components/         # Hero、Sidebar、DownloadButton、SourceBadge
│   └── data/releases.json  # CI 生成，不在页面运行时直连 GitHub API
└── dist/                   # Pages 部署产物
```

**视觉与信息架构**：参考 Vite 官网的“顶部导航 + 侧边栏文档 + Hero + 特性网格 + 代码块”信息架构与明暗主题，但**不使用 Vite 商标、Logo、文案或素材**；SOFTSEEK 使用自有品牌色与图标。移动端优先，桌面端最大宽度约 1280px。

**页面与内容（均为详细版，不接受一句话占位）**：

| 页面 | 必备内容 | 验收 |
|---|---|---|
| 首页 Hero | 软件名、一句话定位、当前正式版号、`下载最新正式版`主 CTA、GitHub 仓库入口、非官方声明 | 首屏 3 秒内可开始下载 |
| 软件介绍 | 是什么、给谁用、纯本地/零服务端、与官方客户端区别、不收集数据、支持范围 | 无夸大/无“官方”误导 |
| 功能介绍 | 聊天/思考、图片多模态、模型与档位、记忆池与清理、语音（P5 状态）、主题、多 Key、导入导出；每项配截图或状态标签 | 功能与已发布版本一致 |
| API 接入指南 | 注册 DeepSeek、创建 Key、余额与计费、应用内填写、BaseURL、模型选择、图片限制、错误码（401/402/429/5xx）、代理与安全 | 新用户可按步骤完成首次对话 |
| 软件使用指南 | 安装与未知来源、首次配置、会话、发图、模型/档位、记忆清理预览、导出/导入、语音、常见问题与故障排查 | 每步有编号与截图 |
| 下载中心 | 最新正式版卡片、测试版入口、版本/日期/大小/minSdk/SHA256、清单签名状态/Key ID、GitHub 直链按钮、更新说明 | 不跳 GitHub 落地页；签名状态可见 |
| 历史版本 | 正式版列表、测试版折叠区、版本/日期/大小/SHA、签名状态、GitHub 直链/复制、分页 | 至少 3 个历史版本可回退 |
| 隐私与免责 | 非官方声明、Key 本地加密、聊天本地存储、不采集地理位置、更新公钥与证书指纹说明、第三方链接 | 与关于页文案一致 |
| FAQ/更新日志 | 安装失败、Key 无效、图片过大、GitHub 访问失败、手动下载与校验 | 覆盖前 10 个高频问题 |

**SEO/性能/可访问性**：静态 HTML 可抓取；`title`/`description`/Open Graph；`sitemap.xml`；图片 `alt`；键盘焦点可见；Lighthouse Performance/Accessibility/SEO ≥ 90；不引入统计脚本和第三方 Cookie。

#### 5.9.7 下载中心、历史版本与 GitHub 单源

**数据来源**：

- GitHub Actions 在发布/定时任务中调用 `GET /repos/{owner}/{repo}/releases?per_page=100`，按 `Link` 头继续翻页直到取完（最多 300 条），读取 `tag_name`、`prerelease`、`published_at`、`assets[].browser_download_url`、`size`，并关联清单的 `signatureStatus`/`signingKeyId`，生成 `website/src/data/releases.json`。
- 页面运行时优先读取这份静态索引；索引缺失时才回退到 GitHub Releases API（带 `Accept`、`User-Agent`、ETag；未认证限流 60 次/小时）。
- 不调用 Gitee API，不做国家码/时区下载分流；GitHub 是唯一资产源。
- 只把 `prerelease=false` 放入“历史正式版”；测试版单独折叠，默认不展示。

**最新正式版**：取 `releases/latest` 或静态索引中 `channel=stable` 的最高 `versionCode`；只展示命名合规且带 `.apk` 的资产；按钮直接指向资产的 `browser_download_url`，不经 Release 页面跳转。清单 `sha256` 必须与 GitHub 资产一致。

**历史版本**：按 SemVer 倒序，展示版本号、发布日期、大小、SHA256 前 12 位、正式/测试标签、GitHub 直链/复制按钮；每个版本可复制直链；支持 `?channel=beta` 过滤。

**直接下载**：

- CTA 使用 `<a href="{asset.browser_download_url}" rel="noopener">`，不经过 `github.com/.../releases` 页面；浏览器由 `Content-Disposition` 直接落盘。
- 不把 APK 放进 Pages 仓库、不用 Pages 代理二进制，避免流量与仓库体积问题。
- 每个按钮旁展示 `版本号 · 大小 · SHA256 前 12 位 · 清单签名：已签名/Key ID`；另提供“复制直链”和“校验说明”。
- GitHub 打不开时，页面提示“复制直链到可访问网络重试”，不伪造备用源；CDN/OSS 待流量增长后另开任务。


#### 5.9.8 发布 Runbook（每次发版照此执行）

1. 冻结代码，更新 `versionName` / `versionCode`、`CHANGELOG.md`、官网文案中的版本号。
2. 跑完整质量门：ktlint、lint、单测/仪器测试、R8 release 构建、签名校验。
3. 生成 `SOFTSEEK-正式版-vX.Y.Z.apk` 与 `.sha256`；测试版用 `SOFTSEEK-测试版-vX.Y.Z-beta.N.apk`。
4. 生成 `updates/stable.json` / `beta.json`，用 GPG 对清单原始字节做 detached 签名；导出公钥。正式版没有 GPG 私钥时直接失败，不发未签名正式版。
5. 打 tag `vX.Y.Z`，创建 GitHub Release；正式版 `prerelease=false`，测试版 `prerelease=true`；上传 APK、`.sha256`、清单、`.asc`、公钥。
6. `pages` workflow 部署官网与清单；等待 Pages 生效（通常 < 2 分钟）。
7. GitHub 单源验收：Release 资产可下载；官网 CTA 指向 GitHub 直链；应用内 24h 检查能读到新清单并通过 GPG 验签；历史版本列表可见。
8. 大陆网络如无法访问 GitHub，记录为已知风险；不阻断发布，提供手动下载说明。
9. 归档发布记录：tag、commit、CI run、APK SHA256、GPG Key ID/验签日志、Release URL、Pages URL。

#### 5.9.9 构建、GPG 清单签名与客户端验签（补充）

**发布流水线**：新增 `.github/workflows/release.yml`，触发条件为推送 `v*` tag 或 `workflow_dispatch`；权限最小化为 `contents: write`，挂在 `production` Environment 下，使用与同步任务相同的人工批准门。

| 步骤 | 动作 | 关键校验 |
|---|---|---|
| 1 | Checkout、JDK 21、Android SDK、Gradle 缓存 | 版本号与 tag 预检 |
| 2 | 从 Secrets 解码 keystore，执行 `assembleRelease` | 不允许 debug 签名；R8/资源混淆按 P7-05 |
| 3 | `apksigner verify --print-certs`、提取 `versionName`/`versionCode`、重命名、`sha256sum` | APK 可安装、证书指纹固定、文件名合规 |
| 4 | 生成 `updates/{channel}.json`：版本、大小、SHA-256、GitHub 直链、`urls[]`、`minSupportedVersionCode` | schema 校验；URL 使用最终资产地址 |
| 5 | 导入 GPG 私钥，`gpg --armor --detach-sign` 生成 `.asc`，本地 `gpg --verify`，导出公钥 | 正式版无 GPG 直接失败；签名覆盖清单原始字节 |
| 6 | 创建/更新 GitHub Release，上传 APK、`.sha256`、清单、`.asc`、公钥 | 资产不覆盖旧 tag；同 tag 重跑先比对 SHA |
| 7 | 触发 `pages`，归档校验结果 | Release SHA、GPG 签名、Pages 部署三处一致 |

关键命令基线（在 runner 上执行，私钥只落在临时 keyring）：

```bash
gpg --batch --yes --import "$GPG_PRIVATE_KEY_FILE"
gpg --batch --yes --pinentry-mode loopback --passphrase "$GPG_PASSPHRASE" \
  --local-user "$GPG_KEY_ID" --armor --detach-sign \
  --output updates/stable.json.asc updates/stable.json
gpg --verify updates/stable.json.asc updates/stable.json
gpg --armor --export "$GPG_KEY_ID" > updates/softseek-update-signing-key.asc
```

**客户端信任存储与校验**：

- 公钥不是秘密，可以内置；但**私钥、keystore、token、对称密钥绝不硬编码**。公钥由 `security/trusted-keys/*.asc` 经构建脚本生成 `app/src/main/res/raw/update_trusted_keys.json`，不在 Kotlin 源码里散落字符串常量。
- APK 签名证书的 SHA-256 指纹由 release keystore 生成，写入 `res/values/security.xml` 或构建期 `BuildConfig`；它同样是公开指纹，不是私钥。
- APK 证书轮换优先使用 Android signing certificate lineage；不支持 lineage 的旧版本，先发一个内置“新旧双指纹”的过渡版，再切换新证书，避免老客户端把正常升级判成签名异常。
- 支持公钥环：`signingKeyId` 必须命中受信列表；未知 Key ID、已撤销 Key、`signatureStatus=unsigned` 的 stable 清单一律拒绝。
- 校验顺序固定为：**清单原始字节 GPG 验签 → schema/通道/versionCode 检查 → 下载 APK 的 size+SHA-256 → APK 证书指纹比对 → 允许弹安装**。清单与 `.asc` 各限制 256KB，APK 超过清单 `size` 或超出上限直接拒绝。证书读取使用 `PackageManager.getPackageArchiveInfo(..., GET_SIGNING_CERTIFICATES)`；Android 11+ 不需要旧 `GET_SIGNATURES` 兼容路径。
- “静默”指校验过程不打断用户；Android 不允许真正的静默安装，最后一步仍由用户确认安装（首次需允许未知来源）。不提供“校验失败仍安装”按钮。

| 校验失败场景 | 行为 |
|---|---|
| 清单 `.asc` 缺失/签名错误/未知 Key | 删除缓存、拦截、提示从官网手动下载；不上报 Key 内容 |
| 清单 JSON 被改但签名未更新 | 验签失败，拦截 |
| APK SHA-256 或大小不符 | 删除 APK，拦截 |
| APK 证书指纹与内置指纹不符 | 删除 APK，拦截；提示签名异常 |
| `versionCode` 低于当前/低于 `minSupportedVersionCode` | 拒绝降级；强制更新场景只给官网入口 |
| 网络中断/超时 | 保留上次已签名清单，不弹未校验更新 |

**密钥边界**：

| 材料 | 存放位置 | 是否可公开 |
|---|---|---|
| Android release keystore | GitHub Secrets / 本地发布机，不进仓库 | 否 |
| GPG 私钥与口令 | GitHub Secrets，job 结束删除临时 keyring | 否 |
| GPG 公钥 | App `res/raw`、官网 `updates/` | 是 |
| APK 证书 SHA-256 | App 资源/`BuildConfig`、官网展示 | 是 |

**验收**：正式版无 GPG 时 CI 必须失败；`gpg --verify` 与 `apksigner verify` 日志归档；篡改清单、篡改 APK、错误公钥、证书不匹配四类负向用例全部被拦截；密钥轮换时旧新双钥可验证，撤销后旧签名被拒。

#### 5.9.10 P7 增量任务与验收

| ID | 任务 | 关键产出 | 依赖 | 人日 | 验收 |
|---|---|---|---|---|---|
| P7-10 | 清单与命名规范 | `updates/*.json` schema、命名校验脚本、CI 版本一致性检查 | P7-05 | 1.0 | 资产名/tag/versionCode/清单四者一致 |
| P7-11 | 应用内升级检查 | WorkManager 24h、GitHub Pages 清单、DataStore 状态、手动检查 | P7-05 | 1.5 | 24h 唯一任务；清单超时保留 pending；304 不重复提示 |
| P7-12 | 升级提醒 UI | 应用内弹窗、稍后/跳过/强制更新、下载跳转与 SHA 提示 | P7-17 | 1.0 | 同版本只提示一次；跳过后新版本恢复；强制更新不可跳过 |
| P7-13 | 官网工程与 Pages | `website/` Vite 工程、`pages.yml`、base path、明暗主题 | P7-05 | 1.0 | Pages URL 可访问；子路径资源不 404 |
| P7-14 | 官网内容 | 介绍/功能/API 指南/使用指南/隐私/FAQ 详细页 | P7-13 | 1.5 | 页面清单全覆盖；新用户照文档可完成首次对话 |
| P7-15 | 下载中心与历史版本 | 最新正式版、历史版本、GitHub API 索引、直链/复制 | P7-13 | 1.5 | GitHub 直链不跳落地页；历史≥3 版；签名状态可见 |
| P7-16 | Release 构建、GPG 清单签名与上传 | `release.yml`、APK 签名/SHA、清单 `.asc`、Release 资产 | P7-05/P7-10 | 1.0 | 正式版无 GPG 失败；`gpg --verify` 通过；资产可下载 |
| P7-17 | 客户端验签、哈希与证书校验 | 受信公钥环、`UpdateVerifier`、失败拦截、负向测试 | P7-11 | 1.5 | 篡改清单/APK/证书不匹配全部拦截；不静默安装 |
| **合计** | | | | **10.0** | |

**新增验收证据**：

- GitHub Release 页面截图 + 资产 SHA256/清单签名对比。
- `stable.json` / `beta.json` 原始文件、`.asc` 签名与 schema 校验日志。
- `gpg --verify`、`apksigner verify --print-certs` 输出；正式版无 GPG 时 CI 失败记录。
- 篡改清单、篡改 APK、错误公钥、证书不匹配四组负向用例的拦截截图/日志。
- 应用内更新检查的 WorkManager 日志、24h 抑制测试记录。
- Pages 部署 run、官网桌面/移动截图、Lighthouse 报告。
- GitHub 不可达时的手动下载提示截图（可选，记录为已知风险）。
- 发布流水线检查记录：`release.yml` 可独立完成发布，无 `release-sync.yml`、无 GITEE Secrets 依赖。
- 历史版本列表截图与至少一次旧版本回退安装记录。

### 5.10 工作量汇总

| 阶段 | 人日 | 占比 | 主要风险 |
|---|---|---|---|
| P0 工具链/契约 | 4.5 | 4.9% | 环境、Key 可用性 |
| P1 工程基础 | 6.0 | 6.6% | 低 |
| P2 数据/记忆池 | 9.0 | 9.9% | 清理规则、附件模型 |
| P3 API/聊天 | 15.0 | 16.5% | SSE、Markdown 库 |
| P4 多模态/设置 | 14.0 | 15.4% | Key 加密、配置一致性 |
| P5 通话 | 15.5 | 17.0% | 回声、16KB、贴脸熄屏 |
| P6 迁移/适配 | 10.0 | 11.0% | 官方格式、性能 |
| P7 测试/发布（含升级/官网/签名） | 17.0 | 18.7% | 回归、合规、密钥管理、单源可用性 |
| **合计** | **91.0** | 100% | 含 20% 缓冲约 **109.2 人日** |

> P7 原 7.0 人日为测试、签名与 GitHub 发布基线；本次新增 10.0 人日为 P7-10～P7-17（24h 升级检查、GitHub Pages 清单、Vite 官网、下载中心、供应链签名与客户端验签）。CDN/OSS 不进入本期，流量增长后另立任务。

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
| M7 可发布 | 91.0 | W19～W20 | RC、APK/AAB、GitHub Release、Vite 官网、更新清单、GPG 签名与验签 | 阶段 7 含 §5.9 验收项全过 |

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
| 本次新增 §5.9 发布/升级 | P7-10～P7-17 | GitHub Release SHA/签名、GPG 验签、24h 检查日志、Pages 部署、历史版本回退记录 |

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
| D8 | ZL-01 视觉/配置/TTS 方向 | 十套视觉风格；Provider+Key；本地 TTS 范围；通话隔离；头像回退；动画状态；列表转场 | **已确认按 §11.3 的 ZL-D1～D7 执行**：默认极简，十套风格可切换并持久化；先选 Provider 再填 Key；本期系统 TTS + 外部 TTS + 本地导入壳；`tags=call`；私有副本回退；业务状态 StateFlow、逐帧 Animatable；PullToRefreshBox + 选择性共享元素 | 若在批次 B/C 中途改方向，会造成设置页、Provider 与通话页返工 |
| D9 | 发布源 | A GitHub 单源；B GitHub+Gitee 双源；C GitHub+CDN/OSS | **已确认 A：P7 只做 GitHub Releases + GitHub Pages；取消 Gitee 仓库/Token/同步 workflow；CDN/OSS 等流量增长后另立任务** | 国内 GitHub 访问不稳是已知风险；不因此阻断本期开发 |

> 说明：D1/D2/D3 为开工前明确确认；D4～D6 按推荐默认执行，如需调整请在进入对应阶段前提出；D7 已按 GitHub owner 确定，发布前冻结；D8 为 ZL-01 的增量任务决策，2026-11-19 已确认；D9 为发布源决策，2026-11-20 已确认。

> GitHub：owner `we007oneday-byte`，建议仓库名 `SoftSeek`（公开、MIT）；仓库创建需先完成 GitHub 授权，安排在 P0-03 或 P7-06 执行。

---

## 10. 下一步

1. **决策已回填**：D1/D2/D3 已确认；D4～D6 按推荐默认执行；D7 包名定为 `io.github.we007oneday_byte.softseek`；D8 按 ZL-01 §11.3 执行；D9 发布源定为 GitHub 单源，Gitee/CDN/OSS 后置。
2. **执行 P0-01～P0-04**：装 JDK 21/SDK 36/Git，生成空壳工程并跑通 `assembleDebug`。
3. **执行 P0-05**：用一个真实 API Key 跑 5 组冒烟；Key 只放本机临时环境变量，不写代码、不进日志、不提交仓库。
4. **进入 P1**：主题 → 导航 → 抽屉 → 通用组件，按阶段 1 验收。
5. 环境就绪后，我可以直接在 `D:\DeepSeekAdK` 生成阶段 1 的工程骨架（Gradle 配置、包结构、主题、导航、空壳页面）。
6. **下一大任务 ZL-01**：P5-14 真机验收收尾后，从 §11 的批次 A（黑白视觉底座）开始；批次 A 不动数据层。

---

## 11. 增量任务 ZL-01（P0，下一大任务）

| 项目 | 内容 |
|---|---|
| 任务编号 | **ZL-01** |
| 优先级 | **P0**（优先级定义，不是阶段编号 P0） |
| 任务性质 | 增量开发，不推翻 P1～P5 已有基线 |
| 当前状态 | 需求、方案与 1～3 项关键决策已确认；2026-11-20 从批次 A 开工 |
| 计划文档 | `docs/ZL-01_增量开发任务与实施计划.md`（含 §7 TTS 选型评估） |
| 基线提交 | `ec9a3c3 fix(P5): request microphone permission and harden offline model install` |
| 独立估算 | 约 **12～18 人日**；不修改原 §0.3 的 91.0 人日基线，待排期确认后合并 |

### 11.1 目标与边界

ZL-01 解决的是“能用之后好不好用”的产品层问题，不再继续堆通话底层：

1. **视觉统一**：把高饱和紫罗兰改为黑白灰单一色系，统一间距、圆角、字号、层级与组件；错误/危险保留唯一红色语义色。
2. **品牌统一**：App 图标、新对话页、AI 通话头像、GitHub 仓库/Release 使用 `APPICONS/` 下的 SOFTSEEK 资产。
3. **信息架构**：设置页从单页分组 + 大量弹窗，改为一级菜单 + 二级页面；聊天底部从“API Key 检测”改为模型/思考强度/最大输出。
4. **模型与 Provider**：支持 OpenAI-compatible 与 Anthropic Messages 协议；用户只填 Key，其余自动填充；模型档案可增删改。
5. **AI 通话独立**：通话会话不再混入文字聊天列表；通话有独立历史、头像、语音输入动画和 TTS 选择。
6. **离线模型管理**：Vosk 从单模型升级为模型目录；可选择、删除模型与安装包。
7. **十套视觉风格**：在极简默认之外，提供云朵、布丁、黑白、太极、黑客、线条、糖果、霓虹、纸墨等可切换布局/配色风格，统一走 `VisualStyleSpec`。
8. **品牌开屏**：系统 Splash 后播放 SK 标记动画，0.8～1.2 秒，只在冷启动播放，可跳过。

**明确不在 ZL-01 内**：

- 不要求配置真实 API Key；Provider 链路用 mock/本地假 Key 验证，真实 Key 部署另开任务。
- 不接完整本地 TTS 推理运行时；本地模型只做导入、校验、试听壳，sherpa-onnx 中文模型另立子任务。
- 不做名人音色库、不做未经授权的声纹克隆。
- 不改 P6 的导入导出/16KB 适配，不改 P7 的发布门禁；ZL-01 完成后仍按 P6/P7 走发布。

### 11.2 批次与交付物

| 批次 | 范围 | 主要交付物 | 估算 |
|---|---|---|---|
| **A 视觉底座** | 黑白 token、十套风格底座、组件统一、App 图标、开屏动画、新对话页 LOGO、Navigation 路由与转场、设置一级菜单骨架 | 可安装的多风格视觉版本；浅/深色截图走查；图标真机检查 | 3～4 人日 |
| **B 配置与模型底座** | ProviderProfile、OpenAI/Anthropic 适配、只填 Key、ModelProfile、底部模型条、设置二级页、Vosk 多模型 | 可切换 Provider/模型；可新增/删除模型档案；可切换/删除离线模型 | 4～6 人日 |
| **C AI 通话** | `tags=call` 会话隔离、通话历史、头像裁剪/回退、10 种语音输入动画、字幕入场/高亮/收束、TTS 后端选择 | 通话独立列表；头像可换可回退；动画可选；字幕与收音动画流畅 | 4～6 人日 |
| **D 动效与验收** | PullToRefreshBox、Shimmer、Coil 渐入、Lottie、更新公告、GitHub 品牌、全量回归 | 列表四态闭环；更新公告页；README/Release 品牌更新；质量门通过 | 2～3 人日 |

### 11.3 已确认决策（2026-11-19）

| 编号 | 决策 | 结论 |
|---|---|---|
| ZL-D1 | 极简主题默认 | 近白底、细线分隔、紧凑布局、深色主按钮；十套风格（云朵/布丁/黑白/太极/极简/黑客/线条/糖果/霓虹/纸墨）可切换并持久化；保留深色与跟随系统；仅错误/危险保留红色语义色 |
| ZL-D2 | API Key 配置 | 先选 provider + 只填 Key；`sk-ant-` 优先 Anthropic，其余探测 OpenAI-compatible `/models`；失败回退手动选择 |
| ZL-D3 | 本地 TTS 范围 | 本期系统 TTS + 外部 OpenAI-compatible TTS + 本地导入壳；sherpa-onnx 另立子任务 |
| ZL-D4 | 通话会话隔离 | `tags=call`，不新增 Room 字段/表；文字列表过滤，通话历史只看 `call` |
| ZL-D5 | 头像回退 | 私有副本缺失/损坏才回退默认；删除相册原图不影响头像 |
| ZL-D6 | 动画状态 | 业务状态 `StateFlow`；逐帧进度 Compose `Animatable`，不回写 ViewModel |
| ZL-D7 | 列表与转场 | Material3 `PullToRefreshBox`；共享元素只用于 LOGO/头像，页面整体 `AnimatedContent` |
| ZL-D8 | 开屏动画 | 系统 Splash 后播放 SK 标记动画，0.8～1.2 秒；仅冷启动，可跳过；不阻塞进入 |
| ZL-D9 | 十套视觉风格 | 云朵、布丁、黑白、太极、极简、黑客、线条、糖果、霓虹、纸墨；每套定义布局+配色，默认极简，可切换持久化 |

### 11.4 验收口径

- 所有 ZL-01-01～23 有实现或明确降级说明，需求编号见计划文档 §1。
- 浅色/深色、主要页面、通话链路、模型切换、API 配置全量手工回归；真机由用户手动测试，不依赖 ADB。
- 单测覆盖 Provider 适配、模型配置、通话标签过滤、头像回退、动画选择持久化。
- `ktlintCheck lintDebug testDebugUnitTest assembleDebug` 全通过。
- APK 体积不因 ZL-01 明显膨胀；本地 TTS 模型必须按需下载，不内置大模型。
- README、更新公告、Release 说明同步；任务板 #40 更新为完成。

### 11.5 TTS 选型结论

- **CloneTTS**：中文/通话/克隆/NPU 方向最合适，但 GitHub 仓库只有 README 与 docs、1 个 commit、无源码、无 LICENSE；不能 fork 或内置，只能作为“外部已安装 TTS/本地 HTTP TTS”兼容目标。
- **NekoSpeak**：MIT 的完整 Android Kotlin/Compose 工程，架构可参考；但它是完整 App 而非库，arm64 APK 约 88MB、universal 约 135MB，中文能力主要在 v1.5.0 预发布（OmniVoice 约 3GB，tokenizer 有限制），且 eSpeak-NG 为 GPLv3、模型许可证混合、含名人音色库；不整包引入。
- **SOFTSEEK 路线**：ZL-01 只做系统 TTS + 外部 OpenAI-compatible TTS + 本地导入壳；后续本地 TTS 采用 sherpa-onnx + 中文 Piper/VITS 模型（如 `zh_CN-huayan`，约 84MB 级，按需下载）。

### 11.6 开工顺序

1. 当前从批次 A 开始：先做主题 token + 十套风格底座 + 图标 + 开屏动画 + 路由骨架，不改数据层。
2. 批次 A 通过真机视觉走查后，再进 B；B 的 Provider/模型配置必须先于 C 的通话模型选择。
3. C 的通话会话隔离先用 `tags=call` 落地，不做 Room 迁移。
4. D 只做动效与品牌收尾，不回头改 A/B 的架构。

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
| P7-10 | P7 | 清单与命名规范 | 待开始 | | |
| P7-11 | P7 | 应用内升级检查 | 待开始 | | |
| P7-12 | P7 | 升级提醒 UI | 待开始 | | |
| P7-13 | P7 | 官网工程/Pages | 待开始 | | |
| P7-14 | P7 | 官网内容 | 待开始 | | |
| P7-15 | P7 | 下载中心/历史版本 | 待开始 | | |
| P7-16 | P7 | Release 构建/GPG 签名 | 待开始 | | |
| P7-17 | P7 | 客户端验签/哈希/证书校验 | 待开始 | | |
| ZL-01 | ZL | UI/UX、配置与 AI 通话增量（P0） | 计划中 | #40 | 2026-11-20 批次 A |

---

**评审人签名：** ________　　**开发执行人签名：** ________　　**日期：** 2026-11-19
