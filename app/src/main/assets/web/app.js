/**
 * app.js - Core application logic, chat session handling, Markdown parser,
 * token streaming renderer, and UI orchestration for NOVA OFFLINE.
 */

const App = {
  currentSessionId: null,
  isGenerating: false,
  currentAssistantMsgElement: null,
  accumulatedResponse: "",
  activeScreen: "chat",

  init() {
    this.bindDom();
    this.bindEvents();
    this.refreshChatHistory();
    this.startNewChat();
  },

  bindDom() {
    // Layout & Navigation
    this.sidebar = document.getElementById('sidebar');
    this.sidebarOverlay = document.getElementById('sidebar-overlay');
    this.btnToggleSidebar = document.getElementById('toggle-sidebar-btn');
    this.btnCloseSidebar = document.getElementById('close-sidebar-btn');
    this.screenTitle = document.getElementById('current-screen-title');

    // Screens
    this.screens = {
      chat: document.getElementById('screen-chat'),
      'model-manager': document.getElementById('screen-model-manager'),
      settings: document.getElementById('screen-settings'),
      about: document.getElementById('screen-about')
    };

    // Chat Elements
    this.messagesContainer = document.getElementById('messages-container');
    this.messageList = document.getElementById('message-list');
    this.chatHero = document.getElementById('chat-empty-hero');
    this.promptInput = document.getElementById('prompt-input');
    this.btnSend = document.getElementById('btn-send-prompt');
    this.btnStop = document.getElementById('btn-stop-generation');
    this.btnNewChat = document.getElementById('btn-new-chat');
    this.chatHistoryList = document.getElementById('chat-history-list');

    // Header buttons
    this.topBtnClear = document.getElementById('top-btn-clear');
    this.topBtnModels = document.getElementById('top-btn-models');
  },

  bindEvents() {
    // Sidebar toggle
    this.btnToggleSidebar.addEventListener('click', () => this.toggleSidebar(true));
    this.btnCloseSidebar.addEventListener('click', () => this.toggleSidebar(false));
    this.sidebarOverlay.addEventListener('click', () => this.toggleSidebar(false));

    // Screen navigation links
    document.querySelectorAll('[data-screen]').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const targetScreen = btn.dataset.screen;
        this.navigateToScreen(targetScreen);
        this.toggleSidebar(false);
      });
    });

    // Top action buttons
    this.topBtnModels.addEventListener('click', () => this.navigateToScreen('model-manager'));
    this.topBtnClear.addEventListener('click', () => this.clearCurrentChat());

    // New Chat
    this.btnNewChat.addEventListener('click', () => {
      this.startNewChat();
      this.navigateToScreen('chat');
      this.toggleSidebar(false);
    });

    // Send & Stop Generation
    this.btnSend.addEventListener('click', () => this.handleSendPrompt());
    this.btnStop.addEventListener('click', () => this.handleStopGeneration());

    // Textarea Auto-resize and Enter key submission
    this.promptInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        this.handleSendPrompt();
      }
    });

    this.promptInput.addEventListener('input', () => {
      this.promptInput.style.height = 'auto';
      const newHeight = Math.min(this.promptInput.scrollHeight, 140);
      this.promptInput.style.height = `${newHeight}px`;
    });

    // Suggestion Chips
    document.querySelectorAll('.chip').forEach(chip => {
      chip.addEventListener('click', () => {
        const prompt = chip.dataset.prompt;
        if (prompt) {
          this.promptInput.value = prompt;
          this.handleSendPrompt();
        }
      });
    });
  },

  toggleSidebar(open) {
    if (open) {
      this.sidebar.classList.add('open');
      this.sidebarOverlay.classList.add('active');
    } else {
      this.sidebar.classList.remove('open');
      this.sidebarOverlay.classList.remove('active');
    }
  },

  navigateToScreen(screenName) {
    if (!this.screens[screenName]) return;

    this.activeScreen = screenName;
    Object.keys(this.screens).forEach(key => {
      this.screens[key].classList.toggle('active', key === screenName);
    });

    // Update Top bar title
    const titles = {
      chat: "Chat",
      'model-manager': "Model Manager",
      settings: "Settings",
      about: "About NOVA"
    };
    this.screenTitle.textContent = titles[screenName] || "NOVA OFFLINE";

    // Update active state on nav buttons
    document.querySelectorAll('[data-screen]').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.screen === screenName);
    });

    if (screenName === 'model-manager' && window.SettingsManager) {
      window.SettingsManager.loadModelInfo();
    }
  },

  startNewChat() {
    this.currentSessionId = null;
    if (window.NOVA && window.NOVA.createNewChat) {
      this.currentSessionId = window.NOVA.createNewChat("New Chat");
    }
    this.messageList.innerHTML = '';
    this.chatHero.classList.remove('hidden');
    this.promptInput.value = '';
    this.promptInput.style.height = 'auto';
    this.refreshChatHistory();
  },

  refreshChatHistory() {
    if (!window.NOVA || !window.NOVA.getChatHistory) return;
    try {
      const raw = window.NOVA.getChatHistory();
      const sessions = JSON.parse(raw);
      this.chatHistoryList.innerHTML = '';

      if (!sessions || sessions.length === 0) {
        this.chatHistoryList.innerHTML = '<div class="history-empty">No conversation history yet.</div>';
        return;
      }

      sessions.forEach(sess => {
        const item = document.createElement('div');
        item.className = `history-item ${sess.id === this.currentSessionId ? 'active' : ''}`;
        item.innerHTML = `
          <span class="history-title" title="${this.escapeHtml(sess.title)}">${this.escapeHtml(sess.title)}</span>
          <button class="history-del-btn" title="Delete conversation" data-id="${sess.id}">
            <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" fill="none" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        `;

        item.addEventListener('click', (e) => {
          if (e.target.closest('.history-del-btn')) return;
          this.loadSession(sess.id);
          this.navigateToScreen('chat');
          this.toggleSidebar(false);
        });

        const delBtn = item.querySelector('.history-del-btn');
        delBtn.addEventListener('click', (e) => {
          e.stopPropagation();
          this.deleteSession(sess.id);
        });

        this.chatHistoryList.appendChild(item);
      });
    } catch (e) {
      console.error("Error refreshing chat history:", e);
    }
  },

  loadSession(sessionId) {
    this.currentSessionId = sessionId;
    if (window.NOVA && window.NOVA.setActiveSession) {
      window.NOVA.setActiveSession(sessionId);
    }

    this.messageList.innerHTML = '';
    this.chatHero.classList.add('hidden');

    try {
      const raw = window.NOVA.getMessages(sessionId);
      const messages = JSON.parse(raw);

      if (!messages || messages.length === 0) {
        this.chatHero.classList.remove('hidden');
      } else {
        messages.forEach(msg => {
          if (msg.role === 'user') {
            this.appendUserMessage(msg.content, false);
          } else if (msg.role === 'assistant') {
            this.appendAssistantMessage(msg.content, msg.id, false);
          }
        });
      }
      this.scrollToBottom();
      this.refreshChatHistory();
    } catch (e) {
      console.error("Error loading session messages:", e);
    }
  },

  deleteSession(sessionId) {
    if (window.NOVA && window.NOVA.deleteChat) {
      window.NOVA.deleteChat(sessionId);
      if (sessionId === this.currentSessionId) {
        this.startNewChat();
      } else {
        this.refreshChatHistory();
      }
    }
  },

  clearCurrentChat() {
    if (!this.currentSessionId) return;

    if (window.SettingsManager) {
      window.SettingsManager.showConfirmDialog(
        "Clear Current Chat",
        "Clear all messages in this conversation?",
        () => {
          if (window.NOVA && window.NOVA.deleteChat) {
            window.NOVA.deleteChat(this.currentSessionId);
            this.startNewChat();
          }
        }
      );
    }
  },

  handleSendPrompt() {
    const prompt = this.promptInput.value.trim();
    if (!prompt || this.isGenerating) return;

    this.chatHero.classList.add('hidden');
    this.appendUserMessage(prompt, true);

    this.promptInput.value = '';
    this.promptInput.style.height = 'auto';

    // Set generating state
    this.setGenerating(true);

    // Prepare assistant container for streaming tokens
    this.accumulatedResponse = "";
    this.currentAssistantMsgElement = this.appendAssistantMessage("", null, true);

    if (window.NOVA && window.NOVA.generate) {
      window.NOVA.generate(prompt);
    } else {
      setTimeout(() => {
        this.onToken("Running in browser preview. Native on-device LLM bridge active on Android.");
        this.onTokenDone();
      }, 300);
    }
  },

  handleStopGeneration() {
    if (window.NOVA && window.NOVA.stopGeneration) {
      window.NOVA.stopGeneration();
    }
    this.setGenerating(false);
    this.removeStreamingCursor();
  },

  setGenerating(generating) {
    this.isGenerating = generating;
    if (generating) {
      this.btnSend.classList.add('hidden');
      this.btnStop.classList.remove('hidden');
    } else {
      this.btnSend.classList.remove('hidden');
      this.btnStop.classList.add('hidden');
    }
  },

  appendUserMessage(text, scroll = true) {
    const item = document.createElement('div');
    item.className = 'message-item user';
    item.innerHTML = `<div class="bubble">${this.escapeHtml(text)}</div>`;
    this.messageList.appendChild(item);
    if (scroll) this.scrollToBottom();
  },

  appendAssistantMessage(markdownContent, msgId, isStreaming = false) {
    const item = document.createElement('div');
    item.className = 'message-item assistant';

    const renderedHtml = this.parseMarkdown(markdownContent);

    item.innerHTML = `
      <div class="message-avatar" title="NOVA AI">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="9" stroke="url(#avGlow)" stroke-width="2"></circle>
          <path d="M12 3v18M3 12h18" stroke="url(#avGlow)"></path>
          <defs>
            <linearGradient id="avGlow" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#38BDF8" />
              <stop offset="100%" stop-color="#A855F7" />
            </linearGradient>
          </defs>
        </svg>
      </div>
      <div class="bubble-wrapper">
        <div class="bubble markdown-body">${renderedHtml}${isStreaming ? '<span class="streaming-cursor"></span>' : ''}</div>
        <div class="message-actions ${isStreaming ? 'hidden' : ''}">
          <button class="action-chip-btn btn-copy-msg" title="Copy response">
            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
            <span>Copy</span>
          </button>
        </div>
      </div>
    `;

    // Bind Copy full message button
    const copyBtn = item.querySelector('.btn-copy-msg');
    copyBtn.addEventListener('click', () => {
      const bubble = item.querySelector('.bubble');
      this.copyToClipboard(bubble.innerText);
    });

    // Bind Code Block copy buttons
    this.bindCodeBlockCopies(item);

    this.messageList.appendChild(item);
    if (isStreaming) this.scrollToBottom();
    return item;
  },

  onToken(token) {
    this.accumulatedResponse += token;
    if (this.currentAssistantMsgElement) {
      const bubble = this.currentAssistantMsgElement.querySelector('.bubble');
      const renderedHtml = this.parseMarkdown(this.accumulatedResponse);
      bubble.innerHTML = `${renderedHtml}<span class="streaming-cursor"></span>`;
      this.bindCodeBlockCopies(this.currentAssistantMsgElement);
      this.scrollToBottom();
    }
  },

  onTokenDone() {
    this.setGenerating(false);
    this.removeStreamingCursor();
    this.refreshChatHistory();
  },

  removeStreamingCursor() {
    if (this.currentAssistantMsgElement) {
      const cursor = this.currentAssistantMsgElement.querySelector('.streaming-cursor');
      if (cursor) cursor.remove();

      const actions = this.currentAssistantMsgElement.querySelector('.message-actions');
      if (actions) actions.classList.remove('hidden');

      this.bindCodeBlockCopies(this.currentAssistantMsgElement);
    }
    this.currentAssistantMsgElement = null;
  },

  bindCodeBlockCopies(parent) {
    parent.querySelectorAll('.btn-copy-code').forEach(btn => {
      btn.onclick = (e) => {
        e.stopPropagation();
        const codeElement = btn.closest('.code-block-container').querySelector('code');
        if (codeElement) {
          this.copyToClipboard(codeElement.innerText);
          const origHtml = btn.innerHTML;
          btn.innerHTML = `<span style="color: #34D399;">✓ Copied</span>`;
          setTimeout(() => { btn.innerHTML = origHtml; }, 1800);
        }
      };
    });
  },

  copyToClipboard(text) {
    if (window.NOVA && window.NOVA.copyToClipboard) {
      window.NOVA.copyToClipboard(text);
    } else if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text);
      if (window.NOVA && window.NOVA.showToast) {
        window.NOVA.showToast("Copied to clipboard");
      }
    }
  },

  scrollToBottom() {
    this.messagesContainer.scrollTop = this.messagesContainer.scrollHeight;
  },

  escapeHtml(str) {
    if (!str) return '';
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  },

  // Lightweight, robust on-device Markdown parser
  parseMarkdown(md) {
    if (!md) return '';

    let out = md;

    // 1. Code blocks: ```lang ... ```
    const codeBlocks = [];
    out = out.replace(/```([a-zA-Z0-9_-]*)\n([\s\S]*?)```/g, (match, lang, code) => {
      const id = `__CODE_BLOCK_${codeBlocks.length}__`;
      const cleanLang = lang || 'CODE';
      const escapedCode = this.escapeHtml(code.trim());
      const blockHtml = `
        <div class="code-block-container">
          <div class="code-block-header">
            <span class="code-lang-label">${cleanLang}</span>
            <button class="btn-copy-code" title="Copy code">
              <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              <span>Copy</span>
            </button>
          </div>
          <pre class="code-block-content"><code>${escapedCode}</code></pre>
        </div>`;
      codeBlocks.push(blockHtml);
      return id;
    });

    // 2. Escape regular HTML tags
    out = this.escapeHtml(out);

    // 3. Restore Code blocks
    codeBlocks.forEach((block, idx) => {
      out = out.replace(`__CODE_BLOCK_${idx}__`, block);
    });

    // 4. Inline code: `code`
    out = out.replace(/`([^`]+)`/g, '<code>$1</code>');

    // 5. Headers
    out = out.replace(/^### (.*$)/gim, '<h3>$1</h3>');
    out = out.replace(/^## (.*$)/gim, '<h2>$1</h2>');
    out = out.replace(/^# (.*$)/gim, '<h1>$1</h1>');

    // 6. Bold & Italic
    out = out.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    out = out.replace(/\*([^*]+)\*/g, '<em>$1</em>');
    out = out.replace(/~~([^~]+)~~/g, '<del>$1</del>');

    // 7. Blockquotes
    out = out.replace(/^> (.*$)/gim, '<blockquote>$1</blockquote>');

    // 8. Unordered Lists
    out = out.replace(/^\s*[-*]\s+(.*)$/gim, '<li>$1</li>');
    out = out.replace(/(<li>[\s\S]*?<\/li>)/gim, '<ul>$1</ul>');
    out = out.replace(/<\/ul>\s*<ul>/g, ''); // consolidate sibling lists

    // 9. Line breaks to paragraphs
    const paragraphs = out.split(/\n{2,}/);
    out = paragraphs.map(p => {
      p = p.trim();
      if (!p) return '';
      if (p.startsWith('<h') || p.startsWith('<ul') || p.startsWith('<blockquote') || p.startsWith('<div class="code-block')) {
        return p;
      }
      return `<p>${p.replace(/\n/g, '<br>')}</p>`;
    }).join('');

    return out;
  }
};

// Global Android to JS token receiver callbacks
window.onNovaToken = function(token, isDone, messageId) {
  if (token) {
    App.onToken(token);
  }
  if (isDone) {
    App.onTokenDone();
  }
};

window.onNovaComplete = function(fullText, messageId) {
  App.onTokenDone();
};

window.onNovaError = function(errorText) {
  App.onToken(`\n\n> ⚠️ **Error**: ${errorText}`);
  App.onTokenDone();
};

window.onNovaGenerationStopped = function() {
  App.handleStopGeneration();
};

document.addEventListener('DOMContentLoaded', () => {
  App.init();
  window.App = App;
});
