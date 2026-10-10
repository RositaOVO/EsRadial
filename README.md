# EsRadial

用 ApricityUI 实现的通用 Minecraft 轮盘库。当前支持 Forge 1.20.1、Java 17、ApricityUI 1.2.6。

默认外观参考 Squad：半透明深灰圆环、分隔线、简单图标、浅灰选中区域和轮盘下方的文字。使用用户提供的 UI.zip 目录和工事图标，没有 AuraTip 依赖。

## 玩家操作

- 按住调用模组的轮盘键打开；向图标所在方向移动鼠标即可选中，移到圆环外也有效。
- 左键点击执行；点击菜单入口进入子菜单，保持同一个 Overlay，不会关了再闪一下。
- 松开打开键只关闭轮盘，不执行选项。库也支持由方块右键打开的普通点击模式。
- 右键返回上一级；根页面右键关闭。Esc、死亡、断线、打开其他 Screen、失去窗口焦点也会结束会话。
- 中心是安全区，不选中外围选项。Espetro 的载具中心额外由原有上车系统提供左键读条。
- 持续操作选项支持按住左键重复执行；松开左键、移到中心或不可用项目就停止。
- 外侧线仅在悬停时显示所选外弧。进度默认贴着内侧圆圈，悬停到对应动作时显示在外弧。
- 扇区可以大小不同并保留空白；空白区域不会选中、执行或继续重复操作。
- 不可用项变淡，显示原因，不执行回调。费用、人数、余额等由调用模组提供。

## 自己调整轮盘

打开轮盘后按 **F6**，拖动图标所在扇区或空槽调整位置，拖动黄色分界点所在的线调整大小，滚轮旋转整个轮盘。
**Tab** 切换 30°/45°、30°、45°、自由吸附；默认吸附到最近的 30°或45°倍数。
**Enter** 保存，**Esc** 取消，**R** 恢复默认（再按 Enter 保存）。编辑时松开打开键不会确认动作。
配置文件为客户端 `config/esradial/layouts.json`，修改后下次打开轮盘生效。
每个菜单和不同选项集合分别保存，布局按稳定按钮 ID 匹配，服务端规则不变。
详见 [布局配置与拖动操作](docs/LAYOUTS.md)。

共享指挥入口、分类目录和吸附说明见 [分类轮盘](docs/SQUAD-TREE.md)。

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
        () -> RadialMenuClientApi.navigate(build), Component.literal("建造"), "#FFFFFFFF").submenuLast()
    .build();
RadialMenuClientApi.open(root,
    RadialMenuClientApi.OpenOptions.hold("mymod", () -> wheelKey.isDown()));
```

这些方法需要 import `org.esradial.client.*`、`net.minecraft.resources.ResourceLocation`、`net.minecraft.network.chat.Component`。回调里的网络请求和 `wheelKey` 来自你的模组。

持续装卸用 `.persistentSlot(...).repeatLast(intervalTicks)`；不可用项目追加 `.disabledLast(Component.literal("弹药不足"))`。`intervalTicks` 必须来自游戏规则，服务端仍校验权限、距离、余额和请求频率。

需要固定位置时，在每个按钮后追加 `.sectorLast(startDegrees, sweepDegrees)`：角度从正上方向顺时针计算，跨过零度也有效。例如 `.sectorLast(-30, 55)` 和 `.sectorLast(25, 45)` 分别是大小不同的两个按钮。未指定的角度自动留空，也可以用 `.gap(130, 40)` 明确保留空位。一个页面开始指定角度后，每个按钮都需要指定一次；按钮索引保持连续，空位不占按钮索引。未使用这些方法的页面仍默认均分。

动态目录可以直接追加 `.squadLayout()`，自动生成不等大小的按钮扇区和两块空白区域，支持 1 到 64 个按钮。显式指定扇区时优先使用指定角度。Espetro 的指挥、建造、技能、职业和补给页面均已启用这一预设；载具页保持固定布局。

真实游戏读条可由 `.progress(() -> new RadialMenuData.Progress("button_id", value, "#FFFF4A4A"))` 提供，`value` 必须是 0 到 1。未悬停这个按钮时画在内圈，悬停时移到对应外弧；中心上车等操作使用 `null` 作为按钮 ID。没有进行操作时只显示轨道，不伪造进度。库内持续操作的外弧直接使用会话的重复计时，不额外建立计时器。

每次开始按键会话只调用一次 `open`，用其 boolean 返回值判断是否打开成功。一次点击执行并关闭后，要等玩家松键再允许新一轮打开，不能每 tick 无限重开。`open` 会拒绝抢占其他模组当前的轮盘。

使用 `navigate(data)` 切页面并记录返回路径，`back()` 返回，`replace(data)` 更新同一页数据。使用 `close("mymod")` 只关闭自己拥有的轮盘；`isOwnedBy` 可查询所有者。从载具根菜单进入其他模块的页面时，原来的按键会话和所有者继续有效。

可选 `RadialMenuRegistry.setMenus(owner, menus)` 管理静态菜单。更新页面保留稳定的按钮 ID；不要每 tick 重建页面。一次会话持有一个 AUI Document，资源重载后重新绑定失效的 DOM 节点。

## 以后换 UI

默认资源逻辑路径是 `esradial/radial.html`。资源放在 `assets/apricityui/apricity/esradial/`，兼容 AUI 1.2.6 的 classpath 资源回退路径。

可在游戏目录的 `apricity/esradial/` 用同名 HTML/CSS 覆盖默认外观。保留 `radial`、`ring`（canvas）、`slots`、`label`、`denial`、`center-title`、`breadcrumb`、`center-back` 的 DOM ID 和页面 meta。改变颜色、字体、边距不需要改 Espetro 的网络代码。

大幅改布局时，客户端可调用 `setRendererFactory(() -> new AuiRadialRenderer("mymod/custom.html"))`，或实现 `RadialRenderer`。命中检测与绘制必须使用同一逻辑坐标系。

## 构建

```text
./gradlew :core:test --configure-on-demand
./gradlew :forge:build
./gradlew :forge:publishToMavenLocal
```

运行 jar 在 `forge/build/libs/`。最终 Forge jar 已包含 core；安装时只需它与 ApricityUI 1.2.6。开发依赖坐标为 `org.esradial:esradial-forge-1.20.1:0.3.0`，本地接入先执行发布任务。

库的服务端引导不加载客户端类，AUI 前置只在客户端要求。Espetro 服务端的权限及业务逻辑未迁移到库里。

## 验证状态

39项核心与布局配置测试通过，包含30°/45°倍数吸附、跨0°边界、空槽、分类点击及松键取消。Forge/AUI源码已使用Java17、真实Forge映射和ApricityUI1.2.6编译。

实际Minecraft联机客户端验证了目录下钻、图标、松键取消、编辑拖动、保存/取消、配置重读与恢复默认；地图嵌入轮盘验证打开、Esc退出及父界面关闭后清理Document。EsPoints另有15项相关测试、Espetro15项适配/权限分类测试通过。

Espetro试玩包使用历史完整基线32d67bb接入本次控制器与载具快照API；PR基于最新1.20.1代码。最新4ae5012仍缺FixedWeaponWheelPacket源码和DragonRise依赖，完整编译未通过。试玩包不能等同于最新分支正式发行包。本机ForgeGradle依赖元数据解析仍有限制，测试包使用Java17、Mixin注解处理与Forge名称映射构建。
