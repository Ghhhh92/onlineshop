<template>
  <div class="buyer-shell">
    <header class="buyer-header">
      <router-link class="buyer-brand" to="/">
        <span class="buyer-logo">店</span>
        <span>在线购物 · 买家端</span>
      </router-link>
      <nav class="buyer-nav" aria-label="买家导航">
        <router-link to="/">首页</router-link>
        <router-link to="/buy">我要购买</router-link>
        <router-link to="/query">口令查询</router-link>
      </nav>
    </header>

    <main class="buyer-main">
      <slot />
    </main>

    <footer class="buyer-footer">
      第 3 组 · 在线购物系统 · 软件工程实践课程项目
    </footer>
  </div>
</template>

<script setup>
</script>

<style scoped>
.buyer-shell {
  --buyer-primary: #168675;
  --buyer-primary-dark: #0f6b5e;
  --buyer-bg: #f4f7f6;
  --buyer-text: #203b38;
  --buyer-muted: #71817c;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  color: var(--buyer-text);
  background:
    radial-gradient(1200px 400px at 50% -160px, rgba(22, 134, 117, 0.16), transparent),
    var(--buyer-bg);
  font-family: system-ui, -apple-system, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  box-sizing: border-box;
}

.buyer-shell * {
  box-sizing: border-box;
}

.buyer-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 14px max(24px, calc((100vw - 1000px) / 2));
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #e1e9e6;
}

.buyer-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--buyer-text);
  font-size: 18px;
  font-weight: 700;
  text-decoration: none;
  white-space: nowrap;
}

.buyer-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  color: #fff;
  background: linear-gradient(135deg, #23a892, #0f6b5e);
  font-size: 16px;
}

.buyer-nav {
  display: flex;
  gap: 26px;
  flex-wrap: wrap;
}

.buyer-nav a {
  position: relative;
  padding: 8px 2px;
  color: #61736e;
  text-decoration: none;
  font-weight: 500;
}

.buyer-nav a:hover {
  color: var(--buyer-primary);
}

.buyer-nav a.router-link-exact-active {
  color: var(--buyer-primary);
  font-weight: 700;
}

.buyer-nav a.router-link-exact-active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 3px;
  border-radius: 3px;
  background: var(--buyer-primary);
}

.buyer-main {
  flex: 1;
  width: 100%;
  max-width: 1000px;
  margin: 0 auto;
  padding: 34px 24px 64px;
}

.buyer-footer {
  padding: 20px 24px;
  text-align: center;
  color: #8a9894;
  font-size: 13px;
  border-top: 1px solid #e1e9e6;
}

@media (max-width: 700px) {
  .buyer-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
    padding: 14px 18px;
  }

  .buyer-main {
    padding: 22px 16px 44px;
  }

  .buyer-nav {
    width: 100%;
    gap: 18px;
  }
}
</style>