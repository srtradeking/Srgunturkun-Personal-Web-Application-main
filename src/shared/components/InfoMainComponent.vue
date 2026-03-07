<template>
  <div class="info-component-root">
    
      <!-- Main Content Section -->
      <div class="main-content">
        <div class="intro-section">
          <img alt="Srgunturkun logo" :src="logo" class="logo">
          <h2 class="headline">As versatile, as professional, as exists...</h2>
          <div class="intro-text">
            <p>Sezai Recep Güntürkün a.k.a "Tradeking", is a computer
            engineer who always prefers to combine his work, hobbies and skills.
            In present, it is undoubtful that every professionality
            is intricated. Even though, some may see that as a "headache";
            I strongly disagree with them by saying "This is an opportunity for expanding our vision."</p>
            <h4>As a result, I will prove given thesis through my work.
            I hope, I may reach to the pleasure of earning your upholding support during my journey &#128151;</h4>
          </div>
        </div>
      </div>
    </div>        

</template>

<script>
import logo from '@/shared/assets/Logo.png'

export default {
  name: 'InfoMainComponent',
  data() {
    return {
      logo
    }
  }
}
</script>

<style scoped>
/* Root wrapper for transition compatibility */
.info-component-root {
  width: 100%;
}

.main-content {
  position: relative;
  z-index: 1;
}

.intro-section {
  text-align: center;
  margin: 4rem 0;
  padding: 0 1rem;
}

.headline {
  font-size: clamp(1.8rem, 4vw, 2.5rem);
  margin-bottom: 2rem;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
  font-weight: 600;
}

.intro-text {
  max-width: 800px;
  margin: 0 auto;
  line-height: 1.8;
}

.intro-text p {
  margin: 3rem 0;
  font-size: 1.15rem;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.intro-text h4 {
  margin: 2rem 0;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.logo {
  width: 50%;
  max-width: 330px;
  min-width: 200px;
  margin-bottom: 2rem;
  height: auto;
  background-color: rgba(143, 134, 134, 0.705);
  background-size: cover;
  border-radius: 16px;
}

</style>