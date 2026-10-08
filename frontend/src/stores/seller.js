import { defineStore } from 'pinia'

export const useSellerStore = defineStore('seller', {
  state: () => ({
    loggedIn: false,
    username: '',
  }),
  actions: {
    setLogin(username) {
      this.loggedIn = true
      this.username = username
    },
    logout() {
      this.loggedIn = false
      this.username = ''
    },
  },
})
