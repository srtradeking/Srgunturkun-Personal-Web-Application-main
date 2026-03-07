<template>
    <!-- Social Info Section -->    
        <div class="social-section">
          <div class="social-content">
              <h2>Visit Our Community</h2>
              <p>Explore and interact with our gaming world.</p>
              <router-link to="/social" class="social-button">
                Visit our community!
              </router-link>
            </div>
          </div>
        
</template>

<script>


export default {
    name: "InfoSocialComponent",
}
</script>

<style scoped>

.social-section {
  position: relative;
  z-index: 1;
  width: 100%;
  min-height: 50vh;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  margin: 2rem auto;
}

.social-section h2 {
  font-size: 2.5rem;
  margin-bottom: 1.5rem;
  text-shadow: 2px 2px 4px rgba(0,0,0,0.3);
}

.social-section p {
  font-size: 1.2rem;
  margin-bottom: 2rem;
  text-shadow: 1px 1px 2px rgba(0,0,0,0.3);
}

.social-content {
  padding: 3rem 2rem;
  text-align: center;
  color: white;
  z-index: 2;
  position: relative;
  max-width: 600px;
}

.social-content h2 {
  color: #ffffff;
  font-size: clamp(1.5rem, 4vw, 2.5rem);
  font-weight: 700;
  margin-bottom: 1.5rem;
  text-shadow: 2px 2px 8px rgba(0, 0, 0, 0.7);
  line-height: 1.2;
}

.social-content p {
  color: rgba(255, 255, 255, 0.95);
  font-size: clamp(1rem, 2vw, 1.25rem);
  line-height: 1.6;
  margin-bottom: 2.5rem;
  text-shadow: 1px 1px 4px rgba(0, 0, 0, 0.5);
}

.social-button {
  display: inline-block;
  padding: 1rem 2.5rem;
  background: linear-gradient(135deg, #dd7724, #ff9558);
  color: white;
  text-decoration: none;
  border-radius: 50px;
  font-weight: 600;
  font-size: 1.1rem;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(221, 119, 36, 0.3);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.social-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(221, 119, 36, 0.4);
  background: linear-gradient(135deg, #ff9558, #dd7724);
}

.social-button:active {
  transform: translateY(0);
}

@media (max-width: 768px) {
  .social-section {
    margin: 1rem auto;
    min-height: 40vh;
  }
  
  .social-content {
    padding: 2rem 1rem;
  }
  
  .social-button {
    padding: 0.8rem 2rem;
    font-size: 1rem;
  }
}
</style>