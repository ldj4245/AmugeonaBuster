export async function api<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const response = await fetch(`/api${path}`, {
    ...options,
    credentials: "same-origin",
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...options.headers,
    },
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(
      body?.message ||
        (response.status === 401
          ? "카카오 연결이 필요합니다."
          : "요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요."),
    );
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
export const post = <T>(path: string, body: unknown) =>
  api<T>(path, { method: "POST", body: JSON.stringify(body) });
export const errorText = (error: unknown) =>
  error instanceof Error ? error.message : "잠시 후 다시 시도해 주세요.";
export const cafeterias = [
  {
    name: "삼성 DSR 타워 웰스토리",
    cotNo: "WEL_DSR",
    hallNo: "HALL_01",
    short: "DSR 타워",
  },
  {
    name: "삼성전자 수원디지털시티 R5",
    cotNo: "WEL_SUWON",
    hallNo: "HALL_02",
    short: "수원 R5",
  },
  {
    name: "삼성전자 기흥캠퍼스 MR1",
    cotNo: "WEL_GIHEUNG",
    hallNo: "HALL_03",
    short: "기흥 MR1",
  },
  {
    name: "삼성전자 화성캠퍼스 D1",
    cotNo: "WEL_HWASEONG",
    hallNo: "HALL_04",
    short: "화성 D1",
  },
  {
    name: "삼성전자 서초사옥 웰스토리",
    cotNo: "WEL_SEOCHO",
    hallNo: "HALL_05",
    short: "서초사옥",
  },
  {
    name: "삼성웰스토리 본사 식당",
    cotNo: "WEL_HQ",
    hallNo: "HALL_06",
    short: "웰스토리 본사",
  },
];
export const readLocal = <T>(key: string, fallback: T): T => {
  try {
    const value = localStorage.getItem(key);
    return value ? (JSON.parse(value) as T) : fallback;
  } catch {
    return fallback;
  }
};
export const saveLocal = (key: string, value: unknown) => {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    /* Preferences are optional. */
  }
};
