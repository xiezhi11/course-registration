import { createStore } from 'vuex'
import api from '../utils/api'

export default createStore({
  state: {
    token: localStorage.getItem('token') || '',
    currentUser: JSON.parse(localStorage.getItem('currentUser') || 'null')
  },
  mutations: {
    SET_TOKEN(state, token) {
      state.token = token
      localStorage.setItem('token', token)
    },
    SET_CURRENT_USER(state, user) {
      state.currentUser = user
      localStorage.setItem('currentUser', JSON.stringify(user))
    },
    CLEAR_AUTH(state) {
      state.token = ''
      state.currentUser = null
      localStorage.removeItem('token')
      localStorage.removeItem('currentUser')
    }
  },
  actions: {
    async login({ commit }, credentials) {
      const response = await api.post('/auth/login', credentials)
      if (response.code === 200) {
        commit('SET_TOKEN', response.data.token)
        commit('SET_CURRENT_USER', response.data.user)
      }
      return response
    },
    logout({ commit }) {
      commit('CLEAR_AUTH')
    }
  },
  getters: {
    isLoggedIn: state => !!state.token,
    currentUser: state => state.currentUser,
    isAdmin: state => state.currentUser?.role === 'ADMIN',
    userId: state => state.currentUser?.id
  }
})
