import { api, unwrap } from './client'

export const authApi = {
  login: (email, password) =>
    api.post('/auth/login', { email, password }).then(unwrap),

  register: (data) =>
    api.post('/auth/register', data).then(unwrap),

  me: () =>
    api.get('/auth/me').then(unwrap),

  forgotPassword: (email) =>
    api.post('/auth/forgot-password', { email }).then(unwrap),

  // BE: GET /auth/reset-password/verify?token=xxx  (NOT POST)
  verifyResetToken: (token) =>
    api.get('/auth/reset-password/verify', { params: { token } }).then(unwrap),

  // BE expects { token, newPassword, confirmPassword }
  resetPassword: (token, newPassword) =>
    api.post('/auth/reset-password', {
      token,
      newPassword,
      confirmPassword: newPassword,
    }).then(unwrap),
}
