/* ===== ScholarTrust — API Utility ===== */
const API = {
  BASE: '',  // same origin — Spring Boot serves static files

  token() { return localStorage.getItem('st_token'); },
  user()  { return JSON.parse(localStorage.getItem('st_user') || 'null'); },

  role() {
    const u = this.user();
    if (!u || !u.role) return null;
    return u.role.replace('ROLE_', '').toUpperCase();
  },

  setSession(token, user) {
    if (user && user.role) {
      user.role = user.role.replace('ROLE_', '').toUpperCase();
    }
    localStorage.setItem('st_token', token);
    localStorage.setItem('st_user', JSON.stringify(user));
  },

  clearSession() {
    localStorage.removeItem('st_token');
    localStorage.removeItem('st_user');
  },

  headers(isFormData = false) {
    const h = {};
    const t = this.token();
    if (t) h['Authorization'] = 'Bearer ' + t;
    if (!isFormData) h['Content-Type'] = 'application/json';
    return h;
  },

  async get(path) {
    const r = await fetch(this.BASE + path, { headers: this.headers() });
    return this._handle(r);
  },

  async post(path, body) {
    const r = await fetch(this.BASE + path, {
      method: 'POST',
      headers: this.headers(),
      body: JSON.stringify(body)
    });
    return this._handle(r);
  },

  async postForm(path, formData) {
    const r = await fetch(this.BASE + path, {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + this.token() },
      body: formData
    });
    return this._handle(r);
  },

  async put(path, body) {
    const r = await fetch(this.BASE + path, {
      method: 'PUT',
      headers: this.headers(),
      body: JSON.stringify(body)
    });
    return this._handle(r);
  },

  async delete(path) {
    const r = await fetch(this.BASE + path, { method: 'DELETE', headers: this.headers() });
    return this._handle(r);
  },

  async _handle(r) {
    const data = await r.json().catch(() => ({ success: false, message: 'Server error or unexpected response' }));
    if (r.status === 401) {
      const p = window.location.pathname;
      const isAuthPage = p.includes('login') || p.includes('register') || p === '/' || p === '/index.html';
      if (!isAuthPage) {
        this.clearSession();
        window.location.href = '/student/login.html';
      }
    }
    return data;
  }
};

/* ---- Auth guards ---- */
function requireStudent() {
  const u = API.user();
  const token = API.token();
  if (!u || !token) {
    window.location.href = '/student/login.html';
    return false;
  }
  const role = (u.role || '').replace('ROLE_', '').toUpperCase();
  if (role !== 'STUDENT') {
    window.location.href = '/admin/login.html';
    return false;
  }
  return true;
}

function requireAdmin() {
  const u = API.user();
  const token = API.token();
  if (!u || !token) {
    window.location.href = '/admin/login.html';
    return false;
  }
  const role = (u.role || '').replace('ROLE_', '').toUpperCase();
  if (role !== 'ADMIN') {
    window.location.href = '/student/login.html';
    return false;
  }
  return true;
}

/* ---- Navbar helpers ---- */
function renderNavUser(elementId) {
  const u = API.user();
  if (u && elementId) {
    const el = document.getElementById(elementId);
    if (el) el.textContent = u.fullName || u.email;
  }
}

function logout() {
  API.clearSession();
  window.location.href = '/student/login.html';
}

function escapeHtml(str) {
  if (str == null) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

/* ---- UI helpers ---- */
function showAlert(id, msg, type = 'error') {
  const el = document.getElementById(id);
  if (!el) return;
  el.className = `alert alert-${type} show`;
  el.innerHTML = msg; // Can contain formatted text, but dynamic strings should use escapeHtml()
  el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function hideAlert(id) {
  const el = document.getElementById(id);
  if (el) el.className = 'alert';
}

function setLoading(btnId, loading, text = 'Submit') {
  const btn = document.getElementById(btnId);
  if (!btn) return;
  btn.disabled = loading;
  btn.innerHTML = loading ? '<span class="spinner"></span> Please wait…' : text;
}

function badgeHtml(status) {
  const s = (status || '').toUpperCase();
  const map = {
    PENDING: 'badge-pending',
    REVIEWING: 'badge-reviewing',
    APPROVED: 'badge-approved',
    REJECTED: 'badge-rejected',
    DISBURSED: 'badge-disbursed',
    ACTIVE: 'badge-active',
    INACTIVE: 'badge-inactive'
  };
  return `<span class="badge ${map[s] || 'badge-pending'}">${status}</span>`;
}

function fmtDate(iso) {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleDateString('en-IN', { day:'numeric', month:'short', year:'numeric' });
  } catch (e) {
    return iso;
  }
}

function fmtMoney(n) {
  if (n == null || isNaN(n)) return '—';
  return '₹' + Number(n).toLocaleString('en-IN');
}

function openModal(id)  { const el = document.getElementById(id); if (el) el.classList.add('open'); }
function closeModal(id) { const el = document.getElementById(id); if (el) el.classList.remove('open'); }