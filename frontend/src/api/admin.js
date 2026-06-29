import { api, unwrap } from './client'

// =====================================================================
// Admin: users management
// BE: /api/admin/users
// =====================================================================
export const adminUsersApi = {
  list: () => api.get('/admin/users').then(unwrap),
  get: (id) => api.get(`/admin/users/${id}`).then(unwrap),
  create: (data) => api.post('/admin/users', data).then(unwrap),
  update: (id, data) => api.put(`/admin/users/${id}`, data).then(unwrap),
  // BE: PUT /admin/users/{id}/status  body: { status }
  updateStatus: (id, status) =>
    api.put(`/admin/users/${id}/status`, { status }).then(unwrap),
  // BE: PUT /admin/users/{userId}/roles  body: { roles: [...] }
  updateRoles: (userId, roles) =>
    api.put(`/admin/users/${userId}/roles`, { roles }).then(unwrap),
  delete: (id) => api.delete(`/admin/users/${id}`).then(unwrap),
}

// BE: /api/admin/roles
export const rolesApi = {
  list: () => api.get('/admin/roles').then(unwrap),
}

// =====================================================================
// Employees
// BE: /api/employees  (note: no /admin prefix)
// DTO fields: employeeCode, fullName, gender, dateOfBirth, phone, email,
//             address, hireDate, positionId, salary, shiftName, status, note
// =====================================================================
export const employeesApi = {
  list: () => api.get('/employees').then(unwrap),
  get: (id) => api.get(`/employees/${id}`).then(unwrap),
  create: (data) => api.post('/employees', data).then(unwrap),
  update: (id, data) => api.put(`/employees/${id}`, data).then(unwrap),
  delete: (id) => api.delete(`/employees/${id}`).then(unwrap),
}

// =====================================================================
// Employee Positions
// BE: /api/employee-positions
// DTO fields: positionName (NOT "name"), description, baseSalary, status
// =====================================================================
export const positionsApi = {
  list: () => api.get('/employee-positions').then(unwrap),
  create: (data) => api.post('/employee-positions', data).then(unwrap),
  update: (id, data) => api.put(`/employee-positions/${id}`, data).then(unwrap),
  delete: (id) => api.delete(`/employee-positions/${id}`).then(unwrap),
}

// =====================================================================
// Reports
// BE: /api/admin/reports
// =====================================================================
export const reportsApi = {
  summary: (from, to) =>
    api.get('/admin/reports/summary', { params: { from, to } }).then(unwrap),
  dailyRevenue: (from, to) =>
    api.get('/admin/reports/revenue/daily', { params: { from, to } }).then(unwrap),
  payments: (from, to) =>
    api.get('/admin/reports/payments', { params: { from, to } }).then(unwrap),
  bestSelling: (from, to, limit = 10) =>
    api.get('/admin/reports/best-selling-foods', {
      params: { from, to, limit },
    }).then(unwrap),
}

// =====================================================================
// Notifications
// Customer: /api/notifications/my/...
// Admin:    /api/admin/notifications/...
// =====================================================================
export const notificationsApi = {
  // Customer
  mine: () => api.get('/notifications/my').then(unwrap),
  myUnread: () => api.get('/notifications/my/unread').then(unwrap),
  myUnreadCount: () =>
    api.get('/notifications/my/unread-count').then(unwrap)
       .then((d) => typeof d === 'number' ? d : d?.count ?? 0),
  markRead: (id) => api.put(`/notifications/my/${id}/read`).then(unwrap),
  markAllRead: () => api.put('/notifications/my/read-all').then(unwrap),
  deleteOne: (id) => api.delete(`/notifications/my/${id}`).then(unwrap),
  deleteAll: () => api.delete('/notifications/my/all').then(unwrap),

  // Admin
  adminList: () => api.get('/admin/notifications').then(unwrap),
  adminUnreadCount: () =>
    api.get('/admin/notifications/unread-count').then(unwrap)
       .then((d) => typeof d === 'number' ? d : d?.count ?? 0),
  adminMarkRead: (id) => api.put(`/admin/notifications/${id}/read`).then(unwrap),
  adminMarkAllRead: () => api.put('/admin/notifications/read-all').then(unwrap),
  adminDeleteOne: (id) => api.delete(`/admin/notifications/${id}`).then(unwrap),
  adminDeleteAll: () => api.delete('/admin/notifications/all').then(unwrap),
}

// =====================================================================
// Chatbot
// BE: /api/chatbot
// =====================================================================
export const chatbotApi = {
  send: (message) =>
    api.post('/chatbot/message', { message }).then(unwrap),
  myHistory: () =>
    api.get('/chatbot/my-history').then(unwrap),
}

// =====================================================================
// Recommendations (behaviour-based)
// BE: /api/recommendations
// =====================================================================
export const recsApi = {
  forMe: (limit = 8) =>
    api.get('/recommendations/my', { params: { limit } }).then(unwrap),
  fromCart: (limit = 8) =>
    api.get('/recommendations/cart', { params: { limit } }).then(unwrap),
}
