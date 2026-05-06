export const TOKEN_KEY = 'shortlink.console.token';

const ADMIN_BASE = import.meta.env.VITE_ADMIN_API_BASE || '/admin-api';
const PROJECT_BASE = import.meta.env.VITE_PROJECT_API_BASE || '/project-api';

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE';

interface RequestOptions {
  method?: HttpMethod;
  body?: unknown;
  query?: Record<string, string | number | boolean | null | undefined>;
  auth?: boolean;
}

interface BackendResult<T> {
  code?: string;
  message?: string;
  data?: T;
}

export interface LoginPayload {
  username: string;
  password: string;
}

export interface RegisterPayload extends LoginPayload {
  realName: string;
  phone: string;
  mail: string;
}

export interface UserInfo {
  id?: number;
  username: string;
  realName?: string;
  phone?: string;
  mail?: string;
}

export interface GroupItem {
  gid: string;
  name: string;
  sortOrder: number;
}

export interface GroupCountItem extends GroupItem {
  linkCount: number;
}

export interface ShortLinkItem {
  id?: number;
  domain: string;
  shortUri: string;
  fullShortUrl: string;
  originUrl: string;
  clickNum: number;
  gid: string;
  enableStatus: number;
  createdType?: number;
  validDateType: number;
  validDate?: string | null;
  describe?: string;
  createTime?: string;
  updateTime?: string;
}

export interface ShortLinkCreatePayload {
  domain: string;
  originUrl: string;
  gid: string;
  createdType: number;
  validDateType: number;
  validDate?: string | null;
  describe?: string;
  username: string;
}

export interface ShortLinkUpdatePayload {
  fullShortUrl: string;
  originUrl: string;
  gid: string;
  validDateType: number;
  validDate?: string | null;
  describe?: string;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
  pages: number;
}

export interface DescriptionResult {
  summary: string;
  isSafe: boolean;
}

let unauthorizedHandler: (() => void) | undefined;

export function setUnauthorizedHandler(handler: () => void) {
  unauthorizedHandler = handler;
}

export function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY) || '';
}

export function storeToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

function buildUrl(base: string, path: string, query?: RequestOptions['query']) {
  const normalizedBase = base.replace(/\/$/, '');
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const url = new URL(`${normalizedBase}${normalizedPath}`, window.location.origin);

  Object.entries(query || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, String(value));
    }
  });

  return url.toString();
}

async function request<T>(base: string, path: string, options: RequestOptions = {}): Promise<T> {
  const token = getStoredToken();
  const headers: Record<string, string> = {
    Accept: 'application/json'
  };

  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }

  if (options.auth !== false && token) {
    headers.Authorization = token;
  }

  const response = await fetch(buildUrl(base, path, options.query), {
    method: options.method || 'GET',
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body)
  });

  const text = await response.text();
  const payload = text ? (JSON.parse(text) as BackendResult<T> | T) : undefined;

  if (response.status === 401) {
    unauthorizedHandler?.();
    throw new Error('登录已过期，请重新登录');
  }

  if (!response.ok) {
    throw new Error(`请求失败：HTTP ${response.status}`);
  }

  if (payload && typeof payload === 'object' && 'code' in payload) {
    const result = payload as BackendResult<T>;
    if (result.code !== '0000') {
      throw new Error(result.message || '后端返回失败');
    }
    return result.data as T;
  }

  return payload as T;
}

function adminRequest<T>(path: string, options?: RequestOptions) {
  return request<T>(ADMIN_BASE, path, options);
}

function projectRequest<T>(path: string, options?: RequestOptions) {
  return request<T>(PROJECT_BASE, path, options);
}

export function toBackendDate(value?: string | null) {
  if (!value) return null;
  const withSeconds = value.length === 16 ? `${value}:00` : value;
  return withSeconds.replace('T', ' ');
}

export function normalizeDomain(domain: string) {
  return domain.trim().replace(/\/+$/, '');
}

export function login(payload: LoginPayload) {
  return adminRequest<string>('/api/shortlink/v1/user/login', {
    method: 'POST',
    body: payload,
    auth: false
  });
}

export function register(payload: RegisterPayload) {
  return adminRequest<void>('/api/shortlink/v1/user/register', {
    method: 'POST',
    body: payload,
    auth: false
  });
}

export function checkUsernameExists(username: string) {
  return adminRequest<boolean>(`/api/shortlink/v1/user/exists/${encodeURIComponent(username)}`, {
    auth: false
  });
}

export function fetchUserInfo() {
  return adminRequest<UserInfo>('/api/shortlink/v1/user/info');
}

export function logout() {
  return adminRequest<boolean>('/api/shortlink/v1/user/logout', {
    method: 'POST'
  });
}

export function fetchGroups() {
  return adminRequest<GroupItem[]>('/api/shortlink/v1/group/list');
}

export function createGroup(groupName: string) {
  return adminRequest<{ name: string; sortOrder: number }>('/api/shortlink/v1/group/save', {
    method: 'POST',
    query: { groupName }
  });
}

export function updateGroup(payload: Pick<GroupItem, 'gid' | 'name'>) {
  return adminRequest<void>('/api/shortlink/v1/group/update', {
    method: 'PUT',
    body: payload
  });
}

export function deleteGroup(gid: string) {
  return adminRequest<void>(`/api/shortlink/v1/group/delete/${encodeURIComponent(gid)}`, {
    method: 'DELETE'
  });
}

export function sortGroups(groups: GroupItem[]) {
  return adminRequest<void>('/api/shortlink/v1/group/sort', {
    method: 'PUT',
    body: groups.map((group, index) => ({
      gid: group.gid,
      sortOrder: index
    }))
  });
}

export function createShortLink(payload: ShortLinkCreatePayload) {
  return projectRequest<ShortLinkItem>('/api/shortlink/v1/link/create', {
    method: 'POST',
    body: {
      ...payload,
      domain: normalizeDomain(payload.domain),
      validDate: payload.validDate || null
    },
    auth: false
  });
}

export function updateShortLink(payload: ShortLinkUpdatePayload) {
  return projectRequest<void>('/api/shortlink/v1/link/update', {
    method: 'PUT',
    body: {
      ...payload,
      validDate: payload.validDate || null
    },
    auth: false
  });
}

export function deleteShortLink(fullShortUrl: string) {
  return projectRequest<void>('/api/shortlink/v1/link/delete', {
    method: 'DELETE',
    query: { fullShortUrl },
    auth: false
  });
}

export async function pageShortLinks(params: { gid?: string; current: number; size: number }) {
  const data = await projectRequest<PageResult<ShortLinkItem> | ShortLinkItem[]>(
    '/api/shortlink/v1/link/page',
    {
      query: params,
      auth: false
    }
  );

  if (Array.isArray(data)) {
    return {
      records: data,
      total: data.length,
      current: params.current,
      size: params.size,
      pages: Math.max(1, Math.ceil(data.length / params.size))
    };
  }

  return {
    records: data?.records || [],
    total: Number(data?.total || 0),
    current: Number(data?.current || params.current),
    size: Number(data?.size || params.size),
    pages: Number(data?.pages || Math.max(1, Math.ceil(Number(data?.total || 0) / params.size)))
  };
}

export function fetchGroupLinkCounts(username: string) {
  return projectRequest<GroupCountItem[]>('/api/shortlink/v1/link/group/list', {
    query: { username },
    auth: false
  });
}

export async function generateDescription(originalUrl: string) {
  const data = await projectRequest<Record<string, unknown>>('/api/shortlink/v1/link/description', {
    query: { originalUrl },
    auth: false
  });

  return {
    summary: String(data?.summary || ''),
    isSafe: Boolean(data?.isSafe ?? data?.is_safe ?? true)
  } satisfies DescriptionResult;
}
