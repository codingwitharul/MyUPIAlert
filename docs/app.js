// MyUPIAlert Landing Page Interactive Engine

// 1. Languages Database
const LANGUAGES = [
  { code: 'hi', name: 'Hindi', native: 'हिन्दी', template: (amt, sender) => `${sender} से ${amt} रुपये प्राप्त हुए` },
  { code: 'en', name: 'English', native: 'English', template: (amt, sender) => `Received ${amt} rupees from ${sender}` },
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

// Audio Context for the Crystal Payment Chime
let audioCtx = null;

function playPaymentChime() {
  try {
    const AudioContext = window.AudioContext || window.webkitAudioContext;
    if (!audioCtx) {
      audioCtx = new AudioContext();
    }
    if (audioCtx.state === 'suspended') {
      audioCtx.resume();
    }

    const now = audioCtx.currentTime;
    
    // Tone 1: High crisp ding
    const osc1 = audioCtx.createOscillator();
    const gain1 = audioCtx.createGain();
    osc1.type = 'sine';
    osc1.frequency.setValueAtTime(880, now); // A5
    osc1.frequency.exponentialRampToValueAtTime(1760, now + 0.12);
    gain1.gain.setValueAtTime(0.4, now);
    gain1.gain.exponentialRampToValueAtTime(0.001, now + 0.4);
    osc1.connect(gain1);
    gain1.connect(audioCtx.destination);
    osc1.start(now);
    osc1.stop(now + 0.4);

    // Tone 2: Harmonic shimmer
    const osc2 = audioCtx.createOscillator();
    const gain2 = audioCtx.createGain();
    osc2.type = 'triangle';
    osc2.frequency.setValueAtTime(1320, now + 0.08); // E6
    gain2.gain.setValueAtTime(0.3, now + 0.08);
    gain2.gain.exponentialRampToValueAtTime(0.001, now + 0.5);
    osc2.connect(gain2);
    gain2.connect(audioCtx.destination);
    osc2.start(now + 0.08);
    osc2.stop(now + 0.5);
  } catch (e) {
    console.warn("Web Audio not available:", e);
  }
}

// Speak Speech in Selected Language
function speakPhrase(phrase, langCode) {
  if (!('speechSynthesis' in window)) {
    alert("Speech Synthesis not supported in this browser. Please use a modern browser.");
    return;
  }

  window.speechSynthesis.cancel();
  
  // Wait 350ms after chime to speak
  setTimeout(() => {
    const utterance = new SpeechSynthesisUtterance(phrase);
    utterance.rate = 0.95; // Slightly measured pace for currency clarity
    utterance.pitch = 1.0;

    // Map language code to BCP 47
    const langMap = {
      hi: 'hi-IN',
      en: 'en-IN',
      ta: 'ta-IN',
      te: 'te-IN',
      kn: 'kn-IN',
      ml: 'ml-IN',
      gu: 'gu-IN',
      mr: 'mr-IN',
      pa: 'pa-IN',
      bn: 'bn-IN',
      or: 'or-IN',
      as: 'as-IN',
      ur: 'ur-IN'
    };
    utterance.lang = langMap[langCode] || 'en-IN';

    // Find best voice
    const voices = window.speechSynthesis.getVoices();
    const matchedVoice = voices.find(v => v.lang.startsWith(langCode));
    if (matchedVoice) {
      utterance.voice = matchedVoice;
    }

    window.speechSynthesis.speak(utterance);
  }, 400);
}

// 2. DOM Ready Logic
document.addEventListener('DOMContentLoaded', () => {
  // Populate Language Dropdown & Matrix
  const simLangSelect = document.getElementById('simLang');
  const langGrid = document.getElementById('langGrid');

  LANGUAGES.forEach(lang => {
    // Select Option
    if (simLangSelect) {
      const opt = document.createElement('option');
      opt.value = lang.code;
      opt.textContent = `${lang.name} (${lang.native})`;
      if (lang.code === 'hi') opt.selected = true;
      simLangSelect.appendChild(opt);
    }

    // Grid Card
    if (langGrid) {
      const card = document.createElement('div');
      card.className = 'lang-card';
      card.dataset.code = lang.code;
      card.innerHTML = `
        <div class="lang-native">${lang.native}</div>
        <div class="lang-english">${lang.name}</div>
      `;
      card.addEventListener('click', () => {
        if (simLangSelect) {
          simLangSelect.value = lang.code;
          updateSimulatorPreview();
          triggerSimulator();
        }
        document.querySelectorAll('.lang-card').forEach(c => c.classList.remove('active'));
        card.classList.add('active');
      });
      langGrid.appendChild(card);
    }
  });

  // Simulator Elements
  const simAmountInput = document.getElementById('simAmount');
  const simSenderInput = document.getElementById('simSender');
  const simPlayBtn = document.getElementById('simPlayBtn');
  const phoneAmount = document.getElementById('phoneAmount');
  const phoneSender = document.getElementById('phoneSender');
  const phoneSpeech = document.getElementById('phoneSpeech');

  function updateSimulatorPreview() {
    const amt = simAmountInput ? simAmountInput.value.trim() || '500' : '500';
    const sender = simSenderInput ? simSenderInput.value.trim() || 'Rahul' : 'Rahul';
    const langCode = simLangSelect ? simLangSelect.value : 'hi';

    const langObj = LANGUAGES.find(l => l.code === langCode) || LANGUAGES[0];
    const phrase = langObj.template(amt, sender);

    if (phoneAmount) phoneAmount.textContent = `₹${amt}`;
    if (phoneSender) phoneSender.textContent = `From ${sender} · PhonePe / UPI`;
    if (phoneSpeech) phoneSpeech.textContent = `"${phrase}"`;

    return { amt, sender, langCode, phrase };
  }

  function triggerSimulator() {
    const { phrase, langCode } = updateSimulatorPreview();
    playPaymentChime();
    speakPhrase(phrase, langCode);

    // Visual ripple effect on phone mockup
    const phoneCard = document.querySelector('.phone-card');
    if (phoneCard) {
      phoneCard.style.boxShadow = '0 0 30px rgba(16, 185, 129, 0.4)';
      setTimeout(() => {
        phoneCard.style.boxShadow = '';
      }, 1500);
    }
  }

  if (simPlayBtn) {
    simPlayBtn.addEventListener('click', triggerSimulator);
  }

  if (simAmountInput) simAmountInput.addEventListener('input', updateSimulatorPreview);
  if (simSenderInput) simSenderInput.addEventListener('input', updateSimulatorPreview);
  if (simLangSelect) simLangSelect.addEventListener('change', updateSimulatorPreview);

  updateSimulatorPreview();

  // 3. Savings Calculator
  const calcCounters = document.getElementById('calcCounters');
  const calcYears = document.getElementById('calcYears');
  const calcCountersVal = document.getElementById('calcCountersVal');
  const calcYearsVal = document.getElementById('calcYearsVal');
  const calcTotalSavings = document.getElementById('calcTotalSavings');

  function calculateSavings() {
    const counters = parseInt(calcCounters ? calcCounters.value : 1, 10);
    const years = parseInt(calcYears ? calcYears.value : 1, 10);

    if (calcCountersVal) calcCountersVal.textContent = `${counters} Counter${counters > 1 ? 's' : ''}`;
    if (calcYearsVal) calcYearsVal.textContent = `${years} Year${years > 1 ? 's' : ''}`;

    // Standard commercial box: ₹175/mo rental + ₹50/mo SIM data fee = ₹225/mo per counter
    // ₹225 * 12 months = ₹2,700/year per counter
    const annualCostPerCounter = 2700;
    const totalSavings = counters * years * annualCostPerCounter;

    if (calcTotalSavings) {
      calcTotalSavings.textContent = `₹${totalSavings.toLocaleString('en-IN')}`;
    }
  }

  if (calcCounters) calcCounters.addEventListener('input', calculateSavings);
  if (calcYears) calcYears.addEventListener('input', calculateSavings);
  calculateSavings();

  // 4. FAQ Accordion
  const faqItems = document.querySelectorAll('.faq-item');
  faqItems.forEach(item => {
    const question = item.querySelector('.faq-question');
    if (question) {
      question.addEventListener('click', () => {
        const isActive = item.classList.contains('active');
        faqItems.forEach(i => i.classList.remove('active'));
        if (!isActive) {
          item.classList.add('active');
        }
      });
    }
  });
});
