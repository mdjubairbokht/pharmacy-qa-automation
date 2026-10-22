(function () {
  'use strict';

  if (sessionStorage.getItem('rx.token')) {
    window.location.replace('app.html#dashboard');
    return;
  }

  const form = document.getElementById('login-form');
  const errorBox = document.querySelector('[data-testid="login-error"]');

  function showError(message) {
    errorBox.textContent = message;
    errorBox.classList.remove('hidden');
  }

  form.addEventListener('submit', async function (event) {
    event.preventDefault();
    errorBox.classList.add('hidden');

    const username = form.username.value.trim();
    const password = form.password.value;
    if (!username || !password) {
      showError('Username and password are required');
      return;
    }

    try {
      const response = await fetch('api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: username, password: password })
      });
      const body = await response.json().catch(function () { return {}; });
      if (!response.ok) {
        showError(body.message || 'Sign in failed');
        return;
      }
      sessionStorage.setItem('rx.token', body.token);
      sessionStorage.setItem('rx.user', JSON.stringify(body));
      window.location.replace('app.html#dashboard');
    } catch (e) {
      showError('Unable to reach the server');
    }
  });
})();
