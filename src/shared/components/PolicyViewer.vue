<template>
  <div class="policy-viewer">
    <!-- Header with back button -->
    <div class="policy-header">
      <button class="back-button" @click="goBack">← Back</button>
      <h1>{{ pageTitle }}</h1>
    </div>

    <!-- Markdown content -->
    <div class="policy-content" v-html="renderedMarkdown"></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, defineProps } from 'vue'
import { useRouter } from 'vue-router'
import { marked } from 'marked'

const props = defineProps({
  type: {
    type: String,
    required: true
  }
})

const router = useRouter()

const markdownContent = ref('')
const policyType = computed(() => props.type)

const pageTitle = computed(() => {
  switch (policyType.value) {
    case 'privacy':
      return 'Privacy Policy'
    case 'cookie':
      return 'Cookie Policy'
    case 'terms':
      return 'Terms of Service'
    default:
      return 'Policy'
  }
})

const renderedMarkdown = computed(() => {
  if (!markdownContent.value) {
    return '<p>Loading...</p>'
  }
  return marked(markdownContent.value)
})

onMounted(async () => {
  try {
    let filePath = ''
    
    switch (policyType.value) {
      case 'privacy':
        filePath = '/PRIVACY_POLICY.md'
        break
      case 'cookie':
        filePath = '/COOKIE_POLICY.md'
        break
      case 'terms':
        filePath = '/TERMS_OF_SERVICE.md'
        break
      default:
        console.warn('Unknown policy type:', policyType.value)
        markdownContent.value = '# Policy Not Found'
        return
    }

    const response = await fetch(filePath)
    if (!response.ok) {
      throw new Error(`Failed to load ${filePath}`)
    }
    markdownContent.value = await response.text()
  } catch (error) {
    console.error('Error loading policy:', error)
    markdownContent.value = `# Error Loading Policy\n\nSorry, we couldn't load the ${pageTitle.value.toLowerCase()}. Please try again later.`
  }
})

function goBack() {
  router.back()
}
</script>

<style scoped>
.policy-viewer {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 2rem;
}

.policy-header {
  max-width: 900px;
  margin: 0 auto 2rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.back-button {
  padding: 0.5rem 1rem;
  background-color: #dd7724;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
  transition: background-color 0.2s ease;
  white-space: nowrap;
}

.back-button:hover {
  background-color: #c45f1a;
}

.policy-header h1 {
  margin: 0;
  font-size: 1.75rem;
  color: #333;
  flex: 1;
}

.policy-content {
  max-width: 900px;
  margin: 0 auto;
  background-color: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  line-height: 1.6;
  color: #333;
}

/* Markdown styling */
.policy-content :deep(h1),
.policy-content :deep(h2),
.policy-content :deep(h3),
.policy-content :deep(h4),
.policy-content :deep(h5),
.policy-content :deep(h6) {
  color: #2c3e50;
  margin-top: 1.5rem;
  margin-bottom: 0.75rem;
  font-weight: 600;
}

.policy-content :deep(h1) {
  font-size: 2rem;
  border-bottom: 2px solid #dd7724;
  padding-bottom: 0.5rem;
}

.policy-content :deep(h2) {
  font-size: 1.5rem;
  color: #dd7724;
}

.policy-content :deep(h3) {
  font-size: 1.25rem;
}

.policy-content :deep(p) {
  margin-bottom: 1rem;
}

.policy-content :deep(ul),
.policy-content :deep(ol) {
  margin-bottom: 1rem;
  padding-left: 2rem;
}

.policy-content :deep(li) {
  margin-bottom: 0.5rem;
}

.policy-content :deep(code) {
  background-color: #f0f0f0;
  padding: 0.2rem 0.4rem;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
}

.policy-content :deep(pre) {
  background-color: #f0f0f0;
  padding: 1rem;
  border-radius: 4px;
  overflow-x: auto;
  margin-bottom: 1rem;
}

.policy-content :deep(blockquote) {
  border-left: 4px solid #dd7724;
  padding-left: 1rem;
  margin-left: 0;
  margin-bottom: 1rem;
  color: #666;
}

.policy-content :deep(a) {
  color: #dd7724;
  text-decoration: none;
}

.policy-content :deep(a:hover) {
  text-decoration: underline;
}

.policy-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 1rem;
}

.policy-content :deep(table th),
.policy-content :deep(table td) {
  border: 1px solid #ddd;
  padding: 0.75rem;
  text-align: left;
}

.policy-content :deep(table th) {
  background-color: #f5f5f5;
  font-weight: 600;
}

/* Responsive design */
@media (max-width: 768px) {
  .policy-viewer {
    padding: 1rem;
  }

  .policy-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .policy-header h1 {
    font-size: 1.5rem;
  }

  .policy-content {
    padding: 1.5rem;
  }
}
</style>
