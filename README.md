# 农作物缺素症识别与科学施肥建议智能体

OHCP 一小时智能编程大赛参赛项目。基于 **RAG（检索增强生成）** 的农作物缺素症智能诊断与科学施肥建议智能体：用户输入作物类型、症状部位、症状表现与环境描述，智能体优先检索**自建爬虫本地 JSON 知识库**，输出多个候选缺素类型与匹配置信度，并生成含农艺调控、有机/生物肥、化学施肥、安全减量提醒的结构化施肥建议，末尾自然融入课程思政内容。

## 核心特性

- **RAG 检索增强生成**：先检索本地知识库再整理输出，禁止大模型凭空作答，核心识别逻辑完全依赖自建知识库。
- **自建爬虫知识库**：内置 Jsoup 爬虫，抓取公开农技科普资料 → 清洗分句 → 按元素/症状关键词结构化 → 增量写入 `data/knowledge.json`（离线时自动回退内置种子知识库）。
- **本地向量化检索**：基于字符 n-gram + 哈希技巧的轻量向量化与余弦相似度，多路加权（向量 + 关键词 + 部位 + 作物）打分，无需外部向量服务，离线可演示。
- **结构化施肥建议**：缺素名称、缺素简介、农艺调控、有机/生物肥方案、化学施肥方案、施肥安全与减量提醒。
- **课程思政自然融入**：随业务结果输出 1-2 行简短引导（农技人物故事 / 粮食安全 / 绿色农业 三选一），不弹窗、不说教。
- **多轮对话记忆**（创新点）：按会话保存上下文，支持追问缺素细节、施肥用量、不同作物适配方案。
- **大模型增强（可选）**：配置 API Key 后由 LangChain4j / SpringAI 对诊断说明做整理润色；未配置时自动降级为知识库模板输出，保证离线可用。

## 技术栈

- 后端：SpringBoot 3.4.5 + SpringAI 1.0.0 + LangChain4j 0.36.2 + Jsoup
- 前端：极简 Html + CSS + 原生 JS（前后端分离，静态资源随应用一起提供）
- 构建：Maven（已内置 `mvnw.cmd` / `mvnw` 一键引导脚本）
- JDK：17+

## 项目结构

```
├── pom.xml                     # Maven 配置
├── mvnw.cmd / mvnw             # 一键启动脚本（自动下载 Maven）
├── src/main/java/com/ohcp/agent
│   ├── OhcpAgentApplication.java   # 启动入口
│   ├── controller/             # 接口层（聊天 / 爬虫 / 统计）
│   ├── entity/                 # 实体包（请求、响应、知识条目等）
│   ├── knowledge/              # 知识库加载、向量化、相似度检索
│   ├── crawler/                # 爬虫包（抓取、清洗、结构化、增量合并）
│   ├── rag/                    # 智能体 RAG 逻辑（实体抽取、建议、思政、记忆、LLM）
│   └── config/                 # 配置（CORS）
├── src/main/resources
│   ├── application.yml         # 配置
│   ├── knowledge.json          # 内置种子知识库（22 条，覆盖 12 种元素）
│   └── static/                 # 前端页面（index.html / css / js）
└── data/knowledge.json         # 运行时爬虫增量产物（首次爬取后生成，已 gitignore）
```

## 快速启动（VS Code，一键）

1. 用 VS Code 打开本项目根目录。
2. 确认已安装 **JDK 17+**（`java -version` 可查看；未安装请装 Temurin/微软 OpenJDK 17）。
3. 在 VS Code 终端（PowerShell）执行：

```powershell
.\mvnw.cmd spring-boot:run
```

首次运行会自动下载 Apache Maven 到项目内 `.mvn-dist/` 并拉取依赖；若本机已安装 Maven，也可直接：

```powershell
mvn spring-boot:run
```

4. 打开浏览器访问：<http://localhost:8080>，即可看到交互页面并现场演示。

打包运行（生成可执行 jar）：

```powershell
.\mvnw.cmd -DskipTests package
java -jar target\ohcp-deficiency-agent-1.0.0.jar
```

## 现场演示说明

- 在输入框粘贴示例：`水稻，叶片从下部老叶开始均匀发黄，植株矮小，分蘖减少。`，点击“识别诊断”。
- 页面左侧展示多候选缺素类型与置信度、结构化施肥建议、思政引导；右侧“诊断详情”同步展示。
- 点击快捷“示例”按钮可一键演示水稻缺氮、番茄缺钙、玉米缺锌。
- 点击“新会话”可清空上下文；继续追问“施肥用量多少”“换成小麦怎么办”可演示多轮对话记忆。

## 可选：配置大模型 API（增强输出）

在环境变量或 `src/main/resources/application.yml` 中设置：

```yaml
OPENAI_API_KEY: sk-xxxx
OPENAI_BASE_URL: https://api.openai.com   # 兼容 OpenAI 协议的地址均可
OPENAI_MODEL: gpt-4o-mini
```

不配置时不影响核心功能，系统自动使用知识库模板输出。

## 主要接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/chat` | 核心聊天诊断（`{sessionId, message, crop, part, symptom, environment}`） |
| POST | `/api/crawler/run` | 手动触发爬虫增量更新知识库 |
| GET | `/api/knowledge/stats` | 知识库统计与条目预览 |
| GET | `/api/session/{id}/history` | 会话历史 |
| DELETE | `/api/session/{id}` | 清空会话 |
| GET | `/api/health` | 健康检查 |

## 提交到 Git 远程仓库

将整套项目推送到已建好的远程仓库：

```bash
cd "D:\Learning\实习规划"
git init                                     # 若尚未初始化
git add .
git commit -m "feat: OHCP农作物缺素症识别与科学施肥建议智能体"
git branch -M main
git remote add origin <远程仓库地址>          # 替换为你的仓库地址
git push -u origin main
```

> 如远程已绑定或使用不同分支名（如 master），请相应调整 `-M` 与 `push` 分支名。

## 说明

- 爬虫默认在启动后自动运行一次，网络不可用时会静默回退内置种子知识库，不影响演示。
- `data/`、`target/`、`.mvn-dist/` 已加入 `.gitignore`，不会提交到仓库。