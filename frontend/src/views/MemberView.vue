<script setup>
import { onMounted, ref } from 'vue'
import { fetchMe } from '../api'

const user = ref(null)
onMounted(async () => {
  user.value = await fetchMe()
})
</script>

<template>
  <div class="sketch-box member">
    <h2>会员中心</h2>
    <p class="text-muted intro">支付模块未接入；等级由后台 user 表维护。</p>
    <table class="plan">
      <thead>
        <tr>
          <th></th>
          <th>免费</th>
          <th class="col-vip">VIP</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>每日对话额度</td>
          <td>1 次</td>
          <td class="col-vip">10+ 次</td>
        </tr>
        <tr>
          <td>标准会员</td>
          <td>—</td>
          <td class="col-vip">3 次 / 日</td>
        </tr>
        <tr>
          <td>当前等级</td>
          <td colspan="2" class="current">{{ user?.memberLevel ?? '-' }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
h2 {
  margin-top: 0;
  color: var(--cg-green-900);
}

.intro {
  margin: 0 0 1.25rem;
  font-size: 0.875rem;
}

.plan {
  width: 100%;
  max-width: 520px;
  border-collapse: collapse;
  font-size: 0.9375rem;
}

th,
td {
  border: 1px solid var(--cg-border);
  padding: 0.75rem 1rem;
  text-align: center;
}

th {
  background: var(--cg-gray-50);
  font-weight: 600;
  color: var(--cg-gray-700);
}

th:first-child,
td:first-child {
  text-align: left;
}

.col-vip {
  background: var(--cg-accent-soft);
}

.current {
  font-weight: 600;
  color: var(--cg-green-900);
}
</style>
