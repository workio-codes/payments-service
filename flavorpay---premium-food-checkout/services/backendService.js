class BackendApiService {
  constructor() {
    this.baseUrl = process.env.BACKEND_API_URL || 'http://localhost:9090';
  }

  async createOrder(orderData) {
    const response = await fetch(`${this.baseUrl}/order/api/orders`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(orderData),
    });

    if (!response.ok) {
      if (response.status === 503) {
        throw new Error('Order service unavailable (503). Start eureka, gateway, and order services.');
      }
      throw new Error(`Order creation failed: ${response.status}`);
    }

    return response.json();
  }

  async createRazorpayOrder(payload) {
    const response = await fetch(`${this.baseUrl}/payments/api/payments/razorpay/order`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      if (response.status === 503) {
        throw new Error('Payment service unavailable (503). Start eureka, gateway, and payment services.');
      }
      throw new Error(`Razorpay order creation failed: ${response.status}`);
    }

    return response.json();
  }

  async verifyRazorpayPayment(payload) {
    const response = await fetch(`${this.baseUrl}/payments/api/payments/razorpay/verify`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      throw new Error(body?.message || `Razorpay verification failed: ${response.status}`);
    }

    return response.json();
  }

  async confirmOrder(orderId) {
    const response = await fetch(`${this.baseUrl}/order/api/orders/${orderId}/confirm`, {
      method: 'POST',
    });

    if (!response.ok) {
      throw new Error(`Order confirmation failed: ${response.status}`);
    }

    return response.json();
  }

  async markOrderPaymentFailed(orderId) {
    const response = await fetch(`${this.baseUrl}/order/api/orders/${orderId}/payment-failed`, {
      method: 'POST',
    });

    if (!response.ok) {
      throw new Error(`Mark payment failed request failed: ${response.status}`);
    }

    return response.json();
  }

  async getAllOrders() {
    const response = await fetch(`${this.baseUrl}/order/api/orders`);
    if (!response.ok) {
      throw new Error(`Failed to fetch orders: ${response.status}`);
    }
    return response.json();
  }

  async getOrderById(orderId) {
    const response = await fetch(`${this.baseUrl}/order/api/orders/${orderId}`);
    if (!response.ok) {
      if (response.status === 404) return null;
      throw new Error(`Failed to fetch order: ${response.status}`);
    }
    return response.json();
  }

  async cancelOrder(orderId) {
    const response = await fetch(
      `${this.baseUrl}/order/api/orders/${orderId}/cancel`,
      { method: 'POST' }
    );
    if (!response.ok) {
      throw new Error(`Order cancellation failed: ${response.status}`);
    }
    return response.json();
  }
}

export const backendService = new BackendApiService();
