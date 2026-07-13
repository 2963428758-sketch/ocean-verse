# 🌊 OceanVerse — 智慧海洋探索平台

> 集海洋生物识别 + 环境监测 + 知识社区于一体的 AI 驱动海洋平台

## 🧱 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | Java 17 |
| 框架 | Spring Boot 3.4.0 |
| ORM | MyBatis-Plus 3.5.9 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7 (Redisson) |
| 连接池 | Druid |
| AI | Spring AI + 阿里云 DashScope |
| 对象存储 | 阿里云 OSS |
| 认证 | JWT (jjwt 0.12.5) |
| API 文档 | SpringDoc OpenAPI |
| 前端 | Vue 3 + Element Plus |
| 部署 | Docker Compose |

## 📦 模块结构

```
OceanVerse-backend/
├── oceanverse-common       # 通用工具、异常、响应封装
├── oceanverse-pojo         # 实体类、DTO/VO
├── user-auth-manager       # 用户认证与权限管理
├── species-manager         # 海洋生物物种管理
├── eco-observation-manager # 生态环境观测
├── data-visual-analytics   # 数据可视化分析
├── smart-ai-service        # AI 智能服务（图像识别 / 问答）
├── community-manager       # 社区管理（帖子 / 评论）
├── message-service         # 消息通知服务
└── oceanverse-app          # 启动模块（聚合入口）
```

## 🚀 快速启动

### 环境要求

- JDK 17+
- Maven 3.8+
- Docker & Docker Compose

### 使用 Docker Compose

```bash
# 启动 MySQL + Redis + 后端 + 前端
docker-compose up -d
```

### 本地开发

```bash
# 启动基础设施
docker-compose up -d mysql redis

# 启动后端（在 OceanVerse-backend 目录下）
mvn clean install -DskipTests
cd oceanverse-app
mvn spring-boot:run

# 启动前端（在 OceanVerse-frontend 目录下）
npm install
npm run dev
```

### 访问

- 后端 API：http://localhost:8080
- API 文档：http://localhost:8080/doc.html
- 前端页面：http://localhost

## 📄 许可证

本项目仅用于学习与实训用途。
