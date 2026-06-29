import { api, unwrap } from './client'

export const cartApi = {
  get: () => api.get('/cart').then(unwrap),

  addItem: (foodId, quantity = 1) =>
    api.post('/cart/items', { foodId, quantity }).then(unwrap),

  updateItem: (itemId, quantity) =>
    api.put(`/cart/items/${itemId}`, { quantity }).then(unwrap),

  removeItem: (itemId) =>
    api.delete(`/cart/items/${itemId}`).then(unwrap),

  clear: () => api.delete('/cart/clear').then(unwrap),
}

/**
 * Đơn món trong mô hình ăn tại nhà hàng. Việc TẠO đơn món diễn ra trong
 * luồng ĐẶT BÀN (đặt món trước) — xem api/reservations.js. Ở đây chỉ còn
 * các thao tác xem / xóa / cập nhật trạng thái.
 */
export const ordersApi = {
  myOrders: () => api.get('/orders/my').then(unwrap),
  myOrder: (id) => api.get(`/orders/my/${id}`).then(unwrap),
  deleteMyOrder: (id) => api.delete(`/orders/my/${id}`).then(unwrap),

  // admin
  adminList: () => api.get('/orders').then(unwrap),
  adminGet: (id) => api.get(`/orders/${id}`).then(unwrap),
  updateStatus: (id, payload) => api.put(`/orders/${id}/status`, payload).then(unwrap),
  adminDelete: (id) => api.delete(`/orders/${id}`).then(unwrap),
  addItem: (id, foodId, quantity) => api.post(`/orders/${id}/items`, { foodId, quantity }).then(unwrap),
  addItemByReservation: (reservationId, foodId, quantity) => api.post(`/orders/by-reservation/${reservationId}/items`, { foodId, quantity }).then(unwrap),
  applyVoucherByReservation: (reservationId, voucherCode) => api.post(`/orders/by-reservation/${reservationId}/voucher`, { voucherCode }).then(unwrap),
  updateItem: (id, itemId, quantity) => api.put(`/orders/${id}/items/${itemId}`, { quantity }).then(unwrap),
  removeItem: (id, itemId) => api.delete(`/orders/${id}/items/${itemId}`).then(unwrap),
}
