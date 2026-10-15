# Social Analytics Dashboard

Ứng dụng web giúp admin tổng hợp và theo dõi lượng tương tác (likes, shares, followers) từ Facebook & Twitter.

# Chạy nhanh

```bash
cp config/application-local.properties.example config/application-local.properties   # điền user/password MySQL
./mvnw test                # chạy test (H2)
./mvnw spring-boot:run     # http://localhost:8080/swagger-ui.html
```
