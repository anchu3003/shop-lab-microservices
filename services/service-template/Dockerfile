# ==============================================================================
# Dockerfile — build & run service template (SL-0.4)
#
# Multi-stage:
#   stage 1 (build)  : Maven + JDK 21, build jar từ toàn bộ reactor
#   stage 2 (runtime): chỉ JRE 21 trên Alpine, chạy bằng user KHÔNG phải root
#
# Build context PHẢI là thư mục gốc repo (không phải services/service-template) vì
# service phụ thuộc module packages/service-commons + parent pom.xml.
#
#   docker build -f services/service-template/Dockerfile \
#     --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) -t shoplab-service-template .
#
# Vì sao alpine: image nhỏ; có sẵn `wget` (busybox) để dùng cho healthcheck.
# ==============================================================================

# ------------------------------- stage 1: build -------------------------------
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

# Copy pom trước để tận dụng Docker layer cache: đổi source không phải tải lại dependency.
#
# ⚠️ PHẢI copy pom của MỌI module trong reactor (parent pom liệt kê `<modules>`): thiếu một cái là
#    Maven chết với "Child module ... does not exist". Thêm service mới ⇒ thêm 1 dòng ở đây.
COPY pom.xml ./
COPY packages/service-commons/pom.xml packages/service-commons/
COPY services/service-template/pom.xml services/service-template/
COPY services/identity-service/pom.xml services/identity-service/
COPY services/catalog-service/pom.xml services/catalog-service/
COPY services/api-gateway/pom.xml services/api-gateway/
RUN mvn -B -q -pl services/service-template -am dependency:go-offline

COPY packages/ packages/
COPY services/ services/
COPY config/ config/

# `verify` để chạy luôn spotless/checkstyle/test — image build cũng là một chốt chất lượng.
#
# `SKIP_TESTS` (SL-1.12): CI truyền `true` vì job `test` đã chạy test cho **cả reactor** — chạy lại
# test trong image chỉ tốn thời gian (đo ở SL-1.0: job `build-image` chiếm 46% phút CI). Mặc định
# `false` để `docker build` tay vẫn test như trước.
ARG SKIP_TESTS=false
RUN mvn -B -pl services/service-template -am verify -DskipTests=${SKIP_TESTS}

# ------------------------------ stage 2: runtime ------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Git sha truyền từ ngoài vào, hiển thị ở GET /version. Không truyền thì "unknown".
ARG GIT_COMMIT=unknown
ENV SHOPLAB_GIT_COMMIT=${GIT_COMMIT}

# Chạy non-root: image mặc định là root, không được dùng root cho process nhận traffic.
RUN addgroup -S shoplab && adduser -S -G shoplab -h /app shoplab

WORKDIR /app
COPY --from=build /workspace/services/service-template/target/shoplab-service-template-*.jar app.jar
RUN chown -R shoplab:shoplab /app

USER shoplab
EXPOSE 8090

# exec form → java là PID 1, nhận trực tiếp SIGTERM (điều kiện để graceful shutdown hoạt động).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
