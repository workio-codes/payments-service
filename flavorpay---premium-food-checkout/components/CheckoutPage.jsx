import React, { useMemo } from 'react';

const CheckoutPage = ({ currentOrder, onPay, isProcessing, error }) => {
  const orderItems = useMemo(
    () => [
      { name: 'Truffle Burger Duo', notes: 'Medium rare, extra cheese', amount: 24.0 },
      { name: 'Classic Lemonade', notes: '500ml, chilled', amount: 4.5 }
    ],
    []
  );

  const subtotal = useMemo(() => orderItems.reduce((sum, item) => sum + item.amount, 0), [orderItems]);
  const deliveryFee = Number((currentOrder.totalAmount - subtotal).toFixed(2));

  return (
    <div className="animate-slide-up p-6 lg:p-10">
      <div className="grid gap-8 lg:grid-cols-[1.25fr_0.75fr]">
        <section className="space-y-6">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary">Payment Details</p>
            <h2 className="mt-2 text-3xl font-bold text-slate-900">Complete your order</h2>
            <p className="mt-2 text-sm text-slate-500">
              Checkout is handled securely in Razorpay's hosted payment modal.
            </p>
          </div>

          <div className="inline-flex items-center gap-2 rounded-xl border border-blue-200 bg-blue-50 px-4 py-2">
            <span className="material-icons text-base text-blue-700">verified_user</span>
            <span className="text-sm font-semibold text-blue-800">Pay with Razorpay</span>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <p className="text-sm font-semibold text-slate-900">You will enter payment details on Razorpay</p>
            <p className="mt-1 text-sm text-slate-600">
              We only create and track the order in this app. Card, UPI, and wallet authentication are completed in
              the Razorpay popup.
            </p>
          </div>

          {error && (
            <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {error}
            </div>
          )}
        </section>

        <aside className="rounded-2xl border border-slate-200 bg-slate-50 p-6 shadow-sm">
          <h3 className="text-lg font-bold text-slate-900">Order summary</h3>
          <p className="mt-1 text-sm text-slate-500">{currentOrder.customerName}</p>

          <div className="mt-6 space-y-4">
            {orderItems.map((item) => (
              <div key={item.name} className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-sm font-semibold text-slate-800">{item.name}</p>
                  <p className="text-xs text-slate-500">{item.notes}</p>
                </div>
                <p className="text-sm font-semibold text-slate-800">${item.amount.toFixed(2)}</p>
              </div>
            ))}
          </div>

          <div className="mt-6 space-y-2 border-t border-slate-200 pt-4 text-sm">
            <div className="flex items-center justify-between text-slate-600">
              <span>Subtotal</span>
              <span>${subtotal.toFixed(2)}</span>
            </div>
            <div className="flex items-center justify-between text-slate-600">
              <span>Delivery</span>
              <span>${deliveryFee.toFixed(2)}</span>
            </div>
            <div className="mt-2 flex items-center justify-between text-base font-bold text-slate-900">
              <span>Total</span>
              <span>${currentOrder.totalAmount.toFixed(2)}</span>
            </div>
          </div>

          <button
            onClick={onPay}
            disabled={isProcessing}
            className={`mt-6 flex w-full items-center justify-center gap-2 rounded-xl px-4 py-3 text-sm font-semibold text-white transition-colors ${
              isProcessing ? 'cursor-not-allowed bg-primary/60' : 'bg-primary hover:bg-blue-700'
            }`}
          >
            {isProcessing ? (
              <>
                <svg className="h-4 w-4 animate-spin" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                Processing...
              </>
            ) : (
              <>
                <span className="material-icons text-base">account_balance_wallet</span>
                Pay with Razorpay
              </>
            )}
          </button>

          <p className="mt-3 text-center text-xs text-slate-500">PCI-compliant. Encrypted transport via gateway on port 9090.</p>
        </aside>
      </div>
    </div>
  );
};

export default CheckoutPage;
