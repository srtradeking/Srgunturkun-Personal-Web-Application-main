<template>

  <InfoMainComponent />
  <InfoSocialComponent />
  <GoogleAdsComponent />
  <InfoContactComponent />
  
</template>

<script>
import InfoMainComponent from '@/shared/components/InfoMainComponent.vue'
import InfoSocialComponent from '@/shared/components/InfoSocialComponent.vue'
import GoogleAdsComponent from '@/shared/components/GoogleAdsComponent.vue'
import InfoContactComponent from '@/shared/components/InfoContactComponent.vue';

export default {
  name: 'AppInfo',
  components: {
    InfoMainComponent,
    InfoSocialComponent,
    GoogleAdsComponent,
    InfoContactComponent
  },
}
</script>

<style scoped>

</style>
