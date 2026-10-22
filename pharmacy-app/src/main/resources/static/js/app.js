(function () {
  'use strict';

  const token = sessionStorage.getItem('rx.token');
  if (!token) {
    window.location.replace('index.html');
    return;
  }
  const user = JSON.parse(sessionStorage.getItem('rx.user') || '{}');
  const isPharmacist = user.role === 'PHARMACIST';
  const state = { drugs: [], patients: [], rxById: {} };

  // ------------------------------------------------------------------ helpers
  const tid = (id, root) => (root || document).querySelector('[data-testid="' + id + '"]');

  function el(tag, attrs, children) {
    const node = document.createElement(tag);
    Object.entries(attrs || {}).forEach(function ([key, value]) {
      if (value === undefined || value === null || value === false) return;
      if (key === 'text') node.textContent = value;
      else if (key === 'onclick') node.addEventListener('click', value);
      else node.setAttribute(key, value === true ? '' : value);
    });
    (children || []).forEach(function (child) {
      node.append(child instanceof Node ? child : document.createTextNode(String(child)));
    });
    return node;
  }

  const money = (value) => '$' + Number(value || 0).toFixed(2);

  async function api(path, options) {
    const opts = options || {};
    const headers = { Authorization: 'Bearer ' + token };
    if (opts.body !== undefined) headers['Content-Type'] = 'application/json';
    const response = await fetch('api/' + path, {
      method: opts.method || 'GET',
      headers: headers,
      body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined
    });
    if (response.status === 401) {
      sessionStorage.clear();
      window.location.replace('index.html');
      throw new Error('Session expired');
    }
    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) {
      const error = new Error((data && data.message) || 'Request failed (' + response.status + ')');
      error.details = (data && data.details) || [];
      throw error;
    }
    return data;
  }

  let toastTimer;
  function toast(message, isError) {
    const box = tid('toast');
    box.textContent = message;
    box.classList.toggle('error', !!isError);
    box.classList.remove('hidden');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { box.classList.add('hidden'); }, 6000);
  }

  function showFormError(box, error) {
    box.replaceChildren(document.createTextNode(error.message));
    if (error.details && error.details.length) {
      box.append(el('ul', {}, error.details.map(function (d) { return el('li', { text: d }); })));
    }
    box.classList.remove('hidden');
  }

  function setState(table, value) { table.setAttribute('data-state', value); }

  // ------------------------------------------------------------------ shell
  tid('current-user').textContent = user.displayName || user.username || '';
  tid('current-role').textContent = user.role || '';

  document.getElementById('logout').addEventListener('click', async function () {
    try { await api('auth/logout', { method: 'POST' }); } catch (e) { /* ignore */ }
    sessionStorage.clear();
    window.location.replace('index.html');
  });

  const views = ['dashboard', 'patients', 'prescriptions', 'inventory'];
  const loaders = {};

  function route() {
    let view = (window.location.hash || '#dashboard').slice(1);
    if (views.indexOf(view) < 0) view = 'dashboard';
    views.forEach(function (v) {
      document.getElementById('view-' + v).classList.toggle('hidden', v !== view);
    });
    document.querySelectorAll('.sidenav a').forEach(function (a) {
      a.classList.toggle('active', a.getAttribute('data-view') === view);
    });
    loaders[view]().catch(function (e) { toast(e.message, true); });
  }
  window.addEventListener('hashchange', route);
  // Clicking the link of the view that is already open does not change the hash: reload it explicitly.
  document.querySelectorAll('.sidenav a').forEach(function (link) {
    link.addEventListener('click', function () {
      if (window.location.hash === link.getAttribute('href')) route();
    });
  });

  async function loadReferenceData() {
    const results = await Promise.all([api('patients'), api('drugs')]);
    state.patients = results[0];
    state.drugs = results[1];
  }

  // ------------------------------------------------------------------ dashboard
  loaders.dashboard = async function () {
    const section = tid('view-dashboard');
    setState(section, 'loading');
    const summary = await api('dashboard');
    Object.entries(summary.prescriptionsByStatus).forEach(function ([status, count]) {
      const card = tid('stat-' + status);
      if (card) card.querySelector('.value').textContent = count;
    });
    tid('stat-patients').querySelector('.value').textContent = summary.patientCount;
    tid('stat-low-stock').querySelector('.value').textContent = summary.lowStockCount;
    tid('stat-sales-today').querySelector('.value').textContent = money(summary.salesToday);
    setState(section, 'ready');
  };

  // ------------------------------------------------------------------ patients
  const patientDialog = document.getElementById('patient-dialog');
  const patientForm = document.getElementById('patient-form');
  const patientError = tid('patient-form-error');
  let editingPatientId = null;

  loaders.patients = async function () {
    const table = tid('patients-table');
    setState(table, 'loading');
    const term = document.getElementById('patient-search').value.trim();
    const patients = await api('patients' + (term ? '?search=' + encodeURIComponent(term) : ''));
    const body = document.getElementById('patients-body');
    body.replaceChildren.apply(body, patients.map(function (p) {
      const allergies = p.allergies.length
        ? p.allergies.map(function (a) { return el('span', { class: 'chip', text: a }); })
        : [el('span', { class: 'muted', text: 'NKA' })];
      return el('tr', { 'data-testid': 'patient-row', 'data-patient-id': p.id }, [
        el('td', { 'data-col': 'name', text: p.lastName + ', ' + p.firstName }),
        el('td', { 'data-col': 'dob', class: 'mono', text: p.dateOfBirth }),
        el('td', { 'data-col': 'phone', class: 'mono', text: p.phone }),
        el('td', { 'data-col': 'allergies' }, allergies),
        el('td', { 'data-col': 'insurance', text: p.insurancePlan || '-' }),
        el('td', { class: 'actions' }, [
          el('button', { class: 'btn btn-sm', 'data-testid': 'patient-edit', text: 'Edit',
            onclick: function () { openPatientDialog(p); } })
        ])
      ]);
    }));
    tid('patients-empty').classList.toggle('hidden', patients.length > 0);
    setState(table, 'ready');
  };

  function openPatientDialog(patient) {
    editingPatientId = patient ? patient.id : null;
    document.getElementById('patient-dialog-title').textContent = patient ? 'Edit patient' : 'Add patient';
    patientError.classList.add('hidden');
    patientForm.reset();
    if (patient) {
      document.getElementById('p-first').value = patient.firstName;
      document.getElementById('p-last').value = patient.lastName;
      document.getElementById('p-dob').value = patient.dateOfBirth;
      document.getElementById('p-gender').value = patient.gender;
      document.getElementById('p-phone').value = patient.phone;
      document.getElementById('p-email').value = patient.email || '';
      document.getElementById('p-address').value = patient.address || '';
      document.getElementById('p-insurance').value = patient.insurancePlan || '';
      document.getElementById('p-allergies').value = patient.allergies.join(', ');
    }
    patientDialog.showModal();
  }

  document.getElementById('add-patient').addEventListener('click', function () { openPatientDialog(null); });
  document.getElementById('patient-cancel').addEventListener('click', function () { patientDialog.close(); });
  document.getElementById('patient-search-btn').addEventListener('click', function () { loaders.patients(); });
  document.getElementById('patient-search').addEventListener('keydown', function (e) {
    if (e.key === 'Enter') loaders.patients();
  });

  patientForm.addEventListener('submit', async function (event) {
    event.preventDefault();
    const value = (id) => document.getElementById(id).value.trim();
    const payload = {
      firstName: value('p-first'),
      lastName: value('p-last'),
      dateOfBirth: value('p-dob') || null,
      gender: value('p-gender'),
      phone: value('p-phone'),
      email: value('p-email') || null,
      address: value('p-address') || null,
      insurancePlan: value('p-insurance') || null,
      allergies: value('p-allergies') ? value('p-allergies').split(',').map(function (s) { return s.trim(); })
        .filter(Boolean) : []
    };
    try {
      const saved = editingPatientId
        ? await api('patients/' + editingPatientId, { method: 'PUT', body: payload })
        : await api('patients', { method: 'POST', body: payload });
      patientDialog.close();
      toast('Patient ' + saved.lastName + ', ' + saved.firstName + (editingPatientId ? ' updated' : ' added'));
      await loaders.patients();
    } catch (e) {
      showFormError(patientError, e);
    }
  });

  // ------------------------------------------------------------------ prescriptions
  const rxDialog = document.getElementById('rx-dialog');
  const rxForm = document.getElementById('rx-form');
  const rxError = tid('rx-form-error');
  const pickupDialog = document.getElementById('pickup-dialog');
  const cancelDialog = document.getElementById('cancel-dialog');
  let activeRx = null;

  const actionLabels = { verify: 'verified', fill: 'filled', refill: 'refilled' };

  function rxActions(rx) {
    const buttons = [];
    const add = (action, label, handler, extraClass) => buttons.push(el('button', {
      class: 'btn btn-sm' + (extraClass ? ' ' + extraClass : ''),
      'data-testid': 'rx-action-' + action, text: label, onclick: handler
    }));
    const simple = (action) => async function () {
      try {
        const updated = await api('prescriptions/' + rx.id + '/' + action, { method: 'POST' });
        toast('Prescription ' + updated.rxNumber + ' ' + actionLabels[action]);
        await loaders.prescriptions();
      } catch (e) {
        toast(e.message, true);
      }
    };
    if (rx.status === 'NEW' && isPharmacist) add('verify', 'Verify', simple('verify'));
    if (rx.status === 'VERIFIED') add('fill', 'Fill', simple('fill'));
    if (rx.status === 'FILLED') add('pickup', 'Pick up', function () { openPickup(rx); });
    if (rx.status === 'PICKED_UP' && rx.refillsRemaining > 0) add('refill', 'Refill', simple('refill'));
    if (rx.status === 'NEW' || rx.status === 'VERIFIED') {
      add('cancel', 'Cancel', function () { openCancel(rx); }, 'btn-danger');
    }
    return buttons;
  }

  loaders.prescriptions = async function () {
    const table = tid('rx-table');
    setState(table, 'loading');
    const filter = document.getElementById('rx-filter').value;
    const results = await Promise.all([
      api('prescriptions' + (filter ? '?status=' + filter : '')),
      api('drugs')
    ]);
    const list = results[0];
    state.drugs = results[1];
    const body = document.getElementById('rx-body');
    body.replaceChildren.apply(body, list.map(function (rx) {
      return el('tr', { 'data-testid': 'rx-row', 'data-rx-number': rx.rxNumber, 'data-rx-id': rx.id }, [
        el('td', { 'data-col': 'rx', class: 'mono', text: rx.rxNumber }),
        el('td', { 'data-col': 'patient', text: rx.patientName }),
        el('td', { 'data-col': 'drug', text: rx.drugName }),
        el('td', { 'data-col': 'qty', class: 'num', text: rx.quantity }),
        el('td', { 'data-col': 'refills', class: 'num', text: rx.refillsRemaining }),
        el('td', { 'data-col': 'status' }, [
          el('span', { class: 'badge ' + rx.status, 'data-testid': 'rx-status', text: rx.status })
        ]),
        el('td', { class: 'actions' }, rxActions(rx))
      ]);
    }));
    tid('rx-empty').classList.toggle('hidden', list.length > 0);
    setState(table, 'ready');
  };

  document.getElementById('rx-filter').addEventListener('change', function () { loaders.prescriptions(); });

  document.getElementById('new-rx').addEventListener('click', async function () {
    try {
      await loadReferenceData();
    } catch (e) {
      toast(e.message, true);
      return;
    }
    const patientSelect = document.getElementById('rx-patient');
    patientSelect.replaceChildren(el('option', { value: '', text: 'Select patient' }));
    state.patients.forEach(function (p) {
      patientSelect.append(el('option', {
        value: p.id, text: p.lastName + ', ' + p.firstName + ' (' + p.dateOfBirth + ')'
      }));
    });
    const drugSelect = document.getElementById('rx-drug');
    drugSelect.replaceChildren(el('option', { value: '', text: 'Select drug' }));
    state.drugs.forEach(function (d) {
      drugSelect.append(el('option', {
        value: d.ndc, text: d.name + ' ' + d.strength + ' ' + d.dosageForm + (d.schedule === 2 ? ' (C-II)' : '')
      }));
    });
    rxForm.reset();
    rxError.classList.add('hidden');
    rxDialog.showModal();
  });
  document.getElementById('rx-cancel').addEventListener('click', function () { rxDialog.close(); });

  function intOrNull(id) {
    const raw = document.getElementById(id).value.trim();
    return raw === '' ? null : Number(raw);
  }

  rxForm.addEventListener('submit', async function (event) {
    event.preventDefault();
    const payload = {
      patientId: intOrNull('rx-patient'),
      ndc: document.getElementById('rx-drug').value,
      quantity: intOrNull('rx-qty'),
      daysSupply: intOrNull('rx-days'),
      refills: intOrNull('rx-refills'),
      prescriber: document.getElementById('rx-prescriber').value.trim(),
      sig: document.getElementById('rx-sig').value.trim()
    };
    try {
      const created = await api('prescriptions', { method: 'POST', body: payload });
      rxDialog.close();
      toast('Prescription ' + created.rxNumber + ' created');
      await loaders.prescriptions();
    } catch (e) {
      showFormError(rxError, e);
    }
  });

  function openPickup(rx) {
    activeRx = rx;
    const drug = state.drugs.find(function (d) { return d.ndc === rx.ndc; });
    document.getElementById('pickup-rx').textContent = rx.rxNumber;
    document.getElementById('pickup-amount').textContent = drug ? money(drug.unitPrice * rx.quantity) : '-';
    pickupDialog.showModal();
  }
  document.getElementById('pickup-close').addEventListener('click', function () { pickupDialog.close(); });
  document.getElementById('pickup-form').addEventListener('submit', async function (event) {
    event.preventDefault();
    try {
      const updated = await api('prescriptions/' + activeRx.id + '/pickup', {
        method: 'POST', body: { paymentMethod: document.getElementById('pickup-payment').value }
      });
      pickupDialog.close();
      toast('Prescription ' + updated.rxNumber + ' picked up');
      await loaders.prescriptions();
    } catch (e) {
      pickupDialog.close();
      toast(e.message, true);
    }
  });

  function openCancel(rx) {
    activeRx = rx;
    document.getElementById('cancel-rx').textContent = rx.rxNumber;
    document.getElementById('cancel-reason').value = '';
    cancelDialog.showModal();
  }
  document.getElementById('cancel-close').addEventListener('click', function () { cancelDialog.close(); });
  document.getElementById('cancel-form').addEventListener('submit', async function (event) {
    event.preventDefault();
    try {
      const updated = await api('prescriptions/' + activeRx.id + '/cancel', {
        method: 'POST', body: { reason: document.getElementById('cancel-reason').value.trim() }
      });
      cancelDialog.close();
      toast('Prescription ' + updated.rxNumber + ' cancelled');
      await loaders.prescriptions();
    } catch (e) {
      toast(e.message, true);
    }
  });

  // ------------------------------------------------------------------ inventory
  loaders.inventory = async function () {
    const table = tid('inventory-table');
    setState(table, 'loading');
    state.drugs = await api('drugs');
    const lowOnly = document.getElementById('low-only').checked;
    const rows = state.drugs.filter(function (d) { return !lowOnly || d.lowStock; });
    const body = document.getElementById('inventory-body');
    body.replaceChildren.apply(body, rows.map(function (d) {
      const nameCell = el('td', { 'data-col': 'name' }, [d.name + ' ' + d.strength + ' ']);
      if (d.schedule === 2) nameCell.append(el('span', { class: 'badge cii', text: 'C-II' }));
      return el('tr', { 'data-testid': 'inventory-row', 'data-ndc': d.ndc }, [
        nameCell,
        el('td', { 'data-col': 'ndc', class: 'mono', text: d.ndc }),
        el('td', { 'data-col': 'class', text: d.drugClass }),
        el('td', { 'data-col': 'price', class: 'num', text: money(d.unitPrice) }),
        el('td', { 'data-col': 'qty', class: 'num', text: d.qtyOnHand }),
        el('td', { 'data-col': 'reorder', class: 'num', text: d.reorderLevel }),
        el('td', { 'data-col': 'status' }, [d.lowStock
          ? el('span', { class: 'badge low', 'data-testid': 'low-stock-badge', text: 'LOW STOCK' })
          : el('span', { class: 'badge ok', text: 'OK' })])
      ]);
    }));

    const select = document.getElementById('receive-drug');
    const current = select.value;
    select.replaceChildren.apply(select, state.drugs.map(function (d) {
      return el('option', { value: d.ndc, text: d.name + ' ' + d.strength });
    }));
    if (current) select.value = current;
    setState(table, 'ready');
  };

  document.getElementById('low-only').addEventListener('change', function () { loaders.inventory(); });

  document.getElementById('receive-form').addEventListener('submit', async function (event) {
    event.preventDefault();
    const ndc = document.getElementById('receive-drug').value;
    const qty = Number(document.getElementById('receive-qty').value.trim());
    try {
      const drug = await api('inventory/' + encodeURIComponent(ndc) + '/adjustments', {
        method: 'POST',
        body: {
          quantityChange: Number.isFinite(qty) ? qty : null,
          type: 'RECEIVE',
          reason: document.getElementById('receive-reason').value.trim() || 'Wholesaler delivery'
        }
      });
      toast('Received ' + qty + ' units of ' + drug.name + '. On hand: ' + drug.qtyOnHand);
      document.getElementById('receive-qty').value = '';
      document.getElementById('receive-reason').value = '';
      await loaders.inventory();
    } catch (e) {
      toast(e.message, true);
    }
  });

  route();
})();
