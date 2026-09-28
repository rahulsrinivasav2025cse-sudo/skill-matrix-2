'use strict';
const LEVELS = ['', 'Beginner', 'Basic', 'Intermediate', 'Advanced', 'Expert'];
const state = { employees: [], skills: [], projects: [], allocations: [] };
const $ = (id) => document.getElementById(id);
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

async function api(method, url, body) {
  const res = await fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  if (res.status === 204) return null;
  let data = null;
  try { data = await res.json(); } catch (e) { /* no body */ }
  if (!res.ok) {
    const fields = data && data.fieldErrors ? Object.values(data.fieldErrors) : [];
    const detail = fields.length ? ' ' + fields.join('; ') : '';
    throw new Error(((data && data.message) || 'Request failed (' + res.status + ').') + detail);
  }
  return data;
}

let toastTimer;
function toast(msg, isError) {
  const t = $('toast');
  t.textContent = msg;
  t.className = 'show' + (isError ? ' error' : '');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { t.className = ''; }, isError ? 6000 : 2800);
}

async function run(action, okMsg) {
  try {
    const r = await action();
    if (okMsg) toast(okMsg);
    return r;
  } catch (e) {
    toast(e.message, true);
    return undefined;
  }
}

const chip = (name, level, removeCall) =>
  '<span class="chip">' + esc(name) + ' <b title="' + LEVELS[level] + '">' + level + '</b>' +
  (removeCall ? '<button title="Remove" aria-label="Remove ' + esc(name) + '" onclick="' + removeCall + '">&times;</button>' : '') + '</span>';

const options = (list, label) => list.map((x) => '<option value="' + x.id + '">' + esc(label(x)) + '</option>').join('');
const levelOptions = () => [1, 2, 3, 4, 5].map((n) => '<option value="' + n + '">' + n + ' - ' + LEVELS[n] + '</option>').join('');
const emptyMsg = (text) => '<div class="empty">' + text + '</div>';

/* ---------- loading ---------- */
async function loadAll() {
  try {
    const [employees, skills, projects, allocations] = await Promise.all([
      api('GET', '/api/employees'), api('GET', '/api/skills'),
      api('GET', '/api/projects'), api('GET', '/api/allocations')
    ]);
    Object.assign(state, { employees, skills, projects, allocations });
    render();
  } catch (e) {
    toast('Could not load data: ' + e.message, true);
  }
}

function keepSelect(id, html) {
  const el = $(id), prev = el.value;
  el.innerHTML = html;
  if (prev && [...el.options].some((o) => o.value === prev)) el.value = prev;
}

function render() {
  renderEmployees(); renderSkills(); renderProjects(); renderAllocations();
  const emp = options(state.employees, (e) => e.name);
  const proj = options(state.projects, (p) => p.name);
  const skill = options(state.skills, (s) => s.name);
  ['esEmp', 'alEmp'].forEach((id) => keepSelect(id, emp));
  ['psProj', 'alProj', 'recProj'].forEach((id) => keepSelect(id, proj));
  ['esSkill', 'psSkill'].forEach((id) => keepSelect(id, skill));
  keepSelect('covProj', proj);
  ['esProf', 'psProf'].forEach((id) => { if (!$(id).options.length) $(id).innerHTML = levelOptions(); });
  if (!$('covProf').options.length) $('covProf').innerHTML = levelOptions();
  const checked = new Set([...document.querySelectorAll('#covSkills input:checked')].map((c) => c.value));
  $('covSkills').innerHTML = state.skills.length
    ? state.skills.map((s) => '<label><input type="checkbox" value="' + s.id + '"' + (checked.has(String(s.id)) ? ' checked' : '') + '>' + esc(s.name) + '</label>').join('')
    : emptyMsg('Add skills first.');
}

/* ---------- employees ---------- */
function renderEmployees() {
  if (!state.employees.length) { $('empTable').innerHTML = emptyMsg('No employees yet. Add the first one above.'); return; }
  $('empTable').innerHTML = '<table><thead><tr><th>Name</th><th>Email</th><th>Capacity used</th><th>Skills (level 1-5)</th><th></th></tr></thead><tbody>' +
    state.employees.map((e) => {
      const pct = e.availableCapacity ? Math.round((e.allocatedCapacity / e.availableCapacity) * 100) : 0;
      return '<tr><td>' + esc(e.name) + '</td><td>' + esc(e.email) + '</td>' +
        '<td><span class="bar"><i style="width:' + pct + '%"></i></span>' + e.allocatedCapacity + '/' + e.availableCapacity + '%</td>' +
        '<td>' + (e.skills.length ? e.skills.map((s) => chip(s.skillName, s.proficiency, 'removeEmpSkill(' + e.id + ',' + s.skillId + ')')).join('') : '<span class="empty">No skills</span>') + '</td>' +
        '<td class="actions"><button class="btn small ghost" onclick="editEmp(' + e.id + ')">Edit</button>' +
        '<button class="btn small danger" onclick="delEmp(' + e.id + ')">Delete</button></td></tr>';
    }).join('') + '</tbody></table>';
}

function resetEmpForm() {
  $('empForm').reset(); $('empId').value = ''; $('empCap').value = 100;
  $('empFormTitle').textContent = 'Add employee'; $('empSave').textContent = 'Add employee'; $('empCancel').hidden = true;
}
window.editEmp = (id) => {
  const e = state.employees.find((x) => x.id === id);
  $('empId').value = e.id; $('empName').value = e.name; $('empEmail').value = e.email; $('empCap').value = e.availableCapacity;
  $('empFormTitle').textContent = 'Edit employee'; $('empSave').textContent = 'Save changes'; $('empCancel').hidden = false;
  window.scrollTo({ top: 0, behavior: 'smooth' });
};
window.delEmp = async (id) => {
  if (!confirm('Delete this employee and their allocations?')) return;
  if (await run(() => api('DELETE', '/api/employees/' + id), 'Employee deleted') !== undefined) loadAll();
};
window.removeEmpSkill = async (eid, sid) => {
  if (await run(() => api('DELETE', '/api/employees/' + eid + '/skills/' + sid), 'Skill removed') !== undefined) loadAll();
};
$('empCancel').onclick = resetEmpForm;
$('empForm').onsubmit = async (ev) => {
  ev.preventDefault();
  const id = $('empId').value;
  const body = { name: $('empName').value, email: $('empEmail').value, availableCapacity: Number($('empCap').value) };
  const r = await run(() => id ? api('PUT', '/api/employees/' + id, body) : api('POST', '/api/employees', body),
    id ? 'Employee updated' : 'Employee added');
  if (r !== undefined) { resetEmpForm(); loadAll(); }
};
$('esSave').onclick = async () => {
  if (!$('esEmp').value || !$('esSkill').value) { toast('Add an employee and a skill first.', true); return; }
  const r = await run(() => api('PUT', '/api/employees/' + $('esEmp').value + '/skills',
    { skillId: Number($('esSkill').value), proficiency: Number($('esProf').value) }), 'Skill level saved');
  if (r !== undefined) loadAll();
};

/* ---------- skills ---------- */
function renderSkills() {
  if (!state.skills.length) { $('skillTable').innerHTML = emptyMsg('No skills yet. Add the first one above.'); return; }
  $('skillTable').innerHTML = '<table><thead><tr><th>Name</th><th>Description</th><th></th></tr></thead><tbody>' +
    state.skills.map((s) => '<tr><td>' + esc(s.name) + '</td><td>' + esc(s.description) + '</td>' +
      '<td class="actions"><button class="btn small ghost" onclick="editSkill(' + s.id + ')">Edit</button>' +
      '<button class="btn small danger" onclick="delSkill(' + s.id + ')">Delete</button></td></tr>').join('') + '</tbody></table>';
}
function resetSkillForm() {
  $('skillForm').reset(); $('skillId').value = '';
  $('skillFormTitle').textContent = 'Add skill'; $('skillSave').textContent = 'Add skill'; $('skillCancel').hidden = true;
}
window.editSkill = (id) => {
  const s = state.skills.find((x) => x.id === id);
  $('skillId').value = s.id; $('skillName').value = s.name; $('skillDesc').value = s.description || '';
  $('skillFormTitle').textContent = 'Edit skill'; $('skillSave').textContent = 'Save changes'; $('skillCancel').hidden = false;
  window.scrollTo({ top: 0, behavior: 'smooth' });
};
window.delSkill = async (id) => {
  if (!confirm('Delete this skill?')) return;
  if (await run(() => api('DELETE', '/api/skills/' + id), 'Skill deleted') !== undefined) loadAll();
};
$('skillCancel').onclick = resetSkillForm;
$('skillForm').onsubmit = async (ev) => {
  ev.preventDefault();
  const id = $('skillId').value;
  const body = { name: $('skillName').value, description: $('skillDesc').value };
  const r = await run(() => id ? api('PUT', '/api/skills/' + id, body) : api('POST', '/api/skills', body),
    id ? 'Skill updated' : 'Skill added');
  if (r !== undefined) { resetSkillForm(); loadAll(); }
};

/* ---------- projects ---------- */
function renderProjects() {
  if (!state.projects.length) { $('projTable').innerHTML = emptyMsg('No projects yet. Add the first one above.'); return; }
  $('projTable').innerHTML = '<table><thead><tr><th>Name</th><th>Description</th><th>Required skills (minimum level)</th><th></th></tr></thead><tbody>' +
    state.projects.map((p) => '<tr><td>' + esc(p.name) + '</td><td>' + esc(p.description) + '</td>' +
      '<td>' + (p.requiredSkills.length ? p.requiredSkills.map((s) => chip(s.skillName, s.proficiency, 'removeProjSkill(' + p.id + ',' + s.skillId + ')')).join('') : '<span class="empty">None yet</span>') + '</td>' +
      '<td class="actions"><button class="btn small ghost" onclick="editProj(' + p.id + ')">Edit</button>' +
      '<button class="btn small danger" onclick="delProj(' + p.id + ')">Delete</button></td></tr>').join('') + '</tbody></table>';
}
function resetProjForm() {
  $('projForm').reset(); $('projId').value = '';
  $('projFormTitle').textContent = 'Add project'; $('projSave').textContent = 'Add project'; $('projCancel').hidden = true;
}
window.editProj = (id) => {
  const p = state.projects.find((x) => x.id === id);
  $('projId').value = p.id; $('projName').value = p.name; $('projDesc').value = p.description || '';
  $('projFormTitle').textContent = 'Edit project'; $('projSave').textContent = 'Save changes'; $('projCancel').hidden = false;
  window.scrollTo({ top: 0, behavior: 'smooth' });
};
window.delProj = async (id) => {
  if (!confirm('Delete this project and its allocations?')) return;
  if (await run(() => api('DELETE', '/api/projects/' + id), 'Project deleted') !== undefined) loadAll();
};
window.removeProjSkill = async (pid, sid) => {
  if (await run(() => api('DELETE', '/api/projects/' + pid + '/skills/' + sid), 'Requirement removed') !== undefined) loadAll();
};
$('projCancel').onclick = resetProjForm;
$('projForm').onsubmit = async (ev) => {
  ev.preventDefault();
  const id = $('projId').value;
  const body = { name: $('projName').value, description: $('projDesc').value };
  const r = await run(() => id ? api('PUT', '/api/projects/' + id, body) : api('POST', '/api/projects', body),
    id ? 'Project updated' : 'Project added');
  if (r !== undefined) { resetProjForm(); loadAll(); }
};
$('psSave').onclick = async () => {
  if (!$('psProj').value || !$('psSkill').value) { toast('Add a project and a skill first.', true); return; }
  const r = await run(() => api('PUT', '/api/projects/' + $('psProj').value + '/skills',
    { skillId: Number($('psSkill').value), proficiency: Number($('psProf').value) }), 'Requirement saved');
  if (r !== undefined) loadAll();
};

/* ---------- allocations ---------- */
function renderAllocations() {
  if (!state.allocations.length) { $('alTable').innerHTML = emptyMsg('No allocations yet. Assign an employee above.'); return; }
  $('alTable').innerHTML = '<table><thead><tr><th>Employee</th><th>Project</th><th>Allocation</th><th>Status</th><th></th></tr></thead><tbody>' +
    state.allocations.map((a) => '<tr><td>' + esc(a.employeeName) + '</td><td>' + esc(a.projectName) + '</td><td>' + a.percentage + '%</td>' +
      '<td><span class="tag ' + (a.status === 'FULLY_ALLOCATED' ? 'ok' : 'warn') + '">' + (a.status === 'FULLY_ALLOCATED' ? 'Fully allocated' : 'Partially allocated') + '</span></td>' +
      '<td class="actions"><button class="btn small ghost" onclick="editAlloc(' + a.id + ',' + a.percentage + ')">Change %</button>' +
      '<button class="btn small danger" onclick="delAlloc(' + a.id + ')">Remove</button></td></tr>').join('') + '</tbody></table>';
}
window.editAlloc = async (id, current) => {
  const v = prompt('New allocation percentage (1-100):', current);
  if (v === null) return;
  const n = Number(v);
  if (!Number.isInteger(n)) { toast('Enter a whole number between 1 and 100.', true); return; }
  if (await run(() => api('PUT', '/api/allocations/' + id, { percentage: n }), 'Allocation updated') !== undefined) loadAll();
};
window.delAlloc = async (id) => {
  if (!confirm('Remove this allocation?')) return;
  if (await run(() => api('DELETE', '/api/allocations/' + id), 'Allocation removed') !== undefined) loadAll();
};
$('alSave').onclick = async () => {
  if (!$('alEmp').value || !$('alProj').value) { toast('Add an employee and a project first.', true); return; }
  const r = await run(() => api('POST', '/api/allocations', {
    employeeId: Number($('alEmp').value), projectId: Number($('alProj').value), percentage: Number($('alPct').value)
  }), 'Employee assigned');
  if (r !== undefined) loadAll();
};

/* ---------- recommendations ---------- */
async function showRecommendations() {
  const pid = $('recProj').value;
  if (!pid) { toast('Choose a project first.', true); return; }
  const list = await run(() => api('GET', '/api/projects/' + pid + '/recommendations'));
  if (list === undefined) return;
  if (!list.length) { $('recTable').innerHTML = emptyMsg('No available employees match this project\'s skills.'); return; }
  $('recTable').innerHTML = '<table><thead><tr><th>Rank</th><th>Employee</th><th>Match</th><th>Matched skills</th><th>Missing</th><th>Free capacity</th><th></th></tr></thead><tbody>' +
    list.map((r) => '<tr><td>' + r.rank + '</td><td>' + esc(r.employeeName) + '</td>' +
      '<td>' + r.matchedSkills + '/' + r.totalRequiredSkills + '<br><small>score ' + r.proficiencyScore + '</small></td>' +
      '<td>' + r.matchedDetails.map((s) => chip(s.skillName, s.proficiency)).join('') + '</td>' +
      '<td>' + (r.missingSkills.length ? esc(r.missingSkills.join(', ')) : '-') + '</td>' +
      '<td>' + r.freeCapacity + '%</td>' +
      '<td class="actions"><button class="btn small" onclick="assignFromRec(' + r.employeeId + ',' + pid + ',' + r.freeCapacity + ')">Assign</button></td></tr>').join('') + '</tbody></table>';
}
window.assignFromRec = async (eid, pid, free) => {
  const v = prompt('Allocation percentage (1-' + Math.min(free, 100) + ' free):', Math.min(free, 100));
  if (v === null) return;
  const n = Number(v);
  if (!Number.isInteger(n)) { toast('Enter a whole number.', true); return; }
  const r = await run(() => api('POST', '/api/allocations', { employeeId: eid, projectId: pid, percentage: n }), 'Employee assigned');
  if (r !== undefined) { await loadAll(); showRecommendations(); }
};
$('recGo').onclick = showRecommendations;

/* ---------- coverage ---------- */
function showCoverage(data) {
  const cls = { COVERED: 'ok', NO_AVAILABILITY: 'warn', GAP: 'bad' };
  const txt = { COVERED: 'Covered', NO_AVAILABILITY: 'Skilled but fully booked', GAP: 'Gap' };
  $('covResult').innerHTML =
    '<p><strong>' + data.coveredSkills + ' of ' + data.totalSkills + '</strong> skills have at least one qualified employee.' +
    (data.gaps.length ? ' Gaps: <strong>' + esc(data.gaps.join(', ')) + '</strong>.' : ' No gaps.') + '</p>' +
    '<div class="table-wrap"><table><thead><tr><th>Skill</th><th>Min level</th><th>Qualified</th><th>Available</th><th>Status</th><th>People</th></tr></thead><tbody>' +
    data.entries.map((e) => '<tr><td>' + esc(e.skillName) + '</td><td>' + e.requiredProficiency + '</td><td>' + e.qualifiedCount + '</td><td>' + e.availableCount + '</td>' +
      '<td><span class="tag ' + cls[e.status] + '">' + txt[e.status] + '</span></td>' +
      '<td>' + (e.employees.map((p) => chip(p.employeeName + ' (' + p.freeCapacity + '% free)', p.proficiency)).join('') || '-') + '</td></tr>').join('') +
    '</tbody></table></div>';
}
$('covProjGo').onclick = async () => {
  if (!$('covProj').value) { toast('Choose a project first.', true); return; }
  const d = await run(() => api('GET', '/api/coverage?projectId=' + $('covProj').value));
  if (d) showCoverage(d);
};
$('covSkillsGo').onclick = async () => {
  const ids = [...document.querySelectorAll('#covSkills input:checked')].map((c) => c.value);
  if (!ids.length) { toast('Select at least one skill.', true); return; }
  const d = await run(() => api('GET', '/api/coverage?skillIds=' + ids.join(',') + '&minProficiency=' + $('covProf').value));
  if (d) showCoverage(d);
};

/* ---------- tabs ---------- */
document.querySelectorAll('#tabs button').forEach((b) => {
  b.onclick = () => {
    document.querySelectorAll('#tabs button').forEach((x) => x.classList.toggle('active', x === b));
    document.querySelectorAll('main section').forEach((s) => s.classList.toggle('active', s.id === b.dataset.tab));
    loadAll();
  };
});

loadAll();
