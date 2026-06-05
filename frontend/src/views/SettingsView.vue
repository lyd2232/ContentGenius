<template>
  <div class="sketch-box settings">
    <h2>设置</h2>
    <section class="block">
      <h3>关于</h3>
      <p>ContentGenius v1.0.0 — AI 内容创作工作台</p>
      <p class="text-muted">
        API 经 Gateway <code>http://localhost:8080</code>，开发环境经 Vite 代理
        <code>/api</code>。
      </p>
    </section>
    <section class="block">
      <h3>账号</h3>
      <p class="text-muted">修改资料、安全设置请使用 Postman 或后续接入 user API。</p>
    </section>
  </div>
</template>

<style scoped>
h2 {
  margin-top: 0;
  color: var(--cg-green-900);
}

h3 {
  margin: 0 0 0.5rem;
  font-size: 0.9375rem;
  color: var(--cg-gray-700);
}

.block {
  margin-bottom: 1.5rem;
}

.block p {
  margin: 0.25rem 0;
  font-size: 0.9375rem;
}

code {
  font-size: 0.8125rem;
  background: var(--cg-gray-100);
  padding: 0.15em 0.4em;
  border-radius: 4px;
  color: var(--cg-green-900);
}
</style>
