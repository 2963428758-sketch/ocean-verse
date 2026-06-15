# 🌊 OceanVerse — 智慧海洋探索平台

> 集 **海洋生物识别 + 环境监测 + 知识社区** 于一体的 AI 驱动海洋平台

## 技术栈

| 层 | 技术 |
|---|------|
| 前端 | Vue 3 + TypeScript + Vite + Element Plus + ECharts + Leaflet |
| 后端 | Spring Boot 3.4 + MyBatis Plus + Spring Security + JWT |
| 数据库 | MySQL 8.0 + Redis + RabbitMQ |
| AI | DeepSeek / 通义千问 API + 图像识别 |
| 部署 | Docker Compose + Nginx |

## 项目结构

```
OceanVerse/
├── OceanVerse-backend/         # 后端 (Spring Boot 多模块)
│   ├── oceanverse-common/      # 通用工具 (JWT/Redis/异常处理)
│   ├── oceanverse-pojo/        # 实体/DTO
│   ├── user-auth-manager/      # 🔐 用户认证 (成员A)
│   ├── species-manager/        # 🐟 物种管理 (成员B)
│   ├── eco-observation-manager/# 🔬 观测管理
│   ├── data-visual-analytics/  # 📊 数据可视化 (成员C)
│   ├── smart-ai-service/       # 🤖 AI 服务 (成员B/E)
│   ├── community-manager/      # 👥 社区互动 (成员D)
│   ├── message-service/        # 📨 消息服务 (WebSocket/MQ)
│   └── oceanverse-app/         # 启动模块
├── OceanVerse-frontend/        # 前端 (Vue 3)
│   └── src/
│       ├── api/                # 接口封装
│       ├── views/              # 页面组件
│       ├── stores/             # Pinia 状态管理
│       ├── router/             # 路由
│       └── layouts/            # 布局
├── database/                   # SQL 脚本
├── docker-compose.yml          # Docker 部署
└── docs/                       # 文档
```

## 快速启动

### 1. 数据库
```bash
# 创建数据库并导入数据
mysql -u root -p < database/oceanverse.sql
```

### 2. 后端
```bash
cd OceanVerse-backend
# 修改配置: oceanverse-app/src/main/resources/application.yml
mvn clean package -DskipTests
java -jar oceanverse-app/target/oceanverse-app-1.0.0-SNAPSHOT.jar
```

### 3. 前端
```bash
cd OceanVerse-frontend
npm install
npm run dev
# 访问 http://localhost:3000
```

### 4. Docker 一键启动
```bash
docker-compose up -d
```

## 测试账号

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | admin123 | 超级管理员 |
| researcher | 123456 | 研究员 |
| observer | 123456 | 观测员 |

## 五人分工

| 成员 | 模块 | 核心工作 |
|------|------|----------|
| A | user-auth-manager | 用户系统、JWT、RBAC、Redis 缓存 |
| B | species-manager + AI | 物种 CRUD、AI 图像识别 |
| C | data-visual-analytics | ECharts 大屏、Leaflet 地图 |
| D | community-manager + AI | 社区动态、评论、AI 智能问答 |
| E | message-service + 运维 | 消息队列、WebSocket、Docker 部署 |

## API 文档

启动后端后访问：`http://localhost:8080/swagger-ui.html`
