# Spring Boot Java プロジェクト → Express JavaScript 移行手順書作成依頼

## パス
`/home/wsl/dev/my-study/msa/microservices-springboot/docs/change`

## 移行目的
この k8s OSS ネイティブの Spring Boot Java プロジェクトを、Express JavaScript に変更する際のベストプラクティスな手順を`/home/wsl/dev/my-study/msa/microservices-springboot/docs/change`配下にマークダウンでまとめてください

---

## Runtime

- Node.js + Express 5系

---

## 使用ライブラリとバージョン

```json
"cors": "^2.8.5",
"dotenv": "^16.3.1",
"express": "^4.18.2",
"js-yaml": "^4.1.0",
"openapi-backend": "^5.10.6",
"zod": "^4.0.5",
"prisma": "^5.7.0"
```

---

## 使用技術

* API Definition: OpenAPI (openapi-backend)
* ORM: Prisma + SQLite
* Environment: dotenv
* CORS: cors
* Validation: zod
* テストコードは一旦不要

---

## 備考

* Spring Boot 側の `@RestController`, `@Service`, `@Repository`, `application.yml` などを Express + ミドルウェア形式に単純置換していく方針。
* openapi.yaml をベースに openapi-backend でルーティングを管理。
* バリデーションは zod によって置き換える。
* DB接続は Prisma + SQLite のローカル環境で代替。
* js-yaml で OpenAPI定義ファイルなどYAML読み込み可能にする。
* .env でポート番号やDB接続などを管理。