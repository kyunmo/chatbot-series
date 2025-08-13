<template>
  <div class="health-check">
    <h3>Backend Health Status</h3>
    <div v-if="loading">Loading...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else-if="health" class="status">
      <p>Status: <span :class="health.status">{{ health.status }}</span></p>
      <p>Database: {{ health.database }}</p>
      <p>Time: {{ health.timestamp }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'

const health = ref(null)
const loading = ref(false)
const error = ref('')

onMounted(async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/health')
    health.value = response.data
  } catch (err) {
    error.value = 'Failed to connect to backend'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.health-check {
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.status .UP {
  color: green;
  font-weight: bold;
}

.status .DOWN {
  color: red;
  font-weight: bold;
}

.error {
  color: red;
}
</style>
