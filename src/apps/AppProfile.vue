<template>
  <div class="profile-app-container">
    <div class="profile-section">
      <ProfileComponent />
      <!-- Google Ads - Between Profile and User Search -->
      <div class="user-search">
        <UserSearchComponent />
      </div>
    </div>
    <GoogleAdsComponent />
  </div>
</template>

<script>
import ProfileComponent from '@/shared/components/ProfileComponent.vue'
import UserSearchComponent from '@/shared/components/UserSearchComponent.vue'
import GoogleAdsComponent from '@/shared/components/GoogleAdsComponent.vue'

export default {
  name: 'AppProfile',
  components: {
    ProfileComponent,
    UserSearchComponent,
    GoogleAdsComponent
  }
}
</script>

<style scoped>
.profile-app-container {
  padding: 20px;
  min-height: 100vh;
}

.profile-section {
  margin-bottom: 30px;
}

.user-search {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
}
</style>