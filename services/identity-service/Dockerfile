# ==============================================================================
# Dockerfile — build & run identity-service (SL-1.1)
#
# Multi-stage:
#   stage 1 (build)  : Maven + JDK 21, build jar từ toàn bộ reactor
#   stage 2 (runtime): chỉ JRE 21 trên Alpine, chạy bằng user KHÔNG phải root
#
# Build context PHẢI là thư mục gốc repo (không phải services/identity-service) vì
# service phụ thuộc module packages/service-commons + parent pom.xml.
#
#   docker build -f services/identity-service/Dockerfile \
#     --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) -t shoplab-identity-service .
#
# ⚠️ `-DskipITs`: integration test dùng Testcontainers nên cần Docker socket, mà
#    trong `docker build` KHÔNG có socket → chạy IT ở đây là fail chắc chắn.
#    IT được chạy ở CI (job `test`) và ở máy dev (`mvn verify`).
#
# Vì sao alpine: image nhỏ; có sẵn `wget` (busybox) để dùng cho healthcheck.
# ==============================================================================

# ------------------------------- stage 1: build -------------------------------
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

# Copy pom trước để tận dụng Docker layer cache: đổi source không phải tải lại dependency.
#
# ⚠️ PHẢI copy pom của MỌI module trong reactor (parent pom liệt kê `<modules>`): thiếu một cái là
#    Maven chết với "Child module ... does not exist". Thêm service mới ⇒ thêm 1 dòng ở đây
#    (và ở Dockerfile của các service khác).
COPY pom.xml ./
COPY packages/service-commons/pom.xml packages/service-commons/
COPY services/service-template/pom.xml services/service-template/
COPY services/identity-service/pom.xml services/identity-service/
COPY services/catalog-service/pom.xml services/catalog-service/
COPY services/inventory-service/pom.xml services/inventory-service/
COPY services/promotion-service/pom.xml services/promotion-service/
COPY services/order-service/pom.xml services/order-service/
COPY services/api-gateway/pom.xml services/api-gateway/
COPY services/payment-service/pom.xml services/payment-service/
COPY services/cart-service/pom.xml services/cart-service/
COPY services/notification-service/pom.xml services/notification-service/
RUN mvn -B -q -pl services/identity-service -am dependency:go-offline

COPY packages/ packages/
COPY services/ services/
COPY config/ config/

# `verify` để chạy luôn spotless/checkstyle/test — image build cũng là một chốt chất lượng.
# `-DskipITs` vì Testcontainers cần Docker socket (không có trong `docker build`).
#
# `SKIP_TESTS` (SL-1.12): CI truyền `true` vì job `test` đã chạy test cho **cả reactor** — chạy lại
# test trong image chỉ tốn thời gian. Mặc định `false` để `docker build` tay vẫn test như trước.
ARG SKIP_TESTS=false
# ⚠️ `-Djacoco.skip=true` (SL-1.5): trong `docker build` không có Docker socket ⇒ `-DskipITs` bỏ
#    integration test ⇒ coverage thiếu phần chỉ IT chạm tới (`SecurityConfig`) ⇒ gate 80% ở
#    `config/*` fail và image build đỏ dù code đúng. Gate coverage thuộc job `test` của CI.
RUN mvn -B -pl services/identity-service -am verify -DskipTests=${SKIP_TESTS} -DskipITs -Djacoco.skip=true

# ------------------------------ stage 2: runtime ------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Git sha truyền từ ngoài vào, hiển thị ở GET /version. Không truyền thì "unknown".
ARG GIT_COMMIT=unknown
ENV SHOPLAB_GIT_COMMIT=${GIT_COMMIT}

# Chạy non-root: image mặc định là root, không được dùng root cho process nhận traffic.
RUN addgroup -S shoplab && adduser -S -G shoplab -h /app shoplab

WORKDIR /app
COPY --from=build /workspace/services/identity-service/target/shoplab-identity-service-*.jar app.jar
RUN chown -R shoplab:shoplab /app

USER shoplab
EXPOSE 8081

# exec form → java là PID 1, nhận trực tiếp SIGTERM (điều kiện để graceful shutdown hoạt động).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
