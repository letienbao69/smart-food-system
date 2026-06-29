import { api, unwrap } from './client'

export const wishlistApi = {
  list: () => api.get('/wishlists/me').then(unwrap),
  add: (foodId) => api.post(`/wishlists/food/${foodId}`).then(unwrap),
  remove: (foodId) => api.delete(`/wishlists/food/${foodId}`).then(unwrap),
  check: (foodId) =>
    api.get(`/wishlists/food/${foodId}/check`).then((r) => r.data),
  count: () => api.get('/wishlists/count').then((r) => r.data),
}

export const reviewsApi = {
  byFood: (foodId) =>
    api.get(`/reviews/food/${foodId}`).then(unwrap),

  create: (data) => api.post('/reviews', data).then(unwrap),

  testimonials: () => api.get('/reviews/testimonials').then(unwrap),
}

export const vouchersApi = {
  validate: (code) =>
    api.get(`/vouchers/validate/${code}`).then(unwrap),
  active: () => api.get('/vouchers/active').then(unwrap),

  // admin
  list: () => api.get('/vouchers').then(unwrap),
  get: (id) => api.get(`/vouchers/${id}`).then(unwrap),
  create: (data) => api.post('/vouchers', data).then(unwrap),
  update: (id, data) => api.put(`/vouchers/${id}`, data).then(unwrap),
  delete: (id) => api.delete(`/vouchers/${id}`).then(unwrap),
}

export const uploadApi = {
  image: (file) => {
    const form = new FormData()
    form.append('file', file)
    return api
      .post('/uploads/image', form, { headers: { 'Content-Type': 'multipart/form-data' } })
      .then(unwrap)
  },
}

export const profileApi = {
  me: () => api.get('/users/me').then(unwrap),
  update: (data) => api.put('/users/me', data).then(unwrap),
  changePassword: (oldPassword, newPassword) =>
    api.put('/users/me/password', { oldPassword, newPassword }).then(unwrap),
}

export const contactsApi = {
  create: (data) => api.post('/contacts', data).then(unwrap),
  list: () => api.get('/contacts').then(unwrap),
  updateStatus: (id, status, reply) => api.put(`/contacts/${id}/status`, { status, reply }).then(unwrap),
  delete: (id) => api.delete(`/contacts/${id}`).then(unwrap),
}
