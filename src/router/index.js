import { createRouter, createWebHistory } from 'vue-router';

// Lazy load all app components with error handling
const AppInfo = () => import('@/apps/AppInfo.vue').catch(err => {
  console.error('Failed to load AppInfo:', err);
  return import('@/apps/AppInfo.vue'); // Retry once
});

const AppSocial = () => import('@/apps/AppSocial.vue').catch(err => {
  console.error('Failed to load AppSocial:', err);
  return import('@/apps/AppSocial.vue'); // Retry once
});

const AppAccManagement = () => import('@/apps/AppAccManagement.vue').catch(err => {
  console.error('Failed to load AppAccManagement:', err);
  return import('@/apps/AppAccManagement.vue'); // Retry once
});

const AppProfile = () => import('@/apps/AppProfile.vue').catch(err => {
  console.error('Failed to load AppProfile:', err);
  return import('@/apps/AppProfile.vue'); // Retry once
});

const AppReports = () => import('@/apps/AppReports.vue').catch(err => {
  console.error('Failed to load AppReports:', err);
  return import('@/apps/AppReports.vue'); // Retry once
});

const AppModeration = () => import('@/apps/AppModeration.vue').catch(err => {
  console.error('Failed to load AppModeration:', err);
  return import('@/apps/AppModeration.vue'); // Retry once
});

const PolicyViewer = () => import('@/shared/components/PolicyViewer.vue').catch(err => {
  console.error('Failed to load PolicyViewer:', err);
  return import('@/shared/components/PolicyViewer.vue'); // Retry once
});

const VerifyEmailPage = () => import('@/shared/components/VerifyEmailPage.vue').catch(err => {
  console.error('Failed to load VerifyEmailPage:', err);
  return import('@/shared/components/VerifyEmailPage.vue'); // Retry once
});

const ReportModal = () => import('@/shared/components/ReportModal.vue').catch(err => {
  console.error('Failed to load ReportModal:', err);
  return import('@/shared/components/ReportModal.vue'); // Retry once
});

const ReportHistory = () => import('@/shared/components/ReportHistory.vue').catch(err => {
  console.error('Failed to load ReportHistory:', err);
  return import('@/shared/components/ReportHistory.vue'); // Retry once
});

// Function to get the appropriate component based on subdomain
function getSubdomainComponent() {
  const hostname = window.location.hostname;
  const productionDomain = import.meta.env?.VITE_PRODUCTION_DOMAIN || process.env.VUE_APP_PRODUCTION_DOMAIN || 'yourdomain.com';
  
  // Check if we're on the production domain
  if (!hostname.includes(productionDomain)) {
    return AppInfo; // Default for localhost/development
  }
  
  const subdomain = hostname.split('.')[0];
  
  switch (subdomain) {
    case 'social':
      return AppSocial;
    case 'account':
    case 'manage':
      return AppAccManagement;
    case 'profiles':
    case 'users':
      return AppProfile;
    case 'www':
    default:
      return AppInfo;
  }
}

const routes = [
  {
    path: '/',
    name: 'Root',
    component: getSubdomainComponent()
  },
  // Fallback routes for development and direct navigation
  {
    path: '/info',
    name: 'AppInfo',
    component: AppInfo
  },
  {
    path: '/social',
    name: 'AppSocial',
    component: AppSocial
  },
  {
    path: '/account',
    name: 'AppAccManagement',
    component: AppAccManagement
  },
  {
    path: '/profile',
    name: 'AppProfile',
    component: AppProfile
  },
  {
    path: '/profiles',
    name: 'AppProfiles',
    component: AppProfile
  },
  // Dynamic user profile route
  {
    path: '/user/:userId',
    name: 'UserProfile',
    component: () => import('@/shared/components/PublicProfileComponent.vue'),
    props: true
  },
  // Dynamic post detail route
  {
    path: '/posts/:postId',
    name: 'PostDetail',
    component: () => import('@/shared/components/PostDetailComponent.vue'),
    props: true
  },
  // Policy routes
  {
    path: '/privacy-policy',
    name: 'PrivacyPolicy',
    component: PolicyViewer,
    props: { type: 'privacy' }
  },
  {
    path: '/cookie-policy',
    name: 'CookiePolicy',
    component: PolicyViewer,
    props: { type: 'cookie' }
  },
  {
    path: '/terms-of-service',
    name: 'TermsOfService',
    component: PolicyViewer,
    props: { type: 'terms' }
  },
  {
    path: '/verify-email',
    name: 'VerifyEmail',
    component: VerifyEmailPage
  },
  // Login route (redirect to account management)
  {
    path: '/login',
    redirect: '/account'
  },
  // Report routes
  {
    path: '/reports',
    name: 'AppReports',
    component: AppReports
  },
  // Moderation panel (Admin/Moderator only)
  {
    path: '/moderation',
    name: 'AppModeration',
    component: AppModeration
  },
  {
    path: '/report-history',
    name: 'ReportHistory',
    component: ReportHistory,
    props: { userId: localStorage.getItem('userId') }
  },
  // Redirect any other paths to root on subdomains
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// Optional: Add navigation guard for additional subdomain handling and lazy loading
router.beforeEach((to, from, next) => {
  // Disable subdomain redirects for development and testing
  // This allows normal navigation to work properly
  next();
});

// Add loading state management for lazy-loaded routes
router.beforeResolve((to, from, next) => {
  // You can add loading logic here if needed
  next();
});

// Handle errors in lazy-loaded components
router.onError((error) => {
  console.error('Router error:', error);
  // You could redirect to an error page or show a notification
});

export default router;