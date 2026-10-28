// 1. Languages Database
const LANGUAGES = [
  { code: 'en', name: 'English', native: 'English', template: (amt, sender) => `Received ${amt} rupees from ${sender}` },
  { code: 'hi', name: 'Hindi', native: 'हिन्दी', template: (amt, sender) => `${sender} से ${amt} रुपये प्राप्त हुए` },
  { code: 'ta', name: 'Tamil', native: 'தமிழ்', template: (amt, sender) => `${sender} இடமிருந்து ${amt} ரூபாய் பெறப்பட்டது` },
  { code: 'te', name: 'Telugu', native: 'తెలుగు', template: (amt, sender) => `${sender} నుండి ${amt} రూపాయలు అందాయి` },
  { code: 'kn', name: 'Kannada', native: 'ಕನ್ನಡ', template: (amt, sender) => `${sender} ಅವರಿಂದ ${amt} ರೂಪಾಯಿ ಸ್ವೀಕರಿಸಲಾಗಿದೆ` },
  { code: 'ml', name: 'Malayalam', native: 'മലയാളം', template: (amt, sender) => `${sender}-ൽ നിന്ന് ${amt} രൂപ ലഭിച്ചു` },
  { code: 'gu', name: 'Gujarati', native: 'ગુજરાતી', template: (amt, sender) => `${sender} તરફથી ${amt} રૂપિયા મળ્યા` },
  { code: 'mr', name: 'Marathi', native: 'मराठी', template: (amt, sender) => `${sender} कडून ${amt} रुपये मिळाले` },
  { code: 'pa', name: 'Punjabi', native: 'ਪੰਜਾਬੀ', template: (amt, sender) => `${sender} ਵੱਲੋਂ ${amt} ਰੁਪਏ ਪ੍ਰਾਪਤ ਹੋਏ` },
  { code: 'bn', name: 'Bengali', native: 'বাংলা', template: (amt, sender) => `${sender} এর কাছ থেকে ${amt} টাকা পাওয়া গেছে` },
  { code: 'or', name: 'Odia', native: 'ଓଡ଼ିଆ', template: (amt, sender) => `${sender} ଙ୍କଠାରୁ ${amt} ଟଙ୍କା ପ୍ରାପ୍ତ ହେଲା` },
  { code: 'as', name: 'Assamese', native: 'অসমীয়া', template: (amt, sender) => `${sender} ৰ পৰা ${amt} টকা পোৱা গ'ল` },
  { code: 'ur', name: 'Urdu', native: 'اردو', template: (amt, sender) => `${sender} سے ${amt} روپے موصول ہوئے` }
];

document.addEventListener('DOMContentLoaded', () => {
  // 1. Initialize Simulator
  const langSelect = document.getElementById('sim-language');

  LANGUAGES.forEach(lang => {
    const option = document.createElement('option');
    option.value = lang.code;
    option.textContent = `${lang.name} (${lang.native})`;
    langSelect.appendChild(option);
  });

  const playBtn = document.getElementById('sim-play-btn');
  const transcript = document.getElementById('sim-transcript');

  playBtn.addEventListener('click', () => {
    const amt = document.getElementById('sim-amount').value || '0';
    const sender = document.getElementById('sim-sender').value || 'Customer';
    const langCode = document.getElementById('sim-language').value;

    const langObj = LANGUAGES.find(l => l.code === langCode);
    const phrase = langObj.template(amt, sender);

    transcript.innerHTML = `🔊 <strong>${phrase}</strong>`;

    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(phrase);
      utterance.lang = `${langCode}-IN`;
      utterance.rate = 0.95;
      window.speechSynthesis.speak(utterance);
    }
  });

  // 2. Setup Three.js Background
  initThreeJS();

  // 3. Scroll Reveal Animations
  const observerOptions = {
    threshold: 0.1,
    rootMargin: "0px 0px -50px 0px"
  };

  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('active');
        observer.unobserve(entry.target);
      }
    });
  }, observerOptions);

  document.querySelectorAll('.reveal').forEach(el => {
    observer.observe(el);
  });
});

function initThreeJS() {
  const canvas = document.getElementById('bg-canvas');
  if (!canvas) return;

  const scene = new THREE.Scene();
  const camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 1000);
  const renderer = new THREE.WebGLRenderer({ canvas: canvas, alpha: true, antialias: true });

  renderer.setSize(window.innerWidth, window.innerHeight);
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));

  // Use Torus (Rings) and Spheres for a more premium, abstract fintech look
  const geometry1 = new THREE.TorusGeometry(1, 0.3, 16, 100);
  const geometry2 = new THREE.SphereGeometry(1, 32, 32);
  const geometry3 = new THREE.TorusKnotGeometry(0.8, 0.2, 100, 16);

  const materials = [
    new THREE.MeshPhysicalMaterial({
      color: 0x6366F1, // Indigo
      metalness: 0.2,
      roughness: 0.1,
      transmission: 0.9,
      thickness: 1.0,
      clearcoat: 1.0
    }),
    new THREE.MeshPhysicalMaterial({
      color: 0x10B981, // Emerald
      metalness: 0.2,
      roughness: 0.1,
      transmission: 0.9,
      thickness: 1.0,
      clearcoat: 1.0
    }),
    new THREE.MeshPhysicalMaterial({
      color: 0xEC4899, // Pink
      metalness: 0.2,
      roughness: 0.1,
      transmission: 0.9,
      thickness: 1.0,
      clearcoat: 1.0
    })
  ];

  const meshes = [];
  const geometries = [geometry1, geometry2, geometry3];

  for (let i = 0; i < 25; i++) {
    const mesh = new THREE.Mesh(
        geometries[i % 3],
        materials[i % 3]
    );

    mesh.position.x = (Math.random() - 0.5) * 35;
    mesh.position.y = (Math.random() - 0.5) * 35;
    mesh.position.z = (Math.random() - 0.5) * 15 - 10;

    const scale = Math.random() * 1.2 + 0.4;
    mesh.scale.set(scale, scale, scale);

    mesh.rotation.x = Math.random() * Math.PI;
    mesh.rotation.y = Math.random() * Math.PI;

    mesh.userData = {
      rx: (Math.random() - 0.5) * 0.01,
      ry: (Math.random() - 0.5) * 0.01,
      speedY: (Math.random() * 0.005) + 0.002
    };

    scene.add(mesh);
    meshes.push(mesh);
  }

  const ambientLight = new THREE.AmbientLight(0xffffff, 0.4);
  scene.add(ambientLight);

  const directionalLight = new THREE.DirectionalLight(0xffffff, 1);
  directionalLight.position.set(5, 10, 7);
  scene.add(directionalLight);

  const pointLight1 = new THREE.PointLight(0x6366F1, 3, 50);
  pointLight1.position.set(10, 10, 10);
  scene.add(pointLight1);

  const pointLight2 = new THREE.PointLight(0x10B981, 2, 50);
  pointLight2.position.set(-10, -10, 10);
  scene.add(pointLight2);

  camera.position.z = 12;

  let mouseX = 0;
  let mouseY = 0;
  let targetX = 0;
  let targetY = 0;
  const windowHalfX = window.innerWidth / 2;
  const windowHalfY = window.innerHeight / 2;

  document.addEventListener('mousemove', (e) => {
    mouseX = (e.clientX - windowHalfX) * 0.0005;
    mouseY = (e.clientY - windowHalfY) * 0.0005;
  });

  const clock = new THREE.Clock();

  function animate() {
    requestAnimationFrame(animate);
    const elapsedTime = clock.getElapsedTime();

    targetX = mouseX * 2;
    targetY = mouseY * 2;

    camera.position.x += (targetX - camera.position.x) * 0.02;
    camera.position.y += (-targetY - camera.position.y) * 0.02;
    camera.lookAt(scene.position);

    meshes.forEach((mesh, index) => {
      mesh.rotation.x += mesh.userData.rx;
      mesh.rotation.y += mesh.userData.ry;
      mesh.position.y += Math.sin(elapsedTime * mesh.userData.speedY * 50 + index) * 0.01;
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
