# SOFTSEEK

SOFTSEEK（SK）是一个纯 Android 原生、Kotlin + Jetpack Compose 的 DeepSeek 第三方 API 客户端。

> 本项目为第三方客户端，非 DeepSeek 官方应用。所有 AI 服务由 DeepSeek 官方提供，应用本身不提供任何代理、中转或收费服务。

## 当前状态

**阶段 1：可编译空壳（P1 工程基础）**

已完成：

- Android 工程骨架、Gradle 版本目录、Compose 主题与导航
- Hilt 依赖注入（`@HiltAndroidApp`、`AppInfo`）
- 主题三模式（跟随系统 / 浅色 / 深色），设置页可循环切换
- 聊天页空壳（会话抽屉、空状态、输入栏占位）
- 设置页占位（API / 模型 / 聊天 / 通话 / 记忆 / 外观 / 数据 / 关于）
- 设置项三种形态（普通 / 开关 / 值）、通用按钮、Markdown 占位
- 引导页、通话页、关于页占位
- 自适应启动图标（含 monochrome）与 SplashScreen
- Android Lint 0 error、JVM 单测、GitHub Actions CI

尚未接入：Room、Retrofit/OkHttp、DataStore、Vosk、导入导出。按任务书 P1～P7 推进。

## 技术栈

- Kotlin 2.0.20
- Jetpack Compose（BOM 2024.10.01，Material3）
- Navigation Compose 2.7.7
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
│   ├── constant/AppConstants.kt
│   └── model/AppInfo.kt
├── di/AppModule.kt
└── ui/
    ├── components/common/   # CommonButton / SettingItem / MarkdownText
    ├── navigation/          # AppNavGraph
    ├── screens/             # chat / settings / guide / call / about
    └── theme/               # Color / Type / Theme / ThemeMode
```

## 质量检查

```powershell
.\gradlew.bat ktlintCheck lintDebug testDebugUnitTest assembleDebug
```

CI 工作流见 `.github/workflows/android.yml`，在 push / PR 时执行同一组检查。

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
