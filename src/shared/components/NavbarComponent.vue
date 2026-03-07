<template>
  <nav class="navbar navbar-expand-lg">
    <div class="container-fluid">
      <div class="brand" @click="navigateAndClose('/')">
        <img :src="InfoIcon" alt="logo" class="logo" />
        <router-link to="/" class="brand-text">Info</router-link>
      </div>

      <div class="route-display">
        <h2>
          Current location &#9193;
          <span class="location-span">
            {{ locationText.split('/')[1] }}
          </span>
        </h2>
      </div>
      
      <!-- Dark Mode Toggle -->
      <button class="theme-toggle" @click="toggleDarkMode" aria-label="Toggle Dark Mode" :title="isDarkMode ? 'Switch to Light Mode' : 'Switch to Dark Mode'">
        <span class="theme-icon">{{ isDarkMode ? '☀️' : '🌙' }}</span>
      </button>

      <!-- Dropdown Container -->
      <div class="dropdown-container">
        <!-- Dropdown Toggle Button -->
        <button 
          class="dropdown-toggle" 
          @click="toggleDropdown"
          aria-label="Navigation Menu"
        >
          <span class="hamburger-icon">
            <span class="line" :class="{ 'open': open }"></span>
            <span class="line" :class="{ 'open': open }"></span>
            <span class="line" :class="{ 'open': open }"></span>
          </span>
          <span class="dropdown-text">Menu</span>
        </button>

        <!-- Dropdown Menu -->
        <ul :class="['dropdown-menu', { 'show': open }]">
          <li class="dropdown-item btn btn-outline-warning" @click="navigateAndClose('/social')">
            <img :src="socialIcon" alt="logo" class="logo" />
            <span>Social</span>
          </li>
          <li class="dropdown-item btn btn-outline-info" @click="navigateAndClose('/profiles')">
            <img :src="defaultAvatar" alt="logo" class="logo" />
            <span>User Profiles</span>
          </li>
          <li class="dropdown-item btn btn-outline-primary" @click="navigateAndClose('/account')">
            <img :src="keyIcon" alt="logo" class="logo" />
            <span>Account Management</span>
          </li>
          <li class="dropdown-item btn btn-outline-success" @click="navigateAndClose('/reports')">
            <img :src="reportsIcon" alt="logo" class="logo" />
            <span>Reports</span>
          </li>
          <li v-if="isAdminOrModerator" class="dropdown-item btn btn-outline-secondary" @click="navigateAndClose('/moderation')">
            <span class="moderation-icon">📋</span>
            <span>Moderation</span>
          </li>
        </ul>
      </div>
    </div>
  </nav>
</template>

<script>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import InfoIcon from '@/shared/assets/InfoIcon.png'
import socialIcon from '@/shared/assets/SocialIcon.png'
import defaultAvatar from '@/shared/assets/DefaultAvatar.png'
import keyIcon from '@/shared/assets/KeyIcon.png'
import reportsIcon from '@/shared/assets/FlagIcon.png'

export default {
  name: 'NavbarComponent',
  setup() {
    const route = useRoute()
    const router = useRouter()
    const isLoggedIn = ref(!!localStorage.getItem('jwtToken'))
    const isDarkMode = ref(false)
    const open = ref(false)
    
    const isAdminOrModerator = computed(() => {
      const role = localStorage.getItem('role') || localStorage.getItem('userRole')
      return role === 'ADMIN' || role === 'MODERATOR'
    })
    let clickOutsideHandler

    const handleClickOutside = (event) => {
      // Check if click is outside the dropdown container
      const dropdownContainer = document.querySelector('.dropdown-container')
      if (dropdownContainer && !dropdownContainer.contains(event.target)) {
        open.value = false
      }
    }

    const updateTheme = () => {
      if (isDarkMode.value) {
        document.body.classList.add('dark-mode')
        localStorage.setItem('theme', 'dark')
      } else {
        document.body.classList.remove('dark-mode')
        localStorage.setItem('theme', 'light')
      }
    }

    const toggleDarkMode = () => {
      isDarkMode.value = !isDarkMode.value
      updateTheme()
    }

    onMounted(() => {
      // Add click outside listener for dropdown
      clickOutsideHandler = handleClickOutside
      document.addEventListener('click', clickOutsideHandler)

      // Initialize theme from localStorage
      const savedTheme = localStorage.getItem('theme')
      if (savedTheme === 'dark') {
        isDarkMode.value = true
        document.body.classList.add('dark-mode')
      }
    })

    onUnmounted(() => {
      // Remove click outside listener
      if (clickOutsideHandler) {
        document.removeEventListener('click', clickOutsideHandler)
      }
    })

    const locationText = computed(() => {
      const path = route.path || '/'
      if (path === '/') {
        return '/Info'
      }
      return `${path}`
    })
    
    const loggedIn = computed(() => !!localStorage.getItem('jwtToken'))
    
    const navigateTo = (path) => {
      router.push(path)
    }

    const close = () => {
      open.value = false
    }

    const toggleDropdown = () => {
      open.value = !open.value
    }

    const navigateAndClose = (path) => {
      navigateTo(path)
      close()
    }
    
    return {
      locationText,
      isLoggedIn: loggedIn,
      isAdminOrModerator,
      navigateTo,
      open,
      close,
      toggleDropdown,
      navigateAndClose,
      isDarkMode,
      toggleDarkMode,
      InfoIcon,
      socialIcon,
      defaultAvatar,
      keyIcon,
      reportsIcon
    }
  }
}
</script>

<style scoped>
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: #1f2937;
  opacity: 1;
  z-index: 1000;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  backdrop-filter: none;
}

.container-fluid {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.route-display {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  text-align: center;
}

.location-span {
  color: #aeb3ff;
  font-weight: bold;
  font-style: italic;
  font-family: 'Gill Sans', 'Gill Sans MT', Calibri, 'Trebuchet MS', sans-serif;
  text-align: center;
}

.route-display h2 {
  color: #e0e0e0;  /* Off-white text color */
  margin: 0;      /* Remove default margins */
  font-size: 2.4rem; /* Adjust size as needed */
}

.container {
  margin:0px;
  padding: 0;
}

.brand {
  display: flex;
  align-items: center;
  margin-right: auto;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: transparent;
  border: 1px solid rgba(224, 224, 224, 0.3);
  color: #e0e0e0;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.brand:hover {
  background: rgba(224, 224, 224, 0.1);
  border-color: rgba(224, 224, 224, 0.5);
}
.logo {
  width: 36px;
  height: 36px;
  object-fit: contain;
  margin-right: 0.5rem;
}
.brand-text {
  color: #e0e0e0;
  font-weight: 600;
  text-decoration: none;
  padding-left: 10px;
  padding-right: 10px;
  display: flex;
  align-items: center;
}

/* Dropdown Styles */
.theme-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem;
  margin-right: 1rem;
  background: transparent;
  border: 1px solid rgba(224, 224, 224, 0.3);
  color: #e0e0e0;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  width: 40px;
  height: 40px;
}

.theme-toggle:hover {
  background: rgba(224, 224, 224, 0.1);
  border-color: rgba(224, 224, 224, 0.5);
  transform: scale(1.05);
}

.theme-icon {
  font-size: 1.2rem;
  line-height: 1;
}

.dropdown-container {
  position: relative;
  display: inline-block;
}

.dropdown-toggle {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: transparent;
  border: 1px solid rgba(224, 224, 224, 0.3);
  color: #e0e0e0;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dropdown-toggle:hover {
  background: rgba(224, 224, 224, 0.1);
  border-color: rgba(224, 224, 224, 0.5);
}

.hamburger-icon {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.hamburger-icon .line {
  width: 18px;
  height: 2px;
  background: #e0e0e0;
  transition: all 0.3s ease;
  transform-origin: center;
}

.hamburger-icon .line.open:nth-child(1) { 
  transform: translateY(5px) rotate(45deg); 
}
.hamburger-icon .line.open:nth-child(2) { 
  opacity: 0; 
}
.hamburger-icon .line.open:nth-child(3) { 
  transform: translateY(-5px) rotate(-45deg); 
}

.dropdown-text {
  font-weight: 500;
  font-size: 0.9rem;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background: #1f2937;
  border: 1px solid #374151;
  border-radius: 8px;
  min-width: 220px;
  max-width: 280px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  list-style: none;
  margin: 0;
  padding: 0.75rem 0;
  z-index: 1000;
  opacity: 0;
  visibility: hidden;
  transform: translateY(-10px);
  transition: all 0.2s ease;
  overflow: hidden;
}

.dropdown-menu.show {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

.dropdown-item {
  display: flex;
  align-items: center;
  width: calc(100% - 1rem);
  padding: 0.75rem 1rem;
  margin: 0.125rem 0.5rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
  box-sizing: border-box;
  white-space: nowrap;
}

.dropdown-item:hover {
  background: rgba(224, 224, 224, 0.1);
  transform: translateX(2px);
}

.dropdown-item .logo {
  width: 18px;
  height: 18px;
  margin-right: 0.5rem;
  flex-shrink: 0;
}

.dropdown-item span {
  color: #e0e0e0;
  font-weight: 500;
  font-size: 0.875rem;
  flex-grow: 1;
  text-align: left;
}
/* Additional dropdown button styling */
.dropdown-item.btn-outline-warning {
  border-color: rgba(217, 70, 239, 0.3);
}

.dropdown-item.btn-outline-warning:hover {
  border-color: #d946ef;
  background: rgba(217, 70, 239, 0.15);
}

.dropdown-item.btn-outline-primary {
  border-color: rgba(59, 130, 246, 0.3);
}

.dropdown-item.btn-outline-primary:hover {
  border-color: #3b82f6;
  background: rgba(59, 130, 246, 0.15);
}

.dropdown-item.btn-outline-success {
  border-color: rgba(16, 185, 129, 0.3);
}

.dropdown-item.btn-outline-success:hover {
  border-color: #10b981;
  background: rgba(16, 185, 129, 0.15);
}

.dropdown-item.btn-outline-info {
  border-color: rgba(6, 182, 212, 0.3);
}

.dropdown-item.btn-outline-info:hover {
  border-color: #06b6d4;
  background: rgba(6, 182, 212, 0.15);
}

.dropdown-item.btn.btn-outline-secondary {
  border-color: rgba(23, 212, 6, 0.3);
}

.dropdown-item.btn.btn-outline-secondary:hover {
  border-color: #10d309;
  background: rgba(6, 182, 212, 0.15);
}

/* Responsive */
@media (max-width: 768px) {
  .route-display {
    display: none; /* Hide route display on mobile for space */
  }
  
  .dropdown-menu {
    right: 0;
    min-width: 180px;
  }
  
  .container-fluid {
    padding: 0 1rem;
  }
}

@media (max-width: 480px) {
  .dropdown-toggle .dropdown-text {
    display: none; /* Hide "Menu" text on very small screens */
  }

  .theme-toggle {
    margin-right: 0.5rem;
    padding: 0.25rem;
    width: 36px;
    height: 36px;
  }
  
  .dropdown-menu {
    min-width: 160px;
  }
}

/* Click outside to close dropdown */
.dropdown-container.open {
  z-index: 1001;
}

/* Moderation icon styling */
.moderation-icon {
  font-size: 1.5rem;
  margin-right: 0.5rem;
}

</style>
