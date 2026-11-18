# 14 · Git 历史敏感信息审计报告

> 审计日期：2026-11-18 · 三个仓库均为**公开 GitHub**，历史已被 clone/fork，所有历史密钥**视为已泄露**。
> 处置决策：**轮换密钥 + 清理 HEAD 残留，不改写历史**（改写历史无法回收已 clone 的副本，且密钥无论如何都要轮换）。

## 1. 审计范围

| 仓库 | remote | 提交数 |
|---|---|---|
| server | `github.com/Hezhero/sfb_server`（upstream jacky4631） | 220 |
| app | `github.com/Hezhero/sfb`（upstream jacky4631） | 171 |
| admin | `github.com/Hezhero/sfb_admin`（upstream jacky4631） | 211 |

## 2. 发现（值打码：前4…后4）

### 历史存在、当前 HEAD 已外移
| 凭据 | 引入 | 现状 |
|---|---|---|
| 好单库 cid/code `XDg…14e` 等 | `a06515c` | 源码已移除（`11b8c1f`），但 `docs/10-三平台可用性评估报告.md` 仍明文记录 |
| 大淘客 appKey/sign `612b…763`/`8bea…944` | `a06515c` | 源码已移除，文档仍明文 |
| 京东 clientSecret `d222…b06` | `12907ab` | 已移除（`690c576`） |
| Apple IAP 共享密钥 `c3d6…793` | `01d346b` | 已改 `@Value`（`690c576`），但历史可查。**必须轮换** |

### ⚠️ 当前 HEAD 仍需处理
| 项 | 位置 | 风险 |
|---|---|---|
| **SM2 商户私钥 `MIGT…hq`** | `mshop-store/.../pay/allinpay/syb/lib/SybConstants.java:14`（**注释状态但完整**） | **高**——注释≠删除，必须清除 |
| RSA 私钥 `MIIC…g==` | `mshop-app/.../config/application.yml`（`rsa.private_key`，真实且与 admin/app 公钥配对） | **高**——泄露即传输加密失效 |
| AES key `LwGo…BY==` | 两处 `application.yml` | 中 |
| smms token `1oOP…lyl` | 两处 `application.yml` | 中 |
| Druid `admin/123456` | `application-dev/prod.yml`（`690c576` 已取消 permitAll，弱口令仍在） | 中-高 |
| JWT `secret: k09BQnaF`（7 位弱密钥） | 两份 prod.yml | 中-高 |
| Android keystore `xxx.jks` | app HEAD 中，弱口令 `123456` 可打开，含 PrivateKeyEntry | **高**——可伪造应用签名 |
| 极光 AppKey `f4b9…551` | `global.dart:94`、`build.gradle:46`（真实值） | 中——账号控制权在作者 |

### 不在 HEAD（历史泄露，仍需轮换）
- admin 前端 RSA 私钥 `MIIB…kB8`（`4414a4c` 已删）
- JWTPush 等其余第三方 key 在各版本 yml 中均以 `xxxxxx` 打码，未见明文真值

## 3. 处置方案对比

| 方案 | 优点 | 代价 | 适用 |
|---|---|---|---|
| **(a) 改写历史 + force push** | 新克隆看不到旧密钥 | 重写所有 hash、两远端强推、全体协作者重克隆、fork/缓存无法回收；**且密钥仍必须轮换** | 私有仓库/协作少/合规硬要求 |
| **(b) 轮换 + HEAD 清理（采用）** | 零协作中断；密钥轮换后旧值失效，风险实际归零 | 旧值永久可见、scanner 持续告警 | **本案**：仓库已公开且被大量 clone/fork |

## 4. 行动清单

**代码侧（本轮做）**
- [ ] 清除 `SybConstants.java:14` 注释中的完整 SM2 私钥
- [ ] 清理 `docs/10` 中明文记录的好单库/大淘客凭据
- [ ] `application.yml` 的 RSA/AES/smms token、JWT secret、Druid 口令改为环境变量占位（明文配置不入库）

**平台侧（需人工轮换，agent 无法代做）**
- [ ] **优先**：Apple IAP 共享密钥、通联 SM2 商户私钥 + MD5 appKey、Android 签名证书（换新证书重新签名发版）
- [ ] 其次：京东/好单库/大淘客/极光/sms 第三方 key
- [ ] 全部：Druid/JWT/RSA/AES/smms 重置

> 即使不改写 git 历史，只要真实密钥轮换/吊销，历史中的旧值即失效。这是本案的实际止损手段。
