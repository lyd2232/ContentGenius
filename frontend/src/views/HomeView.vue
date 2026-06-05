<template>
  <div class="home-page">
    <section class="sketch-box page-intro">
      <h2>工作台</h2>
      <p class="text-muted">
        管理创作项目、调用 Agent 生成稿件，或在素材库上传参考图解析写作风格。
      </p>
      <div class="cards">
        <RouterLink to="/projects" class="card">
          <span class="card-title">我的项目</span>
          <span class="card-desc">创建、版本历史与定稿</span>
        </RouterLink>
        <RouterLink to="/create" class="card card--primary">
          <span class="card-title">AI 创作</span>
          <span class="card-desc">流式生成与多平台适配</span>
        </RouterLink>
        <RouterLink to="/materials" class="card">
          <span class="card-title">素材库</span>
          <span class="card-desc">上传图片 · Vision 风格解析</span>
        </RouterLink>
      </div>
    </section>

    <section class="sketch-box tips-box">
      <h3>创作须知</h3>
      <ul class="tips-list">
        <li>
          <strong>思考模式</strong>：同步四步（大纲 → 正文 → 润色 → 标题），适合需要多轮改标题、改写、润色的稿件；预览区底部分栏改稿，整篇重来请点左侧「开始生成」。
        </li>
        <li>
          <strong>创作主题与要求</strong>：主题保持稳定（RAG / 记忆用）；要求可随时改，首版与重新生成以「创作要求」为准；细改在预览区底部。
        </li>
        <li>
          <strong>快速模式</strong>：支持流式输出，可开 RAG / 联网；生成后可在预览区继续输入要求改稿。
        </li>
        <li>
          <strong>智能路由</strong>：演示 SmartRouter（LLM 意图识别），改稿用自然语言描述；<em>当前准确性待优化</em>，日常改稿建议用思考模式四栏。
        </li>
        <li>
          <strong>定稿与 RAG</strong>：Agent 生成默认为草稿；在「AI 创作」预览或「版本历史」中点击「定稿」后，正文会写入向量库，之后开启 RAG 的创作会检索你的历史定稿风格与表述。
        </li>
        <li>
          <strong>多轮记忆</strong>：各模式均可点「新会话」清空预览与 memoryId；服务端 Redis 记忆约保留 7 天。快速模式「开始生成」会强制新写一篇。
        </li>
        <li>
          <strong>额度</strong>：免费 / 标准 / VIP 每日调用次数不同，以顶栏提示为准。
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.home-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  min-height: 0;
}

.page-intro h2,
.tips-box h3 {
  margin-top: 0;
  color: var(--cg-green-900);
}

.tips-box h3 {
  font-size: 1rem;
  margin-bottom: 0.75rem;
}

.cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
  margin-top: 1.25rem;
}

.card {
  text-decoration: none;
  color: inherit;
  padding: 1.25rem 1.35rem;
  border: 1px solid var(--cg-border);
  border-radius: var(--cg-radius);
  background: var(--cg-surface);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.card:hover {
  border-color: var(--cg-green-700);
  box-shadow: var(--cg-shadow-md);
}

.card--primary {
  background: var(--cg-green-50);
  border-color: rgba(45, 106, 79, 0.25);
}

.card--primary:hover {
  border-color: var(--cg-green-800);
}

.card-title {
  font-weight: 600;
  font-size: 1rem;
  color: var(--cg-green-900);
}

.card-desc {
  font-size: 0.8125rem;
  color: var(--cg-text-muted);
}

.tips-list {
  margin: 0;
  padding-left: 1.15rem;
  font-size: 0.875rem;
  line-height: 1.65;
  color: var(--cg-gray-700);
}

.tips-list li {
  margin-bottom: 0.65rem;
}

.tips-list li:last-child {
  margin-bottom: 0;
}

.tips-list strong {
  color: var(--cg-green-900);
  font-weight: 600;
}
</style>
