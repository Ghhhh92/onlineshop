import { defineStore } from "pinia";
import { getMe } from "@/api/customer";

export const useCustomerStore = defineStore("customer", {
  state: () => ({
    loggedIn: false,
    username: "",
    phone: "",
    defaultLocation: "",
    checked: false,
  }),
  actions: {
    setLogin(customer) {
      this.loggedIn = true;
      this.username = customer.username;
      this.phone = customer.phone;
      this.defaultLocation = customer.defaultLocation;
      this.checked = true;
    },
    logout() {
      this.loggedIn = false;
      this.username = "";
      this.phone = "";
      this.defaultLocation = "";
      this.checked = false;
    },
    async restore() {
      if (this.checked) return;
      try {
        const customer = await getMe();
        this.setLogin(customer);
      } catch (error) {
        if (error.response?.status !== 401) throw error;
        this.logout();
        this.checked = true;
      }
    },
  },
});