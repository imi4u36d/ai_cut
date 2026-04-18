ARG NODE_IMAGE=node:20-bookworm
FROM ${NODE_IMAGE}

WORKDIR /app/apps/admin

COPY apps/admin/.npmrc /app/apps/admin/.npmrc
COPY apps/admin/package.json /app/apps/admin/package.json
COPY apps/admin/package-lock.json /app/apps/admin/package-lock.json

RUN npm ci --registry=https://registry.npmjs.org

COPY apps/admin /app/apps/admin

CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0", "--port", "5174"]
