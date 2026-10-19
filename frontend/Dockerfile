# 前端镜像：两阶段构建（Node 构建静态资源 → nginx 托管）
# 构建：docker build -t <你的命名空间>/onlineshop-frontend:1.0.0 ./frontend

# ---- 阶段 1：构建，用 Node 生成 dist 静态资源 ----
FROM node:20-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm install
COPY . .
RUN npm run build

# ---- 阶段 2：运行，用 nginx 托管 dist 并反向代理 /api ----
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html
EXPOSE 80
