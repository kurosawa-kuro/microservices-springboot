# サービス別詳細移行手順

## 目次

1. [Cards Service 完全移行](#1-cards-service-完全移行)
2. [Loans Service 移行](#2-loans-service-移行)
3. [Accounts Service 移行](#3-accounts-service-移行)
4. [Message Service 移行](#4-message-service-移行)
5. [Gateway Service 移行](#5-gateway-service-移行)

---

## 1. Cards Service 完全移行

### 1.1 プロジェクト初期化

```bash
# Cards service ディレクトリ作成
mkdir -p services/cards
cd services/cards

# package.json 初期化
npm init -y

# 依存関係インストール
npm install express cors dotenv js-yaml openapi-backend zod prisma @prisma/client
npm install -D typescript @types/node @types/express ts-node nodemon jest @types/jest ts-jest
```

### 1.2 TypeScript/Prisma セットアップ

```bash
# TypeScript設定
npx tsc --init

# Prisma初期化
npx prisma init --datasource-provider sqlite
```

### 1.3 Prisma Schema

```prisma
// prisma/schema.prisma
generator client {
  provider = "prisma-client-js"
}

datasource db {
  provider = "sqlite"
  url      = env("DATABASE_URL")
}

model Cards {
  cardId          Int      @id @default(autoincrement())
  mobileNumber    String   @unique
  cardNumber      String   @unique
  cardType        String
  totalLimit      Int
  amountUsed      Int
  availableAmount Int
  createdAt       DateTime @default(now())
  createdBy       String
  updatedAt       DateTime @updatedAt
  updatedBy       String?

  @@map("cards")
}
```

### 1.4 環境変数設定

```bash
# .env
NODE_ENV=development
PORT=9000
DATABASE_URL="file:./data/app.db"
SERVICE_NAME=cards
LOG_LEVEL=info

# Build info (静的値)
BUILD_VERSION=3.0
BUILD_DATE=2024-01-15
```

### 1.5 型定義

```typescript
// src/types/CardsDto.ts
import { z } from 'zod';

export const CardsSchema = z.object({
  mobileNumber: z.string().regex(/^\d{10}$/, "Mobile number must be 10 digits"),
  cardNumber: z.string().optional(),
  cardType: z.string().optional(),
  totalLimit: z.number().int().positive().optional(),
  amountUsed: z.number().int().min(0).optional(),
  availableAmount: z.number().int().min(0).optional()
});

export type CardsDto = z.infer<typeof CardsSchema>;

export const CardsUpdateSchema = z.object({
  mobileNumber: z.string().regex(/^\d{10}$/),
  cardNumber: z.string(),
  cardType: z.string(),
  totalLimit: z.number().int().positive(),
  amountUsed: z.number().int().min(0),
  availableAmount: z.number().int().min(0)
});

export type CardsUpdateDto = z.infer<typeof CardsUpdateSchema>;
```

```typescript
// src/types/ResponseDto.ts
export interface ResponseDto {
  statusCode: string;
  statusMsg: string;
}

export interface ErrorResponseDto {
  apiPath: string;
  errorCode: string;
  errorMessage: string;
  errorTime: string;
}
```

### 1.6 Repository層

```typescript
// src/repositories/CardsRepository.ts
import { PrismaClient, Cards } from '@prisma/client';

export class CardsRepository {
  constructor(private prisma: PrismaClient) {}

  async findByMobileNumber(mobileNumber: string): Promise<Cards | null> {
    return this.prisma.cards.findUnique({
      where: { mobileNumber }
    });
  }

  async create(cardData: Omit<Cards, 'cardId' | 'createdAt' | 'updatedAt'>): Promise<Cards> {
    return this.prisma.cards.create({
      data: {
        ...cardData,
        createdAt: new Date(),
        updatedAt: new Date()
      }
    });
  }

  async update(mobileNumber: string, cardData: Partial<Cards>): Promise<Cards> {
    return this.prisma.cards.update({
      where: { mobileNumber },
      data: {
        ...cardData,
        updatedAt: new Date()
      }
    });
  }

  async delete(mobileNumber: string): Promise<void> {
    await this.prisma.cards.delete({
      where: { mobileNumber }
    });
  }
}
```

### 1.7 Service層

```typescript
// src/services/CardsService.ts
import { CardsRepository } from '../repositories/CardsRepository';
import { CardsDto, CardsUpdateDto } from '../types/CardsDto';
import { Cards } from '@prisma/client';

export class CardsService {
  constructor(private cardsRepository: CardsRepository) {}

  async createCard(mobileNumber: string): Promise<void> {
    const existingCard = await this.cardsRepository.findByMobileNumber(mobileNumber);
    
    if (existingCard) {
      throw new CardAlreadyExistsException(
        `Card already registered with given mobileNumber ${mobileNumber}`
      );
    }

    const cardNumber = this.generateRandomCardNumber();
    const newCard = {
      mobileNumber,
      cardNumber,
      cardType: 'Credit Card',
      totalLimit: 100000,
      amountUsed: 0,
      availableAmount: 100000,
      createdBy: 'System',
      updatedBy: null
    };

    await this.cardsRepository.create(newCard);
  }

  async fetchCard(mobileNumber: string): Promise<CardsDto> {
    const card = await this.cardsRepository.findByMobileNumber(mobileNumber);
    
    if (!card) {
      throw new ResourceNotFoundException(
        `Card not found with given mobileNumber ${mobileNumber}`
      );
    }

    return this.mapToCardsDto(card);
  }

  async updateCard(cardsDto: CardsUpdateDto): Promise<boolean> {
    const card = await this.cardsRepository.findByMobileNumber(cardsDto.mobileNumber);
    
    if (!card) {
      throw new ResourceNotFoundException(
        `Card not found with given mobileNumber ${cardsDto.mobileNumber}`
      );
    }

    await this.cardsRepository.update(cardsDto.mobileNumber, {
      cardType: cardsDto.cardType,
      totalLimit: cardsDto.totalLimit,
      amountUsed: cardsDto.amountUsed,
      availableAmount: cardsDto.availableAmount,
      updatedBy: 'System'
    });

    return true;
  }

  async deleteCard(mobileNumber: string): Promise<boolean> {
    const card = await this.cardsRepository.findByMobileNumber(mobileNumber);
    
    if (!card) {
      throw new ResourceNotFoundException(
        `Card not found with given mobileNumber ${mobileNumber}`
      );
    }

    await this.cardsRepository.delete(mobileNumber);
    return true;
  }

  private generateRandomCardNumber(): string {
    return Math.floor(100000000000 + Math.random() * 900000000000).toString();
  }

  private mapToCardsDto(card: Cards): CardsDto {
    return {
      mobileNumber: card.mobileNumber,
      cardNumber: card.cardNumber,
      cardType: card.cardType,
      totalLimit: card.totalLimit,
      amountUsed: card.amountUsed,
      availableAmount: card.availableAmount
    };
  }
}

// カスタム例外クラス
export class CardAlreadyExistsException extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'CardAlreadyExistsException';
  }
}

export class ResourceNotFoundException extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'ResourceNotFoundException';
  }
}
```

### 1.8 Controller層

```typescript
// src/controllers/CardsController.ts
import { Request, Response, NextFunction } from 'express';
import { CardsService } from '../services/CardsService';
import { CardsUpdateSchema } from '../types/CardsDto';

export class CardsController {
  constructor(private cardsService: CardsService) {}

  createCard = async (req: Request, res: Response, next: NextFunction) => {
    try {
      const { mobileNumber } = req.query;
      
      if (!mobileNumber || typeof mobileNumber !== 'string') {
        return res.status(400).json({
          apiPath: req.path,
          errorCode: 'BAD_REQUEST',
          errorMessage: 'Mobile number is required',
          errorTime: new Date().toISOString()
        });
      }

      await this.cardsService.createCard(mobileNumber);
      
      res.status(201).json({
        statusCode: '201',
        statusMsg: 'Card created successfully'
      });
    } catch (error) {
      next(error);
    }
  };

  fetchCard = async (req: Request, res: Response, next: NextFunction) => {
    try {
      const { mobileNumber } = req.query;
      
      if (!mobileNumber || typeof mobileNumber !== 'string') {
        return res.status(400).json({
          apiPath: req.path,
          errorCode: 'BAD_REQUEST',
          errorMessage: 'Mobile number is required',
          errorTime: new Date().toISOString()
        });
      }

      const card = await this.cardsService.fetchCard(mobileNumber);
      res.status(200).json(card);
    } catch (error) {
      next(error);
    }
  };

  updateCard = async (req: Request, res: Response, next: NextFunction) => {
    try {
      const validatedData = CardsUpdateSchema.parse(req.body);
      const isUpdated = await this.cardsService.updateCard(validatedData);
      
      if (isUpdated) {
        res.status(200).json({
          statusCode: '200',
          statusMsg: 'Card updated successfully'
        });
      }
    } catch (error) {
      next(error);
    }
  };

  deleteCard = async (req: Request, res: Response, next: NextFunction) => {
    try {
      const { mobileNumber } = req.query;
      
      if (!mobileNumber || typeof mobileNumber !== 'string') {
        return res.status(400).json({
          apiPath: req.path,
          errorCode: 'BAD_REQUEST',
          errorMessage: 'Mobile number is required',
          errorTime: new Date().toISOString()
        });
      }

      const isDeleted = await this.cardsService.deleteCard(mobileNumber);
      
      if (isDeleted) {
        res.status(200).json({
          statusCode: '200',
          statusMsg: 'Card deleted successfully'
        });
      }
    } catch (error) {
      next(error);
    }
  };

  getBuildInfo = async (req: Request, res: Response) => {
    res.status(200).json({
      buildVersion: process.env.BUILD_VERSION || '3.0',
      buildDate: process.env.BUILD_DATE || new Date().toISOString().split('T')[0]
    });
  };

  getJavaVersion = async (req: Request, res: Response) => {
    res.status(200).json({
      nodeVersion: process.version,
      platform: process.platform,
      arch: process.arch
    });
  };

  getContactInfo = async (req: Request, res: Response) => {
    res.status(200).json({
      message: 'Welcome to KuroBank cards related local APIs',
      contactDetails: {
        name: 'John Doe - Developer',
        email: 'john@kurobank.com'
      },
      onCallSupport: ['(555) 123-4567', '(555) 765-4321']
    });
  };
}
```

### 1.9 ルーティング

```typescript
// src/routes/index.ts
import { Router } from 'express';
import { CardsController } from '../controllers/CardsController';
import { CardsService } from '../services/CardsService';
import { CardsRepository } from '../repositories/CardsRepository';
import { PrismaClient } from '@prisma/client';
import { correlationIdMiddleware } from '../middleware/correlationId';

const router = Router();
const prisma = new PrismaClient();
const cardsRepository = new CardsRepository(prisma);
const cardsService = new CardsService(cardsRepository);
const cardsController = new CardsController(cardsService);

// 相関IDチェックミドルウェア（fetch, update, delete時のみ必須）
const requireCorrelationId = (req: any, res: any, next: any) => {
  if (!req.headers['kurobank-correlation-id']) {
    return res.status(400).json({
      apiPath: req.path,
      errorCode: 'BAD_REQUEST',
      errorMessage: 'Missing trace header',
      errorTime: new Date().toISOString()
    });
  }
  next();
};

// API routes
router.post('/create', cardsController.createCard);
router.get('/fetch', requireCorrelationId, cardsController.fetchCard);
router.put('/update', cardsController.updateCard);
router.delete('/delete', cardsController.deleteCard);

// Info endpoints
router.get('/build-info', cardsController.getBuildInfo);
router.get('/java-version', cardsController.getJavaVersion);
router.get('/contact-info', cardsController.getContactInfo);

export { router };
```

### 1.10 アプリケーション エントリポイント

```typescript
// src/app.ts
import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import { router } from './routes';
import { errorHandler } from './middleware/errorHandler';
import { correlationIdMiddleware } from './middleware/correlationId';
import { requestLogger } from './middleware/requestLogger';

dotenv.config();

const app = express();

// ミドルウェア設定
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// カスタムミドルウェア
app.use(correlationIdMiddleware);
app.use(requestLogger);

// ルーティング
app.use('/api', router);

// ヘルスチェック
app.get('/actuator/health', (req, res) => {
  res.status(200).json({
    status: 'UP',
    timestamp: new Date().toISOString()
  });
});

// エラーハンドリング
app.use(errorHandler);

export default app;
```

```typescript
// src/server.ts
import app from './app';
import { PrismaClient } from '@prisma/client';

const PORT = process.env.PORT || 9000;
const prisma = new PrismaClient();

// データベース接続確認
prisma.$connect()
  .then(() => {
    console.log('Database connected successfully');
    
    app.listen(PORT, () => {
      console.log(`Cards service is running on port ${PORT}`);
      console.log(`Environment: ${process.env.NODE_ENV}`);
    });
  })
  .catch((error) => {
    console.error('Database connection failed:', error);
    process.exit(1);
  });

// Graceful shutdown
process.on('SIGINT', async () => {
  console.log('Shutting down gracefully...');
  await prisma.$disconnect();
  process.exit(0);
});
```

### 1.11 ミドルウェア実装

```typescript
// src/middleware/correlationId.ts
import { Request, Response, NextFunction } from 'express';
import { v4 as uuidv4 } from 'uuid';

declare global {
  namespace Express {
    interface Request {
      correlationId?: string;
    }
  }
}

export const correlationIdMiddleware = (req: Request, res: Response, next: NextFunction) => {
  const correlationId = req.headers['kurobank-correlation-id'] as string || uuidv4();
  req.correlationId = correlationId;
  res.setHeader('kurobank-correlation-id', correlationId);
  next();
};
```

```typescript
// src/middleware/errorHandler.ts
import { Request, Response, NextFunction } from 'express';
import { ZodError } from 'zod';
import { CardAlreadyExistsException, ResourceNotFoundException } from '../services/CardsService';

export const errorHandler = (error: Error, req: Request, res: Response, next: NextFunction) => {
  console.error(`[${req.correlationId}] Error:`, error);

  if (error instanceof ZodError) {
    return res.status(400).json({
      apiPath: req.path,
      errorCode: 'VALIDATION_ERROR',
      errorMessage: error.errors.map(e => e.message).join(', '),
      errorTime: new Date().toISOString()
    });
  }

  if (error instanceof CardAlreadyExistsException) {
    return res.status(400).json({
      apiPath: req.path,
      errorCode: 'BAD_REQUEST',
      errorMessage: error.message,
      errorTime: new Date().toISOString()
    });
  }

  if (error instanceof ResourceNotFoundException) {
    return res.status(404).json({
      apiPath: req.path,
      errorCode: 'RESOURCE_NOT_FOUND',
      errorMessage: error.message,
      errorTime: new Date().toISOString()
    });
  }

  // デフォルトエラー
  res.status(500).json({
    apiPath: req.path,
    errorCode: 'INTERNAL_SERVER_ERROR',
    errorMessage: 'An unexpected error occurred',
    errorTime: new Date().toISOString()
  });
};
```

```typescript
// src/middleware/requestLogger.ts
import { Request, Response, NextFunction } from 'express';

export const requestLogger = (req: Request, res: Response, next: NextFunction) => {
  const start = Date.now();
  
  res.on('finish', () => {
    const duration = Date.now() - start;
    console.log(`[${req.correlationId}] ${req.method} ${req.path} - ${res.statusCode} (${duration}ms)`);
  });
  
  next();
};
```

### 1.12 Package.json Scripts

```json
{
  "name": "@kurobank/cards-service",
  "version": "1.0.0",
  "scripts": {
    "dev": "nodemon src/server.ts",
    "build": "tsc",
    "start": "node dist/server.js",
    "test": "jest",
    "test:watch": "jest --watch",
    "db:generate": "prisma generate",
    "db:migrate": "prisma migrate dev",
    "db:studio": "prisma studio"
  },
  "dependencies": {
    "express": "^4.18.2",
    "cors": "^2.8.5",
    "dotenv": "^16.3.1",
    "prisma": "^5.7.0",
    "@prisma/client": "^5.7.0",
    "zod": "^3.22.4",
    "uuid": "^9.0.1"
  },
  "devDependencies": {
    "typescript": "^5.3.0",
    "@types/node": "^20.10.0",
    "@types/express": "^4.17.21",
    "@types/cors": "^2.8.17",
    "@types/uuid": "^9.0.7",
    "ts-node": "^10.9.0",
    "nodemon": "^3.0.2",
    "jest": "^29.7.0",
    "@types/jest": "^29.5.8",
    "ts-jest": "^29.1.1"
  }
}
```

---

## 2. Loans Service 移行

Loans ServiceはCards Serviceとほぼ同じ構造のため、以下の差分のみ記載：

### 2.1 主な差分

```typescript
// Loans用のPrisma Schema
model Loans {
  loanId            Int      @id @default(autoincrement())
  mobileNumber      String   @unique
  loanNumber        String   @unique
  loanType          String
  totalLoan         Int
  amountPaid        Int
  outstandingAmount Int
  createdAt         DateTime @default(now())
  createdBy         String
  updatedAt         DateTime @updatedAt
  updatedBy         String?

  @@map("loans")
}
```

```typescript
// LoansService の generateRandomLoanNumber
private generateRandomLoanNumber(): string {
  return Math.floor(100000000000 + Math.random() * 900000000000).toString();
}

// デフォルト値設定
const newLoan = {
  mobileNumber,
  loanNumber,
  loanType: 'Home Loan',
  totalLoan: 1000000,
  amountPaid: 0,
  outstandingAmount: 1000000,
  createdBy: 'System',
  updatedBy: null
};
```

### 2.2 環境設定

```bash
# .env (port変更)
PORT=8090
DATABASE_URL="file:./data/loans.db"
SERVICE_NAME=loans
```

---

## 3. Accounts Service 移行

最も複雑なサービス。他サービスとの通信を含む。

### 3.1 追加依存関係

```bash
npm install axios node-cache
npm install -D @types/node-cache
```

### 3.2 HTTP クライアント設定

```typescript
// src/clients/CardsClient.ts
import axios, { AxiosInstance } from 'axios';

export class CardsClient {
  private client: AxiosInstance;

  constructor(baseURL: string = 'http://cards:9000') {
    this.client = axios.create({
      baseURL,
      timeout: 5000,
      headers: {
        'Content-Type': 'application/json'
      }
    });
  }

  async fetchCardDetails(mobileNumber: string, correlationId: string) {
    try {
      const response = await this.client.get(`/api/fetch`, {
        params: { mobileNumber },
        headers: {
          'kurobank-correlation-id': correlationId
        }
      });
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 404) {
        return null; // カードが存在しない場合
      }
      throw error;
    }
  }
}
```

```typescript
// src/clients/LoansClient.ts
import axios, { AxiosInstance } from 'axios';

export class LoansClient {
  private client: AxiosInstance;

  constructor(baseURL: string = 'http://loans:8090') {
    this.client = axios.create({
      baseURL,
      timeout: 5000
    });
  }

  async fetchLoanDetails(mobileNumber: string, correlationId: string) {
    try {
      const response = await this.client.get(`/api/fetch`, {
        params: { mobileNumber },
        headers: {
          'kurobank-correlation-id': correlationId
        }
      });
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  }
}
```

### 3.3 Accounts Service (顧客詳細取得機能)

```typescript
// src/services/CustomersService.ts
import NodeCache from 'node-cache';
import { CustomerRepository } from '../repositories/CustomerRepository';
import { CardsClient } from '../clients/CardsClient';
import { LoansClient } from '../clients/LoansClient';
import { CustomerDetailsDto } from '../types/CustomerDetailsDto';

export class CustomersService {
  private cache: NodeCache;

  constructor(
    private customerRepository: CustomerRepository,
    private cardsClient: CardsClient,
    private loansClient: LoansClient
  ) {
    // TTL: 30分
    this.cache = new NodeCache({ stdTTL: 1800 });
  }

  async fetchCustomerDetails(mobileNumber: string, correlationId: string): Promise<CustomerDetailsDto> {
    const cacheKey = `customer_details_${mobileNumber}`;
    const cached = this.cache.get<CustomerDetailsDto>(cacheKey);
    
    if (cached) {
      console.log(`[${correlationId}] Cache hit for customer: ${mobileNumber}`);
      return cached;
    }

    // 顧客情報取得
    const customer = await this.customerRepository.findByMobileNumber(mobileNumber);
    if (!customer) {
      throw new ResourceNotFoundException(
        `Customer not found with given mobileNumber ${mobileNumber}`
      );
    }

    // 並行してカード・ローン情報を取得
    const [cardsDetails, loansDetails] = await Promise.allSettled([
      this.cardsClient.fetchCardDetails(mobileNumber, correlationId),
      this.loansClient.fetchLoanDetails(mobileNumber, correlationId)
    ]);

    const customerDetails: CustomerDetailsDto = {
      name: customer.name,
      email: customer.email,
      mobileNumber: customer.mobileNumber,
      accountsDto: {
        accountNumber: customer.accounts[0]?.accountNumber,
        accountType: customer.accounts[0]?.accountType,
        branchAddress: customer.accounts[0]?.branchAddress
      },
      cardsDto: cardsDetails.status === 'fulfilled' ? cardsDetails.value : null,
      loansDto: loansDetails.status === 'fulfilled' ? loansDetails.value : null
    };

    // キャッシュに保存
    this.cache.set(cacheKey, customerDetails);
    console.log(`[${correlationId}] Customer details cached: ${mobileNumber}`);

    return customerDetails;
  }
}
```

---

## 4. Message Service 移行

### 4.1 イベント処理パターン

```typescript
// src/handlers/MessageHandler.ts
import { EventEmitter } from 'events';

export class MessageHandler extends EventEmitter {
  constructor() {
    super();
    this.setupEventHandlers();
  }

  private setupEventHandlers() {
    this.on('account-created', this.handleAccountCreated);
    this.on('send-communication', this.handleSendCommunication);
  }

  private handleAccountCreated = async (eventData: any) => {
    console.log('Processing account created event:', eventData);
    
    // Email送信ロジック
    await this.sendEmail(eventData);
    
    // SMS送信ロジック  
    await this.sendSMS(eventData);
  };

  private handleSendCommunication = async (eventData: any) => {
    const { accountNumber } = eventData;
    
    // 通信送信処理
    console.log(`Sending communication for account: ${accountNumber}`);
    
    return { accountNumber };
  };

  private async sendEmail(data: any): Promise<void> {
    // Email送信実装
    console.log(`Email sent to: ${data.email}`);
  }

  private async sendSMS(data: any): Promise<void> {
    // SMS送信実装  
    console.log(`SMS sent to: ${data.mobileNumber}`);
  }
}
```

---

## 5. Gateway Service 移行

### 5.1 Express Gateway設定

```typescript
// src/gateway.ts
import express from 'express';
import { createProxyMiddleware } from 'http-proxy-middleware';
import { authMiddleware } from './middleware/auth';

const app = express();

app.use(express.json());
app.use(authMiddleware);

// Service routing
app.use('/kurobank/accounts', createProxyMiddleware({
  target: 'http://accounts:8080',
  changeOrigin: true,
  pathRewrite: {
    '^/kurobank/accounts': '/api'
  }
}));

app.use('/kurobank/cards', createProxyMiddleware({
  target: 'http://cards:9000',
  changeOrigin: true,
  pathRewrite: {
    '^/kurobank/cards': '/api'
  }
}));

app.use('/kurobank/loans', createProxyMiddleware({
  target: 'http://loans:8090',
  changeOrigin: true,
  pathRewrite: {
    '^/kurobank/loans': '/api'
  }
}));

export default app;
```

---

## テスト実行方法

```bash
# 各サービスで
npm run build
npm run db:generate
npm run dev

# 動作確認
curl -X POST http://localhost:9000/api/create?mobileNumber=1234567890
curl -H "kurobank-correlation-id: test123" http://localhost:9000/api/fetch?mobileNumber=1234567890
```

次は[API Documentation Migration](./api-documentation.md)を参照してください。