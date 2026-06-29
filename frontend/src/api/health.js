import { api, unwrap } from './client'

/**
 * Health profile + BMI-based recommendation API.
 * Matches HealthRecommendationController exactly.
 */
export const healthApi = {
  getProfile: () => api.get('/health/profile').then(unwrap),

  updateProfile: (data) => api.put('/health/profile', data).then(unwrap),

  getAnalysis: () => api.get('/health/analysis').then(unwrap),

  getRecommendations: (limit = 10, useAi = false) =>
    api
      .get('/health/recommendations', { params: { limit, useAi } })
      .then(unwrap),
}
