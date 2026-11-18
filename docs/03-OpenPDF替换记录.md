# 依赖变更记录：iText 5 → OpenPDF

> 变更日期：2026-10-30
> 变更范围：`mshop-system` 模块
> 变更原因：消除 AGPL v3 的商用合规风险

---

## 1. 为什么要换

原依赖 `com.itextpdf:itextpdf:5.5.13.3` 采用 **AGPL v3** 协议。

AGPL 的关键条款是：只要软件**通过网络对外提供服务**（哪怕不分发二进制），就必须向使用者提供完整源码。服务端是闭源商用部署，这一条无法满足——这比"买个 license"更麻烦，因为它约束的是整个衍生作品。

OpenPDF 是 iText 4.2.0 的分支（`com.github.librepdf:openpdf`），采用 **LGPL + MPL 双协议**，允许闭源调用，不要求开放自身代码。

---

## 2. 改了什么

只动了 2 个文件、6 行。

### 2.1 `mshop-system/pom.xml`

```diff
-        <!-- https://mvnrepository.com/artifact/com.itextpdf/itextpdf -->
-        <dependency>
-            <groupId>com.itextpdf</groupId>
-            <artifactId>itextpdf</artifactId>
-            <version>5.5.13.3</version>
-        </dependency>
+        <!-- PDF 处理库：原 iText 5 为 AGPL v3，闭源商用不合规；改用 OpenPDF（LGPL/MPL） -->
+        <dependency>
+            <groupId>com.github.librepdf</groupId>
+            <artifactId>openpdf</artifactId>
+            <version>1.3.43</version>
+        </dependency>
```

### 2.2 `mshop-system/src/main/java/com/junchen/utils/WordUtil.java`

```diff
-import com.itextpdf.text.Image;
-import com.itextpdf.text.Rectangle;
-import com.itextpdf.text.pdf.PdfReader;
-import com.itextpdf.text.pdf.PdfStamper;
+import com.lowagie.text.Image;
+import com.lowagie.text.Rectangle;
+import com.lowagie.text.pdf.PdfReader;
+import com.lowagie.text.pdf.PdfStamper;
```

**仅此而已**。OpenPDF 保持 iText 4.2 的 `com.lowagie` 包名，且 `PdfReader` / `PdfStamper` / `getOverContent` / `Image.getInstance` / `scaleToFit` / `setAbsolutePosition` 的签名与 iText 5 完全一致，所以业务代码一行没改。

全项目引用 iText 的地方只有这一个文件（已确认：`grep -rn "com.itextpdf" --include=*.java` 仅 4 个 import）。

---

## 3. 验证记录（均为实测）

### 3.1 编译

```
mvn -pl mshop-system -am compile -B
→ BUILD SUCCESS (29s)
```

### 3.2 依赖树

```
mvn -pl mshop-admin dependency:tree | grep -c itextpdf
→ 0
```

`mshop-system` 依赖树中：

```
+- com.github.librepdf:openpdf:jar:1.3.43:compile
\- com.github.binarywang:weixin-java-pay:jar:4.3.0:compile
   \- org.bouncycastle:bcpkix-jdk15on:jar:1.68:compile
```

> **注意**：`bouncycastle` 是**微信支付 SDK** 带来的，不是 OpenPDF。两者恰好共用同一版本 1.68，因此没有冲突，也**不能排除**——排掉微信支付会崩。

### 3.3 打包产物

```
mvn -pl mshop-admin,mshop-app -am package -DskipTests
→ BUILD SUCCESS (3m36s)

mshop-admin-5.1.0.jar  157M
mshop-app-5.1.0.jar    159M

BOOT-INF/lib/openpdf-1.3.43.jar  2,150,122 字节
itext 相关条目：0
```

体积与替换前持平（iText 5 约 2.3MB，OpenPDF 约 2.15MB）。

### 3.4 运行时冒烟（关键）

编译通过不等于能跑。实际调用了 `WordUtil.itextPDFAddPicture`（电子合同盖章链路）：

| 步骤 | 输入 | 结果 |
| --- | --- | --- |
| 生成 2 页 A4 PDF | — | 1115 字节 |
| 生成 400×400 公章 PNG + 428×270 证件图 | — | — |
| 调用 `itextPDFAddPicture` | PDF 流 + 印章/证件 URL | 无异常，耗时 415ms |
| 读回输出 PDF | — | **页数 = 2，尺寸 595×842（A4 正确）** |
| 体积变化 | 1115 → 7099 字节 | 图片确实嵌入（+6KB ≈ 3 张 PNG） |

结论：`PdfStamper.getOverContent().addImage()`、`Image.scaleToFit()`、骑缝章切分（`splitImage`）全部正常。

---

## 4. 版本选择说明

选 **1.3.43** 而不是 2.x：

| 版本线 | Java 要求 | 说明 |
| --- | --- | --- |
| 1.3.x | JDK 17+ | 与本项目 `java.version=17` 完全匹配，社区验证最充分 |
| 2.x | JDK 17+ / 部分 11+ | 包名不变，但依赖基线抬升，对老项目无收益 |

项目是 Spring Boot 2.7.18 + JDK 17 基线，1.3.43 是最稳的选择。若将来升级到 JDK 17+ 且需要新版 PDF 特性，再评估 2.x。

---

## 5. 回滚方法

```bash
git checkout -- mshop-system/pom.xml \
               mshop-system/src/main/java/com/junchen/utils/WordUtil.java
mvn -pl mshop-admin,mshop-app -am package -DskipTests
```

回滚后 AGPL 约束会回来，仅建议用于临时排障。

---

## 6. 后续注意事项

1. **不要再引入 `com.itextpdf` 系的任何包**。iText 7（`com.itextpdf:itext7-core`）同样是 AGPL，`itextpdf:itextpdf:5.x` 也是。
2. 如果将来需要 PDF 表单填充、数字签名等能力，优先看 OpenPDF 是否已支持（1.3.x 支持 `AcroFields`、`PdfSignatureAppearance`），不要顺手加 iText。
3. `shop.convertContract` 若仍为 `false`（默认），盖章链路本身不参与运行——也就是说即使 OpenPDF 出问题也不影响主流程，这是个安全垫。
4. 建议在 CI 里加一条检查：`mvn dependency:tree | grep -i itext` 必须为空，防止依赖被间接引回来。
