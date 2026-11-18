# 12 · Aspose.Words 商用风险与开源替代选型报告

> 调研日期：2026-11-18 · 结论：Aspose.Words 是全项目唯一的商业闭源依赖，且 `convertContract=false` 开关挡不住主签署流程。

## 1. Aspose 在项目里到底做什么

**一句话**：只做「docx 模板占位符替换后，把 docx 转成 PDF」这一步；模板替换本身用 Apache POI XWPF，PDF 盖章用 OpenPDF，Aspose 仅负责 docx→PDF。

| 项 | 事实 |
|---|---|
| 依赖 | `mshop-system/pom.xml:69-75`，`com.aspose:aspose-words:11.10.0.0`（陈旧声明），`<scope>system</scope>` 指向 `lib/aspose-words-18.6.jar`（10.3 MB，原版 18.6，无内置 license） |
| 唯一使用类 | `mshop-system/.../utils/WordUtil.java` |
| 核心方法 | `wordConverterToPdf`（:127-146）：`new Document(wordPath).save(os, SaveFormat.PDF)` |
| 完整链路 | `replaceAndGenerateWord`（POI 替换，:37-98）→ `wordConverterToPdf`（Aspose，:127-146）→ `itextPDFAddPicture`（OpenPDF 盖公章/骑缝章/人脸/身份证，:199-300）→ 上传 OSS |
| 业务场景 | 实名用户电子合同生成与签署，非报表导出 |
| License | 无内置；从 OSS 上的 `shop.licenseUrl` 远程加载（`getLicense`:106-117），加载失败直接返回 false 不生成 PDF |

### ⚠️ 开关的真相（与旧文档矛盾）

`shop.convertContract=false` **只控制登录时补转合同这一条路径**：
- `AuthController.java:467-470`：`if(CONVERT_CONTRACT && "dyb".equals(PAY_NAME)) ...`

**主签署/实名流程无条件下调用 Aspose**，不受开关约束：
- `UserCardController.java:129-130` `generateContract(...)`
- `UserVerificationController.java:136-138` `generateContract(...)`
- 两者最终 → `MwUserCardServiceImpl:126` → `WordUtil.generateEContract` → Aspose

**结论**：`docs/01-安全与授权评估报告.md:218,233` 声称「默认 false 即不启用 Aspose」与代码不符。 Aspose 水印/授权义务实际存在。

## 2. 开源组件对比

| 组件 | 许可证 | 占位符替换 | docx→PDF | 外部进程 | 适配度 |
|---|---|---|---|---|---|
| **Apache POI XWPF** | Apache-2.0 | ✅ 已在用 | ❌ | 无 | **保留** |
| **JODConverter + LibreOffice** | JODConverter Apache-2.0 / LO MPL-2.0 | ❌ | ✅ 高保真 | **需装 LibreOffice** | **推荐用于转换环节** |
| docx4j | Apache-2.0 | ✅ | ✅（FOP 纯 Java，保真有限） | 无 | 中 |
| XDocReport | MIT core（samples LGPL） | ✅ 专为合同设计 | ⚠️ 靠 iText（LGPL，与项目已去 iText 的方向冲突） | 无 | 中 |
| docx-stamper | MIT | ✅ SpEL 占位符 | ❌ | 无 | 中（不能解决 PDF 转换） |
| OpenPDF（项目已用） | LGPL/MPL | ❌ | ❌ | 无 | **保留盖章环节** |

来源：各项目官方 GitHub（apache/poi、jodconverter/jodconverter、plutext/docx4j、opensagres/xdocreport、thombergs/docx-stamper、librepdf/OpenPDF）。

## 3. 推荐方案

**保留 POI 做模板替换 + 引入 JODConverter(Local) 驱动 LibreOffice Headless 替换 `wordConverterToPdf` + 保留 OpenPDF 盖章。**

理由：
1. 项目对 Aspose 的唯一真实依赖就是 docx→PDF 一步；
2. JODConverter + LibreOffice 是唯一高保真且许可证完全商用友好的方案；
3. 改动局限在 `WordUtil.wordConverterToPdf`（约 20 行）+ `pom.xml` 一处依赖，业务层零改动。

**工作量**：代码小（1 方法 + 依赖 + 配置）；运维中（服务器安装 LibreOffice 数百 MB、配置中文字体、验证并发与版式）。

**备选**：不愿引入外部进程则用 docx4j FOP，纯 Java 但版式保真风险更高。

## 4. 迁移验收清单

- [ ] `pom.xml` 移除 aspose-words system scope 依赖与 `lib/aspose-words-18.6.jar`
- [ ] `WordUtil` 移除 `com.aspose.words` import、`getLicense`、`wordConverterToPdf` 改写为 JODConverter
- [ ] 服务器安装 LibreOffice + 中文字体（如 Source Han Sans / Alibaba PuHuiTi）
- [ ] 用真实合同模板验证：段落/表格占位符、公章/骑缝章位置、身份证贴图
- [ ] 确认 PDF 无评估水印
- [ ] 修订 `docs/01` 中关于开关的错误表述
