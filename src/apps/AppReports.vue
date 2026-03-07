<template>
  <ReportModal
    :isOpen="true"
    :contentType="contentType"
    :contentId="contentId"
    :userId="userId"
    @close="handleClose"
  />
</template>

<script>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ReportModal from '@/shared/components/ReportModal.vue'

export default {
  name: 'AppReports',
  components: {
    ReportModal
  },
  setup() {
    const route = useRoute()
    const router = useRouter()

    const contentType = computed(() => {
      const type = route.query.contentType
      const allowed = ['POST', 'COMMENT', 'USER', 'MESSAGE']
      return allowed.includes(type) ? type : 'POST'
    })

    const contentId = computed(() => {
      return route.query.contentId || 'demo-content-id'
    })

    const userId = computed(() => {
      return route.query.userId || null
    })

    function handleClose() {
      router.back()
    }

    return {
      contentType,
      contentId,
      userId,
      handleClose
    }
  }
}
</script>

<style scoped>
</style>