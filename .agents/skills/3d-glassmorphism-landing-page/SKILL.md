---
name: 3d-glassmorphism-landing-page
description: Provides guidelines and boilerplate code to create a highly professional, modern 3D Glassmorphism landing page for tech products using Three.js, Vanilla-Tilt.js, and advanced CSS.
---

# 3D Glassmorphism Landing Page — Implementation Guide

Use this skill when asked to create a professional, "Fintech Startup" or "Apple-like" landing page for a new or existing product. This architecture guarantees a highly interactive, 3D visually stunning website that performs well and looks incredibly premium.

## Core Dependencies
Always include these in the `<head>` or at the end of the `<body>`:
1. **Fonts**: `Inter` (body) and `Outfit` (headings).
2. **Three.js**: For the interactive 3D background.
   `<script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>`
3. **Vanilla-Tilt.js**: For 3D hover effects on cards.
   `<script src="https://cdnjs.cloudflare.com/ajax/libs/vanilla-tilt/1.8.0/vanilla-tilt.min.js"></script>`

## 1. HTML Architecture (`index.html`)

Structure the page with a fixed canvas behind a relative content wrapper. Use `data-tilt` on cards.

```html
<body>
  <!-- Fixed 3D Canvas Background -->
  <canvas id="bg-canvas"></canvas>

  <div class="content-wrapper">
    <!-- Navbar -->
    <nav class="navbar reveal">
      <div class="brand">YourBrand</div>
      <div class="nav-links">
        <a href="#features">Features</a>
        <a href="#" class="btn-primary btn-sm">Get Started</a>
      </div>
    </nav>

    <!-- Hero Section -->
    <header class="hero reveal">
      <div class="hero-content">
        <div class="badge">New Release v2.0</div>
        <h1 class="hero-title">The Ultimate <span class="gradient-text">Product</span>.</h1>
        <p class="hero-subtitle">Engaging subtitle describing the core value proposition.</p>
        <div class="hero-actions">
          <a href="#" class="btn-primary btn-lg glow">Download Now</a>
          <a href="#" class="btn-secondary btn-lg">View Source</a>
        </div>
      </div>
    </header>

    <!-- Features Section (Glass Cards) -->
    <section id="features" class="section reveal">
      <div class="features-grid">
        <div class="feature-card glass" data-tilt data-tilt-max="5" data-tilt-glare data-tilt-max-glare="0.1">
          <div class="icon-box">...</div>
          <h3>Feature 1</h3>
          <p>Feature description goes here.</p>
        </div>
      </div>
    </section>
  </div>
  
  <script src="...vanilla-tilt.min.js"></script>
  <script src="...three.min.js"></script>
  <script src="app.js"></script>
</body>
```

## 2. CSS Design System (`style.css`)

Rely heavily on deep backgrounds, CSS variables for theming, and `backdrop-filter` for the glass effect.

```css
:root {
  --bg-color: #05050A; /* Midnight black/blue */
  --text-primary: #f8fafc;
  --text-secondary: #94a3b8;
  --primary: #6366F1; /* Indigo */
  --primary-glow: rgba(99, 102, 241, 0.5);
  --cyan: #06B6D4;
  --pink: #EC4899;
  
  /* Glassmorphism Tokens */
  --glass-bg: rgba(255, 255, 255, 0.02);
  --glass-border: rgba(255, 255, 255, 0.08);
  --glass-shadow: 0 10px 40px -10px rgba(0, 0, 0, 0.7);
  --glass-blur: 24px;
}

body {
  font-family: 'Inter', sans-serif;
  background-color: var(--bg-color);
  color: var(--text-primary);
  margin: 0;
}

h1, h2, h3 { font-family: 'Outfit', sans-serif; }

#bg-canvas {
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
}

.content-wrapper {
  position: relative;
  z-index: 1;
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 20px;
}

/* Reusable Glass Class */
.glass {
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--glass-border);
  box-shadow: var(--glass-shadow);
  border-radius: 24px;
}

/* Gradient Text */
.gradient-text {
  background: linear-gradient(135deg, var(--cyan), var(--primary), var(--pink));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* Scroll Reveal */
.reveal {
  opacity: 0;
  transform: translateY(30px);
  transition: all 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}
.reveal.active {
  opacity: 1;
  transform: translateY(0);
}
```

## 3. JavaScript Logic (`app.js`)

Implement IntersectionObservers for the `.reveal` classes and Three.js for the dynamic background.

```javascript
document.addEventListener('DOMContentLoaded', () => {
  // 1. Scroll Reveal Logic
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('active');
        observer.unobserve(entry.target);
      }
    });
  }, { threshold: 0.1, rootMargin: "0px 0px -50px 0px" });

  document.querySelectorAll('.reveal').forEach(el => observer.observe(el));

  // 2. Three.js Background Initialization
  initThreeJS();
});

function initThreeJS() {
  const canvas = document.getElementById('bg-canvas');
  if (!canvas) return;

  const scene = new THREE.Scene();
  const camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 1000);
  const renderer = new THREE.WebGLRenderer({ canvas: canvas, alpha: true, antialias: true });
  
  renderer.setSize(window.innerWidth, window.innerHeight);
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));

  // Premium Physical Materials (Glass-like)
  const materialParams = {
    metalness: 0.2,
    roughness: 0.1,
    transmission: 0.9, // This creates the glass refraction
    thickness: 1.0,
    clearcoat: 1.0
  };

  const mat1 = new THREE.MeshPhysicalMaterial({ color: 0x6366F1, ...materialParams });
  const mat2 = new THREE.MeshPhysicalMaterial({ color: 0x10B981, ...materialParams });
  const mat3 = new THREE.MeshPhysicalMaterial({ color: 0xEC4899, ...materialParams });

  // Use abstract geometries
  const geometries = [
    new THREE.TorusGeometry(1, 0.3, 16, 100),
    new THREE.SphereGeometry(1, 32, 32),
    new THREE.TorusKnotGeometry(0.8, 0.2, 100, 16)
  ];
  const mats = [mat1, mat2, mat3];
  const meshes = [];

  for (let i = 0; i < 20; i++) {
    const mesh = new THREE.Mesh(geometries[i % 3], mats[i % 3]);
    mesh.position.set((Math.random() - 0.5) * 35, (Math.random() - 0.5) * 35, (Math.random() - 0.5) * 15 - 10);
    const scale = Math.random() * 1.2 + 0.4;
    mesh.scale.set(scale, scale, scale);
    mesh.rotation.set(Math.random() * Math.PI, Math.random() * Math.PI, 0);

    mesh.userData = {
      rx: (Math.random() - 0.5) * 0.01,
      ry: (Math.random() - 0.5) * 0.01,
      speedY: (Math.random() * 0.005) + 0.002
    };
    scene.add(mesh);
    meshes.push(mesh);
  }

  // Lighting
  scene.add(new THREE.AmbientLight(0xffffff, 0.4));
  scene.add(new THREE.DirectionalLight(0xffffff, 1));
  camera.position.z = 12;

  // Mouse Parallax
  let mouseX = 0, mouseY = 0;
  document.addEventListener('mousemove', (e) => {
    mouseX = (e.clientX - window.innerWidth / 2) * 0.0005;
    mouseY = (e.clientY - window.innerHeight / 2) * 0.0005;
  });

  const clock = new THREE.Clock();

  function animate() {
    requestAnimationFrame(animate);
    const elapsed = clock.getElapsedTime();
    
    // Soft camera tracking
    camera.position.x += (mouseX * 2 - camera.position.x) * 0.02;
    camera.position.y += (-mouseY * 2 - camera.position.y) * 0.02;
    camera.lookAt(scene.position);

    // Float objects
    meshes.forEach((mesh, i) => {
      mesh.rotation.x += mesh.userData.rx;
      mesh.rotation.y += mesh.userData.ry;
      mesh.position.y += Math.sin(elapsed * mesh.userData.speedY * 50 + i) * 0.01;
    });

    renderer.render(scene, camera);
  }
  animate();

  window.addEventListener('resize', () => {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
  });
}
```

## Adaptability Checklist
When adopting this skill for another product:
1. Update `<title>`, `<meta name="description">` in `index.html`.
2. Change the primary gradient colors (`--cyan`, `--primary`, `--pink`) in `style.css` to match the brand identity.
3. Update the Three.js point lights and physical material colors in `app.js` to mirror the CSS variables.
4. Replace the "Simulator" or specific app logic with the actual product demonstration feature.
