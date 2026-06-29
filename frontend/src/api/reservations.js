import { api, unwrap } from './client'

export const reservationsApi = {
  // khách hàng
  create: (data) => api.post('/reservations', data).then(unwrap),
  myList: () => api.get('/reservations/my').then(unwrap),
  myDetail: (id) => api.get(`/reservations/my/${id}`).then(unwrap),
  cancel: (id) => api.post(`/reservations/my/${id}/cancel`).then(unwrap),
  notifyDeposit: (id) => api.post(`/reservations/my/${id}/notify-deposit`).then(unwrap),
  // admin
  adminList: () => api.get('/reservations').then(unwrap),
  adminGet: (id) => api.get(`/reservations/${id}`).then(unwrap),
  updateStatus: (id, payload) => api.put(`/reservations/${id}/status`, payload).then(unwrap),
  adminDelete: (id) => api.delete(`/reservations/${id}`).then(unwrap),
}

// QR đặt cọc giữ bàn — tái dùng endpoint AI payment QR sẵn có
export const depositApi = {
  qr: (amount, reservationCode) =>
    api.post('/ai/payment/qr', { amount, orderCode: reservationCode }).then(unwrap),
}
