# EsRadial

用 ApricityUI 实现的通用 Minecraft 轮盘库。当前支持 Forge 1.20.1、Java 17、ApricityUI 1.2.3.1。

默认外观参考 Squad：半透明深灰圆环、分隔线、简单图标、浅灰选中区域和轮盘下方的文字。没有打包 Squad 的图片、字体或音效，也没有 AuraTip 依赖。

## 玩家操作

- 按住调用模组的轮盘键打开；向图标所在方向移动鼠标即可选中，移到圆环外也有效。
- 左键点击执行；点击菜单入口进入子菜单，保持同一个 Overlay，不会关了再闪一下。
- 松开打开键只关闭轮盘，不执行选项。库也支持由方块右键打开的普通点击模式。
- 右键返回上一级；根页面右键关闭。Esc、死亡、断线、打开其他 Screen、失去窗口焦点也会结束会话。
- 中心是安全区，不选中外围选项。Espetro 的载具中心额外由原有上车系统提供左键读条。
- 持续操作选项支持按住左键重复执行；松开左键、移到中心或不可用项目就停止。
- 不可用项变淡，显示原因，不执行回调。费用、人数、余额等由调用模组提供。

## 模组作者怎么接

EsRadial 负责画轮盘、接鼠标、切换页面、持续操作和关闭后恢复鼠标。你的模组只提供按钮、图标、文字和点击后要做什么；真正的建造、换职、补给仍由各自服务端处理。

只在客户端初始化和调用下面的 API。不要让服务端类直接引用 `org.esradial.client`。

```java
var build = new RadialMenuBuilder(new ResourceLocation("mymod", "build"))
    .title(Component.literal("建造"))
    .slot("hab", new ResourceLocation("mymod", "textures/gui/hab.png"),
        () -> requestHabPlacement(), Component.literal("兵站 · 建材 500"), "#FFFFFFFF")
    .build();
var root = new RadialMenuBuilder(new ResourceLocation("mymod", "root"))
    .title(Component.literal("指挥菜单"))
    .slot("build", new ResourceLocation("mymod", "textures/gui/build.png"),
        () -> RadialMenuClientApi.navigate(build), Component.literal("建造"), "#FFFFFFFF")
    .build();
RadialMenuClientApi.open(root,
    RadialMenuClientApi.OpenOptions.hold("mymod", () -> wheelKey.isDown()));
```

这些方法需要 import `org.esradial.client.*`、`net.minecraft.resources.ResourceLocation`、`net.minecraft.network.chat.Component`。回调里的网络请求和 `wheelKey` 来自你的模组。

持续装卸用 `.persistentSlot(...).repeatLast(intervalTicks)`；不可用项目追加 `.disabledLast(Component.literal("弹药不足"))`。`intervalTicks` 必须来自游戏规则，服务端仍校验权限、距离、余额和请求频率。

每次开始按键会话只调用一次 `open`，用其 boolean 返回值判断是否打开成功。一次点击执行并关闭后，要等玩家松键再允许新一轮打开，不能每 tick 无限重开。`open` 会拒绝抢占其他模组当前的轮盘。

使用 `navigate(data)` 切页面并记录返回路径，`back()` 返回，`replace(data)` 更新同一页数据。使用 `close("mymod")` 只关闭自己拥有的轮盘；`isOwnedBy` 可查询所有者。从载具根菜单进入其他模块的页面时，原来的按键会话和所有者继续有效。

可选 `RadialMenuRegistry.setMenus(owner, menus)` 管理静态菜单。更新页面保留稳定的按钮 ID；不要每 tick 重建页面。一次会话持有一个 AUI Document，资源重载后重新绑定失效的 DOM 节点。

## 以后换 UI

默认资源逻辑路径是 `esradial/radial.html`。资源放在 `assets/apricityui/apricity/esradial/`，兼容 AUI 1.2.3.1 的 classpath 资源回退路径。

可在游戏目录的 `apricity/esradial/` 用同名 HTML/CSS 覆盖默认外观。保留 `radial`、`ring`（canvas）、`slots`、`label`、`denial` 的 DOM ID 和页面 meta。改变颜色、字体、边距不需要改 Espetro 的网络代码。

大幅改布局时，客户端可调用 `setRendererFactory(() -> new AuiRadialRenderer("mymod/custom.html"))`，或实现 `RadialRenderer`。命中检测与绘制必须使用同一逻辑坐标系。

## 构建

```text
./gradlew :core:test --configure-on-demand
./gradlew :forge:build
./gradlew :forge:publishToMavenLocal
```

运行 jar 在 `forge/build/libs/`。最终 Forge jar 已包含 core；安装时只需它与 ApricityUI 1.2.3.1。开发依赖坐标为 `org.esradial:esradial-forge-1.20.1:0.1.0`，本地接入先执行发布任务。

库的服务端引导不加载客户端类，AUI 前置只在客户端要求。Espetro 服务端的权限及业务逻辑未迁移到库里。

## 验证状态

14 项纯逻辑测试已通过，包括各页尺寸的角度命中、快速点击、不可用项、松键取消、导航、重复计时和关闭行为。Forge/AUI Java 源码已用真实 Forge 映射 jar 和 AUI 1.2.3.1 编译验证。

Espetro 1.20.1 的历史基线 32d67bb 接入后，完整源码 Java 编译及 11 项适配测试已通过。最新 427dbbd 缺少部分既有数据包类，附带单独的接入补丁。完整 Gradle 构建及游戏内截图尚未通过，本机 Gradle 无法解析 ForgeGradle 插件元数据。附带的预览 jar 使用 Java 17 编译及 Forge 的命名映射工具重映射，仍需装进实际客户端验证。不能把单元测试通过视为游戏内已验收。
