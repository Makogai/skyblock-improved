# Admin Panel Setup

The admin panel (backend + frontend) lives in **separate repos**. This mod repo does not include them. To develop or run the admin panel alongside the mod:

## 1. Clone the repos into `admin/`

```bash
# From the skyblock-improved root:
mkdir -p admin
git clone <your-backend-repo-url> admin/backend
git clone <your-frontend-repo-url> admin/frontend
```

## 2. Run backend & frontend

```bash
cd admin/backend
cp .env.example .env
# Edit .env: MySQL, JWT_SECRET, ABLY_API_KEY
npm install && npm run seed && npm run start:dev

# In another terminal:
cd admin/frontend
npm install && npm run dev
```

## 3. Structure

The `admin/` folder is **gitignored** in this repo so it stays out of version control. Your backend and frontend repos are the source of truth. Place them in `admin/backend` and `admin/frontend` to keep the paths consistent (vite proxy, mod API URL, etc.).
