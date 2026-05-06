# ShortLink Frontend

Vue3 + Vite 单页控制台，已按当前后端接口拆分代理：

- `admin-api` -> `shortlink_admin`，默认 `http://localhost:8080`
- `project-api` -> `shortlink_project`，默认 `http://localhost:8081`

## 启动

```bash
npm run dev
```

默认访问 `http://localhost:5173`。

当前机器上的 `shortlink_frontend/node_modules` 已复用现有 Vue3/Vite 依赖目录，不需要重新下载依赖。换到其他机器时再执行 `npm install`。

## 接口适配

- 登录、注册、用户信息、分组管理调用 `shortlink_admin`
- 短链创建、分页、更新、删除、AI 摘要、分组短链统计调用 `shortlink_project`
- 登录返回的 `Bearer ...` token 会原样写入 `Authorization` 请求头
- 短链日期会按后端可接收的 `yyyy-MM-dd HH:mm:ss` 传递
