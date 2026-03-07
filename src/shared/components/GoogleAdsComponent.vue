<template>
  <div class="google-ads-container">
    <!-- Google Ads Placeholder -->
    <div class="ad-placeholder">
      <div class="ad-header">
        <span class="ad-label">Advertisement</span>
      </div>
      <div class="ad-content"
        async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-7247132214332218"
     crossorigin="anonymous">
        <div class="ad-space">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 300 250" class="ad-icon">
            <rect width="300" height="250" fill="#f0f0f0" stroke="#ccc" stroke-width="1"/>
            <text x="150" y="115" text-anchor="middle" font-size="16" fill="#999">Google</text>
            <text x="150" y="140" text-anchor="middle" font-size="14" fill="#999">Advertising</text>
          </svg>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * Google Ads Component
 * 
 * This component provides a placeholder for Google AdSense ads.
 * To activate real Google Ads:
 * 
 * 1. Sign up for Google AdSense at https://adsense.google.com
 * 2. Replace the ad-space div with the actual AdSense code
 * 3. Add your client ID and slot ID to the data attributes
 * 4. The script will automatically load and display ads
 * 
 * Props:
 *   - None (can be extended with ad-slot and client-id props if needed)
 */

// This component can be extended with props for different ad formats
// Example: ad-slot, ad-client, ad-format (auto, horizontal, vertical, etc.)
</script>

<style scoped>
.google-ads-container {
  width: 100%;
  max-width: 100%;
  margin: 20px 0;
  padding: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  z-index: 1;
}

.ad-placeholder {
  width: auto;
  max-width: 100%;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.ad-header {
  background: #f9f9f9;
  padding: 8px 12px;
  border-bottom: 1px solid #e0e0e0;
  font-size: 11px;
}

.ad-label {
  color: #999;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.ad-content {
  padding: 10px;
  min-height: 250px;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f5f5f5;
}

.ad-space {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.ad-icon {
  width: 100%;
  max-width: 300px;
  height: auto;
  opacity: 0.5;
}

/* Responsive Design */
@media (max-width: 768px) {
  .google-ads-container {
    margin: 15px 0;
    padding: 15px 0;
  }

  .ad-content {
    min-height: 200px;
  }
}

/* Dark mode support */
@media (prefers-color-scheme: dark) {
  .ad-placeholder {
    background: #2a2a2a;
  }

  .ad-header {
    background: #1f1f1f;
    border-bottom-color: #444;
  }

  .ad-label {
    color: #888;
  }

  .ad-content {
    background: #333;
  }
}
</style>
