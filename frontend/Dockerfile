# syntax=docker/dockerfile:1.7

# Build stage
FROM node:22-alpine AS builder

WORKDIR /app

# Vite inlines import.meta.env.VITE_* at BUILD time, so these public values
# must be present here (not only at container runtime). On EasyPanel the
# service's environment variables are passed as build args automatically.
ARG VITE_PRAXIS_API_BASE_URL
ARG VITE_PRAXIS_DEMO_MODE
ENV VITE_PRAXIS_API_BASE_URL=${VITE_PRAXIS_API_BASE_URL}
ENV VITE_PRAXIS_DEMO_MODE=${VITE_PRAXIS_DEMO_MODE}
ENV CI=true
ENV NO_COLOR=1
ENV FORCE_COLOR=0
ENV TERM=dumb

COPY package*.json ./
RUN --mount=type=cache,target=/root/.npm \
    sh -c 'echo "[build][frontend][npm-ci][start] $(date -u +%Y-%m-%dT%H:%M:%SZ)"; pipe=/tmp/build-log.$$; mkfifo "$pipe"; while IFS= read -r line; do echo "[$(date -u +%Y-%m-%dT%H:%M:%SZ)] $line"; done < "$pipe" & logger=$!; npm ci --prefer-offline --no-audit --fund=false --progress=false --loglevel=warn > "$pipe" 2>&1; status=$?; wait "$logger"; rm -f "$pipe"; echo "[build][frontend][npm-ci][end] $(date -u +%Y-%m-%dT%H:%M:%SZ) status=$status"; exit "$status"'

COPY . .
RUN sh -c 'echo "[build][frontend][vite-build][start] $(date -u +%Y-%m-%dT%H:%M:%SZ)"; pipe=/tmp/build-log.$$; mkfifo "$pipe"; while IFS= read -r line; do echo "[$(date -u +%Y-%m-%dT%H:%M:%SZ)] $line"; done < "$pipe" & logger=$!; npm run build > "$pipe" 2>&1; status=$?; wait "$logger"; rm -f "$pipe"; echo "[build][frontend][vite-build][end] $(date -u +%Y-%m-%dT%H:%M:%SZ) status=$status"; exit "$status"'

# Runtime stage
FROM node:22-alpine

WORKDIR /app

ENV NODE_ENV=production
ENV HOST=0.0.0.0
ENV PORT=80

COPY --from=builder /app/.output ./.output

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=5 \
    CMD wget -qO- http://127.0.0.1:80/ > /dev/null 2>&1 || exit 1

CMD ["node", ".output/server/index.mjs"]
