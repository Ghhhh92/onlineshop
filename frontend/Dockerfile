# 前端镜像：两阶段构建（Node 构建静态资源 → nginx 托管）
# 构建：docker build -t <你的命名空间>/onlineshop-frontend:1.0.0 ./frontend

# ---- 阶段 1：构建，用 Node 生成 dist 静态资源 ----
# 注：node 22 满足 @vueuse/core 等依赖的 engines 要求（>=22）
FROM node:22-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
# 国内网络访问 registry.npmjs.org 常出现 ECONNRESET，改用国内镜像源加速
RUN npm install --registry=https://registry.npmmirror.com
COPY . .
RUN npm run build

# ---- 阶段 2：运行，用 nginx 托管 dist 并反向代理 /api ----
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html
EXPOSE 80
