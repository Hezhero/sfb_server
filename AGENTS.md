## Agent skills

### Issue tracker

Issue 与规格以本地 markdown 文件形式存放在本仓库 `.scratch/<feature>/` 下。详见 `docs/agents/issue-tracker.md`。

### Triage labels

使用五个规范分诊角色，标签字符串与角色名一致（`needs-triage`、`needs-info`、`ready-for-agent`、`ready-for-human`、`wontfix`）。详见 `docs/agents/triage-labels.md`。

### Domain docs

单上下文：仓库根目录一份 `GLOSSARY.md` 加 `docs/adr/`。详见 `docs/agents/domain.md`。

### 改代码或跑测试前

涉及外部 API 参数、订单实体加字段、明文配置、行尾符或接手半成品时，先读 `docs/agents/working-conventions.md`。

### 语言强制规则
1. 所有对话、提问、总结、生成文档（GLOSSARY、ADR、spec、需求文档）**统一使用简体中文**。
2. 代码、命令、变量名、函数名、技术专有名词保留英文原文，禁止翻译。
3. 技能本身的英文流程指令可以正常执行，但产出物必须中文。
4. 禁止自动切换英文输出，即使skill源文件是英文。