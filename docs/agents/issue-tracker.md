# 任务与 Issue 跟踪：GitHub Issues

本仓库的任务、缺陷与 Spec 规范统一托管在 GitHub Issues。所有相关操作均通过 `gh` CLI 执行。

## 常用操作规范

- **创建 Issue**：`gh issue create --title "..." --body "..."`（多行内容推荐使用 heredoc 格式）。
- **查看 Issue 详情**：`gh issue view <number> --comments`（获取 Issue 正文、标签与全部评论）。
- **列出 Issue**：`gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'`，可附加 `--label` 与 `--state` 过滤。
- **添加评论**：`gh issue comment <number> --body "..."`
- **添加/移除标签**：`gh issue edit <number> --add-label "..."` / `--remove-label "..."`
- **关闭 Issue**：`gh issue close <number> --comment "..."`

> 💡 `gh` CLI 会自动根据当前目录的 `git remote -v` 解析关联仓库 (`atengk/mcp-server-rocketmq`)。

## Pull Request 分流与归类

**将外部 PR 作为需求入口：否** _（若后续需要将外部贡献者的 PR 作为需求进行统一分流，可将此项设为 `是`；`/triage` 技能会读取此配置）_。

## 技能协作交互规范

- 当技能提示 **“发布到任务跟踪器 (publish to the issue tracker)”** 时：创建一个 GitHub Issue。
- 当技能提示 **“获取相关工单 (fetch the relevant ticket)”** 时：执行 `gh issue view <number> --comments`。

## Wayfinding 导航与任务图编排

由 `/wayfinder` 技能消费。主图（Map）是一个带 `wayfinder:map` 标签的全局 Issue，子工单（Tickets）作为其关联任务：

- **主图 Issue**：标签为 `wayfinder:map` 的核心 Issue，维护排查笔记、已做决策与模糊待定区。
- **子任务工单**：作为 GitHub Sub-issues 关联到主图（若未开启 Sub-issues，则在主图正文的任务列表中索引并在子 Issue 顶部注明 `Part of #<map>`）。标签格式：`wayfinder:<type>`（如 `research` / `prototype` / `grilling` / `task`）。被认领后分配给对应开发人员。
- **依赖阻断**：使用 GitHub 原生依赖能力：`gh api --method POST repos/<owner>/<repo>/issues/<child>/dependencies/blocked_by -F issue_id=<blocker-db-id>`（其中 `<blocker-db-id>` 为被依赖 Issue 的数字 database id）。当原生依赖不可用时，在子工单正文顶部注明 `Blocked by: #<n>`。
- **可执行前沿检索**：列出主图的未关闭子工单，排除带有未关闭依赖阻断或已被分配的工单，按主图拓扑顺序推进。
- **认领任务**：`gh issue edit <n> --add-assignee @me`。
- **完结归档**：`gh issue comment <n> --body "<answer>"`，然后 `gh issue close <n>`，并将决策要点指针追加到主图的决策记录中。
