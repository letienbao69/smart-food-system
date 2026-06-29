import { api, unwrap } from './client'

export const foodsApi = {
  list: (params = {}) =>
    api.get('/foods', { params }).then(unwrap),

  get: (id) =>
    api.get(`/foods/${id}`).then(unwrap),

  featured: () =>
    api.get('/foods/featured').then(unwrap),

  create: (data) =>
    api.post('/foods', data).then(unwrap),

  update: (id, data) =>
    api.put(`/foods/${id}`, data).then(unwrap),

  delete: (id) =>
    api.delete(`/foods/${id}`).then(unwrap),
}

export const categoriesApi = {
  list: () =>
    api.get('/categories').then(unwrap),

  get: (id) =>
    api.get(`/categories/${id}`).then(unwrap),

  create: (data) =>
    api.post('/categories', data).then(unwrap),

  update: (id, data) =>
    api.put(`/categories/${id}`, data).then(unwrap),

  delete: (id) =>
    api.delete(`/categories/${id}`).then(unwrap),
}
