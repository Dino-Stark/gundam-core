# Context Manager POC v0.0.1：纯文本上下文管理方案

**文档版本**：v0.0.1  
**作者**：Dino Stark  
**日期**：2026-01-24  
**评审状态**：草案  

---

## Executive Summary

本版本是 **Context Manager V7 的 POC 设计文档**，目标是在 **1-2 天内完成可运行验证**，且无需部署新服务、不引入 Code Index、文件解析或多模态处理。POC 仅基于**纯文本对话**，验证上下文裁剪策略是否能在有限窗口内稳定支持长对话任务。

**一句话总结：**
> **V7 POC 做两件事：保持稳定 Prompt 结构 + 引入分段摘要，在“系统 Prompt + Summary + 首轮对话 + 最近 5 轮对话”的窗口内保障记忆与可控性。**

---

## 1. POC 范围与约束

### 1.1 目标
- 验证**简单上下文拼装策略**在长对话任务中的可用性。
- 以 **纯文本对话** 为输入输出，确保 1-2 天内交付。
- 与 V6 思路保持必要一致：**确定性 Prompt 结构** + **上下文截断策略**。

### 1.2 明确不做
- **不做 Code Index / RAG / 文件处理**
- **不做多模态**（图片/文档/代码解析）
- **不做新服务部署**（仅在现有工程中实现）
- **不做复杂一致性/缓存**（无 Redis、无数据库）

### 1.3 Baseline 对比

POC 的 baseline（已给定）：
- 只保留 **system prompt**
- 保留 **第一轮 user + assistant**
- 保留 **最近 5 轮 user + assistant**

V7 POC 在 baseline 上做的改进：
- **结构化拼装顺序明确化**（保证稳定前缀）
- **可配置窗口大小**（默认 5 轮，可调）
- **小规模可观测输出**（统计 token / message 数）

---

## 2. V7 POC 的核心概念（从 V6 继承的简化版）

### 2.1 确定性 Prompt Layout（来自 V6 的 B1/B6 思路简化）

V7 仅保留三个层次：

1. **固定前缀（System Prompt）**
2. **历史摘要（Summary Block）**
3. **对话消息列表（首轮 + 最近 N 轮）**

```
[System Prompt]

[Summary Block]

[Round 1: user]
[Round 1: assistant]

[Round (t-4) ~ Round t: user/assistant]
```

该顺序对缓存友好（即使在 POC 中不做缓存，也有稳定结构），同时把“长期记忆”从近景窗口中分离，便于后续升级。

### 2.2 摘要机制（避免 Summary of Summary）

POC 必须引入 **Summary Block** 来补足记忆力，但需要避免“摘要的摘要”导致语义漂移。建议采用 **分段独立摘要**：

1. 按固定轮次切段（例如每 5~8 轮一段）
2. **每段摘要都从原始对话生成**，不对已有摘要再摘要
3. Summary Block 由多个独立摘要段拼接，超过预算时做**段级裁剪**，而不是再摘要

这样既能保留长程记忆，又避免语义逐层压缩丢失。

### 2.3 上下文裁剪策略（Windowing）

V7 的核心算法：

1. 固定保留 **首轮 user/assistant**
2. 固定保留 **最近 N 轮**（默认 5）
3. 中间所有对话 **直接丢弃**

该策略与 baseline 一致，但将其标准化为 **ContextWindowPolicy** 可配置模块；Summary Block 作为长期记忆补位。

---

## 3. POC 功能清单

### 3.1 必做功能
- **Context 组装（System Prompt + Summary + Messages）**
- **简单裁剪策略**（首轮 + 最近 N 轮）
- **可配置窗口大小**（默认 5）
- **可观测统计**（消息数、估算 token 数）
- **分段摘要生成与拼装**（避免 summary of summary）

### 3.2 可选功能（如时间允许）
- 提供 **A/B 对比模式**：baseline vs V7 POC
- 评估任务输出质量（基于三个数据集）

---

## 4. 类设计（POC 级别）

> 说明：以下类名为建议命名，可在 Java 项目中实现（或使用你现有的语言约定）。

### 4.1 ContextManager
**职责**：组装最终上下文列表

**核心方法**：
- `buildContext(systemPrompt, messages, windowSize)`
  - 输入：系统 Prompt + 全量消息 + 窗口大小
  - 输出：System Prompt + Summary Block + 裁剪后的消息列表

---

### 4.2 ContextWindowPolicy
**职责**：实现裁剪策略

**核心方法**：
- `selectMessages(messages, windowSize)`
  - 保留首轮 + 最近 N 轮

---

### 4.3 SummaryManager
**职责**：生成与管理摘要段（避免 Summary of Summary）

**核心方法**：
- `buildSummaries(messages, segmentSize)`
  - 按固定轮次切段，从原始对话生成摘要段
- `selectSummarySegments(segments, tokenBudget)`
  - 仅做段级裁剪，不做再摘要

---

### 4.4 TokenEstimator (简单估算器)
**职责**：估算 tokens（不需要精确）

**核心方法**：
- `estimateTokens(messages)`
  - 可用字符数 / 4 的方式粗略估算

---

### 4.5 EvaluationRunner (POC 实验入口)
**职责**：跑数据集，输出对比结果

**核心方法**：
- `run(dataset, policy)`
  - 读取数据集 → 调用 ContextManager → 记录结果

---

## 5. 测试与验证计划

### 5.1 单元测试
1. **窗口裁剪测试**
   - 输入 10 轮对话 → 输出首轮 + 最近 5 轮
2. **窗口大小参数测试**
   - windowSize=3 → 输出首轮 + 最近 3 轮
3. **摘要分段与拼装测试**
   - 固定 segmentSize=5 → 生成多段摘要并按预算裁剪
   - 校验不对摘要再摘要（仅段级裁剪）
4. **边界场景**
   - 总轮次不足 5 → 保留全部

### 5.2 功能测试（POC）

基于用户指定数据集：
- **S-NIAH**：验证长上下文中的 passkey 保留能力
- **HotpotQA**：验证多段信息的结合能力
- **OOLONG**：验证长文本推理与聚合能力

**验证方式**：
- baseline vs V7 输出对比
- 统计命中率/正确率（如果有标注）

### 5.3 观察指标
- 平均上下文长度（message 数、token 估算）
- 任务正确率变化
- 失败样例分析（passkey 丢失、多跳推理断裂）

---

## 6. POC 交付物

1. `ContextManagerV7.md`（本设计文档）
2. POC 实现代码（ContextManager + Policy + Runner）
3. 简单测试结果报告（可用 Markdown 记录）

---

## 7. 下一步建议（完成 POC 后）

当 POC 验证有效后，可逐步引入 V6 中的增强能力：
- **摘要机制（B4）**：替代简单裁剪
- **Prefix Cache 友好布局**：降低延迟
- **轻量 RAG**：只做文本检索，不做代码索引

---

## 8. 结论

V7 POC 以 **最低复杂度** 验证上下文管理策略，在 1-2 天内即可完成开发与测试。它保留了 V6 中“确定性布局 + 裁剪窗口”的核心思想，同时大幅剥离复杂系统依赖，非常适合快速验证和方案对齐。
