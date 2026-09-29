/**
 * settings.js - Manages Settings, Model Manager screen, and system specs for NOVA OFFLINE
 */

const PRESETS = {
  assistant: "You are NOVA OFFLINE, a private on-device AI assistant. Answer accurately, concisely, and format all code blocks cleanly.",
  coder: "You are an expert software engineer and systems architect. Write concise, clean, and secure code with explanatory comments. Format all code blocks with proper syntax tags.",
  concise: "You are a concise, direct AI assistant. Provide answers in bullet points or short paragraphs without preamble or unnecessary fluff.",
  creative: "You are an imaginative creative writer and conversationalist. Provide engaging, expressive, and detailed prose with vivid explanations."
};

const SettingsManager = {
  settings: {
    mode: "offline",
    temperature: 0.7,
    max_tokens: 512,
    context_length: 2048,
    system_prompt: PRESETS.assistant
  },

  init() {
    this.bindDomElements();
    this.bindEvents();
    this.loadSettings();
    this.loadModelInfo();
    this.loadAppInfo();
  },

  bindDomElements() {
    this.modeToggle = document.getElementById('setting-mode-toggle');
    this.tempSlider = document.getElementById('setting-temperature');
    this.tempVal = document.getElementById('val-temperature');
    this.maxTokensSlider = document.getElementById('setting-max-tokens');
    this.maxTokensVal = document.getElementById('val-max-tokens');
    this.contextLenSlider = document.getElementById('setting-context-len');
    this.contextLenVal = document.getElementById('val-context-len');
    this.sysPromptInput = document.getElementById('setting-system-prompt');
    this.btnSave = document.getElementById('btn-save-settings');

    // Model Manager elements
    this.mmStatusBadge = document.getElementById('mm-status-badge');
    this.mmFilename = document.getElementById('mm-filename');
    this.mmFilesize = document.getElementById('mm-filesize');
    this.mmHeaderCheck = document.getElementById('mm-header-check');
    this.mmFilePath = document.getElementById('mm-filepath');
    this.btnImportModel = document.getElementById('btn-import-model');
    this.btnDeleteModel = document.getElementById('btn-delete-model');
    this.btnSettingsDeleteModel = document.getElementById('btn-settings-delete-model');
    this.importProgressBox = document.getElementById('import-progress-box');
    this.progressBarFill = document.getElementById('progress-bar-fill');
    this.progressStatusText = document.getElementById('progress-status-text');
    this.progressPercentText = document.getElementById('progress-percentage-text');

    // Header & Sidebar model indicators
    this.headerModelIndicator = document.getElementById('model-header-indicator');
    this.navModelPill = document.getElementById('nav-model-pill');

    // About screen elements
    this.aboutAppVersion = document.getElementById('about-app-version');
    this.aboutDeviceModel = document.getElementById('about-device-model');
    this.aboutAndroidVer = document.getElementById('about-android-ver');
    this.aboutAbi = document.getElementById('about-abi');

    // Confirmation Modal
    this.confirmModal = document.getElementById('confirm-modal');
    this.modalTitle = document.getElementById('modal-title');
    this.modalMessage = document.getElementById('modal-message');
    this.modalBtnCancel = document.getElementById('modal-btn-cancel');
    this.modalBtnConfirm = document.getElementById('modal-btn-confirm');

    this.btnClearAllHistory = document.getElementById('btn-clear-all-history');
  },

  bindEvents() {
    // Sliders real-time value update
    this.tempSlider.addEventListener('input', (e) => {
      this.tempVal.textContent = parseFloat(e.target.value).toFixed(2);
    });

    this.maxTokensSlider.addEventListener('input', (e) => {
      this.maxTokensVal.textContent = e.target.value;
    });

    this.contextLenSlider.addEventListener('input', (e) => {
      this.contextLenVal.textContent = e.target.value;
    });

    // Preset buttons
    document.querySelectorAll('.btn-preset').forEach(btn => {
      btn.addEventListener('click', () => {
        const presetKey = btn.dataset.preset;
        if (PRESETS[presetKey]) {
          this.sysPromptInput.value = PRESETS[presetKey];
        }
      });
    });

    // Save Settings
    this.btnSave.addEventListener('click', () => {
      this.saveSettings();
    });

    // Import Model
    this.btnImportModel.addEventListener('click', () => {
      if (window.NOVA && window.NOVA.importModel) {
        window.NOVA.importModel();
      } else {
        alert("Native bridge window.NOVA not ready.");
      }
    });

    // Delete Model
    const triggerDeleteModel = () => {
      this.showConfirmDialog(
        "Delete GGUF Model",
        "Are you sure you want to delete the active GGUF model file from private app storage? You will need to re-import it to run local inference.",
        () => {
          if (window.NOVA && window.NOVA.deleteModel) {
            window.NOVA.deleteModel();
            this.loadModelInfo();
          }
        }
      );
    };

    this.btnDeleteModel.addEventListener('click', triggerDeleteModel);
    this.btnSettingsDeleteModel.addEventListener('click', triggerDeleteModel);

    // Clear History
    this.btnClearAllHistory.addEventListener('click', () => {
      this.showConfirmDialog(
        "Clear All Chat History",
        "Permanently delete all conversation sessions and messages from SQLite storage? This action cannot be undone.",
        () => {
          if (window.NOVA && window.NOVA.clearAllHistory) {
            window.NOVA.clearAllHistory();
            if (window.App && window.App.refreshChatHistory) {
              window.App.refreshChatHistory();
              window.App.startNewChat();
            }
          }
        }
      );
    });
  },

  loadSettings() {
    if (window.NOVA && window.NOVA.getSettings) {
      try {
        const raw = window.NOVA.getSettings();
        if (raw) {
          const parsed = JSON.parse(raw);
          this.settings = Object.assign(this.settings, parsed);
        }
      } catch (e) {
        console.error("Failed to parse settings from Android:", e);
      }
    }

    // Apply to controls
    this.modeToggle.checked = this.settings.mode === 'offline';
    this.tempSlider.value = this.settings.temperature;
    this.tempVal.textContent = parseFloat(this.settings.temperature).toFixed(2);
    this.maxTokensSlider.value = this.settings.max_tokens;
    this.maxTokensVal.textContent = this.settings.max_tokens;
    this.contextLenSlider.value = this.settings.context_length;
    this.contextLenVal.textContent = this.settings.context_length;
    this.sysPromptInput.value = this.settings.system_prompt;
  },

  saveSettings() {
    this.settings.mode = this.modeToggle.checked ? "offline" : "online";
    this.settings.temperature = parseFloat(this.tempSlider.value);
    this.settings.max_tokens = parseInt(this.maxTokensSlider.value, 10);
    this.settings.context_length = parseInt(this.contextLenSlider.value, 10);
    this.settings.system_prompt = this.sysPromptInput.value.trim();

    if (window.NOVA && window.NOVA.saveSettings) {
      window.NOVA.saveSettings(JSON.stringify(this.settings));
      if (window.NOVA.showToast) {
        window.NOVA.showToast("Settings saved successfully.");
      }
    }
  },

  loadModelInfo() {
    if (!window.NOVA || !window.NOVA.getModelInfo) return;
    try {
      const raw = window.NOVA.getModelInfo();
      const info = JSON.parse(raw);
      this.updateModelUI(info);
    } catch (e) {
      console.error("Error loading model info:", e);
    }
  },

  updateModelUI(info) {
    if (!info) return;

    if (info.installed) {
      this.mmStatusBadge.textContent = "Installed & Active";
      this.mmStatusBadge.className = "badge badge-success";
      this.mmFilename.textContent = info.fileName || "model.gguf";
      this.mmFilesize.textContent = info.fileSizeFormatted || "Unknown";
      this.mmHeaderCheck.textContent = info.isValidGguf ? "Valid (GGUF magic header verified)" : "Invalid Header";
      this.mmFilePath.textContent = info.filePath || "";
      this.btnDeleteModel.disabled = false;

      // Update Header & Sidebar
      this.headerModelIndicator.textContent = info.fileName || "Model Active";
      this.headerModelIndicator.classList.add('active-model');
      this.navModelPill.textContent = "Ready";
      this.navModelPill.className = "pill pill-success";
    } else {
      this.mmStatusBadge.textContent = "Not Installed";
      this.mmStatusBadge.className = "badge badge-warning";
      this.mmFilename.textContent = "None";
      this.mmFilesize.textContent = "0 MB";
      this.mmHeaderCheck.textContent = "—";
      this.mmFilePath.textContent = info.modelsDirPath || "/data/data/com.nova.offline/files/models/";
      this.btnDeleteModel.disabled = true;

      // Update Header & Sidebar
      this.headerModelIndicator.textContent = "No Model Loaded";
      this.headerModelIndicator.classList.remove('active-model');
      this.navModelPill.textContent = "None";
      this.navModelPill.className = "pill pill-warning";
    }
  },

  loadAppInfo() {
    if (!window.NOVA || !window.NOVA.getAppInfo) return;
    try {
      const raw = window.NOVA.getAppInfo();
      const info = JSON.parse(raw);
      if (this.aboutAppVersion) this.aboutAppVersion.textContent = info.version || "1.0";
      if (this.aboutDeviceModel) this.aboutDeviceModel.textContent = info.deviceModel || "Android Device";
      if (this.aboutAndroidVer) this.aboutAndroidVer.textContent = info.androidVersion || "Android OS";
      if (this.aboutAbi) this.aboutAbi.textContent = info.abi || "arm64-v8a";
    } catch (e) {
      console.error("Error reading app info:", e);
    }
  },

  showConfirmDialog(title, message, onConfirm) {
    this.modalTitle.textContent = title;
    this.modalMessage.textContent = message;
    this.confirmModal.classList.remove('hidden');

    const handleConfirm = () => {
      cleanup();
      if (onConfirm) onConfirm();
    };

    const handleCancel = () => {
      cleanup();
    };

    const cleanup = () => {
      this.modalBtnConfirm.removeEventListener('click', handleConfirm);
      this.modalBtnCancel.removeEventListener('click', handleCancel);
      this.confirmModal.classList.add('hidden');
    };

    this.modalBtnConfirm.addEventListener('click', handleConfirm);
    this.modalBtnCancel.addEventListener('click', handleCancel);
  }
};

// Global Android Callback Handlers
window.onNovaModelStatusChanged = function(modelInfoJson) {
  try {
    const info = typeof modelInfoJson === 'string' ? JSON.parse(modelInfoJson) : modelInfoJson;
    SettingsManager.updateModelUI(info);
  } catch (e) {
    console.error("Failed to handle model status update:", e);
  }
};

window.onNovaImportProgress = function(percent, bytesCopied, totalBytes, status) {
  SettingsManager.importProgressBox.classList.remove('hidden');
  SettingsManager.progressStatusText.textContent = status || "Importing model...";
  SettingsManager.progressPercentText.textContent = percent >= 0 ? `${percent}%` : "";
  SettingsManager.progressBarFill.style.width = percent >= 0 ? `${percent}%` : "50%";
};

window.onNovaImportComplete = function(success, message) {
  setTimeout(() => {
    SettingsManager.importProgressBox.classList.add('hidden');
    SettingsManager.progressBarFill.style.width = "0%";
  }, 1200);

  if (window.NOVA && window.NOVA.showToast) {
    window.NOVA.showToast(message);
  }
  SettingsManager.loadModelInfo();
};

document.addEventListener('DOMContentLoaded', () => {
  SettingsManager.init();
});
