# 🎮 Srgunturkun Web Application - E-Sports Community Platform

> **Connect, Compete, Share** - Your ultimate destination for the e-sports community

![Vue.js](https://img.shields.io/badge/Vue.js-3.x-green.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)
![Java](https://img.shields.io/badge/Java-17-orange.svg)

## 🌟 About Srgunturkun Web Application

Srgunturkun Web Application is a comprehensive e-sports community platform designed to bring together gamers, content creators, and e-sports enthusiasts in a unified digital space. Built with modern web technologies, our platform provides a feature-rich environment where users can share gaming content, connect with fellow players, track their gaming profiles, and participate in community discussions.

### 🎮 Core Features

#### 📺 Content Sharing & Discovery
- **Game-specific content categorization** - Organize posts by game titles
- **Video content support** - Share gameplay highlights, tutorials, and streams
- **Advanced filtering system** - Find content by game, type, or popularity
- **Community voting** - React to and rate shared content

#### 👥 Social Features
- **User profiles** - Showcase your gaming achievements and stats
- **Comment system** - Engage in discussions with threaded replies
- **Follow system** - Connect with your favorite gamers
- **Real-time interactions** - Like, comment, and share instantly

#### 🛡️ Community Safety
- **Advanced moderation tools** - Keep the community safe and respectful
- **Reporting system** - Flag inappropriate content and behavior
- **User verification** - Email verification for account security
- **Privacy controls** - Manage your data and visibility preferences

#### ⚙️ User Management
- **Secure authentication** - JWT-based login system
- **Account settings** - Customize your experience
- **Privacy policies** - GDPR-compliant data handling
- **Cookie consent management** - Transparent privacy controls

## 🏗️ Technical Architecture

### Frontend Stack
- **Vue.js 3** with Composition API
- **Vue Router 4** for navigation
- **Pinia** for state management
- **Bootstrap 5** for responsive UI
- **Vite** for fast development and building

### Backend Stack
- **Spring Boot 3.2.0** with Java 17
- **Spring Security** for authentication
- **JWT tokens** for secure sessions
- **PostgreSQL** for primary database
- **Redis** for caching and sessions
- **Docker** for containerization

### Infrastructure
- **Subdomain-based routing** for multi-app architecture
- **Lazy loading** for optimal performance
- **Progressive Web App** capabilities
- **Responsive design** for all devices
- **Dark mode** support

## 📦 Installation & Setup

### Prerequisites
- Node.js 16+ and npm
- Java 17+
- PostgreSQL 13+
- Redis 6+
- Docker (optional)

### Quick Start

1. **Clone the repository**
```bash
git clone https://github.com/your-username/srgunturkun-web-app.git
cd srgunturkun-web-app
```

2. **Install frontend dependencies**
```bash
npm install
```

3. **Set up environment variables**
```bash
cp .env.example .env
# Edit .env with your configuration
```

4. **Start the frontend development server**
```bash
npm run dev
```

5. **Set up the backend**
```bash
cd backend-java
mvn clean install
mvn spring-boot:run
```

6. **Access the application**
- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080`
- API Documentation: `http://localhost:8080/swagger-ui.html`

### Docker Setup

```bash
# Build and run all services
docker-compose up -d

# View logs
docker-compose logs -f
```

## 🌐 Deployment

### Production Deployment

The application supports route-based deployment:

1. **Main Application** - Deploy to `yourdomain.com`
2. **Social Hub** - Deploy to `yourdomain.com/social`
3. **Account Management** - Deploy to `yourdomain.com/account`
4. **Profiles** - Deploy to `yourdomain.com/profiles`

### Environment Configuration

Key environment variables:

```env
VITE_PRODUCTION_DOMAIN=yourdomain.com
VITE_API_BASE_URL=https://yourdomain.com/api
VITE_REDIS_URL=redis://your-redis-server
DATABASE_URL=postgresql://user:pass@your-db-server
JWT_SECRET=your-jwt-secret
```

## 📱 Application Structure

### Frontend Applications

```
src/
├── apps/                    # Main application modules
│   ├── AppInfo.vue         # Landing and information
│   ├── AppSocial.vue       # Social features and content
│   ├── AppAccManagement.vue # User account management
│   ├── AppProfile.vue      # User profiles
│   ├── AppReports.vue      # Reporting system
│   └── AppModeration.vue   # Admin moderation tools
├── shared/                 # Shared components
│   ├── components/         # Reusable UI components
│   ├── assets/            # Images, styles, and static files
│   └── styles/            # Global styles and themes
├── services/              # API and utility services
├── router/               # Vue Router configuration
└── store/                # Pinia state management
```

### Backend Structure

```
backend-java/
├── src/main/java/
│   ├── config/           # Security and database configuration
│   ├── controllers/      # REST API endpoints
│   ├── models/          # Database entities
│   ├── repositories/    # Data access layer
│   ├── services/        # Business logic
│   └── security/        # JWT and authentication
├── src/main/resources/
│   ├── application.yml  # Spring Boot configuration
│   └── db/migration/    # Database schema migrations
└── Dockerfile          # Container configuration
```

## 🔧 Development

### Available Scripts

```bash
# Development
npm run dev          # Start development server
npm run serve        # Start preview server

# Building
npm run build        # Build for production
npm run preview      # Preview production build

# Code Quality
npm run lint         # Run ESLint
npm run lint:fix     # Fix linting issues
```

### Code Style

- Follow Vue.js Style Guide
- Use ESLint for code quality
- Implement responsive design principles
- Write semantic HTML5 markup
- Use Bootstrap 5 for consistent styling

## 🎮 Game Categories

Games are managed from database layer.

## 🔒 Security Features

- **JWT Authentication** - Secure token-based authentication
- **Password Encryption** - BCrypt hashing for passwords
- **CORS Protection** - Cross-origin resource sharing controls
- **SQL Injection Prevention** - Parameterized queries
- **XSS Protection** - Input sanitization and output encoding
- **Rate Limiting** - Prevent brute force attacks
- **HTTPS Enforcement** - SSL/TLS encryption

## 📊 Analytics & Monitoring

- **User Engagement Tracking** - Monitor platform usage
- **Content Performance** - Track popular posts and interactions
- **System Health** - Application performance monitoring
- **Error Logging** - Comprehensive error tracking
- **Database Monitoring** - Query performance and optimization

## 🤝 Contributing

We welcome contributions from the community! Here's how you can help:

1. **Fork the repository**
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Commit your changes** (`git commit -m 'Add amazing feature'`)
4. **Push to the branch** (`git push origin feature/amazing-feature`)
5. **Open a Pull Request**

### Development Guidelines

- Write clean, commented code
- Follow the existing code style
- Add tests for new features
- Update documentation as needed
- Ensure all tests pass before submitting

### Getting Help

- **Documentation** - Check our comprehensive guides
- **Community Forum** - Connect with other developers
- **Issue Tracker** - Report bugs or request features

### FAQ

**Q: How do I add a new game category?**
A: Game categories are managed through database.

**Q: Can I self-host the platform?**
A: Yes, the platform is designed for self-hosting with Docker support.

**Q: What are the system requirements?**
A: Minimum 4GB RAM, 2 CPU cores, and 20GB storage for basic setup.

### Long-term Vision

Our goal is to become the definitive platform for e-sports communities worldwide, providing tools and features that support every aspect of competitive gaming culture.

---

**Built with ❤️ for the e-sports community**

*Join us in building the ultimate gaming community platform!*

## 📞 Contact

- **Email**: sezai012@hotmail.com

---

*⚠️ **Educational Notice**: This project is also provided as an educational resource for developers learning modern web application development. While production-ready, please ensure proper security configurations for live deployments.*
