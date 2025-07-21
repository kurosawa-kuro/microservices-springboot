# Spring Boot → Express.js 移行手順書

## 概要

この文書は、KuroBankマイクロサービス（Spring Boot + Java）をExpress.js + Node.jsに移行するための包括的な手順書です。

## 目次

1. [プロジェクト構成理解](#1-プロジェクト構成理解)
2. [技術スタック対応表](#2-技術スタック対応表)
3. [環境セットアップ](#3-環境セットアップ)
4. [移行順序](#4-移行順序)
5. [共通パターンの移行](#5-共通パターンの移行)
6. [サービス別移行手順](#6-サービス別移行手順)
7. [テスト戦略](#7-テスト戦略)
8. [デプロイメント](#8-デプロイメント)

---

## 1. プロジェクト構成理解

### 現在のサービス構成

```
KuroBank Microservices
├── accounts-service (8080)  # 顧客・口座管理
├── cards-service (9000)     # クレジットカード管理  
├── loans-service (8090)     # ローン管理
├── gatewayserver (8072)     # APIゲートウェイ
└── message-service (9010)   # イベント処理
```

### データフロー

```
Client Request
    ↓
Gateway Server (認証・ルーティング)
    ↓
Accounts Service (オーケストレータ)
    ├── → Cards Service
    ├── → Loans Service  
    └── → Message Service (非同期)
```

---

## 2. 技術スタック対応表

| Spring Boot | Express.js | 用途 |
|-------------|------------|------|
| `@RestController` | `app.get/post/put/delete` | REST API |
| `@Service` | Service classes | ビジネスロジック |
| `@Repository` | Prisma Client | データアクセス |
| `application.yml` | `.env` + `dotenv` | 設定管理 |
| Jakarta Validation | `zod` | バリデーション |
| Spring Data JPA | `Prisma ORM` | データベース |
| RestTemplate | `axios` | HTTP クライアント |
| Spring Cloud Stream | `kafka-node` | メッセージング |
| Micrometer | `prom-client` | メトリクス |
| OpenAPI | `openapi-backend` | API仕様 |

---

## 3. 環境セットアップ

### 3.1 プロジェクト初期化

```bash
# 新しいプロジェクト作成
mkdir microservices-express
cd microservices-express

# 各サービスディレクトリ作成
mkdir -p services/{accounts,cards,loans,gateway,message}
mkdir -p shared/{types,utils,middleware}
mkdir -p docs/api
```

### 3.2 共通依存関係

各サービスの `package.json`:

```json
{
  "name": "@kurobank/[service-name]",
  "version": "1.0.0",
  "dependencies": {
    "express": "^4.18.2",
    "cors": "^2.8.5",
    "dotenv": "^16.3.1",
    "js-yaml": "^4.1.0",
    "openapi-backend": "^5.10.6",
    "zod": "^3.22.0",
    "prisma": "^5.7.0",
    "@prisma/client": "^5.7.0",
    "axios": "^1.6.0",
    "prom-client": "^15.1.0",
    "winston": "^3.11.0",
    "uuid": "^9.0.1"
  },
  "devDependencies": {
    "@types/node": "^20.10.0",
    "typescript": "^5.3.0",
    "ts-node": "^10.9.0",
    "nodemon": "^3.0.0"
  }
}
```

### 3.3 TypeScript設定

`tsconfig.json`:

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "commonjs",
    "moduleResolution": "node",
    "declaration": true,
    "outDir": "./dist",
    "rootDir": "./src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "dist"]
}
```

---

## 4. 移行順序

推奨される移行順序:

1. **共通ライブラリ** (`shared/`)
2. **Message Service** (依存関係なし)
3. **Cards Service** (単純構造)
4. **Loans Service** (Cards類似)
5. **Accounts Service** (オーケストレータ)
6. **Gateway Service** (最後)

---

## 5. 共通パターンの移行

### 5.1 Express アプリケーション基本構造

```typescript
// src/app.ts
import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import { errorHandler } from './middleware/errorHandler';
import { correlationId } from './middleware/correlationId';
import { routes } from './routes';

dotenv.config();

const app = express();

// ミドルウェア
app.use(cors());
app.use(express.json());
app.use(correlationId);

// ルーティング
app.use('/api', routes);

// エラーハンドリング
app.use(errorHandler);

export default app;
```

### 5.2 相関ID ミドルウェア

```typescript
// src/middleware/correlationId.ts
import { Request, Response, NextFunction } from 'express';
import { v4 as uuidv4 } from 'uuid';

export const correlationId = (req: Request, res: Response, next: NextFunction) => {
  const correlationId = req.headers['kurobank-correlation-id'] as string || uuidv4();
  req.correlationId = correlationId;
  res.setHeader('kurobank-correlation-id', correlationId);
  next();
};
```

### 5.3 エラーハンドリング

```typescript
// src/middleware/errorHandler.ts
import { Request, Response, NextFunction } from 'express';
import { ErrorResponseDto } from '../types/ErrorResponseDto';

export const errorHandler = (error: Error, req: Request, res: Response, next: NextFunction) => {
  const errorResponse: ErrorResponseDto = {
    apiPath: req.path,
    errorCode: 'INTERNAL_SERVER_ERROR',
    errorMessage: error.message,
    errorTime: new Date().toISOString()
  };

  res.status(500).json(errorResponse);
};
```

### 5.4 バリデーションミドルウェア

```typescript
// src/middleware/validation.ts
import { Request, Response, NextFunction } from 'express';
import { ZodSchema } from 'zod';

export const validate = (schema: ZodSchema) => {
  return (req: Request, res: Response, next: NextFunction) => {
    try {
      schema.parse(req.body);
      next();
    } catch (error) {
      res.status(400).json({
        apiPath: req.path,
        errorCode: 'VALIDATION_ERROR',
        errorMessage: 'Invalid input data',
        errorTime: new Date().toISOString()
      });
    }
  };
};
```

### 5.5 Prisma セットアップ

各サービスの `schema.prisma`:

```prisma
generator client {
  provider = "prisma-client-js"
}

datasource db {
  provider = "sqlite"
  url      = "file:./data/app.db"
}

model Customer {
  customerId    Int      @id @default(autoincrement())
  name          String
  email         String
  mobileNumber  String   @unique
  createdAt     DateTime @default(now())
  createdBy     String
  updatedAt     DateTime @updatedAt
  updatedBy     String?
  
  accounts      Account[]
}
```

---

## 6. サービス別移行手順

### 6.1 Cards Service 移行例

#### ディレクトリ構造

```
services/cards/
├── src/
│   ├── controllers/
│   │   └── cardsController.ts
│   ├── services/
│   │   └── cardsService.ts
│   ├── repositories/
│   │   └── cardsRepository.ts
│   ├── types/
│   │   ├── CardsDto.ts
│   │   └── CardsEntity.ts
│   ├── middleware/
│   ├── routes/
│   │   └── index.ts
│   └── app.ts
├── prisma/
│   └── schema.prisma
├── package.json
└── .env
```

#### Controller実装

```typescript
// src/controllers/cardsController.ts
import { Request, Response } from 'express';
import { CardsService } from '../services/cardsService';
import { CardsDto } from '../types/CardsDto';

export class CardsController {
  constructor(private cardsService: CardsService) {}

  createCard = async (req: Request, res: Response) => {
    try {
      const { mobileNumber } = req.query;
      await this.cardsService.createCard(mobileNumber as string);
      
      res.status(201).json({
        statusCode: '201',
        statusMsg: 'Card created successfully'
      });
    } catch (error) {
      throw error;
    }
  };

  fetchCard = async (req: Request, res: Response) => {
    try {
      const { mobileNumber } = req.query;
      const card = await this.cardsService.fetchCard(mobileNumber as string);
      
      res.status(200).json(card);
    } catch (error) {
      throw error;
    }
  };

  // 他のメソッドも同様に実装...
}
```

#### Service層実装

```typescript
// src/services/cardsService.ts
import { CardsRepository } from '../repositories/cardsRepository';
import { CardsDto } from '../types/CardsDto';
import { CardsMapper } from '../mappers/cardsMapper';

export class CardsService {
  constructor(private cardsRepository: CardsRepository) {}

  async createCard(mobileNumber: string): Promise<void> {
    const existingCard = await this.cardsRepository.findByMobileNumber(mobileNumber);
    
    if (existingCard) {
      throw new Error(`Card already registered with given mobileNumber ${mobileNumber}`);
    }

    const newCard = {
      mobileNumber,
      cardNumber: this.generateRandomCardNumber(),
      cardType: 'Credit Card',
      totalLimit: 100000,
      amountUsed: 0,
      availableAmount: 100000
    };

    await this.cardsRepository.create(newCard);
  }

  async fetchCard(mobileNumber: string): Promise<CardsDto> {
    const card = await this.cardsRepository.findByMobileNumber(mobileNumber);
    
    if (!card) {
      throw new Error(`Card not found with given mobileNumber ${mobileNumber}`);
    }

    return CardsMapper.mapToCardsDto(card);
  }

  private generateRandomCardNumber(): string {
    return Math.floor(100000000000 + Math.random() * 900000000000).toString();
  }
}
```

---

## 7. テスト戦略

### 7.1 テストファイル構造

```
services/cards/tests/
├── unit/
│   ├── controllers/
│   ├── services/
│   └── repositories/
├── integration/
│   └── api/
└── fixtures/
    └── testData.ts
```

### 7.2 Jest設定

```json
{
  "scripts": {
    "test": "jest",
    "test:watch": "jest --watch",
    "test:coverage": "jest --coverage"
  },
  "jest": {
    "testEnvironment": "node",
    "roots": ["<rootDir>/src", "<rootDir>/tests"],
    "testMatch": ["**/__tests__/**/*.ts", "**/?(*.)+(spec|test).ts"],
    "transform": {
      "^.+\\.ts$": "ts-jest"
    }
  }
}
```

---

## 8. デプロイメント

### 8.1 Docker構成

```dockerfile
# Dockerfile
FROM node:21-alpine

WORKDIR /app

COPY package*.json ./
RUN npm ci --only=production

COPY . .
RUN npm run build

EXPOSE 9000

CMD ["node", "dist/server.js"]
```

### 8.2 docker-compose.yml

```yaml
version: '3.8'
services:
  cards:
    build: ./services/cards
    ports:
      - "9000:9000"
    environment:
      - NODE_ENV=production
      - DATABASE_URL=file:./data/app.db
    volumes:
      - cards-data:/app/data

volumes:
  cards-data:
```

### 8.3 Kubernetes Helm Charts

既存のHelmチャートを参考に、Node.js用に調整:

```yaml
# values.yaml
replicaCount: 2

image:
  repository: kurobytes/cards-express
  tag: "latest"
  pullPolicy: IfNotPresent

service:
  type: ClusterIP
  port: 9000
  targetPort: 9000

resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 250m
    memory: 256Mi
```

---

## 次のステップ

1. [Service-by-Service Migration Details](./service-migration-details.md)
2. [API Documentation Migration](./api-documentation.md)
3. [Monitoring & Observability Setup](./monitoring-setup.md)
4. [Performance Optimization Guide](./performance-optimization.md)

---

## 参考リンク

- [Express.js Best Practices](https://expressjs.com/en/advanced/best-practice-security.html)
- [Prisma Documentation](https://www.prisma.io/docs/)
- [OpenAPI Backend](https://github.com/anttiviljami/openapi-backend)
- [Node.js Microservices Architecture](https://nodejs.org/en/docs/guides/nodejs-docker-webapp/)