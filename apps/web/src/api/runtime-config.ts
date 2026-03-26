export interface RuntimeConfig {
  apiBaseUrl: string;
  storageBaseUrl: string;
}

const defaultRuntimeConfig: RuntimeConfig = {
  apiBaseUrl: "/api/v1",
  storageBaseUrl: "/storage"
};

let runtimeConfig: RuntimeConfig = { ...defaultRuntimeConfig };

function isNonEmptyString(value: unknown): value is string {
  return typeof value === "string" && value.trim().length > 0;
}

function normalizeRuntimeConfig(config: Partial<RuntimeConfig>): RuntimeConfig {
  return {
    apiBaseUrl: isNonEmptyString(config.apiBaseUrl) ? config.apiBaseUrl : defaultRuntimeConfig.apiBaseUrl,
    storageBaseUrl: isNonEmptyString(config.storageBaseUrl) ? config.storageBaseUrl : defaultRuntimeConfig.storageBaseUrl
  };
}

export async function loadRuntimeConfig() {
  try {
    const response = await fetch(new URL("runtime-config.json", document.baseURI).toString(), {
      cache: "no-store"
    });
    if (!response.ok) {
      return runtimeConfig;
    }
    const parsed = (await response.json()) as Partial<RuntimeConfig>;
    runtimeConfig = normalizeRuntimeConfig(parsed);
  } catch {
    return runtimeConfig;
  }
  return runtimeConfig;
}

export function getRuntimeConfig() {
  return runtimeConfig;
}
