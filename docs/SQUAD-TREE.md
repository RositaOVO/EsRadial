# 分类轮盘与角度吸附（0.3.0）

打开 Espetro 指挥轮盘或 EsPoints 标点轮盘都会合并当前身份可用的入口。未安装某个模组时保留已安装模组的入口。

分类按钮带 `›`。左键进入下一层；右键或点击中心返回上一级；根菜单右键关闭。Esc 关闭整个菜单。松开唤出键取消选择。分类点击不执行建造或标点。

## 调整布局

F6 打开当前页的编辑器。拖动按钮/空槽调整顺序，拖动分界线改变相邻扇环大小。

默认吸附到最近的 **30°或45°倍数**：0、30、45、60、90、120、135、150……这不是每15°的网格。

Tab 依次切换：30°/45° → 30° → 45° → 自由调整。
滚轮旋转到下一个吸附位置；自由模式每次旋转5°。
如果某个吸附点会压扁相邻槽位，选择范围内最近的合法点；没有合法点时保持原分界。
Enter 保存，Esc 取消，R 恢复当前页默认布局后用 Enter 保存。

配置文件仍是 `config/esradial/layouts.json`，每个菜单及其权限目录独立保存。手动填写任意有效角度仍然支持；编辑器拖动时才应用当前吸附模式。
旧的同一页配置优先于新的默认几何；要使用新的默认排版，在该页按 F6、R、Enter。

## 模组接入

- `RadialMenuBuilder.submenuLast()` 标记目录，禁止松开键进入目录。
- `RadialCommandMenu.register(owner, rootSupplier, available, prepare)` 提供共享根入口；prepare只在打开前运行。
- `RadialCommandMenu.setOrder(slotId, priority)` 设置共享根入口顺序。
- 子菜单使用既有 `navigate` / `back`，同一 AUI Document 持续复用。
- 其他 Screen 可使用公开的 `RadialLayouts.apply` 读取同一布局配置。

界面素材来自用户提供的 UI.zip；地图图标由 EsPoints 使用 mark.zip、mark1.zip。分类目录图标与地图阵营图标各有用途。
