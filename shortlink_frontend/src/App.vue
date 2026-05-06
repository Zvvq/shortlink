<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import {
  ArrowDown,
  ArrowUp,
  BarChart3,
  Check,
  ChevronLeft,
  ChevronRight,
  Clock,
  Copy,
  ExternalLink,
  Globe,
  Inbox,
  KeyRound,
  Layers,
  Link2,
  LoaderCircle,
  LogOut,
  Mail,
  MousePointerClick,
  Pencil,
  Phone,
  Plus,
  RefreshCw,
  Save,
  Search,
  Sparkles,
  Trash2,
  UserRound,
  X
} from './components/icons';
import {
  checkUsernameExists,
  clearToken,
  createGroup,
  createShortLink,
  deleteGroup,
  deleteShortLink,
  fetchGroupLinkCounts,
  fetchGroups,
  fetchUserInfo,
  generateDescription,
  getStoredToken,
  login,
  logout,
  normalizeDomain,
  pageShortLinks,
  register,
  setUnauthorizedHandler,
  sortGroups,
  storeToken,
  toBackendDate,
  updateGroup,
  updateShortLink
} from './services/api';
import type { GroupItem, GroupCountItem, ShortLinkItem, UserInfo } from './services/api';

type AuthMode = 'login' | 'register';
type NoticeType = 'success' | 'error' | 'info';

const defaultDomain = import.meta.env.VITE_DEFAULT_DOMAIN || 'http://localhost:8081';

const currentUser = ref<UserInfo | null>(null);
const authMode = ref<AuthMode>('login');
const authLoading = ref(false);
const usernameChecking = ref(false);
const usernameHint = ref('');

const authForm = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  mail: ''
});

const groups = ref<GroupItem[]>([]);
const groupCounts = ref<GroupCountItem[]>([]);
const selectedGid = ref('');
const newGroupName = ref('');
const renamingGid = ref('');
const renameValue = ref('');
const groupLoading = ref(false);

const linkLoading = ref(false);
const createLoading = ref(false);
const descLoading = ref(false);
const searchKeyword = ref('');
const lastCreated = ref<ShortLinkItem | null>(null);
const linksPage = reactive({
  records: [] as ShortLinkItem[],
  total: 0,
  current: 1,
  size: 8,
  pages: 1
});

const createForm = reactive({
  domain: defaultDomain,
  originUrl: '',
  gid: '',
  validDateType: 0,
  validDate: '',
  describe: ''
});

const editingLink = ref<ShortLinkItem | null>(null);
const editForm = reactive({
  fullShortUrl: '',
  originUrl: '',
  gid: '',
  validDateType: 0,
  validDate: '',
  describe: ''
});

const notice = reactive({
  message: '',
  type: 'info' as NoticeType
});

let noticeTimer: number | undefined;

const groupCountMap = computed(() => {
  const map = new Map<string, number>();
  groupCounts.value.forEach((item) => map.set(item.gid, Number(item.linkCount || 0)));
  return map;
});

const mergedGroups = computed(() =>
  groups.value.map((group) => ({
    ...group,
    linkCount: groupCountMap.value.get(group.gid) || 0
  }))
);

const currentGroup = computed(() => groups.value.find((group) => group.gid === selectedGid.value));
const currentGroupName = computed(() => currentGroup.value?.name || '未选择分组');
const totalLinks = computed(() => groupCounts.value.reduce((sum, item) => sum + Number(item.linkCount || 0), 0));
const currentClicks = computed(() => linksPage.records.reduce((sum, item) => sum + Number(item.clickNum || 0), 0));
const activeLinks = computed(() => linksPage.records.filter((item) => item.enableStatus === 1).length);
const canPrev = computed(() => linksPage.current > 1);
const canNext = computed(() => linksPage.current < linksPage.pages);

const filteredLinks = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();
  if (!keyword) return linksPage.records;

  return linksPage.records.filter((item) =>
    [item.fullShortUrl, item.originUrl, item.describe, item.shortUri]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword))
  );
});

function showNotice(message: string, type: NoticeType = 'info') {
  notice.message = message;
  notice.type = type;
  window.clearTimeout(noticeTimer);
  noticeTimer = window.setTimeout(() => {
    notice.message = '';
  }, 3200);
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : String(error);
}

function resetWorkspace() {
  currentUser.value = null;
  groups.value = [];
  groupCounts.value = [];
  selectedGid.value = '';
  linksPage.records = [];
  linksPage.total = 0;
  linksPage.current = 1;
  linksPage.pages = 1;
}

async function bootstrap() {
  try {
    currentUser.value = await fetchUserInfo();
    await Promise.all([loadGroups(), loadGroupCounts()]);

    if (!selectedGid.value && groups.value.length) {
      selectedGid.value = groups.value[0].gid;
    }

    createForm.gid = selectedGid.value;
    await loadLinks();
  } catch (error) {
    clearToken();
    resetWorkspace();
    showNotice(getErrorMessage(error), 'error');
  }
}

async function submitAuth() {
  if (!authForm.username.trim() || !authForm.password.trim()) {
    showNotice('请输入用户名和密码', 'error');
    return;
  }

  if (authMode.value === 'register' && (!authForm.realName.trim() || !authForm.phone.trim() || !authForm.mail.trim())) {
    showNotice('请补全注册信息', 'error');
    return;
  }

  try {
    authLoading.value = true;

    if (authMode.value === 'register') {
      await register({
        username: authForm.username.trim(),
        password: authForm.password,
        realName: authForm.realName.trim(),
        phone: authForm.phone.trim(),
        mail: authForm.mail.trim()
      });
      showNotice('注册成功，已为你登录', 'success');
    }

    const token = await login({
      username: authForm.username.trim(),
      password: authForm.password
    });
    storeToken(token);
    await bootstrap();
  } catch (error) {
    showNotice(getErrorMessage(error), 'error');
  } finally {
    authLoading.value = false;
  }
}

async function checkRegisterUsername() {
  if (authMode.value !== 'register' || !authForm.username.trim()) return;

  try {
    usernameChecking.value = true;
    const exists = await checkUsernameExists(authForm.username.trim());
    usernameHint.value = exists ? '用户名已存在' : '用户名可用';
  } catch {
    usernameHint.value = '';
  } finally {
    usernameChecking.value = false;
  }
}

async function signOut() {
  try {
    if (getStoredToken()) {
      await logout();
    }
  } catch {
    // Local logout should still complete when the backend token is already invalid.
  } finally {
    clearToken();
    resetWorkspace();
    showNotice('已退出登录', 'info');
  }
}

async function loadGroups() {
  groupLoading.value = true;
  try {
    const data = await fetchGroups();
    groups.value = [...(data || [])].sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0));
  } finally {
    groupLoading.value = false;
  }
}

async function loadGroupCounts() {
  if (!currentUser.value?.username) return;
  groupCounts.value = await fetchGroupLinkCounts(currentUser.value.username);
}

async function selectGroup(gid: string) {
  selectedGid.value = gid;
  createForm.gid = gid;
  linksPage.current = 1;
  await loadLinks();
}

async function loadLinks() {
  if (!selectedGid.value) {
    linksPage.records = [];
    linksPage.total = 0;
    linksPage.pages = 1;
    return;
  }

  try {
    linkLoading.value = true;
    const data = await pageShortLinks({
      gid: selectedGid.value,
      current: linksPage.current,
      size: linksPage.size
    });
    linksPage.records = data.records || [];
    linksPage.total = data.total || 0;
    linksPage.current = data.current || 1;
    linksPage.size = data.size || linksPage.size;
    linksPage.pages = Math.max(1, data.pages || 1);
  } catch (error) {
    showNotice(getErrorMessage(error), 'error');
  } finally {
    linkLoading.value = false;
  }
}

async function refreshWorkspace() {
  try {
    await Promise.all([loadGroups(), loadGroupCounts()]);
    await loadLinks();
    showNotice('数据已刷新', 'success');
  } catch (error) {
    showNotice(getErrorMessage(error), 'error');
  }
}

async function submitNewGroup() {
  const name = newGroupName.value.trim();
  if (!name) return;

  try {
    await createGroup(name);
    newGroupName.value = '';
    await loadGroups();
    await loadGroupCounts();
    const created = [...groups.value].reverse().find((group) => group.name === name);
    if (created) {
      await selectGroup(created.gid);
    }
    showNotice('分组已创建', 'success');
  } catch (error) {
    showNotice(getErrorMessage(error), 'error');
  }
}

function startRename(group: GroupItem) {
  renamingGid.value = group.gid;
  renameValue.value = group.name;
}

async function commitRename() {
  if (!renamingGid.value || !renameValue.value.trim()) return;

  try {
    await updateGroup({
      gid: renamingGid.value,
      name: renameValue.value.trim()
    });
    renamingGid.value = '';
    renameValue.value = '';
    await loadGroups();
    showNotice('分组已重命名', 'success');
  } catch (error) {
    showNotice(getErrorMessage(error), 'error');
  }
}

async function removeGroup(group: GroupItem) {
  if (!window.confirm(`删除分组「${group.name}」？`)) return;

  try {
    await deleteGroup(group.gid);
    if (selectedGid.value === group.gid) {
      selectedGid.value = '';
    }
    await loadGroups();
    await loadGroupCounts();
    if (!selectedGid.value && groups.value.length) {
      selectedGid.value = groups.value[0].gid;
      createForm.gid = selectedGid.value;
    }
    await loadLinks();
    showNotice('分组已删除', 'success');
  } catch (error) {
    showNotice(getErrorMessage(error), 'error');
  }
}

async function moveGroup(index: number, direction: -1 | 1) {
  const target = index + direction;
  if (target < 0 || target >= groups.value.length) return;

  const nextGroups = [...groups.value];
  const [item] = nextGroups.splice(index, 1);
  nextGroups.splice(target, 0, item);
  groups.value = nextGroups;

  try {
    await sortGroups(nextGroups);
    await loadGroups();
  } catch (error) {
    showNotice(getErrorMessage(error), 'error');
  }
}

async function submitCreate() {
  if (!currentUser.value?.username) return;
  if (!createForm.originUrl.trim()) {
    showNotice('请输入原始链接', 'error');
    return;
  }
  if (!createForm.gid) {
    showNotice('请先选择或创建分组', 'error');
    return;
  }
  if (createForm.validDateType === 1 && !createForm.validDate) {
    showNotice('请选择有效期时间', 'error');
    return;
  }

  try {
    createLoading.value = true;
    const created = await createShortLink({
      domain: normalizeDomain(createForm.domain || defaultDomain),
      originUrl: createForm.originUrl.trim(),
      gid: createForm.gid,
      createdType: 0,
      validDateType: Number(createForm.validDateType),
      validDate: createForm.validDateType === 1 ? toBackendDate(createForm.validDate) : null,
      describe: createForm.describe.trim(),
      username: currentUser.value.username
    });
    lastCreated.value = created;
    createForm.originUrl = '';
    createForm.describe = '';
    createForm.validDate = '';
    await Promise.all([loadGroupCounts(), loadLinks()]);
    showNotice('短链已生成', 'success');
  } catch (error) {
    showNotice(getErrorMessage(error), 'error');
  } finally {
    createLoading.value = false;
  }
}

async function fillDescription() {
  if (!createForm.originUrl.trim()) {
    showNotice('请输入原始链接', 'error');
    return;
  }

  try {
    descLoading.value = true;
    const result = await generateDescription(createForm.originUrl.trim());
    if (!result.isSafe) {
      throw new Error('摘要服务判定该链接不安全');
    }
    createForm.describe = result.summary || createForm.describe;
    showNotice('摘要已生成', 'success');
  } catch (error) {
    showNotice(getErrorMessage(error), 'error');
  } finally {
    descLoading.value = false;
  }
}

function toDateTimeLocal(value?: string | null) {
  if (!value) return '';
  if (/^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}/.test(value)) {
    return value.slice(0, 16).replace(' ', 'T');
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';

  const pad = (num: number) => String(num).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function openEditor(item: ShortLinkItem) {
  editingLink.value = item;
  editForm.fullShortUrl = item.fullShortUrl;
  editForm.originUrl = item.originUrl;
  editForm.gid = item.gid;
  editForm.validDateType = Number(item.validDateType || 0);
  editForm.validDate = toDateTimeLocal(item.validDate);
  editForm.describe = item.describe || '';
}

function closeEditor() {
  editingLink.value = null;
}

async function submitEdit() {
  if (!editingLink.value) return;
  if (!editForm.originUrl.trim()) {
    showNotice('请输入原始链接', 'error');
    return;
  }
  if (editForm.validDateType === 1 && !editForm.validDate) {
    showNotice('请选择有效期时间', 'error');
    return;
  }

  try {
    await updateShortLink({
      fullShortUrl: editForm.fullShortUrl,
      originUrl: editForm.originUrl.trim(),
      gid: editForm.gid,
      validDateType: Number(editForm.validDateType),
      validDate: editForm.validDateType === 1 ? toBackendDate(editForm.validDate) : null,
      describe: editForm.describe.trim()
    });
    closeEditor();
    await loadLinks();
    showNotice('短链已更新', 'success');
  } catch (error) {
    showNotice(getErrorMessage(error), 'error');
  }
}

async function removeLink(item: ShortLinkItem) {
  if (!window.confirm(`删除短链「${item.shortUri}」？`)) return;

  try {
    await deleteShortLink(item.fullShortUrl);
    await Promise.all([loadGroupCounts(), loadLinks()]);
    showNotice('短链已删除', 'success');
  } catch (error) {
    showNotice(getErrorMessage(error), 'error');
  }
}

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text);
  } catch {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand('copy');
    document.body.removeChild(textarea);
  }
  showNotice('已复制到剪贴板', 'success');
}

function openUrl(url: string) {
  const target = /^https?:\/\//i.test(url) ? url : `https://${url}`;
  window.open(target, '_blank', 'noopener,noreferrer');
}

function formatDate(value?: string | null) {
  if (!value) return '永久有效';
  return value.replace('T', ' ').slice(0, 16);
}

function groupName(gid: string) {
  return groups.value.find((group) => group.gid === gid)?.name || gid;
}

function validTypeLabel(type: number) {
  return Number(type) === 1 ? '自定义' : '永久';
}

async function goPage(direction: -1 | 1) {
  const next = linksPage.current + direction;
  if (next < 1 || next > linksPage.pages) return;
  linksPage.current = next;
  await loadLinks();
}

onMounted(() => {
  setUnauthorizedHandler(() => {
    clearToken();
    resetWorkspace();
    showNotice('登录已过期，请重新登录', 'error');
  });

  if (getStoredToken()) {
    bootstrap();
  }
});
</script>

<template>
  <div v-if="!currentUser" class="auth-page">
    <section class="auth-visual">
      <div class="brand-lockup">
        <span class="brand-mark"><Link2 :size="25" /></span>
        <span>ShortLink Console</span>
      </div>
      <h1>短链运营台</h1>
      <p class="auth-subtitle">链接、分组、统计与安全摘要聚合在同一个 Vue3 控制台。</p>
      <div class="signal-board" aria-hidden="true">
        <span class="signal-node node-a"></span>
        <span class="signal-node node-b"></span>
        <span class="signal-node node-c"></span>
        <span class="signal-node node-d"></span>
        <span class="signal-line line-a"></span>
        <span class="signal-line line-b"></span>
        <span class="signal-line line-c"></span>
      </div>
      <div class="metric-strip">
        <span><BarChart3 :size="18" />实时分页</span>
        <span><Layers :size="18" />分组管理</span>
        <span><Sparkles :size="18" />智能摘要</span>
      </div>
    </section>

    <section class="auth-panel" aria-label="登录注册">
      <div class="segmented">
        <button :class="{ active: authMode === 'login' }" type="button" @click="authMode = 'login'">登录</button>
        <button :class="{ active: authMode === 'register' }" type="button" @click="authMode = 'register'">注册</button>
      </div>

      <form class="auth-form" @submit.prevent="submitAuth">
        <label>
          <span>用户名</span>
          <div class="field">
            <UserRound :size="18" />
            <input v-model.trim="authForm.username" autocomplete="username" @blur="checkRegisterUsername" />
          </div>
          <small v-if="authMode === 'register' && (usernameHint || usernameChecking)">
            {{ usernameChecking ? '检查中...' : usernameHint }}
          </small>
        </label>

        <label>
          <span>密码</span>
          <div class="field">
            <KeyRound :size="18" />
            <input v-model="authForm.password" type="password" autocomplete="current-password" />
          </div>
        </label>

        <template v-if="authMode === 'register'">
          <label>
            <span>姓名</span>
            <div class="field">
              <UserRound :size="18" />
              <input v-model.trim="authForm.realName" autocomplete="name" />
            </div>
          </label>
          <label>
            <span>手机</span>
            <div class="field">
              <Phone :size="18" />
              <input v-model.trim="authForm.phone" autocomplete="tel" />
            </div>
          </label>
          <label>
            <span>邮箱</span>
            <div class="field">
              <Mail :size="18" />
              <input v-model.trim="authForm.mail" autocomplete="email" />
            </div>
          </label>
        </template>

        <button class="primary-action" type="submit" :disabled="authLoading">
          <LoaderCircle v-if="authLoading" class="spin" :size="18" />
          <Check v-else :size="18" />
          {{ authMode === 'login' ? '进入控制台' : '创建账号' }}
        </button>
      </form>
    </section>
  </div>

  <div v-else class="console-shell">
    <aside class="sidebar">
      <div class="side-head">
        <span class="brand-mark"><Link2 :size="22" /></span>
        <div>
          <strong>ShortLink</strong>
          <small>{{ currentUser.username }}</small>
        </div>
      </div>

      <form class="new-group" @submit.prevent="submitNewGroup">
        <input v-model.trim="newGroupName" placeholder="新分组" />
        <button type="submit" title="创建分组"><Plus :size="18" /></button>
      </form>

      <div class="group-list" :class="{ loading: groupLoading }">
        <div v-for="(group, index) in mergedGroups" :key="group.gid" class="group-row">
          <button class="group-select" :class="{ active: selectedGid === group.gid }" type="button" @click="selectGroup(group.gid)">
            <Layers :size="17" />
            <span>{{ group.name }}</span>
            <em>{{ group.linkCount }}</em>
          </button>
          <div class="group-actions">
            <button type="button" title="上移" :disabled="index === 0" @click="moveGroup(index, -1)">
              <ArrowUp :size="15" />
            </button>
            <button type="button" title="下移" :disabled="index === groups.length - 1" @click="moveGroup(index, 1)">
              <ArrowDown :size="15" />
            </button>
            <button type="button" title="重命名" @click="startRename(group)">
              <Pencil :size="15" />
            </button>
            <button type="button" title="删除" @click="removeGroup(group)">
              <Trash2 :size="15" />
            </button>
          </div>
        </div>
      </div>

      <div v-if="renamingGid" class="rename-box">
        <input v-model.trim="renameValue" @keyup.enter="commitRename" />
        <button type="button" title="保存" @click="commitRename"><Save :size="16" /></button>
        <button type="button" title="取消" @click="renamingGid = ''"><X :size="16" /></button>
      </div>

      <button class="logout-button" type="button" @click="signOut">
        <LogOut :size="18" />
        退出登录
      </button>
    </aside>

    <main class="workspace">
      <header class="topbar">
        <div>
          <p class="eyebrow">当前分组</p>
          <h2>{{ currentGroupName }}</h2>
        </div>
        <div class="top-actions">
          <div class="search-field">
            <Search :size="18" />
            <input v-model.trim="searchKeyword" placeholder="筛选当前页" />
          </div>
          <button class="icon-text" type="button" title="刷新" @click="refreshWorkspace">
            <RefreshCw :size="18" />
            刷新
          </button>
        </div>
      </header>

      <section class="stats-grid">
        <article class="stat-card">
          <Layers :size="22" />
          <span>分组</span>
          <strong>{{ groups.length }}</strong>
        </article>
        <article class="stat-card">
          <Link2 :size="22" />
          <span>短链</span>
          <strong>{{ totalLinks }}</strong>
        </article>
        <article class="stat-card">
          <MousePointerClick :size="22" />
          <span>本页点击</span>
          <strong>{{ currentClicks }}</strong>
        </article>
        <article class="stat-card">
          <Clock :size="22" />
          <span>启用中</span>
          <strong>{{ activeLinks }}</strong>
        </article>
      </section>

      <section class="workspace-grid">
        <form class="surface creator-panel" @submit.prevent="submitCreate">
          <div class="section-head">
            <div>
              <p class="eyebrow">Create</p>
              <h3>生成短链</h3>
            </div>
            <button class="ghost-action" type="button" :disabled="descLoading" @click="fillDescription">
              <LoaderCircle v-if="descLoading" class="spin" :size="17" />
              <Sparkles v-else :size="17" />
              AI 摘要
            </button>
          </div>

          <div class="form-grid">
            <label class="span-2">
              <span>原始链接</span>
              <input v-model.trim="createForm.originUrl" placeholder="https://example.com/article" />
            </label>
            <label>
              <span>短链域名</span>
              <div class="field compact">
                <Globe :size="17" />
                <input v-model.trim="createForm.domain" />
              </div>
            </label>
            <label>
              <span>分组</span>
              <select v-model="createForm.gid">
                <option value="" disabled>选择分组</option>
                <option v-for="group in groups" :key="group.gid" :value="group.gid">{{ group.name }}</option>
              </select>
            </label>
            <label>
              <span>有效期</span>
              <div class="segmented small">
                <button :class="{ active: createForm.validDateType === 0 }" type="button" @click="createForm.validDateType = 0">永久</button>
                <button :class="{ active: createForm.validDateType === 1 }" type="button" @click="createForm.validDateType = 1">自定义</button>
              </div>
            </label>
            <label>
              <span>到期时间</span>
              <input v-model="createForm.validDate" type="datetime-local" :disabled="createForm.validDateType === 0" />
            </label>
            <label class="span-2">
              <span>描述</span>
              <textarea v-model.trim="createForm.describe" rows="3" placeholder="活动、渠道或页面摘要"></textarea>
            </label>
          </div>

          <button class="primary-action align-end" type="submit" :disabled="createLoading || !groups.length">
            <LoaderCircle v-if="createLoading" class="spin" :size="18" />
            <Plus v-else :size="18" />
            生成短链
          </button>
        </form>

        <section class="surface created-panel">
          <div class="section-head">
            <div>
              <p class="eyebrow">Latest</p>
              <h3>最近生成</h3>
            </div>
          </div>
          <div v-if="lastCreated" class="created-link">
            <strong>{{ lastCreated.shortUri }}</strong>
            <button type="button" @click="copyText(lastCreated.fullShortUrl)">
              <Copy :size="17" />
              复制
            </button>
            <p>{{ lastCreated.fullShortUrl }}</p>
            <span>{{ lastCreated.originUrl }}</span>
          </div>
          <div v-else class="empty-compact">
            <Inbox :size="28" />
            <span>等待第一条短链</span>
          </div>
        </section>
      </section>

      <section class="surface links-panel">
        <div class="section-head">
          <div>
            <p class="eyebrow">Links</p>
            <h3>短链列表</h3>
          </div>
          <div class="pager">
            <button type="button" :disabled="!canPrev" title="上一页" @click="goPage(-1)">
              <ChevronLeft :size="18" />
            </button>
            <span>{{ linksPage.current }} / {{ linksPage.pages }}</span>
            <button type="button" :disabled="!canNext" title="下一页" @click="goPage(1)">
              <ChevronRight :size="18" />
            </button>
          </div>
        </div>

        <div v-if="linkLoading" class="loading-state">
          <LoaderCircle class="spin" :size="24" />
          <span>加载中</span>
        </div>

        <div v-else-if="!filteredLinks.length" class="empty-state">
          <Inbox :size="34" />
          <strong>暂无短链</strong>
          <span>{{ groups.length ? '当前分组还没有短链' : '请先创建分组' }}</span>
        </div>

        <div v-else class="link-table">
          <div class="table-head">
            <span>短链</span>
            <span>原始链接</span>
            <span>分组</span>
            <span>点击</span>
            <span>有效期</span>
            <span>操作</span>
          </div>
          <article v-for="item in filteredLinks" :key="item.fullShortUrl" class="link-row-card">
            <div class="short-cell">
              <strong>{{ item.shortUri }}</strong>
              <span>{{ item.fullShortUrl }}</span>
            </div>
            <div class="origin-cell">
              <span>{{ item.originUrl }}</span>
              <small>{{ item.describe || '无描述' }}</small>
            </div>
            <span class="chip">{{ groupName(item.gid) }}</span>
            <strong class="clicks">{{ item.clickNum || 0 }}</strong>
            <span class="muted">{{ item.validDateType === 0 ? validTypeLabel(item.validDateType) : formatDate(item.validDate) }}</span>
            <div class="row-actions">
              <button type="button" title="复制" @click="copyText(item.fullShortUrl)"><Copy :size="17" /></button>
              <button type="button" title="打开" @click="openUrl(item.fullShortUrl)"><ExternalLink :size="17" /></button>
              <button type="button" title="编辑" @click="openEditor(item)"><Pencil :size="17" /></button>
              <button type="button" title="删除" @click="removeLink(item)"><Trash2 :size="17" /></button>
            </div>
          </article>
        </div>
      </section>
    </main>

    <div v-if="editingLink" class="modal-backdrop" @click.self="closeEditor">
      <form class="modal-panel" @submit.prevent="submitEdit">
        <div class="section-head">
          <div>
            <p class="eyebrow">Edit</p>
            <h3>编辑短链</h3>
          </div>
          <button type="button" title="关闭" @click="closeEditor"><X :size="18" /></button>
        </div>

        <label>
          <span>短链</span>
          <input v-model="editForm.fullShortUrl" disabled />
        </label>
        <label>
          <span>原始链接</span>
          <input v-model.trim="editForm.originUrl" />
        </label>
        <label>
          <span>分组</span>
          <input :value="groupName(editForm.gid)" disabled />
        </label>
        <label>
          <span>有效期</span>
          <div class="segmented small">
            <button :class="{ active: editForm.validDateType === 0 }" type="button" @click="editForm.validDateType = 0">永久</button>
            <button :class="{ active: editForm.validDateType === 1 }" type="button" @click="editForm.validDateType = 1">自定义</button>
          </div>
        </label>
        <label>
          <span>到期时间</span>
          <input v-model="editForm.validDate" type="datetime-local" :disabled="editForm.validDateType === 0" />
        </label>
        <label>
          <span>描述</span>
          <textarea v-model.trim="editForm.describe" rows="3"></textarea>
        </label>

        <button class="primary-action" type="submit">
          <Save :size="18" />
          保存修改
        </button>
      </form>
    </div>
  </div>

  <Transition name="toast">
    <div v-if="notice.message" class="toast" :class="notice.type">
      {{ notice.message }}
    </div>
  </Transition>
</template>
