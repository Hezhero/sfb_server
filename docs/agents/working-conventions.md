# 本仓库的 Agent 工作约定

> AGENTS.md 的导航指针指向本文。这里只放「环境事实」与「必须遵守的流程」，
> 不放代码风格判断（那属于 CODING_STANDARDS.md，由 review 阶段读取）。

---

## 1. 测试使用本地真实 MySQL

**事实**：`mvn test` 连接 `127.0.0.1:3307/mshop`（见 `mshop-app/src/main/resources/config/application-dev.yml`），
不是内存库，也不是 H2。测试之间共享数据状态。

**后果与规则**：

- 给订单实体加字段时，MyBatis-Plus 的 `SELECT` 会带出所有列。
  `TkOrder` 是所有平台订单的基类，**7 张表**（tb/jd/pdd/dy/vip/mt/ele）都必须加列，
  否则报 `Unknown column 'xxx' in 'field list'`。
- 加列后：DDL 写进 `docs/sql/`，**先在本地库执行，再跑测试**。
- `mw_order_ele` 表结构不同（无 `spread_hb_time`），ALTER 时不要写 `AFTER spread_hb_time`。
- 测试数据隔离：不要用硬编码主键插入（库中可能有逻辑删除的同主键行，
  `saveOrUpdate` 因 `is_del=0` 条件查不到而走 insert，撞物理主键）。

**验证命令**（必须从仓库根目录、用 powershell）：

```bash
pwsh -NoProfile -Command "mvn test -o 2>&1 | Select-String -Pattern 'Tests run|BUILD|ERROR' | Select-Object -Last 20"
```

MSYS bash 直接跑 `mvn` 会吞掉全部输出并返回 126（`mvn.cmd` 是 Windows 批处理）。

---

## 2. 配置文件含明文密钥

4 个 `application-{dev,prod}.yml` 的**工作区是真密钥、git 索引里是打码版**
（例如 `dataoke.key` 工作区 `6afa83b...`，HEAD 里 `xxxxxxeb094a5`）。

**规则**：绝不提交这 4 个文件。pre-commit hook 会拦截；手工 `git add` 时也要用精确路径，
**禁止 `git add -A`**。

---

## 3. 行尾符

本仓库索引里存的是 CRLF，但编辑工具常写成 LF（或反之），导致整文件假 diff。

**规则**：改完文件后，提交前用 `git diff --stat` 检查——如果只改了几行却显示几百行变更，
执行 `sed -i 's/\r$//' <文件>`（或反向转换，以 `git diff` 干净为准）。pre-commit hook 会做规范化。

**不要**在 `.gitattributes` 里写 `text eol=lf`：索引里 1498 个文件是 CRLF，
强制规范化会让它们一次性全部产生 diff。统一行尾符需要一次独立的 renormalize 提交。

---

## 4. 外部 API 规格必须先核实

**教训**：工单 03 的子代理凭常识推测大淘客备案接口的参数，三处全错
（主机名 `openapiv2` 应为 `openapi`、`relationApp`+`pid` 官方不存在、响应字段 `relation_status` 不存在）。

**规则**：

1. 涉及新接口的工单，**主代理先把官方文档参数抓下来写进工单**，再交给实现子代理。
   不要让子代理猜上游规格。
2. 子代理标注了「推测」「无法核实」的地方，**主代理必须逐条核实**。
   诚实标注是有价值的信号，不是免责声明。
3. 已核实的规格写进工单的「官方规格」小节，供后续工单复用。

大淘客官方 API 文档结构：

```bash
curl -sS -m 30 -A "Mozilla/5.0 (Windows NT 10.0; Win64; x64)" \
  "https://www.dataoke.com/pmc/api-d.html?id=<ID>" -o dtk.html
# 文档把规格定义在 JS 变量里：
#   var request_tree0 = [...]  请求参数
#   var parent_tree0  = [...]  响应字段
```

已核实并归档的规格见 `.scratch/tri-platform-audit/issues/03-member-filing.md`、
`04-foul-orders.md`、`05-goods-sync.md` 的「官方规格」小节。

---

## 5. 多个子代理并行时的 Maven 竞态

**事实**：多个子代理在同一工作区同时跑 Maven，会共享并互相清理 `target/classes`，
产生 `NoSuchFileException`、class 缺失等假编译错误（`mshop-common`、`mshop-log`、`mshop-tool`
随机中招）。这与源码无关，重试也无法稳定通过。

**规则**：

- 多个子代理并行开发时，**只有主代理跑 Maven**。子代理只写代码与单元测试，
  写完用精确路径提交，不跑 `mvn test` / `mvn test-compile`。
- 所有子代理提交完成后，主代理串行跑一次全量测试，统一验证并修复。
- 这条优先于本文第 1 节的「跑测试命令」——那节是给单代理或主代理用的。

## 6. 接手子代理半成品

子代理可能中途失败，留下半成品。**不要直接 `git checkout .` / `git reset --hard`**。

接手流程：

1. `git status --short` 全量盘点，分类为：临时垃圾 / 未提交的代码产物 / 配置文件
2. 删除临时文件前，对未知目录做内容预览（`.tmp-hold/` 曾存放待放回的测试文件）
3. `mvn test-compile` 验证可编译，再 `mvn test` 验证全绿
4. **不轻信子代理的自述**，主代理亲自跑测试后才提交
