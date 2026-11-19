# SOFTSEEK

SOFTSEEK（SK）是一个纯 Android 原生、Kotlin + Jetpack Compose 的 DeepSeek 第三方 API 客户端。

> 本项目为第三方客户端，非 DeepSeek 官方应用。所有 AI 服务由 DeepSeek 官方提供，应用本身不提供任何代理、中转或收费服务。

## 当前状态

**阶段 5：通话基础（P5，进行中）**

P5 已完成：

- 通话权限基线：`RECORD_AUDIO`、`POST_NOTIFICATIONS`、`FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_MICROPHONE`、`MODIFY_AUDIO_SETTINGS`、`WAKE_LOCK`；麦克风声明为可选硬件
- Vosk 0.3.47 接入；`AsrEngine` 稳定接口（partial/final/error）
- `VoskModelSpec` 固化 small-cn 42MB 模型 URL、大小 `43,898,754`、SHA-256 `3af8b0e7…5b14ba` 与必需文件清单
- 模型管理：Range 断点续传、大小/SHA-256 校验、zip slip/条目数量/解压体积防护、staging 解压后原子替换；失败保留 `.part`
- 设置页“离线识别模型”支持下载进度、取消、失败重试；模型安装在 `filesDir/models/vosk-model-small-cn-0.22`
- Vosk 流式识别：`Recognizer.acceptWaveForm` 端点检测、partial 去重、final 输出、句尾 reset、错误事件
- AudioRecord：16kHz/16bit/mono，优先 `VOICE_COMMUNICATION`，回退 `MIC`，可用时显式启用 `AcousticEchoCanceler`
- 系统 TTS 封装：中文语言检测、语速/音调、stop/release、逐句回调；设置页提供试听入口
- 通话状态机：Dialing/Listening/Thinking/Speaking/Ended/Error，半双工录音；ASR Final 触发 `ChatRepository` 流式回复，回复完成后系统 TTS 播报，播报结束回到 Listening；支持键盘输入、静音、挂断释放
- 通话页：麦克风权限请求、状态/时长、partial 与双方字幕、静音/键盘/挂断按钮、键盘发送、错误提示
- 新增 10 项模型/结果解析单测；当前 50 项单测、Lint 0 issue、debug APK 可构建

P5 尚未接入：分句与打断、音频模式/焦点、前台服务、接近传感器、降档/归档、真机测试。

**阶段 4：多模态、模型与设置（P4，已完成）**

已完成：

- Android 工程骨架、Gradle 版本目录、Compose 主题与导航
- Hilt 依赖注入（`@HiltAndroidApp`、`AppInfo`）
- 主题：跟随系统 / 浅色 / 深色，动态取色、字号缩放、气泡圆角、代码块主题
- 自适应启动图标（含 monochrome）与 SplashScreen
- Room 四张表：`memory_pool_blocks`、`memory_heap_nodes`、`message_attachments`、`memory_pool_config`
- 外键级联、`tags` JSON 转换、枚举转换、`schema` 导出到 `app/schemas/`
- 块/节点/附件/配置 DAO 与 `ConversationRepository`、`MemoryPoolRepository`
- 池块生命周期：新建、重命名、置顶、软删、恢复
- 清理引擎：自动清理默认关闭；手动 `cleanupNow(preview)` 支持预览/取消；阈值 2 条、保护期 24h、置顶排除
- 回收站：`deletedAt` 生效，每日 Worker 永久删除超过 7 天的池块
- 上下文拼装：系统提示、倒序截断、剥离 reasoning、图片按 384 tokens 估算
- Token 估算与 usage 校准
- DataStore 设置体系：单 JSON 键持久化，8 组设置即时生效、重启保留
- API Key：Android Keystore AES/GCM 加密、多 Key、备注、切换、删除，明文不落盘
- 模型：`/models` 刷新与缓存，`deepseek-flash`（文本+图片）与 `deepseek-v4-pro`（纯文本）能力灰显
- 模型/思考档位/最大输出可在会话内切换；参数预设保存、应用、删除、JSON 导出
- 图片：PhotoPicker 多选、EXIF 旋转、长边 1568、JPEG 85、最多 8 张、100MB 缓存上限
- 视觉请求：`content` 数组 + `image_url.detail`，单图 6MB 体积校验；缩略图、全屏预览、待发送删除
- DeepSeek API Provider 抽象、DTO、Retrofit/OkHttp 网络层
- `POST /chat/completions` 流式 SSE 解析：content、reasoning_content、finish_reason、usage、`[DONE]`
- Base URL 强制 HTTPS；代理、连接/读取/写入超时可配置，下一次请求生效
- 余额、今日/本月/累计 token 用量与预警阈值
- 设置页 8 组：API / 模型 / 聊天 / 通话 / 记忆 / 外观 / 数据 / 关于
- 测试 40 项；整体行覆盖率 82.9%（704/849），分支 51.1%（135/264）
- Android Lint 0 issue、ktlint、JVM 单测、GitHub Actions CI

尚未接入：生物识别解锁 Key、会话导入导出、Vosk 语音与 TTS 通话。按任务书 P5～P7 推进。

## 技术栈

- Kotlin 2.0.20
- Jetpack Compose（BOM 2024.10.01，Material3）
- Navigation Compose 2.7.7
- Room 2.7.2 + KSP
- DataStore Preferences 1.1.1
- Coil 2.7.0、ExifInterface 1.3.7
- WorkManager 2.10.0 + Hilt Worker
- Retrofit 2.11.0 + OkHttp 4.12.0 + kotlinx.serialization 1.7.3
- Vosk Android 0.3.47（离线中文 small-cn 模型）
- AGP 8.9.0 / Gradle 8.11.1
- `minSdk 30`，`targetSdk 36`，`compileSdk 36`
- JDK 21

## 构建

前置条件：

1. JDK 21
2. Android SDK Platform 36、Build-Tools 36.0.0、Platform-Tools
3. `local.properties` 中配置 `sdk.dir`（该文件不纳入 Git）

Windows：

```powershell
$env:JAVA_HOME = "D:\Android\jdk21\jdk-21.0.13.1+2"
.\gradlew.bat assembleDebug
```

产物：

```
app/build/outputs/apk/debug/app-debug.apk
```

## 目录结构

```
app/src/main/java/io/github/we007oneday_byte/softseek/
├── ChatApp.kt
├── MainActivity.kt
├── core/
│   ├── asr/                # Vosk 模型规格/下载/校验/解压 + 流式识别
│   ├── audio/              # AudioRecord + AEC
│   ├── auth/               # Keystore AES/GCM 多 Key
│   ├── constant/AppConstants.kt
│   ├── image/              # ImageProcessor（压缩/缓存）
│   ├── model/AppInfo.kt
│   ├── network/            # BaseUrlValidator / DynamicApiFactory
│   ├── settings/           # AppSettings / ParameterPreset
│   └── tts/                # TtsEngine + 系统 TextToSpeech
├── data/
│   ├── cleanup/            # CleanupEngine
│   ├── local/
│   │   ├── dao/            # 块 / 节点 / 附件 / 配置 DAO
│   │   ├── entity/         # Room 实体与关系
│   │   ├── converter/      # tags / 枚举 TypeConverter
│   │   └── AppDatabase.kt
│   ├── remote/             # DeepSeekApi / DTO / SSE / Provider
│   ├── repository/         # Room 仓储 + DefaultChatRepository + UsageRepository
│   ├── settings/           # DataStore SettingsRepository
│   ├── model/              # ModelCatalog
│   └── worker/             # 回收站清理 Worker
├── di/                     # AppModule / DatabaseModule / NetworkModule / ProviderModule / AuthModule
├── domain/
│   ├── auth/               # ApiKeyStore
│   ├── context/            # ContextAssembler / TokenEstimator
│   ├── model/              # 领域模型
│   ├── provider/           # AiProvider / AiChatService
│   └── repository/         # 仓储接口
└── ui/
    ├── components/common/   # CommonButton / SettingItem / MarkdownText
    ├── navigation/          # AppNavGraph
    ├── screens/             # chat / settings / guide / call / about
    └── theme/               # Color / Type / Theme / ThemeMode
```

Room schema 导出目录：`app/schemas/io.github.we007oneday_byte.softseek.data.local.AppDatabase/1.json`。

## 质量检查

```powershell
.\gradlew.bat ktlintCheck lintDebug testDebugUnitTest jacocoDebugReport assembleDebug
```

覆盖率报告：`app/build/reports/jacoco/jacocoDebugReport/html/index.html`。

CI 工作流见 `.github/workflows/android.yml`，在 push / PR 时执行构建、lint 与单测。

## 设计约束

- 无 WebView：所有界面使用 Compose 原生实现
- 无自建服务端：客户端直连 DeepSeek 官方 API
- 无数据上传：API Key、聊天记录、记忆池仅保存在本机
- 最小权限：按功能申请，不申请无关权限
- 全 Kotlin：不引入 Java/Kotlin 混合实现

## 文档

需求分析与开发任务书见 [`docs/SOFTSEEK_需求分析与开发任务书.md`](docs/SOFTSEEK_需求分析与开发任务书.md)。

## License

MIT
