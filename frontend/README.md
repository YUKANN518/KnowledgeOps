# KnowledgeOps frontend

Vue 3 and TypeScript client for the KnowledgeOps operations workspace.

```bash
npm install
npm run dev
```

The development server proxies `/api` to `http://localhost:8080`. Production uses the Nginx reverse proxy defined in `nginx.conf`.

Quality checks:

```bash
npm run lint
npm run type-check
npm test
npm run build
```
