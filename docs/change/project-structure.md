# Express.js マイクロサービス プロジェクト構成

## プロジェクト全体構成

```
microservices-express/
├── services/
│   ├── accounts/                    # Accounts Service (Port: 8080)
│   │   ├── src/
│   │   │   ├── controllers/
│   │   │   │   ├── AccountsController.ts
│   │   │   │   └── CustomerController.ts
│   │   │   ├── services/
│   │   │   │   ├── AccountsService.ts
│   │   │   │   └── CustomersService.ts
│   │   │   ├── repositories/
│   │   │   │   ├── AccountsRepository.ts
│   │   │   │   └── CustomerRepository.ts
│   │   │   ├── clients/
│   │   │   │   ├── CardsClient.ts
│   │   │   │   └── LoansClient.ts
│   │   │   ├── types/
│   │   │   │   ├── AccountsDto.ts
│   │   │   │   ├── CustomerDto.ts
│   │   │   │   ├── CustomerDetailsDto.ts
│   │   │   │   └── ResponseDto.ts
│   │   │   ├── middleware/
│   │   │   │   ├── auth.ts
│   │   │   │   ├── correlationId.ts
│   │   │   │   ├── errorHandler.ts
│   │   │   │   └── validation.ts
│   │   │   ├── routes/
│   │   │   │   └── index.ts
│   │   │   ├── utils/
│   │   │   │   ├── logger.ts
│   │   │   │   └── constants.ts
│   │   │   ├── app.ts
│   │   │   └── server.ts
│   │   ├── prisma/
│   │   │   ├── schema.prisma
│   │   │   └── migrations/
│   │   ├── tests/
│   │   │   ├── unit/
│   │   │   ├── integration/
│   │   │   └── fixtures/
│   │   ├── docker/
│   │   │   └── Dockerfile
│   │   ├── package.json
│   │   ├── tsconfig.json
│   │   ├── .env.example
│   │   └── .gitignore
│   │
│   ├── cards/                       # Cards Service (Port: 9000)
│   │   └── [同じ構造]
│   │
│   ├── loans/                       # Loans Service (Port: 8090)
│   │   └── [同じ構造]
│   │
│   ├── gateway/                     # Gateway Service (Port: 8072)
│   │   ├── src/
│   │   │   ├── middleware/
│   │   │   │   ├── auth.ts
│   │   │   │   ├── rateLimiting.ts
│   │   │   │   └── circuitBreaker.ts
│   │   │   ├── config/
│   │   │   │   └── routes.ts
│   │   │   ├── app.ts
│   │   │   └── server.ts
│   │   └── [基本構造]
│   │
│   └── message/                     # Message Service (Port: 9010)
│       ├── src/
│       │   ├── handlers/
│       │   │   └── MessageHandler.ts
│       │   ├── services/
│       │   │   ├── EmailService.ts
│       │   │   └── SmsService.ts
│       │   └── [基本構造]
│
├── shared/                          # 共通ライブラリ
│   ├── types/
│   │   ├── common/
│   │   │   ├── ResponseDto.ts
│   │   │   ├── ErrorResponseDto.ts
│   │   │   └── BaseEntity.ts
│   │   └── events/
│   │       └── AccountCreatedEvent.ts
│   ├── middleware/
│   │   ├── correlationId.ts
│   │   ├── errorHandler.ts
│   │   ├── requestLogger.ts
│   │   └── validation.ts
│   ├── utils/
│   │   ├── logger.ts
│   │   ├── constants.ts
│   │   └── database.ts
│   └── config/
│       ├── database.ts
│       └── openapi.ts
│
├── docs/                            # ドキュメント
│   ├── api/
│   │   ├── accounts.yaml
│   │   ├── cards.yaml
│   │   ├── loans.yaml
│   │   └── gateway.yaml
│   ├── deployment/
│   │   ├── docker-compose.yml
│   │   ├── k8s/
│   │   └── helm/
│   └── change/                      # この移行ドキュメント
│
├── scripts/                         # 運用スクリプト
│   ├── build-all.sh
│   ├── test-all.sh
│   ├── deploy.sh
│   └── setup-dev.sh
│
├── docker-compose.yml               # 開発環境
├── docker-compose.prod.yml          # 本番環境
├── .env.example
├── .gitignore
├── README.md
└── Makefile
```

## 各サービス共通ディレクトリ構造

### サービスレベル構造

```
services/[service-name]/
├── src/                            # ソースコード
│   ├── controllers/                # REST API コントローラー
│   ├── services/                   # ビジネスロジック
│   ├── repositories/               # データアクセス層
│   ├── clients/                    # 外部サービスクライアント
│   ├── types/                      # TypeScript 型定義
│   ├── middleware/                 # Express ミドルウェア
│   ├── routes/                     # ルーティング設定
│   ├── utils/                      # ユーティリティ関数
│   ├── config/                     # 設定ファイル
│   ├── app.ts                      # Express アプリケーション設定
│   └── server.ts                   # サーバー起動スクリプト
│
├── prisma/                         # データベース関連
│   ├── schema.prisma               # Prisma スキーマ定義
│   ├── migrations/                 # DB マイグレーション
│   └── seed.ts                     # 初期データ投入
│
├── tests/                          # テストファイル
│   ├── unit/                       # 単体テスト
│   │   ├── controllers/
│   │   ├── services/
│   │   └── repositories/
│   ├── integration/                # 結合テスト
│   │   └── api/
│   ├── fixtures/                   # テストデータ
│   │   └── testData.ts
│   └── helpers/                    # テストユーティリティ
│       └── testSetup.ts
│
├── docker/                         # Docker関連
│   ├── Dockerfile                  # 本番用
│   ├── Dockerfile.dev              # 開発用
│   └── docker-compose.yml          # サービス単体テスト用
│
├── docs/                           # サービス固有ドキュメント
│   ├── api.md                      # API仕様書
│   ├── database.md                 # DB設計書
│   └── deployment.md               # デプロイ手順
│
├── scripts/                        # サービス固有スクリプト
│   ├── build.sh
│   ├── test.sh
│   └── deploy.sh
│
├── config/                         # 設定ファイル
│   ├── development.json
│   ├── production.json
│   └── test.json
│
├── package.json                    # Node.js 依存関係
├── tsconfig.json                   # TypeScript 設定
├── jest.config.js                  # Jest テスト設定
├── .env.example                    # 環境変数サンプル
├── .gitignore
└── README.md                       # サービス説明
```

## 共通ライブラリ (shared/) 詳細

### types/common/ - 共通型定義

```typescript
// shared/types/common/ResponseDto.ts
export interface ResponseDto {
  statusCode: string;
  statusMsg: string;
}

// shared/types/common/ErrorResponseDto.ts
export interface ErrorResponseDto {
  apiPath: string;
  errorCode: string;
  errorMessage: string;
  errorTime: string;
}

// shared/types/common/BaseEntity.ts
export interface BaseEntity {
  createdAt: Date;
  createdBy: string;
  updatedAt: Date;
  updatedBy?: string;
}
```

### middleware/ - 共通ミドルウェア

```typescript
// shared/middleware/correlationId.ts
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

### utils/ - 共通ユーティリティ

```typescript
// shared/utils/logger.ts
import winston from 'winston';

export const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: winston.format.combine(
    winston.format.timestamp(),
    winston.format.errors({ stack: true }),
    winston.format.json()
  ),
  transports: [
    new winston.transports.Console(),
    new winston.transports.File({ filename: 'logs/error.log', level: 'error' }),
    new winston.transports.File({ filename: 'logs/combined.log' })
  ]
});
```

```typescript
// shared/utils/constants.ts
export const HTTP_STATUS_CODES = {
  OK: 200,
  CREATED: 201,
  BAD_REQUEST: 400,
  NOT_FOUND: 404,
  INTERNAL_SERVER_ERROR: 500
} as const;

export const ERROR_CODES = {
  VALIDATION_ERROR: 'VALIDATION_ERROR',
  RESOURCE_NOT_FOUND: 'RESOURCE_NOT_FOUND',
  RESOURCE_ALREADY_EXISTS: 'RESOURCE_ALREADY_EXISTS',
  INTERNAL_SERVER_ERROR: 'INTERNAL_SERVER_ERROR'
} as const;
```

## 開発環境セットアップ

### 1. 前提条件

```bash
# 必要なツール
- Node.js 18+
- npm 8+
- Docker & Docker Compose
- SQLite3
```

### 2. プロジェクト初期化

```bash
# プロジェクト作成
mkdir microservices-express
cd microservices-express

# 共通ライブラリセットアップ
mkdir -p shared/{types,middleware,utils,config}

# サービス初期化スクリプト
cat > scripts/create-service.sh << 'EOF'
#!/bin/bash
SERVICE_NAME=$1
SERVICE_PORT=$2

if [ -z "$SERVICE_NAME" ] || [ -z "$SERVICE_PORT" ]; then
  echo "Usage: $0 <service-name> <service-port>"
  exit 1
fi

mkdir -p "services/$SERVICE_NAME"
cd "services/$SERVICE_NAME"

# package.json作成
cat > package.json << EOL
{
  "name": "@kurobank/$SERVICE_NAME-service",
  "version": "1.0.0",
  "scripts": {
    "dev": "nodemon src/server.ts",
    "build": "tsc",
    "start": "node dist/server.js",
    "test": "jest",
    "db:generate": "prisma generate",
    "db:migrate": "prisma migrate dev"
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
    "ts-node": "^10.9.0",
    "nodemon": "^3.0.2",
    "jest": "^29.7.0",
    "ts-jest": "^29.1.1"
  }
}
EOL

# ディレクトリ構造作成
mkdir -p src/{controllers,services,repositories,types,middleware,routes,utils}
mkdir -p tests/{unit,integration,fixtures}
mkdir -p prisma

# .env.example作成
cat > .env.example << EOL
NODE_ENV=development
PORT=$SERVICE_PORT
DATABASE_URL="file:./data/app.db"
SERVICE_NAME=$SERVICE_NAME
LOG_LEVEL=info
EOL

echo "Service $SERVICE_NAME created successfully"
EOF

chmod +x scripts/create-service.sh
```

### 3. サービス作成

```bash
# 各サービス作成
./scripts/create-service.sh accounts 8080
./scripts/create-service.sh cards 9000
./scripts/create-service.sh loans 8090
./scripts/create-service.sh gateway 8072
./scripts/create-service.sh message 9010
```

### 4. Docker Compose設定

```yaml
# docker-compose.yml (開発環境)
version: '3.8'

services:
  accounts:
    build: ./services/accounts
    ports:
      - "8080:8080"
    environment:
      - NODE_ENV=development
    volumes:
      - ./services/accounts:/app
      - accounts-data:/app/data
    depends_on:
      - cards
      - loans

  cards:
    build: ./services/cards
    ports:
      - "9000:9000"
    environment:
      - NODE_ENV=development
    volumes:
      - ./services/cards:/app
      - cards-data:/app/data

  loans:
    build: ./services/loans
    ports:
      - "8090:8090"
    environment:
      - NODE_ENV=development
    volumes:
      - ./services/loans:/app
      - loans-data:/app/data

  gateway:
    build: ./services/gateway
    ports:
      - "8072:8072"
    environment:
      - NODE_ENV=development
    depends_on:
      - accounts
      - cards
      - loans

  message:
    build: ./services/message
    ports:
      - "9010:9010"
    environment:
      - NODE_ENV=development

volumes:
  accounts-data:
  cards-data:
  loans-data:
```

### 5. Makefile

```makefile
# Makefile
.PHONY: help install build test start stop clean

help:
	@echo "Available commands:"
	@echo "  install    - Install dependencies for all services"
	@echo "  build      - Build all services"
	@echo "  test       - Run tests for all services"
	@echo "  start      - Start all services with Docker Compose"
	@echo "  stop       - Stop all services"
	@echo "  clean      - Clean build artifacts and dependencies"

install:
	@for service in services/*; do \
		if [ -d "$$service" ]; then \
			echo "Installing dependencies for $$service..."; \
			cd $$service && npm install && cd ../..; \
		fi \
	done

build: install
	@for service in services/*; do \
		if [ -d "$$service" ]; then \
			echo "Building $$service..."; \
			cd $$service && npm run build && cd ../..; \
		fi \
	done

test:
	@for service in services/*; do \
		if [ -d "$$service" ]; then \
			echo "Testing $$service..."; \
			cd $$service && npm test && cd ../..; \
		fi \
	done

start:
	docker-compose up -d

stop:
	docker-compose down

clean:
	@for service in services/*; do \
		if [ -d "$$service" ]; then \
			echo "Cleaning $$service..."; \
			cd $$service && rm -rf node_modules dist && cd ../..; \
		fi \
	done
	docker-compose down -v
```

## 開発ワークフロー

### 1. 新しいサービス追加

```bash
# サービス作成
./scripts/create-service.sh new-service 8100

# 依存関係インストール
cd services/new-service
npm install

# Prisma初期化
npx prisma init --datasource-provider sqlite

# 開発開始
npm run dev
```

### 2. 日常の開発作業

```bash
# 全サービス起動
make start

# 特定のサービスのみ開発モード
cd services/accounts
npm run dev

# テスト実行
make test

# ビルド
make build
```

### 3. API テスト

```bash
# 各サービスのヘルスチェック
curl http://localhost:8080/actuator/health
curl http://localhost:9000/actuator/health
curl http://localhost:8090/actuator/health

# API動作確認
curl -X POST http://localhost:9000/api/create?mobileNumber=1234567890
curl -H "kurobank-correlation-id: test123" \
     http://localhost:9000/api/fetch?mobileNumber=1234567890
```

この構成により、スケーラブルで保守性の高いマイクロサービスアーキテクチャをExpress.jsで実現できます。