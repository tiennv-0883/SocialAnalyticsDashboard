# Social Analytics Dashboard — Tài liệu dự án

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Lộ trình & tiến độ](#2-lộ-trình--tiến-độ)
3. [Giai đoạn 1 — Khởi tạo project & cấu trúc nền](#3-giai-đoạn-1--khởi-tạo-project--cấu-trúc-nền)
4. [Giai đoạn 2 — Unit Test + Import/Export Excel](#4-giai-đoạn-2--unit-test--importexport-excel)
5. [Giai đoạn 3 — Bảo mật & CSRF + Social Login](#5-giai-đoạn-3--bảo-mật--csrf--social-login)
6. [Giai đoạn 4 — Background Job & MultiThread](#6-giai-đoạn-4--background-job--multithread)
7. [Giai đoạn 5 — JMS & Queue Handling](#7-giai-đoạn-5--jms--queue-handling)
8. [Giai đoạn 6 — WebSocket + Chart Visualization](#8-giai-đoạn-6--websocket--chart-visualization)
9. [Giai đoạn 7 — Spring WebService + Reflection nâng cao + Tổng kết](#9-giai-đoạn-7--spring-webservice--reflection-nâng-cao--tổng-kết)
10. [Technical Requirements Summary](#10-technical-requirements-summary)
11. [Hướng dẫn chạy](#11-hướng-dẫn-chạy)
12. [Cấu trúc mã nguồn](#12-cấu-trúc-mã-nguồn)

---

## 1. Tổng quan

**Mô tả:**
Ứng dụng web giúp admin tổng hợp và theo dõi lượng tương tác (likes, shares, followers) từ Facebook & Twitter.

**Các chức năng chính:**

- Social Login (FB, TW)
- Import file Excel chứa danh sách bài viết
- Background job crawl & update metrics
- Hiển thị biểu đồ (Chart.js)
- Gửi thông báo realtime qua WebSocket
- Xuất kết quả ra file Excel
- Bảo mật với CSRF
- Một vài logic xử lý nền dùng Multithreading & JMS
- Dùng Reflection để xử lý động các kiểu entity export/import

Công nghệ sử dụng: xem [Technical Requirements Summary](#10-technical-requirements-summary).

---|---|
| Nền tảng | Java 17, Spring Boot 4.1.1, Maven (`./mvnw`) |
| Dữ liệu | Spring Data JPA, MySQL 8.4 (H2 in-memory khi test) |
| API docs | springdoc-openapi (Swagger UI) |
| Test | JUnit 5, Mockito, MockMvc, DataJpaTest |
| Excel | Apache POI |
| Bảo mật | Spring Security, CSRF, OAuth2 Client (Facebook, Twitter) |
| Giao diện & realtime | Chart.js, WebSocket |
| Xử lý nền | Multithreading, JMS |

---

## 2. Lộ trình & tiến độ

| # | Giai đoạn | Nội dung chính | Ngày | Trạng thái |
|---|---|---|---|---|
| 1 | Khởi tạo project & cấu trúc nền | Cấu trúc Spring Boot, JPA + MySQL, entity, API CRUD, Swagger | T4 14/10 | ✅ Đã hoàn thành |
| 2 | Unit Test + Import/Export Excel | JUnit5/Mockito, DataJpaTest, Apache POI, Reflection | T4 14/10 (làm bù 15/10) | ✅ Đã hoàn thành |
| 3 | Bảo mật & CSRF + Social Login | Spring Security, CSRF, OAuth2 Login FB/TW | T5 15/10 (làm 16/10) | ✅ Đã hoàn thành |
| 4 | Background Job & MultiThread | `@Scheduled`, `@Async`, ThreadPoolTaskExecutor | T6 16/10 | ✅ Đã hoàn thành |
| 5 | JMS & Queue Handling | ActiveMQ/RabbitMQ, Producer/Consumer, retry & DLQ | T6 16/10 | ✅ Đã hoàn thành |
| 6 | WebSocket + Chart Visualization | STOMP over WebSocket, SockJS + Chart.js | T2 19/10 | ⏳ Chưa thực hiện |
| 7 | Spring WebService + Reflection nâng cao + Tổng kết | Spring-WS (SOAP), Reflection, Integration Test, demo end-to-end | T3 20/10 | ⏳ Chưa thực hiện |

### Kế hoạch triển khai 5 ngày (14/10 – 20/10/2026)

5 ngày làm việc, nghỉ thứ 7 (17/10) và chủ nhật (18/10). Mỗi ngày kết thúc bằng: build + test xanh (`./mvnw test`), cập nhật mục "Kết quả thực hiện" của giai đoạn tương ứng, commit.

| Ngày | Thứ | Giai đoạn | Kết quả cuối ngày |
|---|---|---|---|
| 1 | T4 14/10 | 1 + 2 | Project nền, CRUD, Swagger; unit test service/controller; import/export Excel |
| 2 | T5 15/10 | 3 | Spring Security + CSRF, login Facebook/Twitter, trang dashboard Thymeleaf |
| 3 | T6 16/10 | 4 + 5 | Job cập nhật metrics mỗi giờ chạy đa luồng; JMS `IMPORT_COMPLETED` + retry/DLQ |
| 4 | T2 19/10 | 6 | Biểu đồ Chart.js cập nhật realtime qua WebSocket/STOMP; API `/chart-data` |
| 5 | T3 20/10 | 7 | SOAP client Spring-WS, Reflection export generic, Integration Test, demo end-to-end |

#### Ngày 1 — Thứ 4, 14/10: Nền tảng + Unit Test + Excel

- [x] Khởi tạo Spring Boot project, cấu trúc package, cấu hình MySQL + logging
- [x] Entity `User`, `Post`, `SocialMetric`; API CRUD `/posts`, `/metrics`; Swagger
- [x] Unit test `PostService`, `MetricService` (JUnit5 + Mockito, mock repository)
- [x] Test controller bằng `@WebMvcTest` + MockMvc + `@MockitoBean` (thay cho `@MockBean` ở Spring Boot mới)
- [x] `@DataJpaTest` cho `PostRepository`, `SocialMetricRepository`
- [x] Thêm Apache POI; util `ExcelMapper` dùng Reflection map dòng Excel → entity (annotation `@ExcelColumn` trên field)
- [x] API `POST /import-posts` (multipart `.xlsx` → validate → lưu DB, trả số dòng thành công/lỗi)
- [x] API `GET /export-report` (xuất posts + metric mới nhất ra `.xlsx`)
- [x] File Excel mẫu trong `docs/samples/`

**Hoàn thành khi:** import file mẫu → dữ liệu vào DB; export tải được file mở đúng bằng Excel; test service/controller/repository pass.

> **Ghi chú (cập nhật 15/10):**
> - Giai đoạn 1 đã xong và đã chạy thử trên MySQL 8.4 thật: tạo đủ 3 bảng, CRUD trả đúng mã (201/200/204/404), Swagger UI hoạt động, 4 test pass.
> - Phần giai đoạn 2 (unit test, import/export Excel) không kịp trong ngày 14/10, đã làm bù xong ngày 15/10 → giai đoạn 3 bắt đầu muộn hơn kế hoạch.
> - Đã chốt (15/10):
>   - Đường dẫn API theo tài liệu, không có tiền tố `/api` (`/posts`, `/metrics`, `/import-posts`, `/export-report`).
>   - Giữ Spring Boot 4.1.1.
>   - Chưa liên kết `Post` với `User`, để lại giai đoạn 3.
> - Máy dev đang có MySQL cài sẵn ở cổng 3306 → đã chốt dùng MySQL này; thông tin đăng nhập để trong `config/application-local.properties` (không commit).
> - Code giai đoạn 1 + 2 chưa commit, người dùng tự commit.
> - Khi viết test controller: `@MockBean` đã bị thay bằng `@MockitoBean` ở các bản Spring Boot mới.
> - Lỗi đã gặp: Swagger UI gửi `sort=["string"]` làm API trả 500 → đã sửa bằng `@ParameterObject` cho `Pageable` và kiểm tra field sort hợp lệ (sai trả 400).

#### Ngày 2 — Thứ 5, 15/10: Bảo mật, CSRF, Social Login

- [x] Thêm `spring-boot-starter-security`, `spring-boot-starter-oauth2-client`, Thymeleaf
- [x] `SecurityConfig`: phân quyền URL, form/login page, logout; CSRF bật (cookie token cho API gọi từ JS)
- [x] Trang Thymeleaf: login (nút "Login with Facebook / Twitter"), dashboard, form import Excel có CSRF token
- [x] Đăng ký app trên Meta for Developers và X Developer Portal; cấu hình `client-id`/`client-secret` qua biến môi trường
- [x] Cấu hình provider Facebook (có sẵn) và Twitter/X (khai báo thủ công: authorization/token/user-info URI, PKCE)
- [x] Custom `OAuth2UserService`: tạo/cập nhật `User` theo `provider` + `providerUserId`
- [x] Lưu access token qua `OAuth2AuthorizedClientService` (JPA, bảng `oauth_tokens`) để job dùng lại
- [x] Test: request không có CSRF token bị 403; user chưa đăng nhập bị redirect về login

**Hoàn thành khi:** đăng nhập bằng FB/TW → redirect về dashboard, thông tin user được lưu; form import vẫn hoạt động với CSRF.

> **Ghi chú:**
> - Đăng ký app Facebook và X (Twitter) càng sớm càng tốt; redirect URI dạng `http://localhost:8080/login/oauth2/code/{facebook|twitter}`.
> - Facebook có sẵn cấu hình provider trong Spring Security; X phải khai báo thủ công các URI và bật PKCE.
> - Bật CSRF xong, các lệnh POST/PUT/DELETE từ Swagger UI hoặc `curl` vào các API (`/posts`, `/metrics`, `/import-posts`...) sẽ bị 403 nếu không gửi token → cần chọn cách xử lý (cookie `XSRF-TOKEN` hoặc bỏ CSRF riêng cho API dùng token).
> - Form Thymeleaf dùng `th:action` sẽ tự chèn CSRF token.
> - Cập nhật 16/10:
>   - Đã đăng ký app Facebook + X, key để trong `config/application-local.properties`.
>   - Facebook không cho lưu redirect URI `http://...` (luôn bật Enforce HTTPS) → để trống; ở chế độ Development, Facebook tự cho phép `localhost`.
>   - X không chấp nhận `localhost` → callback dùng `http://127.0.0.1:8080/...`; trang login tự đưa nút X sang `127.0.0.1`.
>   - Spring Security 7 tự bật PKCE cho mọi provider → không cần cấu hình riêng cho X.
>   - CSRF chọn cách cookie `XSRF-TOKEN` (`csrf().spa()`), Swagger UI tự gửi header `X-XSRF-TOKEN`.
>   - Đã đăng nhập thật thành công bằng cả Facebook và X; user + token được lưu vào `users`, `oauth_tokens`.
>   - Lỗi đã gặp với X: lấy được token nhưng `GET /2/users/me` trả 403 `client-not-enrolled` → app X phải nằm trong một **Project** (không để Standalone App). Đã sửa trên X Developer Portal.
>   - Đăng nhập thất bại: lý do được ghi log (`WARN Social login failed ...`) và hiện trên trang `/login?error`.


#### Ngày 3 — Thứ 6, 16/10: Background Job, MultiThread, JMS

- [x] `@EnableScheduling`, `@EnableAsync`; cấu hình `ThreadPoolTaskExecutor` (core/max pool, queue, prefix tên thread)
- [x] Interface `SocialApiClient` + bản mock cho FB/TW (trả số liệu ngẫu nhiên tăng dần)
- [x] `updateSocialMetricsJob` chạy mỗi 1 giờ (`cron` cấu hình được): chia bài viết theo tài khoản, xử lý song song bằng `@Async`/`CompletableFuture`
- [x] Logging theo thread, bắt lỗi từng tài khoản không làm dừng cả job; lưu thời điểm chạy cuối
- [x] Hiển thị "Last updated time" trên dashboard
- [x] Thêm JMS (ActiveMQ Artemis embedded cho dev, Docker cho môi trường thật)
- [x] Producer: sau khi import Excel xong → gửi message `IMPORT_COMPLETED`
- [x] Consumer `@JmsListener` → cập nhật bảng thống kê tổng hợp (async)
- [x] Cấu hình retry (redelivery) và DLQ; test bằng message lỗi cố ý

**Hoàn thành khi:** job tự chạy và cập nhật metrics; import xong thì thống kê tổng hợp được cập nhật qua queue; message lỗi rơi vào DLQ.

> **Ghi chú:**
> - Đây là ngày nặng nhất (2 giai đoạn): ưu tiên job + đa luồng trước, JMS sau.
> - `@Async` không có tác dụng khi gọi method trong cùng một class (self-invocation) → tách sang bean riêng.
> - Tránh job chạy chồng lên nhau khi lần trước chưa xong (dùng `fixedDelay` hoặc cờ khóa).
> - Dùng ActiveMQ Artemis embedded để không phải cài thêm broker khi dev.
> - Cập nhật 16/10 (nhánh `feat/background_job_and_jms`):
>   - Hoàn thành cả giai đoạn 4 và 5 trong ngày, kịp kế hoạch.
>   - Job chống chạy chồng bằng cờ `AtomicBoolean` (lần chạy trùng bị bỏ qua và ghi log), thay vì `fixedDelay`, để giữ được lịch cron mỗi giờ.
>   - Dùng ActiveMQ Artemis nhúng (`spring.artemis.mode=embedded`), message không lưu xuống đĩa (mất khi tắt app); môi trường thật nên dùng broker riêng.
>   - Mock API mặc định lỗi ngẫu nhiên 5% mỗi lần gọi để thấy trạng thái `PARTIAL` và xử lý lỗi theo từng bài; đặt `app.social-api.mock.failure-rate=0` để tắt.


#### Ngày 4 — Thứ 2, 19/10: WebSocket + Biểu đồ

- [ ] Thêm `spring-boot-starter-websocket`; cấu hình STOMP endpoint `/ws` (SockJS), broker `/topic`
- [ ] Sau khi job / listener cập nhật xong → broadcast event lên `/topic/metrics`
- [ ] API `GET /chart-data` (tổng likes/shares/followers theo ngày, lọc theo nền tảng)
- [ ] Dashboard: Chart.js vẽ biểu đồ từ `/chart-data`; client SockJS + STOMP nhận event và cập nhật biểu đồ không cần reload
- [ ] Thông báo realtime (toast) khi import xong / job chạy xong
- [ ] Đảm bảo WebSocket đi qua Spring Security (chỉ user đã đăng nhập)
- [ ] Test `/chart-data` bằng MockMvc

**Hoàn thành khi:** mở dashboard, chạy job thủ công → biểu đồ tự cập nhật.

> **Ghi chú:**
> - Nếu phần retry/DLQ của ngày 3 chưa xong thì làm đầu ngày này.
> - Khi đã bật Spring Security, kết nối STOMP cũng cần xác thực/CSRF → kiểm tra sớm để không bị chặn kết nối.
> - Thư viện phía client (SockJS, STOMP.js, Chart.js) nạp từ CDN, ghim phiên bản cụ thể.

#### Ngày 5 — Thứ 3, 20/10: SOAP, Reflection nâng cao, Tổng kết

- [ ] Thêm Spring-WS; tạo SOAP endpoint mock (exchange rate) + client gọi đến nó
- [ ] Nâng cấp `ExcelMapper` thành export/import generic cho mọi entity bằng Reflection (gọi method, set/get field động)
- [ ] Viết Integration Test cuối cùng: import → job → JMS → chart-data (Testcontainers MySQL nếu có Docker)
- [ ] Đo test coverage (JaCoCo), bổ sung test còn thiếu
- [ ] Đóng gói `./mvnw package`, chạy jar + `docker compose` demo end-to-end
- [ ] Hoàn thiện tài liệu: kết quả từng giai đoạn, sơ đồ kiến trúc, hướng dẫn chạy

**Hoàn thành khi:** demo end-to-end chạy được từ jar; toàn bộ test pass; tài liệu cập nhật đầy đủ.

> **Ghi chú:**
> - Tự dựng SOAP endpoint mock trong project thay vì gọi dịch vụ SOAP công khai (thường không ổn định).
> - Testcontainers cần Docker (máy dev đã có sẵn).
> - Dành thời gian cuối ngày cập nhật đầy đủ mục "Kết quả thực hiện" của mọi giai đoạn và bảng tiến độ.

#### Rủi ro & cách xử lý

| Rủi ro | Cách xử lý |
|---|---|
| Chưa có app Facebook/Twitter hoặc chưa được duyệt quyền | Dùng tài khoản test của app ở chế độ Development; chuẩn bị login local làm phương án dự phòng để demo |
| API thật của FB/TW giới hạn quyền đọc metrics | Dùng mock `SocialApiClient` như yêu cầu tài liệu |
| Ngày 3 nhiều việc (2 giai đoạn) | Ưu tiên job + đa luồng trước; nếu thiếu thời gian, chuyển phần retry/DLQ sang đầu ngày 4 |
| Lệch phiên bản (yêu cầu Spring Boot 3.x, project dùng 4.1.1) | Chốt phiên bản ngay ngày 1 trước khi thêm thư viện mới |

---

## 3. Giai đoạn 1 — Khởi tạo project & cấu trúc nền

**Mục tiêu:**

- Ôn lại cấu trúc chuẩn Spring Boot
- Thiết lập project cho mini project

**Nội dung học:**

- Cấu trúc module: Controller → Service → Repository → DTO → Util
- Application.properties, Logging config
- Cấu hình Maven/Gradle, JPA + MySQL
- Tạo entity `User`, `Post`, `SocialMetric`

**Thực hành:**

- Tạo Spring Boot project (`social-analytics`)
- Tạo bảng & API CRUD cơ bản (`/posts`, `/metrics`)
- Thêm Swagger/OpenAPI

### Kết quả thực hiện

| Hạng mục | Thực hiện |
|---|---|
| Project | Spring Boot 4.1.1, Java 17, Maven (`./mvnw`), artifact `social-analytics` |
| Cấu trúc package | `com.example.socialanalytics.{controller, service, repository, dto, util, entity, exception, config}` |
| Cấu hình | `application.properties` (MySQL qua biến môi trường `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), `logback-spring.xml` (log ra console + `logs/social-analytics.log`) |
| JPA + MySQL | `spring-boot-starter-data-jpa`, `mysql-connector-j`, `ddl-auto=update`; file `compose.yaml` để chạy MySQL 8.4 |
| Entity | `User` (tài khoản admin đăng nhập qua FB/TW), `Post` (bài viết theo dõi), `SocialMetric` (snapshot likes/shares/comments/followers theo thời gian) — đều kế thừa `BaseEntity` (id, createdAt, updatedAt) |
| API CRUD | `/posts`, `/metrics` (GET list có phân trang + lọc + sắp xếp, GET by id, POST, PUT, DELETE) |
| Swagger | springdoc-openapi: UI tại `/swagger-ui.html`, spec tại `/v3/api-docs` |
| Test | `PostMetricApiTests` chạy trên H2 in-memory (profile `test`, không cần MySQL) |

**Quan hệ dữ liệu:**

```
users (id, username, email, display_name, provider, provider_user_id, created_at, updated_at)

posts (id, platform, external_id, url, title, content, created_at, updated_at)
  └── 1 - n ── social_metrics (id, post_id, likes, shares, comments, followers, collected_at, ...)
```

**Danh sách API** (từ giai đoạn 3: phải đăng nhập; POST/PUT/DELETE cần CSRF token):

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/posts?platform=&page=&size=&sort=` | Danh sách bài viết (lọc theo nền tảng, phân trang). `sort` cho phép: `id`, `platform`, `externalId`, `title`, `createdAt`, `updatedAt` |
| GET | `/posts/{id}` | Chi tiết bài viết |
| POST | `/posts` | Tạo bài viết (409 nếu trùng `platform` + `externalId`) |
| PUT | `/posts/{id}` | Cập nhật bài viết |
| DELETE | `/posts/{id}` | Xóa bài viết và các metric của nó |
| GET | `/metrics?postId=&page=&size=&sort=` | Danh sách metric (lọc theo bài viết, mặc định mới nhất trước). `sort` cho phép: `id`, `likes`, `shares`, `comments`, `followers`, `collectedAt`, `createdAt` |
| GET | `/metrics/{id}` | Chi tiết metric |
| POST | `/metrics` | Ghi nhận metric cho bài viết |
| PUT | `/metrics/{id}` | Cập nhật metric |
| DELETE | `/metrics/{id}` | Xóa metric |
| POST | `/jobs/update-metrics/run` | Chạy `updateSocialMetricsJob` ngay (nền); `409` nếu đang chạy |
| GET | `/jobs/update-metrics/last` | Kết quả lần chạy gần nhất |

---

## 4. Giai đoạn 2 — Unit Test + Import/Export Excel

**Mục tiêu:**

- Viết test cho Service & Controller
- Import/export Excel với Apache POI

**Nội dung học:**

- JUnit5 + Mockito: test service, test controller
- DataJpaTest
- Cấu trúc file test, MockBean, MockMvc
- Dùng Apache POI để đọc/ghi file Excel
- Áp dụng Reflection để map dữ liệu từ file Excel vào entity

**Thực hành:**

- Viết test cho `PostService`, `MetricService`
- API `/import-posts` (upload Excel → save DB)
- API `/export-report` (export Excel)

### Kết quả thực hiện

| Hạng mục | Thực hiện |
|---|---|
| Unit test service | `PostServiceTest` (8 test), `MetricServiceTest` (6 test): JUnit5 + Mockito (`@Mock`, `@InjectMocks`), không cần Spring context |
| Test controller | `PostControllerTest`, `MetricControllerTest`, `ExcelControllerTest`: `@WebMvcTest` + MockMvc + `@MockitoBean` (thay `@MockBean`) — kiểm tra status code, JSON, validation, lỗi 404/409/400 |
| Test repository | `RepositoryTest`: `@DataJpaTest` + `TestEntityManager` trên H2 — query theo platform, kiểm tra trùng, lấy metric mới nhất mỗi bài, xóa metric theo bài |
| Test Excel | `ExcelMapperTest`: ghi rồi đọc lại, header khác thứ tự/hoa thường, dòng trống, giá trị enum sai, file không phải Excel |
| Test tích hợp | `ExcelApiTests`: upload Excel → lưu DB → thêm metric → export, đọc lại file export để kiểm tra |
| Apache POI | `poi-ooxml` 5.5.1, đọc/ghi `.xlsx` |
| Reflection | `excel/ExcelMapper` + annotation `@ExcelColumn(name, order, required)`: tìm field có annotation, tạo object bằng constructor không tham số, `Field.set/get` để map dòng Excel ↔ object. Dùng được cho mọi class, không viết code riêng cho từng loại |
| `POST /import-posts` | Upload `.xlsx` (field `file`, tối đa 5MB). Header: `platform, externalId, url, title, content` (không phân biệt hoa thường, thứ tự tùy ý). Dòng hợp lệ được lưu, dòng lỗi (sai dữ liệu, trùng, platform không hỗ trợ, lỗi ràng buộc DB) được trả về trong `errors` kèm số dòng, không làm dừng cả file. Trả `400` cho cả file khi: quá 5.000 dòng (đếm bằng SAX trước khi nạp workbook), thiếu cột bắt buộc (`platform`, `externalId`, `url`). Ô công thức được tính ra giá trị (`FormulaEvaluator`). `content` tối đa 16.000 ký tự |
| `GET /export-report` | Tải file `social-report-<ngày>.xlsx`: mỗi bài viết + metric mới nhất (likes, shares, comments, followers, lastCollectedAt UTC) |
| File mẫu | `docs/samples/posts-sample.xlsx` (5 bài viết) |
| Kết quả test | 44 test, tất cả pass (`./mvnw test`) tại thời điểm hoàn thành giai đoạn 2 |

**Ví dụ kết quả import:**

```json
{
  "totalRows": 5, "imported": 2, "failed": 3,
  "errors": [
    { "row": 4, "message": "Post already exists: FACEBOOK/imp-fb-1" },
    { "row": 5, "message": "url must not be blank" },
    { "row": 6, "message": "Column 'platform': 'INSTAGRAM' must be one of [FACEBOOK, TWITTER]" }
  ]
}
```

**Thử nhanh:**

```bash
curl -F "file=@docs/samples/posts-sample.xlsx" localhost:8080/import-posts
curl -o report.xlsx localhost:8080/export-report
```

---

## 5. Giai đoạn 3 — Bảo mật & CSRF + Social Login

**Mục tiêu:**

- Hiểu cơ chế CSRF và tích hợp bảo mật OAuth2 Login (FB, TW)

**Nội dung học:**

- Spring Security cơ bản
- CSRF token và form submission
- Cấu hình OAuth2 client (Facebook, Twitter)
- Sử dụng `spring-boot-starter-oauth2-client`

**Thực hành:**

- Bật CSRF, thêm filter token vào form
- Login với Facebook & Twitter → redirect về dashboard
- Lưu token truy cập và thông tin user

### Kết quả thực hiện

| Hạng mục | Thực hiện |
|---|---|
| Thư viện | `spring-boot-starter-security`, `spring-boot-starter-security-oauth2-client`, `spring-boot-starter-thymeleaf` (Spring Security 7.1.1) |
| `SecurityConfig` | Trang công khai: `/login`, `/css/**`, Swagger UI, `/v3/api-docs`. Còn lại phải đăng nhập. Chưa đăng nhập: trang web → chuyển về `/login`; API (`/posts`, `/metrics`, `/import-posts`, `/export-report`) → `401` |
| CSRF | Bật. Token nằm trong cookie `XSRF-TOKEN` (`CsrfCookieFilter` luôn phát cookie) và trường ẩn `_csrf` trong form Thymeleaf. POST/PUT/DELETE thiếu token → `403`. Swagger UI tự gửi header `X-XSRF-TOKEN` (`springdoc.swagger-ui.csrf.enabled=true`) |
| OAuth2 Login | Facebook (scope `public_profile`, `email`) và X (scope `users.read`, `tweet.read`, khai báo provider thủ công). PKCE (S256) bật cho cả hai. Đăng nhập xong → `/dashboard`; lỗi → `/login?error`; đăng xuất (POST `/logout`) → `/login?logout` |
| Lưu thông tin user | `SocialOAuth2UserService`: chuẩn hóa profile 2 bên (X trả về trong `data`, không có email), tạo mới hoặc cập nhật `User` theo `provider` + `providerUserId`, ghi `lastLoginAt` |
| Lưu token truy cập | `JpaOAuth2AuthorizedClientService` → bảng `oauth_tokens` (access token, hạn dùng, scope, refresh token nếu có, liên kết `user_id`). Mỗi user + provider một dòng, đăng nhập lại thì cập nhật |
| Giao diện | `login.html` (2 nút đăng nhập), `dashboard.html` (thông tin user, số bài viết/metric, form import Excel có CSRF, tải báo cáo, link Swagger, đăng xuất) |
| Báo lỗi đăng nhập | `LoggingAuthenticationFailureHandler`: ghi log lý do (ví dụ lỗi từ X/Facebook) và hiện trên trang `/login?error` |
| Test | `SecurityConfigTests` (chặn truy cập, 401/302, CSRF 403/201, PKCE, trang dashboard, hiện lỗi đăng nhập, logout), `CsrfCookieTests`, `SocialOAuth2UserServiceTest`, `JpaOAuth2AuthorizedClientServiceTest`. Tổng 60 test pass |
| Kiểm thử thật | Đăng nhập thành công bằng Facebook và X (16/10) |

**Cách dùng:**

1. Chạy app, mở http://localhost:8080 → trang đăng nhập.
2. **Facebook:** bấm "Đăng nhập với Facebook" (chạy trên `localhost`).
3. **X:** bấm "Đăng nhập với X" (tự chuyển sang `127.0.0.1`, vì X không chấp nhận `localhost`).
4. Sau khi đăng nhập, Swagger UI (http://localhost:8080/swagger-ui.html, cùng host với lúc đăng nhập) dùng được session hiện tại.

**Lưu ý:**

- Access token đang lưu dạng chữ thường (chưa mã hóa) trong DB. Chấp nhận được cho dự án học tập; môi trường thật cần mã hóa.
- Facebook ở chế độ Development: chỉ tài khoản có vai trò trong app (Admin/Developer/Tester) đăng nhập được.
- `config/application-local.properties` phải có đủ client-id/secret của cả 2 provider, nếu thiếu app không khởi động được.
- App X phải nằm trong một Project trên X Developer Portal, nếu không `/2/users/me` trả 403 `client-not-enrolled`.

---

## 6. Giai đoạn 4 — Background Job & MultiThread

**Mục tiêu:**

- Tự động cập nhật dữ liệu mạng xã hội định kỳ

**Nội dung học:**

- `@Scheduled` và ThreadPoolTaskExecutor
- `@Async` và `@EnableAsync`
- Kết hợp đa luồng xử lý nhiều tài khoản cùng lúc
- Logging và exception handling cho thread

**Thực hành:**

- Tạo job `updateSocialMetricsJob` chạy mỗi 1 giờ
- Cập nhật metrics (mock API từ FB/TW)
- Thêm màn hình "Last updated time"

### Kết quả thực hiện

| Hạng mục | Thực hiện |
|---|---|
| Bật tính năng | `job/AsyncConfig`: `@EnableScheduling`, `@EnableAsync`, `AsyncUncaughtExceptionHandler` ghi log lỗi của `@Async void` |
| Thread pool | `ThreadPoolTaskExecutor` tên `metricsTaskExecutor`, thread `metrics-N`; core/max/queue cấu hình qua `app.jobs.update-metrics.*`; pool và queue đầy → `CallerRunsPolicy` (chạy trên thread gọi, không bỏ task); chờ task xong khi tắt app (tối đa 30s) |
| Mock API | `SocialApiClient` + `MockSocialApiClient` cho FACEBOOK và TWITTER: số liệu chỉ tăng, có độ trễ (`app.social-api.mock.latency-ms`, mặc định 200ms) và tỉ lệ lỗi (`failure-rate`, mặc định 5%). Chỉ bật khi `app.social-api.mock.enabled=true` (`MockSocialApiConfig`); mặc định tắt, máy dev bật trong `config/application-local.properties`. Nền tảng không có client thì bài viết của nó được bỏ qua (log `WARN`) |
| `updateSocialMetricsJob` | `@Scheduled(cron = "0 0 * * * *")` (mỗi giờ, cấu hình được, `-` để tắt). Bài viết được nhóm theo nền tảng, chia nhóm `chunk-size` (mặc định 20), mỗi nhóm là một task `@Async` trên `MetricsUpdateWorker` (bean riêng để `@Async` đi qua proxy), chờ tất cả bằng `CompletableFuture.allOf` |
| Xử lý lỗi | Lỗi một bài chỉ đánh dấu bài đó (log `WARN [job run N] post X failed`), các bài khác vẫn cập nhật. Trạng thái lần chạy: `SUCCESS` / `PARTIAL` / `FAILED`. Lần chạy dở do tắt app được đánh dấu `FAILED` khi khởi động lại |
| Chống chạy chồng | Đang chạy thì lần kế tiếp (theo lịch hoặc bấm tay) bị bỏ qua. Cờ luôn được mở lại trong `finally`, kể cả khi ghi `job_runs` lúc bắt đầu bị lỗi (DB mất kết nối) |
| Kiểm tra cấu hình | `JobProperties`, `RetentionProperties` có `@Validated`: `chunk-size`, số thread, số ngày giữ dữ liệu phải ≥ 1, cron không trống; sai thì app không khởi động |
| Dọn dữ liệu | `MetricRetentionJob` chạy 03:00 hằng ngày: xóa snapshot cũ hơn `app.jobs.metrics-retention.days` (mặc định 90) theo lô 1.000, luôn giữ snapshot mới nhất của mỗi bài (báo cáo export cần) |
| Lưu lịch sử | Bảng `job_runs`: trigger (`SCHEDULED`/`MANUAL`), trạng thái, thời gian bắt đầu/kết thúc, số bài tổng/cập nhật/lỗi, lỗi đầu tiên |
| Màn hình | Dashboard: **Last updated time** (lần chạy gần nhất có ghi dữ liệu), kết quả lần chạy gần nhất, nút "Chạy cập nhật ngay" (form có CSRF) |
| API | `POST /jobs/update-metrics/run` → `202` (chạy nền) hoặc `409` nếu đang chạy; `GET /jobs/update-metrics/last` → lần chạy gần nhất (`204` nếu chưa chạy) |
| Test | `MockSocialApiClientTest`, `MetricsUpdateWorkerTest` (lỗi từng bài), `UpdateSocialMetricsJobTest` (chia nhóm, PARTIAL/FAILED, bỏ qua khi đang chạy, lỗi lúc ghi bắt đầu không khóa job, bỏ qua nền tảng không có client), `JobConfigurationTest` (cấu hình sai không khởi động, mock chỉ bật theo property), `MetricRetentionJobTest`, `UpdateSocialMetricsJobTests` (pool thật: mọi bài được cập nhật trên thread `metrics-*`, API 202/401) |

---

## 7. Giai đoạn 5 — JMS & Queue Handling

**Mục tiêu:**

- Học cơ chế messaging giữa các service

**Nội dung học:**

- Giới thiệu JMS (ActiveMQ hoặc RabbitMQ)
- Tạo Queue, Listener
- Producer & Consumer message
- Xử lý retry và DLQ (Dead Letter Queue)

**Thực hành:**

- Khi người dùng import Excel → publish message `IMPORT_COMPLETED`
- Listener nhận message → cập nhật thống kê tổng hợp (async)

### Kết quả thực hiện

| Hạng mục | Thực hiện |
|---|---|
| Broker | ActiveMQ Artemis nhúng trong app (`spring-boot-starter-artemis` + `artemis-jakarta-server`), không cần cài thêm |
| Queue | `social.import.completed` và DLQ `social.import.completed.DLQ` (`messaging/Queues`) |
| Message | `IMPORT_COMPLETED` (`ImportCompletedEvent`): importId, tên file, số dòng tổng/thành công/lỗi, số bài theo nền tảng, thời điểm. Gửi dạng JSON (`JacksonJsonMessageConverter`, thuộc tính `_type=IMPORT_COMPLETED`) |
| Producer | `ImportEventPublisher`: `PostExcelService` gửi sau khi import xong (dữ liệu đã lưu). Broker lỗi chỉ ghi log, không làm hỏng kết quả import |
| Consumer | `ImportStatsListener` (`@JmsListener`, chạy bất đồng bộ trên thread của listener container): lưu `import_logs`, cập nhật `platform_stats` (số bài hiện có, số bài đã import, số lần import, lần import gần nhất) |
| Idempotent | `importId` unique: message bị giao lại không bị đếm hai lần |
| Retry & DLQ | Listener ném lỗi → session JMS rollback → broker giao lại tối đa 3 lần, delay 1s tăng gấp đôi (`app.jms.import-completed.*`), sau đó chuyển vào DLQ |
| Màn hình | Dashboard: bảng "Thống kê tổng hợp (cập nhật qua JMS)" theo nền tảng + 5 lần import gần nhất |
| Test | `ImportMessagingTests` (broker thật): import → thống kê cập nhật; cùng message gửi 2 lần chỉ tính 1 lần; message lỗi được thử lại rồi vào DLQ. `PostExcelServiceTest` kiểm tra nội dung message gửi đi |
| Kết quả test | 82 test, tất cả pass |

---

## 8. Giai đoạn 6 — WebSocket + Chart Visualization

**Mục tiêu:**

- Hiển thị biểu đồ realtime khi có dữ liệu mới

**Nội dung học:**

- STOMP over WebSocket
- Spring Messaging
- Client side với SockJS + Chart.js
- Controller gửi broadcast event khi job hoàn tất

**Thực hành:**

- Khi có dữ liệu mới (metrics update) → gửi WebSocket event
- Frontend nhận và update Chart.js
- API `/chart-data` trả dữ liệu tổng hợp

### Kết quả thực hiện

_Chưa thực hiện._

---

## 9. Giai đoạn 7 — Spring WebService + Reflection nâng cao + Tổng kết

**Mục tiêu:**

- Kết nối WebService SOAP
- Áp dụng Reflection nâng cao và tổng hợp project

**Nội dung học:**

- Spring-WS: tạo và tiêu thụ SOAP endpoint
- Reflection: gọi method, set/get field động
- Kỹ thuật generic export/import model
- Tổng kết kiến trúc & review test coverage

**Thực hành:**

- Gọi 1 WebService mock (ví dụ: exchange rate API SOAP)
- Dùng Reflection để tự động xuất model thành Excel
- Viết Integration Test cuối cùng
- Đóng gói project, chạy end-to-end demo

### Kết quả thực hiện

_Chưa thực hiện._

---

## 10. Technical Requirements Summary

| Category | Requirement |
|---|---|
| Framework | Spring Boot 3.x |
| Security | Spring Security + OAuth2 Login + CSRF |
| Database | MySQL / PostgreSQL |
| Messaging | JMS (ActiveMQ / RabbitMQ) |
| Realtime | WebSocket + STOMP |
| Scheduler | @Scheduled + @Async |
| Multithread | ThreadPoolTaskExecutor |
| File Handling | Apache POI for Excel import/export |
| WebService | SOAP integration (Spring-WS) |
| Chart | Chart.js or ECharts |
| Testing | JUnit5 + Mockito + MockMvc |
| API Docs | Swagger / OpenAPI |
| Logging | SLF4J + Logback |
| Frontend (optional) | Thymeleaf or ReactJS minimal UI |

> **Ghi chú:** đã chốt dùng Spring Boot 4.1.1 (không phải 3.x như bảng trên), Java 17, MySQL 8, Apache POI 5.5.1.

---

## 11. Hướng dẫn chạy

**Yêu cầu:** JDK 17, MySQL 8 đang chạy trên máy (cổng 3306).

**1. Cấu hình kết nối MySQL (làm 1 lần):**

```bash
cp config/application-local.properties.example config/application-local.properties
# mở config/application-local.properties, điền spring.datasource.username / password
```

- App chạy với profile mặc định `local` (`spring.profiles.default=local`), nên Spring Boot tự đọc `./config/application-local.properties` khi chạy từ thư mục gốc dự án; giá trị trong file này ghi đè `src/main/resources/application.properties`.
- Test chạy với profile `test` (`src/test/resources/application-test.properties`, H2), không đọc file local này.
- File này đã được `.gitignore`, mật khẩu không bị commit.
- Database `social_analytics` được tạo tự động (`createDatabaseIfNotExist=true`), bảng được tạo bởi `ddl-auto=update`.
- Có thể dùng biến môi trường `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` thay cho file trên.
- Nếu muốn dùng MySQL bằng Docker (`compose.yaml`) thì phải tắt MySQL trên máy hoặc đổi cổng, vì cả hai cùng dùng cổng 3306.

**2. Build, test, chạy:**

```bash
./mvnw test                   # chạy test (dùng H2, không cần MySQL)
./mvnw clean package          # build jar: target/social-analytics-0.0.1-SNAPSHOT.jar
./mvnw spring-boot:run        # chạy ứng dụng tại http://localhost:8080
# hoặc: java -jar target/social-analytics-0.0.1-SNAPSHOT.jar  (chạy từ thư mục gốc dự án)
```

- Chạy thành công khi log có dòng `Started SocialAnalyticsApplication`
- Swagger UI: http://localhost:8080/swagger-ui.html

Ví dụ gọi API:

```bash
curl -X POST localhost:8080/posts -H 'Content-Type: application/json' \
  -d '{"platform":"FACEBOOK","externalId":"fb-1","url":"https://facebook.com/p/1","title":"Hello"}'

curl -X POST localhost:8080/metrics -H 'Content-Type: application/json' \
  -d '{"postId":1,"likes":10,"shares":2,"comments":3,"followers":100}'

curl 'localhost:8080/metrics?postId=1'
```

---

## 12. Cấu trúc mã nguồn

```
social_analytics/
├── compose.yaml                     # MySQL bằng Docker (tùy chọn)
├── config/application-local.properties.example  # mẫu cấu hình MySQL local
├── pom.xml
├── docs/
│   ├── social-analytics-dashboard.md
│   └── samples/posts-sample.xlsx    # file Excel mẫu để import
└── src/
    ├── main/java/com/example/socialanalytics/
    │   ├── config/        # OpenApiConfig, WebConfig, SecurityConfig
    │   ├── controller/    # PostController, MetricController, ExcelController, JobController, LoginController, DashboardController
    │   ├── dto/           # PostRequest/Response, MetricRequest/Response, PostExcelRow, PostReportRow, ImportResult
    │   ├── entity/        # BaseEntity, User, Post, SocialMetric, SocialPlatform, OAuthToken, JobRun, ImportLog, PlatformStats
    │   ├── job/           # AsyncConfig, UpdateSocialMetricsJob, MetricsUpdateWorker, SocialApiClient, MockSocialApiClient
    │   ├── messaging/     # JmsConfig, Queues, ImportCompletedEvent, ImportEventPublisher, ImportStatsListener
    │   ├── excel/         # ExcelMapper (Reflection), @ExcelColumn, ExcelProcessingException
    │   ├── exception/     # GlobalExceptionHandler, ResourceNotFound, DuplicateResource, InvalidSort
    │   ├── repository/    # UserRepository, PostRepository, SocialMetricRepository, OAuthTokenRepository
    │   ├── security/      # SocialOAuth2UserService, JpaOAuth2AuthorizedClientService, CsrfCookieFilter, LoggingAuthenticationFailureHandler
    │   ├── service/       # PostService, MetricService, PostExcelService
    │   └── util/          # DtoMapper, SortValidator
    ├── main/resources/    # application.properties, logback-spring.xml, templates/ (login, dashboard), static/css/
    └── test/
        ├── java/.../service/      # PostServiceTest, MetricServiceTest (Mockito)
        ├── java/.../controller/   # *ControllerTest (@WebMvcTest), PostMetricApiTests, ExcelApiTests (@SpringBootTest)
        ├── java/.../repository/   # RepositoryTest (@DataJpaTest)
        ├── java/.../excel/        # ExcelMapperTest, ExcelTestFiles
        ├── java/.../security/     # SecurityConfigTests, CsrfCookieTests, SocialOAuth2UserServiceTest, JpaOAuth2AuthorizedClientServiceTest
        └── resources/application-test.properties   # H2, profile "test"
```
