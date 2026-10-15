import { defineStore } from "pinia";
import { getMe } from "@/api/seller";
export const useSellerStore = defineStore("seller", {
  state: () => ({ loggedIn: false, username: "", checked: false }),
  actions: {
    setLogin(username) {
      this.loggedIn = true;
      this.username = username;
      this.checked = true;
    },
    logout() {
      this.loggedIn = false;
      this.username = "";
      this.checked = false;
    },
    async restore() {
      if (this.checked) return;
      try {
        const seller = await getMe();
        this.setLogin(seller.username);
      } catch (error) {
        if (error.response?.status !== 401) throw error;
        this.logout();
        this.checked = true;
      }
    },
  },
});
