# SOFTSEEK

SOFTSEEK（SK）是一个纯 Android 原生、Kotlin + Jetpack Compose 的 DeepSeek 第三方 API 客户端。

> 本项目为第三方客户端，非 DeepSeek 官方应用。所有 AI 服务由 DeepSeek 官方提供，应用本身不提供任何代理、中转或收费服务。

## 当前状态

**阶段 3：API 接入与基础聊天（P3）**

已完成：

- Android 工程骨架、Gradle 版本目录、Compose 主题与导航
- Hilt 依赖注入（`@HiltAndroidApp`、`AppInfo`）
- 主题三模式（跟随系统 / 浅色 / 深色），设置页可循环切换
- 自适应启动图标（含 monochrome）与 SplashScreen
- Room 四张表：`memory_pool_blocks`、`memory_heap_nodes`、`message_attachments`、`memory_pool_config`
- 外键级联、`tags` JSON 转换、枚举转换、`schema` 导出到 `app/schemas/`
- 块/节点/附件/配置 DAO 与 `ConversationRepository`、`MemoryPoolRepository`
- 池块生命周期：新建、重命名、置顶、软删、恢复
- 清理引擎：自动清理默认关闭；手动 `cleanupNow(preview)` 支持预览/取消；阈值 2 条、保护期 24h、置顶排除
- 回收站：`deletedAt` 生效，每日 Worker 永久删除超过 7 天的池块
- 上下文拼装：系统提示、倒序截断、剥离 reasoning、图片按 384 tokens 估算
- Token 估算与 usage 校准
- DeepSeek API Provider 抽象、DTO、Retrofit/OkHttp 网络层
- `POST /chat/completions` 流式 SSE 解析：content、reasoning_content、finish_reason、usage、`[DONE]`
- Base URL 强制 HTTPS 校验；连接 15s、普通读取 60s、流式读取 0
- HTTP 错误映射：401/403、402、429、5xx、400 与网络/协议异常
- 聊天状态机：发送、流式增量、停止（保留 partial）、重新生成、失败落库
- 会话抽屉：列表、切换、新建、重命名、置顶、软删
- 消息气泡：思考块折叠、Markdown/代码块、复制/分享/编辑/删除、token 与错误展示
- 会话标题按首条消息本地截断；会话导出为纯文本分享
- 设置页 API Key 输入（当前版本仅保存在内存，重启后失效；P4-08 接 Keystore）
- Robolectric + Room in-memory + SSE/DTO/仓储测试 37 项；P3 整体行覆盖率 86.1%（681/791），分支 54.0%（134/248）
- Android Lint 0 issue、ktlint、JVM 单测、GitHub Actions CI

尚未接入：API Key 加密持久化、附件/图片消息、DataStore、Vosk、导入导出。按任务书 P4～P7 推进。

## 技术栈

- Kotlin 2.0.20
- Jetpack Compose（BOM 2024.10.01，Material3）
- Navigation Compose 2.7.7
- Room 2.7.2 + KSP
- WorkManager 2.10.0 + Hilt Worker
- Retrofit 2.11.0 + OkHttp 4.12.0 + kotlinx.serialization 1.7.3
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
│   ├── auth/               # P3 内存 API Key Store（P4 换 Keystore）
│   ├── constant/AppConstants.kt
│   ├── model/AppInfo.kt
│   └── network/            # BaseUrlValidator
├── data/
│   ├── cleanup/            # CleanupEngine
│   ├── local/
│   │   ├── dao/            # 块 / 节点 / 附件 / 配置 DAO
│   │   ├── entity/         # Room 实体与关系
│   │   ├── converter/      # tags / 枚举 TypeConverter
│   │   └── AppDatabase.kt
│   ├── remote/             # DeepSeekApi / DTO / SSE / Provider
│   ├── repository/         # Room 仓储 + DefaultChatRepository
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
