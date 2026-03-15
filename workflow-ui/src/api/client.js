const BASE = 'http://localhost:8080';

function isNetworkError(err) {
  return err?.message === 'Failed to fetch' || err?.name === 'TypeError';
}

async function request(path, options = {}) {
  const url = `${BASE}${path}`;
  let res;
  try {
    res = await fetch(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
    });
  } catch (err) {
    if (isNetworkError(err)) {
      throw new Error(
        `Cannot connect to the server. Start the backend at ${BASE} (e.g. run workflow-engine with Spring Boot).`
      );
    }
    throw err;
  }
  if (res.status === 204) return null;
  const text = await res.text();
  if (!text) return null;
  let data;
  try {
    data = JSON.parse(text);
  } catch {
    throw new Error(res.statusText || 'Invalid response');
  }
  if (!res.ok) throw new Error(data.message || data.error || res.statusText);
  return data;
}

export const api = {
  workflows: {
    list: (search, page = 0, size = 20) =>
      request(`/workflows?page=${page}&size=${size}${search ? `&search=${encodeURIComponent(search)}` : ''}`),
    get: (id) => request(`/workflows/${id}`),
    getDetail: (id) => request(`/workflows/${id}/detail`),
    create: (body) => request('/workflows', { method: 'POST', body: JSON.stringify(body) }),
    update: (id, body) => request(`/workflows/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    delete: (id) => request(`/workflows/${id}`, { method: 'DELETE' }),
  },
  steps: {
    list: (workflowId) => request(`/workflows/${workflowId}/steps`),
    add: (workflowId, body) =>
      request(`/workflows/${workflowId}/steps`, { method: 'POST', body: JSON.stringify(body) }),
    update: (id, body) => request(`/steps/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    delete: (id) => request(`/steps/${id}`, { method: 'DELETE' }),
  },
  rules: {
    list: (stepId) => request(`/steps/${stepId}/rules`),
    add: (stepId, body) =>
      request(`/steps/${stepId}/rules`, { method: 'POST', body: JSON.stringify(body) }),
    update: (id, body) => request(`/rules/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    delete: (id) => request(`/rules/${id}`, { method: 'DELETE' }),
  },
  executions: {
    list: (page = 0, size = 20) => request(`/executions?page=${page}&size=${size}`),
    get: (id) => request(`/executions/${id}`),
    start: (workflowId, body) =>
      request(`/workflows/${workflowId}/execute`, { method: 'POST', body: JSON.stringify(body) }),
    cancel: (id) => request(`/executions/${id}/cancel`, { method: 'POST' }),
    retry: (id) => request(`/executions/${id}/retry`, { method: 'POST' }),
  },
};
