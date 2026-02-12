# Payment Gateway (Spring Boot + React + Kafka)

Microservices checkout demo with:
- `eureka` (service discovery)
- `gateway` (API gateway on `9090`)
- `order` (order lifecycle)
- `payment` (Razorpay + payment verification)
- `frontend` (React checkout UI)
- `mysql` + `kafka` + `zookeeper`

## Run Entire Stack with Docker

1. Clone

```bash
git clone https://gitlab.com/day08-group/payment-gateway.git
cd payment-gateway
```

2. Optional: set Razorpay test keys

```bash
export RAZORPAY_KEY_ID=rzp_test_xxxxx
export RAZORPAY_KEY_SECRET=xxxxx
```

3. Build and start everything

```bash
docker compose up -d --build
```

Notes:
- All services (`eureka`, `gateway`, `order`, `payment`, `frontend`, `mysql`, `kafka`, `zookeeper`) run inside Docker.
- MySQL is intentionally not published to host port `3306`; services connect to `mysql:3306` over the Docker network.

4. Open app

- Frontend: `http://localhost:3000`
- Gateway: `http://localhost:9090`
- Eureka: `http://localhost:8761`

5. Check containers

```bash
docker compose ps
```

6. View logs (example)

```bash
docker compose logs -f payment order gateway
```

7. Stop stack

```bash
docker compose down
```

8. Stop and remove DB volume too

```bash
docker compose down -v
```

## Kafka Flow

1. UI creates order (`Pending`) via `order`.
2. UI verifies Razorpay payment via `payment`.
3. `payment` publishes to Kafka topic `payment-status`.
4. `order` consumes and updates status:
   - `SUCCESS` -> `Confirmed`
   - `FAILED` -> `Payment Failed`
