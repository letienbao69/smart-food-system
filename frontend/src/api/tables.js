import { api, unwrap } from './client'

export const tablesApi = {
  list: () => api.get('/tables').then(unwrap),
  available: (minCapacity) =>
    api.get('/tables/available', { params: minCapacity ? { minCapacity } : {} }).then(unwrap),
  get: (id) => api.get(`/tables/${id}`).then(unwrap),
  // admin
  create: (data) => api.post('/tables', data).then(unwrap),
  update: (id, data) => api.put(`/tables/${id}`, data).then(unwrap),
  delete: (id) => api.delete(`/tables/${id}`).then(unwrap),
}
