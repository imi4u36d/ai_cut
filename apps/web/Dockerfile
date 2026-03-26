ARG NODE_IMAGE=docker.m.daocloud.io/library/node:20-bookworm
FROM ${NODE_IMAGE}

WORKDIR /app/apps/web

COPY apps/web/package.json /app/apps/web/package.json
COPY apps/web/package-lock.json /app/apps/web/package-lock.json

RUN npm install --registry=https://registry.npmjs.org

COPY apps/web /app/apps/web

CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0", "--port", "5173"]
